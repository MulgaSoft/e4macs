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

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRewriteTarget;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.texteditor.ITextEditor;

import com.mulgasoft.emacsplus.TecoRegister;
import com.mulgasoft.emacsplus.execute.RectangleSupport;

/**
 * Implement - insert-register
 * 
 * Insert register text at cursor
 *
 * @author Mark Feber - initial API and implementation
 */
public class RegisterInsertHandler extends RegisterHandler {
	
	/**
	 * @see com.mulgasoft.emacsplus.minibuffer.IMinibufferExecutable#getMinibufferPrefix()
	 */
	public String getMinibufferPrefix() {
		return INSERT_PREFIX;
	}
	
	/**
	 * @see com.mulgasoft.emacsplus.commands.RegisterHandler#needsSelection()
	 */
	protected boolean needsSelection() {
		return false;
	}

	/**
	 * Insert register text at cursor
	 * Remove any selection and place cursor at beginning (or end if ^U) of inserted text
	 * 
	 * 
	 * @see com.mulgasoft.emacsplus.minibuffer.IMinibufferExecutable#executeResult(org.eclipse.ui.texteditor.ITextEditor, java.lang.Object)
	 */
	public boolean doExecuteResult(ITextEditor editor, Object minibufferResult) {
		
		if (minibufferResult != null) {
			int offset = getCursorOffset(editor);
			IDocument document = getThisDocument(editor);
			String key = (String)minibufferResult;
			String text = null;
			String[] rectangle = null;
			text = TecoRegister.getInstance().getText(key);
			if (text != null) {
				try {
					text = convertDelimiters(text);
					// insert text at current cursor
					updateText(document,offset, 0, text);
					if (getCallCount() > 1) {
						// if called with ^U, then move cursor to end of inserted text
						offset += text.length();
					}

					showResultMessage(editor, String.format(INSERTED, key), false);
				} catch (BadLocationException e) {
					showResultMessage(editor, BAD_INSERT_LOCATION, true);
				}					
			} else if ((rectangle = TecoRegister.getInstance().getRectangle(key)) != null) {
				// use widget to avoid unpleasant scrolling side effects of IRewriteTarget
				Control widget = getTextWidget(editor);
				// wrap in compound change and no redraw
				IRewriteTarget rt = (IRewriteTarget) editor.getAdapter(IRewriteTarget.class);
				try {
					widget.setRedraw(false);
					if (rt != null) {
						rt.beginCompoundChange();
					}
					int last = new RectangleSupport(document,editor).insertRectangle(editor, document, rectangle);
					if (getCallCount() > 1) {
						// if called with ^U, then move cursor to end of inserted text
						offset = last;
					}
					showResultMessage(editor, String.format(INSERTED, key), false);
				} catch (BadLocationException e) {
					showResultMessage(editor, BAD_INSERT_LOCATION, true);
				} finally  {
					if (rt != null) {
						rt.endCompoundChange();
					}
					widget.setRedraw(true);
				}
			} else {
				showResultMessage(editor, String.format(NO_TEXT,key), true);
			}
			selectAndReveal(editor, offset, offset);
		} else {
			showResultMessage(editor, NO_REGISTER, true);
		}
		return true;
	}
}
