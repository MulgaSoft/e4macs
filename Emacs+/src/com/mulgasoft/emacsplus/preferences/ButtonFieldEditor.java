/**
 * Copyright (c) 2009, 2010 Mark Feber, MulgaSoft
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package com.mulgasoft.emacsplus.preferences;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.preference.FieldEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;

/**
 * A stand-alone button for inclusion in a preference page
 * 
 * @author Mark Feber - initial API and implementation
 */
public class ButtonFieldEditor extends FieldEditor {

	Composite top = null;
	private Button button;
	
	/**
	 * Construct a stand-alone button for inclusion in the preference page
	 *  
	 * @param parent the composite parent
	 * @param buttonText displayed by the button
	 * @param onPush the button behavior on push: a SelectionAdapter
	 * new SelectionAdapter() {
	 *     public void widgetSelected(SelectionEvent evt) {
	 *     	// button behavior
	 *     }
	 * }
	 */
	public ButtonFieldEditor(Composite parent, String buttonText, SelectionAdapter onPush) {
		super("noop",buttonText,parent);	//$NON-NLS-1$
		button.addSelectionListener(onPush);
	}
	
    protected Button getButtonControl(Composite parent) {
        if (button == null) {
            button = new Button(parent, SWT.PUSH);
            button.setText(getLabelText());
            button.setFont(parent.getFont());
            button.addDisposeListener(new DisposeListener() {
                public void widgetDisposed(DisposeEvent event) {
                    button = null;
                }
            });
        } else {
            checkParent(button, parent);
        }
        return button;
    }

    public void setEnabled(boolean enabled, Composite parent) {
        getButtonControl(parent).setEnabled(enabled);
    }

    /**
	 * @see org.eclipse.jface.preference.FieldEditor#adjustForNumColumns(int)
	 */
	@Override
	protected void adjustForNumColumns(int numColumns) {
		((GridData)top.getLayoutData()).horizontalSpan = numColumns;		
	}

	/**
	 * @see org.eclipse.jface.preference.FieldEditor#doFillIntoGrid(org.eclipse.swt.widgets.Composite, int)
	 */
	@Override
	protected void doFillIntoGrid(Composite parent, int numColumns) {
		top = parent;
        GridData gd = new GridData();
        gd.horizontalSpan = numColumns;
        gd.horizontalAlignment = GridData.FILL;
        gd.horizontalAlignment = GridData.END;
        gd.grabExcessHorizontalSpace = true;        
        
        button = getButtonControl(parent);
        
        int widthHint = convertHorizontalDLUsToPixels(button, IDialogConstants.BUTTON_WIDTH);
        gd.widthHint = Math.max(widthHint, button.computeSize(SWT.DEFAULT, SWT.DEFAULT, true).x);
        button.setLayoutData(gd);
	}

	/**
	 * @see org.eclipse.jface.preference.FieldEditor#doLoad()
	 */
	@Override
	protected void doLoad() {
		// nope
	}

	/**
	 * @see org.eclipse.jface.preference.FieldEditor#doLoadDefault()
	 */
	@Override
	protected void doLoadDefault() {
		// nope
	}

	/**
	 * @see org.eclipse.jface.preference.FieldEditor#doStore()
	 */
	@Override
	protected void doStore() {
		// nope
	}

	/**
	 * @see org.eclipse.jface.preference.FieldEditor#getNumberOfControls()
	 */
	@Override
	public int getNumberOfControls() {
		return 1;
	}

}
