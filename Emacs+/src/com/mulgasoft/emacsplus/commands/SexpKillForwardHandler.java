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
import org.eclipse.ui.console.TextConsoleViewer;
import org.eclipse.ui.texteditor.ITextEditor;

import com.mulgasoft.emacsplus.MarkUtils;

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
		// clear any selection before embarking on the kill as depending on the selection, it's offset may not be the cursor offset
		ITextSelection sexpSelection = MarkUtils.setSelection(editor, getCursorOffset(editor), 0);
		int oldOffset = sexpSelection.getOffset();
		int newOffset = super.transform(editor, document, sexpSelection, event);
		this.updateText(document, oldOffset, Math.abs(newOffset-oldOffset), EMPTY_STR);
		return oldOffset;
	}
	
	/**
	 * Sexp kill commands require the offset, other sexp commands don't return it for performance reasons
	 *  
	 * @see com.mulgasoft.emacsplus.commands.SexpHandler#noSelectTransform(org.eclipse.ui.texteditor.ITextEditor, int, org.eclipse.jface.text.ITextSelection, boolean)
	 */
	protected int noSelectTransform(ITextEditor editor, int offset, ITextSelection selection, boolean moveit) {
		super.noSelectTransform(editor, offset, selection, moveit);
		return offset;
	}
	
	protected int noSelectTransform(TextConsoleViewer viewer, int offset, ITextSelection selection, boolean moveit) {
		super.noSelectTransform(viewer, offset, selection, moveit);
		return offset;
	}
	
	// Enable edit constraint	
	protected boolean isBlocked() {
		return !isEditable();
	}
}
