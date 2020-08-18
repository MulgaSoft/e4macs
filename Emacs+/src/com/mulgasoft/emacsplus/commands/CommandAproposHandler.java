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

import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.eclipse.core.commands.Command;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.common.NotDefinedException;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.swt.SWT;
import org.eclipse.ui.texteditor.ITextEditor;

import com.mulgasoft.emacsplus.EmacsPlusActivator;
import com.mulgasoft.emacsplus.execute.CommandHelp;
import com.mulgasoft.emacsplus.execute.CommandSupport;
import com.mulgasoft.emacsplus.execute.EmacsPlusConsole;
import com.mulgasoft.emacsplus.minibuffer.AproposMinibuffer;

/**
 * Implement: apropos
 * 
 * Display information about matching commands in the Emacs+ console
 * 
 * @author Mark Feber - initial API and implementation
 */
public class CommandAproposHandler extends MinibufferExecHandler implements INonEditingCommand {

	private static final String A_MSG = "[";														   //$NON-NLS-1$ 
	private static final String Z_MSG = "] ";   													   //$NON-NLS-1$ 
	private static final String MX_MSG = "M-x ... RET";												   //$NON-NLS-1$ 
	
	private static String APROPOS_PREFIX = EmacsPlusActivator.getResourceString("Cmd_Apropos_Prefix"); //$NON-NLS-1$  
	private static String APROPOS_FAIL = EmacsPlusActivator.getResourceString("Cmd_Fail");  		   //$NON-NLS-1$  
	private static String REGEX_WRAPPER = ".*"; 													   //$NON-NLS-1$
	
	private String prefix = APROPOS_PREFIX;
	
	private String blanks;
	
	public String getMinibufferPrefix() {
		return prefix;
	}
	
	/**
	 * @see com.mulgasoft.emacsplus.commands.EmacsPlusCmdHandler#transform(ITextEditor, IDocument, ITextSelection, ExecutionEvent)
	 */
	@Override
	protected int transform(ITextEditor editor, IDocument document, ITextSelection currentSelection,
			ExecutionEvent event) throws BadLocationException {
		return bufferTransform(new AproposMinibuffer(this), editor, event); 		
	}

	/**
	 * @see com.mulgasoft.emacsplus.minibuffer.IMinibufferExecutable#executeResult(org.eclipse.ui.texteditor.ITextEditor, java.lang.Object)
	 */
	public boolean doExecuteResult(ITextEditor editor, Object minibufferResult) {
		printCommands(editor,(String)minibufferResult);
		return true;
	}

	private void printCommands(ITextEditor editor, String apropos) {
		CommandSupport commander = new CommandSupport();
		SortedMap<String, Command>commandList = commander.getCommandList(editor);

		String regApropos = apropos.trim();
		if (regApropos.length() == 0) {
			// detect empty string edge case
			fail(editor,apropos);
			return;
		}
		if (Character.isJavaIdentifierPart(regApropos.charAt(0))) {
			regApropos = REGEX_WRAPPER + regApropos;
		}
		if (Character.isJavaIdentifierPart(regApropos.charAt(regApropos.length()-1))) {
			regApropos = regApropos + REGEX_WRAPPER;
		}
		try {
			Pattern.compile(apropos);
		}  catch (PatternSyntaxException p) {
			fail(editor,apropos);
			return;
		}
		// get all appropriate commands (including currently disabled ones) that match
		SortedMap<String, Command> aproposTree = commander.getCommandSubTree(commandList, regApropos, true, true);
		Set<Entry<String, Command>> entries = aproposTree.entrySet();
		Set<String> keys = aproposTree.keySet();

		if (!entries.isEmpty()) {
			int maxLen = 0;
			for (String key : keys) {
				int len = key.length();
				if (len > maxLen) {
					maxLen = len;
				}
			}
			StringBuilder nameBuf = new StringBuilder(maxLen);
			for (int i=0; i < maxLen; i++) {
				nameBuf.append(' ');
			}
			blanks = nameBuf.toString();
			EmacsPlusConsole console = EmacsPlusConsole.getInstance();
			console.clear();
			console.activate();
			for (Entry<String, Command> entry : entries) {
				nameBuf.replace(0, maxLen, blanks);
				nameBuf.replace(0, entry.getKey().length(), entry.getKey());
				printCommand(nameBuf.toString(), entry.getValue(), console);
			}
		} else {
			fail(editor,apropos);
		}
	}

	private void fail(ITextEditor editor, String apropos) {
		try {
			prefix = APROPOS_FAIL;
			this.showResultMessage(editor, apropos, true);
		} finally {
			prefix = APROPOS_PREFIX;
		}		
	}
	
	private void printCommand(String name, Command command, EmacsPlusConsole console) {
		console.printBold(name + SWT.TAB);
		String bindingStrings = CommandHelp.getKeyBindingString(command, true);
		bindingStrings = (bindingStrings == null) ? MX_MSG : bindingStrings; 
		console.printContext(A_MSG + bindingStrings + Z_MSG);
		try {
			String desc = command.getDescription();
			if (desc != null) {
				desc = desc.replaceAll(CR, CR + blanks + SWT.TAB);
				console.print(desc + CR);
			} else {
				console.print(CR);
			}
		} catch (NotDefinedException e) {
		}
	}
	
}
