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
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.ui.console.IConsoleView;
import org.eclipse.ui.console.TextConsoleViewer;
import org.eclipse.ui.texteditor.ITextEditor;

/**
 * Implements: exchange-point-and-mark
 * 
 * @author Mark Feber - initial API and implementation
 */
public class MarkExchangeHandler extends EmacsPlusNoEditHandler implements IConsoleDispatch {

	/**
	 * @see com.mulgasoft.emacsplus.commands.EmacsPlusCmdHandler#transform(ITextEditor, IDocument, ITextSelection, ExecutionEvent)
	 */
	@Override
	protected int transform(ITextEditor editor, IDocument document, ITextSelection currentSelection, ExecutionEvent event)
	throws BadLocationException {
		int mark1 = getMark(editor);
		int offset = currentSelection.getOffset();
		int length = currentSelection.getLength(); 
		if (length > 0 && mark1 != offset && mark1 != offset + length) {
			// if selection has been set by mouse (or some other non-mark command)
			// swap point and mark around the selected region
			int coffset = getCursorOffset(editor);
			int mark2 = setMark(editor, coffset, false);
			// which end is the cursor?
			int newCursor = (coffset == offset ? offset + length : offset);
			selectAndReveal(editor,newCursor,mark2);
		} else if (mark1 != -1) {
			// move mark
			int mark2 = setMark(editor, getCursorOffset(editor,currentSelection), false);
			if (mark1 != mark2) {
				selectAndReveal(editor,mark1,mark2);
			}
		}
		return NO_OFFSET;
	}
	
	/**
	 * Support exchange for simple mark on TextConsole
	 * 
	 * @see com.mulgasoft.emacsplus.commands.IConsoleDispatch#consoleDispatch(org.eclipse.ui.console.TextConsoleViewer, org.eclipse.ui.console.IConsoleView, org.eclipse.core.commands.ExecutionEvent)
	 */
	public Object consoleDispatch(TextConsoleViewer viewer, IConsoleView activePart, ExecutionEvent event) {
		int mark = viewer.getMark();
		StyledText st = viewer.getTextWidget(); 
		if (mark != -1) {
			try {
				st.setRedraw(false);
				int offset = st.getCaretOffset();
				viewer.setMark(offset);
				st.setCaretOffset(mark);
				int len = offset - mark;
				viewer.setSelectedRange(offset, -len);
			} finally {
				st.setRedraw(true);
			}
		}
		return null;
	}
}
