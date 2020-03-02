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

import java.util.Stack;
import java.util.regex.Pattern;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IFindReplaceTarget;
import org.eclipse.jface.text.IFindReplaceTargetExtension3;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.MarkSelection;
import org.eclipse.jface.text.TextEvent;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.texteditor.ITextEditor;

import com.mulgasoft.emacsplus.EmacsPlusUtils;
import com.mulgasoft.emacsplus.KillRing;
import com.mulgasoft.emacsplus.MarkUtils;
import com.mulgasoft.emacsplus.RegexpRingBuffer;
import com.mulgasoft.emacsplus.RegexpRingBuffer.RegexpRingBufferElement;
import com.mulgasoft.emacsplus.RingBuffer;
import com.mulgasoft.emacsplus.RingBuffer.IRingBufferElement;
import com.mulgasoft.emacsplus.preferences.PrefVars.SearchExitOption;
import static com.mulgasoft.emacsplus.preferences.PrefVars.SEARCH_EXIT_OPTION;
import static com.mulgasoft.emacsplus.EmacsPlusUtils.getPreferenceStore;

/**
 * Search minibuffer handles Ctrl and Alt
 * 
 * @author Mark Feber - initial API and implementation
 */
public abstract class SearchMinibuffer extends HistoryMinibuffer {

	private final static String WORD_EXP = "\\W*$*\\W*\\w*";					//$NON-NLS-1$
	private final static String LINE_EXP = ".*$";   							//$NON-NLS-1$
	
	protected final static String KOLON = ": "; 								//$NON-NLS-1$

	// Used to work around bug in org.eclipse.jface.text.FindReplaceDocumentAdapter
	public final static String REGEX_BOL = "^"; 								//$NON-NLS-1$ 
	public final static String REGEX_BOL_HACK = "^[\\s\\S]";					//$NON-NLS-1$
	public final static String REGEX_EOL = "$"; 								//$NON-NLS-1$ 
	public final static String REGEX_EOL_HACK = "$[\\s\\S]";					//$NON-NLS-1$ 
	
	private static final  String CASE_SENSITIVE = "Case_Sensitive_Message"; 	//$NON-NLS-1$
	private static final  String CASE_INSENSITIVE = "Case_Insensitive_Message"; //$NON-NLS-1$

	// TODO preferences?
	// Alts
	protected static final char YANK = 'y';
	protected static final char CASE = 'c';
	// Ctrls
	protected static final char EOL = 'j';
	protected static final char WORD = 'w';
	protected static final char LINEorYANK = 'y';
	protected static final char CM_DEL= 'w';
	protected static final char CM_ADD= 'y';
	protected static final char CANCEL = 'g';
	protected static final char CTRL_QUOTE = 'q';
	
	// position of the last upper case character
	private int currentCasePos = -1;
	private int wrapPosition = -1;
	
	private boolean found = false;

	private boolean forward = true;
	private boolean incrFind = true;
	// in widget offsets
	private int startOffset = 0;
	private int searchOffset = 0;
	// in doc offset
	private int markOffset = 0;

	protected static final int WRAP_INDEX = -1;
	
	private boolean regexp = false;
	
	private boolean quoting = false;
	
	private boolean searching = false;

	// if true, interpret C-y and M-y sub-commands as gnu yank commands
	private boolean gnuYankCommands = false;
	private boolean wasYanked = false;
	private boolean yanked = false;
	
	/**
	 * Record manual case sensitivity state; override text input case if not OFF
	 */
	enum Case_Sensitive {
		/** No manual case sensitivity has been set */
		OFF,
		/** Search string should be used in a case sensitive manner */
		SENSITIVE,
		/** Search string should be used in a case insensitive manner */
		INSENSITIVE
	};
	
	// command driven case sensitivity state
	Case_Sensitive toggleCase = Case_Sensitive.OFF;
	
	private static SearchExitOption search_exit_option = SearchExitOption.valueOf((String)SEARCH_EXIT_OPTION.getValue());

	static {
		// listen for changes in the property store
		getPreferenceStore().addPropertyChangeListener(
				new IPropertyChangeListener() {
					public void propertyChange(PropertyChangeEvent event) {
						if (SEARCH_EXIT_OPTION.getPref().equals(event.getProperty())) {
							search_exit_option = SearchExitOption.valueOf((String)event.getNewValue());
						}
					}
				}
		);
	}
		
	/**
	 */
	public SearchMinibuffer() {
		super();
	}

	public SearchMinibuffer(boolean forward, boolean regexp) {
		super();
		setRegexp(regexp);
		setForward(forward);
	}
	
	/**
	 * Get the state of the manual case sensitivity setting.
	 * @see com.mulgasoft.emacsplus.minibuffer.SearchMinibuffer.Case_Sensitive
	 * 
	 * @return the Case_Sensitive setting
	 */
	Case_Sensitive isToggleCase() {
		return toggleCase;
	}

	/**
	 * Set whether to force case insensitive search
	 * 
	 * @param caseState the case state of the search string
	 */
	void toggleCase(boolean caseState) {
		if (toggleCase == Case_Sensitive.OFF) {
			toggleCase = (caseState ? Case_Sensitive.INSENSITIVE : Case_Sensitive.SENSITIVE);
		} else {
			toggleCase = (toggleCase == Case_Sensitive.SENSITIVE ? Case_Sensitive.INSENSITIVE : Case_Sensitive.SENSITIVE);
		}			
		EmacsPlusUtils.showMessage(getEditor(),(toggleCase == Case_Sensitive.SENSITIVE ? CASE_SENSITIVE : CASE_INSENSITIVE), false);
	}

	/**
	 * If user has explicitly chosen a case sensitivity, return that, else rely on super
	 * 
	 * @see com.mulgasoft.emacsplus.minibuffer.SearchMinibuffer#isCaseSensitive()
	 */
	protected boolean isCaseSensitive() {
		boolean result;
		if (toggleCase != Case_Sensitive.OFF) {
			result = toggleCase == Case_Sensitive.SENSITIVE; 
		} else {
			result = caseCaseSensitive();
		}
		return result;
	}

	private boolean caseCaseSensitive() {
		return currentCasePos != -1;
	}
	
	/**
	 * If true, yank from kill buffer onto search string
	 * 
	 * @return the gnuSubCommand state
	 */
	protected boolean isGnuYankCommands() {
		return gnuYankCommands;
	}

	/**
	 * Set whether to yank from kill buffer onto search string
	 * 
	 * @param gnuSubCommands state; if true, then enable
	 */
	protected void setGnuSubCommands(boolean gnuSubCommands) {
		this.gnuYankCommands = gnuSubCommands;
	}
	
	/**
	 * @return the quoting
	 */
	protected boolean isQuoting() {
		return quoting;
	}

	/**
	 * @param quoting the quoting to set
	 */
	protected void setQuoting(boolean quoting) {
		this.quoting = quoting;
	}

	/**
	 * @return the searching
	 */
	protected boolean isSearching() {
		return searching;
	}

	/**
	 * @param searching the searching to set
	 */
	protected void setSearching(boolean searching) {
		this.searching = searching;
	}

	/**
	 * @return the incrFind
	 */
	protected boolean isIncrFind() {
		return incrFind;
	}

	/**
	 * @param incrFind the incrFind to set
	 */
	protected void setIncrFind(boolean incrFind) {
		this.incrFind = incrFind;
	}
	
	protected void setForward(boolean val){
		forward = val;
	}
	
	protected boolean isForward(){
		return forward;
	}

	/**
	 * @return the regexp state
	 */
	protected boolean isRegexp() {
		return regexp;
	}

	/**
	 * @param regexp is true if working with regexps
	 */
	public void setRegexp(boolean regexp) {
		this.regexp = regexp;
	}

	protected void setWrapPosition() {
		if (wrapPosition == WRAP_INDEX) {
			wrapPosition = getSearchStates().size();
		} else {
			wrapPosition = WRAP_INDEX;
		}
	}
	
	protected boolean isWrapped() {
		return wrapPosition != -1;
	}
	
	private void setWasYanked(boolean val) {
		wasYanked = val;
	}
	
	private void setYanked(boolean val) {
		yanked = val;
	}
	
	/**
	 * @see com.mulgasoft.emacsplus.minibuffer.WithMinibuffer#executeResult(org.eclipse.ui.texteditor.ITextEditor, java.lang.Object)
	 */
	protected boolean executeResult(ITextEditor editor, Object commandResult) {
		if (commandResult != null) {
			addToHistory((String)commandResult, getRXString());
		}
		return true;
	}

	public boolean beginSession(ITextEditor editor, IWorkbenchPage page, ExecutionEvent event) {
		try {
			setSearching(true);
			boolean result = super.beginSession(editor, page, event); 
			if (result) {
				setStartOffset(getTextWidget().getCaretOffset());
				setMarkOffset(MarkUtils.getCursorOffset(editor));
				setSearchOffset(getStartOffset());
			}
			return result;
		} finally  {
			setSearching(false);
		}
	}
	
	/**
	 * Perform incremental search on minibuffer string
	 * 
	 * @see com.mulgasoft.emacsplus.minibuffer.WithMinibuffer#addIt(org.eclipse.swt.events.VerifyEvent)
	 */
	protected void addIt(VerifyEvent event) {
		addIt(event,true);
	}

	protected void addIt(VerifyEvent event, boolean searcher) {	
		char c = event.character;
		saveState();
		super.addIt(event);	// add text to minibuffer display
		regAddit(event);	// add text to regexp string
		
		if (searcher) {
			if (addCheckCaseRegexp() && !isCaseSensitive() && Character.isUpperCase(c) && Character.toLowerCase(c) != c) {
				currentCasePos = getMBString().length();
			}
			findNext(getSearchString(),true);
		}
	}
	
	protected void addIt(String addStr) {
		String adder = addStr;
		saveState();
		if (!isCaseSensitive() && addStr != null) {
			adder = addStr.toLowerCase();
		}
		super.addIt(adder);
		regQAddit(adder);
	}
	
	private boolean addCheckCaseRegexp() {
		boolean result = true;
		// ignore quoted characters in regexp 
		if (isRegexp() && getMB().charAt(-2) == '\\' && getMB().charAt(-3) != '\\') {
			result = false;
		}
		return result;
	}

	// for use on whole string replacement
	protected void checkCasePos(String str) {
		currentCasePos = -1;
		if (str != null) {
			if (isRegexp()) {
				MinibufferImpl mb = getMB();
				boolean isQuote = false;
				for (int i = 0; i< mb.getLength(); i++) {
					char c;
					if (isQuote) {
						isQuote = false;						
						continue;
					} else {
						if ((c = mb.charAt(i)) == '\\') {
							isQuote = true;
						} else if (Character.isUpperCase(c) && Character.toLowerCase(c) != c) {
							currentCasePos = i;
						}
					}
				}
			} else if (!str.equals(str.toLowerCase())) {
				currentCasePos = getMBLength();
			}
		}
	}

	protected String normalizeString(String message) {
		return EmacsPlusUtils.normalizeString(message,-1);
	}

	/**
	 * @see com.mulgasoft.emacsplus.minibuffer.WithMinibuffer#getMinibufferPrefix()
	 */
	@Override
	protected String getMinibufferPrefix() {return EMPTY_STR;}

	/**
	 * @see com.mulgasoft.emacsplus.minibuffer.WithMinibuffer#handlesAlt()
	 */
	@Override
	protected boolean handlesAlt() {
		return true;
	}
	
	/**
	 * @see com.mulgasoft.emacsplus.minibuffer.WithMinibuffer#handlesCtrl()
	 */
	@Override
	protected boolean handlesCtrl() {
		return true;
	}

	/**
	 * @see com.mulgasoft.emacsplus.minibuffer.WithMinibuffer#handleKey(org.eclipse.swt.events.VerifyEvent)
	 */
	@Override
	protected void handleKey(VerifyEvent event) {
		boolean isQuoting = quoting;
		try {
			if (isQuoting) {
				event.data = QUOTING;
			}
			if (event.keyCode != SWT.ALT) {
				// ignore naked alt
				setYanked(false);
			}
			setSearching(true);
			super.handleKey(event);
		} finally  {
			if (isQuoting){
				quoting = false;
			}
			setWasYanked(yanked);
			setSearching(false);
		}
	}

	/**
	 * @see com.mulgasoft.emacsplus.minibuffer.WithMinibuffer#noCharEvent(org.eclipse.swt.events.VerifyEvent)
	 */
	protected void noCharEvent(VerifyEvent event) {

		switch (event.keyCode) {
			// remove minimal support for in line editing
			case SWT.HOME:
			case SWT.END:
			case SWT.ARROW_LEFT:
			case SWT.ARROW_RIGHT:
			case SWT.PAGE_DOWN:
			case SWT.PAGE_UP:
				// Since we've disabled the key filter force the action by
				// disabling the key, and calling the command directly
				// since Mac doesn't handle simple resendEvent well
				event.doit = false;
				ITextEditor ed= this.getEditor();
				leave();
				executeBinding(ed, event.stateMask, event);
				break;
			default:
				super.noCharEvent(event);
				break;
		}
	}

	/**
	 * @see com.mulgasoft.emacsplus.minibuffer.WithMinibuffer#charEvent(org.eclipse.swt.events.VerifyEvent)
	 */
	@Override
	protected void charEvent(VerifyEvent event) {
		switch (event.character) {

		case 0x1B: // ESC - leave, but re-send for processing
			super.charEvent(event);
			this.resendEvent(event);
			break;
		default:
			super.charEvent(event);
		}
	}

	protected boolean dispatchCtrl(VerifyEvent event) {
		return dispatchCtrl(event,true);
	}

	protected boolean dispatchCtrl(VerifyEvent event, boolean search) {
		
		boolean result = false;
		//			
		String regExp = null;
		if (quoting) {
			switch (event.character) {
			// ^J == current eol character(s)
			case CR:
			case LF:
				addIt(getEol());
				break;
			default:
				addIt(event);
			}
			if (search) {
				findNext(getSearchString(),true);
			}
			updateStatusLine(getMBString());			
			result = true;
		} else {
			switch (event.keyCode) {
			case EOL:
				addIt(getEol());
				if (search) {
					findNext(getSearchString(),true);
				}
				updateStatusLine(getMBString());			
				result = true;
				break;

			case LINEorYANK: // C-y
				switch(search_exit_option) {
					case t:
						if (isGnuYankCommands()) {
							// yank from kill buffer onto search string
							yankStr(false);
							result = true;
							break;
						} 
						// yank rest of line from buffer onto end of search string & continue
						regExp = LINE_EXP;
						break;
					case nil:
						quoteIt(event);
						break;
					case disable:
						result = super.dispatchCtrl(event);
						break;
				}
			case WORD: // yank word from buffer onto end of search string & continue
				switch(search_exit_option) {
					case t:					
						if (isFound() || getMBLength() == 0) {
							if (regExp == null) {
								regExp = WORD_EXP;
							}
							StyledText w = getTextWidget(); 
							int caretOff = w.getCaretOffset();
							int slen = getMBLength();
							try {
								w.setRedraw(false);
								int catPos = caretOff + (isForward() ? 0 : slen);
								IFindReplaceTarget target = getTarget();
								// always search forward when concatenating
								int fpos = ((IFindReplaceTargetExtension3)target).findAndSelect(catPos, regExp, true, false, false, true);
								if (fpos > -1) {
									addIt(target.getSelectionText());
									setSearchOffset(caretOff + (isForward() ? -slen : getMBLength()));
									findNext(getSearchString(), true);
									updateStatusLine(getMBString());
								}
							} catch (Exception e) {
								popSearchState();
							} finally {
								w.setRedraw(true);
							}
							result = true;
							break;
						}
					case nil:
						quoteIt(event);
						break;
					case disable:
						result = super.dispatchCtrl(event);
						break;
				}
				break;
			case CANCEL:
				if (!cancelSearch()){
					event.doit = false;
				}
				result = true;
				break;
			case CTRL_QUOTE:
				quoting = true;
				result = true;
				break;
			default:
				if (search_exit_option == SearchExitOption.nil ) {
					quoteIt(event);
				} else {
					result = super.dispatchCtrl(event);
				}
			break;
			}
		}
		return result;
	}

	private void quoteIt(VerifyEvent event) {
		setQuoting(true);
		SearchMinibuffer.this.handleKey(event);
	}

	protected boolean dispatchAlt(VerifyEvent event) {
		boolean result = false;
		switch (event.keyCode) {
			case YANK:	// yank (or yank pop) killed text onto end of search string and search for it.
				switch(search_exit_option) {
					case t:
						yankStr(isGnuYankCommands());
						result = true;
						event.doit = false;
						break;
					case nil:
						quoting = true;
						result = true;
						break;
					case disable:
						result = super.dispatchCtrl(event);
						break;
				}				
				break;
			default:
				result = super.dispatchAlt(event);
		}
		return result;
	}

	/**
	 * Yank or Yank Pop onto search string
	 * if yank pop, then remove previous yank
	 * 
	 * @param popit true if popping
	 */
	private void yankStr(boolean popit) {
		String killStr = null;
		if (popit) {
			if (wasYanked) {
				popSearchState();	// remove previous yank
				killStr = KillRing.getInstance().yankPop();
			}
		} else {
			killStr = KillRing.getInstance().yank();
		}
		if (killStr != null) {
			addIt(killStr);
			findNext(getSearchString(),true);
			updateStatusLine(getMBString());
			setYanked(true);
		} else {
			beep();
		}
	}
	
	/**
	 * @see com.mulgasoft.emacsplus.minibuffer.WithMinibuffer#dispatchAltCtrl(org.eclipse.swt.events.VerifyEvent)
	 */
	protected boolean dispatchAltCtrl(VerifyEvent event) {
		return 	dispatchAltCtrl(event,true);
	}
	
	/**
	 * Allow single character operations on an existing search string
	 * 
	 * @param event
	 * @param search - true if we should search after operation
	 * @return true if event handled
	 */
	protected boolean dispatchAltCtrl(VerifyEvent event, boolean search) {
		// allows editing of history/yanked search string
		boolean result = false;
		switch (event.keyCode) {
		case CM_DEL:
			// remove the last character and search
			super.backSpaceChar(event);
			getRX().bsChar();
			if (search) {
				findNext(getSearchString(),true);
			}
			updateStatusLine(getMBString());
			result = true;
			break;
		case CM_ADD:
			StyledText w = getTextWidget(); 
			int catPos = w.getCaretOffset() + (isForward() ? 0 : getMBLength());
			String text = w.getText(catPos, catPos);
			if (w.getLineDelimiter().contains(text)) {
				text = w.getLineDelimiter();
			}
			addIt(text);
			if (search) {
				findNext(getSearchString(),true);
			}
			updateStatusLine(getMBString());
			result = true;
			break;
		}
		if (!result && hasBinding(event)) {
			// exit search and execute the command
			ITextEditor ed = getEditor();					
			leave();
			executeBinding(ed,event);
			result = true;
		}
		return result;
	}
	
	protected boolean handlesTab() {
		return true;
	}
	
	protected boolean dispatchTab(VerifyEvent event) {
		addIt(event);
		return true;
	}
	
	/**
	 * @see com.mulgasoft.emacsplus.minibuffer.WithMinibuffer#leave(boolean)
	 */
	@Override
	protected void leave(boolean closeDialog) {
		leave();
	}

	protected void leave() {
		StyledText text = getTextWidget();
		if (text != null && !text.isDisposed()) {
			leave(text.getCaretOffset());
		} else {
			super.leave(true);
		}
	}
	
	protected void leave(int off) {
		leave(off,0, true);
	}

	protected void leave(int offset, int len, boolean isWidget) {
		addToHistory();
		ISourceViewer viewer = getViewer();
		if (viewer != null) {
			int off = (isWidget ? MarkUtils.widget2ModelOffset(getViewer(), offset) : offset);
			viewer.setSelectedRange(off, len);
			viewer.revealRange(off,0);
		}
		super.leave(true);
	}

	protected boolean cancelSearch() {
		leave();
		return true;
	}

	protected String getSelectionText() {
		String result = null;
		IFindReplaceTarget target = getTarget();
		if (target != null) {
			result = target.getSelectionText();
		}
		return result;
	}
	
	protected Point getSelection() {
		Point result = null;
		IFindReplaceTarget target = getTarget();
		if (target != null) {
			result = target.getSelection();
		}
		return result;
	}

	protected boolean findNext(String searchStr) {
		return findNext(searchStr,false);
	}
	
	private boolean searchEnabled = true;
	
	/**
	 * @return the searchEnabled
	 */
	protected boolean isSearchEnabled() {
		return searchEnabled;
	}

	/**
	 * @param searchEnabled the searchEnabled to set
	 */
	protected void setSearchEnabled(boolean searchEnabled) {
		this.searchEnabled = searchEnabled;
	}

	/*
	 * In order to work around a bug in Eclipse, where it doesn't deal properly with 
	 * the return result from the Matcher in FindReplaceDocumentAdapter, we need to
	 * provide special processing here and in SearchReplace
	 */
	/**
	 * true if the search string consists solely of a regex line delimiter (^|$) 
	 */
	private boolean regexLD = false;
	
	/**
	 * @return the regexLD
	 */
	protected boolean isRegexLD() {
		return regexLD;
	}

	/**
	 * @param regexLD the regexLD to set
	 */
	private void setRegexLD(boolean regLD) {
		this.regexLD = regLD;
	}

	/**
	 * Find the next instance of the search string in the buffer
	 * 
	 * @param searchStr
	 * @param addit - true when just added text to search string
	 * @return true if we found a match
	 */
	protected boolean findNext(String searchStr,boolean addit) {
		if (isSearchEnabled()) {
			setRegexLD(false);
			StyledText text = getTextWidget();
			IFindReplaceTarget target = getTarget();
			if (text != null && target != null) {
				int searchIndex = getSearchOffset();
				if (searchIndex != WRAP_INDEX) {
					Point p = getSelection();
					// if not building the string (or wrapping) search from caret position
					if (!addit) {
						if (isFound() && p != null) {
							searchIndex = p.x;
							if (isForward()) {
								// increment index by (actual or implicit) selection width
								searchIndex += (p.y == 0 ? getEol().length() : p.y); 
							}
						} else {
							// Cannot use target.getSelection since that does not return which side of the
							// selection the caret is on.
							searchIndex = text.getCaretOffset();
						}
					}
					if (!forward && p != null) {
						// if at beginning of line, back up by full eol, else just 1
						searchIndex -= ((p.x == text.getOffsetAtLine(text.getLineAtOffset(p.x))) ? getEol().length() : 1);
					}
				}
				int index = findTarget(target,searchStr,searchIndex, forward);
				boolean justFound = (index != WRAP_INDEX);
				if (isFound() && !justFound && (addit && !isRegexp())) {
					beep();
				} else if (!addit) {
					setSearchOffset(index);
				}
				setFound(justFound);
			}
		}
		return isFound();
	}
	
	protected int findTarget(IFindReplaceTarget target, String searchStr, int searchIndex, boolean forward) {
		int result = WRAP_INDEX;
		StyledText text = getTextWidget();		
		try {
			text.setRedraw(false);
			IFindReplaceTargetExtension3 target3 = (IFindReplaceTargetExtension3) target;
			// index is widget offset (or -1)
			try {
				String searcher = searchStr;
				// syntax check
				if (isRegexp()) {
					Pattern.compile(searchStr);
					// Work around bug in org.eclipse.jface.text.FindReplaceDocumentAdapter 
					// - remember that it is a line delimiter search
					// and use a hacked search string instead
					if (searcher.length() == 1 ) {
						if (REGEX_BOL.equals(searcher)) {
							searcher = REGEX_BOL_HACK;
							setRegexLD(true);
						} else if (REGEX_EOL.equals(searcher)) {
							searcher = REGEX_EOL_HACK;
							setRegexLD(true);
						}
					}
				}
				result = target3.findAndSelect(searchIndex, searcher, forward, isCaseSensitive(), false, isRegexp());
			} catch (Exception e) {
			}
			// Eclipse fails on edge case (with '\s' search string) when searching in reverse direction
			// it decides that it has a bad location in org.eclipse.jface.text.TextViewer.validateSelectionRange(int[])
			// so, while result is ~EOF, the selection has not been moved, so regexp-isearch-backward will get stuck
			// at the beginning of the file instead of looping back over the end 
			Point p = target.getSelection();
			if (!forward) {
				if (result > 0 && (result != p.x && result != (p.x + p.y))) {
					// get the last valid position (note we won't get here if text does not end with delimiter) 
					int last = text.getCharCount() - text.getLineDelimiter().length();
					if (last < result) {
						// validate last position and if it passes use it as the search result
						int check = MarkUtils.widget2ModelOffset(getViewer(), last);
						try {
							getDocument().getLineInformationOfOffset(check);
							result = last;
							p = new Point(result,0);
						} catch (BadLocationException e) {
							// validation failed, just stick with broken behavior 
						}
					}
				}
				text.setSelectionRange(p.x + p.y, -p.y);
			}
			if (isRegexLD()) {
				text.setSelectionRange(p.x, 0);
			}
			text.showSelection();
		} finally {
			text.setRedraw(true);
		}
		return result;
	}
	
	// ISelectionChangedListener
	public void selectionChanged(SelectionChangedEvent event) {
		boolean ignore= false;
		ISelection selection= event.getSelection();
		ITextSelection textSelection = ((selection instanceof ITextSelection) ? (ITextSelection)selection : null);
		if (textSelection != null) {
			Point range= getSelection();
			ignore= textSelection.getOffset() + textSelection.getLength() == range.x + range.y;
		} else if (selection instanceof MarkSelection) {
			// ignore mark selections as this is a side-effect of leaving the search
			ignore = true;
		}
		if (!isSearching() && !ignore) {
			// leave with affecting the selection position
			super.leave(false);
		}
	}
	
	// ITextListener
	/*
	 * @see ITextListener#textChanged(TextEvent)
	 */
	public void textChanged(TextEvent event) {
		if (event.getDocumentEvent() != null) {
//			leave();
		}
	}

	/**
	 * @return the found state
	 */
	protected boolean isFound() {
		return found;
	}

	/**
	 * @param found the new found state
	 */
	protected void setFound(boolean found) {
		this.found = found;
	}

	/**
	 * @return the startOffset
	 */
	protected int getStartOffset() {
		return startOffset;
	}
	
	protected void setStartOffset(int offset) {
		startOffset = offset;
	}
	
	/**
	 * @return the searchOffset
	 */
	protected int getSearchOffset() {
		return searchOffset;
	}

	/**
	 * @param searchOffset the searchOffset to set
	 */
	protected void setSearchOffset(int searchOffset) {
		this.searchOffset = searchOffset;
	}

	/**
	 * @return the markOffset
	 */
	protected int getMarkOffset() {
		return markOffset;
	}

	/**
	 * @param markOffset the markOffset to set
	 */
	protected void setMarkOffset(int markOffset) {
		this.markOffset = markOffset;
	}

	/**
	 * @see com.mulgasoft.emacsplus.minibuffer.WithMinibuffer#initializeBuffer(org.eclipse.ui.texteditor.ITextEditor, org.eclipse.ui.IWorkbenchPage)
	 */
	@Override
	protected boolean initializeBuffer(ITextEditor editor, IWorkbenchPage page) {
		return true;
	}

	/**** Search State Stack ****/
	
	private Stack<SearchState> searchState;
	
	private Stack<SearchState> getSearchStates() {
		if (searchState == null) {
			searchState = new Stack<SearchState>();
		}
		return searchState;
	}
	
	/**
	 * Store the search result.
	 */
	protected void saveState() {
		saveState(false);
	}
	
	/**
	 * Store the search state unless the previous state was from the history
	 * 
	 * @param fromHistory - flag where current state comes from
	 */
	protected void saveState(boolean fromHistory) {
		// delay once if current element is from history ring buffer
		// as we don't search immediately (unlike yank, or ^y & ^w)
		if (!getHistoryState()) {
			getSearchStates().push(new SearchState());
		}
		setHistoryState(fromHistory);
	}
	
	// true if last search string was supplied by history buffer
	private boolean historyState = false;
	private boolean getHistoryState() {
		return historyState;
	}

	private void setHistoryState(boolean state) {
		historyState = state;
	}

	/**
	 * Pop the search stack to previous state
	 */
	protected boolean popSearchState() {
		boolean result = true;
		StyledText text= getTextWidget();
		
		if (text == null || text.isDisposed()) {
			result = false;;
		} else {
			SearchState searchResult= null;
			
			if (!getSearchStates().empty())
				searchResult= getSearchStates().pop();

			if (searchResult == null) {
				result = false;
			} else {
				searchResult.restoreState(text);
				updateStatusLine(getMBString());
			}
		}
		setHistoryState(false);
		return result;
	}

	/**
	 * If not in found state, pop the state stack until we are (or it's empty)
	 * 
	 * @return false if already in found state, else true
	 */
	protected boolean goToFoundState() {
		boolean result = false;
		if (!isFound() && popSearchState()) {
			while (!isFound() && popSearchState()) {
				;
			}
			result = true;
		}
		return result;
	}
	
	/**
	 * Data structure for a search result.
	 */
	private class SearchState {
		String mb, rx;
		int selection, length, index, findLength, regLength;
		boolean sfound, sforward;

		/**
		 * Creates a new search result data object and fills
		 * it with the current values of this target.
		 */
		public SearchState() {
			// WIDGET OFFSET 
			Point p= getTarget().getSelection();
			selection= p.x;
			length= p.y;
			index= getSearchOffset();
			mb = getMBString();
			rx = getRXString();
			findLength= getMBLength();
			regLength = getRXLength();
			sfound= found;
			sforward= forward;
			checkState(this);
		}
		
		/**
		 * Just save one version of the string(s) at the top
		 * 
		 * @param currentState
		 */
		private void checkState(SearchState currentState) {
			SearchState prevState= null;
			if (!getSearchStates().empty())
				prevState= getSearchStates().peek();
			if (prevState != null) {
				if (prevState.mb != null && prevState.mb.equals(currentState.mb)) {
					prevState.mb = null;
				}
				if (prevState.rx != null && prevState.rx.equals(currentState.rx)) {
					prevState.rx = null;
				}
			}
		}

		public void restoreState(StyledText text) {
			// WIDGET OFFSET 
			text.setSelectionRange(selection, length);
			text.showSelection();

			// relies on the contents of the StringBuilder
			if (mb != null) {
				getMB().init(mb);
			}
			if (rx != null) {
				getRX().init(rx);
			}			
			setMBLength(findLength);
			setRXLength(regLength);
			setSearchOffset(index);
			found= sfound;
			forward= sforward;

			// Recalculate the indices
			if (findLength <= currentCasePos)
				currentCasePos= -1;
			
			if (getSearchStates().size() < wrapPosition)
				setWrapPosition();
		}
	}

	
	/**
	 * @see com.mulgasoft.emacsplus.minibuffer.WithMinibuffer#initMinibuffer(java.lang.String)
	 */
	@Override
	protected void initMinibuffer(String newString) {
		super.initMinibuffer(newString);
		if (isRegexp()) {
			getRX().init(newString);
		}
	}

	/* ***************** regexp string buffer *******************/
	private MinibufferImpl regexpStringImpl;
	
	protected MinibufferImpl getRX() {
		if (regexpStringImpl == null){
			regexpStringImpl = new MinibufferImpl(getDocument());		
		}
		return regexpStringImpl;
	}

	protected void regAddit(VerifyEvent event) {
		getRX().addChar(event.character);
	}	

	protected void regQAddit(String addStr) {
		String str = (isRegexp() ? Pattern.quote(addStr) : addStr);
		getRX().append(str);
	}	

	protected String getRXString() {
		return getRX().getString();
	}
	
	protected int getRXLength() {
		return getRX().getLength();
	}
	
	protected void setRXLength(int length) {
		getRX().setLength(length);
	}
	
	protected String getSearchString() {
		String result;
		if (isRegexp()) {
			result = getRXString(); 
		} else {
			result = super.getSearchString();
		}
		return result; 
	}
	
	/**
	 * Add the strings to the ring buffer
	 * 
	 * @param searchStr the search string 
	 * @param regexpStr the regexp version of the string; can be null
	 * @return the ring buffer element
	 */
	protected IRingBufferElement<String> addToHistory(String searchStr, String regexpStr) {
		IRingBufferElement<String> result = null;
		if (searchStr != null && searchStr.length() > 0) {
			result = super.addToHistory(searchStr);
			if (result != null && result instanceof RegexpRingBufferElement) {
				((RegexpRingBufferElement) result).setRegexp(regexpStr);
			}
		}
		return result;
	}
	
	protected IRingBufferElement<String> addToHistory(String historyStr) {
		return addToHistory(historyStr, historyStr);
	}
	
	protected IRingBufferElement<String> addToHistory() {
		return addToHistory(getMBString(), getRXString());		
	}
	
	/**
	 * @see com.mulgasoft.emacsplus.minibuffer.HistoryMinibuffer#replaceFromHistory(com.mulgasoft.emacsplus.RingBuffer.IRingBufferElement)
	 */
	@Override
	protected void replaceFromHistory(IRingBufferElement<?> rbe) {
		saveState(true);		
		super.replaceFromHistory(rbe);
		if (rbe != null && rbe instanceof RegexpRingBufferElement) {
			getRX().init(((RegexpRingBufferElement)rbe).getRegexp());
		}
		checkCasePos(getMBString());
	}

	/**** Local RingBuffers: use lazy initialization holder class idiom ****/

	/**
	 * @see com.mulgasoft.emacsplus.minibuffer.HistoryMinibuffer#getHistoryRing()
	 */
	@Override
	@SuppressWarnings("unchecked")	
	protected RingBuffer<String> getHistoryRing() {
		return (isRegexp() ? RegexpRing.ring : SearchRing.ring);
	}
	
	private static class SearchRing {
		static final RingBuffer<String> ring = new RingBuffer<String>();		
	}
	
	private static class RegexpRing {
		static final RegexpRingBuffer ring = new RegexpRingBuffer();		
	}

}
