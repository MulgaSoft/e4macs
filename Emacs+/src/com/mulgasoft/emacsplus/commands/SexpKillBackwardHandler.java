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
 * Implements: backward-kill-sexp
 * 
 * Kill one s-expression backwards
 * 
 * @author Mark Feber - initial API and implementation
 */
public class SexpKillBackwardHandler extends SexpBackwardHandler {
	
	/**
	 * @see com.mulgasoft.emacsplus.commands.SexpBaseBackwardHandler#transform(ITextEditor, IDocument, ITextSelection, ExecutionEvent)
	 */
	protected int transform(ITextEditor editor, IDocument document, ITextSelection currentSelection, ExecutionEvent event) throws BadLocationException {
		int oldOffset = currentSelection.getOffset();
		int newOffset = super.transform(editor, document, currentSelection, event);
		this.updateText(document, newOffset, Math.abs(oldOffset-newOffset), EMPTY_STR);
		return newOffset;
	}
	// Enable edit constraint
	protected boolean isBlocked() {
		return !isEditable();
	}
}
