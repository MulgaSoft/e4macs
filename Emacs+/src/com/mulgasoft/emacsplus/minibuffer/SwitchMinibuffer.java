/**
 * Copyright (c) 2009, 2014 Mark Feber, MulgaSoft
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
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.regex.Pattern;

import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.texteditor.ITextEditor;

import com.mulgasoft.emacsplus.EmacsPlusUtils;
import com.mulgasoft.emacsplus.RingBuffer;
import com.mulgasoft.emacsplus.execute.BufferDialog;

/**
 * Implement switch-to-buffer minibuffer
 * Support wild cards, regex's, completion and i-search within buffer list
 * 
 * @author Mark Feber - initial API and implementation
 */
public class SwitchMinibuffer extends CompletionMinibuffer {

	private IEditorReference[] sortedRefs;

	private IEditorReference defaultFile = null;
	private String defaultFilePrefix = null;
	private TreeMap<String,IEditorReference> bufferList = null;
	private Map<String,IEditorReference> collisions = new HashMap <String,IEditorReference>();
	private IWorkbenchPage page = null;

	private String prefix = null;

	/**
	 * @param executable
	 */
	public SwitchMinibuffer(IMinibufferExecutable executable) {
		super(executable);
	}

	/**
	 * @see com.mulgasoft.emacsplus.minibuffer.WithMinibuffer#initializeBuffer(org.eclipse.ui.texteditor.ITextEditor, org.eclipse.ui.IWorkbenchPage)
	 */
	@Override
	protected boolean initializeBuffer(ITextEditor editor, IWorkbenchPage page) {
		this.page = page;
		EmacsPlusUtils.clearMessage(editor);
		IEditorReference[] refs = this.getSortedRefs();
		if (refs != null && refs.length > 0) {
			if (refs.length < 2) {
				defaultFile = refs[0];
			} else {
				defaultFile = refs[refs.length - 2];
			}
			defaultFilePrefix = prePre + defaultFile.getName() + prePost;
		}
		return true;
	}

	/**
	 * Populate prefix with default buffer if possible
	 * 
	 * @see com.mulgasoft.emacsplus.minibuffer.WithMinibuffer#getMinibufferPrefix()
	 */
	@Override
	protected String getMinibufferPrefix() {
		if (prefix == null) {
			prefix = super.getMinibufferPrefix();
		}
		String postFix = defaultFilePrefix;
		if (isSearching()) {
			postFix = ' ' + getSearchingPrefix();
		} else if (getMBLength() > 0) {
			postFix = EMPTY_STR;
		}
		return String.format(prefix, postFix);
	}

	protected void resetSearch() {
		setMBString(EMPTY_STR);
		super.resetSearch();
	}
	
	/**
	 * Convert buffer name to editor object before call back to command executable
	 * 
	 * @see com.mulgasoft.emacsplus.minibuffer.WithMinibuffer#executeResult(org.eclipse.ui.texteditor.ITextEditor, java.lang.Object)
	 */
	@Override
	protected boolean executeResult(ITextEditor editor, Object minibufferResult) {
		String bufferName = (String)minibufferResult;
		IEditorReference result = null;
		if (bufferName != null && bufferName.length() > 0) {
			// get the editor reference by name
			result = getBufferList().get(bufferName);
			if (result == null) {
				try {
					// Attempt auto-completion if name fetch failed
					SortedMap<String, IEditorReference> viewTree = getBuffers(bufferName,false, false);
					if (viewTree.size() == 1) {
						bufferName = viewTree.firstKey();
						result = viewTree.get(bufferName);
					}
				} catch (Exception e) {
					// Could be a java.util.regex.PatternSyntaxException on weird input
					// when looking for a match; just ignore and command will abort
				}
			}
		} else if (defaultFile != null) {
			result = defaultFile;
		}
		if (result != null) {
			addToHistory(result.getName());
			setExecuting(true);
			// remember the executable as getEditor(true), if it has to restore the editor,
			// will automatically give it focus, which results in a call to out focusLost....
			IMinibufferExecutable ex = getExecutable();
			super.executeResult(editor, ex, result.getEditor(true));
		} else {
			beep();
		}
		return true;
	}
	
	/**
	 * Enable history commands
	 * 
	 * @see com.mulgasoft.emacsplus.minibuffer.WithMinibuffer#handlesAlt()
	 */
	@Override
	protected boolean handlesAlt() {
		return true;
	}

	protected SortedMap<String,?> getCompletions(String searcher) {
		return getBuffers(RWILD + searcher + RWILD, false, true);
	}
	
	protected SortedMap<String,?> getCompletions() {
		return getCompletions(getSearchStr());
	}
	
	/**
	 * @return the sortedRefs
	 */
	protected IEditorReference[] getSortedRefs() {
		
		if (sortedRefs == null) {
			sortedRefs = EmacsPlusUtils.getSortedEditors(page);
			this.page = null;
			if (sortedRefs == null || sortedRefs.length == 0) {
				leave(true);
			}
		}
		return sortedRefs;
	}

	/**
	 * On first call, convert the editor list to an ordered tree
	 * Exclude the current buffer from the list
	 *  
	 * @return the bufferList
	 */
	protected TreeMap<String, IEditorReference> getBufferList() {
		if (bufferList == null) {
			bufferList = new TreeMap<String,IEditorReference>();
			IEditorReference[] refs = getSortedRefs();
			if (refs != null && refs.length > 1) {
				for (int i= refs.length - 2; i >= 0; i--) {
					bufferList.put(checkName(refs[i]), refs[i]);
				}
			}
			if (!collisions.isEmpty()) {
				for (String key : collisions.keySet()) {
					IEditorReference collision = collisions.get(key);			
					bufferList.remove(key);
					bufferList.put(fixName(key,collision),collision);
				}
				collisions.clear();
			}
		}
		return bufferList;
	}

	private String checkName(IEditorReference ref){
		String result = ref.getName().trim();
		// avoid collisions
		IEditorReference collision; 
		if ((collision = bufferList.get(result)) != null) {
			collisions.put(result, collision);
			result = fixName(result,ref);
		}
		return result;
	}
	
	/**
	 * When there are multiple buffers with the same name, disambiguate 
	 * by adding the title tool tip to the string we're going to use
	 * 
	 * @param name - the name that collides with others
	 * @param ref - the editor reference object
	 * @return a disambiguated name
	 */
	private String fixName(String name, IEditorReference ref) {
		String result = name;
		String tip = ref.getTitleToolTip();
		if (tip != null) {
			String subPart;
			int sublen = tip.length() - (result.length() + 1);
			// some plugins construct inconsiderate (empty) names
			if (sublen < 0) {
				subPart = ref.getTitle();
			} else {
				// remove the buffer name part of the tip
				subPart = tip.substring(0, sublen);
			}
			result = result + '(' + subPart + ')';
		}
		return result;
	}
	
	/**
	 * @see com.mulgasoft.emacsplus.minibuffer.WithMinibuffer#showCompletions()
	 */
	@Override
	protected void showCompletions() {
		String name = getMBString();
		SortedMap<String,?> bufferTree;
		if (isSearching() && getSearchStr().length() > 0) {
			bufferTree = getSearchResults();
		} else {
			bufferTree = getBuffers(name,false,false);			
		}
		if (bufferTree == null) {
			beep();
			leave();
		} else {
			if (bufferTree.size() > 1) {
				if (getMiniDialog() == null) {
					setMiniDialog(new BufferDialog(null, this, getEditor()));
				}
				((BufferDialog)getMiniDialog()).open(bufferTree);
				setShowingCompletions(true);				
			}
			showCompletionStatus(bufferTree,name);
		}
	}

	/**
	 * Compute the set of buffers that match the subString
	 * subString may be null, initial text, or wildcarded (* , ?)
	 * If no match on initial pass, try with case insensitivity
	 *  
	 * @param subString
	 * @return a SortedMap of buffers that match
	 */
	private SortedMap<String,IEditorReference> getBuffers(String subString, boolean insensitive, boolean regex) {
		SortedMap<String,IEditorReference> result = null;
		if (subString != null && subString.length() > 0) {
			result = new TreeMap<String,IEditorReference>();
			Set<String> keySet = getBufferList().keySet();
			String searchStr = (regex ? subString : toRegex(subString));
			boolean isRegex = (regex || isRegex(searchStr,subString));
			if (insensitive || isRegex) {
				try {
					Pattern pat = Pattern.compile(searchStr + RWILD, (insensitive ? Pattern.CASE_INSENSITIVE : 0));	//$NON-NLS-1$
					// we have to build the map up one by one on regex search
					for (String key : keySet) {
						if (pat.matcher(key).matches()) {
							IEditorReference c = getBufferList().get(key);
							result.put(key, c);
						}
					}
				} catch (Exception e) {
					// ignore any PatternSyntaxException
				}
				if (result.size() == 0 && !insensitive) {
					// try non-regex lookup
					result = getSubBuffers(subString, keySet);					
				}
			} else {
				result = getSubBuffers(subString, keySet);
			}
			if ((result == null || result.size() == 0) && !insensitive) {
				// try once with case insensitivity
				return getBuffers(subString, true, regex);
			}
		} else {
			result = getBufferList();
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
	private SortedMap<String, IEditorReference> getSubBuffers(String subString, Set<String> keySet) {
		SortedMap<String, IEditorReference> result = null;
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
				result = getBufferList().tailMap(fromKey);
			} else {
				result = getBufferList().subMap(fromKey, toKey);
			}
		}
		return result;
	}
	
	protected void leave(boolean closeDialog) {
		try {
			super.leave(closeDialog);
		} finally {
			this.bufferList = null;
			this.defaultFile = null;
		}
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

	/**** Local RingBuffer: use lazy initialization holder class idiom ****/

	/**
	 * @see com.mulgasoft.emacsplus.minibuffer.HistoryMinibuffer#getHistoryRing()
	 */
	@Override
	@SuppressWarnings("unchecked")	
	protected RingBuffer<String> getHistoryRing() {
		return SwitchRing.ring;
	}
	
	private static class SwitchRing {
		static final RingBuffer<String> ring = new RingBuffer<String>();		
	}

}
