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

import com.mulgasoft.emacsplus.EmacsPlusUtils;
import com.mulgasoft.emacsplus.IEmacsPlusCommandDefinitionIds;

/**
 * Implements: forward-block-of-lines
 * 
 * @author Mark Feber - initial API and implementation
 */
public abstract class BlockHandler extends EmacsPlusNoEditHandler {

	public static final int BLOCK_SIZE = 6;

	private static int blockMovementSize = BLOCK_SIZE;
	
	public static void setBlockSize(int size){
		blockMovementSize = size;
	}
	protected abstract int getDirection();
	
	/**
	 * @see com.mulgasoft.emacsplus.commands.EmacsPlusCmdHandler#transform(ITextEditor, IDocument, ITextSelection, ExecutionEvent)
	 */
	@Override
	protected int transform(ITextEditor editor, IDocument document, ITextSelection currentSelection,
			ExecutionEvent event) throws BadLocationException {

		Control text = getTextWidget(editor);
		String cmd = ((getDirection() == FORWARD) ? IEmacsPlusCommandDefinitionIds.NEXT_LINE : IEmacsPlusCommandDefinitionIds.PREVIOUS_LINE);
		try {
			// use widget to avoid unpleasant scrolling side effects of IRewriteTarget			
			text.setRedraw(false);		
			for (int i=0; i < blockMovementSize; i++) {
				try {
					EmacsPlusUtils.executeCommand(cmd, null, editor);
				} catch (Exception e)  {}
			}
		} finally {
			text.setRedraw(true);
		}
		return NO_OFFSET;
	}
}
