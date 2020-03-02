/**
 * Copyright (c) 2009-2011 Mark Feber, MulgaSoft
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package com.mulgasoft.emacsplus.preferences;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.ui.texteditor.ITextEditor;

/**
 * Implement: set-variable
 * 
 * @author Mark Feber - initial API and implementation
 */
public class SetVariableHandler extends EvalHandler {

	protected String getTypePrompt(EvalType e) {
		return e.toString();
	}
	
	/**
	 * @see com.mulgasoft.emacsplus.commands.EmacsPlusCmdHandler#transform(org.eclipse.ui.texteditor.ITextEditor,
	 *      org.eclipse.jface.text.IDocument, org.eclipse.jface.text.ITextSelection,
	 *      org.eclipse.core.commands.ExecutionEvent)
	 */
	@Override
	protected int transform(ITextEditor editor, IDocument document, ITextSelection currentSelection,
			ExecutionEvent event) throws BadLocationException {

		mbState = variableSetState();
		mbState.run(editor);
		return NO_OFFSET;
	}

}
