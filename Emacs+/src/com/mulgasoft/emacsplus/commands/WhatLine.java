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
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.ui.texteditor.ITextEditor;

import com.mulgasoft.emacsplus.EmacsPlusActivator;

/**
 * Implement: what-line
 * 
 * Print the current buffer line number and narrowed line number of point
 * 
 * @author Mark Feber - initial API and implementation
 */
public class WhatLine extends EmacsPlusNoEditHandler {


	private final static String LINE = EmacsPlusActivator.getResourceString("What_Line");   	   //$NON-NLS-1$
	private final static String H_LINE = EmacsPlusActivator.getResourceString("What_Narrow_Line"); //$NON-NLS-1$
	
	/**
	 * @see com.mulgasoft.emacsplus.commands.EmacsPlusNoEditHandler#transform(org.eclipse.ui.texteditor.ITextEditor, org.eclipse.jface.text.IDocument, org.eclipse.jface.text.ITextSelection, org.eclipse.core.commands.ExecutionEvent)
	 */
	@Override
	protected int transform(ITextEditor editor, IDocument document, ITextSelection currentSelection,
			ExecutionEvent event) throws BadLocationException {
		
		int nLine = document.getLineOfOffset(getCursorOffset(editor,currentSelection));
		String msg = String.format(LINE, nLine+1);
		if (editor.showsHighlightRangeOnly()) {
			int nhLine = nLine - document.getLineOfOffset(editor.getHighlightRange().getOffset());
			msg += String.format(H_LINE, nhLine+1); 
		}
		asyncShowMessage(editor, msg, false);
		return super.transform(editor, document, currentSelection, event);
	}
}
