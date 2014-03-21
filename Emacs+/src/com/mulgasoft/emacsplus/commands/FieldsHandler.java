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

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.ui.texteditor.ITextEditor;

import com.mulgasoft.emacsplus.EmacsPlusActivator;
import com.mulgasoft.emacsplus.EmacsPlusUtils;

/**
 * Base class for field sorting commands
 * 
 * @author Mark Feber - initial API and implementation
 */
public abstract class FieldsHandler extends LineHandler {

	private final static String INSUFFICIENT_FIELDS = EmacsPlusActivator.getResourceString("Insufficient_Fields"); //$NON-NLS-1$
	private final static String SPLIT_ON = "\\s+";   															   //$NON-NLS-1$
	
	protected abstract class FieldsCompare implements Comparable<FieldsCompare> {}
	protected abstract FieldsCompare getComparable(String txt, String field);

	/**
	 * @see com.mulgasoft.emacsplus.commands.EmacsPlusCmdHandler#transform(ITextEditor, IDocument, ITextSelection, ExecutionEvent)
	 */
	@Override
    protected int transform(ITextEditor editor, IDocument document, ITextSelection currentSelection, ExecutionEvent event) 
    throws BadLocationException {

    	int result = getCursorOffset(editor,currentSelection);
    	int count = getUniversalCount();
    	boolean back = count < 0;
    	int index = (count > 0 ? count-1 : Math.abs(count));
    	// get the selection encompassing the appropriate set of lines
    	ITextSelection selection = getLineSelection(editor,document,currentSelection);
    	if (selection != null) {
    		int offset = selection.getOffset();
    		int endOffset = offset+selection.getLength(); 
    		int begin = document.getLineOfOffset(offset);
    		int end = document.getLineOfOffset(endOffset);
    		if (begin != end && begin < end) {
    			int len = (end-begin)+1;
    			ArrayList<FieldsCompare> alst = new ArrayList<FieldsCompare>();
    			// get each line and check for presence of 'field'
    			for (int i = 0; i < len; i++) {
    				IRegion region = document.getLineInformation(begin+i);
    				String txt = document.get(region.getOffset(),region.getLength());
    				String[] split = txt.split(SPLIT_ON);
    				int splen = split.length;
    				int idx = (back ? splen-index : index); 
    				if (idx >= 0 && idx < splen) {
    					alst.add(getComparable(txt,split[idx]));
    				} else {
    					asyncShowMessage(editor,String.format(INSUFFICIENT_FIELDS,txt), true);
    					return NO_OFFSET;
    				}
    			}
    			// sort on 'field'
    			Collections.sort(alst);
    			String[] array = new String[alst.size()];
    			int i = 0;
    			for (Comparable<?> n : alst) {
    				array[i++] = n.toString();
    			}
    			updateLines(document,selection,array);    			
    		} else {
    			EmacsPlusUtils.showMessage(editor, NO_REGION, true);
    		}
    	} else {
    		EmacsPlusUtils.showMessage(editor, NO_REGION, true);
    	}
    	return result;
    }

	/**
	 * @see com.mulgasoft.emacsplus.commands.EmacsPlusCmdHandler#isLooping()
	 */
	protected boolean isLooping() {
		return false;
	}

}
