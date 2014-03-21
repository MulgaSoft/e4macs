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
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.ui.texteditor.ITextEditor;

import com.mulgasoft.emacsplus.EmacsPlusUtils;

/**
 * Implement: reverse-region
 * Reverse the order of lines in a region.
 * 
 * @author Mark Feber - initial API and implementation
 */
public class ReverseRegionHandler extends LineHandler {

	/**
	 * @see com.mulgasoft.emacsplus.commands.EmacsPlusCmdHandler#transform(ITextEditor, IDocument, ITextSelection, ExecutionEvent)
	 */
	@Override
	protected int transform(ITextEditor editor, IDocument document, ITextSelection currentSelection,
			ExecutionEvent event) throws BadLocationException {
    	ITextSelection selection = getLineSelection(editor,document,currentSelection);
    	if (selection != null) {
    		int offset = selection.getOffset();
    		int endOffset = offset+selection.getLength(); 
    		int begin = document.getLineOfOffset(offset);
    		int end = document.getLineOfOffset(endOffset);
			if (begin != end && begin < end) {
				// grab the lines
				int len = end-begin+1; 
				String[] array = new String[len];
				for (int i = 0; i < len; i++) {
					IRegion region = document.getLineInformation(begin+i);
					array[i] = document.get(region.getOffset(),region.getLength());
				}
				// and reverse them
				updateLines(document,new TextSelection(document,offset,endOffset-offset),reverse(array));
			}
		} else {
			EmacsPlusUtils.showMessage(editor, NO_REGION, true);
		}
		return NO_OFFSET;
	}
	
	/**
	 * Determine the largest possible region containing complete sets of line begin/end
	 * 
	 * @see com.mulgasoft.emacsplus.commands.LineHandler#getMinSelection(IDocument, ITextSelection)
	 */
	protected ITextSelection getMinSelection(IDocument document,ITextSelection selection) throws BadLocationException {
		if (selection != null) {
			int offset = selection.getOffset();
			int endOffset = offset + selection.getLength(); 
			int begin = document.getLineOfOffset(offset);
			if (document.getLineOffset(begin) != offset) {
				// if we're not at the beginning of the line, move to next one
				if (++begin < document.getNumberOfLines()) {
					offset = document.getLineOffset(begin);
				}
			}
			IRegion region = document.getLineInformationOfOffset(endOffset);
			if (region.getOffset()+region.getLength() != endOffset) {
				// if we're not at the end of the line, move to previous one
				int end = document.getLineOfOffset(endOffset);
				if (--end >= 0) {
					region = document.getLineInformation(end);
					endOffset = region.getOffset()+region.getLength();
				}
			}
			selection = new TextSelection(document,offset,endOffset-offset);
		}
		return selection;
	}

}
