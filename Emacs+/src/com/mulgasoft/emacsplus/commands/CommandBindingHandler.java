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
import java.util.TreeMap;

import org.eclipse.core.commands.Command;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.common.NotDefinedException;
import org.eclipse.jface.bindings.TriggerSequence;
import org.eclipse.swt.SWT;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.keys.IBindingService;

import com.mulgasoft.emacsplus.execute.CommandSupport;
import com.mulgasoft.emacsplus.execute.EmacsPlusConsole;

/**
 * Implements: describe-bindings
 * 
 * Show a list of all currently active key bindings and their associated command 
 * 
 * @author Mark Feber - initial API and implementation
 */
public class CommandBindingHandler extends EmacsPlusNoEditHandler {

	private final static String DASH_SEPR = " - ";	//$NON-NLS-1$ 
	
	/**
	 * Execute directly
	 *
	 * @see com.mulgasoft.emacsplus.commands.EmacsPlusCmdHandler#execute(org.eclipse.core.commands.ExecutionEvent)
	 */
	@Override
	public Object execute(ExecutionEvent event) {
		try {
			printBindings(getEditor(event));
		} catch (ExecutionException e) {
		}
		return null;
	}
	
	private class CB {
		public String name;
		public Command command;

		public CB(String name, Command command) {
			this.name = name;
			this.command = command;
		}
	}
	
	private void printBindings(IEditorPart editorPart) {
		IBindingService bs = (IBindingService) PlatformUI.getWorkbench().getService(IBindingService.class);
		
		CommandSupport commander = new CommandSupport();
		SortedMap<String, Command>commandList = commander.getCommandList(editorPart);
		SortedMap<String, CB> bindings = new TreeMap<String, CB>(); 
		Set<Entry<String, Command>> entries = commandList.entrySet();		
		
		for (Entry<String, Command> entry : entries) {		
			Command cmd = entry.getValue();
			TriggerSequence[] triggers = bs.getActiveBindingsFor(cmd.getId());
			if (triggers.length > 0) {
				CB cb = new CB(entry.getKey(), cmd);
				for (int i = 0; i < triggers.length; i++) {
					bindings.put(triggers[i].format(), cb);
				}
			}
		}
		
		Set<String> keys = bindings.keySet();
		if (!keys.isEmpty()) {
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
			for (Entry<String, CB> entry : bindings.entrySet()) {
				nameBuf.replace(0, maxLen, blanks);
				nameBuf.replace(0, entry.getKey().length(), entry.getKey());
				printBinding(nameBuf.toString(), entry.getValue(), console);
			}
			console.setFocus(true);
		}
	}
	
	private String blanks;

	private void printBinding(String name, CB cb, EmacsPlusConsole console) {
		console.printBold(name + SWT.TAB);
		try {
			console.printContext(cb.name);
			String desc = cb.command.getDescription();
			if (desc != null) {
				desc = desc.replaceAll(CR, EMPTY_STR);
				console.print(DASH_SEPR + desc + CR);
			} else {
				console.print(CR);
			}
		} catch (NotDefinedException e) {
			// can't happen as we've fetched everything from Eclipse directly
		}
	}
}
