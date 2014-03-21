/**
 * Copyright (c) 2009-2011 Mark Feber, MulgaSoft
 *<p>
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

/**
 * Implements: pop-tag-mark
 *<p>
 * Each time you M-s or M-., in any buffer, the start location is recorded in the tag mark ring in 
 * addition to the current buffer's own mark ring
 *<p>
 * The command pop-tag-mark jumps to the buffer and position of the latest entry in the tag ring.
 * It also rotates the ring, so that successive uses of pop-tag-mark take you to earlier mark positions.
 *<p>
 * @author Mark Feber - initial API and implementation
 */
public class MarkTagPopHandler extends MarkGlobalHandler {

	/**
	 * @see com.mulgasoft.emacsplus.commands.EmacsPlusNoEditHandler#transform(ITextEditor, IDocument, ITextSelection, ExecutionEvent)
	 */
	@Override
	protected int transform(ITextEditor editor, IDocument document, ITextSelection currentSelection, ExecutionEvent event)
	throws BadLocationException {
		return doTransform(editor,document,currentSelection,true,true);
	}

}
