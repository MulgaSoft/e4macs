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

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.commands.Command;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.NotEnabledException;
import org.eclipse.core.commands.NotHandledException;
import org.eclipse.core.commands.common.CommandException;
import org.eclipse.core.commands.common.NotDefinedException;
import org.eclipse.jface.bindings.TriggerSequence;
import org.eclipse.jface.bindings.keys.KeySequence;
import org.eclipse.jface.bindings.keys.KeyStroke;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.widgets.Event;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.commands.ICommandService;
import org.eclipse.ui.console.IConsoleView;
import org.eclipse.ui.console.TextConsoleViewer;
import org.eclipse.ui.keys.IBindingService;
import org.eclipse.ui.texteditor.ITextEditor;

import com.mulgasoft.emacsplus.EmacsPlusUtils;
import com.mulgasoft.emacsplus.IEmacsPlusCommandDefinitionIds;
import com.mulgasoft.emacsplus.MarkUtils;

/** 
 * Abstract handler for all mark aware movement commands
 * 
 * If the mark is set and attached to the current selection, then extend the selection
 * with the movement; otherwise just move
 * 
 * If shift-select-mode is enabled, and the SHIFT key is depressed when invoking
 * the movement command, the mark is set before moving point. For more information
 * @see com.mulgasoft.emacsplus.commands.ShiftSelectModeHandler
 * 
 * @author Mark Feber - initial API and implementation
 */
public abstract class EmacsMovementHandler extends EmacsPlusNoEditHandler implements IConsoleDispatch {

	/** When true, the previous command was invoked with a shift-select key binding */
	boolean wasShifted = false;  		   // remember the previous state
	
	/** When true, the command was invoked with a shift-select key binding */
	private static boolean shifted = false; // holds the current state
		
	/** Briefly retain the event in case of ^U invocation of command */
	private ExecutionEvent executeEvent = null;	// holds the invoking execution event or null
		
	// The SHIFT modifier family of bindings is loaded by the Options plugin.
	// Only enable mode, if Options (with all the necessary key bindings) is loaded
	// Once set, the user can change this flag to disable shift mode
	private static class SelectMode {
		// is shift mode enabled?
		private static boolean enabled = isShiftEnabled();
	}

	/**
	 * @see com.mulgasoft.emacsplus.commands.EmacsPlusCmdHandler#execute(org.eclipse.core.commands.ExecutionEvent)
	 */
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		// if the movement command was called via transforWithCount after detecting a universal
		// arg, the execute method is only called once regardless of the number of iterations
		// so remember the event that initially invoked us
		executeEvent = event;
		return super.execute(event);
	}

	/**
	 * Move using either a selection command or an non-selection command depending
	 * on the state of the mark, currentSelection, and position
	 * 
	 * @param editor
	 * @param currentSelection
	 * @param withoutSelect - non-selection command id 
	 * @param withSelect - selection command id
	 * 
	 * @return true if the movement was made with selection
	 * @throws BadLocationException
	 */
	protected boolean moveWithMark(ITextEditor editor, ITextSelection currentSelection, String withoutSelect, String withSelect)
	throws BadLocationException {
		boolean result = false;
		try {
			setShiftedState();
			boolean markit  = isMarkEnabled(editor,currentSelection);
			if (isShiftMode()) {
				if (wasShifted) {
					if (isShifted()) {
						// just keep selecting
						markit = true;
					} else {
						Map<String,String> params = new HashMap<String,String>();
						params.put(SHIFT_ARG,ShiftState.CLEAR.toString());
						// clear selection and turn off mark; execute a command so kbd macros can emulate
						// include an empty Event so that when defining a kbd macro the command will be included
						EmacsPlusUtils.executeCommand(IEmacsPlusCommandDefinitionIds.SET_MARK, params, new Event(), editor);
						markit = false;
					}
				} else if (isShifted()) {
					Map<String,String> params = new HashMap<String,String>();
					params.put(SHIFT_ARG,ShiftState.SET.toString());
					// clear selection and restart mark; execute a command so kbd macros can emulate
					// include an empty Event so that when defining a kbd macro the command will be included
					EmacsPlusUtils.executeCommand(IEmacsPlusCommandDefinitionIds.SET_MARK, params, new Event(), editor);
					markit = true;
				}
			}
			if (markit) {
				EmacsPlusUtils.executeCommand(withSelect, null, editor);
				// set mark flag if we're back at ground zero, else clear
				setFlagMark(getCurrentSelection(editor).getLength() == 0);
				result =  true;
			} else {
				EmacsPlusUtils.executeCommand(withoutSelect, null, editor);
			}
		} catch (ExecutionException e) {
		} catch (NotDefinedException e) {
		} catch (NotEnabledException e) {
		} catch (NotHandledException e) {
		} catch (CommandException e) {
		} 
		return result;
	}

	// When invoked with ^U, movement can expand the selection
	// so, check each time
	@Override
	protected ITextSelection getCmdSelection(ITextEditor editor,
			ITextSelection selection) throws ExecutionException {
		ITextSelection cSelection = getCurrentSelection(editor);
		if (!cSelection.equals(selection)) {
			return cSelection;
		} else {
			return super.getCmdSelection(editor, selection);
		}
	}
	
	/**
	 * @see com.mulgasoft.emacsplus.commands.IConsoleDispatch#consoleDispatch(TextConsoleViewer, IConsoleView, ExecutionEvent)
	 */
	public Object consoleDispatch(TextConsoleViewer viewer, IConsoleView activePart, ExecutionEvent event) {
		
		StyledText st = viewer.getTextWidget(); 
		String id = event.getCommand().getId(); 
		boolean isSelect =  isMarkEnabled(viewer,(ITextSelection)viewer.getSelection());
		int action = getDispatchId(id,isSelect);
		if (action > -1) {
			st.invokeAction(action);
		} else if ((id = getId(isSelect)) != null) {
			// support sexps 
			try {
				EmacsPlusUtils.executeCommand(id, null, activePart);
			} catch (Exception e) {
				e.printStackTrace();
			}			
		}
		return null;
	}
	
	/**
	 * Fetch the correct dispatch id
	 *  
	 * @return return new id or -1
	 */
	private int getDispatchId(String id, boolean selectIt) {
		Integer dispatch = (selectIt ? dispatchSelectIds.get(id) : dispatchCmdIds.get(id));
		if (dispatch == null) {
			dispatch = -1;
		}
		return dispatch;
	}
	
	private String getId(boolean isSelect) {
		if (isSelect) {
			return getSelectId(); 
		} else {
			return getNoSelectId();
		}
	}
	
	/**
	 * Get the selecting command id for use in the ConsoleView
	 * 
	 * @return a command id that supports the ConsoleView or null
	 */
	protected String getSelectId() {
		return null;
	}
	
	/**
	 * Get the non-selecting command id for use in the ConsoleView
	 * 
	 * @return a command id that supports the ConsoleView or null
	 */
	protected String getNoSelectId() {
		return null;
	}
	
	// Shift key processing
	
	/**
	 * Check if the Options plugin is loaded by checking whether the shift-select-mode command is handled
	 * as the handler is set up in the Options plugin.xml
	 * 
	 * @return true if Options loaded, else false
	 */
	// Checking for the handler seems to be the only reliable way of checking - is there a better way?
	private static boolean isShiftEnabled() {
		// The shift-select-mode handler is 'registered' in the Options plugin.xml
		boolean result = false;
		ICommandService ics = ((ICommandService) PlatformUI.getWorkbench().getService(ICommandService.class));
		if (ics != null && ics.getDefinedCommandIds().contains(IEmacsPlusCommandDefinitionIds.SHIFT_SELECT)) {
			Command command = ics.getCommand(IEmacsPlusCommandDefinitionIds.SHIFT_SELECT);
			result = command.isHandled();
		}
		return result;
	}
	
	/**
	 * Change the enabled state of shift-select-mode to value
	 * 
	 * @param value true to enable, false to disable
	 */
	void setShiftMode(boolean value) {
		SelectMode.enabled = value;
	}
	
	/**
	 * Is shift-select-mode enabled? 
	 * 
	 * @return true if enabled, else false
	 */
	boolean isShiftMode() {
		return SelectMode.enabled; 
	}
	
	/**
	 * Check the SHIFT state of the command's invocation.
	 * 
	 * @return true if invoked by a key binding containing SHIFT modifier, else false
	 */
	public static boolean isShifted() {
		return EmacsMovementHandler.shifted;
	}

	/**
	 * Force clearing of shifted flag when appropriate 
	 */
	public static void clearShifted() {
		shifted = false;
	}
	
	/**
	 * Examine the ExecutionEvent trigger for the presence of the SHIFT key.
	 * Note that event may be null depending on how the command was invoked
	 * 
	 * @param event the Event trigger
	 */
	private void setShifted(ExecutionEvent event) {
		if (isShiftMode() && event != null) {
			EmacsMovementHandler.shifted = getShifted(event); 
		}
	}
	
	/**
	 * Set the shift select flags for this command:
	 * - remember the previous state of shift
	 * - verify that this is a shift select movement command
	 * - if so, examine the event to see if SHIFT is set
	 */
	private void setShiftedState() {
		if (executeEvent != null) {
			// remember state of previous command
			wasShifted = isShifted();
			// on shift-select-mode commands, check for the SHIFT key
			if (MarkUtils.isShiftCommand(executeEvent.getCommand().getId())) {
				setShifted(executeEvent);
			} else {
				clearShifted();
				// we only care if its shifted
				executeEvent = null;
			}
		}
	}

	/**
	 * Does the trigger for this movement command contain the SHIFT key?
	 * 
	 * Enforce that the <binding> and <binding>+SHIFT belong to the same Command. 
	 * If not, don't apply shift selection (if enabled) for this command (i.e. return false).
	 *  
	 * @param event the Execution event that invoked this command
	 * 
	 * @return true if SHIFT modifier was set, else false
	 */
	private boolean getShifted(ExecutionEvent event) {
		// NB: only single keystroke commands are valid 
		boolean result = false;
		Object trigger = event.getTrigger();
		Event e = null;
		if (trigger != null && trigger instanceof Event && ((e = (Event)trigger).stateMask & SWT.SHIFT )!= 0) {
			String cmdId = event.getCommand().getId();
			int mask = (e.stateMask & SWT.MODIFIER_MASK) ^ SWT.SHIFT;
			int u_code = Character.toUpperCase((char)e.keyCode);
			IBindingService bs = (IBindingService) PlatformUI.getWorkbench().getService(IBindingService.class);
			if (cmdId != null && bs != null) {
				TriggerSequence[] sequences = bs.getActiveBindingsFor(cmdId);
				for (TriggerSequence s : sequences) {
					if (s instanceof KeySequence) {
						KeyStroke[] strokes = ((KeySequence)s).getKeyStrokes();
						if (strokes.length == 1) {
							KeyStroke k = strokes[strokes.length - 1];
							// if keyCode is alpha, then we test uppercase, else keyCode
							if (k.getModifierKeys() == mask
									&& (k.getNaturalKey() == u_code || k.getNaturalKey() == e.keyCode)) {
								result = true;
								break;
							}
						}
					}
				}
			} 
		}
		return result;
	}

}
