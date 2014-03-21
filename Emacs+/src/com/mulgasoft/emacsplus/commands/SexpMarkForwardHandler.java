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

import com.mulgasoft.emacsplus.IEmacsPlusCommandDefinitionIds;

/**
 * @author Mark Feber - initial API and implementation
 */
public class SexpMarkForwardHandler extends EmacsMovementHandler {

	/**
	 * @see com.mulgasoft.emacsplus.commands.EmacsPlusNoEditHandler#transform(ITextEditor, IDocument, ITextSelection, ExecutionEvent)
	 */
	@Override
	protected int transform(ITextEditor editor, IDocument document, ITextSelection currentSelection,
			ExecutionEvent event) throws BadLocationException {
		this.moveWithMark(editor, currentSelection, IEmacsPlusCommandDefinitionIds.FORWARD_SEXP, IEmacsPlusCommandDefinitionIds.SELECT_FORWARD_SEXP);
		return NO_OFFSET;
	}
	
	protected String getSelectId() {
		return IEmacsPlusCommandDefinitionIds.SELECT_FORWARD_SEXP;
	}
	
	protected String getNoSelectId() {
		return IEmacsPlusCommandDefinitionIds.FORWARD_SEXP;
	}
}
