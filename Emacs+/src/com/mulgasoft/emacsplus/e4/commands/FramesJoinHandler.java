/**
 * Copyright (c) 2009, 2014 Mark Feber, MulgaSoft
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package com.mulgasoft.emacsplus.e4.commands;

import org.eclipse.e4.core.contexts.IEclipseContext;

import com.mulgasoft.emacsplus.e4.commands.WindowJoinCmd.Join;

/**
 * E4 Dispatch method for join-other-frames
 * 
 * @author mfeber - Initial API and implementation
 */
public class FramesJoinHandler extends E4WindowHandler<FrameJoinCmd> {

	public FramesJoinHandler() {
		super(FrameJoinCmd.class);
	}

	@Override
	protected void addToContext(IEclipseContext ctx) {
		ctx.set(E4CmdHandler.CMD_CTX_KEY, Join.ALL);
		super.addToContext(ctx);
	}

}
