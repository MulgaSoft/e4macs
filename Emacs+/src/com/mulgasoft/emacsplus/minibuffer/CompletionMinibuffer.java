/**
 * Copyright (c) 2009, 2014 Mark Feber, MulgaSoft
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package com.mulgasoft.emacsplus.minibuffer;

import java.util.Iterator;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.regex.Pattern;

import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.TraverseEvent;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.ui.IPartListener;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.texteditor.ITextEditor;

import com.mulgasoft.emacsplus.EmacsPlusActivator;
import com.mulgasoft.emacsplus.EmacsPlusUtils;
import com.mulgasoft.emacsplus.RingBuffer.IRingBufferElement;
import com.mulgasoft.emacsplus.execute.SelectionDialog;
import com.mulgasoft.emacsplus.execute.ISelectExecute;

	// TODO support y-p -> yank-pop completion
/**
 * Completion base class
 * 
 * @author Mark Feber - initial API and implementation
 */
public abstract class CompletionMinibuffer extends ExecutingMinibuffer implements IPartListener, ISelectExecute {

	static final String SEARCH_PREFIX = EmacsPlusActivator.getResourceString("IS_Forward");	//$NON-NLS-1$  
	
	protected abstract SortedMap<String,?> getCompletions();
	
	final static String RWILD = ".*";	//$NON-NLS-1$ 
	final static String prePre = " (";	//$NON-NLS-1$
	final static String prePost = ")";	//$NON-NLS-1$

	// Flag whether we are searching 
	private boolean isSearching = false;
	// Hold the text of the search string entered by the user
	private StringBuilder searchStr = null;
	// Hold the of array of (ordered) buffers that match the search string 
	private String[] searchArray = null;
	// Index into searchArray
	private int searchIndex = -1;
	// Direction of search
	private int searchDir = 1;
	// Remember last successful search string
	static String lastSearch = null;
	// Hold the full search results 
	private SortedMap<String,?> searchResults = null;
	// Remember if we were showing completions during search
	private boolean showingCompletions = false;
	
	private SelectionDialog miniDialog = null;
	
	/**
	 * @param executable
	 */
	public CompletionMinibuffer(IMinibufferExecutable executable) {
		super(executable);
	}

	/**
	 * @return the miniDialog
	 */
	protected SelectionDialog getMiniDialog() {
		return miniDialog;
	}
	
	/**
	 * @param miniDialog the miniDialog to set
	 */
	protected void setMiniDialog(SelectionDialog miniDialog) {
		this.miniDialog = miniDialog;
	}
	
	protected void closeDialog() {
		showingCompletions = false;
		try {
			if (miniDialog != null) {
//				miniDialog.shutdown();
				miniDialog.close();
				miniDialog = null;
			}
		} catch (Exception e) {}
	}
	
	private boolean mouseInDialog() {
		boolean result = false;
		if (miniDialog != null) {
			result = miniDialog.mouseIn();
		}
		return result;
	}
	
	protected boolean isCompleting() {
		return true;
	}
	
	boolean isSearching() {
		return isSearching;
	}
	
	String getSearchStr() {
		return (searchStr == null ? EMPTY_STR : searchStr.toString());
	}
	
	void setShowingCompletions(boolean value) {
		showingCompletions = value;
	}

	boolean isShowingCompletions() {
		return showingCompletions;
	}
	
	SortedMap<String,?> getSearchResults() {
		return searchResults;
	}
	
	String getSearchResult() {
		String result = null;
		if (searchIndex > -1) {
			result = searchArray[searchIndex];
		}
		return result;
	}
	
	protected String getCompletionMinibufferPrefix() {
		String result = super.getMinibufferPrefix(); 
		if (isSearching()) {
			result = getSearchingPrefix() + ' ' + result;
		}
		return result;
	}
	
	protected String getSearchingPrefix() {
		String str = getSearchStr();
		return '(' + SEARCH_PREFIX + ((str != null && str.length() > 0) ? ": " + str  : EMPTY_STR) + ')';
	}
	
	/**
	 * @see com.mulgasoft.emacsplus.minibuffer.WithMinibuffer#showCompletions()
	 */
	@Override
	protected void showCompletions() {
		String name = getMBString();
		SortedMap<String, ?> compTree;
		if (isSearching() && getSearchStr().length() > 0) {
			compTree = getSearchResults();
		} else {
			compTree = getCompletions(name); 
		}
		if (compTree != null) {
			if (compTree.size() > 1) {
				if (getMiniDialog() == null) {
					setMiniDialog(new SelectionDialog(null, this, getEditor()));
				}
				((SelectionDialog) getMiniDialog()).open(compTree);
				setShowingCompletions(true);
			}
			EmacsPlusUtils.forceStatusUpdate(getEditor());
			showCompletionStatus(compTree,name);
		} else {
			updateStatusLine(name + NOMATCH_MSG);
		}
	}
	
	/**
	 * Update the status line/minibuffer text based on the content of the completion tree
	 * 
	 * @param compTree the Map of the available completions
	 * @param name the contents of the minibuffer so far
	 */
	protected void showCompletionStatus(SortedMap<String, ?> compTree, String name) {
		String newName;
		if (compTree.size() == 0) {
			updateStatusLine((isSearching() ? EMPTY_STR : name) + NOMATCH_MSG);
		} else if (compTree.size() == 1) {
			closeDialog();
			newName = compTree.firstKey();
			if (!name.equals(newName) && !isSearching()) {
				initMinibuffer(newName);
			}
			updateStatusLine(newName + COMPLETE_MSG);
		} else if (!isSearching()) {
			if (name.length() > 0) {
				newName = getCommonString(compTree.keySet(),name);
				if (!name.equals(newName)) {
					initMinibuffer(newName);
				}
			}
			updateStatusLine(getMBString());
		}
	}
	
	protected SortedMap<String, ?> getCompletions(String searchSubstr) {
		return getCompletions(searchSubstr, false, false);
	}

	protected SortedMap<String, ?> getCompletions(String searchSubstr, boolean insensitive, boolean regex) {
		SortedMap<String, ?> result = getCompletions();
		if (searchSubstr != null && result != null) {
			Set<String> keySet = result.keySet();
			String searchStr = (regex ? searchSubstr : toRegex(searchSubstr));
			boolean isRegex = (regex || isRegex(searchStr,searchSubstr));
			if (insensitive || isRegex) {
				try {
					SortedMap<String,? super Object> regResult = new TreeMap<String, Object>();
					Pattern pat = Pattern.compile(searchStr + RWILD, (insensitive ? Pattern.CASE_INSENSITIVE : 0));	//$NON-NLS-1$
					// we have to build the map up one by one on regex search
					for (String key : keySet) {
						if (pat.matcher(key).matches()) {
							regResult.put(key, result.get(key));
						}
					}
					result = regResult; 					
				} catch (Exception e) {
					// ignore any PatternSyntaxException
				}
				if (result.size() == 0 && !insensitive) {
					// try non-regex lookup
					result = getCompletions(searchSubstr, keySet);					
				} 
			} else {
				result = getCompletions(searchSubstr, keySet);
			}
			if ((result == null || result.size() == 0) && !insensitive) {
				// try once with case insensitivity
				return getCompletions(searchSubstr, true, regex);
			}
		}
		return result;
	}
	
	/**
	 * Walk the buffer list looking for matches with subString on initial characters
	 * 
	 * @param subString
	 * @param result
	 * @param keySet
	 * @return subsection of map, each of whose entries start with subString
	 */
	private SortedMap<String, ?> getCompletions(String subString, Set<String> keySet) {
		SortedMap<String, ?> result = null;
		String fromKey = null;
		String toKey = null;
		for (String key : keySet) {
			if (key.startsWith(subString)) {
				if (fromKey == null) {
					fromKey = key;
				}
			} else if (fromKey != null) {
				toKey = key;
				break;
			}
		}
		// too bad we can't use 1.6
		if (fromKey != null) {
			if (toKey == null) {
				result = getCompletions().tailMap(fromKey);
			} else {
				result = getCompletions().subMap(fromKey, toKey);
			}
		}
		return result;
	}
	
	/**
	 * Determine the longest common command name substring that starts with the current substring
	 *  
	 * @param keySet
	 * @param subString
	 * @return the longest common name
	 */
	protected String getCommonString(Set<String> keySet, String subString) {
		String result = (isWildCarded(subString) ? EMPTY_STR : subString);
		Iterator<String> it = keySet.iterator();

		String key;
		String possible;

		key = it.next();
		do {
			if (key.length() > result.length()) {
				// advance possible by on character
				possible = key.substring(0, result.length()+1);
				// check the rest to see if they all have the new possible
				while (it.hasNext()) {
					key = it.next();
					if (!key.startsWith(possible)) {
						return result;
					}
				}
				result = possible;
				// reset the iterator and re-initialize key
				it = keySet.iterator();
				key = it.next();
			}
		} while (result.length() < key.length());

		return result;
	}

	protected void executeCR(VerifyEvent event) {
		if (isSearching()){
			String key = getSearchResult();
			if (key != null) {
				// save search characters
				lastSearch = searchStr.toString(); 
				resetSearch();
				// and put full key into buffer for execution 
				setMBString(key);
			} else {
				// Mimic Emacs by restoring to initial state, but not leaving
				setMBString(EMPTY_STR);
				event.doit = false;
				resetSearch();
				return;
			}
		}
		super.executeCR(event);
	}

	/**
	 * Turn off searching before invoking history
	 * 
	 * @see com.mulgasoft.emacsplus.minibuffer.HistoryMinibuffer#replaceFromHistory(com.mulgasoft.emacsplus.RingBuffer.IRingBufferElement)
	 */
	protected void replaceFromHistory(IRingBufferElement<?> rbe) {
		if (rbe != null && isSearching()) {
			resetSearch();
		}
		super.replaceFromHistory(rbe);
	}
	
	/**
	 * @see com.mulgasoft.emacsplus.minibuffer.WithMinibuffer#handlesTab()
	 */
	@Override
	protected boolean handlesTab() {
		return true;
	}

	/**
	 * @see com.mulgasoft.emacsplus.minibuffer.WithMinibuffer#dispatchTab(org.eclipse.swt.events.VerifyEvent)
	 */
	protected boolean dispatchTab(VerifyEvent event) {
		// handle tab after disabling tab traversal 
		showCompletions();
		return false;
	}

	/**
	 * @see com.mulgasoft.emacsplus.minibuffer.WithMinibuffer#handlesCtrl()
	 */
	@Override
	protected boolean handlesCtrl() {
		return true;
	}

	/**
	 * Support ^s and ^r for searching within buffer list and ^g to break out of search
	 * 
	 * @see com.mulgasoft.emacsplus.minibuffer.WithMinibuffer#dispatchCtrl(org.eclipse.swt.events.VerifyEvent)
	 */
	@Override
	protected boolean dispatchCtrl(VerifyEvent event) {
		boolean result = false;

		switch (event.keyCode) {
			case 's':
				forwardSearch();
				break;
			case 'r':
				backwardSearch();
				break;
			case 'k':
				// revert to initial state
				resetSearch();
				break;
			case 'g':
				if (isSearching) {
					resetSearch();
					break;
				}
				// otherwise, ^g interrupts
			default:
				leave();
				result = true;
		}
		return result;
	}
	
	/**
	 * Turn off searching and reset state
	 */
	protected void resetSearch() {
		isSearching = false;
		searchStr = null;
		searchArray = null;
		searchResults = null;
		searchIndex = -1;
		if (showingCompletions) {
			closeDialog();
		}
		updateStatusLine(getMBString());
	}
	
	private void forwardSearch() {
		searchDir = 1;
		search();
	}
	private void backwardSearch() {
		searchDir = -1;
		search();
	}
	
	/**
	 * Turn on searching, or continue through array if already searching
	 */
	private void search() {
		if (!isSearching) {
			isSearching = true;
			searchStr = new StringBuilder();
			// add any text so far
			if (getMB().getLength() > 0) {
				searchStr.append(getMBString());
			}
			updateSearch();
		} else if (searchArray != null && searchArray.length > 0) {
			updateStatusLine(searchArray[getNextIndex()]);				
		} else if (lastSearch != null) {
			searchStr = new StringBuilder();
			searchStr.append(lastSearch);
			setMBString(lastSearch);
			updateSearch();
		}
	}
	
	private int getNextIndex() {
		searchIndex += searchDir;
		if (searchIndex >= searchArray.length) {
			searchIndex = 0;
		} else if (searchIndex < 0) {
			searchIndex = searchArray.length -1;
		}
		return searchIndex;
	}
	
	/**
	 * Recompute the search results
	 */
	private void updateSearch() {
		searchIndex = -1;
		searchArray = null;
		searchResults = null;
		if (searchStr.length() > 0) {
			searchResults = getCompletions(searchStr.toString());
			if (searchResults != null && !searchResults.isEmpty()) {
				searchArray = new String[searchResults.size()];
				searchResults.keySet().toArray(searchArray);
				updateStatusLine(searchArray[getNextIndex()]);
				if (showingCompletions) {
					closeDialog();
					showCompletions();
				}
			} else {
				updateStatusLine(EMPTY_STR);
				beep();
			}
		} else {
			updateStatusLine(EMPTY_STR);
		}
	}
	
	/**
	 * When searching, add a character to the search string
	 * 
	 * @see com.mulgasoft.emacsplus.minibuffer.WithMinibuffer#addIt(org.eclipse.swt.events.VerifyEvent)
	 */
	protected void addIt(VerifyEvent event) {
		if (isSearching) {
			searchStr.append(event.character);
			updateSearch();
			event.doit = false;
		} else {
			super.addIt(event);
		}
	}
	
	/**
	 * When searching, remove a character from the search string
	 * 
	 * @see com.mulgasoft.emacsplus.minibuffer.WithMinibuffer#backSpaceChar(org.eclipse.swt.events.VerifyEvent)
	 */
	protected void backSpaceChar(VerifyEvent event) {
		if (isSearching) {
			int index = searchStr.length();
			if (index > 0) {
				searchStr.deleteCharAt(index-1);
				updateSearch();
			} else {
				beep();
			}
			event.doit = false;
		} else {
			super.backSpaceChar(event);
		}
	}

	// Regex/wildcard support
	
	/**
	 * Does the command string contain wild cards?
	 * 
	 * @param searchStr
	 * @return true if wildcards present
	 */
	protected boolean isWildCarded(String searchStr){
		return (searchStr.matches(".*[\\?|\\*].*"));	//$NON-NLS-1$
	}

	protected boolean isRegex(String searchStr, String subStr) {
		boolean result = false;
		if (!searchStr.equals(subStr)) {
			result =  true;
		} else 
			for (char c : searchStr.toCharArray()) {
				// Always treat . as a path separator
				if (c != '.' && !Character.isJavaIdentifierPart(c)) {
					result = true;
					break;
				}
		}
		return result;
	}
	
	/**
	 * Convert command string to simple regexp
	 * 
	 * @param searchStr
	 * @return regexp string
	 */
	protected String toRegex(String searchStr){
		String result = searchStr; 
		if (searchStr != null && isWildCarded(searchStr)) {
			result = searchStr.replaceAll("([^.]|^)\\*","$1.*"); //$NON-NLS-1$ //$NON-NLS-2$
			result = result.replace('?', '.');
		}
		return result;
	}

	// Listener support
	
	/**
	 * @see com.mulgasoft.emacsplus.minibuffer.HistoryMinibuffer#addOtherListeners(IWorkbenchPage, ISourceViewer, StyledText)
	 */
	@Override
	protected void addOtherListeners(IWorkbenchPage page, ISourceViewer viewer, StyledText widget) {
		if (page != null) {
			page.addPartListener(this);
		}
		super.addOtherListeners(page, viewer, widget);
	}	
	
	/**
	 * @see com.mulgasoft.emacsplus.minibuffer.HistoryMinibuffer#removeOtherListeners(IWorkbenchPage, ISourceViewer, StyledText)
	 */
	@Override
	protected void removeOtherListeners(IWorkbenchPage page, ISourceViewer viewer, StyledText widget) {
		if (page != null) {
			page.removePartListener(this);
		}
		super.removeOtherListeners(page, viewer, widget);
	}
	
	// ISelectionChangedListener
	public void selectionChanged(SelectionChangedEvent event) {
		leave();
	}
	
	/**
	 * @see FocusListener#focusGained(org.eclipse.swt.events.FocusEvent)
	 */
	@Override
	public void focusGained(FocusEvent e) {
		if (!isExecuting()) {
			leave();
		}
	}
	/**
	 * @see FocusListener#focusLost(org.eclipse.swt.events.FocusEvent)
	 */
	public void focusLost(FocusEvent e) {
		// check if focus was lost to anything but the completions dialog
		if (!isShowingCompletions() || !mouseInDialog()) {
			leave(true);
		}
	}
	
	// TraverseListener - enable tab completion
	
	/**
	 * Enable tab completion by disabling tab traversal
	 * 
	 * @see com.mulgasoft.emacsplus.minibuffer.HistoryMinibuffer#keyTraversed(org.eclipse.swt.events.TraverseEvent)
	 */
	public void keyTraversed(TraverseEvent e) {
		switch (e.detail) {
		case SWT.TRAVERSE_TAB_NEXT:
			// ignore tab traversal
			e.doit = false;
			break;
		default:
			super.keyTraversed(e);
		}
	}
	
	// Part activation handles the case where we want the dialog to stay open for 
	// double click (which gives it focus), but it should be removed if focus goes
	// to any other part (which we can detect on part deactivation)
	
	/**
	 * @see org.eclipse.ui.IPartListener#partActivated(org.eclipse.ui.IWorkbenchPart)
	 */
	public void partActivated(IWorkbenchPart part) {
	}
	/**
	 * @see org.eclipse.ui.IPartListener#partBroughtToTop(org.eclipse.ui.IWorkbenchPart)
	 */
	public void partBroughtToTop(IWorkbenchPart part) {
	}
	/**
	 * If we are the part deactivated, then time to go
	 * 
	 * @param part
	 */
	private void checkDeactivation(IWorkbenchPart part) {
		// getAdapter gets the correct part when dealing with multi-page, otherwise identity
		if ((IWorkbenchPart) part.getAdapter(ITextEditor.class) == getEditor() || (IWorkbenchPart)getEditor() == part){
			leave(true);
		}
	}
	
	/**
	 * @see org.eclipse.ui.IPartListener#partClosed(org.eclipse.ui.IWorkbenchPart)
	 */
	public void partClosed(IWorkbenchPart part) {
	}
	
	/**
	 * @see org.eclipse.ui.IPartListener#partDeactivated(org.eclipse.ui.IWorkbenchPart)
	 */
	public void partDeactivated(IWorkbenchPart part) {
		checkDeactivation(part);
	}
	/**
	 * @see org.eclipse.ui.IPartListener#partOpened(org.eclipse.ui.IWorkbenchPart)
	 */
	public void partOpened(IWorkbenchPart part) {
	}
	
}
