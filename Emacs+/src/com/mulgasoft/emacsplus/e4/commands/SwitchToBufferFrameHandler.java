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
 * E4 Dispatch method for switch-to-buffer-other-frame 
 * 
 * @author mfeber - Initial API and implementation
 */
public class SwitchToBufferFrameHandler extends E4WindowHandler<SwitchToBufferFrameCmd> {

	private static final String PREFIX = EmacsPlusActivator.getResourceString("Switch_Other_Frame"); //$NON-NLS-1$ 
	
	/**
	 * @param clazz
	 */
	public SwitchToBufferFrameHandler() {
		super(SwitchToBufferFrameCmd.class);
	}
	
	@Override
	protected void addToContext(IEclipseContext ctx) {
		ctx.set(E4CmdHandler.CMD_CTX_KEY, false);		
		ctx.set(E4CmdHandler.PRE_CTX_KEY, PREFIX);
		super.addToContext(ctx);
	}

}
