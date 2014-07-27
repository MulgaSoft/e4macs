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

/**
 * E4 Dispatch method for make-frame
 * 
 * @author mfeber - Initial API and implementation
 */
public class FrameCreateHandler extends E4WindowHandler<FrameCreateCmd> {

	public FrameCreateHandler() {
		super(FrameCreateCmd.class);
	}
	
	@Override
	protected void addToContext(IEclipseContext ctx) {
		ctx.set(E4CmdHandler.CMD_CTX_KEY, WindowSplitCmd.getDirection(true));
		super.addToContext(ctx);
	}

}
