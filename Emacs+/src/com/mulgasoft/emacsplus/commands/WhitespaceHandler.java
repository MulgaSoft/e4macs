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

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.ui.texteditor.ITextEditor;

/**
 * Handler for removing various kinds of whitespace 
 * 
 * @author Mark Feber - initial API and implementation
 */
public abstract class WhitespaceHandler extends EmacsPlusCmdHandler {

	/**
	 * Replace any whitespace around offset of the selection with the String replace
	 * @param editor
	 * @param document
	 * @param selection
	 * @param replace - the replacement string 
	 * @param ignoreCR - count over EOLs if true
	 * @return the offset at the end of the replacement
	 * @throws BadLocationException
	 */
	protected int transformSpace(ITextEditor editor, IDocument document, ITextSelection selection, String replace, boolean ignoreCR) throws BadLocationException {
		return transformSpace(editor,document,selection.getOffset(),replace,ignoreCR);
	}
	
	/**
	 * Replace any whitespace around offset with the String replace
	 * @param editor
	 * @param document
	 * @param offset
	 * @param replace - the replacement string 
	 * @param ignoreCR - count over EOLs if true
	 * @return the offset at the end of the replacement
	 * @throws BadLocationException
	 */
	protected int transformSpace(ITextEditor editor, IDocument document, int offset, String replace, boolean ignoreCR) throws BadLocationException {
		int left=countWS(document, offset-1, -1,ignoreCR);
		int right=countWS(document, offset, 1,ignoreCR);
		// When dealing with line removal, first line differs from remaining
		if (ignoreCR) {
			replace = ((offset-left == 0) ? replace : replace + getLineDelimiter() );
		}
		this.updateText(document, offset-left, right+left, replace);
		return offset-left+replace.length();
	}

	
	/**
	 * Count the whitespace to the left and right of offset, potentially counting over EOLs.
	 * @param document
	 * @param offset
	 * @param dir
	 * @param ignoreCR - count over EOLs if true
	 * @return the whitespace count
	 * @throws BadLocationException
	 */
	protected int countWS(IDocument document, int offset, int dir, boolean ignoreCR) throws BadLocationException {
		String eol = getLineDelimiter();
		int lineOff = offset;
		int off = offset;
		int lastOff = document.getLength(); // -1;	// n
		char c;
		while ((-1 < off && off < lastOff) && (c = document.getChar(off)) <= ' ') { 
			if (eol.indexOf(c) != -1) {
				if (!ignoreCR) {
					break;
				}
				// preserve the position past the last EOL
				lineOff = off + dir;
			}
			off = off + dir;
		}
		// if ignoreCR == true, then we're interested in complete blank lines only
		return Math.abs(offset - (ignoreCR ? lineOff : off));
	}
	
	/**
	 * Is the text represented by the region just whitespace?
	 * 
	 * @param document
	 * @param region
	 * @return true if just whitespace
	 * @throws BadLocationException
	 */
	protected boolean isBlankLine(IDocument document, IRegion region) throws BadLocationException{
		boolean result = false;
		int len = this.countWS(document, region.getOffset(), 1, false);
		result = (len >= region.getLength());
		return result;
	}
}