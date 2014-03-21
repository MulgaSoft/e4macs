/**
 * Copyright (c) 2009, 2010 Mark Feber, MulgaSoft
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 */
package com.mulgasoft.emacsplus.commands;

import org.eclipse.ui.texteditor.ITextEditor;

import com.mulgasoft.emacsplus.TecoRegister;

/**
 * Implements - increment-register
 * 
 * Increment the number in *R* by <n> (defaults to 1)
 * 
 * @author Mark Feber - initial API and implementation
 */
public class RegisterIncrementHandler extends RegisterHandler implements INonEditingCommand {

	/**
	 * @see com.mulgasoft.emacsplus.minibuffer.IMinibufferExecutable#getMinibufferPrefix()
	 */
	public String getMinibufferPrefix() {
		return INCREMENT_PREFIX;
	}

	protected boolean needsSelection() {
		return false;
	}

	/**
	 * @see com.mulgasoft.emacsplus.minibuffer.IMinibufferExecutable#executeResult(org.eclipse.ui.texteditor.ITextEditor, java.lang.Object)
	 */
	public boolean doExecuteResult(ITextEditor editor, Object minibufferResult) {
		
		if (minibufferResult != null && ((String)minibufferResult).length() > 0) {
			String key = (String)minibufferResult;
			int incr = 1;
			if (getCallCount() > 1) {
				incr = getCallCount();
			} 
			Integer number = TecoRegister.getInstance().increment(key,incr);
			if (number != null) {
				showResultMessage(editor, String.format(REGISTER_NUMBER,key,number), false);
			} else {
				showResultMessage(editor, String.format(REGISTER_NO_NUMBER, key), true);
			}
		} else {
			showResultMessage(editor, NO_REGISTER, true);
		}
		return true;
	}
}
