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

import org.eclipse.ui.texteditor.ITextEditor;

import com.mulgasoft.emacsplus.RingBuffer;

/**
 * Just read a string and return if for execution by the apropos command
 * 
 * @author Mark Feber - initial API and implementation
 */
public class AproposMinibuffer extends TextMinibuffer {

	/**
	 * @param executable
	 */
	public AproposMinibuffer(IMinibufferExecutable executable) {
		super(executable);
	}

	/**
	 * @see com.mulgasoft.emacsplus.minibuffer.WithMinibuffer#handlesCtrl()
	 */
	@Override
	protected boolean handlesAlt() {
		// enable history for Apropos
		return true;
	}

	/**
	 * @see com.mulgasoft.emacsplus.minibuffer.ExecutingMinibuffer#executeResult(org.eclipse.ui.texteditor.ITextEditor, java.lang.Object)
	 */
	@Override
	protected boolean executeResult(ITextEditor editor, Object commandResult) {
		boolean result = true;
		String apropos = (String)commandResult;
		if (apropos != null && apropos.length() > 0) {
			addToHistory(apropos); // add to command history
			result = super.executeResult(editor, commandResult);
		}
		return result;
	}

	/**** Local RingBuffer: use lazy initialization holder class idiom ****/

	/**
	 * @see com.mulgasoft.emacsplus.minibuffer.HistoryMinibuffer#getHistoryRing()
	 */
	@Override
	@SuppressWarnings("unchecked")	
	protected RingBuffer<String> getHistoryRing() {
		return AproposRing.ring;
	}
	
	private static class AproposRing {
		static final RingBuffer<String> ring = new RingBuffer<String>();		
	}

}
