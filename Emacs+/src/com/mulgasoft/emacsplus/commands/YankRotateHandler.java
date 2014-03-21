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

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.handlers.HandlerUtil;

import com.mulgasoft.emacsplus.EmacsPlusUtils;
import com.mulgasoft.emacsplus.KillRing;
import com.mulgasoft.emacsplus.RingBuffer.IRingBufferElement;

/**
 * Implements: rotate-yank-pointer
 * 
 * Change the element that the kill ring points to by ARG positions where 1 is current
 * If ARG is negative, then rotate in the opposite direction
 * 
 * @author Mark Feber - initial API and implementation
 */
public class YankRotateHandler extends EmacsPlusNoEditHandler {

	private static int MAX_SHOW = 80;
	
	/**
	 * Execute directly
	 * 
	 * @see com.mulgasoft.emacsplus.commands.EmacsPlusCmdHandler#execute(org.eclipse.core.commands.ExecutionEvent)
	 */
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		IWorkbenchPart apart = HandlerUtil.getActivePart(event);
		if (apart != null) {
			int count = extractUniversalCount(event);
			if (!isUniversalPresent()) {
				// if default 1, then we want to rotate once 'forward'
				count++;
			}
			IRingBufferElement<?> element = KillRing.getInstance().rotateYankPos(count);
			if (element != null) {
				asyncShowMessage(apart, EmacsPlusUtils.normalizeString(element.toString(), MAX_SHOW), false);
			}
		}
		return null;
	}
	
	protected boolean isZero() {
		// zero ARG means rotate back one
		return true;
	}
	
}
