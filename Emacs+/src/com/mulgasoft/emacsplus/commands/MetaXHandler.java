/**
 * Copyright (c) 2009, 2014 Mark Feber, MulgaSoft
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 */
package com.mulgasoft.emacsplus.commands;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.NotEnabledException;
import org.eclipse.core.commands.NotHandledException;
import org.eclipse.core.commands.common.CommandException;
import org.eclipse.core.commands.common.NotDefinedException;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.ui.texteditor.ITextEditor;

import com.mulgasoft.emacsplus.EmacsPlusActivator;
import com.mulgasoft.emacsplus.IEmacsPlusCommandDefinitionIds;
import com.mulgasoft.emacsplus.execute.ICommandResult;
import com.mulgasoft.emacsplus.minibuffer.MetaXMinibuffer;

/**
 * Implements: execute-extended-command
 *  
 * Handle M-x style command execution - read a function name (with completion) and call it
 * 
 * @author Mark Feber - initial API and implementation
 */
public class MetaXHandler extends ExecuteCommandHandler implements INonEditingCommand {

	private static String NOTENABLED_MSG = A_MSG + EmacsPlusActivator.getResourceString("MetaX_NotEnabled") + Z_MSG; //$NON-NLS-1$  
	private static String NOTHANDLED_MSG = A_MSG + EmacsPlusActivator.getResourceString("MetaX_NotHandled") + Z_MSG; //$NON-NLS-1$  
	private static String FAILED_MSG = A_MSG + EmacsPlusActivator.getResourceString("MetaX_Failed") + Z_MSG;		 //$NON-NLS-1$  
	
	private static final String MX_PREFIX = "M-x "; 																 //$NON-NLS-1$ 

	/**
	 * @see com.mulgasoft.emacsplus.commands.MinibufferHandler#getMinibufferPrefix()
	 */
	public String getMinibufferPrefix() {
		return MX_PREFIX;
	}
	
	/**
	 * @see com.mulgasoft.emacsplus.commands.EmacsPlusCmdHandler#transform(ITextEditor, IDocument, ITextSelection, ExecutionEvent)
	 */
	@Override
	protected int transform(ITextEditor editor, IDocument document, ITextSelection currentSelection, ExecutionEvent event) 
	throws BadLocationException {
		return bufferTransform(new MetaXMinibuffer(this), editor, event);
	}
	
	/**
	 * @see com.mulgasoft.emacsplus.commands.EmacsPlusCmdHandler#isLooping()
	 */
	@Override
	protected boolean isLooping() {
		return false;
	}
	
	/**
	 * Allow ^U 0 commands to be called via M-x
	 * @see com.mulgasoft.emacsplus.commands.EmacsPlusCmdHandler#isZero()
	 */
	protected boolean isZero() {
		return true;
	}

	/**
	 * @see com.mulgasoft.emacsplus.minibuffer.IMinibufferExecutable#executeResult(org.eclipse.ui.texteditor.ITextEditor, java.lang.Object)
	 */
	public boolean doExecuteResult(final ITextEditor editor, Object minibufferResult) {

		if (minibufferResult != null) {
			final ICommandResult commandR = (ICommandResult) minibufferResult;
			final String name = commandR.getName();
			executeWithSelectionCheck(editor, new IWithSelectionCheck() {
				public void execute() {
					String rms = MetaXHandler.this.getResultMessage();
					boolean isError = MetaXHandler.this.isResultError();
					try {
						if (isUniversalPresent()) {
							executeUniversal(editor,commandR.getCommand(),null,getUniversalCount(),true);
						} else {
							executeCommand(commandR.getCommand().getId(), null, editor);
						}
					} catch (ExecutionException e) {
						rms = name + FAILED_MSG;
						isError = true;
					} catch (NotDefinedException e) {
						rms = name + FAILED_MSG;
						isError = true;
					} catch (NotEnabledException e) {
						rms = name + NOTENABLED_MSG;
						isError = true;
					} catch (NotHandledException e) {
						rms = name + NOTHANDLED_MSG;
						isError = true;
					} catch (CommandException e) {
						rms = name + FAILED_MSG;
						isError = true;
					} finally {
						if (isError || IEmacsPlusCommandDefinitionIds.statusCommands.get(commandR.getCommand().getId()) == null) {
							showResultMessage(editor, rms, isError);
						}
					}
				}
			});
		}
		return true;
	}
	
}