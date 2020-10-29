package gov.nih.nci.curator;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.EnumMap;
import java.util.Map.Entry;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.border.Border;

import org.protege.editor.core.ui.preferences.PreferencesLayoutPanel;
import org.protege.editor.owl.model.OWLModelManager;
import org.protege.editor.owl.model.event.EventType;
import org.protege.editor.owl.model.inference.OWLReasonerManager;
import org.protege.editor.owl.model.inference.ProtegeOWLReasonerInfo;
import org.protege.editor.owl.model.inference.ReasonerStatus;
import org.protege.editor.owl.model.inference.ReasonerPreferences.OptionalInferenceTask;
import org.protege.editor.owl.ui.preferences.OWLPreferencesPanel;

import gov.nih.nci.curator.CuratorReasonerPreferences.OptionalEditChecksTask;


/**
 *
 * @author Evren Sirin
 */
public class CuratorPreferencesPanel extends OWLPreferencesPanel {
	private enum ExplanationMode {
		NONE("Disable explanations"), LIMITED("Limit explanations to"), ALL("Show all explanations");

		private final String label;

		ExplanationMode(final String theLabel) {
			label = theLabel;
		}

		@Override
		public String toString() {
			return label;
		}
	}
	private ButtonGroup explanationModeGroup = new ButtonGroup();;
	private EnumMap<ExplanationMode, JRadioButton> explanationModeButtons = new EnumMap<ExplanationMode, JRadioButton>(ExplanationMode.class);
	private JSpinner explanationCount;
	private EnumMap<OptionalEditChecksTask, JCheckBox> enabledMap = new EnumMap<>(OptionalEditChecksTask.class);
	
	CuratorReasonerPreferences prefs = CuratorReasonerPreferences.getInstance();

	@Override
	public void initialise() throws Exception {
		
		setLayout(new BorderLayout());
		

		Box box = Box.createVerticalBox();
		box.add(createExplanationPanel());
		box.add(Box.createVerticalGlue());
		add(box, BorderLayout.NORTH);
		
		PreferencesLayoutPanel layoutPanel = new PreferencesLayoutPanel();
		add(layoutPanel, BorderLayout.CENTER);
		
		layoutPanel.addSeparator();        
		
		layoutPanel.addGroup("Curator Edit Checks");

        layoutPanel.addGroupComponent(getCheckBox(CuratorReasonerPreferences.OptionalEditChecksTask.CHECK_DISJOINT_CLASSES, 
        		"Check Disjointness"));
        layoutPanel.addGroupComponent(getCheckBox(CuratorReasonerPreferences.OptionalEditChecksTask.CHECK_REDUNDANT_PARENT, 
        		"Check Redundant Parent"));
        layoutPanel.addGroupComponent(getCheckBox(CuratorReasonerPreferences.OptionalEditChecksTask.CHECK_UNSUPPORTED_CONSTRUCTS, 
        		"Check Unsupported Language Constructs"));
        layoutPanel.addGroupComponent(getCheckBox(CuratorReasonerPreferences.OptionalEditChecksTask.CHECK_SUBCLASS_FULL_DEFINED, 
        		"Check Defined Class has no subclass"));
        layoutPanel.addGroupComponent(getCheckBox(CuratorReasonerPreferences.OptionalEditChecksTask.CHECK_BAD_RETREE, 
        		"Check retree to disjoint domain has no references"));
	}

	private Border createTitledBorder(JPanel panel, String title) {
		Color color = panel.getBackground();
		Border shadow = BorderFactory.createMatteBorder(1, 0, 0, 0, color.darker());
		Border highlight = BorderFactory.createMatteBorder(1, 0, 0, 0, color.brighter());
		Border etchedLine = BorderFactory.createCompoundBorder(shadow, highlight);
		return BorderFactory.createTitledBorder(etchedLine, title);
	}


	private JPanel createExplanationPanel() {
		JPanel panel = new JPanel();
		panel.setLayout(new GridBagLayout());
		panel.setBorder(createTitledBorder(panel, "Explanations"));
		GridBagConstraints c = new GridBagConstraints();

		ActionListener listener = new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				explanationCount.setEnabled(explanationModeButtons.get(ExplanationMode.LIMITED).isSelected());
			}
		};

		//CuratorReasonerPreferences prefs = CuratorReasonerPreferences.getInstance();

		int expCount = prefs.getExplanationCount();

		ButtonGroup group = new ButtonGroup();

		c.gridx = 0;
		c.gridy = 0;
		c.gridwidth = 1;
		c.gridheight = 1;
		c.fill = GridBagConstraints.NONE;
		c.insets = new Insets(5,10,0,10);
		c.anchor = GridBagConstraints.FIRST_LINE_START;
		panel.add(createButton(ExplanationMode.NONE, explanationModeButtons, explanationModeGroup, listener), c);

		c.gridy = 1;
		panel.add(createButton(ExplanationMode.LIMITED, explanationModeButtons, explanationModeGroup, listener), c);

		c.gridy = 2;
		panel.add(createButton(ExplanationMode.ALL, explanationModeButtons, explanationModeGroup, listener), c);

		c.gridx = 1;
		c.gridy = 1;
		c.weightx = 1.0;
		c.anchor = GridBagConstraints.WEST;
		c.insets = new Insets(5, 0, 0, 10);
		explanationCount = new JSpinner(new SpinnerNumberModel(Math.max(1, expCount), 1, 99, 1));
		explanationCount.setEnabled(expCount > 0);
		panel.add(explanationCount, c);

		explanationModeButtons.get(explanationMode(expCount)).setSelected(true);

		return panel;
	}

	private ExplanationMode explanationMode(int expCount) {
		return expCount == 0 ? ExplanationMode.NONE : expCount < 0 ? ExplanationMode.ALL : ExplanationMode.LIMITED;
	}

	private int explanationCount(ExplanationMode explanationMode, int expCount) {
		return explanationMode == ExplanationMode.NONE ? 0 : explanationMode == ExplanationMode.ALL ? -1 : expCount;
	}

	private <E extends Enum<E>> JRadioButton createButton(E type, EnumMap<E, JRadioButton> buttons, ButtonGroup group, ActionListener listener) {
		String label = type.toString();
		JRadioButton button = new JRadioButton(label.charAt(0) + label.substring(1).toLowerCase());
		button.setActionCommand(type.name());
		button.addActionListener(listener);
		button.setAlignmentX(Component.LEFT_ALIGNMENT);

		group.add(button);
		buttons.put(type, button);
		return button;
	}
	
	private JCheckBox getCheckBox(OptionalEditChecksTask task, String description) {    
        JCheckBox enabledBox = enabledMap.get(task);
        if (enabledBox == null) {
        	/**
			description = "<html><body>" + description + " <span style='color: gray;'>("
					+ timeToString(preferences.getTimeInTask(task)) + " total/"
					+ timeToString(preferences.getAverageTimeInTask(task))
					+ " average)</span>";
					**/
            enabledBox = new JCheckBox(description);
            enabledBox.setSelected(prefs.isEnabled(task));
            enabledMap.put(task, enabledBox);
        }
        return enabledBox;
    }

	@Override
	public void applyChanges() {
		

		ExplanationMode explanationMode = ExplanationMode.valueOf(explanationModeGroup.getSelection().getActionCommand());
		System.out.println(explanationCount(explanationMode, (Integer) explanationCount.getValue()));
		prefs.setExplanationCount(explanationCount(explanationMode, (Integer) explanationCount.getValue()));
		
		
	        for (Entry<OptionalEditChecksTask, JCheckBox> entry : enabledMap.entrySet()) {
	            OptionalEditChecksTask task = entry.getKey();
	            JCheckBox enabledBox = entry.getValue();
	            prefs.setEnabled(task, enabledBox.isSelected());
	        }
	        

		boolean preferencesUpdated = prefs.save();

		if (preferencesUpdated) {
			OWLModelManager modelManager = getOWLModelManager();
			OWLReasonerManager reasonerManager = modelManager.getOWLReasonerManager();
			ProtegeOWLReasonerInfo reasoner = reasonerManager.getCurrentReasonerFactory();
			ReasonerStatus reasonerStatus = reasonerManager.getReasonerStatus();

			// if pellet was already initialized, we need to reset it
			if (reasoner instanceof CuratorReasonerFactory) {
				((CuratorReasonerFactory) reasoner).preferencesUpdated();

				if (reasonerStatus == ReasonerStatus.INITIALIZED) {
					reasonerManager.killCurrentReasoner();
					modelManager.fireEvent(EventType.REASONER_CHANGED);
				}
			}
		}
	}

	@Override
	public void dispose() throws Exception {
	}
}