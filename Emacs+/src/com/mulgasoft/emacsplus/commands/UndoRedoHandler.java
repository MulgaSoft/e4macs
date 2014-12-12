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
import org.eclipse.core.commands.NotEnabledException;
import org.eclipse.core.commands.common.CommandException;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.ui.texteditor.ITextEditor;

import com.mulgasoft.emacsplus.EmacsPlusActivator;
import com.mulgasoft.emacsplus.EmacsPlusUtils;
import com.mulgasoft.emacsplus.MarkUtils;
import com.mulgasoft.emacsplus.MarkUtils.ICommandIdListener;
import com.mulgasoft.emacsplus.preferences.EmacsPlusPreferenceConstants;

import static com.mulgasoft.emacsplus.IEmacsPlusCommandDefinitionIds.UNDO_REDO;
import static com.mulgasoft.emacsplus.IEmacsPlusCommandDefinitionIds.EMP_UNDO;
import static com.mulgasoft.emacsplus.IEmacsPlusCommandDefinitionIds.EMP_REDO;

/**
 * Approximate support for enhanced undo behavior from Emacs:
 * 
 * Enhance the Eclipse Undo operation to (partially) mimic the Emacs linkage of
 * undo/redo.
 * 
 * When performing one or more undo commands any non-edit command, other than an
 * undo command, breaks the sequence of undo commands. Starting from that
 * moment, the entire sequence of undo commands just performed acts as if placed
 * onto the undo stack (i.e. they will be redone in sequence on subsequent undo
 * commands). Thus, you can redo changes you have undone by typing `C-f' or any
 * other command that has no important effect, and then using more undo
 * commands.
 * 
 * Note, however, that unlike Emacs, as soon as the document is altered (other
 * than by an undo/redo command) the redo stack is discarded (as shocking as
 * this discard may sound, this is the normal Eclipse behavior). 
 * See org.eclipse.text.undo.DocumentUndoManager and
 *     org.eclipse.core.commands.operations.DefaultOperationHistory.add()
 * 
 * @author Mark Feber - initial API and implementation
 */
public class UndoRedoHandler extends EmacsPlusCmdHandler implements ICommandIdListener {
	
	private static final String UNDO_EMPTY = EmacsPlusActivator.getResourceString("Undo_Empty");   //$NON-NLS-1$  
	private static final String UNDO_STATUS = EmacsPlusActivator.getResourceString("Undo_Status"); //$NON-NLS-1$  
	private static final String REDO_STATUS= EmacsPlusActivator.getResourceString("Redo_Status");  //$NON-NLS-1$  

	private static UndoState state;
	private static boolean emacsUndo = EmacsPlusUtils.getPreferenceBoolean(EmacsPlusPreferenceConstants.P_EMACS_UNDO);
	
	public UndoRedoHandler() {
		super();
		state = undo;
		MarkUtils.addCommandIdListener(this);
	}
	
	public static void setEmacsUndo(boolean emacsUndo) {
		UndoRedoHandler.emacsUndo = emacsUndo;
	}
	
	/**
	 * @see com.mulgasoft.emacsplus.commands.EmacsPlusCmdHandler#transform(ITextEditor, IDocument, ITextSelection, ExecutionEvent)
	 */
	@Override
	protected int transform(ITextEditor editor, IDocument document,
			ITextSelection currentSelection, ExecutionEvent event)
			throws BadLocationException {
		undoRedo(editor);
		return NO_OFFSET;
	}

	private Object undoRedo(ITextEditor editor) {
		Object result = null;
		try {
			EmacsPlusUtils.showMessage(editor, state.toString(), false);
			return executeCommand(state.getCommandId(), null, editor);
		} catch (ExecutionException e) {
		} catch (NotEnabledException e) {
			// Undo/redo commands are not enabled, if there's nothing to do
			state.notEnabled(editor);
		} catch (CommandException e) {
		}
		return result;
	}
	
	/**
	 * @see com.mulgasoft.emacsplus.MarkUtils.ICommandIdListener#setCommandId(java.lang.String)
	 */
	public void setCommandId(String commandId) {
		if (emacsUndo) {
			if (commandId != null) {
				// transition state based on previous command executed
				state.updateState((UNDO_REDO.equals(commandId) || EMP_UNDO.equals(commandId)), commandId);
			}
		}
	}

	private interface UndoState {
		void updateState(boolean isUndo, String id);
		void notEnabled(ITextEditor editor);
		String getCommandId();
	}
	
	private final UndoState undo = new UndoState() {

		private boolean preUndo = false;
		private boolean noMore = false;
		
		public void updateState(boolean isUndo, String id) {
			if (!isUndo) {
				if (preUndo) {
					preUndo = false;
					state = redo;
				}
			} else if (noMore){
				noMore = false;
				preUndo = false;
				state = redo;
			} else {
				preUndo = true;
			}
		}
		
		public void notEnabled(ITextEditor editor) {
			// we've run out of undo, so complain
			EmacsPlusUtils.showMessage(editor, UNDO_EMPTY, true);
			noMore = true;
		}

		public String getCommandId() {
			return EMP_UNDO;
		}
		
		public String toString() {
			return UNDO_STATUS;
		}
	};
	
	private final UndoState redo = new UndoState() {

		public void updateState(boolean isUndo, String id) {
			// TODO: verify that Eclipse clears redo on text changed
		}
		
		public void notEnabled(ITextEditor editor) {
			// we've run out of redo, transition to undo
			state = undo;
			undoRedo(editor);
		}
		
		public String getCommandId() {
			return EMP_REDO;
		}
		
		public String toString() {
			return REDO_STATUS;
		}
	};
}
