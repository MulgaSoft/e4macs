/**
 * Copyright (c) 2009, 2010 Mark Feber, MulgaSoft
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package com.mulgasoft.emacsplus.commands;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.ui.texteditor.ITextEditor;

import com.mulgasoft.emacsplus.IEmacsPlusCommandDefinitionIds;

/**
 * Provide generic dispatch for mark/no mark movement behavior
 * 
 * @author Mark Feber - initial API and implementation
 */
public class ParagraphMarkForwardHandler extends EmacsMovementHandler {

	/**
	 * @see com.mulgasoft.emacsplus.commands.EmacsPlusCmdHandler#transform(ITextEditor, IDocument, ITextSelection, ExecutionEvent)
	 */
	@Override
	protected int transform(ITextEditor editor, IDocument document, ITextSelection currentSelection,
			ExecutionEvent event) throws BadLocationException {
		moveWithMark(editor, currentSelection, getNoSelectId(), getSelectId());;
		return NO_OFFSET;
	}
	
	protected String getSelectId() {
		return IEmacsPlusCommandDefinitionIds.SELECT_FORWARD_PARAGRAPH;
	}
	
	protected String getNoSelectId() {
		return IEmacsPlusCommandDefinitionIds.FORWARD_PARAGRAPH;
	}

}
