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
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.ui.texteditor.ITextEditor;

import com.mulgasoft.emacsplus.EmacsPlusActivator;
import com.mulgasoft.emacsplus.execute.KbdMacroSupport;
import com.mulgasoft.emacsplus.minibuffer.KbdMacroNameMinibuffer;

/**
 * Provide a name for the current keyboard macro
 * This makes a copy the macro and assigns it the name
 *  
 * To save a keyboard macro for longer than until you define the next one,
 * you must give it a name using `M-x name-last-kbd-macro'.  This reads a
 * name as an argument using the minibuffer and defines that name to
 * execute the macro.  The macro name is made a command, and defining it in
 * this way makes it a valid command name for calling with `M-x'
 * 
 * @author Mark Feber - initial API and implementation
 */
public class KbdMacroNameHandler extends KbdMacroDefineHandler {
	
	private static String KBD_NAME_PREFIX = EmacsPlusActivator.getResourceString("KbdMacro_Name_Prefix");	//$NON-NLS-1$  
	
	public String getMinibufferPrefix() {
		return KBD_NAME_PREFIX;
	}
	
	/**
	 * @see com.mulgasoft.emacsplus.commands.EmacsPlusCmdHandler#transform(ITextEditor, IDocument, ITextSelection, ExecutionEvent)
	 */
	@Override
	protected int transform(ITextEditor editor, IDocument document, ITextSelection currentSelection, ExecutionEvent event)
	throws BadLocationException {
		if (KbdMacroSupport.getInstance().hasKbdMacro(false)) {
			return bufferTransform(new KbdMacroNameMinibuffer(this), editor, event);
		} else {
			asyncShowMessage(editor, NO_MACRO_ERROR, true);
		}
		return NO_OFFSET;
	}

	/**
	 * @see com.mulgasoft.emacsplus.commands.MinibufferExecHandler#doExecuteResult(org.eclipse.ui.texteditor.ITextEditor, java.lang.Object)
	 */
	@Override
	protected boolean doExecuteResult(ITextEditor editor, Object minibufferResult) {
		String name = (String)minibufferResult;
		if (name != null && name.length() > 0) {
 			if (nameKbdMacro(name, editor) == null) {
 				beep();
 			}
		}
		// exit after execution
		return true;
	}
}
