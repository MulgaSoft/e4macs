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

import java.util.Iterator;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;

import com.mulgasoft.emacsplus.IRegisterContents;
import com.mulgasoft.emacsplus.TecoRegister;
import com.mulgasoft.emacsplus.execute.EmacsPlusConsole;

/**
 * @author Mark Feber - initial API and implementation
 */
public class RegisterListHandler extends RegisterViewHandler {
	
	/**
	 * @see com.mulgasoft.emacsplus.commands.EmacsPlusCmdHandler#execute(org.eclipse.core.commands.ExecutionEvent)
	 */
	public Object execute(ExecutionEvent event) throws ExecutionException {

		TecoRegister register = TecoRegister.getInstance();
		Iterator<String> it = register.iterator(); 
		if (it.hasNext()) {
			EmacsPlusConsole console = EmacsPlusConsole.getInstance();
			console.clear();
			console.activate();
			while (it.hasNext()) {
				String key = it.next();
				IRegisterContents contents = register.getContents(key);
				if (contents != null) {
					printContents(console,key,contents);
				}	
			}
		} else {
			showResultMessage(getTextEditor(event), NO_REGISTER, true);
		}
		return null;
	}
}
