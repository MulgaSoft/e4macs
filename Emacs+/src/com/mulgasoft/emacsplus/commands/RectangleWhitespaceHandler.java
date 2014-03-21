/**
 * Copyright (c) 2009, 2010 Mark Feber, MulgaSoft
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package com.mulgasoft.emacsplus.commands;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.ui.texteditor.ITextEditor;

/**
 * Delete initial whitespace in each of the lines on the specified rectangle. (delete-whitespace-rectangle)
 * Delete all whitespace following a specified column in each line.
 * The left edge of the rectangle specifies the position in each line
 * at which whitespace deletion should begin.  On each line in the
 * rectangle, all continuous whitespace starting at that column is deleted.
 *  
 * @author Mark Feber - initial API and implementation
 */
public class RectangleWhitespaceHandler extends RectangleHandler {

	/**
	 * @see com.mulgasoft.emacsplus.commands.RectangleHandler#doTransform(org.eclipse.ui.texteditor.ITextEditor, org.eclipse.jface.text.IDocument, org.eclipse.jface.text.ITextSelection)
	 */
	@Override
	protected int doTransform(ITextEditor editor, IDocument document,
			ITextSelection currentSelection) throws BadLocationException {
		int result = NO_OFFSET;
		result = updateRectangle(editor, document, getImpliedSelection(editor,currentSelection), null, false, true);		
		return result;
	}

}
