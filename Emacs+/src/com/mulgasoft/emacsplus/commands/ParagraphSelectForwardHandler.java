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
 * @author Mark Feber - initial API and implementation
 */
public class ParagraphSelectForwardHandler extends ParagraphForwardHandler {

	/**
	 * @see com.mulgasoft.emacsplus.commands.ParagraphMovementHandler#doTransform(ITextEditor, IDocument, ITextSelection, boolean)
	 */
	@Override
	protected int doTransform(ITextEditor editor, IDocument document, ITextSelection selection, boolean isMark)
			throws BadLocationException {
		return super.doTransform(editor, document, selection, true);
	}

}
