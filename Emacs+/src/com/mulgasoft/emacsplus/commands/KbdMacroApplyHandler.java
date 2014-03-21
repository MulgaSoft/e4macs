/**
 * Copyright (c) 2009, 2010 Mark Feber, MulgaSoft
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package com.mulgasoft.emacsplus.commands;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.ui.texteditor.ITextEditor;

import com.mulgasoft.emacsplus.EmacsPlusUtils;
import com.mulgasoft.emacsplus.MarkUtils;

/**
 * Implements: apply-macro-to-region-lines 
 * (C-x C-k r)
 * 
 * Repeat the last defined keyboard macro on each line that begins in the region.
 * It does this line by line, by moving point to the beginning of the line
 * and then executing the macro.
 * 
 * @author Mark Feber - initial API and implementation
 */
public class KbdMacroApplyHandler extends KbdMacroExecuteHandler {

	private Selector selector = null; 
	
	protected void runMacro(final ITextEditor editor,final IDocument document,final ITextSelection selection, final KbdLock vkf, final int count,
			final String cmdId, final MacroCount keepCount) {
		selector = new Selector(selection,editor,document,cmdId, keepCount);		
		// selectNext changes selection in editor, so must be called within ui thread
		if (selector.selectNext()) {
			selector.startUndo();
			super.runMacro(editor, document, selector.getSelection(), vkf, count, cmdId, keepCount);
		}
	}
	
	/**
	 * @see com.mulgasoft.emacsplus.commands.KbdMacroExecuteHandler#executeOnce(ITextEditor, IDocument, ITextSelection, KbdMacroExecuteHandler.KbdLock)
	 */
	@Override
	protected void executeOnce(ITextEditor editor, IDocument document, ITextSelection currentSelection, final KbdLock vkf)
	throws BadLocationException {
		try {
			super.executeOnce(editor, document, null, vkf);
		} finally {
			EmacsPlusUtils.asyncUiRun(new Runnable() {
				public void run() {
					if (!isInterrupted()) {
						try {
							if (selector.selectNext()) {
								incrementExecutionCount();	// prepare for next execution
								pushExecution(selector.editor,vkf);	// re-register key listener as well
								KbdMacroApplyHandler.super.runMacro(selector.editor, selector.document, selector.getSelection(), vkf, 1, selector.cmdId,null);
							} else {
								selector.stopUndo();
							}
						} catch (Exception e) {
							EmacsPlusUtils.showMessage(selector.editor, KBD_INTERRUPTED, true);							
						}
					} else {
						selector.stopUndo();
					}
				}
			});
		}
	}

	/**
	 * Finesse undoProtect as we want an undo to apply to all iterations of the apply-macro
	 * 
	 * @see com.mulgasoft.emacsplus.commands.KbdMacroExecuteHandler#undoProtect(ITextEditor, KbdMacroExecuteHandler.MacroCount)
	 */
	protected Runnable[] undoProtect(ITextEditor editor, final MacroCount keepCount) {
		return new Runnable[2];
	}
	
	/**
	 * Utility class to keep track of the lines over which we're moving
	 */
	private class Selector {
		int begin;	// the first line
		int end;	// the last line
		ITextEditor editor;
		IDocument document;
		String cmdId;
		Runnable[] undo;
		
		Selector(ITextSelection selection, ITextEditor editor, IDocument document, String id, MacroCount keepCount) {
			int begin = selection.getOffset();
			int end = begin + selection.getLength();
			// get begin and end line information
			try {
				begin = document.getLineOfOffset(begin);
				int e = document.getLineOfOffset(end);
				// if the selection ends at the beginning of the last line, ignore it
				if (document.getLineOffset(e) == end) {
					--e;
				}
				end = e;
			} catch (BadLocationException e) {}
			this.cmdId = id;
			this.editor = editor;
			this.document = document;
			// set for pre-increment
			this.begin = --begin;
			this.end = end;
			undo = KbdMacroApplyHandler.super.undoProtect(editor, keepCount);			
		}
		
		void startUndo() {
			EmacsPlusUtils.asyncUiRun(undo[0]);			
		}
		
		void stopUndo() {
			EmacsPlusUtils.asyncUiRun(undo[1]);			
		}
		
		/**
		 * Move to the beginning of the next line in sequence
		 * 
		 * @return true on success, false when we've run out of lines 
		 */
		boolean selectNext() {
			boolean result = (++begin <= end);
			if (result) {
				getWorkbenchPage().activate(editor);
				try {
					MarkUtils.setCursorOffset(editor, document.getLineOffset(begin));					
				} catch (BadLocationException e) {
					result = false;
				}				
			}
			return result;
		}
		
		/**
		 * Get the current position as a selection
		 * @return current offset selection
		 */
		ITextSelection getSelection() {
			ITextSelection result = null;
			try {
				result =  new TextSelection(document,document.getLineOffset(begin),0); 
				} catch (BadLocationException e) {}
			return result;
		}
	};
}
