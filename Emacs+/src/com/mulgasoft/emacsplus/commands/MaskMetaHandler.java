/**
 * Copyright (c) 2026 Mark Feber, MulgaSoft
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
 * Fake out the Eclipse Meta key handler on Windows in the case where it is not already bound to a command.
 * This is a dummy handler that is never called despite being bound to the Meta-P/N pair of key bindings
 * (they are only enabled when the com.mulgasoft.emacsPlus.minibufferMeta context is activated).
 * It appears to work as it causes Eclipse to think that there is a valid binding for the pair, so that 
 * it dispatches the key VerifyEvent to the WithMinibuffer VerifyKeyListener which dispatches on to the 
 * appropriate subclass to use in traversing its history RingBuffer.
 * 
 * @author Mark Feber - initial API and implementation
 */
public class MaskMetaHandler extends EmacsPlusNoEditHandler {
	/**
	 * @see com.mulgasoft.emacsplus.commands.EmacsPlusCmdHandler#transform(ITextEditor, IDocument, ITextSelection, ExecutionEvent)
	 */
	@Override
	protected int transform(ITextEditor editor, IDocument document, ITextSelection currentSelection, ExecutionEvent event) 
	throws BadLocationException {
		return super.transform(editor, document, currentSelection, event);
	}
	

}
