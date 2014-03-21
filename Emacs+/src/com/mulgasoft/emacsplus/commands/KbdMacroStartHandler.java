/**
 * Copyright (c) 2009, 2014 Mark Feber, MulgaSoft
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package com.mulgasoft.emacsplus.commands;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.ui.texteditor.ITextEditor;

import com.mulgasoft.emacsplus.EmacsPlusUtils;
import com.mulgasoft.emacsplus.IEmacsPlusCommandDefinitionIds;
import com.mulgasoft.emacsplus.execute.KbdMacroSupport;
import com.mulgasoft.emacsplus.execute.KbdMacroSupport.IKbdExecutionListener;

/**
 * Start defining a keyboard macro
 * 
 * If called with ^U, append keys to the last keyboard macro after re-executing it.
 * If called with ^U ^U, append keys to the last keyboard macro without re-executing it.
 * 
 * @author Mark Feber - initial API and implementation
 */
public class KbdMacroStartHandler extends EmacsPlusCmdHandler {

	private static String KBD_START_MSG = "KbdMacro_Start"; 			//$NON-NLS-1$
	private static String KBD_APPEND_MSG = "KbdMacro_Append";   		//$NON-NLS-1$
	protected static final String NO_MACRO_ERROR = "KbdMacro_No_Error"; //$NON-NLS-1$	
	
	private final static int WAIT_TIME = 5000; 
	private final static int SLEEP_TIME = 500; 
	
	public Object execute(final ExecutionEvent event) throws ExecutionException {
		final ITextEditor editor = getTextEditor(event);
		if (editor != null) { 
			final int uArg = this.extractUniversalCount(event);
			if (uArg > 1 && (!KbdMacroSupport.getInstance().hasKbdMacro() || KbdMacroSupport.getInstance().isBusy())) {
				beep();
				asyncShowMessage(editor, NO_MACRO_ERROR, true);
			} else if (uArg > 1 && uArg < 16) {
				// pre-execute the current macro before continuing with the definition
				new Thread(new Runnable() {
					// do this in a separate thread, so the ui thread can complete the macro before we queue the start
					public void run() {
						final CountDownLatch latch = new CountDownLatch(1);
						try {
							// give time for fingers to leave the keyboard and any key up events to come through
							Thread.sleep(SLEEP_TIME);
							// register for execution completion before queue
							KbdMacroExecuteHandler.addExecutionListener(new IKbdExecutionListener() {
								public void executionDone() {
									latch.countDown();
								}
							});
							EmacsPlusUtils.asyncUiRun(new Runnable() {
								public void run() {
									try {
										KbdMacroStartHandler.this.executeCommand(IEmacsPlusCommandDefinitionIds.KBDMACRO_EXECUTE, null, editor);
									} catch (Exception e) { e.printStackTrace();}
								}
							});
							latch.await(WAIT_TIME,TimeUnit.MILLISECONDS);
						} catch (Exception e) {e.printStackTrace();}
						queueStart(editor,uArg);
					}
				}).start();
			} else {
				queueStart(editor,uArg);				
			}
		}
		return null;
	}
	
	/**
	 * Queue the definition start 
	 *  
	 * @param editor
	 * @param uArg
	 */
	private void queueStart(final ITextEditor editor, final int uArg) {
		// queue the definition
		EmacsPlusUtils.asyncUiRun(new Runnable() {
			public void run() {
				KbdMacroSupport.getInstance().startKbdMacro(editor, uArg > 1);
				KbdMacroStartHandler.this.asyncShowMessage(editor, (uArg > 1) ? KBD_APPEND_MSG : KBD_START_MSG, false);
			}
		});
	}
	
	/**
	 * @see com.mulgasoft.emacsplus.commands.EmacsPlusCmdHandler#transform(ITextEditor, IDocument, ITextSelection, ExecutionEvent)
	 */
	@Override
	protected int transform(ITextEditor editor, IDocument document,
			ITextSelection currentSelection, ExecutionEvent event)
			throws BadLocationException {
		return NO_OFFSET;
	}
	
	@Override
	protected boolean isLooping() {
		return false;
	}

}
