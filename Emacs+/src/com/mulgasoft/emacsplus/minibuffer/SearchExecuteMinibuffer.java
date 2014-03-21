/**
 * Copyright (c) 2009, 2010 Mark Feber, MulgaSoft
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 */
package com.mulgasoft.emacsplus.minibuffer;

import org.eclipse.jface.text.ITextSelection;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.ui.texteditor.ITextEditor;

import com.mulgasoft.emacsplus.MarkUtils;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import com.mulgasoft.emacsplus.execute.ISearchResult;

/**
 * Provide minibuffer with text searching, the result of which is passed to the command for execution
 *  
 * @author Mark Feber - initial API and implementation
 */
public class SearchExecuteMinibuffer extends SearchMinibuffer {
	
	IMinibufferExecutable executable;
	
	public SearchExecuteMinibuffer(IMinibufferExecutable executable) {
		this.setRegexp(true);
		this.executable = executable;
	}
	
	/**
	 * Prime the minibuffer with the supplied text (which must be guaranteed to be present and selected in the buffer)
	 *  
	 * @param selection
	 */
	public void initMinibufferSelection(ITextSelection selection){
		if (selection != null && selection.getLength() > 0) {
			String text = selection.getText();
			saveState();
			try {
				// temporarily disable selection changed
				setSearching(true);				
				MarkUtils.setSelection(getEditor(),selection);
			} finally {
				setSearching(false);
			}
			initMinibuffer(text);
			checkCasePos(text);
			addToHistory();
			// force start (for history searches) to beginning of text rather than cursor
			setStartOffset(getTextWidget().getSelectionRange().x);
			setFound(true);
		}
	}
	
	/**
	 * @see com.mulgasoft.emacsplus.minibuffer.SearchMinibuffer#getMinibufferPrefix()
	 */
	@Override
	protected String getMinibufferPrefix() {
		return executable.getMinibufferPrefix();
	}
	
	/**
	 * @see com.mulgasoft.emacsplus.minibuffer.SearchMinibuffer#executeResult(org.eclipse.ui.texteditor.ITextEditor, java.lang.Object)
	 */
	protected boolean executeResult(ITextEditor editor, Object commandResult) {
		boolean result = true;
		final String searchStr = getRXString();
		try {
			addToHistory();
			// syntax check
			if (isRegexp()) {
				Pattern.compile(searchStr);
			}
			result =  executable.executeResult(editor, new ISearchResult() {
				public String getSearchStr() {
					return searchStr;
				}

				public boolean isCaseSensitive() {
					return SearchExecuteMinibuffer.this.isCaseSensitive();
				}
			});
		} catch (PatternSyntaxException p) {
				setResultString(p.getLocalizedMessage(), true);
		}
		return result;
	}

	/**
	 * @see com.mulgasoft.emacsplus.minibuffer.SearchMinibuffer#dispatchAlt(org.eclipse.swt.events.VerifyEvent)
	 */
	protected boolean dispatchAlt(VerifyEvent event) {
		boolean result = false;
		if (event.keyCode == CASE) {
			toggleCase(super.isCaseSensitive());
			result = true;
		} else  {
			result = super.dispatchAlt(event);
		}
		return result;
	}
	
	/**
	 * Reset the search indexes and find 'new' string
	 * 
	 * @see com.mulgasoft.emacsplus.minibuffer.HistoryMinibuffer#historyTransition(int)
	 */
	protected void 	historyTransition(int keyCode) {
		switch (keyCode) {
			case NEXT:
			case PREV:
			case NEXT_ARROW:
			case PREV_ARROW:
				String str = getSearchString();
				if (str != null && str.length() > 0) {
					// first jump to start position
					int off = getStartOffset();
					setSearchOffset(off);
					getTextWidget().setCaretOffset(off);
					findNext(str);
				}
				break;

		}
	}
	
	/**
	 * @see com.mulgasoft.emacsplus.minibuffer.WithMinibuffer#backSpaceChar(org.eclipse.swt.events.VerifyEvent)
	 */
	protected void backSpaceChar(VerifyEvent event) {
		popSearchState();
		event.doit = false;		
	}
	
	/**
	 * @see com.mulgasoft.emacsplus.minibuffer.WithMinibuffer#deleteChar(org.eclipse.swt.events.VerifyEvent)
	 */
	protected void deleteChar(VerifyEvent event) {
		popSearchState();
		event.doit = false;		
	}

	/**
	 * @see com.mulgasoft.emacsplus.minibuffer.SearchMinibuffer#cancelSearch()
	 */
	protected boolean cancelSearch() {
		if (!goToFoundState()) {
			leave(getStartOffset());
		}
		return true;
	}		
}
