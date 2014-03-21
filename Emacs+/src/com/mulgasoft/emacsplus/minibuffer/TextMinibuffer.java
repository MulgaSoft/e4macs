/**
 * Copyright (c) 2009, 2010 Mark Feber, MulgaSoft
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 */
package com.mulgasoft.emacsplus.minibuffer;

import com.mulgasoft.emacsplus.EmacsPlusActivator;
import com.mulgasoft.emacsplus.RingBuffer;

/**
 * Simple executing minibuffer
 * 
 * @author Mark Feber - initial API and implementation
 */
public class TextMinibuffer extends ExecutingMinibuffer {

	protected final static String BAD_NUMBER = EmacsPlusActivator.getResourceString("Bad_Number");  						   //$NON-NLS-1$
	
	static final String YESORNO_Y = EmacsPlusActivator.getResourceString("YesOrNo_Y");  									   //$NON-NLS-1$ 
	static final String YESORNO_YES = EmacsPlusActivator.getResourceString("YesOrNo_Yes");  								   //$NON-NLS-1$ 
	static final String YESORNO_N = EmacsPlusActivator.getResourceString("YesOrNo_N");  									   //$NON-NLS-1$ 
	static final String YESORNO_NO = EmacsPlusActivator.getResourceString("YesOrNo_No");									   //$NON-NLS-1$ 
	static final String YESORNO_BAD = String.format(EmacsPlusActivator.getResourceString("YesOrNo_Bad"),YESORNO_Y, YESORNO_N); //$NON-NLS-1$ 

	boolean isYesOrNo(String yesOrNo) throws YesOrNoException {
		boolean result = false;
		String lower = yesOrNo.toLowerCase();
		if (YESORNO_Y.equals(lower) || YESORNO_YES.equals(lower)) {
			result = true;
		} else if (!(YESORNO_N.equals(lower) || YESORNO_NO.equals(lower))) {
			throw new YesOrNoException(YESORNO_BAD);
		}
		return result;
	}
	/**
	 * @param executable
	 */
	public TextMinibuffer(IMinibufferExecutable executable) {
		super(executable);
	}

	/**
	 * @see com.mulgasoft.emacsplus.minibuffer.HistoryMinibuffer#getHistoryRing()
	 */
	@Override
	protected <T> RingBuffer<T> getHistoryRing() {
		return null;
	}

	/**
	 * @see com.mulgasoft.emacsplus.minibuffer.WithMinibuffer#handlesCtrl()
	 */
	@Override
	protected boolean handlesCtrl() {
		return false;
	}

	/**
	 * @see com.mulgasoft.emacsplus.minibuffer.WithMinibuffer#handlesCtrl()
	 */
	@Override
	protected boolean handlesAlt() {
		// disable history for simple text commands
		return false;
	}

	/**
	 * @see com.mulgasoft.emacsplus.minibuffer.WithMinibuffer#handlesTab()
	 */
	@Override
	protected boolean handlesTab() {
		return false;
	}
}
