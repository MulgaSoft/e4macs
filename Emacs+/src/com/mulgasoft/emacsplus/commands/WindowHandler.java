/**
 * Copyright (c) 2009, 2014 Mark Feber, MulgaSoft
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 */
package com.mulgasoft.emacsplus.commands;

import static com.mulgasoft.emacsplus.EmacsPlusUtils.getPreferenceStore;
import static com.mulgasoft.emacsplus.preferences.PrefVars.ENABLE_SPLIT_SELF;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.ui.texteditor.ITextEditor;

import com.mulgasoft.emacsplus.EmacsPlusUtils;

/**
 * Go behind enemy lines to implement window behavior
 * 
 * The split window/stack code is in part based on information gleaned from: 
 *  - https://bugs.eclipse.org/bugs/show_bug.cgi?id=95357 
 * 
 * @author Mark Feber - initial API and implementation
 */

// TODO: When the top left editor has been squashed by surrounding editors so that it has no (or little) display area 
// beyond its tab, then the merge fails, and acts as if SWT.TOP had been specified.  This is because the eclipse drag
// mechanism is position based, rather than container based.

// TODO: There is a bug in eclipse which provokes the initialization of all editors (rather than just the active editors)
// under certain circumstances (yet to be determined).  It can be provoked by moving the sash with lots of editors to a 
// different sash

public abstract class WindowHandler extends EmacsPlusNoEditHandler {
	// TODO Leave stub for Luna (?) implementation
	protected final static String COMPLAIN_E4 = com.mulgasoft.emacsplus.EmacsPlusActivator.getResourceString("E4IsBroken");	//$NON-NLS-1$
	
	protected int transform(ITextEditor editor, IDocument document, ITextSelection currentSelection,
			ExecutionEvent event) throws BadLocationException {
		com.mulgasoft.emacsplus.EmacsPlusUtils.showMessage(editor, COMPLAIN_E4, true);		
		return NO_OFFSET;
	}
	
	// split editor in two when true, else just rearrange editors in stack
	private static boolean splitSelf = EmacsPlusUtils.getPreferenceBoolean(ENABLE_SPLIT_SELF.getPref());
	
	static {
		// listen for changes in the property store
		getPreferenceStore().addPropertyChangeListener(
				new IPropertyChangeListener() {
					public void propertyChange(PropertyChangeEvent event) {
						if (ENABLE_SPLIT_SELF.getPref().equals(event.getProperty())) {
							WindowHandler.setSplitSelf((Boolean)event.getNewValue());
						}
					}
				}
		);
	}
	
	/**
	 * @param splitSelf the splitSelf to set
	 */
	public static void setSplitSelf(boolean splitSelf) {
		WindowHandler.splitSelf = splitSelf;
	}

	/**
	 * @return the splitSelf
	 */
	public static boolean isSplitSelf() {
		return splitSelf;
	}

}
