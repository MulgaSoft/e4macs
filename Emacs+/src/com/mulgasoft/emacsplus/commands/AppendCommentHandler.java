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
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.ui.texteditor.ITextEditor;

/**
 * @author Mark Feber - initial API and implementation
 */
public class AppendCommentHandler extends EmacsPlusCmdHandler {

	// hack this until we uncover the generic way of determining comment char for editor type
	private final static String COMMENT_MARKER = "//";  					//$NON-NLS-1$
	private final static String LINE_COMMENT = "\t" + COMMENT_MARKER + " "; //$NON-NLS-1$ //$NON-NLS-2$
	private final static String COMMENT_REGEX = COMMENT_MARKER+"\\W+.*";	//$NON-NLS-1$ 
	
	/**
	 * @see com.mulgasoft.emacsplus.commands.EmacsPlusCmdHandler#transform(ITextEditor, IDocument, ITextSelection, ExecutionEvent)
	 */
	@Override
	protected int transform(ITextEditor editor, IDocument document, ITextSelection currentSelection, ExecutionEvent event)
			throws BadLocationException {
		IRegion reg = document.getLineInformationOfOffset(currentSelection.getOffset());
		String lineTxt = document.get(reg.getOffset(),reg.getLength());
		// Check if the line already contains a eol comment
		int pos = lineTxt.lastIndexOf(COMMENT_MARKER);
		if (pos < 0) {
			pos = reg.getOffset() + reg.getLength();
			document.replace(pos, 0, LINE_COMMENT);
			return pos + LINE_COMMENT.length();
		} else {
			// If it already exists, position cursor at the comment
			return pos
					+ COMMENT_MARKER.length()
					+ reg.getOffset()
					+ (lineTxt.substring(pos).matches(COMMENT_REGEX) ? 1 : 0);
		}
	}

}
