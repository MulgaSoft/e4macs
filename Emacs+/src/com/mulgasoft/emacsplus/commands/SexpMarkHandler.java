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
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.ui.texteditor.ITextEditor;

import com.mulgasoft.emacsplus.MarkUtils;

/**
 * Implements: mark-sexp. 
 * Select one s-expression forward without moving point
 * If called with negative arg, mark-sexps backward
 *  
 * @author Mark Feber - initial API and implementation
 */
public class SexpMarkHandler extends SexpForwardHandler {

	/**
	 * @see com.mulgasoft.emacsplus.commands.EmacsPlusCmdHandler#transform(ITextEditor, IDocument, ITextSelection, ExecutionEvent)
	 */
	@Override
	protected int transform(ITextEditor editor, IDocument document, ITextSelection currentSelection, ExecutionEvent event)
	throws BadLocationException {
		markSexp(editor,document,currentSelection,false);
		return NO_OFFSET;
	}

	protected void markSexp(ITextEditor editor, IDocument document, ITextSelection currentSelection, boolean wordp) throws BadLocationException {
		boolean backp = getUniversalCount() < 0; 
		// remember where we came from
		int begOffset = currentSelection.getOffset();
		int cursorOffset = MarkUtils.getCursorOffset(editor); 
		int mark;
		ITextSelection selection = currentSelection;
		if (selection.getLength() == 0) {
			// set the mark at the beginning of the first sexp
			mark = setMark(editor,cursorOffset);
		} else {
			mark = getMark(editor);
		}
		try {
			selection = new TextSelection(document,((mark < cursorOffset) ? mark : begOffset+selection.getLength()), 0);
			if (backp) {
				selection = new SexpBackwardHandler().getNextSexp(document, selection, wordp);
			} else {
				selection = getNextSexp(document, selection, wordp);
			}
		} catch (BadLocationException e) {
		}
		if (selection == null) {
			unbalanced(editor,true);
			throw new BadLocationException();
		}
		// compute the entire region and select it
		int endOffset = (backp ? selection.getOffset() : selection.getOffset()+selection.getLength());
		selection = new TextSelection(document, endOffset, (cursorOffset - endOffset)); 
		// set new mark, but don't save previous mark to mark ring
		setMark(editor,selection.getOffset(),false);
		setSelection(editor,selection);
		// and make sure the end is visible
		MarkUtils.revealRange(editor, endOffset, 0);
	}
}
