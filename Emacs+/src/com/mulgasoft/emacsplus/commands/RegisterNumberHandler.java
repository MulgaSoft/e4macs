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

import org.eclipse.jface.text.ITextSelection;
import org.eclipse.ui.texteditor.ITextEditor;

import com.mulgasoft.emacsplus.TecoRegister;

/**
 * Implement - number-to-register
 * 
 * Store a number in a register.
 * If number is nil, a decimal number is read from the buffer starting at point
 *
 * @author Mark Feber - initial API and implementation
 */
public class RegisterNumberHandler extends RegisterHandler implements INonEditingCommand {

	/**
	 * @see com.mulgasoft.emacsplus.minibuffer.IMinibufferExecutable#getMinibufferPrefix()
	 */
	public String getMinibufferPrefix() {
		return NUMBER_PREFIX;
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
			int number = 0; 
			if (getCallCount() > 1) {
				number = getCallCount();
			} else {
				ITextSelection sel = getCurrentSelection(editor);
				// if some text is selected, use it
				if (sel.getLength() == 0) {
					// else get the next token in the buffer and try that
					sel = getNextSelection(getThisDocument(editor), sel);
				}
				String text = null;
				if (sel != null && (text = sel.getText()) != null && text.length() > 0) {
					try {
						number = Integer.parseInt(text);
						int offend = sel.getOffset()+sel.getLength();
						selectAndReveal(editor, offend, offend);
					} catch (NumberFormatException e) {
					}
				} 
			}
			TecoRegister.getInstance().put(key,number);				
			showResultMessage(editor, String.format(REGISTER_NUMBER, key,number), false);
		} else {
			showResultMessage(editor, NO_REGISTER, true);
		}
		return true;
	}
}
