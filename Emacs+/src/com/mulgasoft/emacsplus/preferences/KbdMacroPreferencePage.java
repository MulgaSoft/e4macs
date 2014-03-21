/**
 * Copyright (c) 2009, 2010 Mark Feber, MulgaSoft
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package com.mulgasoft.emacsplus.preferences;

import org.eclipse.jface.preference.DirectoryFieldEditor;
import org.eclipse.jface.preference.RadioGroupFieldEditor;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Composite;

import com.mulgasoft.emacsplus.EmacsPlusActivator;
import com.mulgasoft.emacsplus.EmacsPlusUtils;
import com.mulgasoft.emacsplus.commands.KbdMacroFileHandler;
import com.mulgasoft.emacsplus.execute.KbdMacroSupport;
import com.mulgasoft.emacsplus.execute.KbdMacroSupport.LoadState;

/**
 * Kbd Macro preference page
 * - Sets the kbd macro storage directory
 * - Sets the kbd macro auto-load option and (optional) subset to load
 * 
 * @author Mark Feber - initial API and implementation
 */
public class KbdMacroPreferencePage extends EmacsPlusPreferenceBase {

	private KbdMacroListEditor someListEditor = null;
	private Composite someListParent = null;	
	private Composite loadParent = null;		
	private DirectoryFieldEditor directoryEditor = null;
	private RadioGroupFieldEditor radioEditor = null;
	private ButtonFieldEditor loadButton = null;
	
	public KbdMacroPreferencePage() {
		super(FLAT);
		setPreferenceStore(EmacsPlusActivator.getDefault().getPreferenceStore());		
	}

	/**
	 * @see org.eclipse.jface.preference.FieldEditorPreferencePage#createFieldEditors()
	 */
	@Override
	protected void createFieldEditors() {
		Composite parent = getFieldEditorParent();
		addField(
			new LabelFieldEditor(
				EmacsPlusActivator.getString("EmacsPlusPref_KbdMacroDir"),  											 //$NON-NLS-1$
				parent));
		
		parent = getFieldEditorParent();
		directoryEditor = new DirectoryFieldEditor(
				EmacsPlusPreferenceConstants.P_KBD_MACRO_DIRECTORY,
				EmacsPlusUtils.EMPTY_STR,
				parent);
		addField(directoryEditor);
		addSpace();
		
		parent = getFieldEditorParent();
		radioEditor = new RadioGroupFieldEditor(EmacsPlusPreferenceConstants.P_KBD_MACRO_AUTO_LOAD,
				EmacsPlusActivator.getString("EmacsPlusPref_KbdMacroLoad"), 1, new String[][] { 						 //$NON-NLS-1 
						{ EmacsPlusActivator.getString("EmacsPlusPref_KbdMacroAll"), LoadState.ALL.toString() },		 //$NON-NLS-1 
						{ EmacsPlusActivator.getString("EmacsPlusPref_KbdMacroNone"), LoadState.NONE.toString() },  	 //$NON-NLS-1 
						{ EmacsPlusActivator.getString("EmacsPlusPref_KbdMacroSelected"), LoadState.SOME.toString() } }, //$NON-NLS-1 
						parent, true);
		addField(radioEditor);
		
		parent = getFieldEditorParent();
		loadParent = parent;
		loadButton = new ButtonFieldEditor(loadParent, EmacsPlusActivator.getString("EmacsPlusPref_KbdMacroLoadButton"), //$NON-NLS-1$
				new SelectionAdapter() {
                public void widgetSelected(SelectionEvent evt) {
                	// save the current state
                	KbdMacroPreferencePage.this.performApply();
                	// load the kbd macros
                	KbdMacroSupport.getInstance().autoLoadMacros();
                }
            });
		addField(loadButton);
		
		parent = getFieldEditorParent();
		someListParent = parent;
		someListEditor = new KbdMacroListEditor(
				EmacsPlusPreferenceConstants.P_KBD_MACRO_NAME_LOAD,
				EmacsPlusActivator.getString("EmacsPlusPref_KbdMacroLoadName"), 										 //$NON-NLS-1$
				EmacsPlusActivator.getString("EmacsPlusPref_KbdMacroAddTitle"), 										 //$NON-NLS-1$
				someListParent, directoryEditor);
		addField(someListEditor);
		
		// enable/disable the list editor based on the auto load state preference
		String currentValue = (getPreferenceStore().getString(EmacsPlusPreferenceConstants.P_KBD_MACRO_AUTO_LOAD)); 
		setSomeListEnabled(currentValue);
		setLoadButtonEnabled(currentValue);
	}
	
	/**
	 * @see org.eclipse.jface.preference.FieldEditorPreferencePage#performOk()
	 */
	public boolean performOk() {
		boolean result = super.performOk();
		if (result) {
			KbdMacroFileHandler.setKbdMacroDirectory(getPreferenceStore().getString(EmacsPlusPreferenceConstants.P_KBD_MACRO_DIRECTORY));
			KbdMacroSupport.setLoadState(LoadState.valueOf(getPreferenceStore().getString(EmacsPlusPreferenceConstants.P_KBD_MACRO_AUTO_LOAD)));
			getPreferenceStore().getString(EmacsPlusPreferenceConstants.P_KBD_MACRO_NAME_LOAD);
		}
		return result;
	}
	
    /**
     * Listen for changes in the radio and directory dialogs
     * 
     * @see org.eclipse.jface.preference.FieldEditorPreferencePage#propertyChange(org.eclipse.jface.util.PropertyChangeEvent)
     */
    public void propertyChange(PropertyChangeEvent event) {
    	super.propertyChange(event);
    	if (event.getSource().equals(radioEditor)) {
    		// new value is different form old value when enabling new value
    		if (!event.getNewValue().equals(event.getOldValue())){
    			// validate the current active set, as the directory may have changed and
    			// Eclipse doesn't force a change event on the directory editor when selection changes 
    			someListEditor.validate();
    			String newValue = (String)event.getNewValue();
    			setSomeListEnabled(newValue);	
    			setLoadButtonEnabled(newValue);	
    		}
    	} else if (event.getSource().equals(directoryEditor)) {
    		// the interface thinks the directory has changed, so validate the current active set
    		// if we're in the select some auto-load state
    		if (someListEditor.isEnabled()) {
    			someListEditor.validate();
    		}
    	}
    }	
	
	/**
	 * Enable/Disable embedded list editor
	 * 
	 * @param value if LoadState.SOME then enable, else disable
	 */
	private void setSomeListEnabled(String value) {
		someListEditor.setEnabled((LoadState.SOME.toString().equals(value) ? true : false), someListParent);
	}
	
	private void setLoadButtonEnabled(String value) {
		loadButton.setEnabled((LoadState.NONE.toString().equals(value) ? false : true), loadParent);
	}

}
