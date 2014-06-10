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

import com.mulgasoft.emacsplus.EmacsPlusActivator;

/**
 * Prompt for a buffer and select it in another frame. If:
 *  - only one frame, then split and select new buffer
 *  - multiple frames, move buffer to adjacent frame if not in destination frame,
 *  - else simply select in destination frame
 *  
 *  When splitting, split horizontally unless called with ^U
 *  
 * @author mfeber - Initial API and implementation
 */
public class SwitchToBufferOtherHandler extends E4WindowHandler<SwitchToBufferOtherCmd> {

	private static final String PREFIX = EmacsPlusActivator.getResourceString("Switch_Other_Buffer"); //$NON-NLS-1$ 
	
	public SwitchToBufferOtherHandler() {
		super(SwitchToBufferOtherCmd.class);
	}

	@Override
	protected void addToContext(IEclipseContext ctx) {
		ctx.set(E4CmdHandler.CMD_CTX_KEY, false);		
		ctx.set(E4CmdHandler.PRE_CTX_KEY, PREFIX);
		super.addToContext(ctx);
	}

}
