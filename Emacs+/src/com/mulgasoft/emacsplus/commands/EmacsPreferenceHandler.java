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

/**
 * @author Mark Feber - initial API and implementation
 */
public class EmacsPreferenceHandler extends CommandHandler {

	/**
	 * @see com.mulgasoft.emacsplus.commands.CommandHandler#getCommandAndArgs()
	 */
	@Override
	protected String[] getCommandAndArgs() {
		return new String[] { "org.eclipse.ui.window.preferences",  		  //$NON-NLS-1$
				"preferencePageId", 										  //$NON-NLS-1$ 
				"com.mulgasoft.emacsplus.preferences.EmacsPlusPreferencePage" //$NON-NLS-1$ 
		};
	}
}
