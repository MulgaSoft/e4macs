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

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import org.eclipse.jface.bindings.Binding;
import org.eclipse.jface.bindings.keys.KeySequence;
import org.eclipse.jface.bindings.keys.KeyStroke;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.ui.keys.IBindingService;
import org.eclipse.ui.texteditor.ITextEditor;

import com.mulgasoft.emacsplus.EmacsPlusActivator;
import com.mulgasoft.emacsplus.IEmacsPlusCommandDefinitionIds;
import com.mulgasoft.emacsplus.MarkUtils;
import com.mulgasoft.emacsplus.RingBuffer.IRingBufferElement;

/**
 * Support regular and regexp i-search minibuffer processing.
 * Used directly by: isearch-forward, isearch-backward, isearch-forward-regexp, isearch-backward-regexp
 * 
 * Support typing `M-c' within an incremental search to toggle the case sensitivity of that search.  
 * The effect does not extend beyond the current incremental search to the next one, but it does override
 * the effect of adding or removing an upper-case letter in the current search.
 * 
 * @author Mark Feber - initial API and implementation
 */
public class ISearchMinibuffer extends SearchMinibuffer {

	private static final String IS_WRAP_MARKER = EmacsPlusActivator.getResourceString("IS_Wrap_Marker");			  //$NON-NLS-1$
	private static final String IS_NOT_FOUND = EmacsPlusActivator.getResourceString("IS_Not_Found");				  //$NON-NLS-1$
	private static final String IS_FORWARD = EmacsPlusActivator.getResourceString("IS_Forward");					  //$NON-NLS-1$
	private static final String IS_BACKWARD = EmacsPlusActivator.getResourceString("IS_Backward");  				  //$NON-NLS-1$
	private static final String IS_REGEXP = EmacsPlusActivator.getResourceString("IS_Regexp");  					  //$NON-NLS-1$
	
	private final static char SPACE = ' ';
	private static final String IS_BACKWARD_P = IS_FORWARD + SPACE + IS_BACKWARD ;
	private static final String IS_FORWARD_REGEXP_P = IS_REGEXP + SPACE + IS_FORWARD;
	private static final String IS_BACKWARD_REGEXP_P = IS_REGEXP + SPACE + IS_BACKWARD_P;

	private static final String ISF = IEmacsPlusCommandDefinitionIds.ISEARCH_FORWARD;
	private static final String ISB = IEmacsPlusCommandDefinitionIds.ISEARCH_BACKWARD;
	private static final String ISRF = IEmacsPlusCommandDefinitionIds.ISEARCH_REGEXP_FORWARD;
	private static final String ISRB = IEmacsPlusCommandDefinitionIds.ISEARCH_REGEXP_BACKWARD;
	
	private static final int FORWARD = -1;
	private static final int REVERSE = -2;

	private boolean historyUpdated = false;
	private boolean checkHistoryUpdated = false;

	// hash table to cache current bindings of i-search(regexp)-forward/backward commands 
	private Map<Integer,Integer> keyHash = new HashMap<Integer,Integer>();
	// temporary flag for prefix computation
	boolean isAdding = false;

	/**
	 * Default is forward, not regexp
	 */
	public ISearchMinibuffer() {
		this(true, false);
	}
	
	/**
	 * @param forward - true if searching forward
	 * @param regexp - true if regexp searching
	 */
	public ISearchMinibuffer(boolean forward, boolean regexp) {
		super(forward, regexp);
	}

	protected void addIt(VerifyEvent event, boolean searcher) {	
		try {
			isAdding = true;
			super.addIt(event, searcher);
		} finally {
			isAdding = false;
			updateStatusLine();
		}
	}
	
	private String getSearchPrefix() {
		String result;
		if (isForward()) {
			result = (isRegexp() ? IS_FORWARD_REGEXP_P : IS_FORWARD);
		} else {
			result = (isRegexp() ? IS_BACKWARD_REGEXP_P : IS_BACKWARD_P);
		}
		return result;
	}
	
	/**
	 * @see com.mulgasoft.emacsplus.minibuffer.ExecutingMinibuffer#getMinibufferPrefix()
	 */
	public String getMinibufferPrefix() {
		boolean notFound = (!isAdding && !isFound() && getMBLength() > 0);
		String prefix = (notFound ? IS_NOT_FOUND : (isWrapped() ? IS_WRAP_MARKER : EMPTY_STR)) + getSearchPrefix(); 
		return prefix + KOLON;
	}
	
	protected void setWrapPosition(){
		super.setWrapPosition();
		updateStatusLine();
	}
	
	private void setHistoryUpdated(boolean val) {
		historyUpdated = val;
	}

	boolean wasHistoryUpdated() {
		return checkHistoryUpdated;
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
	 * Emacs style cancel:
	 *	"Ctrl-g: while searching or when search has failed cancels input back to what has been found successfully.
	 *   when search is successful aborts and moves point to starting point.";
	 */
	protected boolean cancelSearch() {
		if (!goToFoundState()) {
			addToHistory(getMBString(),getRXString());
			leave(getStartOffset());
			beep();
		}
		return true;
	}
	
	protected void leave() {
		// don't set mark if we're back at start
		if (isFound() || getStartOffset() != getTextWidget().getCaretOffset()) {
			MarkUtils.setMark(getEditor(),getMarkOffset());
		}
		super.leave();
	}
	
	/**
	 * @see com.mulgasoft.emacsplus.minibuffer.SearchMinibuffer#replaceFromHistory(com.mulgasoft.emacsplus.RingBuffer.IRingBufferElement)
	 */
	@Override
	protected void replaceFromHistory(IRingBufferElement<?> history) {
		try {
			setFound(false); // on incremental search, history update invalidates found state
			isAdding = true; // flag search string change for minibuffer prefix computation
			super.replaceFromHistory(history);
			if (history != null) {
				setHistoryUpdated(true);
			}
		} finally {
			isAdding = false;
		}
	}

	private boolean isSpecial(VerifyEvent event) {
		return ((event.stateMask & SWT.MODIFIER_MASK) == 0 && 
				((event.keyCode == SWT.ALT ) || (event.keyCode == SWT.CTRL) || (event.keyCode == SWT.SHIFT)));
	}
	
	/**
	 * @see com.mulgasoft.emacsplus.minibuffer.SearchMinibuffer#handleKey(org.eclipse.swt.events.VerifyEvent)
	 */
	@Override
	protected void handleKey(VerifyEvent event) {
		// ensure flag is only enabled for one round
		// to allow CR after history update
		if (!isSpecial(event) && (checkHistoryUpdated = historyUpdated)) {
			setHistoryUpdated(false);
		}
		super.handleKey(event);
	}

	/**
	 * @see com.mulgasoft.emacsplus.minibuffer.SearchMinibuffer#findNext(java.lang.String)
	 */
	@Override
	protected boolean findNext(String searchStr) {
		boolean wasFound = isFound();
		boolean result = super.findNext(searchStr); 
		if (!result && wasFound) {
			updateStatusLine();
		}
		return result; 
	}

	/**
	 * @see com.mulgasoft.emacsplus.minibuffer.SearchMinibuffer#executeResult(org.eclipse.ui.texteditor.ITextEditor, java.lang.Object)
	 */
	@Override
	protected boolean executeResult(ITextEditor editor, Object commandResult) {
		if (wasHistoryUpdated()) {
			findNext(getSearchString());
			return false;
		} else {
			return super.executeResult(editor, commandResult);
		}
	}

	private boolean repeatSearch(boolean direction) {
		boolean result = true;
		try {
			setSearching(true);			
			saveState();
			String searchStr = getSearchString();
			if (searchStr == null || searchStr.length() == 0) {
				// if empty, try to initialize search string on repeat
				replaceFromHistory();
				searchStr = getSearchString();
				if (searchStr == null || searchStr.length() == 0) {
					return result;
				}
			} else {
				addToHistory(getMBString(), getRXString());
				if (isFound() || wasHistoryUpdated()) {
					if (isForward()) {
						Point p = getSelection();
						// get (actual or implicit) width to increment offset
						if (p != null ){
							setSearchOffset(getSearchOffset()+ (p.y == 0 ? getEol().length() : p.y));									
						}
					}
				} else {
					// its a wrap
					setSearchOffset(WRAP_INDEX);
					if (!isWrapped()) {
						setWrapPosition();
					}
				}
			}
			findNext(searchStr);
		} finally {
			setSearching(false);
			updateStatusLine();
		}
		return result;
	}

	boolean wasFound = true;
	boolean quoteIt = false;
	
	/**
	 * @see com.mulgasoft.emacsplus.minibuffer.SearchMinibuffer#findNext(java.lang.String, boolean)
	 */
	@Override
	protected boolean findNext(String searchStr, boolean addit) {
		boolean result = super.findNext(searchStr, addit);
		if (!result && getMBLength() > 0) {
			// ignore regexp syntax failures as we build string			
			boolean skipIt = addit && !checkRegexp();	
			if (wasFound && (!skipIt && !quoteIt)) {
				beep();
				wasFound = false;
			}
			if (!skipIt) {
				quoteIt = false;
			}
		} else {
			wasFound = true;
		}
		return result;
	}
	
	/**
	 * Sanity check the regular expression
	 * 
	 * @return true if not regexp or is regexp and pattern compiles, else false
	 */
	private boolean checkRegexp() {
		boolean result = true;
		if (isRegexp()) {
			try {
				Pattern.compile(getSearchString());
			} catch (Exception e) {
				result = false;
			}
		}
		return result;
	}

	/**
	 * @see com.mulgasoft.emacsplus.minibuffer.SearchMinibuffer#dispatchCtrl(org.eclipse.swt.events.VerifyEvent)
	 */
	@Override
	protected boolean dispatchCtrl(VerifyEvent event) {
		boolean result = true;

		switch (checkKeyCode(event)) {
			case FORWARD:
				forwardSearch();
				break;
			case REVERSE:
				reverseSearch();
				break;
			default:
				result = super.dispatchCtrl(event);
		}
		return result;
	}
	
	/**
	 * @see com.mulgasoft.emacsplus.minibuffer.SearchMinibuffer#dispatchAlt(org.eclipse.swt.events.VerifyEvent)
	 */
	protected boolean dispatchAlt(VerifyEvent event) {
		boolean result = true;
		switch (checkKeyCode(event)) {
			case FORWARD:
				forwardSearch();
				break;
			case REVERSE:
				reverseSearch();
				break;
			case CASE:
				toggleCase(super.isCaseSensitive());
				break;
			default:
				result = super.dispatchAlt(event);
		}
		return result;
	}

	/**
	 * Check for C-M bindings for the i-search forward/reverse commands
	 * 
	 * @see com.mulgasoft.emacsplus.minibuffer.SearchMinibuffer#dispatchAltCtrl(org.eclipse.swt.events.VerifyEvent)
	 */
	@Override
	protected boolean dispatchAltCtrl(VerifyEvent event) {
		boolean result = true;
		switch (checkKeyCode(event)) {
		case FORWARD:
			forwardSearch();
			break;
		case REVERSE:
			reverseSearch();
			break;

		default:
			result = super.dispatchAltCtrl(event); 
		}
		return result;
	}

	private void forwardSearch() {
		if (!isForward()){
			if (isFound()) {
				setSearchOffset(getSearchOffset() + getMBLength());
			}
			setForward(true);
			updateStatusLine();
		}
		repeatSearch(isForward());			
	}
	
	private void reverseSearch() {
		if (isForward()){
			if (isFound()) {
				setSearchOffset(getSearchOffset() - getMBLength());
			}
			setForward(false);
			updateStatusLine();
		}
		repeatSearch(isForward());

	}

	/**
	 * Dynamically determine the bindings for forward and reverse i-search
	 * For repeat search, Emacs treats i-search and i-search-regexp identically
	 * 
	 * @param event
	 * @return the FORWARD, REVERSE, or the source keyCode
	 */
	private int checkKeyCode(VerifyEvent event) {
		int result = event.keyCode;
		Integer val = keyHash.get(Integer.valueOf(event.keyCode + event.stateMask));
		if (val == null) { 
			KeyStroke keyst = KeyStroke.getInstance(event.stateMask, Character.toUpperCase(result));
			IBindingService bindingService = getBindingService();
			Binding binding = bindingService.getPerfectMatch(KeySequence.getInstance(keyst));
			if (binding != null) {
				if (ISF.equals(binding.getParameterizedCommand().getId())){
					result = FORWARD;
					keyHash.put(Integer.valueOf(event.keyCode + event.stateMask),Integer.valueOf(FORWARD));
				} else if (ISB.equals(binding.getParameterizedCommand().getId())) {
					result = REVERSE;
					keyHash.put(Integer.valueOf(event.keyCode + event.stateMask),Integer.valueOf(REVERSE));
				} else if (ISRF.equals(binding.getParameterizedCommand().getId())) {
					result = FORWARD;
					keyHash.put(Integer.valueOf(event.keyCode + event.stateMask),Integer.valueOf(FORWARD));
				} else if (ISRB.equals(binding.getParameterizedCommand().getId())) {
					result = REVERSE;
					keyHash.put(Integer.valueOf(event.keyCode + event.stateMask),Integer.valueOf(REVERSE));
				} 
			}
		} else {
			result = val;
		}
		return result;
	}
}
