/**
 * Copyright (c) 2009, 2010 Mark Feber, MulgaSoft
 *<p>
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *<p>
 */
package com.mulgasoft.emacsplus.commands;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.Position;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.texteditor.ITextEditor;

import com.mulgasoft.emacsplus.MarkUtils;

/**
 * Implements: new-line-indent
 *<p>
 * Insert a new line with possible auto-indent
 *<p>
 * According to org.eclipse.ui.texteditor.InsertLineAction "By operating directly on
 * the text widget, any auto-indent strategies can pick up on the
 * delimiter and perform any content-dependent modifications." such as syntactic auto-indent.
 *<p>
 * @author Mark Feber - initial API and implementation
 */
public class NewLineIndentHandler extends LineHandler {

	/**
	 * @see com.mulgasoft.emacsplus.commands.EmacsPlusCmdHandler#transform(ITextEditor, IDocument, ITextSelection, ExecutionEvent)
	 */
	@Override
	protected int transform(ITextEditor editor, IDocument document, ITextSelection currentSelection, ExecutionEvent event)
	throws BadLocationException {
		int result = NO_OFFSET; 
		Control c = MarkUtils.getTextWidget(editor);
		if (c instanceof StyledText) {
			StyledText st = (StyledText)c;
			String ld = st.getLineDelimiter();
			int insertionOffset = getCursorOffset(editor,currentSelection);
			int widgetInsertionOffset = st.getCaretOffset();
			
			// the offset position will be updated by the internals on insertion/indent
			Position caret= new Position(insertionOffset, 0);
			document.addPosition(caret);
			
			st.setSelectionRange(widgetInsertionOffset, 0);
			// operate directly on the widget
			st.replaceTextRange(widgetInsertionOffset, 0, ld);
			document.removePosition(caret);
			
			if (st.getSelection().x == widgetInsertionOffset) {
				// move cursor by the amount detected
				result = getCursorOffset(editor) + caret.offset - insertionOffset;
			} 
		}
		return result;
	}
}
