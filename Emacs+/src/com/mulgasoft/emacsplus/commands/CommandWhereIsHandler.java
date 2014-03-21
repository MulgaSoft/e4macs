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

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.swt.SWT;
import org.eclipse.ui.texteditor.ITextEditor;

import com.mulgasoft.emacsplus.EmacsPlusActivator;
import com.mulgasoft.emacsplus.execute.EmacsPlusConsole;
import com.mulgasoft.emacsplus.execute.CommandHelp;
import com.mulgasoft.emacsplus.execute.ICommandResult;
import com.mulgasoft.emacsplus.minibuffer.MetaXMinibuffer;

/**
 * Implements: where-is-command
 * 
 * Reads a command name from the minibuffer and displays just key-binding information about it 
 * 
 * @author Mark Feber - initial API and implementation
 */
public class CommandWhereIsHandler extends MinibufferExecHandler implements INonEditingCommand {

	private static String WI_PREFIX = EmacsPlusActivator.getResourceString("Cmd_WI_Prefix");	//$NON-NLS-1$
	
	/**
	 * @see com.mulgasoft.emacsplus.minibuffer.IMinibufferExecutable#getMinibufferPrefix()
	 */
	public String getMinibufferPrefix() {
		return WI_PREFIX;
	}
	
	/**
	 * @see com.mulgasoft.emacsplus.commands.AbstractCmdHandler#transform(org.eclipse.ui.texteditor.ITextEditor, org.eclipse.jface.text.IDocument, org.eclipse.jface.text.ITextSelection, org.eclipse.core.commands.ExecutionEvent)
	 */
	protected int transform(ITextEditor editor, IDocument document, ITextSelection currentSelection,
			final ExecutionEvent event) throws BadLocationException {

		return bufferTransform(new MetaXMinibuffer(this), editor, event); 		
	}

	/**
	 * @see com.mulgasoft.emacsplus.minibuffer.IMinibufferExecutable#executeResult(org.eclipse.ui.texteditor.ITextEditor, java.lang.Object)
	 */
	public boolean doExecuteResult(ITextEditor editor, Object minibufferResult) {

		if (minibufferResult != null) {
			EmacsPlusConsole console = EmacsPlusConsole.getInstance();
			console.clear();
			console.activate();

			ICommandResult commandR = (ICommandResult) minibufferResult;
			String name = commandR.getName();
			String[] bindings = CommandHelp.getKeyBindingStrings(commandR.getCommand(), true);
			if (bindings.length == 0) {
				console.print(String.format(CMD_NO_RESULT,name));
				console.printBinding(CMD_NO_BINDING);
			} else {
				console.print(String.format(CMD_RESULT,name) + CR);
				for (int i = 0; i < bindings.length; i+=2) {
					console.printBinding(SWT.TAB + bindings[i]);
					console.printContext(A_MSG + bindings[i+1] + Z_MSG + CR);
				}
			}
		}
		return true;
	}
}
