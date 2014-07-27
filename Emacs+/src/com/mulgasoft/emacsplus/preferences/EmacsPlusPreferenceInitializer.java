/**
 * Copyright (c) 2009-2012 Mark Feber, MulgaSoft
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 */
package com.mulgasoft.emacsplus.preferences;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;

import com.mulgasoft.emacsplus.EmacsPlusActivator;
import com.mulgasoft.emacsplus.EmacsPlusUtils;
import com.mulgasoft.emacsplus.YankRotate;
import com.mulgasoft.emacsplus.commands.BlockHandler;
import com.mulgasoft.emacsplus.execute.KbdMacroSupport.LoadState;

import static com.mulgasoft.emacsplus.preferences.PrefVars.KILL_RING_MAX;
import static com.mulgasoft.emacsplus.preferences.PrefVars.KILL_WHOLE_LINE;
import static com.mulgasoft.emacsplus.preferences.PrefVars.ENABLE_GNU_SEXP;
import static com.mulgasoft.emacsplus.preferences.PrefVars.ENABLE_DOT_SEXP;
import static com.mulgasoft.emacsplus.preferences.PrefVars.ENABLE_UNDER_SEXP;
import static com.mulgasoft.emacsplus.preferences.PrefVars.REPLACE_TEXT_TO_KILLRING;
import static com.mulgasoft.emacsplus.preferences.PrefVars.DELETE_WORD_TO_CLIPBOARD;
import static com.mulgasoft.emacsplus.preferences.PrefVars.DELETE_SEXP_TO_CLIPBOARD;
import static com.mulgasoft.emacsplus.preferences.PrefVars.ENABLE_SPLIT_SELF;
import static com.mulgasoft.emacsplus.preferences.PrefVars.FRAME_INIT;
import static com.mulgasoft.emacsplus.preferences.PrefVars.FRAME_DEF;

/**
 * Class used to initialize default preference values.
 * 
 * @author Mark Feber - initial API and implementation
 */
public class EmacsPlusPreferenceInitializer extends AbstractPreferenceInitializer {
	
	// Edit,File,Navigate,Refactor,Run/Search,Source,Text Editing,Window,Kbd Macros
	private static final String defaultCategories = "org.eclipse.ui.category.edit," //$NON-NLS-1$  
	+ "org.eclipse.ui.category.file,"   											//$NON-NLS-1$  
	+ "org.eclipse.ui.category.navigate,"   										//$NON-NLS-1$  
	+ "org.eclipse.jdt.ui.category.refactoring,"									//$NON-NLS-1$ 
	+ "org.eclipse.debug.ui.category.run,"  										//$NON-NLS-1$
	+ "org.eclipse.search.ui.category.search,"  									//$NON-NLS-1$  
	+ "org.eclipse.jdt.ui.category.source," 										//$NON-NLS-1$  
	+ "org.eclipse.ui.category.textEditor," 										//$NON-NLS-1$  
	+ "org.eclipse.ui.category.window," 											//$NON-NLS-1$  
    + "emacsplus.keyboard.macros";  												//$NON-NLS-1$  
	
	private static final String defaultBrowseHighlight = "237,237,252";	//$NON-NLS-1$ 
	
	/**
	 * @see org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer#initializeDefaultPreferences()
	 */
	public void initializeDefaultPreferences() {
		IPreferenceStore store = EmacsPlusActivator.getDefault().getPreferenceStore();
		store.setDefault(EmacsPlusPreferenceConstants.P_ROTATE_DIR, YankRotate.BACKWARD.id());
		store.setDefault(EmacsPlusPreferenceConstants.P_GNU_YANK,true);
		store.setDefault(EmacsPlusPreferenceConstants.P_EMACS_UNDO,true);
		store.setDefault(EmacsPlusPreferenceConstants.P_AUTO_BROWSE_KR,false);
		store.setDefault(EmacsPlusPreferenceConstants.P_AUTO_BROWSE_HIGHLIGHT,defaultBrowseHighlight);
		store.setDefault(EmacsPlusPreferenceConstants.P_BLOCK_MOVE_SIZE, Integer.toString(BlockHandler.BLOCK_SIZE));
		store.setDefault(EmacsPlusPreferenceConstants.P_COMMAND_CATEGORIES, defaultCategories);	
		store.setDefault(EmacsPlusPreferenceConstants.P_CTRL_DIGIT_ARGUMENT,false);
		store.setDefault(EmacsPlusPreferenceConstants.P_DISABLE_INLINE_EDIT,false);
		store.setDefault(EmacsPlusPreferenceConstants.P_KBD_MACRO_DIRECTORY,EmacsPlusUtils.EMPTY_STR);
		store.setDefault(EmacsPlusPreferenceConstants.P_KBD_MACRO_NAME_LOAD,EmacsPlusUtils.EMPTY_STR);
		store.setDefault(EmacsPlusPreferenceConstants.P_KBD_MACRO_AUTO_LOAD,LoadState.NONE.toString());
		// new definition style
		store.setDefault(ENABLE_SPLIT_SELF.getPref(),(Boolean)ENABLE_SPLIT_SELF.getDefault());
		store.setDefault(FRAME_INIT.getPref(),(String)FRAME_INIT.getDefault());
		store.setDefault(FRAME_DEF.getPref(),(String)FRAME_DEF.getDefault());
		store.setDefault(ENABLE_SPLIT_SELF.getPref(),(Boolean)ENABLE_SPLIT_SELF.getDefault());
		store.setDefault(DELETE_WORD_TO_CLIPBOARD.getPref(),(Boolean)DELETE_WORD_TO_CLIPBOARD.getDefault());
		store.setDefault(DELETE_SEXP_TO_CLIPBOARD.getPref(),(Boolean)DELETE_SEXP_TO_CLIPBOARD.getDefault());
		store.setDefault(KILL_RING_MAX.getPref(), Integer.toString((Integer)KILL_RING_MAX.getDefault()));
		store.setDefault(REPLACE_TEXT_TO_KILLRING.getPref(),(Boolean)REPLACE_TEXT_TO_KILLRING.getDefault());
		store.setDefault(ENABLE_GNU_SEXP.getPref(),(Boolean)ENABLE_GNU_SEXP.getDefault());
		store.setDefault(ENABLE_DOT_SEXP.getPref(),(Boolean)ENABLE_DOT_SEXP.getDefault());
		store.setDefault(ENABLE_UNDER_SEXP.getPref(),(Boolean)ENABLE_UNDER_SEXP.getDefault());
		// preferences that are only set by toggle commands and have no preference UI manifestation
		store.setDefault(KILL_WHOLE_LINE.getPref(),(Boolean)KILL_WHOLE_LINE.getDefault());
		}
}