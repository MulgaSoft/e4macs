/**
 * Copyright (c) 2009, 2010 Mark Feber, MulgaSoft
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package com.mulgasoft.emacsplus.commands;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.IExecutionListener;
import org.eclipse.core.commands.IHandler;
import org.eclipse.core.commands.NotHandledException;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.ui.commands.ICommandService;
import org.eclipse.ui.texteditor.ITextEditor;

import com.mulgasoft.emacsplus.Beeper;

/**
 * Determine if the command overruns the buffer boundaries, and beep if so.  
 * One of the many pieces that aids the kdb macro execution semantics
 * 
 * @author Mark Feber - initial API and implementation
 */
public class EmacsOverrunHandler extends EmacsMovementHandler {

	/**
	 * Check for attempt cursor movement beyond buffer boundaries and beep if detected
	 * 
	 * @see com.mulgasoft.emacsplus.commands.EmacsMovementHandler#moveWithMark(ITextEditor,
	 *      ITextSelection, String, String)
	 */
	@Override
	protected boolean moveWithMark(ITextEditor editor, ITextSelection currentSelection, String withoutSelect,
			String withSelect) throws BadLocationException {
		int beforeOff = getCursorOffset(editor, currentSelection);
		boolean result = false;
		try {
			addCheckCommand(editor);
			result = super.moveWithMark(editor, currentSelection, withoutSelect, withSelect);
			if (beforeOff == getCursorOffset(editor)) {
				// Some movement commands simply change/remove the selection on first invocation
				ITextSelection sel = (ITextSelection) editor.getSelectionProvider().getSelection();
				if (currentSelection.getLength() == sel.getLength()) {
					// assume we're locked in place by buffer boundaries
					// so we can avoid full check on things like previous-line, etc.
					beep();
				}
			}
		} finally {
			Beeper.setBeepon(true); // always restore
			removeCheckCommand(editor);
		}
		return result;
	}

	// The class is not visible, so just add some hackery
	private final static String ASSIST_HANDLER = "org.eclipse.jface.text.contentassist";	//$NON-NLS-1$

	private void addCheckCommand(ITextEditor editor) {
		((ICommandService) editor.getSite().getService(ICommandService.class)).addExecutionListener(cae);
	}

	private void removeCheckCommand(ITextEditor editor) {
		((ICommandService) editor.getSite().getService(ICommandService.class)).removeExecutionListener(cae);
	}

	/**
	 * Listener to determine when we're in a content assist context and disable beeping on cursor
	 * movement (as we're not moving inside the editor, but the popup)
	 */
	// TODO: perhaps there's a better way?
	private static IExecutionListener cae = new IExecutionListener() {

		public void notHandled(String commandId, NotHandledException exception) {
		}

		public void postExecuteFailure(String commandId, ExecutionException exception) {
		}

		public void postExecuteSuccess(String commandId, Object returnValue) {
		}

		public void preExecute(String commandId, ExecutionEvent event) {
			IHandler handler = event.getCommand().getHandler();
			// The handler class is not visible
			if (handler != null && handler.getClass().getName().startsWith(ASSIST_HANDLER)) {
				Beeper.setBeepon(false); // disable beep if in content assist
			}
		}
	};

}
