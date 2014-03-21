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
 * Implement: split-line
 * Split current line, moving portion beyond point vertically down
 * 
 * @author Mark Feber - initial API and implementation
 *
 */
public class SplitLineHandler extends LineHandler {

	/**
	 * @see com.mulgasoft.emacsplus.commands.EmacsPlusCmdHandler#transform(org.eclipse.ui.texteditor.ITextEditor, org.eclipse.jface.text.IDocument, org.eclipse.jface.text.ITextSelection, org.eclipse.core.commands.ExecutionEvent)
	 */
	@Override
	protected int transform(ITextEditor editor, IDocument document, ITextSelection currentSelection,
			ExecutionEvent event) throws BadLocationException {
		String eol = getLineDelimiter();
		StringBuilder buf = new StringBuilder(eol);
		int offset = currentSelection.getOffset();
		IRegion reg = document.getLineInformationOfOffset(offset);
		buf.append(collectWS(document,offset,(offset - reg.getOffset())));
		updateText(document, offset, 0, buf.toString());
		return NO_OFFSET;
	}	// 

	private String collectWS(IDocument document, int off, int count) throws BadLocationException {
		StringBuilder result = new StringBuilder();
		String eol = getLineDelimiter();
		char c;
		// start looking at the first previous char from offset
		while ((-1 < --off) && count > 0 && (c = document.getChar(off)) != -1) {
			if (eol.indexOf(c) != -1) {
				// ignore any eol chars
			} else if (c <= ' ') {
				result.append(c);
			} else {
				result.append(' ');
			}
			--count;
		}
		return result.reverse().toString();
	}
}
