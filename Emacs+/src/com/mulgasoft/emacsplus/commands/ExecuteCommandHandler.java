/**
 * Copyright (c) 2009, 2010 Mark Feber, MulgaSoft
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package com.mulgasoft.emacsplus.commands;

import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.IPostSelectionProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.ui.texteditor.ITextEditor;

/**
 * Abstract class to provide required selection changed behavior for commands that invoke other commands
 * 
 * @author Mark Feber - initial API and implementation
 */
public abstract class ExecuteCommandHandler extends MinibufferExecHandler {

	/* Hack Alert: The JavaEditor (maybe others) adds a post selection listener that clears the status 
	 * message (bogosity).  Since we want the key binding help (or other message) to stick around, we have
	 * to add a temporary selection changed listener to restore the message if it gets removed.
	 * 
	 * In some cases, JavaEditor.selectionChanged (which clears the status line) is called directly instead 
	 * of from iterating through listeners (e.g org.eclipse.jdt.internal.ui.javaeditor.reconciled) 
	 * (even more bogosity) which means the appropriate message won't always redisplay correctly and
	 * there is nothing obvious we can do about it. 
	 */

	/**
	 * Call execute on checkers with an additional listener for selection change during execution
	 *  
	 * @param editor
	 * @param checkers
	 */
	protected void executeWithSelectionCheck(final ITextEditor editor, IWithSelectionCheck checkers) {

		ISelectionChangedListener listener = new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				ExecuteCommandHandler.this.showResultMessage(editor);
				removeListener(event.getSelectionProvider(),this);
			}
		};
		ITextSelection before = getCurrentSelection(editor);
		try {
			addListener(editor.getSelectionProvider(),listener);
			checkers.execute();
		} finally {
			if (before.equals(getCurrentSelection(editor))) {
				// remove it if the selection did not change
				removeListener(editor.getSelectionProvider(),listener);
			}
		}
	}

	/**
	 * Add the selection changed listener
	 *  
	 * @param selectionProvider
	 */
	private void addListener(ISelectionProvider selectionProvider, ISelectionChangedListener listener) {
		if (selectionProvider instanceof IPostSelectionProvider)  {
			IPostSelectionProvider provider= (IPostSelectionProvider) selectionProvider;
			provider.addPostSelectionChangedListener(listener);
		} else  {
			selectionProvider.addSelectionChangedListener(listener);
		}
	}
	
	/**
	 * Remove the selection changed listener
	 * 
	 * @param selectionProvider
	 */
	private void removeListener(ISelectionProvider selectionProvider, ISelectionChangedListener listener) {
		if (selectionProvider instanceof IPostSelectionProvider)  {
			IPostSelectionProvider provider= (IPostSelectionProvider) selectionProvider;
			provider.removePostSelectionChangedListener(listener);
		} else  {
			selectionProvider.removeSelectionChangedListener(listener);
		}
	}
	
	/**
	 * Work around issue with Eclipse where a selection change listener is required to redisplay
	 * a cleared message if a selection changed occurred during execution
	 * 
	 * @author Mark Feber - initial API and implementation
	 */
	protected interface IWithSelectionCheck {
		void execute();
	}

}
