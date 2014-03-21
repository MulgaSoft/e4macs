/**
 * Copyright (c) 2009, 2010 Mark Feber, MulgaSoft
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package com.mulgasoft.emacsplus.preferences;

import com.mulgasoft.emacsplus.EmacsPlusActivator;
import com.mulgasoft.emacsplus.execute.CommandSupport;

/**
 * @author Mark Feber - initial API and implementation
 *
 */
public class CategoriesPreferencePage extends EmacsPlusPreferenceBase {

	/**
	 * @param style
	 */
	public CategoriesPreferencePage() {
		super(FLAT);
		setPreferenceStore(EmacsPlusActivator.getDefault().getPreferenceStore());		
	}

	/**
	 * @see org.eclipse.jface.preference.FieldEditorPreferencePage#createFieldEditors()
	 */
	@Override
	protected void createFieldEditors() {
		addSpace();
		addField(
				new LabelFieldEditor(
						EmacsPlusActivator.getString("EmacsPlusPref_CmdCategoriesDesc"),
						getFieldEditorParent()));
		addSpace();
		addField(
				new CommandCategoryEditor(
						EmacsPlusPreferenceConstants.P_COMMAND_CATEGORIES,
						EmacsPlusActivator.getString("EmacsPlusPref_CmdCategories"),								   //$NON-NLS-1$
						EmacsPlusActivator.getString("EmacsPlusPref_AddCategoryTitle"),  							   //$NON-NLS-1$
						getFieldEditorParent()));
	}
	
	public boolean performOk() {
		boolean result = super.performOk();
		if (result) {
			String newCategories = getPreferenceStore().getString(EmacsPlusPreferenceConstants.P_COMMAND_CATEGORIES);
			CommandSupport.setCategories(CommandCategoryEditor.parseResults(newCategories));
		}
		return result;
	}

}
