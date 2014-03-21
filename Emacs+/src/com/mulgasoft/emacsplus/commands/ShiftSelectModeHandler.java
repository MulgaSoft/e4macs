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
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.ui.texteditor.ITextEditor;

import com.mulgasoft.emacsplus.EmacsPlusActivator;

/**
 * Implements: shift-select-mode
 *  
 * Toggle value on command; show value without change when ARG present
 * The variable `ShiftSelectMode' determines whether GNU-style shift-selection
 * is enabled
 *
 * From the GNU manual:
 * If you hold down the SHIFT key while typing a cursor motion command,
 * this sets the mark before moving point, so that the region extends from
 * the original position of point to its new position.  This feature,
 * newly introduced in Emacs 23, is referred to as "shift-selection".  It
 * is similar to the way text is selected in other editors.
 * 
 *    The mark set via shift-selection behaves a little differently from
 * what we have described above.  Firstly, in addition to the usual ways
 * of deactivating the mark (such as changing the buffer text or typing
 * `C-g'), the mark is deactivated by any UNSHIFTED cursor motion
 * command.  Secondly, any subsequent SHIFTED cursor motion command
 * avoids setting the mark anew.  Therefore, a series of shifted cursor
 * motion commands will continuously extend the region.
 * 
 *    Shift-selection only works if the shifted cursor motion key is not
 * already bound to a separate command
 * 
 * @author Mark Feber - initial API and implementation
 *
 */
public class ShiftSelectModeHandler extends EmacsMovementHandler {

	private final static String SHIFT_SELECTION = EmacsPlusActivator.getResourceString("Shift_Selection"); //$NON-NLS-1$
	
	/**
	 * @see com.mulgasoft.emacsplus.commands.EmacsPlusCmdHandler#transform(ITextEditor, IDocument, ITextSelection, ExecutionEvent)
	 */
	@Override
	protected int transform(ITextEditor editor, IDocument document, ITextSelection currentSelection, ExecutionEvent event)
	throws BadLocationException {
		if (!isUniversalPresent()) {
			setShiftMode(!isShiftMode());
		}
		asyncShowMessage(editor,String.format(SHIFT_SELECTION,isShiftMode()),false);
		return NO_OFFSET;
	}

	/**
	 * @see com.mulgasoft.emacsplus.commands.EmacsPlusCmdHandler#isLooping()
	 */
	@Override
	protected boolean isLooping() {
		return false;
	}
	
}
