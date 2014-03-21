/**
 * Copyright (c) 2009, 2010 Mark Feber, MulgaSoft
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package com.mulgasoft.emacsplus.preferences;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.Set;
import java.util.StringTokenizer;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.StatusDialog;
import org.eclipse.jface.preference.DirectoryFieldEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Shell;

import com.mulgasoft.emacsplus.execute.KbdMacroSupport;

/**
 * @author Mark Feber - initial API and implementation
 */
public class KbdMacroListEditor extends EMPListEditor {

    // The array of names currently set in this dialog
    ArrayList<String> active = new ArrayList<String>();

    /**
     * sub dialog title
     */
    private String chooserLabel;
    
    private DirectoryFieldEditor directoryEditor = null;
    
	/**
	 * @param name
	 * @param labelText
	 * @param chooserLabel
	 * @param parent
	 */
	KbdMacroListEditor(String name, String labelText, String chooserLabel, Composite parent, DirectoryFieldEditor directoryEditor) {
        super(name,labelText,parent);
        this.chooserLabel = chooserLabel;
		this.directoryEditor = directoryEditor;
    }

	public boolean isEnabled() {
		return getLabelControl().isEnabled();
	}
	
	public void validate() {
		Set<String> all = getAllMacros();
		ArrayList<String> valid = new ArrayList<String>();
		for (String s : active) {
			if (all.contains(s)) {
				valid.add(s);
			}
		}
		if (active.size() != valid.size()) {
			active = valid;
			activeChangeCheck();
			setPresentsDefaultValue(checkDefaults(active));
			super.selectionChanged();
		}
	}
	
    /**
     * create a 'list' of macro names in a parsable string
     * 
     * @see com.mulgasoft.emacsplus.preferences.MListEditor#createList(java.lang.String[])
     */
    protected String createList(String[] items) {

    	String[] orderedMacros = sortMacros(active);
        StringBuilder result = new StringBuilder();

        for (int i = 0; i < orderedMacros.length; i++) {
            result.append(orderedMacros[i]);
            result.append(SEPARATOR);
        }
        return result.toString();
    }

    /**
     * @param macroList is a 'list' of macro names in a parsable string
     *  
     * @see org.eclipse.jface.preference.ListEditor#parseString(java.lang.String)
     */
    protected String[] parseString(String macroList) {
        active = convertToMacros(macroList,getAllMacros());
        return active.toArray(new String[0]);
    }
    
    /**
     * Compute the set of macros not currently active
     * 
     * @param all the map of kbd macros in the current directory
     * @return the list of currently non-selected macros 
     */
    private ArrayList<String> getInactive(Set<String> all) {
    	ArrayList<String> result = new ArrayList<String>();
        for (String s : all) {
        	if (!active.contains(s)) {
        		result.add(s);
        	}
        }
    	return result;
    }
    
    /**
     * @see org.eclipse.jface.preference.ListEditor#getNewInputObject()
     */
    protected String getNewInputObject() {
    	// fetch each time in case list/directory has changed
    	Set<String> allMacros =  getAllMacros();
		MacroDialog dialog = new MacroDialog(getShell());
		dialog.setTitle(chooserLabel);
		dialog.setMacros(getInactive(allMacros));
		// modal dialog
		dialog.open();
		// modal dialog return
		active.retainAll(allMacros);	// check that all actives are still valid
		activeChangeCheck();	// add (any) new ones to the widget 
		return null;
	}

    @Override
	protected void doLoad() {
		super.doLoad();
		selectionChanged();
	}

    @Override
	protected void selectionChanged() {
		checkActive();
		setPresentsDefaultValue((active != null) ? checkDefaults(active) : true);
		super.selectionChanged();
	}
    
    
    // default macro set is the empty list
    private boolean checkDefaults(ArrayList<String> compareMacs) {
    	return !(compareMacs.size() > 0);
	}
    
    /**
     * If the widget set is different from active set, then update the active set
     */
    private void checkActive() {
    	List myList = getListUnchecked();
    	if (myList != null) {
    		String[] items = myList.getItems();
    		if (active.size() != items.length && !active.equals(Arrays.asList(items))) {
    			active.clear();
    			for (String item : items) {
    				active.add(item);
    			}
    		}
    	}    	
    }
    
    /**
     * If the active set is different from widget set, then update the widget set
     */
    private void activeChangeCheck() {
    	List myList = getListUnchecked();
    	if (myList != null) {
    		String[] items = myList.getItems();
    		if (active.size() != items.length && !active.equals(Arrays.asList(items))) {
    			myList.removeAll();
    			for (String item : active) {
    				myList.add(item);
    			}
    			setPresentsDefaultValue(checkDefaults(active));
    			super.selectionChanged();
    		}
    	}    	
    }
    
    String getLabel(String str) {
    	return str;
    }

    private ArrayList<String> convertToMacros(String idList, Set<String> allMacros){
    	ArrayList<String> result = new ArrayList<String>();
    	StringTokenizer st = new StringTokenizer(idList, SEPARATOR);
    	while (st.hasMoreElements()) {
    		String next = ((String) st.nextElement()).trim(); 
    		if (allMacros.contains(next)) {
    			result.add(next);        			
    		}
    	}
    	return result;
    }
    
    /**
     * Turn the comma separated string into an array of macro names
     * 
     * @param macroNameList
     * @return array of macro names
     */
    public static String[] parseResults(String macroNameList){
        StringTokenizer st = new StringTokenizer(macroNameList, SEPARATOR);
        ArrayList<String> macs = new ArrayList<String>();
        while (st.hasMoreElements()) {
        	String next = ((String) st.nextElement()).trim(); 
        	if (next.length() > 0) {
        		macs.add(next);
        	}
        }
        return macs.toArray(new String[0]);    	
    }
    
    /**
     * Receive the results of the add dialog
     * 
     * @param addMacros
     */
    void setOkMacros(ArrayList<String> addMacros) {
    	active.addAll(addMacros);    	
    }
    
    private Set<String> getAllMacros() {
    	return KbdMacroSupport.getFileMap(directoryEditor.getStringValue()).keySet();    	
    }
    

    private String[] sortMacros(ArrayList<String> macs) {
    	String[] ordered = macs.toArray(new String[0]);    		
    	Arrays.sort(ordered);
    	return ordered;
    }

    /**
     * A Kbd Macro chooser dialog for adding inactive macros to the active list
     * 
     * @author Mark Feber - initial API and implementation
     */
    private class MacroDialog extends StatusDialog {

    	List macList;
    	
    	// Array of macros from which to choose
    	ArrayList<String> macroArray = null;
    	
    	public MacroDialog(Shell parent) {
			super(parent);
		}
    	
    	/** 
    	 * Return the selected macros on ok exit
    	 *
    	 * @see org.eclipse.jface.dialogs.Dialog#okPressed()
    	 */
    	protected void okPressed() {
    		setOkMacros(this.getNewMacros());
    		super.okPressed();
    	}

		/**
		 * @see org.eclipse.jface.dialogs.Dialog#isResizable()
		 */
		protected boolean isResizable() {
			return false;
		}

		/**
		 * @see Dialog#createDialogArea(Composite)
		 */
		protected Control createDialogArea(Composite parent) {
			Composite result = new Composite(parent, SWT.NONE);
			result.setLayout(new GridLayout());
			result.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
			macList = new List(result, SWT.BORDER | SWT.MULTI );
			macList.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
			if (macroArray != null){
				for (int i=0; i< macroArray.size(); i++){
					macList.add(getLabel(macroArray.get(i)));
				}
				macList.pack(true);
				macList.computeSize(SWT.DEFAULT, SWT.DEFAULT);
				macList.setVisible(true);
			} 
			result.setVisible(true);
			macList.addDisposeListener(new DisposeListener() {
				public void widgetDisposed(DisposeEvent e) {
					macList = null;
				}
			});
			return result;
		}

    	/**
    	 * Initialize our macroArray with the macros
    	 * 
    	 * @param macros
    	 */
    	void setMacros(ArrayList<String> macros){
    		String[] ordered = sortMacros(macros);
    		macroArray = new ArrayList<String>();
    		for (int i=0; i < ordered.length; i++){
    			macroArray.add(ordered[i]);
    		}
    	}
    	
		/**
		 * Get all the selected macros from the add dialog
		 * @return
		 */
    	ArrayList<String> getNewMacros() {
			int[] selection = macList.getSelectionIndices();
			ArrayList<String> macros = new ArrayList<String>();
			for (int i = 0; i < selection.length; i++) {
				macros.add(macroArray.get(selection[i]));	
			}
			return macros;
		}
	}
}
