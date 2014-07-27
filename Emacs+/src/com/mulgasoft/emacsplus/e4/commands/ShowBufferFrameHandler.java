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
 * E4 Dispatch method for display-buffer-other-frame
 *  
 * Prompt for a buffer and display it in another frame. If:
 *  - only one frame, then split and display new frame
 *  - multiple frames, move buffer to adjacent frame if not in destination frame,
 *  - else simply expose in destination frame
 *  
 * @author mfeber - Initial API and implementation
 */
public class ShowBufferFrameHandler extends SwitchToBufferFrameHandler {

	private static final String PREFIX = EmacsPlusActivator.getResourceString("Display_Other_Frame"); //$NON-NLS-1$ 
	
	@Override
	protected void addToContext(IEclipseContext ctx) {
		super.addToContext(ctx);
		ctx.set(E4CmdHandler.CMD_CTX_KEY, true);		
		ctx.set(E4CmdHandler.PRE_CTX_KEY, PREFIX);
	}
}
