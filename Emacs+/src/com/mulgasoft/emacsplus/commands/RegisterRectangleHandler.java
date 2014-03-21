/**
 * Copyright (c) 2009, Mark Feber, MulgaSoft
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
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.texteditor.ITextEditor;

import com.mulgasoft.emacsplus.TecoRegister;
import com.mulgasoft.emacsplus.execute.RectangleSupport;

/**
 * Implement - copy-rectangle-to-register
 * 
 * Copy rectangular region into specified register.
 * With prefix argument, delete as well.
 * 
 * @author Mark Feber - initial API and implementation
 */
public class RegisterRectangleHandler extends RegisterHandler implements INonEditingCommand {

	/**
	 * @see com.mulgasoft.emacsplus.minibuffer.IMinibufferExecutable#getMinibufferPrefix()
	 */
	public String getMinibufferPrefix() {
		return COPY_PREFIX;
	}

	/**
	 * @see com.mulgasoft.emacsplus.minibuffer.IMinibufferExecutable#executeResult(org.eclipse.ui.texteditor.ITextEditor, java.lang.Object)
	 */
	public boolean doExecuteResult(ITextEditor editor, Object minibufferResult) {
		
		if (minibufferResult != null && ((String)minibufferResult).length() > 0) {
			String key = (String)minibufferResult;
			ITextSelection selection = getImpliedSelection(editor, getCurrentSelection(editor));
			IDocument document = getThisDocument(editor);
			// if called with ^U, then delete text as well
			boolean delete = isEditable() && getCallCount() > 1;
			String[] rect;
			// use widget to avoid unpleasant scrolling side effects of IRewriteTarget			
			Control widget = getTextWidget(editor);
			IRewriteTarget rt = (IRewriteTarget) editor.getAdapter(IRewriteTarget.class);
			try {
				if (delete) {
					// wrap in compound change and no redraw
					widget.setRedraw(false);
					if (rt != null) {
						rt.beginCompoundChange();
					}
				}
				rect = new RectangleSupport(document,editor).copyRectangle(editor, document, selection, delete);
				if (rect != null && rect.length > 0) {
					TecoRegister.getInstance().put(key,rect);
					showResultMessage(editor, String.format(COPIED, key), false);
				}
			} catch (BadLocationException e) {
				showResultMessage(editor, BAD_INSERT_LOCATION, true);
			} finally  {
				if (rt != null) {
					rt.endCompoundChange();
				}
				widget.setRedraw(true);
			}
		} else {
			showResultMessage(editor, NO_REGISTER, true);
		}
		return true;
	}
}

