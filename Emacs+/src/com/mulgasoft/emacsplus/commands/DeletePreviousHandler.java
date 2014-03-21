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
import org.eclipse.ui.console.TextConsoleViewer;

import com.mulgasoft.emacsplus.IEmacsPlusCommandDefinitionIds;
import com.mulgasoft.emacsplus.KillRing;

/**
 * Console command for delete-previous-word
 * 
 * @author Mark Feber - initial API and implementation
 */
public class DeletePreviousHandler extends ConsoleCmdHandler {
	
	/**
	 * @see com.mulgasoft.emacsplus.commands.ConsoleCmdHandler#getId(ExecutionEvent, TextConsoleViewer)
	 */
	@Override
	protected String getId(ExecutionEvent event, TextConsoleViewer viewer) {
		String id = IEmacsPlusCommandDefinitionIds.DELETE_PREVIOUS_WORD;
		KillRing.getInstance().setKill(id, true);
		return id; 
	}
}
