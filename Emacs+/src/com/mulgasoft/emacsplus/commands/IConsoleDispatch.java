/**
 * Copyright (c) 2009, 2010 2010 Mark Feber, MulgaSoft
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package com.mulgasoft.emacsplus.commands;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.ui.console.IConsoleView;
import org.eclipse.ui.console.TextConsoleViewer;

/**
 * @author Mark Feber - initial API and implementation
 */
public interface IConsoleDispatch {
	
	/**
	 * Dispatch to the appropriate action in the TextConsole viewer
	 * 
	 * @param viewer
	 * @param event
	 * 
	 * @return whatever the action returns or null
	 */
	Object consoleDispatch(TextConsoleViewer viewer, IConsoleView activePart, ExecutionEvent event);
}
