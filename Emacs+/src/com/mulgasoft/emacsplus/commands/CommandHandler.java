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

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.common.CommandException;

/**
 * @author Mark Feber - initial API and implementation
 */
public abstract class CommandHandler extends EmacsPlusNoEditHandler {
	
	abstract protected String[] getCommandAndArgs();
	
	/**
	 * Execute directly
	 * 
	 * @see com.mulgasoft.emacsplus.commands.EmacsPlusCmdHandler#execute(org.eclipse.core.commands.ExecutionEvent)
	 */
	@Override
	public Object execute(ExecutionEvent event) {
		Object result = null;
		String[] commandArgs = getCommandAndArgs();
		if (commandArgs != null && commandArgs.length > 0) {
			String commandId = commandArgs[0];
			Map<String, String> argMap = new HashMap<String, String>();
			int cLen = commandArgs.length;
			for (int i=1; i< cLen; i++) {
				if (i+1 >=cLen){
					// illegal param/value set
					break;
				}
				argMap.put(commandArgs[i], commandArgs[++i]);
			}
			try {
				result =  (argMap.isEmpty() ? executeCommand(commandId, null, getEditor(event)) : 
						executeCommand(commandId, argMap, null, getEditor(event)));
			} catch (ExecutionException e) {
				beep();
			} catch (CommandException e) {
				beep();
			}
		}
		return result;
	}
}
