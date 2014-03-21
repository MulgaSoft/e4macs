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
import com.mulgasoft.emacsplus.execute.EmacsPlusConsole;
import com.mulgasoft.emacsplus.execute.KbdMacroSupport;
import com.mulgasoft.emacsplus.minibuffer.IMinibufferState;
import com.mulgasoft.emacsplus.minibuffer.KbdMacroMinibuffer;

/**
 * @author Mark Feber - initial API and implementation
 */
public class KbdMacroEditHandler extends KbdMacroDefineHandler {

	private static String KBD_VIEW_PREFIX = EmacsPlusActivator.getResourceString("KbdMacro_View_Prefix");	//$NON-NLS-1$
	
	/**
	 * @see com.mulgasoft.emacsplus.commands.EmacsPlusCmdHandler#transform(org.eclipse.ui.texteditor.ITextEditor, org.eclipse.jface.text.IDocument, org.eclipse.jface.text.ITextSelection, org.eclipse.core.commands.ExecutionEvent)
	 */
	@Override
	protected int transform(ITextEditor editor, IDocument document,
			ITextSelection currentSelection, ExecutionEvent event)
			throws BadLocationException {
		if (isUniversalPresent()) {
			mbState = nameState();
			mbState.run(editor);
		} else {
			displayMacro(null);
		}
		return NO_OFFSET;
	}
	
	private void displayMacro(String name) {
		EmacsPlusConsole console = EmacsPlusConsole.getInstance();
		console.clear();
		console.activate();
		console.print(KbdMacroSupport.getInstance().getKbdMacro(name).toString());
	}
	
	/**
	 * Get state to handle prompt for getting kbd macro name
	 * 
	 * @return naming prompt state
	 */
	private IMinibufferState nameState() {
	
		return new IMinibufferState() {
			public String getMinibufferPrefix() {
				return KBD_VIEW_PREFIX;
			}
			
			public int run(ITextEditor editor) {
				miniTransform(new KbdMacroMinibuffer(KbdMacroEditHandler.this),editor,null);
				return NO_OFFSET;
			}
			
			public boolean executeResult(ITextEditor editor, Object minibufferResult) {
				boolean result = true;
				if (minibufferResult != null) {
					String name = (String) minibufferResult;
					if (KbdMacroSupport.getInstance().getKbdMacro(name) != null) {				
						// macro by that name
						displayMacro(name);
					} else {
						// no macro found
						asyncShowMessage(editor, String.format(NO_NAME_UNO, name), true);							
					}
				}
				return result;
			}
		};
	}
	
}
