/**
 * Copyright (c) 2009, 2013 Mark Feber, MulgaSoft
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 */
package com.mulgasoft.emacsplus.minibuffer;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IFindReplaceTarget;
import org.eclipse.jface.text.IFindReplaceTargetExtension3;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.text.undo.DocumentUndoManagerRegistry;
import org.eclipse.text.undo.IDocumentUndoManager;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.texteditor.ITextEditor;

import com.mulgasoft.emacsplus.EmacsPlusActivator;
import com.mulgasoft.emacsplus.MarkUtils;
import com.mulgasoft.emacsplus.RegexpRingBuffer;
import com.mulgasoft.emacsplus.RingBuffer;
import com.mulgasoft.emacsplus.preferences.EmacsPlusPreferenceConstants;

/**
 * Emacs style replace & query-replace minibuffer
 * 
 * When used for query-replace, it supports the following sub-commands:
 * Space or `y' to replace one match
 * Delete or `n' to skip to next
 * RET or `q' to exit
 * Period to replace one match and exit
 * Comma to replace but not move point immediately
 * ! to replace all remaining matches with no more questions
 * 
 * TODO - add case-fold-search and case-replace 
 * Matching is independent of case if `case-fold-search' is non-nil and
 * FROM-STRING has no uppercase letters.  Replacement transfers the case
 * pattern of the old text to the new text, if `case-replace' and
 * `case-fold-search' are non-nil and FROM-STRING has no uppercase
 * letters.  \(Transferring the case pattern means that if the old text
 * matched is all caps, or capitalized, then its replacement is upcased
 * or capitalized.)
 * 
 * @author Mark Feber - initial API and implementation
 */
public class SearchReplaceMinibuffer extends SearchMinibuffer {

	// TODO - html files don't seem to come with a undoer - can we add?
	
	// Preference: use yank & yank pop for C-y & M-y if true
	// else use unified approach
	private static boolean GNU_YANK = EmacsPlusActivator.getDefault().getPreferenceStore().getBoolean(EmacsPlusPreferenceConstants.P_GNU_YANK);
	
	// TODO - preferences ? contextualized sub-commands?
	private static final char QUIT = 'q';
	private static final char YES = 'y';
	private static final char YES_SP = ' ';
	private static final char NO = 'n';
	private static final char ALL = '!';
	private static final char PAUSE = ',';
	private static final char DO_ONE = '.';

	private final static char QUESTION = '?';
	
	private static final  String QR_REPLACE = EmacsPlusActivator.getResourceString("QR_Replace");   			//$NON-NLS-1$
	private static final  String QR_QUERY = EmacsPlusActivator.getResourceString("QR_Query");   				//$NON-NLS-1$
	private static final  String QR_REPLACE_STR = EmacsPlusActivator.getResourceString("QR_Replace_Str");   	//$NON-NLS-1$
	private static final  String QR_REPLACE_REGEXP = EmacsPlusActivator.getResourceString("QR_Replace_Regexp"); //$NON-NLS-1$
	private static final  String QR_QUERY_STR = EmacsPlusActivator.getResourceString("QR_Query_Str");   		//$NON-NLS-1$
	
	private static final  String QR_QUERY_WITH = EmacsPlusActivator.getResourceString("QR_Query_With"); 		//$NON-NLS-1$
	private static final  String QR_REPLACE_END = EmacsPlusActivator.getResourceString("QR_Replace_End");   	//$NON-NLS-1$
	private static final  String QR_REPLACE_ENDS = EmacsPlusActivator.getResourceString("QR_Replace_Ends"); 	//$NON-NLS-1$
	
	private String prefix = null;
	
	private String replaceStr;
	private String userReplaceStr;
	private String searchStr;
	private String displayStr;
	// if a selection is present, this is the end of it (in widget coords)
	private Position regionLimit = null;
	
	private boolean replaceAll = false;
	
	private enum QueryState {
		Query, Replace, Search
	}
	
	private QueryState state = QueryState.Query;
	
	/*
	 * Emacs q/r commands on match:
	 * Type Space or `y' to replace one match, Delete or `n' to skip to next,
	 * RET or `q' to exit, Period to replace one match and exit,
	 * Comma to replace but not move point immediately,
	 * ! to replace all remaining matches with no more questions,
	 * 
	 * Not Implemented: C-r to enter recursive edit (M-C-c to get out again),
	 * Not Implemented: C-w to delete match and recursive edit,
	 * Not Implemented: C-l to clear the frame, redisplay, and offer same replacement again,
	 * Not Implemented: ^ to move point back to previous match.
	 */
	
	/*
	 * From the q/r doc:
	 * Preserves case in each replacement if `case-replace' and `case-fold-search'
	 * are non-nil and FROM-STRING has no uppercase letters.
	 * (Preserving case means that if the string matched is all caps, or capitalized,
	 * then its replacement is upcased or capitalized.)
	 */
	
	public SearchReplaceMinibuffer() {
		this(false);
	}
	
	public SearchReplaceMinibuffer(boolean regexp) {
		this(regexp,false);
	}
	
	public SearchReplaceMinibuffer(boolean regexp, boolean replaceAll) {
		super(true, regexp);
		setReplaceAll(replaceAll);
		setIncrFind(true);
		setGnuSubCommands(GNU_YANK);
	}
	
	public boolean beginSession(ITextEditor editor, IWorkbenchPage page, ExecutionEvent event) {
		setLimit(WRAP_INDEX);	// initialize to ~infinite
		boolean result = super.beginSession(editor, page, event); 
		if (result) {
			ISelection selection = editor.getSelectionProvider().getSelection();
			if (selection instanceof ITextSelection) {
				ITextSelection sel = (ITextSelection)selection;
				// constrain search/replace to selected region
				if (sel.getLength() > 0) {
					// set end of this replacement in model coords
					// so we include any auto-expand regions
					setLimit(sel.getOffset() + sel.getLength());
					// force start to beginning of selection rather than cursor
					setStartOffset(getTextWidget().getSelectionRange().x);
					setSearchOffset(getStartOffset());
				}
			}
		}
		return result;
	}
	
	/**
	 * @see com.mulgasoft.emacsplus.minibuffer.HistoryMinibuffer#removeOtherListeners(IWorkbenchPage, ISourceViewer, StyledText)
	 */
	@Override
	protected void removeOtherListeners(IWorkbenchPage page, ISourceViewer viewer, StyledText widget) {
		super.removeOtherListeners(page,viewer,widget);
		// remove the limit position, if present, on exit
		setLimit(WRAP_INDEX);
	}

	public static void enableGnuSubCommands(boolean gnuYank) {
		SearchReplaceMinibuffer.GNU_YANK = gnuYank;	
	}
	
	public String getMinibufferPrefix() {
		if (prefix == null) {
			prefix = getQueryPrefix();
		}
		return prefix;
	}
	
	private void setMinibufferPrefix(String prefix) {
		this.prefix = prefix;
	}
	
	private String getPrimaryPrefix() {
		String result = null;
		if (isReplaceAll()) {
			result = QR_REPLACE;
		} else {
			result = QR_QUERY;
		}
		return result;
	}

	private String getTypePrefix() {
		String result = getPrimaryPrefix();
		result = result + (isRegexp() ? QR_REPLACE_REGEXP : (isReplaceAll() ? QR_REPLACE_STR : QR_QUERY_STR));
		return result;
	}
	
	private String getQueryPrefix() {
		return getTypePrefix() + KOLON;
	}
	
	private String getReplacePrefix() {
		return getTypePrefix() + ' ' + normalizeString(getDisplayStr()) + ' ' + QR_QUERY_WITH + KOLON;
	}
	
	private String getSearchPrefix() {
		return QR_REPLACE + ' ' + normalizeString(getDisplayStr()) + ' ' + QR_QUERY_WITH + normalizeString(userReplaceStr) + QUESTION + KOLON;
	}

	/**
	 * @return the replaceAll
	 */
	protected boolean isReplaceAll() {
		return replaceAll;
	}

	/**
	 * @param replaceAll the replaceAll to set
	 */
	public void setReplaceAll(boolean replaceAll) {
		this.replaceAll = replaceAll;
	}
		/**
	 * @return the displayStr
	 */
	protected String getDisplayStr() {
		return displayStr;
	}

	/**
	 * @param displayStr the displayStr to set
	 */
	protected void setDisplayStr(String displayStr) {
		this.displayStr = displayStr;
	}

	/**
	 * @return the searchStr
	 */
	protected String getSearchStr() {
		return searchStr;
	}

	/**
	 * @param searchStr the searchStr to set
	 */
	protected void setSearchStr(String searchStr) {
		this.searchStr = searchStr;
	}

	/**
	 * Set the end of the limitRegion
	 * Use model coords so that if there is a collapsed section in the selected
	 * region, any auto-expand during search will be accounted for in the position 
	 * 
	 * @param val in model coords or WRAP_INDEX to disable
	 */
	private void setLimit(int val) {
		IDocument doc;
		if (val == WRAP_INDEX) {
			if (regionLimit != null && (doc = getDocument()) != null) {
				doc.removePosition(regionLimit);
			}
			regionLimit = null;
		} else if ((doc = getDocument()) != null) {
			try {
				regionLimit = new Position(val);
				doc.addPosition(regionLimit);
			} catch (BadLocationException e) {
				regionLimit = null;				
			}
		}
	}
	
	private int getLimit() {
		return (regionLimit != null ? regionLimit.getOffset() : WRAP_INDEX);
	}
	
	/**
	 * @see com.mulgasoft.emacsplus.minibuffer.SearchMinibuffer#executeResult(org.eclipse.ui.texteditor.ITextEditor, java.lang.Object)
	 */
	protected boolean executeResult(ITextEditor editor, Object minibufferResult) {
		boolean result = false;
		switch(state) {
		case Query:
			state = QueryState.Replace;
			String searchStr = getSearchString();
			if (searchStr == null || searchStr.length() == 0) {
				finish();
			}
			setSearchStr(searchStr);
			setDisplayStr(getMBString());
			addToHistory(getMBString(),getRXString());
			setMinibufferPrefix(getReplacePrefix());
			initMinibuffer(EMPTY_STR);
			break;
		case Replace:
			state = QueryState.Search;
			userReplaceStr = getMBString();
			addToHistory(userReplaceStr);
			// Gnu fix ups 
			replaceStr = convertGnuStr(userReplaceStr);
			if (!isFound()) {
				// TODO: when implementing tags version, keep looking through files until found
				// findNextFile();
				// else
				finish();
				result = true;
				break;
			}
			if (isReplaceAll()) {
				replaceAll();
				result = true;
			} else {
				setMinibufferPrefix(getSearchPrefix());
			}
			initMinibuffer(EMPTY_STR);
			break;
		case Search:
			// time to go
			result = true;
			finish();
			break;
		}
		return result;
	}

	// TODO: preference?
	private boolean isGnuSyntax() {
		return true;
	}
	
	/**
	 * Examine replacement string for gnu syntax
	 * 	entire string: \& -> \0 
	 * @param rplString
	 * @return rplString with replacements if gnuSyntax supported
	 */
	private String convertGnuStr(String rplString) {
		String newStr = rplString;
		if (isRegexp() && isGnuSyntax() && rplString.indexOf('\\') > -1) {
			char[] chars = new char[rplString.length()];
			newStr.getChars(0, newStr.length(), chars, 0);
			boolean backSlash = false;
			for (int i=0; i<chars.length; i++) {
				if (chars[i] == '\\') {
					backSlash = !backSlash;
				} else if (backSlash && chars[i] == '&') {
					chars[i] = '0';
					backSlash = false;
				} else {
					backSlash = false;
				}
			}
			newStr = new String(chars);
		}
		return newStr;
	}
	
	protected void checkCasePos(String str) {
		switch(state) {
			case Query:
				// only check case on search string
				super.checkCasePos(str);
				break;
			default:
				break;
		}
	}
	
	/**
	 * @see com.mulgasoft.emacsplus.minibuffer.SearchMinibuffer#addIt(org.eclipse.swt.events.VerifyEvent)
	 */
	@Override
	protected void addIt(VerifyEvent event) {
		switch(state) {
		case Query:
			super.addIt(event);
			break;
		case Replace:
			super.addIt(event,false);
			break;
		case Search:
			processTextInput(event);
			break;
		}
	}

	// Local interpretation of BS and DEL
	protected void backSpaceChar(VerifyEvent event){
		switch (state) {
		case Query:
			popSearchState();
			event.doit = false;
			break;
		case Search:
			skipReplace();
			event.doit = false;
			break;
		default:
			super.backSpaceChar(event);
		}
	}
	
	protected void deleteChar(VerifyEvent event){
		switch (state) {
		case Query:
			popSearchState();
			event.doit = false;
			break;
		case Search:
			skipReplace();
			event.doit = false;
			break;
		default:
			super.deleteChar(event);
		}
	}
		
	/**
	 * @see com.mulgasoft.emacsplus.minibuffer.SearchMinibuffer#dispatchCtrl(org.eclipse.swt.events.VerifyEvent)
	 */
	@Override
	protected boolean dispatchCtrl(VerifyEvent event) {
		boolean result = false;
		switch(state) {
		case Query:
			result = super.dispatchCtrl(event);
			break;
		case Replace:
			if (isQuoting()) {
				return super.dispatchCtrl(event,false);
			} else {
				switch (event.keyCode) {
				case EOL:
					result = super.dispatchCtrl(event,false);					
					break;
				case CTRL_QUOTE:
					result = super.dispatchCtrl(event);
					break;
				case LINEorYANK:
					if (isGnuYankCommands()) {
						boolean wasEnabled = isSearchEnabled(); 
						try {
							setSearchEnabled(false);
							result = super.dispatchCtrl(event);
						} finally {
							setSearchEnabled(wasEnabled);
						}
						break;
					}
				case CANCEL:
				case WORD:
					result = cancelSearch();
					break;
				}
			}
			break;
		case Search:
			switch (event.keyCode) {
			case CANCEL:
				result = cancelSearch();
				break;
			}
			finish();
			break;
		}
		return result;
	}
	
	/**
	 * @see com.mulgasoft.emacsplus.minibuffer.SearchMinibuffer#dispatchAlt(org.eclipse.swt.events.VerifyEvent)
	 */
	@Override
	protected boolean dispatchAlt(VerifyEvent event) {
		boolean result = false;
		switch(state) {
		case Query:
			result = super.dispatchAlt(event);
			break;
		case Replace:
			try {
				setSearchEnabled(false);
				result = super.dispatchAlt(event);
			} finally {
				setSearchEnabled(true);
			}
			break;
		case Search:
			finish();
			break;
		}
		return result;
	}

	/**
	 * Reset the search indexes and find 'new' string
	 * 
	 * @see com.mulgasoft.emacsplus.minibuffer.HistoryMinibuffer#historyTransition(int)
	 */
	protected void 	historyTransition(int keyCode) {
		switch(state) {
		case Query:
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
		default:
			break;
		}
	}
	
	/**
	 * @see com.mulgasoft.emacsplus.minibuffer.SearchMinibuffer#dispatchAltCtrl(org.eclipse.swt.events.VerifyEvent)
	 */
	protected boolean dispatchAltCtrl(VerifyEvent event) {
		boolean result = false;
		switch(state) {
		case Query:
			// only when building search string
			result = super.dispatchAltCtrl(event,true);
			break;
		case Replace:
		case Search:
		default:
			finish();
			break;
		}
		return result;
	}
	
	/**
	 *  Return to start offset if not in Query state or can't return to a found state
	 *
	 * @see com.mulgasoft.emacsplus.minibuffer.SearchMinibuffer#cancelSearch()
	 */
	protected boolean cancelSearch() {
		if (state != QueryState.Query || !goToFoundState()) {
			leave(getStartOffset());
		}
		return true;
	}	
	
	private int findCount = 0;
	private boolean paused = false;

	private void processTextInput(VerifyEvent event) {
		switch (event.character) {
		case ALL:
			replaceAll();
			break;
		case PAUSE:
			// replace one (if not paused) and wait
			if (!paused) {
				replaceIt();
				paused = true;
			}
			break;
		case DO_ONE:
			// replace one and exit
			if (replaceIt()){
				finish();
			}
			break;
		case YES_SP:
		case YES:
			// replace
			if (replaceIt() && !findNext(getSearchStr()))
				finish();
			break;
		case NO:
			// move along to the next
			skipReplace();
			break;
		case QUIT:
			finish();
			break;
		default:
			// else add the character and terminate
			finish();
		    this.resendEvent(event);
		}

	}
	
	private boolean checkPaused() {
		boolean result = paused;
		if (paused) {
			paused = false;
			if (!findNext(getSearchStr()))
				finish();
		} 
		return result;
	}

	/**
	 * Refine target search to not proceed beyond a region limit, if present
	 *  
	 * @see com.mulgasoft.emacsplus.minibuffer.SearchMinibuffer#findTarget(org.eclipse.jface.text.IFindReplaceTarget, java.lang.String, int, boolean)
	 */
	protected int findTarget(IFindReplaceTarget target, String searchStr, int searchIndex, boolean forward) {
		int result = WRAP_INDEX;
		StyledText text = getTextWidget();		
		try {
			text.setRedraw(false);
			result = super.findTarget(target,searchStr,searchIndex,forward);
			// if present, check if we've gone past the end of the region 
			if (getLimit() != WRAP_INDEX && result != WRAP_INDEX) {
				int pos = MarkUtils.widget2ModelOffset(getViewer(), result);
				// include length of search result in check
				if (pos + target.getSelection().y > getLimit()) {
					result = WRAP_INDEX;
					// restore last position the region
					setSelection(searchIndex);
				}
			}
		} finally {
			text.setRedraw(true);
		}
		return result;
	}
	
	/**
	 * Skip a single replacement 
	 */
	private void skipReplace() {
		if (!checkPaused()) { 
			StyledText w = getTextWidget();
			if (w != null && !w.isDisposed()) {
				// position at the end of the current proposed replacement
				setSearchOffset(w.getCaretOffset());
			}
			if (!findNext(getSearchStr()))
				finish();		
		}
	}
	
	/**
	 * Replace all occurrences
	 */
	private void replaceAll() {
		boolean allState = isReplaceAll(); 
		IDocumentUndoManager undoer = DocumentUndoManagerRegistry.getDocumentUndoManager(getDocument());
		try {
			paused = false;
			setReplaceAll(true);	// force flag so we don't re-enter the undoer during case replacement
			if (undoer != null) {
				undoer.beginCompoundChange();
			}
			replaceIt();
			while (findNext(getSearchStr())){
				replaceIt();
			}
		} finally {
			setReplaceAll(allState);
			if (undoer != null) {
				undoer.endCompoundChange();
			}
		}
		finish();
	}

	/**
	 * Perform one replacement
	 * 
	 * @return the replacement index
	 */
	private boolean replaceIt() {
		boolean result = false;
		boolean forward = true;
		if (!checkPaused()) { 
			findCount++;
			try {
				result = true;
				int index = getSearchOffset();
				IFindReplaceTarget target = getTarget();
				int fpos = findTarget(target,getSearchStr(), index, forward);
				if (fpos > -1) {
					boolean initial = false;
					boolean all = false;
					String replacer = replaceStr;
					if (!isCaseSensitive() && replacer.length() > 0) {
						// Preserve case using the emacs definition
						// - Preserving case means that if the string matched is all caps, or capitalized,
						//   then its replacement is upper cased or capitalized.)
						String replacee = target.getSelectionText().trim();
						if (replacee != null && replacee.length() > 0) {
							initial = Character.isUpperCase(replacee.charAt(0));
							all = initial;
							if (initial) {
								for (int i = 1; i < replacee.length(); i++) {
									if (!Character.isUpperCase(replacee.charAt(i))) {
										all = false;
										break;
									}
								}
							}
						}
					}
					int adjust = 0;
					if (all || initial) {
						caseReplace(replacer,index,all);
					} else {
						if (isRegexLD()) {
							adjust = replacer.length();	// prevents repetitious find of same EOL
							// now we need the offset in model coords
							ITextSelection sel = (ITextSelection)getEditor().getSelectionProvider().getSelection();
							// use document 'insert' instead of broken target replace
							getDocument().replace(sel.getOffset(),0,replacer);
							// search uses cursor position - s/r is always forward
							MarkUtils.setCursorOffset(getEditor(), sel.getOffset()+adjust);
						} else {
							((IFindReplaceTargetExtension3)target).replaceSelection(replacer, isRegexp());
						}
					}
					Point p = target.getSelection();
					int rIndex = p.x + p.y + adjust;
					setSearchOffset(rIndex);					
				}
			} catch (Exception e) {
				setResultString(e.getLocalizedMessage(), true);
				finish();
				beep();
			}
		}
		return result;
	}

	/**
	 * Case-based replacement - after the initial find has already happened
	 * 
	 * @param replacer - the replacement string (may be regexp)
	 * @param index - offset of find
	 * @param all - all if true, else initial
	 * @return - the replacement region
	 * 
	 * @throws BadLocationException
	 */
	private IRegion caseReplace(String replacer, int index, boolean all) throws BadLocationException {
		IRegion result = null;
		IDocumentUndoManager undoer = DocumentUndoManagerRegistry.getDocumentUndoManager(getDocument());
		try {
			if (!isReplaceAll() && undoer != null) {
				undoer.beginCompoundChange();
			}
			IFindReplaceTarget target = getTarget();
			// first replace with (possible regexp) string
			((IFindReplaceTargetExtension3)target).replaceSelection(replacer, isRegexp());
			// adjust case of actual replacement string
			replacer = target.getSelectionText();
			String caseReplacer = replacer;
			if (all) {
				caseReplacer = caseReplacer.toUpperCase();
			} else {
				caseReplacer = caseReplacer.trim();
				caseReplacer = Character.toUpperCase(caseReplacer.charAt(0)) + 
				caseReplacer.substring(1,caseReplacer.length()).toString();
				// now update the replacement string with the re-cased part
				caseReplacer = replacer.replaceFirst(replacer.trim(), caseReplacer);
			}
			int ind = target.findAndSelect(index, replacer, true, false, false);
			if (ind > -1) {
				target.replaceSelection(caseReplacer);
			}
		}  finally {
			if (!isReplaceAll() && undoer != null) {
				undoer.endCompoundChange();
			}
		}
		return result;
	}

	private void finish(){
		setMinibufferPrefix(EMPTY_STR);
		if (getResultString() == null) {
			setResultString(String.format(getTypePrefix() + ' ' + (findCount == 1 ? QR_REPLACE_END : QR_REPLACE_ENDS), findCount),false);
		}
		if (isFound() || (findCount > 0 && (isReplaceAll() || getSearchOffset() == WRAP_INDEX))){
			StyledText w = getTextWidget();
			if (w != null && !w.isDisposed()) {
				// position at the end of the current proposed replacement
				setSearchOffset(w.getCaretOffset());
			}
		}
		setSelection();
		MarkUtils.setMark(getEditor(),getMarkOffset());
		leave();
	}
	
	private void setSelection(int off) {
		ISourceViewer viewer = getViewer();
		viewer.setSelectedRange(MarkUtils.widget2ModelOffset(viewer, off), 0);
	}
	
	private void setSelection() {
		setSelection(getSearchOffset());
	}
	
	/**** Local RingBuffers: use lazy initialization holder class idiom ****/

	/**
	 * @see com.mulgasoft.emacsplus.minibuffer.HistoryMinibuffer#getHistoryRing()
	 */
	@Override
	protected RingBuffer<String> getHistoryRing() {
		return (isRegexp() ? QRegexpRing.ring : QRRing.ring);
	}
	
	private static class QRRing {
		static final RingBuffer<String> ring = new RingBuffer<String>();		
	}
	
	private static class QRegexpRing {
		static final RegexpRingBuffer ring = new RegexpRingBuffer();
	}

}
