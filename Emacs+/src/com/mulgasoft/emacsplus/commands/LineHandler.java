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
import org.eclipse.jface.text.TextSelection;
import org.eclipse.ui.texteditor.ITextEditor;

import com.mulgasoft.emacsplus.EmacsPlusActivator;

/**
 * Group line handlers together
 * 
 * The variable `SortFoldCase' determines whether alphabetic case affects the sort order.
 * Toggle value on command; show value without change when ARG present
 * 
 * @author Mark Feber - initial API and implementation
 */
public class LineHandler extends EmacsPlusCmdHandler {

	protected final static String NO_REGION = "No_Region_Msg";  										   //$NON-NLS-1$
	protected final static String CASE_SENSITIVE = EmacsPlusActivator.getResourceString("Case_Sensitive"); //$NON-NLS-1$
	
	protected static boolean SortFoldCase = false; 	// ignore case on true

	/**
	 * @see com.mulgasoft.emacsplus.commands.EmacsPlusCmdHandler#transform(org.eclipse.ui.texteditor.ITextEditor, org.eclipse.jface.text.IDocument, org.eclipse.jface.text.ITextSelection, org.eclipse.core.commands.ExecutionEvent)
	 */
	@Override
	protected int transform(ITextEditor editor, IDocument document, ITextSelection currentSelection, ExecutionEvent event)
	throws BadLocationException {
		if (!isUniversalPresent()) {
			SortFoldCase = !SortFoldCase;
		}
		asyncShowMessage(editor,String.format(CASE_SENSITIVE,!SortFoldCase),false);
		return NO_OFFSET;
	}
	
	/**
	 * Get the selection that encompasses the correct region or set of lines for the command
	 * These commands will use the region between point and mark (even if not active), if a
	 * selection is not present. 
	 * 
	 * @param editor
	 * @param document
	 * @param selection
	 * @return the selection encompassing the appropriate region (or null)
	 * 
	 * @throws BadLocationException
	 */
	protected ITextSelection getLineSelection(ITextEditor editor, IDocument document, ITextSelection selection) throws BadLocationException {
		return getMinSelection(document,getImpliedSelection(editor, selection));
	}
	
    /**
     * Reduce the selection to remove a trailing EOL
     * 
     * @param document
     * @param selection
     * @return the selection potentially reduced to the appropriate region (or null)
     * 
     * @throws BadLocationException
     */
    protected ITextSelection getMinSelection(IDocument document, ITextSelection selection) throws BadLocationException {
    	if (selection != null) {
			int offset = selection.getOffset();
			int endOffset = offset + selection.getLength();
			IRegion region = document.getLineInformationOfOffset(endOffset);
			if (endOffset == region.getOffset()) {
				// ignore and back up if we're at the beginning of the selection's last line
				region = document.getLineInformation(document.getLineOfOffset(endOffset) - 1);
				endOffset = region.getOffset() + region.getLength();
				selection = new TextSelection(document, offset, endOffset - offset);
			}
		}
		return selection;
    }
    
    protected String[] reverse(String[] array) {
    	int len = array.length -1;	
    	int stop = array.length / 2;
    	for (int i = 0; i < stop; i++) {
    		String tmp = array[i];
    		array[i] = array[len -i];
    		array[len -i] = tmp;
    	}
    	return array;
    }
    
    /**
	 * Update the selection with the set of lines in the array
	 * 
	 * @param document
	 * @param selection
	 * @param array
	 * @throws BadLocationException
	 */
	protected void updateLines(IDocument document, ITextSelection selection, String[] array) throws BadLocationException  {
		StringBuilder builder = new StringBuilder();
		String ld = getLineDelimiter();		
		for (String t : array) {
			builder.append(t);
			builder.append(ld);
		}
		// remove terminal delimiter
		builder.replace(builder.length()-ld.length(),builder.length(),EMPTY_STR);
		updateText(document,selection,builder.toString());
	}

}
