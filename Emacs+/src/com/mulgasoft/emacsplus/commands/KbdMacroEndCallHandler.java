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
import com.mulgasoft.emacsplus.execute.KbdMacroSupport;


/**
 * Implement: kbd-macro-end-or-call
 * 
 * If a keyboard macro is being defined, end the definition;
 * otherwise, execute the most recent keyboard macro
 * 
 * @author Mark Feber - initial API and implementation
 */
public class KbdMacroEndCallHandler extends EmacsPlusCmdHandler {

	public Object execute(ExecutionEvent event) throws ExecutionException {
		ITextEditor editor = getTextEditor(event);
		if (editor != null) {
			KbdMacroSupport support = KbdMacroSupport.getInstance(); 
			// returns true if macro was being defined, else false
			if (!support.endKbdMacro()){
				if (support.hasKbdMacro()) {
					try {
						executeCommand(IEmacsPlusCommandDefinitionIds.KBDMACRO_EXECUTE, null, editor);
					} catch (CommandException e) {}
				}
			}
		}
		return null;
	}		

	/**
	 * @see com.mulgasoft.emacsplus.commands.EmacsPlusCmdHandler#transform(org.eclipse.ui.texteditor.ITextEditor, org.eclipse.jface.text.IDocument, org.eclipse.jface.text.ITextSelection, org.eclipse.core.commands.ExecutionEvent)
	 */
	@Override
	protected int transform(ITextEditor editor, IDocument document,
			ITextSelection currentSelection, ExecutionEvent event)
			throws BadLocationException {
		return NO_OFFSET;
	}

}
