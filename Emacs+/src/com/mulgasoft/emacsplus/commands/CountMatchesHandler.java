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

import static com.mulgasoft.emacsplus.minibuffer.SearchMinibuffer.REGEX_BOL;
import static com.mulgasoft.emacsplus.minibuffer.SearchMinibuffer.REGEX_BOL_HACK;
import static com.mulgasoft.emacsplus.minibuffer.SearchMinibuffer.REGEX_EOL;
import static com.mulgasoft.emacsplus.minibuffer.SearchMinibuffer.REGEX_EOL_HACK;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.FindReplaceDocumentAdapter;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.ui.texteditor.ITextEditor;

import com.mulgasoft.emacsplus.EmacsPlusActivator;
import com.mulgasoft.emacsplus.minibuffer.ReadRegexpMinibuffer;

/**
 * Implements: count-matches
 * 
 * Print and return number of matches following point for REGEXP
 * 
 * As in Emacs, this handler starts looking for the next match from the end of the previous match;
 * therefore, it ignores matches that overlap a previously found match.
 * 
 * @author Mark Feber - initial API and implementation
 */
public class CountMatchesHandler extends MinibufferExecHandler implements INonEditingCommand  {

	private final static String OCCURRENCE = "Count_Match_Occurrence";  									   //$NON-NLS-1$
	private final static String OCCURRENCES = EmacsPlusActivator.getResourceString("Count_Match_Occurrences"); //$NON-NLS-1$

	/**
	 * @see com.mulgasoft.emacsplus.commands.EmacsPlusCmdHandler#transform(ITextEditor, IDocument, ITextSelection, ExecutionEvent)
	 */
	@Override
	protected int transform(ITextEditor editor, IDocument document, ITextSelection currentSelection, ExecutionEvent event)
	throws BadLocationException {
		return bufferTransform(new ReadRegexpMinibuffer(this), editor, event); 		
	}

	/**
	 * @see com.mulgasoft.emacsplus.commands.MinibufferExecHandler#doExecuteResult(org.eclipse.ui.texteditor.ITextEditor, java.lang.Object)
	 */
	@Override
	protected boolean doExecuteResult(ITextEditor editor, Object minibufferResult) {
		String msg = null;
		boolean isError = false;
		if (minibufferResult == null) {
			msg = String.format(msg, 0);
		} else {
			try {
				int count = 1;	// cursor is already at the end of the first find
				int begin = getCursorOffset(editor);
				String searchStr = getSearchStr(minibufferResult); 
				FindReplaceDocumentAdapter fda =  new FindReplaceDocumentAdapter(getThisDocument(editor));
				IRegion found = null;
				while ((found = getNextMatch(fda,begin,searchStr)) != null) {
					++count;
					int tmp = found.getOffset() + found.getLength();
					if (tmp != begin) {
						begin = tmp;
					} else {
						// offset should always move after match, but just in case
						msg = "Infinite loop on = " + searchStr;	//$NON-NLS-1$
						isError = true;
						break;
					}
				}
				if (msg == null) {
					msg = ((count == 1) ? OCCURRENCE : String.format(OCCURRENCES, count));
				}
			} catch (BadLocationException e) {
				// shouldn't happen, but alert user if it does
				msg = BAD_LOCATION_ERROR;
				isError = true;
			}
		}
		asyncShowMessage(editor, msg, isError);
		return true;
	}

	private String getSearchStr(Object minibufferResult) {
		String result = (String)minibufferResult;
		// workaround Eclipse bug in FindReplaceDocumentAdapter
		if (REGEX_BOL.equals(result)) {
			result = REGEX_BOL_HACK;
		} else if (REGEX_EOL.equals(result)) {
			result = REGEX_EOL_HACK;
		}
		return result;
	}
	
	/**
	 * Find the next match in the document and return its region
	 * 
	 * @param documentAdapter
	 * @param begin the search here
	 * @param regExp
	 * @return the found region or null
	 * @throws BadLocationException 
	 */
	private IRegion getNextMatch(FindReplaceDocumentAdapter fda, int begin, String regExp) throws BadLocationException {
		return fda.find(begin, regExp, true, false, false, true);
	}

}
