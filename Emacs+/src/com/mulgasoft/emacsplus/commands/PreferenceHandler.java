/**
 * Copyright (c) 2009-2011 Mark Feber, MulgaSoft
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package com.mulgasoft.emacsplus.commands;

import org.eclipse.ui.texteditor.ITextEditor;

import com.mulgasoft.emacsplus.EmacsPlusActivator;

/**
 * Just a place to collect commands that operate on preferences
 * 
 * @author Mark Feber - initial API and implementation
 */
public abstract class PreferenceHandler extends MinibufferHandler {
	
	protected static String getResourceString(String key) {
		return EmacsPlusActivator.getResourceString(key);
	}

	protected boolean isShowPreference(ITextEditor editor, boolean val, String key) {
		boolean result = isUniversalPresent();
		if (result) {
			showPreference(editor,val,key);			
		}
		return result;
	}

	protected void showPreference(ITextEditor editor, Object val, String key) {
		asyncShowMessage(editor,String.format(getResourceString(key),val),false);					
	}
}
