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

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.ui.texteditor.ITextEditor;

/**
 * Capitalize word/selection
 * 
 * @author Mark Feber - initial API and implementation
 */
public class CapitalizeCaseHandler extends CaseCommandHandler {

	/* (non-Javadoc)
	 * @see com.mulgasoft.emacsplus.commands.AbstractCmdHandler#transform(org.eclipse.jface.text.IDocument, org.eclipse.jface.text.ITextSelection)
	 */
	@Override
	protected int transform(ITextEditor editor, IDocument document, ITextSelection currentSelection,
			ExecutionEvent event) throws BadLocationException {
		
		ITextSelection forward = currentSelection;
		SexpForwardHandler forsexp = new SexpForwardHandler();
		
		// maximum position in the region
		int maxOff = currentSelection.getOffset()+ currentSelection.getLength();
		// offset in the region string
		int stringOff = 0;
		// initialize with region text
		StringBuilder text = new StringBuilder(currentSelection.getText());
		
		// iterate through all the words in the region
		while (forward != null && forward.getOffset() < maxOff) {
			forward = forsexp.getTransSexp(document, forward.getOffset(), true);
			if (forward != null) {
				// the initial sexp may have looked backward to determine extent
				if (forward.getOffset() < currentSelection.getOffset()) {
					forward = new TextSelection(document, currentSelection.getOffset(), forward.getLength()
							- (currentSelection.getOffset() - forward.getOffset()));
				}
				// don't go past the region extent
				if (forward.getOffset() > maxOff) {
					break;
				}
				// limit any case change to extent
				if (forward.getOffset() + forward.getLength() > maxOff) {
					forward = new TextSelection(document, forward.getOffset(), forward.getLength()
							- (forward.getOffset() + forward.getLength() - maxOff));
				}
				// now update the string buffer
				stringOff = forward.getOffset() - currentSelection.getOffset();
				text.replace(stringOff, stringOff + forward.getLength(), forward.getText().substring(0, 1)
						.toUpperCase()
						+ forward.getText().substring(1).toLowerCase());
				forward = new TextSelection(document, forward.getOffset() + forward.getLength(), 0);
			}
		}
 		updateText(document, currentSelection, text.toString());
		return super.transform(editor, document, currentSelection, event);
	}
}
