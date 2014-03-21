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
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.ui.texteditor.ITextEditor;

import com.mulgasoft.emacsplus.EmacsPlusActivator;
import com.mulgasoft.emacsplus.EmacsPlusUtils;

/**
 * Implements: count-lines-region
 * 
 * Print number of lines and characters in the region between point and mark
 * (the mark does not have to be active).
 * The command will return the total number of lines in the region.
 * 
 * @author Mark Feber - initial API and implementation
 */
public class CountLinesRegion extends EmacsPlusNoEditHandler {

	private final static String COUNT_REGION= EmacsPlusActivator.getResourceString("Count_Region_Msg"); //$NON-NLS-1$ 
	private final static String NO_COUNT_REGION = "No_Region_Msg";  									//$NON-NLS-1$
	
	/**
	 * @see com.mulgasoft.emacsplus.commands.EmacsPlusNoEditHandler#transform(ITextEditor, IDocument, ITextSelection, ExecutionEvent)
	 */
	@Override
	protected int transform(ITextEditor editor, IDocument document, ITextSelection currentSelection, ExecutionEvent event)
	throws BadLocationException {
		String msg = NO_COUNT_REGION;
		int lineTotal = 0;		
		int mark = getMark(editor);
		if (mark != -1) {
			int offset = getCursorOffset(editor,currentSelection);
			if (offset > mark) {
				int tmp = mark;
				mark = offset;
				offset = tmp;
			}
			int oline= document.getLineOfOffset(offset);
			int mline= document.getLineOfOffset(mark);
			IRegion mReg = document.getLineInformation(mline);
			lineTotal = Math.abs(mline - oline);
			// if not at beginning of line, then increment count to include current line
			if (mReg.getOffset() != mark) {
				++lineTotal;
			} else if (document.getLength() == mark) {
				// only if at eof and preceding didn't fire (so only eol at eof)
				++lineTotal;
			}
			int charTotal = document.get(offset, mark-offset).length();
			msg = String.format(COUNT_REGION, lineTotal, charTotal);
		}
		setCmdResult(new Integer(lineTotal));		
		EmacsPlusUtils.showMessage(editor, msg, false);
		return super.transform(editor, document, currentSelection, event);
	}

}
