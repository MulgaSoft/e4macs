/**
 * Copyright (c) 2009, 2014 Mark Feber, MulgaSoft
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 */
package com.mulgasoft.emacsplus.e4.commands;

import static com.mulgasoft.emacsplus.e4.commands.WindowJoinCmd.JOIN;

import org.eclipse.e4.core.contexts.IEclipseContext;

/**
 * Implements: join-other-window
 * 
 * This is the Eclipse Emacs+ analog of delete-other-windows
 * 
 * @author Mark Feber - initial API and implementation
 */
public class WindowJoinHandler extends E4WindowHandler<WindowJoinCmd> {
		
	public WindowJoinHandler() {
		super(WindowJoinCmd.class);
	}
	
	@Override
	protected void addToContext(IEclipseContext ctx) {
		ctx.set(E4CmdHandler.CMD_CTX_KEY, JOIN.All);
		super.addToContext(ctx);
	}

}
