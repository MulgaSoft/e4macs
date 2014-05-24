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

import org.eclipse.e4.core.contexts.IEclipseContext;

import static com.mulgasoft.emacsplus.e4.commands.WindowJoinCmd.JOIN;

/**
 * Implements: delete-window
 * 
 * As Eclipse uses PartStacks (which roughly correspond to an emacs frame - except they 
 * can contain more than one buffer), merge and delete the selected PartStack.
 * 
 * @author Mark Feber - initial API and implementation
 */
public class WindowDeleteHandler extends E4WindowHandler<WindowJoinCmd> {

	public WindowDeleteHandler() {
		super(WindowJoinCmd.class);
	}
	
	@Override
	protected void addToContext(IEclipseContext ctx) {
		ctx.set(E4CmdHandler.CMD_CTX_KEY, JOIN.One);
		super.addToContext(ctx);
	}
	
}
