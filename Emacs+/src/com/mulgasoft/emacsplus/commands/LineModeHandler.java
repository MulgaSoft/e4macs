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
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.common.CommandException;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.ui.texteditor.ITextEditor;

import com.mulgasoft.emacsplus.IEmacsPlusCommandDefinitionIds;

/**
 * Implements: line-number-mode
 * 
 * Dispatch to the Eclipse command.  We do this here as simply overriding the command in the plugin.xml
 * will shadow the Eclipse command name: toggle-line-number
 * 
 * @author Mark Feber - initial API and implementation
 */
public class LineModeHandler extends EmacsPlusCmdHandler {

	/**
	 * @see com.mulgasoft.emacsplus.commands.EmacsPlusCmdHandler#transform(ITextEditor, IDocument, ITextSelection, ExecutionEvent)
	 */
	@Override
	protected int transform(ITextEditor editor, IDocument document, ITextSelection currentSelection,
			ExecutionEvent event) throws BadLocationException {
		return NO_OFFSET;
	}

	/**
	 * @see com.mulgasoft.emacsplus.commands.EmacsPlusCmdHandler#execute(org.eclipse.core.commands.ExecutionEvent)
	 */
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		Object result = null;
		ITextEditor editor = getTextEditor(event);
		if (editor != null) {
			try {
				;
				result = executeCommand(IEmacsPlusCommandDefinitionIds.LINENUMBER_TOGGLE,null,editor);
			} catch (CommandException e) {
				// ignore
			}
		}
		return result;
	}

}
