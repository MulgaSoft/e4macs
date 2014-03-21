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
 * Implement - point-to-register
 * 
 * Record the position of point and the current buffer in register *R*
 * 
 * @author Mark Feber - initial API and implementation
 */
public class RegisterPointHandler extends RegisterHandler implements INonEditingCommand {
	
	protected boolean needsSelection() {
		return false;
	}
	
	/**
	 * @see com.mulgasoft.emacsplus.minibuffer.IMinibufferExecutable#getMinibufferPrefix()
	 */
	public String getMinibufferPrefix() {
		return POINT_PREFIX;
	}
	
	/**
	 * @see com.mulgasoft.emacsplus.minibuffer.IMinibufferExecutable#executeResult(org.eclipse.ui.texteditor.ITextEditor, java.lang.Object)
	 */
	public boolean doExecuteResult(ITextEditor editor, Object minibufferResult) {
		
		if (minibufferResult != null) {
			String key = (String)minibufferResult;
			TecoRegister.getInstance().put(key,editor,getCursorOffset(editor));
			showResultMessage(editor, String.format(LOCATION, key), false);
		} else {
			showResultMessage(editor, NO_REGISTER, true);
		}
		return true;
	}
}
