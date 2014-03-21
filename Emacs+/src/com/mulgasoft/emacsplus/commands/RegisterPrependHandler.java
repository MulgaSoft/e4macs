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

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.common.CommandException;
import org.eclipse.ui.texteditor.ITextEditor;

import com.mulgasoft.emacsplus.IEmacsPlusCommandDefinitionIds;
import com.mulgasoft.emacsplus.TecoRegister;

/**
 * Implement - prepend-to-register
 * 
 * Prepend region to text in register *R*
 *  
 * @author Mark Feber - initial API and implementation
 */
public class RegisterPrependHandler extends RegisterHandler implements INonEditingCommand {


	/**
	 * @see com.mulgasoft.emacsplus.minibuffer.IMinibufferExecutable#getMinibufferPrefix()
	 */
	public String getMinibufferPrefix() {
		return PREPEND_PREFIX;
	}
	
	/**
	 * @see com.mulgasoft.emacsplus.minibuffer.IMinibufferExecutable#executeResult(org.eclipse.ui.texteditor.ITextEditor, java.lang.Object)
	 */
	public boolean doExecuteResult(ITextEditor editor, Object minibufferResult) {
		
		if (minibufferResult != null) {
			String key = (String)minibufferResult;
			String text = getCurrentSelection(editor).getText();
			if (text != null && text.length() > 0) {
				String rText = TecoRegister.getInstance().getText(key);
				if (rText != null) {
					text = text + rText;
				}
				TecoRegister.getInstance().put(key,text);
				if (isEditable() && getCallCount() > 1) {
					// if called with ^U, then delete text as well
					try {
						this.executeCommand(IEmacsPlusCommandDefinitionIds.EMP_CUT, null, editor);
					} catch (ExecutionException e) {
					} catch (CommandException e) {}
				} else {
					int offset = getCursorOffset(editor); 
					selectAndReveal(editor, offset, offset);
				}
				showResultMessage(editor, String.format(PREPENDED, key), false);
			}
		} else {
			showResultMessage(editor, NO_REGISTER, true);
		}
		return true;
	}
	}
