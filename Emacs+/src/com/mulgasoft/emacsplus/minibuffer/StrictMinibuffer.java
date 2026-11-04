/**
 * Copyright (c) 2009-2020 Mark Feber, MulgaSoft
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package com.mulgasoft.emacsplus.minibuffer;

import java.util.Collections;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.ui.texteditor.ITextEditor;

import com.mulgasoft.emacsplus.RingBuffer;

/**
 * Read text from the minibuffer, allowing only elements in the list of candidates to be entered.
 * <SPACE> or <TAB> will provoke completion unless the subclass overrides the behavior
 * 
 * @author Mark Feber - initial API and implementation
 */
public abstract class StrictMinibuffer extends CompletionMinibuffer {

	private List<String> candidates = Collections.emptyList();
	// If true, execute immediately if only one completion is found.
	// candidates should be single characters only when immediate
	private boolean immediate = false;
	private boolean caseSensitive = false;

	/**
	 * @param executable
	 */
	public StrictMinibuffer(IMinibufferExecutable executable) {
		super(executable);
	}

	public StrictMinibuffer(IMinibufferExecutable executable, List<String> candidates) {
		this(executable);
		setCandidates(candidates);
	}

	public StrictMinibuffer(IMinibufferExecutable executable, List<String> candidates, boolean immediate) {
		this(executable, candidates);
		setImmediate(immediate);
	}

	protected void setImmediate(boolean immediate) {
		this.immediate = immediate;
	}

	protected boolean isImmediate() {
		return immediate;
	}

	protected void setCandidates(List<String> candidates) {
		this.candidates = candidates;
	}

	protected boolean canBeCandidate(String possible) {
		return matchCandidate(possible, true) != null;
	}

	protected String matchCandidate(String possible, boolean allowMultiples) {
		String result = null;
		// weirdly, startsWith returns true on the empty string  
		if (possible != null && possible.length() > 0) {
			String can = (caseSensitive ? possible : possible.toLowerCase()); 
			for (String c : candidates) {
				int count = 0;
				if (c.startsWith(can)) {
					if (++count > 1 && !allowMultiples) {
						result = null;
						break;
					}
					result = c;
				}
			}
		}
		return result;
	}

	public SortedMap<String, String> getCompletions() {
		SortedMap<String, String> result = new TreeMap<String, String>();
		for (String s : candidates) {
			result.put(s, s);
		}
		return result;
	}

	/**
	 * @see com.mulgasoft.emacsplus.execute.ISelectExecute#execute(java.lang.Object)
	 */
	public void execute(Object selection) {
		String key = (String) selection;
		setExecuting(true);
		executeResult(getEditor(), key);
		leave(true);
	}

	/**
	 * {@inheritDoc}
	 * This will return the first match, if the minibuffer exited with a substring 	 
	 */
	@Override
	protected boolean executeResult(ITextEditor editor, Object commandResult) {
		Object result = commandResult;
		// A subclass might have already converted the minibuffer result to some relevant non-String object
		// that will be passed back to the handler
		if (commandResult instanceof String) {
			result = matchCandidate((String)commandResult, false);
		}
		return super.executeResult(editor, result);
	}

	/**
	 * Read a string that must be one of the candidate strings
	 * 
	 * @see com.mulgasoft.emacsplus.minibuffer.WithMinibuffer#charEvent(org.eclipse.swt.events.VerifyEvent)
	 */
	protected void charEvent(VerifyEvent event) {
		event.doit = false;
		switch (event.character) {
			case 0x0D: // CR - execute command (if complete) \r
			case 0x1B: // ESC - another way to leave
			case 0x08: // BS
			case 0x7F: // DEL
				super.charEvent(event);
				break;
			case ' ': //
				handleCompleting(event);
				break;
			case SWT.TAB:
				if (handlesTab()) {
					handleCompleting(event);
					break;
				}
			default:
				if (checkControl(event)) {
					if (dispatchCtrl(event)) {
						event.doit = false;
						break;
					}
				} else if (checkCharacter(event)) {
					super.charEvent(event);
					if (isImmediate()) {
						executeCR(event);
					}
					break;
				} else {
					beep();
				}
		}
	}
	
	private boolean checkCharacter(VerifyEvent event) {
		boolean result = (Character.isLetter(event.character) && 
				((event.stateMask & SWT.MODIFIER_MASK) == 0 || (event.stateMask ^ SWT.MOD2) == 0) &&
				(canBeCandidate(getMBString() + event.character)));		
		return result;
	}

	protected boolean dispatchCtrl(VerifyEvent event) {
		boolean result = checkExit(event) || checkEdit(event);
		if (!result) {
			switch (event.keyCode) {
			case 'g':
				leave();
				result = true;
				break;
			default:
				beep();
				break;
			}
		}
		return result;
	}

	protected void handleCompleting(VerifyEvent event) {
		super.charEvent(event);
		if (isImmediate() && candidates.contains(getMBString())) {
			executeCR(event);
		}
	}

	/**
	 * @see com.mulgasoft.emacsplus.minibuffer.WithMinibuffer#handlesAlt()
	 */
	@Override
	protected boolean handlesAlt() {
		// disable history for strict commands
		return false;
	}

	/**
	 * @see com.mulgasoft.emacsplus.minibuffer.HistoryMinibuffer#getHistoryRing()
	 */
	@Override
	protected <T> RingBuffer<T> getHistoryRing() {
		return null;
	}

}
