/**
 * Copyright (c) 2009-2020 Mark Feber, MulgaSoft
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package com.mulgasoft.emacsplus.minibuffer;

import java.util.Arrays;

import org.eclipse.ui.texteditor.ITextEditor;

/**
 * @author Mark Feber - initial API and implementation
 */
public class BooleanMinibuffer extends StrictMinibuffer {

	/**
	 * @param executable
	 */
	public BooleanMinibuffer(IMinibufferExecutable executable) {
		super(executable);
		setCandidates(Arrays.asList(Boolean.TRUE.toString(), Boolean.FALSE.toString()));
	}

	/**
	 * Convert string to EMPSetq object
	 * 
	 * @see com.mulgasoft.emacsplus.minibuffer.ExecutingMinibuffer#executeResult(org.eclipse.ui.texteditor.ITextEditor, java.lang.Object)
	 */
	@Override
	protected boolean executeResult(ITextEditor editor, Object commandResult) {
		return super.executeResult(editor,getBoolean((String)commandResult));
	}

	private Boolean getBoolean(String cmdResult) {
		Boolean result = null;
		String match = matchCandidate(cmdResult, true);
		if (match != null) {
			result = new Boolean(match);
		}
		return result;
	}
	
}
