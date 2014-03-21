/**
 * Copyright (c) 2009, Mark Feber, MulgaSoft
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

public class PAppendCommentHandler extends AppendCommentHandler {

	protected int transform(ITextEditor editor, IDocument document, ITextSelection currentSelection, ExecutionEvent event)
			throws BadLocationException {
		int currentLine = currentSelection.getStartLine();
		currentLine = currentLine -1;
		if (currentLine < 0){
			throw new BadLocationException();
		}
		IRegion reg = document.getLineInformation(currentLine);
		ITextSelection newSelection = new TextSelection(document,reg.getOffset(), 0);
		return super.transform(editor,document, newSelection, event);
	}
}
