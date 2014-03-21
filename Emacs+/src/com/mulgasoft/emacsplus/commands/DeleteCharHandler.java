/**
 * Copyright (c) 2009, 2010, Mark Feber, MulgaSoft
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

/**
 * Add delete character to Console command set
 * 
 * @author Mark Feber - initial API and implementation
 */
public class DeleteCharHandler extends ConsoleCmdHandler {

	protected String getId(ExecutionEvent event, TextConsoleViewer viewer) {
		return IEmacsPlusCommandDefinitionIds.DELETE_NEXT; 
	}
}
