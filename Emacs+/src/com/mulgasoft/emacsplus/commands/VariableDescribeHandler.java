/**
 * Copyright (c) 2009-2013 Mark Feber, MulgaSoft
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
import org.eclipse.swt.SWT;
import org.eclipse.ui.texteditor.ITextEditor;

import com.mulgasoft.emacsplus.EmacsPlusActivator;
import com.mulgasoft.emacsplus.execute.EmacsPlusConsole;
import com.mulgasoft.emacsplus.minibuffer.EvalMinibuffer;
import com.mulgasoft.emacsplus.preferences.PrefVars;

/**
 * @author Mark Feber - initial API and implementation
 *
 */
public class VariableDescribeHandler extends MinibufferExecHandler implements INonEditingCommand {

	private static final String VAR_DESC_HEADING= EmacsPlusActivator.getResourceString("Cmd_DescHeading"); //$NON-NLS-1$
	private static final String DESC_PREFIX = EmacsPlusActivator.getResourceString("Var_Desc_Prefix");     //$NON-NLS-1$
	private static final String VAR_VAL_HEADING= EmacsPlusActivator.getResourceString("Var_Value"); 	   //$NON-NLS-1$
	private static final String DESC_ID = EmacsPlusActivator.getResourceString("Var_Id");   			   //$NON-NLS-1$
	
	/**
	 * @see com.mulgasoft.emacsplus.minibuffer.IMinibufferExecutable#getMinibufferPrefix()
	 */
	public String getMinibufferPrefix() {
		return DESC_PREFIX;
	}

	/**
	 * @see com.mulgasoft.emacsplus.commands.EmacsPlusCmdHandler#transform(org.eclipse.ui.texteditor.ITextEditor, org.eclipse.jface.text.IDocument, org.eclipse.jface.text.ITextSelection, org.eclipse.core.commands.ExecutionEvent)
	 */
	@Override
	protected int transform(ITextEditor editor, IDocument document, ITextSelection currentSelection,
			ExecutionEvent event) throws BadLocationException {
		return bufferTransform(new EvalMinibuffer(this), editor, event); 		
	}

		/**
	 * @see com.mulgasoft.emacsplus.commands.MinibufferExecHandler#doExecuteResult(org.eclipse.ui.texteditor.ITextEditor, java.lang.Object)
	 */
	@Override
	protected boolean doExecuteResult(ITextEditor editor, Object minibufferResult) {
		if (minibufferResult != null && minibufferResult instanceof PrefVars) {
			EmacsPlusConsole console = EmacsPlusConsole.getInstance();
			console.clear();
			console.activate();

			PrefVars var = (PrefVars) minibufferResult;
			printVarDetails(var, console);
		}
		return true;
	}
	
	/**
	 * Print the details about a  on the Emacs+ console
	 * 
	 * @param var
	 * @param console
	 */
	void printVarDetails(PrefVars var, EmacsPlusConsole console) {

		String name = var.getDisplayName();
		console.printBold(DESC_ID);
		console.printContext(name + CR + CR);
		console.printBold(' ' + VAR_VAL_HEADING);
		console.print(var.getValue() + CR + CR);
		console.printBold(' ' + VAR_DESC_HEADING + CR + SWT.TAB);
		console.print(var.getDescription());
	}
}
