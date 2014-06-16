/**
 * Copyright (c) 2009, 2010 Mark Feber, MulgaSoft
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package com.mulgasoft.emacsplus.commands;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.ui.texteditor.ITextEditor;

import com.mulgasoft.emacsplus.EmacsPlusUtils;
import com.mulgasoft.emacsplus.KillRing;

/**
 * Implements: append-next-kill
 * 
 * Cause following command, if it kills, to append to previous kill
 * 
 * @author Mark Feber - initial API and implementation
 */
public class AppendNextKillHandler extends EmacsPlusNoEditHandler {

	private final static String APPEND_KILL = "Append_Kill";	//$NON-NLS-1$
	
	protected int transform(ITextEditor editor, IDocument document, ITextSelection currentSelection,
			ExecutionEvent event) throws BadLocationException {
		KillRing.getInstance().setForceAppend(true);
		// don't override M-x info if called from M-x
		if (event.getTrigger() != null) {
			EmacsPlusUtils.showMessage(editor, APPEND_KILL, false);
		}
		return NO_OFFSET;
	}
	
}
