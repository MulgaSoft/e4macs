/**
 * Copyright (c) 2009, 2010 Mark Feber, MulgaSoft
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 */
package com.mulgasoft.emacsplus.preferences;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.StringTokenizer;

import org.eclipse.core.commands.Category;
import org.eclipse.core.commands.common.NotDefinedException;
import org.eclipse.jface.dialogs.StatusDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.commands.ICommandService;

//import org.eclipse.ui.dialogs.FilteredList;
// TODO: Change this & MListEditor to use FilteredList
/**
 * @author Mark Feber - initial API and implementation
 */
public class CommandCategoryEditor extends EMPListEditor {
	
    ArrayList<Category> active = new ArrayList<Category>();
    // The array of categories currently NOT set in this dialog
    ArrayList<Category> inactive = new ArrayList<Category>();
    // When the chooser add dialogs, this holds the new ones
    ArrayList<Category> newOnes = null;

    /**
     * sub dialog title
     */
    private String chooserLabel;

	/**
	 * @param name
	 * @param labelText
	 * @param chooserLabel
	 * @param parent
	 */
	CommandCategoryEditor(String name, String labelText, String chooserLabel, Composite parent) {
        super(name,labelText,parent);
        this.chooserLabel = chooserLabel;
    }

	
    @Override
	protected void doLoad() {
		super.doLoad();
		selectionChanged();
	}

	/**
     * @see org.eclipse.jface.preference.ListEditor#getNewInputObject()
     */
    protected String getNewInputObject() {

    	List myList = getListUnchecked();
    	
		CategoryDialog dialog = new CategoryDialog(getShell());
		dialog.setTitle(chooserLabel);
		dialog.setCategories(inactive);
		dialog.open();
		if (newOnes != null && myList != null) {
			for (int i = 0; i< newOnes.size(); i++) {
				myList.add(getLabel(newOnes.get(i)));
			}
			active.addAll(newOnes);
			inactive.removeAll(newOnes);
			newOnes = null;
			selectionChanged();
		}
		return null;
	}
    
    @Override
	protected void selectionChanged() {
		checkCats();
		setPresentsDefaultValue((active != null) ? checkDefaults(active) : false);
		super.selectionChanged();
	}
    
    private Category[] defaultCats;
    private boolean checkDefaults(ArrayList<Category> compareCats) {

		boolean result = true;
		Category[] newArray = sortCats(compareCats);
		if (defaultCats == null) {
			defaultCats = sortCats(convertToCats(getPreferenceStore().getDefaultString(getPreferenceName()),
					getAllCategories()));
		}
		if (newArray.length == defaultCats.length) {
			for (int i = 0; i < newArray.length; i++) {
				if (newArray[i] != defaultCats[i]) {
					result = false;
					break;
				}
			}
		} else {
			result = false;
		}
		return result;
	}
    
	// TODO: Make our own version with FilteredList, so we can dump this crap
    // hack so we can use the list control as is
    private void checkCats(){
    	List myList = this.getListUnchecked();
    	if (myList != null) {
			String[] items = myList.getItems();
			if (items.length != active.size()) {
				// a remove happened 
				ArrayList<Category> moveCats = new ArrayList<Category>();
				String[] catLabels = new String[active.size()];
				for (int i = 0; i < active.size(); i++) {
					catLabels[i] = getLabel(active.get(i));
				}
				for (int i = 0; i < catLabels.length; i++) {
					boolean ok = false;
					for (int j = 0; j < items.length; j++) {
						if (items[j].equals(catLabels[i])) {
							ok = true;
							break;
						}
					}
					if (!ok) {
						moveCats.add(active.get(i));
					}
				}
				if (!moveCats.isEmpty()) {
					active.removeAll(moveCats);
					inactive.addAll(moveCats);
				}
			}
		}
    }
    
    /**
     * Cook up a label that is, hopefully, distinct and useful
     * @param cat
     * @return
     */
    String getLabel(Category cat) {
    	String result = null;
    	try {
    		String desc = cat.getDescription();
			result= cat.getName() + DISPLAY_SEPARATOR + (desc != null ? desc : "");	//$NON-NLS-1$
		} catch (NotDefinedException e) {}
		return result;
    }
    
    /**
     * @see org.eclipse.jface.preference.ListEditor#createList(java.lang.String[])
     */
    protected String createList(String[] items) {

    	Category[] orderedCats = sortCats(active);
        StringBuilder result = new StringBuilder("");//$NON-NLS-1$

        for (int i = 0; i < orderedCats.length; i++) {
            result.append(orderedCats[i].getId());
            result.append(SEPARATOR);
        }
        return result.toString();
    }

    /**
     * @param catIdList is a 'list' of category ids
     *  
     * @see org.eclipse.jface.preference.ListEditor#parseString(java.lang.String)
     */
    protected String[] parseString(String catIdList) {
    	
        Category[] allCats = this.getAllCategories();
        active = convertToCats(catIdList,allCats);
        inactive = new ArrayList<Category>();
        for (int i=0; i< allCats.length; i++){
        	if (!active.contains(allCats[i])){
        		inactive.add(allCats[i]);
        	}
        }
        String[] result = new String[active.size()];
        for (int i=0; i< result.length; i++){
        	result[i] = getLabel(active.get(i));
        }
        return result;
    }
    
    private ArrayList<Category> convertToCats(String idList, Category[] allCats){
    	ArrayList<Category> result;
        StringTokenizer st = new StringTokenizer(idList, SEPARATOR);
        result = new ArrayList<Category>();
        while (st.hasMoreElements()) {
        	String next = ((String) st.nextElement()).trim(); 
        	for (int i=0; i< allCats.length; i++){
        		if (allCats[i].getId().equals(next)){
        			result.add(allCats[i]);        			
        			break;
        		}
        	}
        }
        return result;
    }
    
    /**
     * Receive the results of the add dialog
     * 
     * @param cats
     */
    void setOkCategories(ArrayList<Category> cats) {
    	newOnes = cats;
    }
    
    Category[] sortCats(java.util.List<Category> sCats) {
		Category[] sCatsArray = sCats.toArray(new Category[0]);
		Arrays.sort(sCatsArray, new Comparator<Category>() {
			public int compare(Category o1, Category o2) {
				int result = 0;
				try {
					result = o1.getName().compareTo(o2.getName());
				} catch (NotDefinedException e) {}
				return result;
			}
		});
		return sCatsArray;
	}
    
    /**
     * Get all the currently defined categories
     * @return
     */
    private Category[] getAllCategories() {
    	return ((ICommandService) PlatformUI.getWorkbench().getService(ICommandService.class)).getDefinedCategories();
    }
    
    /**
     * A Category chooser dialog for adding unused categories to the category list
     * used to generate the executable command list
     * 
     * @author Mark Feber - initial API and implementation
     */
    private class CategoryDialog extends StatusDialog {

    	List catList;
    	// Array of categories from which to choose
    	ArrayList<Category> categoryArray = null;
    	
    	public CategoryDialog(Shell parent) {
			super(parent);
		}
    	
    	/** 
    	 * Return the selected categories on ok exit
    	 *
    	 * @see org.eclipse.jface.dialogs.Dialog#okPressed()
    	 */
    	protected void okPressed() {
    		setOkCategories(this.getNewCategories());
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
			catList = new List(result, SWT.BORDER | SWT.MULTI );
			catList.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
			if (categoryArray != null){
				for (int i=0; i< categoryArray.size(); i++){
					catList.add(getLabel(categoryArray.get(i)));
				}
				catList.pack(true);
				catList.computeSize(SWT.DEFAULT, SWT.DEFAULT);
				catList.setVisible(true);
			} 
			result.setVisible(true);
			catList.addDisposeListener(new DisposeListener() {
				public void widgetDisposed(DisposeEvent e) {
					catList = null;
				}
			});
			return result;
		}

    	/**
    	 * Initialize our categories with cats
    	 * 
    	 * @param cats
    	 */
    	void setCategories(ArrayList<Category> cats){
    		
    		Category[] orderedCats = sortCats(cats);
    		categoryArray = new ArrayList<Category>();
    		for (int i=0; i < orderedCats.length; i++){
    			categoryArray.add((Category)orderedCats[i]);
    		}
    	}
    	
		/**
		 * Get all the selected categories from the add dialog
		 * @return
		 */
    	ArrayList<Category> getNewCategories() {
			int[] selection = catList.getSelectionIndices();
			ArrayList<Category> cats = new ArrayList<Category>();
			for (int i = 0; i < selection.length; i++) {
				cats.add(categoryArray.get(selection[i]));	
			}
			return cats;
		}
	}
}
