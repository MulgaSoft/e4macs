/**
 * Copyright (c) 2009, 2010 Mark Feber, MulgaSoft
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 */
package com.mulgasoft.emacsplus.commands;


/**
 * Implements: backward-block-of-lines
 * 
 * @author Mark Feber - initial API and implementation
 */
public class BlockBackwardHandler extends BlockHandler {

	protected int getDirection() {
		return BACKWARD;
	}
}
