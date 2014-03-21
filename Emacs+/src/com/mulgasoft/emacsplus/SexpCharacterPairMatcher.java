/**
 * Copyright (c) 2009, 2010 Mark Feber, MulgaSoft
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 */
package com.mulgasoft.emacsplus;

import java.util.List;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.Region;

/**
 * Ensure any matching bracket stays within the comment or string if necessary
 * Also, invalidate character constants (using java syntax) as candidates for a match
 * 
 * @author Mark Feber - initial API and implementation
 */
public class SexpCharacterPairMatcher extends HackDefaultCharacterPairMatcher {

	private static char CQUOTE_CHAR = '\'';
	private Position exPosition = null;
	private List<Position> exPositions = null;
	
	public SexpCharacterPairMatcher(char[] chars) {
		super(chars);
	}

	protected synchronized IRegion performMatch(IDocument doc, int caretOffset) throws BadLocationException {
		int charOffset = caretOffset - 1;
		boolean forward = fPairs.isStartCharacter(doc.getChar(charOffset));
		// simple documents (e.g. .txt) don't have type categories
		if (EmacsPlusUtils.getTypeCategory(doc) != null) {
			exPositions = EmacsPlusUtils.getExclusions(doc, EmacsPlusUtils.ALL_POS);
			// remember category position if first character is in a comment or string
			exPosition = EmacsPlusUtils.inPosition(doc, exPositions, charOffset);
		}
		IRegion reg = super.performMatch(doc, caretOffset);
		// if we started in a category position, make sure we end in the same one
		if (reg == null || 
			(exPosition != null && !exPosition.includes(forward ? reg.getOffset() + reg.getLength() : reg.getOffset()))) {
			return new Region(charOffset, 1);	
		}
		return reg;
	}

	/**
	 * Searches <code>doc</code> for the specified end character, <code>end</code>.
	 * 
	 * @param doc the document to search
	 * @param start the opening matching character
	 * @param end the end character to search for
	 * @param searchForward search forwards or backwards?
	 * @param boundary a boundary at which the search should stop
	 * @param startPos the start offset
	 * @return the index of the end character if it was found, otherwise -1
	 * @throws BadLocationException
	 */
	protected int findMatchingPeer(DocumentPartitionAccessor doc, char start, char end, boolean searchForward, int boundary, int startPos) throws BadLocationException {
		int pos= startPos;
		while (pos != boundary) {
			final char c= doc.getChar(pos);
			if (doc.isMatch(pos, end) && !isExcluded(doc.getDocument(),pos,false)) {
				return pos;
			} else if (c == start && doc.inPartition(pos) && !isExcluded(doc.getDocument(),pos,searchForward)) {
				pos= findMatchingPeer(doc, start, end, searchForward, boundary,
						doc.getNextPosition(pos, searchForward));
				if (pos == -1) return -1;
			}
			pos= doc.getNextPosition(pos, searchForward);
		}
		return -1;
	}
	
	/**
	 * Don't match with bracket if it is a character constant
	 * 
	 * @param doc
	 * @param offset
	 * @param initOffset
	 * 
	 * @return true if character at offset is not just a character constant
	 */
	private boolean isExcluded(IDocument doc,int offset,boolean initOffset) {
		// if we didn't start in a string or comment, exclude any in a string or comment
		if (exPosition == null && exPositions != null && EmacsPlusUtils.inPosition(doc, exPositions, offset)!= null){
			return true;
		}		
		try {
			// don't match bracket if it is a character constant
			if (doc.getChar(offset-1) == CQUOTE_CHAR && doc.getChar(offset+ 1) == CQUOTE_CHAR){
				return true;
			}
		} catch (BadLocationException e) {}
		return false;
	}
}
