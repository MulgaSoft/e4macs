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

import org.eclipse.core.commands.Command;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.jface.bindings.Binding;
import org.eclipse.jface.bindings.keys.KeyStroke;
import org.eclipse.jface.text.TextEvent;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.texteditor.ITextEditor;

import com.mulgasoft.emacsplus.IEmacsPlusCommandDefinitionIds;
import com.mulgasoft.emacsplus.execute.IBindingResult;
import com.mulgasoft.emacsplus.execute.IUniversalResult;
import org.eclipse.jface.bindings.keys.KeySequence;

/**
 * Read a number and execute a subsequent command n times
 *
 * @author Mark Feber - initial API and implementation
 */
public class UniversalMinibuffer extends KeyHandlerMinibuffer {	

	/*
	 * C-u runs `universal-argument'
	 * 
	 * Begin a numeric argument for the following command.
	 *  
	 * Digits or minus sign following C-u make up the numeric argument. C-u following
	 * the digits or minus sign ends the argument. C-u without digits or minus
	 * sign provides 4 as argument. Repeating C-u without digits or minus sign
	 * multiplies the argument by 4 each time.
	 */
	private static int MULTIPLIER = 4;
	
	private int argumentCount = 4;
	private StringBuilder countBuf;
	private StringBuilder prefixBuf; 
	private String uprefix;
	
	private final static String CPREFIX = "C-"; 	//$NON-NLS-1$
	private final static String MPREFIX = "M-"; 	//$NON-NLS-1$
	
	private int triggerCount = 0;
	private int triggerMask = 0;
	private char triggerChar = 0x15;	// default to ^U
	private boolean restart = false;
	private boolean executed = false;
	
	private int uTriggerMask = SWT.CTRL;	// universal digit-argument reset
	private char uTriggerChar = 0x15;	// universal digit-argument reset (^U)
	
	/**
	 * @param executable
	 */
	public UniversalMinibuffer(IMinibufferExecutable executable) {
		super(executable);
	}

	private void setArgumentCount(int count) {
		argumentCount = count;
	}
	
	private boolean isMinus(Event event) {
		// OS X has a different character for ^-, so also check keyCode
		return ('-' == event.character || 45 == event.keyCode);		
	}
	private boolean isMinus(VerifyEvent event) {
		// OS X has a different character for ^-, so also check keyCode
		return ('-' == event.character || 45 == event.keyCode);		
	}
	
	/**
	 * @see com.mulgasoft.emacsplus.minibuffer.KeyHandlerMinibuffer#beginSession(ITextEditor, IWorkbenchPage, ExecutionEvent)
	 */
	@Override
	public boolean beginSession(ITextEditor editor, IWorkbenchPage page, ExecutionEvent event) {
		
		countBuf = new StringBuilder();
		Object eTrigger = event.getTrigger();
		if (eTrigger instanceof Event) {
			char c = ((Event)eTrigger).character;
			if (c != 0) {
				triggerMask = ((Event)eTrigger).stateMask;
				if (Character.isDigit(c)) {
					countBuf.append(c);
				} else if (isMinus((Event)eTrigger)) {
					countBuf.append('-');
					setArgumentCount(-1);
				} else {
					// remember non-numeric character that launched us
					triggerChar = c;
				}
			}
		}
		executed = false;
		triggerCount = 0;
		prefixBuf  = new StringBuilder();
		// The handler sets the prefix based on the key invocation
		uprefix = getExecutable().getMinibufferPrefix().trim();
		return super.beginSession(editor, page, event);
	}

	/**
	 * @see com.mulgasoft.emacsplus.minibuffer.KeyHandlerMinibuffer#charEvent(org.eclipse.swt.events.VerifyEvent)
	 */
	@Override
	protected void charEvent(VerifyEvent event) {
		event.doit = false;
		if (!hasTrigger()) {
			// accumulate multiple u-a invocations, or reset on delayed repeat
			if (isUniversalKey(event)) {
				processUniversal();
			} else if (!restart && (Character.isDigit(event.character) && 
					((event.stateMask & SWT.MODIFIER_MASK)== 0 || event.stateMask == triggerMask))) { 	// if plain number
				// build up the number string
				setArgumentCount(1);
				addToCount(event.character);
			} else if (!restart && isMinus(event)) {
				resetToMinus(event.keyCode, event.stateMask);
			} else {
				// else building trigger sequence
				addToTrigger(event);
			}
		} else {
			// continue with trigger sequence
			addToTrigger(event);
		}
		updatePrefix();
	}

	/**
	 * @param event
	 */
	private void resetToMinus(int keyCode, int stateMask) {
		countBuf = new StringBuilder();				
		countBuf.append('-');				
		setArgumentCount(-1);
		resetPrefix();
		updatePrefix(this.getTrigger(keyCode, stateMask));
	}
	
	private boolean isUniversalKey(VerifyEvent event) {
		
		boolean result = false;
		if (result= (event.character == triggerChar && event.stateMask == triggerMask)) {
			if (triggerChar == '-') {
				uprefix = getTrigger(event.keyCode,event.stateMask);
				result = false;
			} else if (result = (event.character == uTriggerChar && (event.stateMask  & SWT.MODIFIER_MASK) == uTriggerMask)) {
				// the universal reset value
				uprefix = getTrigger(event.keyCode,event.stateMask);
			}
		} else if (triggerMask == 0) {
			// likely invoked by M-x universal-argument or from kbd macro 
			if (result = (event.character == uTriggerChar && (event.stateMask & SWT.MODIFIER_MASK) == uTriggerMask)){
				updatePrefix(' ');
			}
		}
		return result;
	}

	/**
	 * On universal key detection, process appropriately depending on state of count, etc. 
	 */
	private void processUniversal() {
		String nextPrefix = uprefix;
		if (restart) {
			// reset and restart count accumulation
			restart = false;
			countBuf = new StringBuilder();
		}
		if (countBuf.length() == 0) {
			setArgumentCount(argumentCount * MULTIPLIER);
		} else if (countBuf.length() == 1 && countBuf.charAt(0) == '-') {
			setArgumentCount(argumentCount * MULTIPLIER);
			// remove from buffer, so it will reset on integer argument
			countBuf.deleteCharAt(0);
		} else {
			// flag an end to numeric input - Emacs restarts the count at 1 if yet another C-u appears
			restart = true;
			setArgumentCount(1);
			updatePrefix(' ');
		}
		// add universal prefix to minibuffer prefix 
		updatePrefix(nextPrefix);
	}
	
	private void addToCount(char number) {
		// add number
		countBuf.append(number);
		updatePrefix(number);
	}
	
	private void addToTrigger(VerifyEvent event) {
		triggerCount++;
		super.charEvent(event);
	}
	
	private void updatePrefix() {
		if (!executed) {
			setMinibufferPrefix(getCommandString());
			initMinibuffer(EMPTY_STR);
		}
	}
	
	private void updatePrefix(String nextfix) {
		updatePrefix(nextfix, true);
	}
	
	private void updatePrefix(String nextfix, boolean isString) {
		prefixBuf.append(nextfix);
		if (isString) {
			prefixBuf.append(' ');
		}
	}
	
	private void updatePrefix(char nextfix) {
		updatePrefix(normalizeChar(nextfix), false);
	}
	
	private String getCommandString() {
		return (prefixBuf.toString() + ((getTrigger() == null) ? EMPTY_STR : ' ' + getTrigger().format().trim())); 
	}
	
	private void resetPrefix() {
		prefixBuf = new StringBuilder();
	}
	
	/**
	 * Adapt the prefix display in the minibuffer to the actual key binding
	 * 
	 * @param keyCode
	 * @param stateMask
	 * @return the trigger string
	 */
	public String getTrigger(int keyCode, int stateMask) {
		StringBuilder result = new StringBuilder();
		String c = new String(Character.toChars(keyCode));
		switch (stateMask) {
		case SWT.CTRL:
			result.append(CPREFIX);
			result.append(c);
			break;
		case SWT.ALT:
			result.append(MPREFIX);
			result.append(c);
			break;
		case SWT.CTRL|SWT.ALT:
			result.append(CPREFIX);
			result.append(MPREFIX);
			result.append(c);
			break;
		default:
			result.append(KeyStroke.getInstance(stateMask,keyCode).format());
		}
		if (result.length() > 0) {
			result.append(' ');
		}
		return result.toString();
	}

	/**
	 * @see com.mulgasoft.emacsplus.minibuffer.ExecutingMinibuffer#executeResult(org.eclipse.ui.texteditor.ITextEditor, java.lang.Object)
	 */
	@Override
	protected boolean executeResult(ITextEditor editor, Object commandResult) {
		boolean result = true;
		String resultString = getCommandString();
		setResultString(resultString, false);
		setResultMessage(resultString, false, true);
		try {
			setExecuting(true);
			result =  exitExecuteResult(editor, commandResult);
		} finally {
			setExecuting(false);
			executed = true;
		} 
		return result;
	}

	/**
	 * @see com.mulgasoft.emacsplus.minibuffer.KeyHandlerMinibuffer#getResult(Binding, KeySequence, String)
	 */
	@Override
	protected IBindingResult getResult(final Binding binding, final KeySequence trigger, String triggerString) {

		// key character is only > 0 if it is stand alone
		int charpoint = getKeyCharacter();
		String character = null;
		if (binding == null && charpoint > 0 && triggerCount < 2) {
			if (charpoint == SWT.CR || charpoint == SWT.LF) {
				character = getEol();
			} else if (charpoint == SWT.BS) {
				character = new String(Character.toChars(charpoint));
			} else if ((Character.isWhitespace(charpoint)) || (charpoint > ' ')) {
				character = new String(Character.toChars(charpoint));
			}
		}
		if (countBuf.length() > 0) {
			try {
				if (countBuf.length() == 1 && countBuf.charAt(0)== '-') {
					;	// just use argument Count
				} else {
					setArgumentCount(Integer.parseInt(countBuf.toString()));
				}
			} catch (NumberFormatException e) {
					// bad count
					setArgumentCount(1);
			}
		}
		final String key = character;
		final boolean notNumeric = countBuf.length() == 0 && argumentCount == 4;	// flag whether a number was entered into the minibuffer

		return new IUniversalResult() {
			public Binding getKeyBinding() { return binding; }
			public String getKeyString() { return key; }
			public int getCount() { return argumentCount; }
			public boolean isNumeric() { return !notNumeric; }
			public KeySequence getTrigger() { return trigger; }
		};
	}

	// ISelectionChangedListener
	/* (non-Javadoc)
	 * @see com.mulgasoft.emacsplus.minibuffer.WithMinibuffer#selectionChanged(org.eclipse.jface.viewers.SelectionChangedEvent)
	 */
	public void selectionChanged(SelectionChangedEvent event) {
		// During execution of sub-command, selection can change
		if (!isExecuting()) {
			super.selectionChanged(event);
		}
	}
	
	// ITextListener
	/* (non-Javadoc)
	 * @see com.mulgasoft.emacsplus.minibuffer.WithMinibuffer#textChanged(org.eclipse.jface.text.TextEvent)
	 */
	public void textChanged(TextEvent event) {
		// During execution of sub-command, text can change
		if (!isExecuting()) {
			super.textChanged(event);
		}
	}
	
	// Support simple Alt re-binding of universal-argument
	private Traverser universalTraverser = new Traverser();

	/* (non-Javadoc)
	 * @see com.mulgasoft.emacsplus.Minibuffer#addOtherListeners(org.eclipse.jface.text.source.ISourceViewer, org.eclipse.swt.custom.StyledText)
	 */
	protected void addOtherListeners(IWorkbenchPage page, ISourceViewer viewer, StyledText widget) {
		Display.getCurrent().addFilter(SWT.Traverse, universalTraverser);
	}

	/* (non-Javadoc)
	 * @see com.mulgasoft.emacsplus.Minibuffer#removeOtherListeners(org.eclipse.jface.text.source.ISourceViewer, org.eclipse.swt.custom.StyledText)
	 */
	protected void removeOtherListeners(IWorkbenchPage page, ISourceViewer viewer, StyledText widget) {
		Display.getCurrent().removeFilter(SWT.Traverse, universalTraverser);
	}

	private boolean isUniversalBinding() {
		boolean result = false;
		Binding bind = getBinding();
		if (bind != null) {
			Command cmd = bind.getParameterizedCommand().getCommand();
			result = (cmd != null && cmd.getId().equals(IEmacsPlusCommandDefinitionIds.UNIVERSAL_ARGUMENT));
		}
		return result;
	}
	
	private class Traverser implements Listener {

		public void handleEvent(Event event) {
			boolean check = checkKey(event.stateMask, event.keyCode, event.character);
			if (check && isUniversalBinding()) {
				// clear key cache
				resetKeys();
				// check for binding of the form M-1 etc.
				if (Character.isDigit(event.character)) {
					addToCount(event.character);
				} else if (isMinus(event)) {
					resetToMinus(event.keyCode, event.stateMask);
				} else {
					// reset count
					processUniversal();
				}
				updatePrefix();
			} else if (processKey(check,event.keyCode)) {
				leave();
			}
			// setting detail to NONE but doit=true disables further processing
			event.type = SWT.None;
			event.detail = SWT.TRAVERSE_NONE;
			event.doit = true;
		}
	}
}
