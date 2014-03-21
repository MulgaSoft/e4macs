/**
 * Copyright (c) 2009, 2010 Mark Feber, MulgaSoft
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package com.mulgasoft.emacsplus.commands;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;

/**
 * Implements: up-list
 * 
 * Move forward out of one level of parentheses;  with ARG, do this that many times.
 * A negative argument means move backward but still to a less deep spot.
 * 
 * @author Mark Feber - initial API and implementation
 */
public class UpListHandler extends BackwardUpListHandler {
	
	protected int doTransform(IDocument document, ITextSelection selection, int cursorOffset,boolean isBackup)
	throws BadLocationException {
		// simply reverse the direction flag
		return super.doTransform(document,selection,cursorOffset,!isBackup);
	}

}
