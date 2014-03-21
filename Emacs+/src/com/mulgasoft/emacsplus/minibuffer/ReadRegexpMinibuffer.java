/**
 * Copyright (c) 2009, 2010 Mark Feber, MulgaSoft
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 */
package com.mulgasoft.emacsplus.minibuffer;

import org.eclipse.ui.texteditor.ITextEditor;

import com.mulgasoft.emacsplus.EmacsPlusActivator;

/**
 * Simple extension for use in counting matches
 * 
 * @author Mark Feber - initial API and implementation
 */
public class ReadRegexpMinibuffer extends ISearchMinibuffer {

	private final static String prefix = EmacsPlusActivator.getResourceString("Count_Match_Prefix");	//$NON-NLS-1$
	private IMinibufferExecutable executable;
	
	/**
	 * 
	 */
	public ReadRegexpMinibuffer(IMinibufferExecutable executable) {
		// forward, regexp
		super(true, true);
		this.executable = executable;
	}

	
	/**
	 * @see com.mulgasoft.emacsplus.minibuffer.ISearchMinibuffer#getMinibufferPrefix()
	 */
	@Override
	public String getMinibufferPrefix() {
		return prefix;
	}


	/**
	 * @see com.mulgasoft.emacsplus.minibuffer.SearchMinibuffer#executeResult(org.eclipse.ui.texteditor.ITextEditor, java.lang.Object)
	 */
	protected boolean executeResult(ITextEditor editor, Object commandResult) {
		String result = null;
		if (wasHistoryUpdated()) {
			findNext(getSearchString());
		}
		if (this.isFound()) {
			result = getSearchString(); 
		}
		return executable.executeResult(editor,result);
	}


	/**
	 * @see com.mulgasoft.emacsplus.minibuffer.ISearchMinibuffer#leave()
	 */
	@Override
	protected void leave() {
		// always go back to where we started
		leave(getStartOffset());
	}
}
