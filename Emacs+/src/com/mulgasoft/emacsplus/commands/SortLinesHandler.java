/**
 * Copyright (c) 2009, 2010 Mark Feber, MulgaSoft
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package com.mulgasoft.emacsplus.commands;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.ui.texteditor.ITextEditor;

import com.mulgasoft.emacsplus.EmacsPlusUtils;

/**
 * Sort lines in region alphabetically; argument means descending order.
 * The variable `SortFoldCase' determines whether alphabetic case affects
 * the sort order.
 * 
 * In Emacs, partial selection on a line is included in the sort from point or mark
 * 
 * @author Mark Feber - initial API and implementation
 */
public class SortLinesHandler extends LineHandler {

    /**
     * @see com.mulgasoft.emacsplus.commands.EmacsPlusCmdHandler#transform(ITextEditor, IDocument, ITextSelection, ExecutionEvent)
     */
    @Override
    protected int transform(ITextEditor editor, IDocument document, ITextSelection currentSelection, ExecutionEvent event) 
    throws BadLocationException {
    	int result = getCursorOffset(editor,currentSelection);
    	// get the selection encompassing the appropriate set of lines
    	ITextSelection selection = getLineSelection(editor,document,currentSelection);
    	if (selection != null) {
    		int offset = selection.getOffset();
    		int endOffset = offset+selection.getLength(); 
    		int begin = document.getLineOfOffset(offset);
    		int end = document.getLineOfOffset(endOffset);
    		if (begin != end && begin < end) {
    			ArrayList<String> alst = new ArrayList<String>();    			
    			IRegion region = document.getLineInformation(begin);
    			// get text from point or mark
    			alst.add(document.get(offset,region.getLength()-(offset-region.getOffset())));
    			for (int i = begin+1; i < end; i++) {
    				region = document.getLineInformation(i);
    				// get full line of text
    				alst.add(document.get(region.getOffset(),region.getLength()));
    			}
    			region = document.getLineInformation(end);
    			// get text to point or mark
    			alst.add(document.get(region.getOffset(),endOffset - region.getOffset()));
    			Collections.sort(alst,(getComparator(isUniversalPresent())));
    			updateLines(document,selection,alst.toArray(new String[0]));
    		} else {
    			EmacsPlusUtils.showMessage(editor, NO_REGION, true);
    		}
    	} else {
    		EmacsPlusUtils.showMessage(editor, NO_REGION, true);
    	}
    	return result;
    }
    	
    private Comparator<String> getComparator(boolean reverse) {
    	return (SortFoldCase ? new SortInsensitiveComparator(reverse) : new SortComparator(reverse));
    }

    private class SortComparator implements Comparator<String> {
	
    	private boolean reverse = false;
    	SortComparator(boolean reverse) {
    		this.reverse = reverse;
    	}    	
		/**
		 * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
		 */
		public int compare(String o1, String o2) {
			int result = o1.trim().compareTo(o2.trim()); 
			return (reverse ? -result : result); 
		}
    }
    	
    private class SortInsensitiveComparator implements Comparator<String> {
    	
    	private boolean reverse = false;
    	SortInsensitiveComparator(boolean reverse) {
    		this.reverse = reverse;
    	}
    
		/**
		 * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
		 */
		public int compare(String o1, String o2) {
			int result = String.CASE_INSENSITIVE_ORDER.compare(o1.trim(),o2.trim());
			return (reverse ? -result : result); 
		}
    }
    
    /**
	 * @see com.mulgasoft.emacsplus.commands.EmacsPlusCmdHandler#isLooping()
	 */
	protected boolean isLooping() {
		return false;
	}

}
