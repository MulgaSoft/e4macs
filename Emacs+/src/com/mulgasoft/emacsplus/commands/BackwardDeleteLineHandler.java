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
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.ui.texteditor.ITextEditor;

import com.mulgasoft.emacsplus.IEmacsPlusCommandDefinitionIds;
import com.mulgasoft.emacsplus.KillRing;

/**
 * Synthesize command for use with negative universal-argument
 * 
 * @author Mark Feber - initial API and implementation
 */
public class BackwardDeleteLineHandler extends EmacsPlusCmdHandler {

	/**
	 * @see com.mulgasoft.emacsplus.commands.EmacsPlusCmdHandler#transform(ITextEditor, IDocument, ITextSelection, ExecutionEvent)
	 */
	@Override
	protected int transform(ITextEditor editor, IDocument document, ITextSelection currentSelection,
			ExecutionEvent event) throws BadLocationException {
		try {
			executeCommand(IEmacsPlusCommandDefinitionIds.BACKWARD_CHAR, null, editor);
			String id = IEmacsPlusCommandDefinitionIds.DELETE_LINE;
			try {
				// force kill ring to treat it as a backwardly deleting method
				KillRing.getInstance().setKill(id,true);
				executeCommand(id, null, editor);
			} finally {
				KillRing.getInstance().setKill(id,false);
			}
		} catch (ExecutionException e) {
		} catch (CommandException e) {
		}
		return NO_OFFSET;
	}
}
