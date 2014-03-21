/**
 * Copyright (c) 2009, 2010 Mark Feber, MulgaSoft
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 */
package com.mulgasoft.emacsplus.commands;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.ui.texteditor.ITextEditor;

import com.mulgasoft.emacsplus.EmacsPlusActivator;
import com.mulgasoft.emacsplus.minibuffer.ReadNumberMinibuffer;

/**
 * Implements: goto-char
 * 
 * Move point to specified character position (^U or minibuffer)
 * 
 * @author Mark Feber - initial API and implementation
 */
public class GotoChar extends MinibufferExecHandler implements INonEditingCommand {

	private static String GOTO_CHAR_PREFIX = EmacsPlusActivator.getResourceString("Goto_Char_Prefix");	//$NON-NLS-1$
	private boolean markSet = false;
	
	/**
	 * @see com.mulgasoft.emacsplus.commands.MinibufferHandler#getMinibufferPrefix()
	 */
	public String getMinibufferPrefix() {
		return GOTO_CHAR_PREFIX;
	}

	/**
	 * @see com.mulgasoft.emacsplus.commands.EmacsPlusCmdHandler#isLooping()
	 */
	@Override
	protected boolean isLooping() {
		return false;
	}

	/**
	 * @see com.mulgasoft.emacsplus.commands.EmacsPlusCmdHandler#transform(ITextEditor, IDocument, ITextSelection, ExecutionEvent)
	 */
	@Override
	protected int transform(ITextEditor editor, IDocument document, ITextSelection currentSelection, ExecutionEvent event) 
	throws BadLocationException {
		markSet = this.isMarkEnabled(editor, currentSelection);
		if (isUniversalPresent()) {
			int count = getUniversalCount();
			executeResult(editor,new Integer(count));
			return NO_OFFSET;
		} else {
			return bufferTransform(new ReadNumberMinibuffer(this), editor, event);
		}
	}

	/**
	 * @see com.mulgasoft.emacsplus.commands.MinibufferExecHandler#doExecuteResult(org.eclipse.ui.texteditor.ITextEditor, java.lang.Object)
	 */
	@Override
	protected boolean doExecuteResult(ITextEditor editor, Object minibufferResult) {
		gotoChar(editor, getThisDocument(editor),((Integer)minibufferResult).intValue());
		return true;
	}
	
	private void gotoChar(ITextEditor editor, IDocument document, int charPos) {
		int destPos = document.getLength();
		if (charPos <= destPos) {
			destPos = charPos;
		}
		//org.eclipse.jface.text.TextViewer.validateSelectionRange() takes care of the case
		//where charPos is inside a multi-character line delimiter by moving past delimiter
		if (markSet) {
			// set new point/mark selection when mark enabled
			int mark = getMark(editor);
			selectAndReveal(editor,destPos,mark);
			setFlagMark(mark - destPos == 0);
		} else {
			selectAndReveal(editor, destPos, destPos);
		}
	}
}
