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
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.ui.texteditor.ITextEditor;

import com.mulgasoft.emacsplus.MarkUtils;

/**
 * Implements: mark-paragraph
 * 
 * Put point and mark around this or next paragraph
 * 
 * If current selection is empty and inside a paragraph, mark the enclosing paragraph,
 * else the following or preceding one(s) depending on the value of the universal arg. 
 * If current selection is not empty, mark the following, or preceding paragraph (or 
 * paragraph section, if the entire paragraph is not currently selected). 
 * 
 * @author Mark Feber - initial API and implementation
 */
public class ParagraphMarkHandler extends ParagraphHandler {

	/**
	 * @see com.mulgasoft.emacsplus.commands.EmacsPlusCmdHandler#transform(ITextEditor, IDocument, ITextSelection, ExecutionEvent)
	 */
	@Override
	protected int transform(ITextEditor editor, IDocument document, ITextSelection currentSelection,
			ExecutionEvent event) throws BadLocationException {
		boolean forward = getUniversalCount() > 0;
		
		if (forward && isAtBottom(editor,document,currentSelection)) {
			// nowhere to go & Emacs doesn't beep in this state
			// Emacs actually selects the previous paragraph in this state which seems wrong
		} else if (!forward && isAtTop(editor,currentSelection)) {
			// nowhere to go & Emacs doesn't beep in this state
			// Emacs actually selects the following paragraph in this state which seems wrong
		} else if (currentSelection.getLength() == 0) {
			// select entire paragraph
			int line = document.getLineOfOffset(getCursorOffset(editor,currentSelection)); 
			// are we inside a paragraph?
			if (!isBlank(document,line)) {
				int begin = (forward ? getParagraphOffset(editor,false) : getParagraphOffset(editor,true));
				setSelection(editor,currentSelection);	// restore for movement in reverse direction
				int end = (forward ? getParagraphOffset(editor,true) : getParagraphOffset(editor,false));
				// invert selection so cursor appears at correct end
				ITextSelection selection = new TextSelection(document, end, begin - end);
				setMark(editor,end);
				setSelection(editor,selection);
				MarkUtils.revealRange(editor, end, 0);
			} else {
				setMark(editor,currentSelection.getOffset());
				nextParagraph(editor, document, currentSelection, forward);
			}
		} else {
			nextParagraph(editor, document, currentSelection, forward);
		}
		return NO_OFFSET;
	}

	private void nextParagraph(ITextEditor editor, IDocument document, ITextSelection selection, boolean forward) {
		int cursorOffset = MarkUtils.getCursorOffset(editor);
		int offset = NO_OFFSET;
		MarkUtils.setSelection(editor,getMark(editor),0);
		// select next in appropriate direction
		try {
			if (forward) {
				int line = document.getLineOfOffset(getCursorOffset(editor,selection)); 
				// are we at the beginning of a multi-line blank section?
				if (isBlank(document,line) && isBlank(document,line+1)) {
					// then jump over it
					getParagraphOffset(editor,true);				
				}
				offset = getParagraphOffset(editor,true);
			} else {
				offset = getParagraphOffset(editor,false);
			}
		} catch (BadLocationException e) {
			// should only happen if we're already at the end
			// restore the selection highlight
			reverseSelection(editor,document,selection);
		} catch (IndexOutOfBoundsException e) {
			// should only happen if we've moved to the top
			// work around bug in org.eclipse.jface.text.TextViewer.findAndSelect()
			// where it doesn't check for 0 offset & 0 length  
			offset = 0;
		}
		if (offset != NO_OFFSET) {
			setMark(editor,offset,false);
			setSelection(editor,new TextSelection(document, offset, (cursorOffset - offset)));
			MarkUtils.revealRange(editor, offset, 0);
		}
	}
	
	private void reverseSelection(ITextEditor editor, IDocument document, ITextSelection selection) {
		// invert selection so cursor appears at correct end
		int end = selection.getOffset() + selection.getLength();
		setSelection(editor,new TextSelection(document,end,-selection.getLength()));
	}
}
