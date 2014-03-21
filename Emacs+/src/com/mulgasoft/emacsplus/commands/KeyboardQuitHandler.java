/**
 * Copyright (c) 2009, 2010, Mark Feber, MulgaSoft
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 */
package com.mulgasoft.emacsplus.commands;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.ui.console.IConsoleView;
import org.eclipse.ui.console.TextConsoleViewer;
import org.eclipse.ui.texteditor.ITextEditor;

import com.mulgasoft.emacsplus.EmacsPlusUtils;
import com.mulgasoft.emacsplus.MarkUtils;

/**
 * Implements: keyboard-quit
 * 
 * Clear the current selection (if any), and the status area, and beep
 * 
 * @author Mark Feber - initial API and implementation
 */
public class KeyboardQuitHandler extends EmacsPlusNoEditHandler implements IConsoleDispatch {

	/**
	 * @see com.mulgasoft.emacsplus.commands.EmacsPlusCmdHandler#transform(ITextEditor, IDocument, ITextSelection, ExecutionEvent)
	 */
	@Override
	protected int transform(ITextEditor editor, IDocument document, ITextSelection currentSelection, ExecutionEvent event) 
	throws BadLocationException {

		// clear status area
		EmacsPlusUtils.clearMessage(editor);
		EmacsMovementHandler.clearShifted(); // set mark always removes shift select flag
		beep();
		int offset = NO_OFFSET;
		if (currentSelection.getLength() > 0) {
			// when region is narrowed, avoid provoking a range update in eclipse code 
			if (editor.showsHighlightRangeOnly()) {
				MarkUtils.setSelection(editor, getCursorOffset(editor), 0);
			} else {
				offset = getCursorOffset(editor);	
			}
		}
		return offset;
	}
	
	/**
	 * Support simple clear mark/selection on TextConsole
	 * 
	 * @see com.mulgasoft.emacsplus.commands.IConsoleDispatch#consoleDispatch(TextConsoleViewer, IConsoleView, ExecutionEvent)
	 */
	public Object consoleDispatch(TextConsoleViewer viewer, IConsoleView activePart, ExecutionEvent event) {
		MarkUtils.clearConsoleMark(viewer);
		return null;
	}	
}
