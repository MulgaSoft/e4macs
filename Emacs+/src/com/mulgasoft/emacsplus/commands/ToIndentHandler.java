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
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.ui.texteditor.ITextEditor;

/**
 * Implements: back-to-indentation
 * 
 * M-m: This command, given anywhere on a line, positions point 
 * at the first nonblank character on the line, if any, 
 * or else at the end of the line.
 * 
 * @author Mark Feber - initial API and implementation
 */

// TODO for later
//  backward-to-indentation       M-x ... RET
//     Move backward ARG lines and position at first non-blank character.
//  forward-to-indentation        M-x ... RET
//     Move forward ARG lines and position at first non-blank character.
// 
public class ToIndentHandler extends EmacsMovementHandler {

	/**
	 * Look for first non-whitespace character on the line and move to it
	 * 
	 * @see com.mulgasoft.emacsplus.commands.EmacsPlusNoEditHandler#transform(org.eclipse.ui.texteditor.ITextEditor, org.eclipse.jface.text.IDocument, org.eclipse.jface.text.ITextSelection, org.eclipse.core.commands.ExecutionEvent)
	 */
	@Override
	protected int transform(ITextEditor editor, IDocument document, ITextSelection currentSelection,
			ExecutionEvent event) throws BadLocationException {

		int offset = getCursorOffset(editor,currentSelection);
		IRegion linfo = document.getLineInformationOfOffset(offset);
		int llen = linfo.getLength();
		String line = document.get(linfo.getOffset(), llen);
		int index = 0;
		while (index < llen && Character.isWhitespace(line.charAt(index))) {
			index++;
		}
		int newOffset = linfo.getOffset() + index;
		// don't move if we're at the same offset 
		if (newOffset != offset) { 
			// if past the last character on the line, set to end
			if (index == llen) {
				--index;
			}
			setCursorOffset(editor, newOffset);
			if (isMarkEnabled(editor, currentSelection)) {
				// set new point/mark selection when mark enabled
				int mark = getMark(editor);
				selectAndReveal(editor,newOffset,mark);
				setFlagMark(mark - newOffset == 0);
			}
		}
		return NO_OFFSET;
	}
}
