/**
 * Copyright (c) 2009, 2010 Mark Feber, MulgaSoft
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package com.mulgasoft.emacsplus.preferences;

import org.eclipse.jface.preference.FieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import com.mulgasoft.emacsplus.EmacsPlusUtils;

/**
 * Abstract base class for Emacs+ preference pages
 * 
 * @author Mark Feber - initial API and implementation
 */
public abstract class EmacsPlusPreferenceBase extends FieldEditorPreferencePage	implements IWorkbenchPreferencePage{

	public EmacsPlusPreferenceBase(int style) {
		super(style);
	}
	
	/**
	 * @see org.eclipse.ui.IWorkbenchPreferencePage#init(org.eclipse.ui.IWorkbench)
	 */
	public void init(IWorkbench workbench) {
	}
	
	/**
     * Creates a tab of one horizontal spans.
     * 
     * @param parent
     *            the parent in which the tab should be created
     */
	void addSpace() {
        addField(new SpacerFieldEditor(getFieldEditorParent()));
    }
    

	public class SpacerFieldEditor extends LabelFieldEditor {
		// Implemented as an empty label field editor.
		public SpacerFieldEditor(Composite parent) {
			super(EmacsPlusUtils.EMPTY_STR, parent);	//$NON-NLS-1$
		}
	}
	
	/**
	 * A field editor for displaying labels not associated with other widgets.
	 */
	class LabelFieldEditor extends FieldEditor {

		private Label label;

		// All labels can use the same preference name since they don't
		// store any preference.
		public LabelFieldEditor(String value, Composite parent) {
			super("label", value, parent);	//$NON-NLS-1$
		}

		// Adjusts the field editor to be displayed correctly
		// for the given number of columns.
		protected void adjustForNumColumns(int numColumns) {
			((GridData) label.getLayoutData()).horizontalSpan = numColumns;
		}

		// Fills the field editor's controls into the given parent.
		protected void doFillIntoGrid(Composite parent, int numColumns) {
			label = getLabelControl(parent);

			GridData gridData = new GridData();
			gridData.horizontalSpan = numColumns;
			gridData.horizontalAlignment = GridData.FILL;
			gridData.grabExcessHorizontalSpace = false;
			gridData.verticalAlignment = GridData.CENTER;
			gridData.grabExcessVerticalSpace = false;

			label.setLayoutData(gridData);
		}

		// Returns the number of controls in the field editor.
		public int getNumberOfControls() {
			return 1;
		}

		// Labels do not persist any preferences, so these methods are empty.
		protected void doLoad() {
		}

		protected void doLoadDefault() {
		}

		protected void doStore() {
		}
	}
	
	class RectangleFieldEditor extends StringFieldEditor {

		public RectangleFieldEditor(String name, String labelText, int width, int strategy, Composite parent) {
			super(name, labelText, width, strategy, parent);
		}

		@Override
		protected boolean doCheckState() {
			return super.doCheckState() && PrefVars.PRect.parseRect(getStringValue()) != null;
		}

	}

}
