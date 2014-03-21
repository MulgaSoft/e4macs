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

import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.TraverseEvent;
import org.eclipse.swt.events.TraverseListener;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.widgets.Event;
import org.eclipse.ui.IWorkbenchPage;

import com.mulgasoft.emacsplus.RingBuffer;
import com.mulgasoft.emacsplus.YankRotate;
import com.mulgasoft.emacsplus.RingBuffer.IRingBufferElement;

/**
 * Support a minibuffer with RingBuffer based history list
 * 
 * @author Mark Feber - initial API and implementation
 */
public abstract class HistoryMinibuffer extends WithMinibuffer implements TraverseListener {

	// TODO preferences?
	static final int NEXT = 'n';
	static final int PREV = 'p';
	static final int NEXT_ARROW = SWT.ARROW_DOWN;
	static final int PREV_ARROW = SWT.ARROW_UP;

	// For issues with non-generic method overriding a generic one see:
	// http://www.angelikalanger.com/GenericsFAQ/FAQSections/TechnicalDetails.html#FAQ823
	/**
	 * Get the history ring buffer for this minibuffer
	 * @return RingBuffer instance
	 */
	protected abstract <T> RingBuffer<T> getHistoryRing();
	
	/**
	 * @see com.mulgasoft.emacsplus.minibuffer.WithMinibuffer#handlesAlt()
	 */
	@Override
	protected boolean handlesAlt() {
		return true;
	}

	/**
	 * Handle the history navigation commands (via arrow keys) 
	 * 
	 * @see com.mulgasoft.emacsplus.minibuffer.WithMinibuffer#noCharEvent(org.eclipse.swt.events.VerifyEvent)
	 */
	protected void noCharEvent(VerifyEvent event) {
		switch (event.keyCode) {
			case NEXT_ARROW:
				historyChange(event,YankRotate.BACKWARD);
				event.doit = false;
				break;
			case PREV_ARROW:
				historyChange(event,YankRotate.FORWARD);
				event.doit = false;
				break;
			default:
				super.noCharEvent(event);
				break;
		}
	}
	
	/**
	 * Handle the history navigation commands (via Meta) 
	 * 
	 * @see com.mulgasoft.emacsplus.minibuffer.WithMinibuffer#dispatchAlt(org.eclipse.swt.events.VerifyEvent)
	 */
	protected boolean dispatchAlt(VerifyEvent event) {
		boolean result = false;
		switch (event.keyCode) {
			case NEXT: 	// search for the next item in the search ring.
				historyChange(event,YankRotate.BACKWARD);
				result = true;
				break;
			case PREV: 	// search for the previous item in the search ring.
				historyChange(event,YankRotate.FORWARD);
				result = true;
				break;
			// TODO - window captures Alt-SPACE first
			/*	
			case ' ': // space completion
			if (isCompleting()) {
				showCompletions();
				result = true;
				break;
			}
			// TODO - window captures Alt-TAB first
			case '\t':	// complete the search string using the search ring. 
   				System.out.println("Alt-TAB: complete the search string using the search ring.");
			    break;
			 */			
			default:
				result = super.dispatchAlt(event);
		}
		return result;
	}
	
	/**
	 * Update the minibuffer string with the history entry
	 * 
	 * @param event the event provoking the history change
	 * @param dir the direction in which to rotate the history ring
	 */
	private void historyChange(VerifyEvent event, YankRotate dir) {
		// don't start rotating through history unless the preceding key was a history command 
		replaceFromHistory(wasHistKey(event) ? getHistoryRing().rotateYankPos(dir) : getHistoryRing().yankElement());
		historyTransition(event.keyCode);
	}
	
	/**
	 * Initialize the minibuffer with the string from the yanked RingBuffer element
	 */
	protected void replaceFromHistory() {
		replaceFromHistory(getHistoryRing().yankElement());
	}
	
	/**
	 * Initialize the minibuffer with the string from the RingBuffer element
	 *  
	 * @param rbe
	 */
	protected void replaceFromHistory(IRingBufferElement<?> rbe) {
		if (rbe != null) {
			String historyStr = rbe.toString();
			if (historyStr != null) {
				initMinibuffer(historyStr);
			}
		}
	}
	
	/**
	 * Hook method for subclasses to add behavior when the entry has changed via a history key
	 * Default does nothing.
	 *  
	 * @param keyCode the key that provoked the transition
	 */
	protected void historyTransition (int keyCode) {
		// default do nothing
	}

	private boolean wasHistKey(VerifyEvent event) {
		int last = getLastKeyCode(); 
		return (last == NEXT || last == PREV || last == NEXT_ARROW || last == PREV_ARROW);
	}
	
	protected <T> IRingBufferElement<T> addToHistory(T history) {
		IRingBufferElement<T> result = null;
		if (history != null && history.toString().length() > 0) {
			RingBuffer<T> hist = getHistoryRing();
			// check against (possible) rotate position and bottom and top position
			if (hist != null && (hist.isEmpty() || !hist.isDuplicate(history))) {
				result = hist.putNext(history);
			}
		}
		return result;
	}

	/**
	 * @see com.mulgasoft.emacsplus.minibuffer.WithMinibuffer#addOtherListeners(IWorkbenchPage, ISourceViewer, StyledText)
	 */
	@Override
	protected void addOtherListeners(IWorkbenchPage page, ISourceViewer viewer, StyledText widget) {
		if (handlesAlt()) {
			widget.addTraverseListener(this);
		}
	}

	/**
	 * @see com.mulgasoft.emacsplus.minibuffer.WithMinibuffer#removeOtherListeners(IWorkbenchPage, ISourceViewer, StyledText)
	 */
	@Override
	protected void removeOtherListeners(IWorkbenchPage page, ISourceViewer viewer, StyledText widget) {
		if (handlesAlt()) {
			widget.removeTraverseListener(this);
		}
	}

	// TraverseListener
	// Disable mnemonic traversal for M-p & M-n
	public void keyTraversed(TraverseEvent e) {
		switch (e.detail) {
			case SWT.TRAVERSE_MNEMONIC:
				if ((e.stateMask & SWT.MOD3) != 0) {
					switch (e.character) {
						case PREV:
						case NEXT:
							// if no key binding (which would take precedence) 
							// then pass to our handlers it directly
							if (!hasBinding(e)){
								handleTraverseEvent(e);
							}
					}
				}
				break;
			default:
		}
	}
	
	/**
	 * Disable eclipse traversal event, and dispatch into our Alt/Ctrl
	 * handlers in place of it
	 * 
	 * @param e the trapped TraverseEvent
	 */
	protected void handleTraverseEvent(TraverseEvent e) {
		// setting detail to NONE but doit=true disables further processing
		e.detail = SWT.TRAVERSE_NONE;
		e.doit = true;

		Event ee = new Event();
		ee.character = e.character;
		ee.doit = true;
		ee.stateMask = (e.stateMask & SWT.MODIFIER_MASK);
		ee.keyCode = e.keyCode;

		ee.display = e.display;
		ee.widget = e.widget;	// will throw an exception if not valid
		ee.time = e.time;
		ee.data = e.data;

		switch (ee.stateMask) {
			case SWT.CONTROL:	// Emacs+ key binding forces CTRL 
				dispatchCtrl(new VerifyEvent(ee));
				break;
			case SWT.ALT:	// AFAIK MOD3 is always ALT
				dispatchAlt(new VerifyEvent(ee));
				break;
		}
	}
}
