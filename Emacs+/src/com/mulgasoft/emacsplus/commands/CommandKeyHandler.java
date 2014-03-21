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

import org.eclipse.core.commands.Command;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.common.NotDefinedException;
import org.eclipse.jface.bindings.Binding;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.ui.texteditor.ITextEditor;

import com.mulgasoft.emacsplus.EmacsPlusActivator;
import com.mulgasoft.emacsplus.EmacsPlusUtils;
import com.mulgasoft.emacsplus.execute.EmacsPlusConsole;
import com.mulgasoft.emacsplus.execute.IBindingResult;
import com.mulgasoft.emacsplus.minibuffer.KeyHandlerMinibuffer;

/**
 * Implements: describe-key
 * 
 * Describe key binding in Emacs+ console
 * 
 * @author Mark Feber - initial API and implementation
 */
public class CommandKeyHandler extends CommandDescribeHandler {
	
	private static String KEY_DESC_PREFIX = EmacsPlusActivator.getResourceString("Cmd_Key_Prefix");	//$NON-NLS-1$
	
	/**
	 * @see com.mulgasoft.emacsplus.minibuffer.IMinibufferExecutable#getMinibufferPrefix()
	 */
	public String getMinibufferPrefix() {
		return KEY_DESC_PREFIX;
	}
	
	/**
	 * @see com.mulgasoft.emacsplus.commands.EmacsPlusCmdHandler#transform(ITextEditor, IDocument, ITextSelection, ExecutionEvent)
	 */
	protected int transform(ITextEditor editor, IDocument document, ITextSelection currentSelection,
			ExecutionEvent event) throws BadLocationException {
		return bufferTransform(new KeyHandlerMinibuffer(this), editor, event);
	}
	
	/**
	 * @see com.mulgasoft.emacsplus.minibuffer.IMinibufferExecutable#executeResult(org.eclipse.ui.texteditor.ITextEditor, java.lang.Object)
	 */
	public boolean executeResult(final ITextEditor editor, final Object minibufferResult) {
		
		if (minibufferResult != null) {
			final EmacsPlusConsole console = EmacsPlusConsole.getInstance();
			console.clear();
			console.activate();

			EmacsPlusUtils.asyncUiRun(new Runnable() {
				public void run() {
					String summary = EMPTY_STR;

					IBindingResult bindingR = (IBindingResult) minibufferResult;
					String name = bindingR.getKeyString();
					if (bindingR == null || bindingR.getKeyBinding() == null) {
						summary = String.format(CMD_NO_RESULT, name) + CMD_NO_BINDING;
						console.print(summary);
					} else {
						Binding binding = bindingR.getKeyBinding();
						summary = String.format(CMD_KEY_RESULT,name); 
						console.print(summary);
						try {
							Command com = getCommand(binding);
							if (com != null) {
								name = normalizeCommandName(com.getName());
								summary += name;
								console.printBinding(name + CR);
								printCmdDetails(getPCommand(binding),console);
							}
						} catch (NotDefinedException e) {
							// can't happen as the Command will be null or valid
						}
					}
					showResultMessage(editor, summary, false);
				}});
		}
		return true;
	}
}
