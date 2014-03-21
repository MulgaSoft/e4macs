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
import org.eclipse.swt.custom.StyledText;
import org.eclipse.ui.texteditor.ITextEditor;

import com.mulgasoft.emacsplus.KillRing;

/**
 * @author Mark Feber - initial API and implementation
 */
public class YankHandler extends BaseYankHandler {

	/**
	 * Implements: yank
	 * 
	 * Add the yanked text to the document and return the new offset
	 *  
	 * `C-y' with an argument restores the text from the specified
	 * kill ring entry, counting back from the most recent as 1.  Thus, `C-u 2
	 * C-y' gets the next-to-the-last block of killed text--it is equivalent
	 * to `C-y M-y'.  `C-y' with a numeric argument starts counting from the
	 * "last yank" pointer, and sets the "last yank" pointer to the entry that
	 * it yanks.
	 * 
	 * @see com.mulgasoft.emacsplus.commands.EmacsPlusCmdHandler#transform(ITextEditor, IDocument, ITextSelection, ExecutionEvent)
	 */
	@Override
	protected int transform(ITextEditor editor, IDocument document, ITextSelection currentSelection, ExecutionEvent event) throws BadLocationException {
		int len = yankIt(document,currentSelection);
		if (len > 0) {
			// set mark after yank, as Eclipse clears it earlier
			setMark(editor, currentSelection.getOffset());
		}
		return currentSelection.getOffset() + len; 
	}

	protected int yankIt(IDocument document, ITextSelection selection) throws BadLocationException {
		int count = getUniversalCount();
		// if count != 1, then will rotate to specified entry
		KillRing.getInstance().rotateYankPos(count);
		String yankText = KillRing.getInstance().yank();
		int len = insertText(document,selection,yankText);
		if (len > 0){
			KillRing.getInstance().setYanked(true);
		}
		return len;
	}

	@Override
	protected void paste(ExecutionEvent event, StyledText widget, boolean isProcess) {
		KillRing kb = KillRing.getInstance();
		// get current yank text (as it may not match clip text)
		String yankText = convertDelimiters(kb.yank(),isProcess);
		if (yankText != null && yankText.length() > 0) {
			String cacheText = kb.getClipboardText();
			try {
				if (!yankText.equals(cacheText)) {
					kb.setClipboardText(yankText);
				} else {
					cacheText = null;
				}
				super.paste(event, widget);
			} finally {
				if (cacheText != null){
					kb.setClipboardText(cacheText);
				}
			}
		}
	}

	/**
	 * @see com.mulgasoft.emacsplus.commands.EmacsPlusCmdHandler#isLooping()
	 */
	protected boolean isLooping() {
		return false;
	}

	protected boolean isZero() {
		// zero ARG means rotate back one and yank
		return true;
	}

}
