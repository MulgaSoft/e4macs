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
 * Implements: goto-line
 * 
 * Counts from line 1 at beginning of buffer.  Move point in the current buffer,
 * and leave mark at the previous position. 
 * 
 * If the universal arg is present, use that as the destination line number,
 * else read it from the minibuffer.  
 * 
 * @author Mark Feber - initial API and implementation
 */
public class GotoLine extends MinibufferExecHandler implements INonEditingCommand {

	private static String GOTO_LINE_PREFIX = EmacsPlusActivator.getResourceString("Goto_Line_Prefix");	//$NON-NLS-1$
	private boolean markSet = false;
	
	/**
	 * @see com.mulgasoft.emacsplus.commands.MinibufferHandler#getMinibufferPrefix()
	 */
	public String getMinibufferPrefix() {
		return GOTO_LINE_PREFIX;
	}

	/**
	 * @see com.mulgasoft.emacsplus.commands.EmacsPlusCmdHandler#transform(ITextEditor, IDocument, ITextSelection, ExecutionEvent)
	 */
	@Override
	protected int transform(ITextEditor editor, IDocument document, ITextSelection currentSelection, ExecutionEvent event)
	throws BadLocationException {
		markSet = isMarkEnabled(editor, currentSelection);
		if (isUniversalPresent()) {
			int count = getUniversalCount();
			executeResult(editor,new Integer(count));
			return NO_OFFSET;
		} else {
			return bufferTransform(new ReadNumberMinibuffer(this), editor, event);
		}
	}
	
	/**
	 * @see com.mulgasoft.emacsplus.commands.EmacsPlusCmdHandler#isLooping()
	 */
	@Override
	protected boolean isLooping() {
		return false;
	}

	/**
	 * @see com.mulgasoft.emacsplus.commands.MinibufferExecHandler#doExecuteResult(org.eclipse.ui.texteditor.ITextEditor, java.lang.Object)
	 */
	@Override
	protected boolean doExecuteResult(ITextEditor editor, Object minibufferResult) {
		gotoLine(editor, getThisDocument(editor),((Integer)minibufferResult).intValue());
		return true;
	}

	private void gotoLine(ITextEditor editor, IDocument document, int linePos) {
		int destPos = document.getNumberOfLines();
		// for this command, line count starts at 1, but the document is 0 based
		if (--linePos < 0){
			linePos = 0;
		}
		if (linePos < destPos) {
			destPos = linePos;
		}
		try {
			int offset = document.getLineOffset(destPos);
			if (markSet) {
				// set new point/mark selection when mark enabled
				int mark = getMark(editor);
				selectAndReveal(editor,offset,mark);
				setFlagMark(mark - offset == 0);
			} else {
				// set mark before moving
				setMark(editor);
				selectAndReveal(editor, offset, offset);
			}
		} catch (BadLocationException e) {
			if (linePos > destPos) {
				// some buffers fail at buffer end; recursive call will back it up one
				gotoLine(editor,document,destPos);
			}
		}
	}
}
