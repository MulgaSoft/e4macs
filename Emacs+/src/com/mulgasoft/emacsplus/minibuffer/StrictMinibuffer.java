/**
 * Copyright (c) 2009-2011 Mark Feber, MulgaSoft
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

import com.mulgasoft.emacsplus.RingBuffer;

/**
 * Read text from the minibuffer, allowing only elements in the list of candidates to be entered.
 * <SPACE> or <TAB> will provoke completion unless the subclass overrides the behavior
 * 
 * @author Mark Feber - initial API and implementation
 */
public abstract class StrictMinibuffer extends CompletionMinibuffer {

	private List<String> candidates = Collections.emptyList();
	// If true, execute immediately if only one completion is found
	private boolean immediate = false;

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

	void setImmediate(boolean immediate) {
		this.immediate = immediate;
	}

	boolean isImmediate() {
		return immediate;
	}

	protected void setCandidates(List<String> candidates) {
		this.candidates = candidates;
	}

	protected boolean canBeCandidate(String possible) {
		return matchCandidate(possible) != null;
	}

	protected String matchCandidate(String possible) {
		String result = null;
		for (String c : candidates) {
			if (c.startsWith(possible)) {
				result = c;
				break;
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
				} else if (Character.isLetter(event.character) && ((event.stateMask & SWT.MODIFIER_MASK) == 0)
						&& (canBeCandidate(getMBString() + event.character))) {
					super.charEvent(event);
				} else {
					beep();
				}
		}
	}

	protected boolean dispatchCtrl(VerifyEvent event) {
		boolean result = false;

		switch (event.keyCode) {
			case 'g':
				leave();
				result = true;
				break;
			default:
				beep();
				break;
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
	 * @see com.mulgasoft.emacsplus.minibuffer.HistoryMinibuffer#getHistoryRing()
	 */
	@Override
	protected <T> RingBuffer<T> getHistoryRing() {
		return null;
	}

	/**
	 * @see com.mulgasoft.emacsplus.minibuffer.WithMinibuffer#handlesCtrl()
	 */
	@Override
	protected boolean handlesCtrl() {
		return true;
	}

	/**
	 * @see com.mulgasoft.emacsplus.minibuffer.WithMinibuffer#handlesCtrl()
	 */
	@Override
	protected boolean handlesAlt() {
		// disable history for strict commands
		return false;
	}

}
