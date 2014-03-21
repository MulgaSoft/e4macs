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
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.ui.texteditor.ITextEditor;

import com.mulgasoft.emacsplus.BufferLocal;

/**
 * Implements: widen. Remove the restrictions (narrowing) from the buffer
 * 
 * @author Mark Feber - initial API and implementation
 */
public class RegionWidenHandler extends EmacsPlusNoEditHandler {

	/**
	 * @see com.mulgasoft.emacsplus.commands.EmacsPlusNoEditHandler#transform(org.eclipse.ui.texteditor.ITextEditor, org.eclipse.jface.text.IDocument, org.eclipse.jface.text.ITextSelection, org.eclipse.core.commands.ExecutionEvent)
	 */
	@Override
	protected int transform(ITextEditor editor, IDocument document, ITextSelection currentSelection,
			ExecutionEvent event) throws BadLocationException {

		// because of issues with preferences -> java -> editor [] only show the selected Java element
		// always set the highlight range to false
		if (editor != null) {
			IRegion remembered= editor.getHighlightRange();
			editor.resetHighlightRange();
			editor.showHighlightRangeOnly(false);
			if (remembered != null) {
				editor.setHighlightRange(remembered.getOffset(), remembered.getLength(), false);
			}
			// remove buffer local value
			BufferLocal.getInstance().kill(editor, BufferLocal.NARROW_REGION);
		} else {
			beep();
		}
		return currentSelection.getOffset();
	}
}
