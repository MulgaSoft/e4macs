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

import java.util.Map;
import java.util.Set;

import org.eclipse.core.commands.Command;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ParameterizedCommand;
import org.eclipse.core.commands.common.NotDefinedException;
import org.eclipse.jface.bindings.Binding;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.swt.SWT;
import org.eclipse.ui.texteditor.ITextEditor;

import com.mulgasoft.emacsplus.EmacsPlusActivator;
import com.mulgasoft.emacsplus.execute.EmacsPlusConsole;
import com.mulgasoft.emacsplus.execute.CommandHelp;
import com.mulgasoft.emacsplus.execute.ICommandResult;
import com.mulgasoft.emacsplus.minibuffer.MetaXMinibuffer;

/**
 * Describe command/binding information in Emacs+ console
 * 
 * @author Mark Feber - initial API and implementation
 */
public class CommandDescribeHandler extends MinibufferExecHandler implements INonEditingCommand {

	static String CMD_KEY_RESULT =  EmacsPlusActivator.getResourceString("Cmd_Key_Result"); 		   //$NON-NLS-1$
	private static String DESC_PREFIX = EmacsPlusActivator.getResourceString("Cmd_Desc_Prefix");	   //$NON-NLS-1$
	private static String DESC_ID = EmacsPlusActivator.getResourceString("Cmd_Id"); 				   //$NON-NLS-1$
	private static String CMD_KEY_HEADING= EmacsPlusActivator.getResourceString("Cmd_KeyHeading");     //$NON-NLS-1$
	private static String CMD_DESC_HEADING= EmacsPlusActivator.getResourceString("Cmd_DescHeading");   //$NON-NLS-1$
	private static String CMD_PARAM_HEADING= EmacsPlusActivator.getResourceString("Cmd_ParamHeading"); //$NON-NLS-1$
	
	/**
	 * @see com.mulgasoft.emacsplus.minibuffer.IMinibufferExecutable#getMinibufferPrefix()
	 */
	public String getMinibufferPrefix() {
		return DESC_PREFIX;
	}
	
	/**
	 * @see com.mulgasoft.emacsplus.commands.EmacsPlusCmdHandler#transform(ITextEditor, IDocument, ITextSelection, ExecutionEvent)
	 */
	@Override
	protected int transform(ITextEditor editor, IDocument document, ITextSelection currentSelection,
			ExecutionEvent event) throws BadLocationException {
		MetaXMinibuffer mini = new MetaXMinibuffer(this);
		mini.setIgnoreDisabled(true);	// allow currently disabled commands to appear
		return bufferTransform(mini, editor, event); 		
	}

	/**
	 * @see com.mulgasoft.emacsplus.minibuffer.IMinibufferExecutable#executeResult(ITextEditor, java.lang.Object)
	 */
	public boolean doExecuteResult(ITextEditor editor, Object minibufferResult) {

		if (minibufferResult != null) {
			EmacsPlusConsole console = EmacsPlusConsole.getInstance();
			console.clear();
			console.activate();

			ICommandResult commandR = (ICommandResult) minibufferResult;
			String name = commandR.getName();
			Command cmd = commandR.getCommand();
			console.printBold(name + CR);
			printCmdDetails(cmd, console);
		}
		return true;
	}
	
	/**
	 * Print the details about a parameterized command on the Emacs+ console
	 * 
	 * @param cmd
	 * @param console
	 */
	void printCmdDetails(ParameterizedCommand cmd, EmacsPlusConsole console) {
		String[] bindings = CommandHelp.getKeyBindingStrings(cmd, false);
		String[] abindings = CommandHelp.getKeyBindingStrings(cmd, true);
		
		console.print(SWT.TAB + DESC_ID);
		console.printContext(cmd.getId() + CR);
		
		@SuppressWarnings("unchecked")
		Map<String,?> parameterMap = cmd.getParameterMap();
		if (!parameterMap.isEmpty()) {
			console.printBold(' ' + CMD_PARAM_HEADING + CR);
			try {
				Set<String> keySet = parameterMap.keySet();
				for (String key : keySet) {
					console.print(SWT.TAB + key + '=' + parameterMap.get(key) + CR);
				}
			} catch (Exception e) {}
		}
		
		console.printBold(' ' + CMD_KEY_HEADING + CR);
		printDetails(cmd.getCommand(), bindings, abindings, console);
	}
	
	/**
	 * Print the details about a command on the Emacs+ console
	 * 
	 * @param cmd
	 * @param console
	 */
	void printCmdDetails(Command cmd, EmacsPlusConsole console) {
		String[] bindings = CommandHelp.getKeyBindingStrings(cmd, false);
		String[] abindings = CommandHelp.getKeyBindingStrings(cmd, true);
		
		console.print(SWT.TAB + DESC_ID);
		console.printContext(cmd.getId() + CR);
		console.printBold(' ' + CMD_KEY_HEADING + CR);
		printDetails(cmd, bindings, abindings, console);
	}
	
	private void printDetails(Command cmd, String[] bindings, String[] abindings, EmacsPlusConsole console) {
		if (bindings.length == 0) {
			console.printBold(SWT.TAB + CMD_NO_BINDING + CR);
		} else {
			for (int i = 0; i < abindings.length; i+=2) {
				console.printBinding(SWT.TAB + abindings[i]);
				console.printContext(A_MSG + abindings[i+1] + Z_MSG + CR);
			}
			for (int i = 0; i < bindings.length; i+=2) {
				boolean printit = true;
				for (int j=0; j < abindings.length; j+=2) {
					if (bindings[i].equals(abindings[j])) {
						printit = false;
						break;
					}
				}
				if (printit) {
					// don't bold non-active bindings
					console.print(SWT.TAB + bindings[i]);
					console.printContext(A_MSG + bindings[i+1] + Z_MSG + CR);
				}
			}
		}
		console.printBold(' ' + CMD_DESC_HEADING + CR + SWT.TAB);
		try {
			console.print(cmd.getDescription());
		} catch (NotDefinedException e) {
		}
	}
	


	Command getCommand(Binding binding) {
		Command result = null;
		ParameterizedCommand pc = binding.getParameterizedCommand();
		if (pc != null) {
			result = pc.getCommand(); 		
		}
		return result;
	}
	
	ParameterizedCommand getPCommand(Binding binding) {
		return binding.getParameterizedCommand();
	}
}
