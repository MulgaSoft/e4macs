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
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.texteditor.ITextEditor;


/**
 * @author Mark Feber - initial API and implementation
 */
public abstract class ParagraphMovementHandler extends ParagraphHandler implements INonEditingCommand {
	
	abstract protected int getParagraphOffset(ITextEditor editor, IDocument document, ITextSelection selection); 
	
	protected int transform(ITextEditor editor, IDocument document, ITextSelection currentSelection,
			ExecutionEvent event) throws BadLocationException {
		return doTransform(editor,document,currentSelection,false);
	}

	protected int doTransform(ITextEditor editor, IDocument document, final ITextSelection selection, boolean isMark)
	throws BadLocationException {
		int result = NO_OFFSET;
		Control widget = null;
		try {
			// use widget to avoid unpleasant scrolling side effects of IRewriteTarget
			widget = getTextWidget(editor);
			setRedraw(widget,false);
			result = getParagraphOffset(editor,document,selection);
		} finally {
			if (isMark && selection != null && result != NO_OFFSET) {
				result = selectTransform(editor,result,selection);
			}
			setRedraw(widget,true);
		}
		return result;
	}

}
