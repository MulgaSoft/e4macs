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
import org.eclipse.ui.texteditor.ITextEditor;

/**
 * Implements: delete-blank-lines. 
 * 
 *  On blank line, delete all surrounding blank lines, leaving just one.
 *  On isolated blank line, delete that one.
 *  On non-blank line, delete any immediately following blank lines.
 * 
 * @author Mark Feber - initial API and implementation
 *
 */
public class BlankLineHandler extends WhitespaceHandler {

	/* (non-Javadoc)
	 * @see com.mulgasoft.emacsplus.commands.AbstractCmdHandler#transform(org.eclipse.ui.texteditor.ITextEditor, org.eclipse.jface.text.IDocument, org.eclipse.jface.text.ITextSelection, org.eclipse.core.commands.ExecutionEvent)
	 */
	@Override
	protected int transform(ITextEditor editor, IDocument document, ITextSelection currentSelection,
			ExecutionEvent event) throws BadLocationException {

		int offset = currentSelection.getOffset();
		int lineNum = document.getLineOfOffset(offset);
		boolean nextBlank = (lineNum < document.getNumberOfLines() -1) && isBlankLine(document,document.getLineInformation(lineNum + 1)); 
		if (!isBlankLine(document, document.getLineInformationOfOffset(offset))) {	// on non-blank line
			if (nextBlank) {
				// remove any following blank lines
				offset = document.getLineOffset(document.getLineOfOffset(offset) + 1);
				transformSpace(editor, document, offset, EMPTY_STR, true);
			}
			return NO_OFFSET; 	// leave cursor in place
		} else {
			// if at multiple blank lines
			if (nextBlank || (lineNum != 0 && isBlankLine(document, document.getLineInformation(lineNum - 1)))) {
				// leave a blank line
				offset = transformSpace(editor, document, offset, getLineDelimiter(), true);
				// position cursor at the beginning the blank line 
				return document.getLineInformation(document.getLineOfOffset(offset) -1).getOffset();
			} else {	// single blank line
				// remove the blank line
				return transformSpace(editor, document, offset, EMPTY_STR, true);
			}
		}
	}
	
}
