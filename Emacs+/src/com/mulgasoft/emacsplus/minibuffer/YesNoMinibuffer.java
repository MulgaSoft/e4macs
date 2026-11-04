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

import java.util.List;

import org.eclipse.ui.texteditor.ITextEditor;

/**
 * Read yes/no from the minibuffer
 * 
 * Minibuffer result is Boolean true if yes, false if no, null if neither
 * 
 * @author Mark Feber - initial API and implementation
 */
public class YesNoMinibuffer extends StrictMinibuffer {

	/**
	 * @param executable
	 */
	public YesNoMinibuffer(IMinibufferExecutable executable) {
		super(executable);
		setCandidates(List.of(YESORNO_YES,YESORNO_NO));
	}
	
	/**
	 * @param executable
	 */
	public YesNoMinibuffer(IMinibufferExecutable executable, boolean immediately) {
		this(executable);
		setImmediate(immediately);
		setCandidates(List.of(YESORNO_Y,YESORNO_N));
	}
	
	/**
	 * @see com.mulgasoft.emacsplus.minibuffer.ExecutingMinibuffer#executeResult(org.eclipse.ui.texteditor.ITextEditor, java.lang.Object)
	 */
	protected boolean executeResult(ITextEditor editor, Object commandResult) {
		return super.executeResult(editor, returnResult((String)commandResult));
	}

	/**
	 * Force result to either initial character or full string depending on minibuffer state
	 * Our handler wants a Boolean result
	 * 
	 * @param yorn
	 * @return true if yes, false if no, or null 
	 */
	private Boolean returnResult(String yorn) {
		Boolean result = null;
		if (yorn != null && yorn.length() > 0) {
			if ((YESORNO_Y.equalsIgnoreCase(yorn)) || (YESORNO_YES.equalsIgnoreCase(yorn))) {
				result = true;
			} else if ((YESORNO_N.equalsIgnoreCase(yorn)) || (YESORNO_NO.equalsIgnoreCase(yorn))) {
				result = false;
			}
		}
		return result;
	}
}
