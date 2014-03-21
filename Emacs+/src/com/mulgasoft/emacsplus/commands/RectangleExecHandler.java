/**
 * Copyright (c) 2009, 2010 Mark Feber, MulgaSoft
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package com.mulgasoft.emacsplus.commands;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRewriteTarget;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.texteditor.ITextEditor;

import com.mulgasoft.emacsplus.EmacsPlusUtils;
import com.mulgasoft.emacsplus.execute.RectangleSupport;
import com.mulgasoft.emacsplus.minibuffer.RectangleMinibuffer;

/**
 * Base class for rectangle commands that use the minibuffer
 * 
 * @author Mark Feber - initial API and implementation
 */
public abstract class RectangleExecHandler extends MinibufferExecHandler {

	private static final String RECTANGLE_NO_MARK= "Rectangle_No_Mark";	//$NON-NLS-1$  

	RectangleSupport rs = null; 
	IDocument document;
	ITextSelection selection = null;
	
	protected abstract boolean isReplace();
	
	/**
	 * @see com.mulgasoft.emacsplus.commands.EmacsPlusCmdHandler#transform(org.eclipse.ui.texteditor.ITextEditor, org.eclipse.jface.text.IDocument, org.eclipse.jface.text.ITextSelection, org.eclipse.core.commands.ExecutionEvent)
	 */
	@Override
	protected int transform(ITextEditor editor, IDocument document, ITextSelection currentSelection,
			ExecutionEvent event) throws BadLocationException {
		int result = NO_OFFSET;
		selection= getImpliedSelection(editor, currentSelection);
		if (selection != null) {
			rs = new RectangleSupport(document, editor);
			this.document = document;
			result = bufferTransform(new RectangleMinibuffer(this), editor, event);
		} else {
			EmacsPlusUtils.showMessage(editor, RECTANGLE_NO_MARK, true);
		}
		return result;
	}

	@Override
	protected boolean doExecuteResult(ITextEditor editor, Object minibufferResult) {
		// use widget to avoid unpleasant scrolling side effects of IRewriteTarget
		Control widget = getTextWidget(editor);
		// wrap in compound change and no redraw
		IRewriteTarget rt = (IRewriteTarget) editor.getAdapter(IRewriteTarget.class);
		try {
			if (rt != null) {
				rt.beginCompoundChange();
			}
			widget.setRedraw(false);
			int offset = rs.updateRectangle(editor, document, selection, (String)minibufferResult, isReplace(),false);
			if (offset > 0) {
				selectAndReveal(editor, offset, offset);
			}
		} finally  {
			if (rt != null) {
				rt.endCompoundChange();
			}
			widget.setRedraw(true);
		}
		return true;
	}

}
