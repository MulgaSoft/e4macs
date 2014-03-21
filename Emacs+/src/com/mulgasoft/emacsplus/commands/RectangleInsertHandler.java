/**
 * Copyright (c) 2009, 2010 Mark Feber, MulgaSoft
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package com.mulgasoft.emacsplus.commands;

import com.mulgasoft.emacsplus.EmacsPlusActivator;

/**
 * Insert STRING on each line of rectangle (`string-insert-rectangle').
 *  
 * @author Mark Feber - initial API and implementation
 */
public class RectangleInsertHandler extends RectangleExecHandler {

	private static final String RECTANGLE_INSERT_PREFIX= EmacsPlusActivator.getResourceString("Rectangle_Insert_Prefix");	//$NON-NLS-1$
	
	public String getMinibufferPrefix() {
		return RECTANGLE_INSERT_PREFIX;
	}
	
	/**
	 * @see com.mulgasoft.emacsplus.commands.RectangleExecHandler#isReplace()
	 */
	@Override
	protected boolean isReplace() {
		return false;
	}

}
