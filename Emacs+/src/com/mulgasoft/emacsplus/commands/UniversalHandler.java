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
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.ParameterizedCommand;
import org.eclipse.core.commands.common.CommandException;
import org.eclipse.jface.bindings.Binding;
import org.eclipse.jface.bindings.Trigger;
import org.eclipse.jface.bindings.keys.KeySequence;
import org.eclipse.jface.bindings.keys.KeyStroke;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Event;
import org.eclipse.ui.texteditor.ITextEditor;

import com.mulgasoft.emacsplus.EmacsPlusActivator;
import com.mulgasoft.emacsplus.EmacsPlusUtils;
import com.mulgasoft.emacsplus.IEmacsPlusCommandDefinitionIds;
import com.mulgasoft.emacsplus.execute.IUniversalResult;
import com.mulgasoft.emacsplus.execute.KbdMacroSupport;
import com.mulgasoft.emacsplus.minibuffer.UniversalMinibuffer;

/**
 * Implements: universal-argument
 * 
 * C-u: Begin a numeric argument for the following command.
 * - If there's a command binding, call the command with the argument
 * - If unmodified character, insert the character argument times into the buffer
 * -- on character insertion, if argument < 0 then leave cursor at front, else move to end
 * 
 * @author Mark Feber - initial API and implementation
 */
public class UniversalHandler extends ExecuteCommandHandler implements INonEditingCommand {

	private static String UA_ERROR = EmacsPlusActivator.getResourceString("UA_Error"); //$NON-NLS-1$
	private final static String ERROR_PREFIX = "%s: ";  							   //$NON-NLS-1$
	private final static String BS = String.valueOf(SWT.BS);
	
	private final static String INITIAL_PREFIX = "C-u"; 							   //$NON-NLS-1$
	private String prefix = INITIAL_PREFIX;
	
	/**
	 * @see com.mulgasoft.emacsplus.minibuffer.IMinibufferExecutable#getMinibufferPrefix()
	 */
	public String getMinibufferPrefix() {
		return prefix;
	}

	/**
	 * @see com.mulgasoft.emacsplus.commands.EmacsPlusCmdHandler#transform(ITextEditor, IDocument, ITextSelection, ExecutionEvent)
	 */
	protected int transform(ITextEditor editor, IDocument document, ITextSelection currentSelection, ExecutionEvent event)
	throws BadLocationException {
		prefix = INITIAL_PREFIX;	//reset in case of M-x invocation 
		UniversalMinibuffer mini = new UniversalMinibuffer(this);
		Object eTrigger = event.getTrigger();
		if (eTrigger instanceof Event) {
			Event ev = (Event)eTrigger;
			prefix = mini.getTrigger(ev.keyCode,ev.stateMask);
		}
		if (KbdMacroSupport.getInstance().isExecuting()) {
			// call without asynchronous wrapper
			// TODO: with ^U, key sequences (instead of commands) appear in kbd macro
			return bufferTransform(mini, editor, event); 		
		} else {
			return miniTransform(mini, editor, event); 		
		}
	}
	
	/**
	 * @see com.mulgasoft.emacsplus.minibuffer.IMinibufferExecutable#executeResult(org.eclipse.ui.texteditor.ITextEditor, java.lang.Object)
	 */
	public boolean doExecuteResult(final ITextEditor editor, final Object minibufferResult) {
		showResultMessage(editor);
		this.executeWithSelectionCheck(editor,  new IWithSelectionCheck () {
			public void execute() { 
				innerExecuteResult(editor,minibufferResult);
			}
		});
		return true;
	}

	/**
	 * Do the command/character execution
	 * 
	 * @param editor
	 * @param minibufferResult
	 * @return true
	 */
	private boolean innerExecuteResult(ITextEditor editor, Object minibufferResult) {
		IUniversalResult ua = (IUniversalResult)minibufferResult; 
		int arg = ua.getCount();

		boolean isMacro = false;
		String trigger = ua.getKeyString();
		Binding binding = ua.getKeyBinding();
		if (binding != null) {
			ParameterizedCommand pcmd = binding.getParameterizedCommand();
			if (pcmd != null) {
				Command cmd = pcmd.getCommand();
				try {
					Event synthEvent = makeEvent(ua);
					isMacro = EmacsPlusUtils.isMacroId(cmd.getId());
					String newid = null;
					// check for 0 dispatch and execute directly if present
					if (arg == 0 && (newid = getDispatchId(cmd.getId(),0)) != null) {
						executeCommand(newid, synthEvent, editor); 
					} else {
						executeUniversal(editor, cmd, synthEvent, arg, ua.isNumeric());
					} 
				} catch (ExecutionException e) {
					errorResult(editor,ERROR_PREFIX + e.getLocalizedMessage());
				} catch (CommandException e) {
					errorResult(editor,ERROR_PREFIX + e.getLocalizedMessage());
				}
			}
		} else if (trigger != null) {
			if (BS.equals(trigger)) { 
				// Hack in backspace as it is handled internally
				executeWithDispatch(editor, IEmacsPlusCommandDefinitionIds.DELETE_PREVIOUS, arg);
			} else {
				// use negative arg to determine offset placement after insertion rather than
				// returning a natural number error as in Emacs
				int abs = ((arg < 0) ? -arg : arg);
				StringBuilder buf = new StringBuilder(abs);
				for (int i = 0; i < abs; i++) {
					buf.append(trigger);
				}
				try {
					int offset = getCursorOffset(editor);
					getThisDocument(editor).replace(offset, 0, buf.toString());
					// on negative count, leave cursor where it is
					setCursorOffset(editor, offset + (arg > 0 ? (arg * trigger.length()) : 0));
				} catch (BadLocationException e) {
					errorResult(editor,ERROR_PREFIX + e.getLocalizedMessage());
				}
			}
		} else {
			errorResult(editor, UA_ERROR);
		}

		if (!isMacro || isResultError()) {
			showResultMessage(editor);
		}
		return true;
	}

	private void errorResult(ITextEditor editor, String formatMessage) {
		setResultMessage(String.format(formatMessage, getResultMessage()), true);
		beep();
	}
	
	private Event makeEvent(IUniversalResult ua) {
		Event result = new Event();
		KeySequence keys = ua.getTrigger();
		if (keys != null) {
			Trigger[] triggers = keys.getTriggers();
			if (triggers[0] instanceof KeyStroke) {	// really, all it can be anyway
				KeyStroke ks = (KeyStroke)triggers[triggers.length - 1];
				result.keyCode = ks.getNaturalKey();
				result.stateMask = ks.getModifierKeys() & SWT.MODIFIER_MASK;
			}
		}
		return result;
	}
}
