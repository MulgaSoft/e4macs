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
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.common.CommandException;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.ui.texteditor.ITextEditor;

import com.mulgasoft.emacsplus.IEmacsPlusCommandDefinitionIds;
import com.mulgasoft.emacsplus.KillRing;

/**
 * Synthesize command for use with negative universal-argument
 * 
 * @author Mark Feber - initial API and implementation
 */
public class BackwardKillLineHandler extends EmacsPlusCmdHandler {

	/**
	 * @see com.mulgasoft.emacsplus.commands.EmacsPlusCmdHandler#transform(ITextEditor, IDocument, ITextSelection, ExecutionEvent)
	 */
	@Override
	protected int transform(ITextEditor editor, IDocument document, ITextSelection currentSelection,
			ExecutionEvent event) throws BadLocationException {
		try {
			int offset = getCursorOffset(editor,currentSelection);
			IRegion reg = document.getLineInformationOfOffset(offset);
			// if we're at the start of the line, delete back one character
			if (reg.getOffset() == offset) {
				String id = IEmacsPlusCommandDefinitionIds.DELETE_PREVIOUS;
				try {
					// force kill ring to remember anomalous command
					KillRing.getInstance().setKill(id,true);
					executeCommand(id, null, editor);
				} finally {
					KillRing.getInstance().setKill(id,false);
				}
			}
			this.executeCommand(IEmacsPlusCommandDefinitionIds.CUT_LINE_TO_BEGINNING, null, editor);
		} catch (ExecutionException e) {
		} catch (CommandException e) {
		}
		return NO_OFFSET;
	}

}
