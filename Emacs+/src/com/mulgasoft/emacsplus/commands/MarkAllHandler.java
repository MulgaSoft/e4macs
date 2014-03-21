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

/**
 * Implements: mark-whole-buffer
 * 
 * Add mark behavior to select all
 * Select the buffer, point at beginning and mark at end
 *  
 * @author Mark Feber - initial API and implementation
 */
public class MarkAllHandler extends EmacsPlusNoEditHandler {

	/**
	 * @see com.mulgasoft.emacsplus.commands.EmacsPlusNoEditHandler#transform(ITextEditor, IDocument, ITextSelection, ExecutionEvent)
	 */
	@Override
	protected int transform(ITextEditor editor, IDocument document, ITextSelection currentSelection, ExecutionEvent event)
	throws BadLocationException {
		setMark(editor);								  // first remember our start
		setMark(editor, document.getLength());  		  // then mark the bottom of the buffer
		selectAndReveal(editor, 0, document.getLength()); // and set point at the top
		return NO_OFFSET;
	}
}
