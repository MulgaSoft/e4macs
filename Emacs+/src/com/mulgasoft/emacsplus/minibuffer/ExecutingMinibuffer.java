/**
 * Copyright (c) 2009, 2010 Mark Feber, MulgaSoft
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 */
package com.mulgasoft.emacsplus.minibuffer;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.texteditor.ITextEditor;

import com.mulgasoft.emacsplus.EmacsPlusActivator;

/**
 * On CR, pass the minibuffer result back to the command invocation
 * 
 * @author Mark Feber - initial API and implementation
 */
public abstract class ExecutingMinibuffer extends HistoryMinibuffer {

	static final String A_MSG = " [";   																  //$NON-NLS-1$ 
	static final String Z_MSG = "]";																	  //$NON-NLS-1$ 
	
	static String ABORT_MSG = A_MSG + EmacsPlusActivator.getResourceString("Exec_Abort") + Z_MSG;   	  //$NON-NLS-1$  
	static String COMPLETE_MSG = A_MSG + EmacsPlusActivator.getResourceString("Exec_Completion") + Z_MSG; //$NON-NLS-1$  
	static String NOMATCH_MSG = A_MSG + EmacsPlusActivator.getResourceString("Exec_NoMatch") + Z_MSG;     //$NON-NLS-1$ 
	
	private IMinibufferExecutable executable;
	
	public ExecutingMinibuffer(IMinibufferExecutable executable) {
		super();
		setExecutable(executable);
	}
	
	public void initMinibufferText(String text){
		initMinibuffer(text);
	}
	
	/**
	 * @see com.mulgasoft.emacsplus.minibuffer.WithMinibuffer#getMinibufferPrefix()
	 */
	@Override
	protected String getMinibufferPrefix() {
		return (executable != null ? executable.getMinibufferPrefix() : EMPTY_STR);
	}

	protected IMinibufferExecutable getExecutable() {
		return executable;
	}

	private void setExecutable(IMinibufferExecutable executable) {
		this.executable = executable;
	}
	
	/**
	 * Pass the minibuffer result back to the command invocation
	 * 
	 * @see com.mulgasoft.emacsplus.minibuffer.WithMinibuffer#executeResult(org.eclipse.ui.texteditor.ITextEditor, java.lang.Object)
	 */
	@Override
	protected boolean executeResult(ITextEditor editor, Object commandResult) {
		return executeResult(editor,executable,commandResult);
	}
	
	boolean executeResult(ITextEditor editor, IMinibufferExecutable executable, Object commandResult) {
		boolean result = true;
		if (executable != null) {
			result = executable.executeResult(editor,commandResult);
		}
		return result;
	}
	
	protected boolean exitExecuteResult(ITextEditor editor, Object commandResult) {
		// Save the executable for invocation after leaving current minibuffer.
		// We do this so that we can call another minibuffer command invoked from here 
		// in this thread, rather than processing it as an asynchUI.  The proximate 
		// cause is that the invoked minibuffer is set up before the invoking
		// minibuffer is torn down (which can call setKeyFilter(false) and the command
		// we're invoking may have specified true.
		boolean result = false;
		IMinibufferExecutable exec = getExecutable();
		leave();
		if (exec != null) {
			result =  executeResult(editor,exec,commandResult);
		}
		return result;
	}
	
	
	/**
	 * Set the result message back on the invoking command
	 * 
	 * @param resultString the message
	 * @param resultError error if true
	 * @param forceNow if true, display immediately
	 */
	protected void setResultMessage(String resultString, boolean resultError, boolean forceNow) {
		if (forceNow) {
			super.setResultMessage(resultString, resultError);
		}
		executable.setResultMessage(resultString,resultError);
	}
	
	/**
	 * @see com.mulgasoft.emacsplus.minibuffer.WithMinibuffer#setResultMessage(java.lang.String, boolean)
	 */
	protected void setResultMessage(String resultString, boolean resultError) {
		 executable.setResultMessage(resultString,resultError);
	}
	
	protected void setResultMessage(String resultString) {
		 executable.setResultMessage(resultString,false);
	}
	
	/**
	 * @see com.mulgasoft.emacsplus.minibuffer.WithMinibuffer#initializeBuffer(org.eclipse.ui.texteditor.ITextEditor, org.eclipse.ui.IWorkbenchPage)
	 */
	@Override
	protected boolean initializeBuffer(ITextEditor editor, IWorkbenchPage page) {
		return true;
	}

	/**
	 * Minor cleanups after leaving
	 * 
	 * @see com.mulgasoft.emacsplus.minibuffer.WithMinibuffer#leave(boolean)
	 */
	protected void leave(boolean closeDialog) {
		super.leave(closeDialog);
		setExecutable(null);
	}
	
	/**
	 * charEvent fragment: only allow numbers or edits & escapes
	 * @param event
	 */
	protected void numCharEvent(VerifyEvent event) {
		event.doit = false;
		switch (event.character) {
		case 0x0D: // CR - execute command (if complete) \r
		case 0x1B: // ESC - another way to leave
		case 0x08: // BS
		case 0x7F: // DEL
			super.charEvent(event);
			break;
		default:
			if (checkAlt(event)) {
				// enable history events
				if (dispatchAlt(event)) {
					event.doit = false;
					break;
				}
			} else if ((Character.isDigit(event.character) && ((event.stateMask & SWT.MODIFIER_MASK) == 0)) || ('-' == event.character)) { 
				// accept if plain number or minus				
				super.charEvent(event);
			} else {
				beep();
			}
		}
	}

	/**
	 * charEvent fragment: process any text char, but also treat it as a <CR>
	 * @param event
	 */
	protected void immediateCharEvent(VerifyEvent event) {
		event.doit = false;
		switch (event.character) {
		case 0x0D: // CR - execute command (if complete) \r
		case 0x1B: // ESC - another way to leave
		case 0x08: // BS
		case 0x7F: // DEL
			super.charEvent(event);
			break;
		default:
			// respond immediately to a character
			super.charEvent(event);
			this.executeCR(event);
		}
	}
	
	/**
	 * charEvent fragment: disallow spaces and tabs
	 * @param event
	 */
	protected void noSpaceCharEvent(VerifyEvent event) {
		event.doit = false;
		switch (event.character) {
		case ' ': // beep on space
		case '\t': // or tab
			event.doit = false;
			beep();
			break;
		default:
			super.charEvent(event);
		}
	}
}
