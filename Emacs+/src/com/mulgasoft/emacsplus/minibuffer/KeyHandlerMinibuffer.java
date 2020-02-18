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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.jface.bindings.Binding;
import org.eclipse.jface.bindings.keys.KeySequence;
import org.eclipse.jface.bindings.keys.KeyStroke;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.keys.IBindingService;
import org.eclipse.ui.texteditor.ITextEditor;

import com.mulgasoft.emacsplus.RingBuffer;
import com.mulgasoft.emacsplus.execute.IBindingResult;

/**
 * Handle key input and determine binding information about it
 * 
 * Some info gleaned from: org.eclipse.jface.bindings.keys.KeySequenceText
 * 
 * @author Mark Feber - initial API and implementation
 */
public class KeyHandlerMinibuffer extends ExecutingMinibuffer { 

	private String prefix = null;
	private List<KeyStroke> keys = new ArrayList<KeyStroke>();
	private Binding binding = null;
	private KeySequence trigger = null;
	private int keyCharacter = 0;
	private IBindingService bindingService;
	
	/**
	 * @see com.mulgasoft.emacsplus.minibuffer.WithMinibuffer#beginSession(ITextEditor, IWorkbenchPage, ExecutionEvent)
	 */
	@Override
	public boolean beginSession(ITextEditor editor, IWorkbenchPage page, ExecutionEvent event) {
		bindingService = (IBindingService) PlatformUI.getWorkbench().getService(IBindingService.class);
		return super.beginSession(editor, page, event);
	}

	/**
	 * @return the keyCharacter
	 */
	protected int getKeyCharacter() {
		return keyCharacter;
	}

	/**
	 * @param keyCharacter the keyCharacter to set
	 */
	protected void setKeyCharacter(int keyCharacter) {
		this.keyCharacter = keyCharacter;
	}

	/**
	 * @return the binding
	 */
	protected Binding getBinding() {
		return binding;
	}

	/**
	 * @param binding the binding to set
	 */
	protected void setBinding(Binding binding) {
		this.binding = binding;
	}

	/**
	 * @return the trigger
	 */
	protected KeySequence getTrigger() {
		return trigger;
	}

	/**
	 * @param trigger the trigger to set
	 */
	protected void setTrigger(KeySequence trigger) {
		this.trigger = trigger;
	}

	protected boolean hasTrigger() {
		return getTrigger() != null;
	}

	/**
	 * Reset state of keys processed so far 
	 */
	protected void resetKeys() {
		trigger = null;
		keys = new ArrayList<KeyStroke>(); 
	}
	
	public String getMinibufferPrefix() {
		if (prefix == null) {
			prefix = super.getMinibufferPrefix();
		}
		return prefix;
	}
	
	protected void setMinibufferPrefix(String nextfix) {
		this.prefix = super.getMinibufferPrefix() + nextfix;
	}

	/**
	 * @param executable
	 */
	public KeyHandlerMinibuffer(IMinibufferExecutable executable) {
		super(executable);
	}

	/**
	 * @see com.mulgasoft.emacsplus.minibuffer.HistoryMinibuffer#getHistoryRing()
	 */
	@Override
	protected RingBuffer<Object> getHistoryRing() {
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
	 * @see com.mulgasoft.emacsplus.minibuffer.WithMinibuffer#handlesTab()
	 */
	@Override
	protected boolean handlesTab() {
		return true;
	}

	/**
	 * @see com.mulgasoft.emacsplus.minibuffer.WithMinibuffer#charEvent(org.eclipse.swt.events.VerifyEvent)
	 */
	protected void charEvent(VerifyEvent event) {
		if (processKey(event)) {
			leave();
		}
	}
	
	/**
	 * @see com.mulgasoft.emacsplus.minibuffer.WithMinibuffer#noCharEvent(org.eclipse.swt.events.VerifyEvent)
	 */
	protected void noCharEvent(VerifyEvent event) {
		// process if unicode or more than just modifier keys
		if (((event.keyCode & SWT.KEYCODE_BIT) != 0) || (event.keyCode & SWT.MODIFIER_MASK) == 0) {
			if (processKey(event)) {
				leave();
			}
		} else {
			event.doit = false;
		}
	}
	
	private boolean processKey(VerifyEvent event) {
		return processKey(checkKey(event), event.keyCode); 		
	}
	
	protected boolean processKey(boolean checked, int keyCode) {
		boolean result = checked; 
		final String triggerString = ((trigger != null) ? trigger.format() : String.valueOf(keyCode));
		setMinibufferPrefix(triggerString);
		updateStatusLine(EMPTY_STR);
		if (result) {
			if (binding != null) {
				// we have an exact match
				result = executeResult(getEditor(), getResult(binding,trigger,triggerString));				
			} else if (trigger != null) {
				result = false;
			}
		} else {
			result = executeResult(getEditor(), getResult(null,trigger, triggerString));
		}
		return result;
	}
	
	private boolean checkKey(VerifyEvent event) {
		event.doit = false;
		return checkKey(event.stateMask, event.keyCode, event.character);
	}
		
	/**
	 * Check if the (accumulated) key strokes have a single binding
	 * 
	 * @param state
	 * @param keyCode
	 * @param character
	 * 
	 * @return true if most unique binding, else false
	 */
	protected boolean checkKey(int state, int keyCode, int character) {
		boolean result = true;
		keys.add(getKey(state,keyCode,character));
		trigger = KeySequence.getInstance(keys);
		binding = bindingService.getPerfectMatch(trigger);
		boolean partial = bindingService.isPartialMatch(trigger); 
		if (binding == null) {
			if (!partial) {
				keyCharacter = character;
				result = false; 
			}
		} else if (partial) {
			// keep looking when there are additional partial matches
			binding = null;
		}
		return result;
	}

	private KeyStroke getKey(int state, int keyCode,int character) {
		int result = keyCode;
		if (state ==0 && keys.size() == 0) {
			result = keyCode;
		} else if (state == SWT.SHIFT) {
			// handle characters with different shift values
			state = 0;
			result = character;
		} else {
			result = Character.toUpperCase(keyCode);  
		}
		return KeyStroke.getInstance(state,result);
	}
	
	protected IBindingResult getResult(final Binding binding, final KeySequence trigger, final String triggerString) {
		return new IBindingResult() {
				public Binding getKeyBinding() { return binding; }
				public String getKeyString() { return triggerString; }
				public KeySequence getTrigger() { return trigger; }
		};
	}

	private Traverser traverser = new Traverser();

	/**
	 * @see com.mulgasoft.emacsplus.minibuffer.HistoryMinibuffer#addOtherListeners(IWorkbenchPage, ISourceViewer, StyledText)
	 */
	protected void addOtherListeners(IWorkbenchPage page, ISourceViewer viewer, StyledText widget) {
		Display.getCurrent().addFilter(SWT.Traverse, traverser);
	}

	/**
	 * @see com.mulgasoft.emacsplus.minibuffer.HistoryMinibuffer#removeOtherListeners(IWorkbenchPage, ISourceViewer, StyledText)
	 */
	protected void removeOtherListeners(IWorkbenchPage page, ISourceViewer viewer, StyledText widget) {
		Display.getCurrent().removeFilter(SWT.Traverse, traverser);
	}

	private class Traverser implements Listener {

		public void handleEvent(Event event) {
			if (processKey(checkKey(event.stateMask, event.keyCode, event.character),event.keyCode)) {
				leave();
			}
			// setting detail to NONE but doit=true disables further processing
			event.type = SWT.None;
			event.detail = SWT.TRAVERSE_NONE;
			event.doit = true;
		}
	}
}
