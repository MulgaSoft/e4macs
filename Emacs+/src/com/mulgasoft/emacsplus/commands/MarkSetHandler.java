/**
 * Copyright (c) 2009, 2010, Mark Feber, MulgaSoft
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
import org.eclipse.jface.text.Position;
import org.eclipse.ui.console.IConsoleView;
import org.eclipse.ui.console.TextConsoleViewer;
import org.eclipse.ui.texteditor.ITextEditor;

import com.mulgasoft.emacsplus.MarkUtils;

/**
 * Implements: set-mark-command
 * 
 * Correct the Eclipse behavior of set-mark
 * 
 * - Clears any previous selection
 * - Supports pushing and popping of mark
 *  
 * @author Mark Feber - initial API and implementation
 */
public class MarkSetHandler extends EmacsPlusNoEditHandler implements IConsoleDispatch {

	/**
	 * @see com.mulgasoft.emacsplus.commands.EmacsPlusCmdHandler#transform(ITextEditor, IDocument, ITextSelection, ExecutionEvent)
	 */
	@Override
	protected int transform(ITextEditor editor, IDocument document, ITextSelection currentSelection, ExecutionEvent event) 
	throws BadLocationException {
		int offset = getCursorOffset(editor,currentSelection);
		try {
			boolean hadSel = currentSelection.getLength() > 0;
			boolean markState = false;  		 // deactivates mark
			switch (getShift(event)) {
				case CLEAR:
					MarkUtils.setSelection(editor,offset,0);
					break;
				case SET:
					MarkUtils.setSelection(editor,offset,0);
					setMark(editor,offset);
					markState = true;
					break;
				default: 
					EmacsMovementHandler.clearShifted(); // normal set mark always removes shift select flag
					if (hadSel) {
						// this will remove any selection, so internal mark code won't get confused
						setCursorOffset(editor,offset);
					}
					if (getUniversalCount() >= 4) {
						// Pop the new mark and use the old mark as pos 
						Position markAndPos = MarkUtils.popMark(editor,document);
						if (markAndPos != null) {
							int pos = markAndPos.getOffset();
							selectAndReveal(editor,pos,pos);
							setMark(editor,markAndPos.getLength(),false);	// set it as current mark, but don't save again
						}
					} else if (getUniversalCount() > 1) {
						// command version of push-mark without activating
						setMark(editor,offset);	// command version of C-<SPC> C-<SPC>
					} else {
						if (!isFlagMark()) {
							setMark(editor,offset);
						}
						//if called with C-<SPC> C-<SPC>: Set the mark, pushing it onto the mark ring, without activating it.
						markState = (hadSel || !isFlagMark());
					}
			}
			setFlagMark(markState);
		} catch (Exception e) {}
		return NO_OFFSET;
	}

	private ShiftState getShift(ExecutionEvent event) {
		try {
			String shiftArg = event.getParameter(SHIFT_ARG);
			if (shiftArg != null && shiftArg.length() > 0) {
				return ShiftState.valueOf(shiftArg);
			}
		} catch (Exception e) {}	//ignore  
		return ShiftState.NONE;
	}
	
	/**
	 * @see com.mulgasoft.emacsplus.commands.EmacsPlusCmdHandler#isLooping()
	 */
	@Override
	protected boolean isLooping() {
		return false;
	}

	/**
	 * Support simple mark on TextConsole
	 * 
	 * @see com.mulgasoft.emacsplus.commands.IConsoleDispatch#consoleDispatch(TextConsoleViewer, IConsoleView, ExecutionEvent)
	 */
	public Object consoleDispatch(TextConsoleViewer viewer, IConsoleView activePart, ExecutionEvent event) {
		int offset = viewer.getTextWidget().getCaretOffset();
		viewer.setSelectedRange(offset, 0);
		viewer.setMark(offset);
		return null;
	}	
}
