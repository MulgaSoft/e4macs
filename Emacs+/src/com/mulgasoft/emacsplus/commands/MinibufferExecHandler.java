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

import com.mulgasoft.emacsplus.EmacsPlusActivator;
import com.mulgasoft.emacsplus.minibuffer.IMinibufferExecutable;

/**
 * Abstract base class for command handlers that execute using the result from a minibuffer
 * 
 * @author Mark Feber - initial API and implementation
 */
public abstract class MinibufferExecHandler extends MinibufferHandler implements IMinibufferExecutable {
	
	static String CMD_RESULT =  EmacsPlusActivator.getResourceString("Cmd_Result"); 	   //$NON-NLS-1$
	static String CMD_NO_RESULT =  EmacsPlusActivator.getResourceString("Cmd_No_Result");  //$NON-NLS-1$
	static String CMD_NO_BINDING = EmacsPlusActivator.getResourceString("Cmd_No_Binding"); //$NON-NLS-1$

	private final static String A_SPACE = " ";  										   //$NON-NLS-1$ 
	private final static String A_DASH = "-";   										   //$NON-NLS-1$
	
	/**
	 * Inner form of executeResult for minibuffer handlers
	 * 
	 * @param editor
	 * @param minibufferResult
	 * 
	 * @return true if we should exit after the execution
	 */
	protected abstract boolean doExecuteResult(ITextEditor editor, Object minibufferResult);
	
	/**
	 * @see com.mulgasoft.emacsplus.minibuffer.IMinibufferExecutable#executeResult(ITextEditor, Object)
	 */
	public boolean executeResult(ITextEditor editor, Object minibufferResult) {
		boolean result = doExecuteResult(editor,minibufferResult);
		if (result) {
			postExecute();
		}
		return result;
	}

	/**
	 * Delay post transform cleanup until after minibuffer result execution
	 * 
	 * @see com.mulgasoft.emacsplus.commands.EmacsPlusCmdHandler#isTransform()
	 */
	@Override
	protected boolean isTransform() {
		return false;
	}
	
	/**
	 * Format command name string to Emacs standard by lower-casing and
	 * replacing any spaces with dashes
	 * 
	 * @param name the command name string
	 * @return modified command name string
	 */
	String normalizeCommandName(String name) {
		return name.trim().toLowerCase().replace(A_SPACE,A_DASH);
	}

}
