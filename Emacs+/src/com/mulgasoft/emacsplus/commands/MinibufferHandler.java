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
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.ITextEditor;

import com.mulgasoft.emacsplus.EmacsPlusUtils;
import com.mulgasoft.emacsplus.MarkUtils;
import com.mulgasoft.emacsplus.execute.KbdMacroSupport;
import com.mulgasoft.emacsplus.minibuffer.WithMinibuffer;

/**
 * Abstract base class for commands using the minibuffer
 * 
 * @author Mark Feber - initial API and implementation
 */
public abstract class MinibufferHandler extends EmacsPlusCmdHandler {

	protected static final String A_MSG = " ["; 	 //$NON-NLS-1$ 
	protected static final String Z_MSG = "]";  	 //$NON-NLS-1$ 
	protected static final String COMMA_SEPR = ", "; //$NON_NLS-1$

	private String resultMessage = null;
	private boolean resultError = false;
	
	/**
	 * Initialize and start the minibuffer listening for the keyboard in the current thread
	 * 
	 * @param mini the com.mulgasoft.emacsplus.minibuffer.... instance
	 * @param editor
	 * @param event
	 * @return NO_OFFSET
	 * @throws BadLocationException
	 */
	public static int bufferTransform(final WithMinibuffer mini, final ITextEditor editor, final ExecutionEvent event) throws BadLocationException {
		
		mini.setCommandId(MarkUtils.getCurrentCommand());	// remember the command that invoked us
		mini.beginSession(editor, PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage(),event);
		
		if (!KbdMacroSupport.getInstance().isExecuting()) {		
			EmacsPlusUtils.asyncUiRun(new Runnable() {
				public void run() {
					// must run in ui thread to get the update
					mini.updateStatusLine();
				}
			});
		}
		return NO_OFFSET;
	}

	/**
	 * Initialize and start the minibuffer listening for the keyboard in a separate ui thread
	 * 
	 * @param mini the com.mulgasoft.emacsplus.minibuffer.... instance
	 * @param editor
	 * @param event
	 * @return NO_OFFSET
	 */
	public static int miniTransform(final WithMinibuffer mini, final ITextEditor editor, final ExecutionEvent event) {
		EmacsPlusUtils.asyncUiRun(new Runnable() {
			public void run() {
				mini.setCommandId(MarkUtils.getCurrentCommand());	// remember the command that invoked us
				mini.beginSession(editor, PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage(),event);
			}
		});
		return NO_OFFSET;
	}
	
	/**
	 * @return the resultMessage
	 */
	protected String getResultMessage() {
		return resultMessage;
	}

	/**
	 * @param resultMessage the resultMessage to set
	 */
	public void setResultMessage(String resultMessage) {
		setResultMessage(resultMessage, false);
	}

	/**
	 * @param resultMessage the resultMessage to set
	 */
	public void setResultMessage(String resultMessage, boolean resultError) {
		this.resultMessage = resultMessage;
		this.resultError = resultError;		
	}

	/**
	 * @return the resultError
	 */
	protected boolean isResultError() {
		return resultError;
	}

	protected void showResultMessage(ITextEditor editor) {
		showResultMessage(editor, getResultMessage(), isResultError());
	}
	
	protected void showResultMessage(ITextEditor editor, String message, boolean error) {
		if (message != null) {
			String mess = ((message.length() > 0) ? getMinibufferPrefix() : EMPTY_STR) + message;
			EmacsPlusUtils.showMessage(editor, mess, error);
		}
	}
	
	public String getMinibufferPrefix() {
		return EMPTY_STR;
	}
	
}
