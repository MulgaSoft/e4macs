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
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.ui.texteditor.ITextEditor;

import com.mulgasoft.emacsplus.EmacsPlusUtils;
import com.mulgasoft.emacsplus.execute.KbdMacroSupport;

/**
 * Implement: kbd-macro-end
 * 
 * @author Mark Feber - initial API and implementation
 *
 */
public class KbdMacroEndHandler extends EmacsPlusCmdHandler {

	private static String KBD_NOT_MSG = "KbdMacro_Not_Defining";	//$NON-NLS-1$
	
	public Object execute(ExecutionEvent event) throws ExecutionException {
		ITextEditor editor = getTextEditor(event);
		if (editor != null) { 
			if (!KbdMacroSupport.getInstance().endKbdMacro()){
				EmacsPlusUtils.showMessage(editor, KBD_NOT_MSG, false);
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
