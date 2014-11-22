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
import org.eclipse.jface.text.TextSelection;
import org.eclipse.ui.console.IConsoleView;
import org.eclipse.ui.console.TextConsoleViewer;
import org.eclipse.ui.texteditor.ITextEditor;

import com.mulgasoft.emacsplus.EmacsPlusUtils;

/**
 * Implements: backward-up-list
 * 
 * backward-up-list with a generalized bracket set
 *
 * backward-up-list moves backward up past one unmatched opening delimiter.
 * A positive argument serves as a repeat count; a negative argument reverses 
 * the direction of motion, so that the command moves forward and up one or more levels.
 * 
 * @author Mark Feber - initial API and implementation
 */
public class BackwardUpListHandler extends SexpBaseBackwardHandler implements IConsoleDispatch {

	/**
	 * @see com.mulgasoft.emacsplus.commands.EmacsPlusCmdHandler#transform(ITextEditor, IDocument, ITextSelection, ExecutionEvent)
	 */
	@Override
	protected int transform(ITextEditor editor, IDocument document, ITextSelection currentSelection,
			ExecutionEvent event) throws BadLocationException {
		boolean isBackup = getUniversalCount() > 0;  	// normal direction
		int offset = doTransform(document,currentSelection,getCursorOffset(editor,currentSelection),isBackup);
		if (offset < 0) {
			unbalanced(editor,false);
			// trapped by generalized beep() exit
			throw new BadLocationException();
		} else {
			return endTransform(editor,offset,currentSelection,new TextSelection(document,offset,offset - currentSelection.getOffset()));	
		}
	}
	
	/**
	 * @see com.mulgasoft.emacsplus.commands.SexpBaseBackwardHandler#consoleDispatch(TextConsoleViewer, IConsoleView, ExecutionEvent)
	 */
	public Object consoleDispatch(final TextConsoleViewer viewer, final IConsoleView activePart, ExecutionEvent event) {

		IDocument doc = viewer.getDocument();
		boolean isBackup = getUniversalCount() > 0;  	// normal direction
		ITextSelection selection = (ITextSelection) viewer.getSelectionProvider().getSelection();
		try {
			int offset = doTransform(doc, selection, viewer.getTextWidget().getCaretOffset(),isBackup);
			if (offset == NO_OFFSET) {
				unbalanced(activePart,false);
			} else {
				endTransform(viewer, offset, selection, new TextSelection(null,offset,offset - selection.getOffset()));
			}
		} catch (BadLocationException e) {}
		return null;
	}
	
	protected int doTransform(IDocument document, ITextSelection selection, int cursorOffset,boolean isBackup)
	throws BadLocationException {
		return backUpOffset(document,selection,cursorOffset,isBackup);
	}
	
	private int backUpOffset(IDocument document, ITextSelection selection, int cursorOffset, boolean isBackup)
	throws BadLocationException {
		int result = NO_OFFSET;
		int offset = cursorOffset;
		// remember the cursor so we don't go short
		int endList = cursorOffset;
		// but check for start at open bracket
		if (isBackup) {
			if (isOpen(document.getChar(offset))) {
				// remember the end of the current sub-list that started at offset
				IRegion end = getBracketMatch(getThisDocument(),offset+1);
				if (end != null) {
					endList = end.getOffset()+ end.getLength()-1;
				}
				if (--offset < 0) {
					return result;
				}
			}
		} else  {
			IRegion region = document.getLineInformationOfOffset(offset);
			// check for position immediately after a close bracket
			if (region.getOffset() != offset && isClose(document.getChar(offset-1))) {
				if (offset < region.getOffset() + region.getLength()) {
					offset++;
				} else {
					// move over eol
					offset = offset + EmacsPlusUtils.getEol(document).length();
				}
				endList = offset;
			}
		}
		
		TextSelection textSelection = new TextSelection(document,offset,0);
		IRegion match =  matchBracket(document,textSelection,endList);	
		if (match != null) {
			if (isBackup) {
				if (match.getOffset() + match.getLength() >= endList) {
					result = match.getOffset();
				} 
			} else {
				result = match.getOffset() + match.getLength();
			}
		}
		
		return result;
	}
	
	/**
	 * Find the next matching bracket
	 * 
	 * @param document
	 * @param selection
	 * @return The bracketed region or null
	 * @throws BadLocationException
	 */
	private IRegion matchBracket(IDocument document, ITextSelection selection, int endList) throws BadLocationException {
		
		if (selection == null) {
			return null;
		}
		
		int offset = selection.getOffset();
		if (isOpen(document.getChar(offset))) {
			// go to the end and check that it is past the initial ending
			IRegion end = getBracketMatch(document,offset+1);

			if (end == null) {
				return null;
			} else {
				offset = end.getOffset();
				if (offset + end.getLength() < endList) {
					if (--offset > -1) {
						// back up and recurse 
						return matchBracket(document,new TextSelection(document,offset,0),endList);
					} else {
						return null;
					}
				} else {
					return end;
				}
			}
		} else if (offset - 1 > 0 && isClose(document.getChar(offset-1))) {
			// if at the end of a sub-list, jump to the beginning and recurse
			IRegion begin = getBracketMatch(document,offset);
			return matchBracket(document,new TextSelection(document,begin.getOffset(),0),endList);
		} 			
		ITextSelection nextSexp = getNextSexp(document, selection,false,UP);
		if (selection.equals(nextSexp)) { 
			return null;
		}
		// if neither begin or end, look backward for next one and recurse
		return matchBracket(document,nextSexp,endList);
	}

	/**
	 * @see com.mulgasoft.emacsplus.commands.SexpHandler#endTransform(ITextEditor, int, ITextSelection, ITextSelection)
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
	 * @see com.mulgasoft.emacsplus.commands.SexpHandler#endTransform(TextConsoleViewer, int, ITextSelection, ITextSelection)
	 */
	@Override
	protected int endTransform(TextConsoleViewer viewer, int offset,
			ITextSelection origSelection, ITextSelection selection) {
		if (isMarkEnabled(viewer, origSelection)) {
			return selectTransform(viewer, offset, origSelection, selection);
		} else {
			return noSelectTransform(viewer, offset, selection, false);
		}
	}

	/**
	 * Skip everything but brackets
	 * 
	 * @see com.mulgasoft.emacsplus.commands.SexpHandler#skipChar(char, boolean)
	 */
	protected boolean skipChar(char c, boolean wordp) {
		// skip everything but brackets
		return !isBracket(c,OPEN) && !isBracket(c,CLOSE);
	}
}
