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

import java.text.BreakIterator;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.ui.console.IConsoleView;
import org.eclipse.ui.console.TextConsoleViewer;
import org.eclipse.ui.texteditor.ITextEditor;

/**
 * Implements: forward-sexp. Move forward one s-expression
 *  
 * @author Mark Feber - initial API and implementation
 */
public abstract class SexpBaseForwardHandler extends SexpHandler implements IConsoleDispatch {

	@Override
	protected int getDirection() {
		return FORWARD;
	}
	
	/**
	 * @see com.mulgasoft.emacsplus.commands.EmacsPlusNoEditHandler#transform(ITextEditor, IDocument, ITextSelection, ExecutionEvent)
	 */
	protected int transform(ITextEditor editor, IDocument document, ITextSelection currentSelection, ExecutionEvent event)
	throws BadLocationException {
		ITextSelection selection = null;
		try {
			ITextSelection nSelection = currentSelection;
			if (checkMark(editor, currentSelection.getOffset(),currentSelection.getLength())) {
				// when expanding mark, always start from the cursor
				nSelection = new TextSelection(document, getCursorOffset(editor,currentSelection), 0);
			}
			selection = getNextSexp(document, nSelection);
		} catch (BadLocationException e) {}
		
		if (selection == null) {
			selection = currentSelection;
			if (isUnbalanced()) {
				unbalanced(editor, true);
				throw new BadLocationException();
			}
		}
		return endTransform(editor,selection.getOffset() + selection.getLength(),currentSelection,selection);
	}

	/**
	 * @see com.mulgasoft.emacsplus.commands.IConsoleDispatch#consoleDispatch(org.eclipse.ui.console.TextConsoleViewer, org.eclipse.ui.console.IConsoleView, org.eclipse.core.commands.ExecutionEvent)
	 */
	public Object consoleDispatch(TextConsoleViewer viewer, IConsoleView activePart, ExecutionEvent event) {

		IDocument document = viewer.getDocument();
		ITextSelection currentSelection = (ITextSelection)viewer.getSelectionProvider().getSelection();
		ITextSelection selection = new TextSelection(document, viewer.getTextWidget().getCaretOffset(), 0);
		try {
			selection = getNextSexp(document, selection);
			if (selection == null) {
				selection = currentSelection;
				unbalanced(activePart,true);
				return null;
			} else {
				return endTransform(viewer, selection.getOffset() + selection.getLength(), currentSelection, selection);
			}
		} catch (BadLocationException e) {
		}
		return null;
	}
	
	/**
	 * @see com.mulgasoft.emacsplus.commands.SexpHandler#getNextPosition(org.eclipse.jface.text.IDocument, java.text.BreakIterator, int)
	 */
	@Override
	protected int getNextPosition(IDocument document, BreakIterator iter, int pos) {
		int result = iter.following(pos);
		if (result != BreakIterator.DONE) {
			result = checkDot(document, pos, result);
			result = checkUnder(document, result);
		}
		return result;
	}

	/**
	 * @see com.mulgasoft.emacsplus.commands.SexpHandler#getNextPosition(org.eclipse.jface.text.IDocument, java.text.BreakIterator)
	 */
	@Override
	protected int getNextPosition(IDocument document, BreakIterator iter) {
		int pos = iter.current();
		int result = iter.next();
		if (result != BreakIterator.DONE) {
			result = checkDot(document,pos,result);
			result = checkUnder(document,result);			
		}
		return result;
	}

	/**
	 * @see com.mulgasoft.emacsplus.commands.SexpHandler#isBracket(char)
	 */
	@Override
	protected boolean isBracket(char c){
		return isBracket(c,OPEN);
	}
	
	protected boolean isUnbalanced(char c, int updown) {
		boolean result = false; 
		if (GNU_SEXP && updown != DOWN) {
			result = isBracket(c,CLOSE); 
		}
		setUnbalanced(result);
		return result;
	}
	
	@Override
	protected int getBracketPosition(IDocument document, int pos) {
		int result = -1;
		IRegion reg = getBracketMatch(document, pos);
		if (reg != null){
			result = reg.getOffset() + reg.getLength();
		}
		return result;
	}
	
	
	/**
	 * @see com.mulgasoft.emacsplus.commands.SexpHandler#getTransSexp(org.eclipse.jface.text.IDocument, int, boolean)
	 */
	@Override
	ITextSelection getTransSexp(IDocument document, int pos, boolean wordp)
	throws BadLocationException {
		ITextSelection result = null;
		SexpHandler backsexp = new SexpBackwardHandler();
		
		result = getNextSexp(document, new TextSelection(document,pos,0), wordp);		
		if (result != null) {
			// get full sexp in case currentSelection is in the middle of it
			ITextSelection full = new TextSelection(document, result.getOffset() + result.getLength(), 0); 
			full = backsexp.getNextSexp(document, full, wordp);
			// make sure full starts before result
			if (full != null && full.getOffset() < result.getOffset()) {
				result = full;
			}
			result = new TextSelection(document, result.getOffset(), result.getText().trim().length());
			String text;
			while ((text = result.getText()).length() == 1 && !Character.isJavaIdentifierPart(text.charAt(0))) {
				result = getNextSexp(document, new TextSelection(document, result.getOffset() + 1, 0), wordp);
				result = backsexp.getNextSexp(document, new TextSelection(document, result.getOffset()
						+ result.getLength(), 0), wordp);
				result = new TextSelection(document, result.getOffset(), result.getText().trim().length());
				text = result.getText();
			}
			// transpose around . if it is a break character
			if (isDot() && text.charAt(0) == '.') {
				result = new TextSelection(document, result.getOffset()+1, result.getLength()-1); 
			}
		}
		return result;
	}
}
