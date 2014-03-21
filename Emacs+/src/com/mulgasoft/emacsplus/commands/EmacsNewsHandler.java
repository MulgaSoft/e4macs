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
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;

import com.mulgasoft.emacsplus.EmacsPlusActivator;
import com.mulgasoft.emacsplus.execute.EmacsPlusConsole;

/**
 * @author Mark Feber - initial API and implementation
 *
 */
public class EmacsNewsHandler extends EmacsPlusNoEditHandler {

	private final static Color ENTRY_COLOR = new Color(null, 0,0,255); 
	
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {

		String changes = EmacsPlusActivator.getResourceString("EmacsPlusChanges");	//$NON-NLS-1$  
		EmacsPlusConsole console = EmacsPlusConsole.getInstance();
		console.clear();
		console.activate();
		String[] lines = changes.split("\\n");	//$NON-NLS-1$
		for (String line : lines) {
			if (Character.isDigit(line.charAt(0))) {
				console.print(line,ENTRY_COLOR,SWT.BOLD);
			} else {
				console.print(line);
			}
			console.print(CR);
		}
		console.setFocus(true);
		return null;
	}

}
