/**
 * Copyright (c) 2009, 2010 Mark Feber, MulgaSoft
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package com.mulgasoft.emacsplus.minibuffer;

import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.ui.texteditor.ITextEditor;

import com.mulgasoft.emacsplus.RingBuffer;

/**
 * @author Mark Feber - initial API and implementation
 */
public class RectangleMinibuffer extends TextMinibuffer {

	/**
	 * Simple text minibuffer for use in rectangle commands that need input text
	 * 
	 * @param executable
	 */
	public RectangleMinibuffer(IMinibufferExecutable executable) {
		super(executable);
	}

	/**
	 * Store text in history and invoke the executable command
	 * 
	 * @see com.mulgasoft.emacsplus.minibuffer.ExecutingMinibuffer#executeResult(org.eclipse.ui.texteditor.ITextEditor, java.lang.Object)
	 */
	@Override
	protected boolean executeResult(ITextEditor editor, Object commandResult) {
		boolean result = true;
		String text = (String)commandResult;
		if (text != null) {
			// skip history on erase replace
			if (text.length() > 0) {
				addToHistory(text); // add to command history
			}
			result = super.executeResult(editor, commandResult);
		}
		return result;
	}

	/**
	 * Enable history commands
	 * 
	 * @see com.mulgasoft.emacsplus.minibuffer.WithMinibuffer#handlesAlt()
	 */
	@Override
	protected boolean handlesAlt() {
		return true;
	}
	
	protected boolean handlesTab() {
		return true;
	}

	protected boolean dispatchTab(VerifyEvent event) {
		addIt(event);
		return true;
	}

	/**** Local RingBuffer: use lazy initialization holder class idiom ****/

	/**
	 * @see com.mulgasoft.emacsplus.minibuffer.HistoryMinibuffer#getHistoryRing()
	 */
	@Override
	@SuppressWarnings("unchecked")	
	protected RingBuffer<String> getHistoryRing() {
		return RectangleRing.ring;
	}
	
	private static class RectangleRing {
		static final RingBuffer<String> ring = new RingBuffer<String>();		
	}
	
}
