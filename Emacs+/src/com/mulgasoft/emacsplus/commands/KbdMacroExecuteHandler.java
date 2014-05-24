/**
 * Copyright (c) 2009, 2010 Mark Feber, MulgaSoft
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package com.mulgasoft.emacsplus.commands;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.Vector;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRewriteTarget;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.VerifyKeyListener;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.ITextEditor;

import com.mulgasoft.emacsplus.Beeper;
import com.mulgasoft.emacsplus.EmacsPlusActivator;
import com.mulgasoft.emacsplus.EmacsPlusUtils;
import com.mulgasoft.emacsplus.IBeepListener;
import com.mulgasoft.emacsplus.execute.KbdMacroSupport;
import com.mulgasoft.emacsplus.execute.KbdMacroSupport.IKbdExecutionListener;
import com.mulgasoft.emacsplus.execute.KbdMacroSupport.KbdEvent;
import org.eclipse.swt.widgets.Display;

/**
 * Execute the keyboard macro
 * A prefix argument serves as a repeat count
 * A prefix argument of zero means repeat until error.
 * 
 * @author Mark Feber - initial API and implementation
 */
public class KbdMacroExecuteHandler extends EmacsPlusNoEditHandler {
//	we're not truly no-edit, but let any sub-commands decide (Eclipse will ignore buffer text input for us)
		
	protected static final String KBD_INTERRUPTED = EmacsPlusActivator.getResourceString("KbdMacro_Interrupted"); //$NON-NLS-1$
	protected static final String KBD_ITERATION = EmacsPlusActivator.getResourceString("KbdMacro_Iteration");     //$NON-NLS-1$
	protected static final String KBD_ITERATIONS = EmacsPlusActivator.getResourceString("KbdMacro_Iterations");   //$NON-NLS-1$
	protected static final String KBD_BINDING_WARNING = "KbdMacro_BadBinding";  								  //$NON-NLS-1$
	protected static final String NO_MACRO_ERROR = "KbdMacro_No_Error"; 										  //$NON-NLS-1$
	// Thread name when executing
	private static final String KBD_THREAD = "Kbd Macro Execution"; 											  //$NON-NLS-1$ 
	private static final int KEY_WAIT = 5000;
	private static final int LONG_WAIT = 10000; 
	
	private static boolean interrupted = false;
	private static int executeCount = 0;	// keep track of iterative and nested execution
	
	private String kbdMacroName = null;

	void setKbdMacroName(String kbdMacroName) {
		this.kbdMacroName = kbdMacroName;
	}
	
	String getKbdMacroName() {
		return kbdMacroName;
	}

	protected int incrementExecutionCount() {
		return executeCount++;
	}
	
	private int decrementExecutionCount() {
		return executeCount--;
	}

	protected class MacroCount {
		private int counter = 0;
		void addCounter() {
			++counter;
		}
		int getCounter() {
			return counter;
		}
	}
	
	@Override
	protected Object transformWithCount(ITextEditor editor, IDocument document, ITextSelection selection, ExecutionEvent event) {
		Event e = null;
		if (event != null && (e = (Event)event.getTrigger()) != null) {
			// disallow macro execution with modifier key
			if (e.stateMask != 0) {
				beep();
				asyncShowMessage(editor, KBD_BINDING_WARNING, true);
				return null;
			}
		} 
		MacroCount keepCount = null;
		
		if (hasKbdMacro()) {
			int count = Math.abs(getUniversalCount()); 
			if (incrementExecutionCount() == 0) {
				// only add first time (not on iterative/nested invocations)
				addBeeper();
				keepCount = new MacroCount();
				if (count == 0) {
					count = Integer.MAX_VALUE;	// essentially forever
				}
			}
			// synchronize key listeners for this invocation (may be nested)
			KbdLock kbdLock = new KbdLock();
			pushExecution(editor,kbdLock);
			runMacro(editor, document, selection, kbdLock, count, event.getCommand().getId(),keepCount);
		} else {
			beep();
			asyncShowMessage(editor, NO_MACRO_ERROR, true);
		}
		return null;
	}
	
	protected boolean hasKbdMacro() {
		return KbdMacroSupport.getInstance().hasKbdMacro() && !KbdMacroSupport.getInstance().isBusy(); 
	}

	protected void runMacro(ITextEditor editor, IDocument document, ITextSelection selection, KbdLock vkf, int count,
			final String cmdId) {
		runMacro(editor, document, selection, vkf, count, cmdId, null);
	}
	
	/**
	 * Run each instance of the macro in a non-ui thread, so it can wait for each keyboard (and other)
	 * event to be processed as both key and asynchronous events are processed in the same loop in 
	 * org.eclipse.swt.widgets.Display.readAndDispatch() which unfortunately can run all asynchronous 
	 * requests before processing the next key event depending on the timing of their arrival 
	 * 
	 * @param editor
	 * @param document
	 * @param selection
	 * @param vkf
	 * @param count
	 * @param cmdId
	 * @param keepCount
	 */
	protected void runMacro(final ITextEditor editor,final IDocument document,final ITextSelection selection, final KbdLock vkf, final int count,
			final String cmdId, final MacroCount keepCount) {
		new Thread(new Runnable() {public void run() {
			int counter = count;
			// get the undo Runnable wrappers
			Runnable[] undo = undoProtect(editor, keepCount);
			try {
				EmacsPlusUtils.asyncUiRun(undo[0]);			
				if (!isInterrupted() && selection != null && checkSelection(selection)) {
					executeOnce(editor, document, selection, vkf);
				}
			} catch (Exception e) {
				beep();
			} finally {
				EmacsPlusUtils.asyncUiRun(undo[1]);			
			}
			if (!isInterrupted() && --counter > 0) {
				runMacro(editor, document, selection, vkf, counter, cmdId, keepCount);
			} else {
				// notify listener on exit if we were executing a named or bound kbd macro
				notifyKbdListener(cmdId);
				final int times = ((keepCount != null) ? keepCount.getCounter() : 0); 
				EmacsPlusUtils.asyncUiRun(new Runnable() {
					public void run() {
						decrementExecutionCount();
						if (popExecution(editor) ==  0 && isInterrupted()) {
							EmacsPlusUtils.asyncUiRun(new Runnable() {public void run() {
							asyncShowMessage(editor, KBD_INTERRUPTED + String.format(((times == 1) ? KBD_ITERATION : KBD_ITERATIONS),times), true);}});
						}
					}
				});
			}
		}},KBD_THREAD).start();		
	}

	/**
	 * Iterate through the macro events once.
	 * After each event is submitted to the ui-thread wait for the event to be processed before moving to the next event
	 * This guarantees that keyboard events will be read properly before the next event is submitted 
	 * 
	 * @param editor
	 * @param document
	 * @param currentSelection
	 * @param vkf
	 * @throws BadLocationException
	 */
	protected void executeOnce(ITextEditor editor, IDocument document, ITextSelection currentSelection, KbdLock vkf)
	throws BadLocationException {
		ArrayList<KbdEvent> macro = (kbdMacroName == null) ? KbdMacroSupport.getInstance().getKbdMacroEvents()
				: KbdMacroSupport.getInstance().getKbdMacroEvents(kbdMacroName);
		if (editor != null && macro != null && !macro.isEmpty()) {
			for (KbdEvent e : macro) {
				if (!isInterrupted()) {
					queueRunner(e,editor,vkf);						
				}
			}
		}
	}
	
	/**
	 * Create and queue the appropriate Runnable for the kbd event type
	 * 
	 * @param event the KbdEvent
	 * @param editor
	 * @return the new Runnable
	 */
	private Runnable queueRunner(KbdEvent event, ITextEditor editor, KbdLock vkf) {
		Runnable result = null;
		String cmdId = null;
		if (event.getEvent() != null) {	// It's a key event
			result = getKeyRunner(event, editor);
			CountDownLatch latch = new CountDownLatch(1);
			vkf.setLatch(latch);
			EmacsPlusUtils.asyncUiRun(result);			
			try {
				if (!isInterrupted()){
					//Event e = event.getEvent();
					// System.out.println("wait " + event.toString() + " " + e.character + " " + e.keyCode 
					// + " " + e.stateMask + " " + e.type + " " + e.doit);					
					// if we see a beep interrupt, stop waiting
					myBeeper.setKeyLock(vkf);
					if (event.isWait()) {
						// wait for simulated key event to be processed before going to the next kbd event
						latch.await(KEY_WAIT,TimeUnit.MILLISECONDS);
					}
				}
			} catch (InterruptedException e) {
			} finally {
				vkf.setLatch(null);	// we're the only waiter
				myBeeper.setKeyLock(null);
			}
		} else if ((cmdId = event.getCmd()) != null) {	// It's a command id
			result = getCmdRunner(event, editor, vkf);
			// register for notification of macro completion
			addKbdListener(cmdId); 
			EmacsPlusUtils.asyncUiRun(result);
			// wait until the macro completes
			waitForKbdListener(cmdId);
		} else if (event.isExit()) {	// It's an exit from a minibuffer
			result = getExitRunner(vkf);
			EmacsPlusUtils.asyncUiRun(result);
		}
		return result;
	}
	
	/**
	 * Create the Runnable for executing a command (which may be a named/bound kbd macro)
	 * 
	 * @param event
	 * @param editor
	 * @param vkf
	 * @return the Runnable
	 */
	private Runnable getCmdRunner(KbdEvent event, final ITextEditor editor, final KbdLock vkf) {
		final String cmdId = event.getCmd();
		@SuppressWarnings("unchecked")
		final Map<String,?> parameters = (Map<String,?>) event.getCmdParameters();
		final KbdMacroExecuteHandler executeHandler = this;
		return new Runnable() {
			public void run() {
				if (executeHandler.isInterrupted()) {
					return;
				}
				try {
					ITextEditor current = EmacsPlusUtils.getCurrentEditor();
					if (parameters != null) {
						KbdMacroExecuteHandler.this.executeCommand(cmdId, parameters, null,
								(current != null ? current : editor));
					} else {
						KbdMacroExecuteHandler.this.executeCommand(cmdId, null,
								(current != null ? current : editor));
					}
				} catch (Exception e) {
					if (isMacro(cmdId)) {
						notifyKbdListener(cmdId); 
					}
				} 
			}
		};
	}

	/**
	 * Create the Runnable for posting a key event
	 * 
	 * @param event
	 * @param editor
	 * @return the Runnable
	 */
	private Runnable getKeyRunner(final KbdEvent event, ITextEditor editor) {
		final KbdMacroExecuteHandler executeHandler = this;
		return new Runnable() {
			Event ee = event.getEvent();
			
			public void run() {
				if (executeHandler.isInterrupted()){
					return;
				}
				// to support internal Emacs+ control sequences in minibuffer commands:
				// The .post event ignores the stateMask, so force CTRL/ALT/Shift keys manually when required
				Event shiftless = null;
				Event ctrlless = null;
				Event altless = null;
				Event cmdless = null;
				Display display = PlatformUI.getWorkbench().getDisplay();
				try {
					if (ee.stateMask != 0) {
						if ((ee.stateMask & SWT.SHIFT) != 0){
							shiftless = KbdMacroExecuteHandler.this.maskEvent(SWT.SHIFT);
							display.post(shiftless);
						}			
						if ((ee.stateMask & SWT.CTRL) != 0) {
							ctrlless = KbdMacroExecuteHandler.this.maskEvent(SWT.CTRL);
							display.post(ctrlless);
						}
						if ((ee.stateMask & SWT.ALT) != 0) {
							altless = KbdMacroExecuteHandler.this.maskEvent(SWT.ALT);
							display.post(altless);
						}
						if ((ee.stateMask & SWT.COMMAND) != 0) {
							cmdless = KbdMacroExecuteHandler.this.maskEvent(SWT.COMMAND);
							display.post(cmdless);
						}
					}
					ee.doit = true;
					ee.type = SWT.KeyDown;					
					executeHandler.setKeyEvent(ee);
					display.post(ee);
//					System.out.println("post " + ee.character);					
				} finally {
					if (ee.stateMask != 0) {
						if (shiftless != null) {
							shiftless.type = SWT.KeyUp;
							shiftless.doit = true;
							display.post(shiftless);
						}
						if (ctrlless != null) {
							ctrlless.type = SWT.KeyUp;
							ctrlless.doit = true;
							display.post(ctrlless);
						}
						if (altless != null) {
							altless.type = SWT.KeyUp;
							altless.doit = true;
							display.post(altless);
						}
						if (cmdless != null) {
							cmdless.type = SWT.KeyUp;
							cmdless.doit = true;
							display.post(cmdless);
						}						
					}
				}
			}
		};		
	}
	
	/**
	 * Create a Runnable to ensure the proper exit from a minibuffer command
	 * @param vkf
	 * @return the Runnable
	 */
	private Runnable getExitRunner(final KbdLock vkf) {
		return new Runnable() {
			public void run() {
				if (KbdMacroSupport.getKbdMinibuffer() != null) {
					KbdMacroSupport.getKbdMinibuffer().endSession();
				}
			}
		};		
	}
	
	/**
	 * Create a simple event with a stateMask based keyCode
	 *  
	 * @param mask
	 * @return
	 */
	private Event maskEvent(int mask) {
		Event x = new Event();
		x.keyCode = mask;
		x.type = SWT.KeyDown;
		x.doit = true;
		return x;
	}

	/**
	 * Get the undo Runnable wrappers
	 * 
	 * @param editor
	 * @return begin and end undoProtect wrappers
	 */
	protected Runnable[] undoProtect(ITextEditor editor, final MacroCount keepCount) {
		Runnable[] result = new Runnable[2];
		// use widget to avoid unpleasant scrolling side effects of IRewriteTarget
		final Control widget = getTextWidget(editor);;
		final IRewriteTarget rt = (IRewriteTarget) editor.getAdapter(IRewriteTarget.class);;
		result[0] = new Runnable() {
			public void run() {
				if (rt != null) {
					rt.beginCompoundChange();
				}
				setRedraw(widget,false);
			}
		};
		result[1] = new Runnable() {
			public void run() {
				setRedraw(widget, true);
				if (rt != null) {
					rt.endCompoundChange();
				}
				if (!isInterrupted() && keepCount != null) {
					// we've finished one more loop of the main macro
					keepCount.addCounter();
				}
			}
		};
		return result;
	}

	/**
	 * Determine if the id belongs to a kbd macro execution command
	 * 
	 * @param id - command id
	 * @return true if will execute a kbd macro
	 */
	private boolean isMacro(String id) {
		// verify id type
		return EmacsPlusUtils.isMacroId(id);
	}
	
	/* Listener support for detecting inner kbd macro completion 
	 * If the command invocation is a named kbd macro, then wait for it's nested invocation
	 * to complete before proceeding with the calling kbd macro 
	 */
	
	private static Map<String,CountDownLatch>kbdListeners = new Hashtable<String,CountDownLatch>();

	private void notifyKbdListener(String key) {
		CountDownLatch lock = kbdListeners.get(key);
		if (lock != null) {
			kbdListeners.remove(key);
			lock.countDown();
		}
	}

	private void waitForKbdListener(String key) {
		CountDownLatch lock = null;
		if (isMacro(key) && (lock = kbdListeners.get(key)) != null) {
			try {
				lock.await(LONG_WAIT,TimeUnit.MILLISECONDS);
			} catch (InterruptedException e) {}
		}
	}

	private void addKbdListener(String key) {
		if (isMacro(key)) {
			kbdListeners.put(key, new CountDownLatch(1));
		}
	}

	/* Listener support for detecting beep errors */

	private static KbdBeepListener myBeeper = new KbdBeepListener();

	private static class KbdBeepListener implements IBeepListener { 

		private KbdLock keywait = null; 
		
		public void beepInterrupt() {
			setInterrupted(true);
			Beeper.removeBeepListener(this);
			if (keywait != null) {
				keywait.notifyLock();
			}
			// exit minibuffer - it's often an ISearch 
			if (KbdMacroSupport.getKbdMinibuffer() != null) {
				KbdMacroSupport.getKbdMinibuffer().endSession();
			}
		}
		
		public void setKeyLock(KbdLock keywait) {
			this.keywait = keywait;
		}
	};
	
	void addBeeper() {
		setInterrupted(false);
		Beeper.addBeepListener(myBeeper);
	}
	
	void removeBeeper() {
		Beeper.removeBeepListener(myBeeper);
	}

	public final boolean isInterrupted() {
		return interrupted;
	}

	private static void setInterrupted(boolean interrupt) {
		KbdMacroExecuteHandler.interrupted = interrupt;
	}
	
	/* Support for maintaining correct key listeners during execution */
	
	// remember the key listeners of preceding kbd macros on the stack
	private static Stack<KbdLock> lockStack = new Stack<KbdLock>();
	
	protected void pushExecution(ITextEditor editor, KbdLock newLock) {
		if (!lockStack.isEmpty()) {
			KbdMacroSupport.getInstance().setExecuting(false, editor,lockStack.peek());			
		}
		lockStack.push(newLock);
		KbdMacroSupport.getInstance().setExecuting(true, editor,newLock);
	}
	
	/**
	 * Remove current key listener and restore previous if present
	 * 
	 * @param editor
	 * @return the number or locks remaining in the stack
	 */
	protected int popExecution(ITextEditor editor) {
		if (!lockStack.isEmpty()) {
			KbdMacroSupport.getInstance().setExecuting(false, editor,lockStack.pop());			
			if (!lockStack.isEmpty()) {
				KbdMacroSupport.getInstance().setExecuting(true, editor,lockStack.peek());			
			} else {
				// we're done with all execution
				notifyExecutionListeners();
				removeBeeper();
			}
		}
		return lockStack.size();
	}
	
	/* Listener support for notification after execution */

	private static List<IKbdExecutionListener> executionListners = new Vector<IKbdExecutionListener>();
	
	public static synchronized void addExecutionListener(IKbdExecutionListener listener) {
		executionListners.add(listener);
	}
	
	public synchronized void notifyExecutionListeners() {
		for (IKbdExecutionListener k : executionListners) {
			synchronized(k) {
				k.executionDone();
			}
		}
		executionListners.clear();
	}
	
	/* Key Listener support to detect any real keyboard events */
	
	// since our key events are processed serially, we can detect if user does something to interrupt 
	static private int keyCode = 0;
	static private int stateMask = 0;

	private void setKeyEvent(Event e){
		keyCode = e.keyCode;
		stateMask = e.stateMask;
	}
	
	private void clearKeyEvent(){
		keyCode = 0;
		stateMask = 0;
	}
	
	/**
	 * Check if this is one of the manually CTRL/ALT/Command/Shift keys from the .post call above
	 * keyCode is a simple state key and is included in contained event's stateMask
	 * 
	 * @param keyCode from the verify key event
	 * @return true if it is ours
	 */
	private boolean isSpecial(int keyCode) {
		return (((keyCode == SWT.ALT ) || (keyCode == SWT.COMMAND ) || (keyCode == SWT.CTRL) || (keyCode == SWT.SHIFT)) && ((keyCode & stateMask) != 0));
	}
	
	public class KbdLock implements VerifyKeyListener {

		private CountDownLatch latch = null;

		private CountDownLatch setLatch(CountDownLatch latch) {
			CountDownLatch result = this.latch;
			this.latch = latch;
			return result;
		}

		public void notifyLock() {
			try {
				if (latch != null) {
					latch.countDown();
				}
			} catch (Exception e) {}
		}

		/**
		 * @see org.eclipse.swt.custom.VerifyKeyListener#verifyKey(org.eclipse.swt.events.VerifyEvent)
		 */
		public void verifyKey(VerifyEvent event) {
			// ignore the stateMask keyCodes, otherwise require a match
			if (!isSpecial(event.keyCode)) { 
				// A real (user) key event must have happened
				if (event.keyCode != keyCode || event.stateMask != stateMask) {
					event.doit = false;
					// short circuit and beep
					setInterrupted(true);
					Beeper.beep();	// this will notify
				} else {
					// detect kbd macro key event
					clearKeyEvent();
					notifyLock();
					PlatformUI.getWorkbench().getDisplay().post(upEvent(event));
				}
			}
		}

		// Linux, at least, requires this up event, but on other systems can't be posted until
		// we've received the KeyDown event
		private Event upEvent(VerifyEvent event) {
			Event e = new Event();
			e.keyCode = event.keyCode;
			e.character = event.character;
			e.stateMask = event.stateMask;
			e.type = SWT.KeyUp;
			e.doit = true;
			return e;
		}

	}

}
