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

import com.mulgasoft.emacsplus.IEmacsPlusCommandDefinitionIds;

/**
 * Implement: scroll-other-window
 * 
 * Scroll the adjacent frame's window down one block of lines
 * 
 * @author Mark Feber - initial API and implementation
 */
public class OtherWindowScrollDownHandler extends E4WindowHandler<OtherWindowCmd> {

	public OtherWindowScrollDownHandler() {
		super(OtherWindowCmd.class);
	}

	@Override
	protected void addToContext(IEclipseContext ctx) {
		ctx.set(E4CmdHandler.CMD_CTX_KEY, IEmacsPlusCommandDefinitionIds.SCROLL_DOWN);
		super.addToContext(ctx);
	}

}
