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

import com.mulgasoft.emacsplus.BufferLocal;
import com.mulgasoft.emacsplus.MarkUtils;

/**
 * Implements: narrow-to-region. Restrict editing of the buffer to the selected region.
 * 
 * @author Mark Feber - initial API and implementation
 */
public class RegionNarrowHandler extends EmacsPlusNoEditHandler {

	/**
	 * @see com.mulgasoft.emacsplus.commands.EmacsPlusNoEditHandler#transform(org.eclipse.ui.texteditor.ITextEditor, org.eclipse.jface.text.IDocument, org.eclipse.jface.text.ITextSelection, org.eclipse.core.commands.ExecutionEvent)
	 */
	@Override
	protected int transform(ITextEditor editor, IDocument document, ITextSelection currentSelection,
			ExecutionEvent event) throws BadLocationException {

		if (editor != null) {
			// if nothing is selected, beep()
			if (currentSelection.getLength() == 0) {
				beep();
			} else {
				// narrow to selection
				editor.resetHighlightRange();
				editor.showHighlightRangeOnly(true);
				editor.setHighlightRange(currentSelection.getOffset(), currentSelection.getLength(), true);
				// Remember region to support narrow/widen in the face of Eclipse's bad behavior on activation:
				// org.eclipse.jdt.internal.ui.javaeditor.BasicJavaEditorActionContributor.setActiveEditor
				BufferLocal.getInstance().set(editor, BufferLocal.NARROW_REGION, editor.getHighlightRange());
				MarkUtils.setSelection(editor, getCursorOffset(editor), 0);
			}
		}
		return super.transform(editor, document, currentSelection, event);
	}
}
