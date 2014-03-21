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
import org.eclipse.jface.text.TextSelection;
import org.eclipse.ui.texteditor.ITextEditor;

/**
 * Implements: transpose-characters
 * 
 * Interchange characters around point, moving forward one character.
 * With prefix ARG, effect is to take character before point
 * and drag it forward past ARG other characters (backward if ARG negative).
 * If no argument and at end of line, the previous two chars are exchanged.
 *  
 * @author Mark Feber - initial API and implementation
 */
public class TransposeCharHandler extends EmacsPlusCmdHandler {

	/**
	 * @see com.mulgasoft.emacsplus.commands.EmacsPlusCmdHandler#transform(ITextEditor, IDocument, ITextSelection, ExecutionEvent)
	 */
	@Override
	protected int transform(ITextEditor editor, IDocument document, ITextSelection currentSelection, ExecutionEvent event) throws BadLocationException {
		
		IRegion reg = document.getLineInformationOfOffset(currentSelection.getOffset());
		int coff = currentSelection.getOffset();
		int lineNo = document.getLineOfOffset(coff);
		int firstLen = 1;
		int secondLen = 1;
		ITextSelection xelection;
		int uarg = getUniversalCount();
		int eolLen = getLineDelimiter().length();
		
		// take character before point and drag it backward past ARG other characters if ARG negative).
		if (uarg < 0) {
			if (coff == reg.getOffset()) {
				// back up over eol
				coff -= eolLen;
				lineNo = document.getLineOfOffset(coff);
			} else {
				// back up one character
				--coff;
			}
		}
		
		if (coff == reg.getOffset()) {	// handle beginning of a line
			// if at the beginning of a line, make sure we're not at the beginning of the file
			if (lineNo == 0){
				return currentSelection.getOffset();
			}
			// back over line delimiter
			firstLen = eolLen;
		} 
		if (coff == reg.getOffset()+reg.getLength()){	// handle end of a line
			if (isUniversalPresent() || reg.getLength() < 2) {
				// if at the end of a line move across line delimiter
				String eol = document.getLineDelimiter(lineNo);
				// unless at end of file
				if (eol == null) {
					return currentSelection.getOffset();
				}
				secondLen = eol.length();
			} else {
				//  If no argument and at end of line, the previous two chars are exchanged.
				--coff;
			}
		} 
		xelection = new TextSelection(document,coff-firstLen,firstLen+secondLen);
		updateText(document,xelection,xelection.getText().substring(firstLen)+xelection.getText().substring(0,firstLen));
		int result = xelection.getOffset() + xelection.getLength();
		
		if (uarg < 0 && result != 0) {
			// adjust position of result when backing up
			IRegion line = document.getLineInformationOfOffset(result);
			if (result == line.getOffset() || line.getLength() == 0) {
				result -= eolLen;
			} else {
				--result;
			}
		}
		return result; 
	}

}
