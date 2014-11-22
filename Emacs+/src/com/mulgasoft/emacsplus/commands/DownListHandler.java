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
import org.eclipse.jface.text.TextSelection;
import org.eclipse.ui.console.IConsoleView;
import org.eclipse.ui.console.TextConsoleViewer;
import org.eclipse.ui.texteditor.ITextEditor;

/**
 * Implements: down-list
 * 
 * Move forward down one level of brackets. With arg, do this that many times.
 * 
 * @author Mark Feber - initial API and implementation
 */
public class DownListHandler extends SexpForwardHandler implements IConsoleDispatch {

	/**
	 * @see com.mulgasoft.emacsplus.commands.SexpBaseForwardHandler#transform(ITextEditor, IDocument, ITextSelection, ExecutionEvent)
	 */
	@Override
	protected int transform(ITextEditor editor, IDocument document, ITextSelection currentSelection, ExecutionEvent event)
	throws BadLocationException {
		ITextSelection selection = null;
		selection = downList(document, currentSelection);
		if (selection == null) {
			unbalanced(editor,true);
			// trapped by generalized beep() exit
			throw new BadLocationException();
		}
		return endTransform(editor,selection.getOffset() + selection.getLength(), currentSelection, selection);
	}

	/**
	 * @see com.mulgasoft.emacsplus.commands.SexpBaseForwardHandler#consoleDispatch(TextConsoleViewer, IConsoleView, ExecutionEvent)
	 */
	public Object consoleDispatch(final TextConsoleViewer viewer, final IConsoleView activePart, ExecutionEvent event) {

		IDocument doc = viewer.getDocument();
		ITextSelection currentSelection = (ITextSelection) viewer.getSelectionProvider().getSelection();
		ITextSelection selection = downList(doc, currentSelection);
		if (selection == null) {
			unbalanced(activePart,false);
		} else {
			endTransform(viewer,selection.getOffset() + selection.getLength(), currentSelection, selection);
		}
		return null;
	}
	
	/**
	 * Proceed down into next list
	 * 
	 * @param document
	 * @param currentSelection
	 * 
	 * @return selection positioned after next list opening or null if unbalanced
	 */
	private ITextSelection downList(IDocument document, ITextSelection currentSelection) {
		ITextSelection selection = currentSelection;
		try {
			int newOffset = selection.getOffset() + selection.getLength(); 
			do  {
				int oldOff = newOffset;
				selection = new TextSelection(document,newOffset,0);
				selection = getNextSexp(document, selection, false, DOWN);
				if (selection != null) {
					newOffset = selection.getOffset() + selection.getLength();
					if (oldOff == newOffset) {
						// if no movement, then unbalanced
						selection = null;
					}
				}
			}
			while (selection != null && !isDownList(document,selection));
		} catch (BadLocationException e) {
			// mark as unbalanced; transform will re-throw
			selection = null;
		}
		return selection;
	}
	
	/**
	 * Test for sexp open character
	 * 
	 * @param document
	 * @param selection
	 * @return true if we're at a sexp open character
	 * @throws BadLocationException
	 */
	private boolean isDownList(IDocument document, ITextSelection selection) throws BadLocationException {
		return isOpen(document.getChar(selection.getOffset()+selection.getLength()-1));
	}
	

	/**
	 * @see com.mulgasoft.emacsplus.commands.SexpBaseForwardHandler#isBracket(char)
	 * 
	 * Match both open and close, so we can test for sexp end when moving forward
	 */
	protected boolean isBracket(char c) {
		for (int i = 0; i < getSexpBrackets().length; i++) {
			if (c == getSexpBrackets()[i]) {
				return true;
			}
		}
		return false;
	}
	
	/**
	 * @see com.mulgasoft.emacsplus.commands.SexpHandler#endTransform(org.eclipse.ui.texteditor.ITextEditor, int, org.eclipse.jface.text.ITextSelection, org.eclipse.jface.text.ITextSelection)
	 */
	@Override
	protected int endTransform(ITextEditor editor, int offset,
			ITextSelection origSelection, ITextSelection selection) {
		if (isMarkEnabled(editor,origSelection)) {
			return selectTransform(editor,offset,origSelection,selection);
		} else {
			return noSelectTransform(editor,offset,selection,true);	
		}
	}

	/**
	 * @see com.mulgasoft.emacsplus.commands.SexpHandler#endTransform(org.eclipse.ui.console.TextConsoleViewer, int, org.eclipse.jface.text.ITextSelection, org.eclipse.jface.text.ITextSelection)
	 */
	@Override
	protected int endTransform(TextConsoleViewer viewer, int offset,
			ITextSelection origSelection, ITextSelection selection) {
		if (isMarkEnabled(viewer, origSelection)) {
			return selectTransform(viewer, offset, origSelection, selection);
		} else {
			return noSelectTransform(viewer, offset, selection, true);
		}
	}
}
