/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
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

import org.eclipse.jface.preference.FieldEditor;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Widget;

/**
 * Hack org.eclipse.jface.preference.ListEditor so it does what we want
 * since eclipse makes it hard to make reasonable changes through sub-classing 
 * @author Mark Feber - initial API and implementation
 */

/**
 * An abstract field editor that manages a list of input values. 
 * The editor displays a list containing the values, buttons for
 * adding and removing values and Restoring defaults
 * <p>
 * Subclasses must implement the <code>parseString</code>,
 * <code>createList</code>, and <code>getNewInputObject</code>
 * framework methods.
 * </p>
 */
public abstract class MListEditor extends FieldEditor {

    /**
     * The list widget; <code>null</code> if none
     * (before creation or after disposal).
     */
    private List list;

    /**
     * The button box containing the Add, Remove, Up, and Down buttons;
     * <code>null</code> if none (before creation or after disposal).
     */
    private Composite buttonBox;

    /**
     * The Add button.
     */
    private Button addButton;

    /**
     * The Remove button.
     */
    private Button removeButton;
    /**
     * The Defaults button.
     */
    private Button defaultsButton;

    /**
     * The selection listener.
     */
    private SelectionListener selectionListener;

    /**
     * Creates a new list field editor 
     */
    protected MListEditor() {
    }

    /**
     * Creates a list field editor.
     * 
     * @param name the name of the preference this field editor works on
     * @param labelText the label text of the field editor
     * @param parent the parent of the field editor's control
     */
    protected MListEditor(String name, String labelText, Composite parent) {
        init(name, labelText);
        createControl(parent);
    }

    /**
     * Combines the given list of items into a single string.
     * This method is the converse of <code>parseString</code>. 
     * <p>
     * Subclasses must implement this method.
     * </p>
     *
     * @param items the list of items
     * @return the combined string
     * @see #parseString
     */
    protected abstract String createList(String[] items);
    
    /**
     * Creates and returns a new item for the list.
     * <p>
     * Subclasses must implement this method.
     * </p>
     *
     * @return a new item
     */
    protected abstract String getNewInputObject();
    
    /**
     * Splits the given string into a list of strings.
     * This method is the converse of <code>createList</code>. 
     * <p>
     * Subclasses must implement this method.
     * </p>
     *
     * @param stringList the string
     * @return an array of <code>String</code>
     * @see #createList
     */
    protected abstract String[] parseString(String stringList);
    
    /**
     * Notifies that the Add button has been pressed.
     */
    private void addPressed() {

        String input = getNewInputObject();

        if (input != null) {
            int index = list.getSelectionIndex();
            if (index >= 0) {
				list.add(input, index + 1);
			} else {
				list.add(input, 0);
			}
            selectionChanged();
        }
    }

    /**
     * Method declared on FieldEditor.
     */
    protected void adjustForNumColumns(int numColumns) {
        Control control = getLabelControl();
        ((GridData) control.getLayoutData()).horizontalSpan = numColumns;
        ((GridData) list.getLayoutData()).horizontalSpan = numColumns - 1;
    }

    /**
     * Creates the Add, Remove, Up, and Down button in the given button box.
     *
     * @param box the box for the buttons
     */
    private void createButtons(Composite box) {
        addButton = createPushButton(box, "ListEditor.add");//$NON-NLS-1$
        removeButton = createPushButton(box, "ListEditor.remove");//$NON-NLS-1$
        defaultsButton = createPushButton(box, "defaults");//$NON-NLS-1$
    }

    

    /**
     * Helper method to create a push button.
     * 
     * @param parent the parent control
     * @param key the resource name used to supply the button's label text
     * @return Button
     */
    private Button createPushButton(Composite parent, String key) {
        Button button = new Button(parent, SWT.PUSH);
        String buttonText = JFaceResources.getString(key);
        button.setText(buttonText);
        button.setFont(parent.getFont());
        GridData data = new GridData(GridData.FILL_HORIZONTAL);
        int widthHint = convertHorizontalDLUsToPixels(button,
                IDialogConstants.BUTTON_WIDTH);
        data.widthHint = Math.max(widthHint, button.computeSize(SWT.DEFAULT,
                SWT.DEFAULT, true).x);
        button.setLayoutData(data);
        button.addSelectionListener(getSelectionListener());
        return button;
    }

    /**
     * Creates a selection listener.
     */
    public void createSelectionListener() {
        selectionListener = new SelectionAdapter() {
            public void widgetSelected(SelectionEvent event) {
                Widget widget = event.widget;
                if (widget == addButton) {
                    addPressed();
                } else if (widget == removeButton) {
                    removePressed();
                }else if (widget == defaultsButton) {
                    defaultsPressed();
                } else if (widget == list) {
                    selectionChanged();
                }
            }
        };
    }

    /**
     * Method declared on FieldEditor.
     */
    protected void doFillIntoGrid(Composite parent, int numColumns) {
        Control control = getLabelControl(parent);
        GridData gd = new GridData();
        gd.horizontalSpan = numColumns;
        control.setLayoutData(gd);

        list = getListControl(parent);
        gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.verticalAlignment = GridData.FILL;
        gd.horizontalSpan = numColumns - 1;
        gd.grabExcessHorizontalSpace = true;
        list.setLayoutData(gd);

        buttonBox = getButtonBoxControl(parent);
        gd = new GridData();
        gd.verticalAlignment = GridData.BEGINNING;
        buttonBox.setLayoutData(gd);
    }

    /**
     * Method declared on FieldEditor.
     */
    protected void doLoad() {
        if (list != null) {
            String s = getPreferenceStore().getString(getPreferenceName());
            String[] array = parseString(s);
            for (int i = 0; i < array.length; i++) {
                list.add(array[i]);
            }
        }
    }

    /**
     * Method declared on FieldEditor.
     */
    protected void doLoadDefault() {
        if (list != null) {
            list.removeAll();
            String s = getPreferenceStore().getDefaultString(getPreferenceName());
            String[] array = parseString(s);
            for (int i = 0; i < array.length; i++) {
                list.add(array[i]);
            }
        }
    }

    /**
     * Method declared on FieldEditor.
     */
    protected void doStore() {
        String s = createList(list.getItems());
        if (s != null) {
			getPreferenceStore().setValue(getPreferenceName(), s);
		}
    }

    /**
     * Returns this field editor's button box containing the Add, Remove,
     * Up, and Down button.
     *
     * @param parent the parent control
     * @return the button box
     */
    public Composite getButtonBoxControl(Composite parent) {
        if (buttonBox == null) {
            buttonBox = new Composite(parent, SWT.NULL);
            GridLayout layout = new GridLayout();
            layout.marginWidth = 0;
            buttonBox.setLayout(layout);
            createButtons(buttonBox);
            buttonBox.addDisposeListener(new DisposeListener() {
                public void widgetDisposed(DisposeEvent event) {
                    addButton = null;
                    removeButton = null;
                    defaultsButton = null;
                    buttonBox = null;
                }
            });

        } else {
            checkParent(buttonBox, parent);
        }
        changeEnabled();
        return buttonBox;
    }

    /**
     * Returns this field editor's list control.
     * Support multi-selection
     * 
     * @param parent the parent control
     * @return the list control
     */
    public List getListControl(Composite parent) {
        if (list == null) {
            list = new List(parent, SWT.BORDER | SWT.MULTI| SWT.V_SCROLL | SWT.H_SCROLL);
            list.setFont(parent.getFont());
            list.addSelectionListener(getSelectionListener());
            list.addDisposeListener(new DisposeListener() {
                public void widgetDisposed(DisposeEvent event) {
                    list = null;
                }
            });
        } else {
            checkParent(list, parent);
        }
        return list;
    }

    protected List getListUnchecked() {
    	return list;
    }

    /**
     * Method declared on FieldEditor.
     */
    public int getNumberOfControls() {
        return 2;
    }

    /**
     * Returns this field editor's selection listener.
     * The listener is created if necessary.
     *
     * @return the selection listener
     */
    private SelectionListener getSelectionListener() {
        if (selectionListener == null) {
			createSelectionListener();
		}
        return selectionListener;
    }

    /**
     * Returns this field editor's shell.
     * <p>
     * This method is internal to the framework; subclassers should not call
     * this method.
     * </p>
     *
     * @return the shell
     */
    protected Shell getShell() {
        if (addButton == null) {
			return null;
		}
        return addButton.getShell();
    }

    

    /**
     * Notifies that the Remove button has been pressed.
     */
    private void removePressed() {
        int[] selections = list.getSelectionIndices();
        if (selections.length > 0) {
			list.remove(selections);
            selectionChanged();
		}
    }

    protected void defaultsPressed() {
    	doLoadDefault();
    	selectionChanged();
    }
    /**
     * Notifies that the list selection has changed.
     */
    protected void selectionChanged() {
    	changeEnabled();
    }
    
    private void changeEnabled() {
    	removeButton.setEnabled(list.getSelectionIndex() >= 0);
        defaultsButton.setEnabled(!presentsDefaultValue());
    }
    
    /**
     * Method declared on FieldEditor.
     */
    public void setFocus() {
        if (list != null) {
            list.setFocus();
        }
    }

    /**
     * @see org.eclipse.jface.preference.FieldEditor#setEnabled(boolean, org.eclipse.swt.widgets.Composite)
     */
    public void setEnabled(boolean enabled, Composite parent) {
        super.setEnabled(enabled, parent);
        getListControl(parent).setEnabled(enabled);
        addButton.setEnabled(enabled);
        removeButton.setEnabled(enabled);
        defaultsButton.setEnabled(enabled);
    }
}
