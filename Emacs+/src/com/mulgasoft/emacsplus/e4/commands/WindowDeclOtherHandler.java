/**
 * Copyright (c) 2009, 2014 Mark Feber, MulgaSoft
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package com.mulgasoft.emacsplus.e4.commands;

/**
 * E4 dispatch method for: open-declaration-other-window
 * 
 * @author mfeber - Initial API and implementation
 */
public class WindowDeclOtherHandler extends E4WindowHandler<WindowDeclOtherCmd> {

	public WindowDeclOtherHandler() {
		super(WindowDeclOtherCmd.class);
	}
}
