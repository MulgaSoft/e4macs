/**
 * Copyright (c) 2009, 2010 Mark Feber, MulgaSoft
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package com.mulgasoft.emacsplus.minibuffer;

import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.regex.Pattern;

import com.mulgasoft.emacsplus.EmacsPlusUtils;
import com.mulgasoft.emacsplus.RingBuffer;
import com.mulgasoft.emacsplus.commands.KbdMacroFileHandler;
import com.mulgasoft.emacsplus.execute.KbdMacroSupport;
import com.mulgasoft.emacsplus.execute.SelectionDialog;

/**
 * @author Mark Feber - initial API and implementation
 *
 */
public class KbdMacroMinibuffer extends CompletionMinibuffer {

	KbdMacroFileHandler filehandler = null;
	
	/**
	 * @param executable
	 */
	public KbdMacroMinibuffer(IMinibufferExecutable executable) {
		super(executable);
		if (executable instanceof KbdMacroFileHandler) {
			filehandler = (KbdMacroFileHandler)executable;
		}
	}

	/**
	 * @see com.mulgasoft.emacsplus.minibuffer.ExecutingMinibuffer#getMinibufferPrefix()
	 */
	protected String getMinibufferPrefix() {
		return getCompletionMinibufferPrefix();
	}
	
	/**
	 * @see com.mulgasoft.emacsplus.execute.ISelectExecute#execute(java.lang.Object)
	 */
	public void execute(Object selection) {
		String key = (String)selection;
		setExecuting(true);
		executeResult(getEditor(),key);
		leave(true);
	}
	
	protected void showCompletions() {
		String name = getMBString();
		SortedMap<String, ?> viewTree;
		if (isSearching() && getSearchStr().length() > 0) {
			viewTree = getSearchResults();
		} else {
			viewTree = getCompletions(name); 
		}
		if (viewTree != null) {
			if (viewTree.size() > 1) {
				if (getMiniDialog() == null) {
					setMiniDialog(new SelectionDialog(null, this, getEditor()));
				}
				((SelectionDialog) getMiniDialog()).open(viewTree);
				setShowingCompletions(true);
			}
			EmacsPlusUtils.forceStatusUpdate(getEditor());
			String newName;
			if (viewTree.size() == 0) {
				updateStatusLine((isSearching() ? EMPTY_STR : name) + NOMATCH_MSG);
			} else if (viewTree.size() == 1) {
				closeDialog();
				newName = viewTree.firstKey();
				if (!name.equals(newName) && !isSearching()) {
					initMinibuffer(newName);
				}
				updateStatusLine(newName + COMPLETE_MSG);
			} else if (!isSearching()) {
				if (name.length() > 0) {
					newName = getCommonString(viewTree.keySet(),name);
					if (!name.equals(newName)) {
						initMinibuffer(newName);
					}
				}
				updateStatusLine(getMBString());
			}

		} else {
			updateStatusLine(name + NOMATCH_MSG);
		}
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
	
	protected SortedMap<String, ?> getCompletions() {
		if (filehandler != null) {
			return filehandler.getCompletions();
		} else {
			return KbdMacroSupport.getCompletionList();
		}
	}
	
	/**
	 * @see com.mulgasoft.emacsplus.minibuffer.WithMinibuffer#handlesTab()
	 */
	@Override
	protected boolean handlesTab() {
		// enable tab completion
		return true;
	}

	/**
	 * @see com.mulgasoft.emacsplus.minibuffer.WithMinibuffer#handlesCtrl()
	 */
	@Override
	protected boolean handlesAlt() {
		// no history either
		return false;
	}

	/**
	 * @see com.mulgasoft.emacsplus.minibuffer.HistoryMinibuffer#getHistoryRing()
	 */
	@Override
	protected RingBuffer<Object> getHistoryRing() {
		// No history on kbd macro names
		return null;
	}

}
