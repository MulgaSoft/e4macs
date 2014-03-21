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
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.ui.editors.text.EditorsUI;
import org.eclipse.ui.texteditor.ITextEditor;
import static org.eclipse.ui.texteditor.AbstractDecoratedTextEditorPreferenceConstants.EDITOR_CURRENT_LINE;

/**
 * Implement: hl-line-mode
 * 
 * Toggle mode that highlights the line about the buffer's point in all buffers
 * 
 * @author Mark Feber - initial API and implementation
 */
public class ToggleLineHighlight extends EmacsPlusNoEditHandler {

	/**
	 * @see com.mulgasoft.emacsplus.commands.EmacsPlusNoEditHandler#transform(ITextEditor, IDocument, ITextSelection, ExecutionEvent)
	 */
	@Override
	protected int transform(ITextEditor editor, IDocument document, ITextSelection currentSelection,
			ExecutionEvent event) throws BadLocationException {
		IPreferenceStore store= EditorsUI.getPreferenceStore();
		if (store != null) {
			store.setValue(EDITOR_CURRENT_LINE, !store.getBoolean(EDITOR_CURRENT_LINE));
		}
		return NO_OFFSET;
	}
}
