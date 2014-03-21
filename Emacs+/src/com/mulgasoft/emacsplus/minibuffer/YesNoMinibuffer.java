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

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.ui.texteditor.ITextEditor;

/**
 * Read yes/no from the minibuffer
 * 
 * Minibuffer result is Boolean true if yes, false if no, null if neither
 * 
 * @author Mark Feber - initial API and implementation
 */
public class YesNoMinibuffer extends TextMinibuffer {

	private boolean immediately = false;
	
	/**
	 * @param executable
	 */
	public YesNoMinibuffer(IMinibufferExecutable executable) {
		super(executable);
	}
	
	/**
	 * @param executable
	 */
	public YesNoMinibuffer(IMinibufferExecutable executable, boolean immediately) {
		this(executable);
		this.immediately = immediately;
	}
	
	/**
	 * @see com.mulgasoft.emacsplus.minibuffer.ExecutingMinibuffer#executeResult(org.eclipse.ui.texteditor.ITextEditor, java.lang.Object)
	 */
	@Override
	protected boolean executeResult(ITextEditor editor, Object commandResult) {
		return super.executeResult(editor, returnResult((String)commandResult));
	}
	
	/**
	 * Read a yes or no string
	 * 
	 * @see com.mulgasoft.emacsplus.minibuffer.WithMinibuffer#charEvent(org.eclipse.swt.events.VerifyEvent)
	 */
	protected void charEvent(VerifyEvent event) {
		event.doit = false;
		switch (event.character) {
		case 0x0D: // CR - execute command (if complete) \r
		case 0x1B: // ESC - another way to leave
		case 0x08: // BS
		case 0x7F: // DEL
			super.charEvent(event);
			break;
		default:
			if (immediately && (YESORNO_Y.equalsIgnoreCase(String.valueOf(event.character))  || YESORNO_N.equalsIgnoreCase(String.valueOf(event.character) ))) {
				// respond immediately to a character
				super.charEvent(event);
				executeCR(event);
			} else if (!immediately && Character.isLetter(event.character) && ((event.stateMask & SWT.MODIFIER_MASK) == 0)) {
				// accept if plain letter
				super.charEvent(event);
			} else {
				beep();
			}
		}
	}
	
	/**
	 * Force result to either initial character or full string depending on minibuffer state
	 * 
	 * @param yorn
	 * @return true if yes, false if no, or null 
	 */
	private Boolean returnResult(String yorn) {
		Boolean result = null;
		if (yorn != null && yorn.length() > 0) {
			if (immediately) {
				if (YESORNO_Y.equalsIgnoreCase(yorn)) {
					result = true;
				} else if (YESORNO_N.equalsIgnoreCase(yorn)) {
					result = false;
				}
			} else {
				if (YESORNO_YES.equalsIgnoreCase(yorn)) {
					result = true;
				} else if (YESORNO_NO.equalsIgnoreCase(yorn)) {
					result = false;
				}
			}
		}
		return result;
	}
}
