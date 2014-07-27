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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
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

	private TreeMap<String, BufRef> bufferMap = null;
	private IEditorReference defaultFile = null;
	private String defaultFilePrefix = null;
	private IWorkbenchPage page = null;

	private String prefix = null;
	private boolean withSelf = false;
	
	private TreeMap<String, BufRef> getBufferMap() {
		return bufferMap;
	}
	
	/**
	 * @param executable
	 */
	public SwitchMinibuffer(IMinibufferExecutable executable) {
		super(executable);
	}

	public SwitchMinibuffer(IMinibufferExecutable executable, boolean withSelf) {
		this(executable);
		this.withSelf = withSelf; 
	}
	
	/**
	 * @see com.mulgasoft.emacsplus.minibuffer.WithMinibuffer#initializeBuffer(org.eclipse.ui.texteditor.ITextEditor, org.eclipse.ui.IWorkbenchPage)
	 */
	@Override
	protected boolean initializeBuffer(ITextEditor editor, IWorkbenchPage page) {
		this.page = page;
		EmacsPlusUtils.clearMessage(editor);
		List<BufRef> refs = setupRefs();
		defaultFile = refs.get(0).getRef();
		defaultFilePrefix = prePre + defaultFile.getName() + prePost;
		setHistoryRing(refs);
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
			BufRef buf = getBufferMap().get(bufferName);			
			result = buf != null ? buf.getRef() : null;
			if (result == null) {
				try {
					// Attempt auto-completion if name fetch failed
					SortedMap<String, BufRef> viewTree = getBuffers(bufferName,false, false);
					if (viewTree.size() == 1) {
						bufferName = viewTree.firstKey();
						result = viewTree.get(bufferName).getRef();
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
	 * Convert the platforms array of editor references in activation order into
	 * structures used by the history and completion mechanisms
	 * 
	 * @return the List of converted references
	 */
	private List<BufRef> setupRefs() {
		List<BufRef> buffers = new ArrayList<BufRef>();
		if (bufferMap == null) {
			IEditorReference[] tmp = EmacsPlusUtils.getSortedEditors(page); 
			this.page = null;
			if (tmp == null || tmp.length <= 1) {
				leave(true);
			} else {
				bufferMap = new TreeMap<String,BufRef>();
				Set<BufRef> checkSet = new HashSet<BufRef>();
				BufRef collider = null;				
				for (int i=(withSelf ? 0 : 1); i< tmp.length; i++) {
					BufRef rr = new BufRef(tmp[i].getName().trim(), tmp[i]);
					if ((collider = bufferMap.get(rr.getName())) != null) {
						checkSet.add(collider);
						rr.setName(fixName(rr.getName(),rr.getRef()));
					}
					bufferMap.put(rr.getName(), rr);
					buffers.add(rr);
				}
				// fix up the colliders
				Iterator<BufRef> it = checkSet.iterator();
				while (it.hasNext()) {
					BufRef rr = it.next();
					bufferMap.remove(rr.getName());
					rr.setName(fixName(rr.getName(),rr.getRef()));
					bufferMap.put(rr.getName(), rr);
				}
			}
		}
		return buffers;
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
	private SortedMap<String,BufRef> getBuffers(String subString, boolean insensitive, boolean regex) {
		SortedMap<String,BufRef> result = null;
		if (subString != null && subString.length() > 0) {
			result = new TreeMap<String, BufRef>();
			Set<String> keySet = getBufferMap().keySet();
			String searchStr = (regex ? subString : toRegex(subString));
			boolean isRegex = (regex || isRegex(searchStr,subString));
			if (insensitive || isRegex) {
				try {
					Pattern pat = Pattern.compile(searchStr + RWILD, (insensitive ? Pattern.CASE_INSENSITIVE : 0));	//$NON-NLS-1$
					// we have to build the map up one by one on regex search
					for (String key : keySet) {
						if (pat.matcher(key).matches()) {
							BufRef c = getBufferMap().get(key);
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
			result = getBufferMap();
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
	private SortedMap<String, BufRef> getSubBuffers(String subString, Set<String> keySet) {
		SortedMap<String, BufRef> result = null;
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
				result = getBufferMap().tailMap(fromKey);
			} else {
				result = getBufferMap().subMap(fromKey, toKey);
			}
		}
		return result;
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

	private class BufRef {
		private IEditorReference ref;
		private String name;
		BufRef(String name, IEditorReference ref) {
			this.name = name;
			this.ref = ref;
		}
		void setName(String name) {
			this.name = name;
		}
		String getName() {
			return name;
		}
		IEditorReference getRef() {
			return ref;
		}
		public String toString() {
			return getName();
		}
	}
	
	/**** Local RingBuffer ****/

	/**
	 * Initialize with the current buffer set
	 * 
	 * @see com.mulgasoft.emacsplus.minibuffer.HistoryMinibuffer#getHistoryRing()
	 */
	private RingBuffer<BufRef> ring = null;
	
	private void setHistoryRing(List<BufRef> refs) {
		ring = new RingBuffer<BufRef>(refs);
		ring.setInfiniteLoop(false);
	}
	
	@Override
	@SuppressWarnings("unchecked")	
	protected RingBuffer<BufRef> getHistoryRing() {
		return ring;
	}
	
}
