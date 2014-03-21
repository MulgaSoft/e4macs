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
 * Implements: pop-global-mark
 * 
 * Each time you set a mark, in any buffer, this is recorded in the global mark ring in addition to the
 * current buffer's own mark ring, if the buffer is not at the top of the global mark ring already.
 * 
 * The command pop-global-mark jumps to the buffer and position of the latest entry in the global ring.
 * It also rotates the ring, so that successive uses of pop-global-mark take you to earlier buffers and 
 * mark positions.
 * 
 * @author Mark Feber - initial API and implementation
 */
public class MarkGlobalPopHandler extends MarkGlobalHandler {

	/**
	 * @see com.mulgasoft.emacsplus.commands.EmacsPlusNoEditHandler#transform(ITextEditor, IDocument, ITextSelection, ExecutionEvent)
	 */
	@Override
	protected int transform(ITextEditor editor, IDocument document, ITextSelection currentSelection, ExecutionEvent event)
	throws BadLocationException {
		return doTransform(editor,document,currentSelection,true,false);
	}
}
