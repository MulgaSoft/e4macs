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

import com.mulgasoft.emacsplus.EmacsPlusUtils;
import com.mulgasoft.emacsplus.IEmacsPlusCommandDefinitionIds;

/**
 * @author Mark Feber - initial API and implementation
 *
 */
public class BeginLineHandler extends EmacsMovementHandler {

	/**
	 * @see com.mulgasoft.emacsplus.commands.EmacsPlusCmdHandler#transform(org.eclipse.ui.texteditor.ITextEditor, org.eclipse.jface.text.IDocument, org.eclipse.jface.text.ITextSelection, org.eclipse.core.commands.ExecutionEvent)
	 */
	@Override
	protected int transform(ITextEditor editor, IDocument document, ITextSelection currentSelection,
			ExecutionEvent event) throws BadLocationException {
		int count = getUniversalCount();
		if (Math.abs(count) > 1) {
			try {
				EmacsPlusUtils.executeCommand(IEmacsPlusCommandDefinitionIds.NEXT_LINE, (count < 0 ? count + 1 : count -1), null, editor);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		moveWithMark(editor, getCurrentSelection(editor), IEmacsPlusCommandDefinitionIds.LINE_START, IEmacsPlusCommandDefinitionIds.SELECT_LINE_START);
		return NO_OFFSET;
	}

	@Override
	protected boolean isLooping() {
		return false;
	}

}
