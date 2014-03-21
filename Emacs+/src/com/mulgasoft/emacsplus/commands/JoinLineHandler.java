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
 * Implement join-line: Join this line to previous and fix up whitespace at join.
 * If there is a fill prefix, delete it from the beginning of this line.
 * With ^U, join this line to following line.
 *
 * @author Mark Feber - initial API and implementation
 */
public class JoinLineHandler extends WhitespaceHandler {

	/**
	 * @see com.mulgasoft.emacsplus.commands.EmacsPlusCmdHandler#transform(ITextEditor, IDocument, ITextSelection, ExecutionEvent)
	 */
	@Override
	protected int transform(ITextEditor editor, IDocument document, ITextSelection currentSelection, ExecutionEvent event)
	throws BadLocationException {
		int result = NO_OFFSET;
		boolean isNormal = (getUniversalCount() == 1);
		int line = document.getLineOfOffset(getCursorOffset(editor,currentSelection));
		// check buffer location of line
		if ((isNormal ? line != 0 : ++line < document.getNumberOfLines()-1)) {
			int eolLen = document.getLineDelimiter(line-1).length();
			IRegion region = document.getLineInformation(line);
			// position between lines
			int joinOffset = region.getOffset() - eolLen;
			selectAndReveal(editor, joinOffset, joinOffset);
			document.replace(joinOffset, eolLen, EMPTY_STR);
			// TODO: need semantic decision here based on content of join
			result = transformSpace(editor, document, joinOffset, SPACE_STR, false);
		} 
		return result;
	}

	/**
	 * @see com.mulgasoft.emacsplus.commands.EmacsPlusCmdHandler#isLooping()
	 */
	@Override
	protected boolean isLooping() {
		//With ^U, join this line to following line.
		return false;
	}
	
	/**
	 * @see com.mulgasoft.emacsplus.commands.EmacsPlusCmdHandler#undoProtect()
	 */
	@Override
	protected boolean undoProtect() {
		return true;
	}

}
