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

import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.ui.texteditor.ITextEditor;

import com.mulgasoft.emacsplus.RingBuffer;

/**
 * A minibuffer that reads only numbers (and minus)
 * 
 * @author Mark Feber - initial API and implementation
 */
public class ReadNumberMinibuffer extends TextMinibuffer {

	/**
	 * @param executable
	 */
	public ReadNumberMinibuffer(IMinibufferExecutable executable) {
		super(executable);
	}

	/**
	 * @see com.mulgasoft.emacsplus.minibuffer.WithMinibuffer#handlesCtrl()
	 */
	@Override
	protected boolean handlesAlt() {
		// enable history for number reader
		return true;
	}

	/**
	 * @see com.mulgasoft.emacsplus.minibuffer.ExecutingMinibuffer#executeResult(org.eclipse.ui.texteditor.ITextEditor, java.lang.Object)
	 */
	@Override
	protected boolean executeResult(ITextEditor editor, Object commandResult) {
		boolean result = true;
		String numberStr = (String)commandResult;
		if (numberStr != null && numberStr.length() > 0) {
			try {
				Integer number = Integer.valueOf(numberStr);
				result = super.executeResult(editor, number);
				addToHistory(number); // add to command history
			} catch (NumberFormatException e) {
				super.setResultMessage(String.format(BAD_NUMBER,numberStr), true, true);				
			}
		}
		return result;
	}

	/**
	 * Use number reading fragment
	 * 
	 * @see com.mulgasoft.emacsplus.minibuffer.WithMinibuffer#charEvent(org.eclipse.swt.events.VerifyEvent)
	 */
	protected void charEvent(VerifyEvent event) {
		numCharEvent(event);
	}

	/**** Local RingBuffer: use lazy initialization holder class idiom ****/

	/**
	 * @see com.mulgasoft.emacsplus.minibuffer.HistoryMinibuffer#getHistoryRing()
	 */
	@Override
	@SuppressWarnings("unchecked")
	protected RingBuffer<Integer> getHistoryRing() {
		return NumberRing.ring;
	}
	
	private static class NumberRing {
		static final RingBuffer<Integer> ring = new RingBuffer<Integer>();		
	}

}
