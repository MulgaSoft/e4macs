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

import com.mulgasoft.emacsplus.minibuffer.ISearchMinibuffer;

/**
 * Implements: isearch-backward-regexp 
 * 
 * @author Mark Feber - initial API and implementation
 */
public class ISearchRegexpBackwardHandler extends MinibufferHandler implements INonEditingCommand {

	/**
	 * @see com.mulgasoft.emacsplus.commands.EmacsPlusCmdHandler#transform(ITextEditor, IDocument, ITextSelection, ExecutionEvent)
	 */
	@Override
	protected int transform(final ITextEditor editor, IDocument document, ITextSelection currentSelection,
			final ExecutionEvent event) throws BadLocationException {
		
		return bufferTransform(new ISearchMinibuffer(false, true), editor, event); 		
	}
}
