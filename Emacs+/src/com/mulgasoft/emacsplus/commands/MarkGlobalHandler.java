/**
 * Copyright (c) 2009, 2010 Mark Feber, MulgaSoft
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package com.mulgasoft.emacsplus.commands;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.part.MultiPageEditorPart;
import org.eclipse.ui.part.MultiPageEditorSite;
import org.eclipse.ui.texteditor.ITextEditor;

import com.mulgasoft.emacsplus.EmacsPlusActivator;
import com.mulgasoft.emacsplus.EmacsPlusUtils;
import com.mulgasoft.emacsplus.IBufferLocation;
import com.mulgasoft.emacsplus.MarkUtils;

/**
 * Common code for pop-global-mark and rotate-global-mark
 *
 * @author Mark Feber - initial API and implementation
 */
public abstract class MarkGlobalHandler extends EmacsPlusNoEditHandler {

	private static String BAD_MARK = EmacsPlusActivator.getResourceString("Bad_Mark");	//$NON-NLS-1$

	/**
	 * Get the next position off the global mark ring and move to that file and location
	 *
	 * @param editor
	 * @param document
	 * @param currentSelection
	 * @param norotate - if true, pop else rotate and pop
	 * @return NO_OFFSET
	 * @throws BadLocationException
	 */
	protected int doTransform(ITextEditor editor, IDocument document, ITextSelection currentSelection, boolean norotate, boolean isTags)
	throws BadLocationException {
		// get editor and offset
		IBufferLocation location = (isTags ? MarkUtils.popTagMark() : MarkUtils.popGlobalMark(norotate));
		if (location != null) {
			if (currentSelection != null &&
					location.getEditor() == editor && location.getOffset() == currentSelection.getOffset()) {
				// if we're already at the global mark location, move to next location
				// recurse with no selection to avoid infinite loop if only one global location
				return doTransform(editor,document,null,norotate, isTags);
			}
			ITextEditor jumpTo = location.getEditor();
			int offset = location.getOffset();
			IWorkbenchPage page = getWorkbenchPage();
			IEditorPart part = jumpTo;
			if (part != null) {
				// move to the correct page
				IEditorPart apart = part;
				IEditorSite esite = part.getEditorSite();
				if (esite instanceof MultiPageEditorSite) {
					apart = ((MultiPageEditorSite)esite).getMultiPageEditor();
					// handle multi page by activating the correct part within the parent
					if (apart instanceof MultiPageEditorPart) {
						((MultiPageEditorPart)apart).setActiveEditor(part);
					}
				}
				// check to make sure the editor is still valid
				if (page.findEditor(apart.getEditorInput()) != null)  {
					// then activate
					page.activate(apart);
					page.bringToTop(apart);
					if (part instanceof ITextEditor) {
						selectAndReveal((ITextEditor) part,offset,offset);
						EmacsPlusUtils.clearMessage(part);
					}
				} else {
					EmacsPlusUtils.showMessage(editor, String.format(BAD_MARK, apart.getTitle()), true);
				}
			}
		} else {
			beep();
		}
		return NO_OFFSET;
	}
}
