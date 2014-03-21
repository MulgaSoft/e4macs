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
 * Implements: forward-kill-sexp
 * 
 * Kill one s-expression forwards
 * 
 * @author Mark Feber - initial API and implementation
 */
public class SexpKillForwardHandler extends SexpForwardHandler {
	
	/**
	 * @see com.mulgasoft.emacsplus.commands.SexpBaseForwardHandler#transform(ITextEditor, IDocument, ITextSelection, ExecutionEvent)
	 */
	protected int transform(ITextEditor editor, IDocument document, ITextSelection currentSelection, ExecutionEvent event) throws BadLocationException {
		int oldOffset = currentSelection.getOffset();
		int newOffset = super.transform(editor, document, currentSelection, event);
		this.updateText(document, oldOffset, Math.abs(newOffset-oldOffset), EMPTY_STR);
		return oldOffset;
	}
	
	// Enable edit constraint	
	protected boolean isBlocked() {
		return !isEditable();
	}
}
