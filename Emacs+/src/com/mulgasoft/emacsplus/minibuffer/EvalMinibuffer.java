/**
 * Copyright (c) 2009-2012 Mark Feber, MulgaSoft
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package com.mulgasoft.emacsplus.minibuffer;

import java.util.SortedMap;

import org.eclipse.ui.texteditor.ITextEditor;

import com.mulgasoft.emacsplus.RingBuffer;
import com.mulgasoft.emacsplus.preferences.PrefVars;

/**
 * Select the preference variable to eval/setq 
 * 
 * @author Mark Feber - initial API and implementation
 */
public class EvalMinibuffer extends CompletionMinibuffer {

	/**
	 * @param executable
	 */
	public EvalMinibuffer(IMinibufferExecutable executable) {
		super(executable);
	}
	
	/**
	 * @see com.mulgasoft.emacsplus.minibuffer.CompletionMinibuffer#getCompletions()
	 */
	@Override
	protected SortedMap<String, PrefVars> getCompletions() {
		return PrefVars.getCompletions(false);
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

	/**
	 * Convert string to EMPSetq object
	 * 
	 * @see com.mulgasoft.emacsplus.minibuffer.ExecutingMinibuffer#executeResult(org.eclipse.ui.texteditor.ITextEditor, java.lang.Object)
	 */
	@Override
	protected boolean executeResult(ITextEditor editor, Object commandResult) {
		return super.executeResult(editor,getCompletions().get(commandResult));
	}
		
	/**
	 * @see com.mulgasoft.emacsplus.minibuffer.WithMinibuffer#handlesTab()
	 */
	@Override
	protected boolean handlesTab() {
		// enable tab completion
		return true;
	}

	/**** Local RingBuffer: use lazy initialization holder class idiom ****/

	/**
	 * @see com.mulgasoft.emacsplus.minibuffer.HistoryMinibuffer#getHistoryRing()
	 */
	@Override
	@SuppressWarnings("unchecked")	
	protected RingBuffer<String> getHistoryRing() {
		return SetqRing.ring;
	}
	
	private static class SetqRing {
		static final RingBuffer<String> ring = new RingBuffer<String>();		
	}

}
