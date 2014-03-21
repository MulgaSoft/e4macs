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
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.ui.texteditor.ITextEditor;

import com.mulgasoft.emacsplus.KillRing;

/**
 * Kill the text of the region-rectangle, saving its contents as the 
 * "last killed rectangle" (`kill-rectangle').
 * 
 * @author Mark Feber - initial API and implementation
 */
public class RectangleKillHandler extends RectangleHandler {

	/**
	 * Kill the rectangle and save its contents
	 * 
	 * @see com.mulgasoft.emacsplus.commands.RectangleHandler#doTransform(org.eclipse.ui.texteditor.ITextEditor, org.eclipse.jface.text.IDocument, org.eclipse.jface.text.ITextSelection)
	 */
	protected int doTransform (ITextEditor editor, IDocument document, ITextSelection currentSelection)
	throws BadLocationException {
		int result = NO_OFFSET;
		ITextSelection selection = getImpliedSelection(editor,currentSelection);
		if (selection != null) {
			String[] copy;
			try {
				KillRing.getInstance().setDeactivated(true);
				copy = rs.copyRectangle(editor, document, selection, true);
				setLastRectangle(copy);
			} catch (Exception e) {
			} finally {
				// re-activate kill-ring
				KillRing.getInstance().setDeactivated(false);
			}
		}
		return result;
	}
}
