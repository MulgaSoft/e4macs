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
 * Implements: forward-paragraph
 * 
 * Move forward to end of paragraph
 * 
 * @author Mark Feber - initial API and implementation
 */
public class ParagraphForwardHandler extends ParagraphMovementHandler {

	protected int getParagraphOffset(ITextEditor editor, IDocument document, ITextSelection selection) {
		int result = NO_OFFSET;
		try {
			int line = document.getLineOfOffset(getCursorOffset(editor, selection)); 
			// are we at the beginning of a multi-line blank section?
			if (isBlank(document,line) && isBlank(document,line+1)) {
				// then jump over it
				result = getParagraphOffset(editor,true);				
			}
			result = getParagraphOffset(editor,true);
		} catch (BadLocationException e) {
			// should only happen if we're already at the end
		}
		return result;
	}
}
