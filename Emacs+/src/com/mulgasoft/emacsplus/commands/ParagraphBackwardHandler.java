/**
 * Copyright (c) 2009, 2010 Mark Feber, MulgaSoft
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package com.mulgasoft.emacsplus.commands;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.ui.texteditor.ITextEditor;

/**
 * Implements: backward-paragraph
 * 
 * Move backward to start of paragraph.
 * 
 * @author Mark Feber - initial API and implementation
 */
public class ParagraphBackwardHandler extends ParagraphMovementHandler {

	protected int getParagraphOffset(ITextEditor editor, IDocument document, ITextSelection selection) {
		int result = NO_OFFSET;
		// if we're not at buffer top (or top of narrowed region)
		if (!isAtTop(editor,selection)) {
			try {
				result = getParagraphOffset(editor,false);
			} catch (IndexOutOfBoundsException e) {
				// work around bug in org.eclipse.jface.text.TextViewer.findAndSelect()
				// where it doesn't check for 0 offset & 0 length  
				result = 0;
			}		
		}
		return result;
	}

}
