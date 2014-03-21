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
 * Print number of lines in current buffer, and how many are before or after point.
 * The command will return the total number of lines in the buffer.
 *  
 * @author Mark Feber - initial API and implementation
 */
public class CountLinesBuffer extends EmacsPlusNoEditHandler {

	private final static String COUNT_BUFFER = EmacsPlusActivator.getResourceString("Count_Buffer_Msg");	//$NON-NLS-1$
	
	/**
	 * @see com.mulgasoft.emacsplus.commands.EmacsPlusNoEditHandler#transform(org.eclipse.ui.texteditor.ITextEditor, org.eclipse.jface.text.IDocument, org.eclipse.jface.text.ITextSelection, org.eclipse.core.commands.ExecutionEvent)
	 */
	@Override
	protected int transform(ITextEditor editor, IDocument document, ITextSelection currentSelection,
			ExecutionEvent event) throws BadLocationException {
		int offset = currentSelection.getOffset();
		IRegion lineInfo = document.getLineInformationOfOffset(offset);
		int lineTotal = document.getNumberOfLines();
		int lineNumber = document.getLineOfOffset(offset);
		// only include current, if at beginning of line
		if (offset != lineInfo.getOffset()) {
			++lineNumber;
		}
		if (document.getLength() == offset) {
			lineNumber = lineTotal;
		}
		setCmdResult(new Integer(lineTotal));
		EmacsPlusUtils.showMessage(editor, String.format(COUNT_BUFFER, lineTotal, lineNumber, lineTotal - lineNumber), false);
		return super.transform(editor, document, currentSelection, event);
	}

}
