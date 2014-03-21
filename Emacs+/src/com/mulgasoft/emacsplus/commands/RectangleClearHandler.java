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
 * Clear the region-rectangle by replacing all of its contents with spaces (`clear-rectangle').
 * 
 * @author Mark Feber - initial API and implementation
 */
public class RectangleClearHandler extends RectangleHandler {

	/* (non-Javadoc)
	 * @see com.mulgasoft.emacsplus.commands.AbstractCmdHandler#transform(org.eclipse.ui.texteditor.ITextEditor, org.eclipse.jface.text.IDocument, org.eclipse.jface.text.ITextSelection, org.eclipse.core.commands.ExecutionEvent)
	 */
	@Override
	protected int doTransform(ITextEditor editor, IDocument document,
			ITextSelection currentSelection)
			throws BadLocationException {
		int result = NO_OFFSET;
		result = updateRectangle(editor, document, getImpliedSelection(editor,currentSelection), null, true);		
		return result;
	}
}
