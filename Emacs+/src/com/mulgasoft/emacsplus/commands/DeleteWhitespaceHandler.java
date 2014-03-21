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
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.jface.text.Position;

/**
 * Implement: delete-trailing-whitespace
 * 
 * Delete all the trailing whitespace across the current buffer.
 * All whitespace after the last non-whitespace character in a line is deleted.
 * This respects narrowing, created by C-x n n and friends.
 * 
 * This command differs from the Eclipse remove-trailing-whitespace by 
 * operating on only one file and respecting the narrowed region of a buffer.
 * 
 * @author Mark Feber - initial API and implementation
 */
public class DeleteWhitespaceHandler extends EmacsPlusCmdHandler {

	/**
	 * Determine the correct set of lines and delete the whitespace at the end of each of them.
	 * 
	 * @see com.mulgasoft.emacsplus.commands.EmacsPlusCmdHandler#transform(ITextEditor, IDocument, ITextSelection, ExecutionEvent)
	 */
	@Override
	protected int transform(ITextEditor editor, IDocument document, ITextSelection currentSelection,
			ExecutionEvent event) throws BadLocationException {
		int result = NO_OFFSET;
		int lastLine = 0;
		int firstLine = 0;
		int maxOffset = Integer.MAX_VALUE;		
		if (editor.showsHighlightRangeOnly()) {
			// if the buffer is narrowed, then operate on precise region
			IRegion narrow = editor.getHighlightRange();
			int lastOffset = narrow.getOffset() + narrow.getLength();
			firstLine = document.getLineOfOffset(narrow.getOffset()); 
			lastLine = document.getLineOfOffset(lastOffset);
			IRegion endInfo = document.getLineInformationOfOffset(lastOffset);
			if (endInfo.getOffset() != lastOffset) {
				// point maxOffset at the last character in the narrowed region
				maxOffset = lastOffset -1;
			} else {
				// back up if we're at the first offset of the last line
				lastLine--;
			}
		} else {
			lastLine = document.getNumberOfLines() -1;
		}
		if (firstLine <= lastLine) {
			Position coff = new Position(getCursorOffset(editor,currentSelection),0);
			try {
				// record the position so we can restore it after any whitespace deletions
				document.addPosition(coff);
				deleteWhitepace(lastLine,firstLine,maxOffset,document);
			} catch (BadLocationException e) {
				// shouldn't happen, but alert user if it does
				asyncShowMessage(editor,BAD_LOCATION_ERROR,true);
			} finally {
				result = coff.getOffset();
				document.removePosition(coff);
			}
		}
		return result;
	}

	/**
	 * Delete the whitespace at the end of each line from the bottom up
	 * 
	 * @param lastLine last line in the region
	 * @param firstLine first line in the region 
	 * @param maxOffset when narrowed, this is the last offset on the last line
	 * @param document the model
	 * @throws BadLocationException 
	 */
	private void deleteWhitepace(int lastLine, int firstLine, int maxOffset, IDocument document) throws BadLocationException {
		// bottoms up
		for (int i= lastLine; i >= firstLine; i--) {
			IRegion line= document.getLineInformation(i);
			if (line.getLength() == 0)
				continue;
			int lineStart= line.getOffset();
			int lineLen = line.getLength();
			int lineEnd = lineStart + lineLen;
			if (lineEnd > maxOffset) {
				lineLen -= (lineEnd - maxOffset) - 1;
				lineEnd = maxOffset;
			} else {
				lineEnd--;
			}
			int j= lineEnd;
			//				String t = document.get(lineStart,line.getLength());
			while (j >= lineStart && Character.isWhitespace(document.getChar(j))) --j;
			if (j < lineEnd) {
				String newText = document.get(lineStart,(j - lineStart)+1);
				updateText(document,lineStart,lineLen ,newText);
			}
		}
	}

	/**
	 * Force undo protect
	 * @see com.mulgasoft.emacsplus.commands.EmacsPlusCmdHandler#undoProtect()
	 */
	protected boolean undoProtect() {
		return true;
	}
	
}
