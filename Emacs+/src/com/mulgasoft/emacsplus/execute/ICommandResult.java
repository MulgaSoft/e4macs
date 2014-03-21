/**
 * Copyright (c) 2009, 2010 Mark Feber, MulgaSoft
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 */
package com.mulgasoft.emacsplus.execute;

import org.eclipse.core.commands.Command;

/**
 * A minibuffer result interface for command invocations
 * 
 * @author Mark Feber - initial API and implementation
 */
public interface ICommandResult {
	/**
	 * Get the Command object result set by the minibuffer 
	 * 
	 * @return the Command
	 */
	Command getCommand();
	/**
	 * Get the name of the command as entered by the user
	 * 
	 * @return the name 
	 */
	String getName();
}
