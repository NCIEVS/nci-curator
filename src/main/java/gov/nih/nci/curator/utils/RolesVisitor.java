package gov.nih.nci.curator.utils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.Nonnull;

import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLClassExpressionVisitor;
import org.semanticweb.owlapi.model.OWLEquivalentClassesAxiom;
import org.semanticweb.owlapi.model.OWLObjectAllValuesFrom;
import org.semanticweb.owlapi.model.OWLObjectComplementOf;
import org.semanticweb.owlapi.model.OWLObjectExactCardinality;
import org.semanticweb.owlapi.model.OWLObjectHasValue;
import org.semanticweb.owlapi.model.OWLObjectIntersectionOf;
import org.semanticweb.owlapi.model.OWLObjectMaxCardinality;
import org.semanticweb.owlapi.model.OWLObjectMinCardinality;
import org.semanticweb.owlapi.model.OWLObjectSomeValuesFrom;
import org.semanticweb.owlapi.model.OWLObjectUnionOf;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class RolesVisitor implements OWLClassExpressionVisitor {
	private OWLClass cls = null;
	private OWLOntology ont = null;
	
	private boolean local = true;
	private int non_local = 0;
	private boolean first = true;
	
	public boolean errors = false;
	
	private static final Logger log = LoggerFactory.getLogger(RolesVisitor.class);

	public List<OWLObjectSomeValuesFrom> roles = new ArrayList<OWLObjectSomeValuesFrom>();
	
	public Set<OWLAxiom> bad_constructs = new HashSet<OWLAxiom>();
	private OWLAxiom cur_ax = null;

	public RolesVisitor(OWLClass entity, OWLOntology ontology) {
		cls = entity;
		ont = ontology;
	}
	
	public void setEntity(OWLClass e, boolean loc) {
		this.cls = e;
		roles = new ArrayList<OWLObjectSomeValuesFrom>();
		local = loc;
		non_local = 0;
		first = true;
		
		errors = false;
		bad_constructs = new HashSet<OWLAxiom>();
		cur_ax = null;
	}

	@Override
	public void visit(OWLClass ce) {
		if (ce.equals(cls) && first) {
			first = false;
			
			for (OWLSubClassOfAxiom ax : ont.getSubClassAxiomsForSubClass(cls)) {
				OWLClassExpression exp = ax.getSuperClass();
				cur_ax = ax;
				exp.accept(this);				
			}
			for (OWLEquivalentClassesAxiom eax : ont.getEquivalentClassesAxioms(cls)) {
				for (OWLClassExpression exp : eax.getClassExpressions()) {
					if (exp.isOWLClass() && exp.asOWLClass().equals(cls)) {

					} else {
						cur_ax = eax;
						exp.accept(this);
					}

				}
			}
		} else if (!ce.equals(cls) && !local) {
			non_local++;
			for (OWLSubClassOfAxiom ax : ont.getSubClassAxiomsForSubClass(ce)) {
				OWLClassExpression exp = ax.getSuperClass();
				exp.accept(this);				
			}
			for (OWLEquivalentClassesAxiom eax : ont.getEquivalentClassesAxioms(ce)) {
				for (OWLClassExpression exp : eax.getClassExpressions()) {
					if (exp.isOWLClass() && exp.asOWLClass().equals(ce)) {

					} else {
						exp.accept(this);
					}

				}
			}
			non_local--;
		}
	}

	@Override
	public void visit(OWLObjectIntersectionOf ce) {
		for (OWLClassExpression exp : ce.getOperands()) {
			exp.accept(this);
			
		}
	}

	@Override
	public void visit(@Nonnull OWLObjectUnionOf ce) {
		
		for (OWLClassExpression exp : ce.getOperands()) {
			exp.accept(this);
			
		}
	}

	

	@Override
	public void visit(@Nonnull OWLObjectSomeValuesFrom ce) {
		if (!roles.contains(ce)) {
			roles.add(ce);
		}

	}
	
	public void visit(OWLObjectComplementOf ce) {
		processUnsupportedExp(ce);
    }
	
	public void visit(OWLObjectAllValuesFrom ce) {
		processUnsupportedExp(ce);
		
    }
	
	/**
     * visit OWLObjectHasValue type
     *
     * @param ce ce to visit
     */
	public void visit(OWLObjectHasValue ce) {
		processUnsupportedExp(ce);
	}

    /**
     * visit OWLObjectMinCardinality type
     *
     * @param ce ce to visit
     */
    public void visit(OWLObjectMinCardinality ce) {
    	processUnsupportedExp(ce);
    }

    /**
     * visit OWLObjectExactCardinality type
     *
     * @param ce ce to visit
     */
    public void visit(OWLObjectExactCardinality ce) {
    	processUnsupportedExp(ce);
    }

    /**
     * visit OWLObjectMaxCardinality type
     *
     * @param ce ce to visit
     */
    public void visit(OWLObjectMaxCardinality ce) {
    	processUnsupportedExp(ce);
    }
    
    private void processUnsupportedExp(OWLClassExpression exp) {
    	if (non_local > 0) { 
    	} else {
    		log.info("Class: {} Expression Not Supported: {}", cls.getIRI().getFragment(), printExpression(exp));
    		errors = true;    		
    		this.bad_constructs.add(cur_ax);
    		cur_ax = null;

    		doDefault(exp);
    	}
    }
	
	private String printExpression(OWLClassExpression exp) {
		if (exp instanceof OWLObjectComplementOf) {
			return "not " + printExpression(((OWLObjectComplementOf) exp).getOperand());
		} else if (exp instanceof OWLObjectAllValuesFrom) {
			OWLObjectAllValuesFrom oavf = (OWLObjectAllValuesFrom) exp;
			return 
					oavf.getProperty().asOWLObjectProperty().getIRI().getFragment() +
					" only " +
					printExpression(oavf.getFiller());
		} else if (exp instanceof OWLObjectMinCardinality) {
			OWLObjectMinCardinality omc = (OWLObjectMinCardinality) exp;
			return "at least " + omc.getCardinality() + " " +
					omc.getProperty().asOWLObjectProperty().getIRI().getFragment();
					

		} else if (exp instanceof OWLClass) {
			return exp.asOWLClass().getIRI().getFragment();
		}
		return exp.toString();

	}
	
	

}
