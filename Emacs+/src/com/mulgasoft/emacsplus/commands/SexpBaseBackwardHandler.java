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
import java.util.regex.Matcher;

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
 * Implements: backward-sexp. Move backward one s-expression
 * 
 * @author Mark Feber - initial API and implementation
 */
public abstract class SexpBaseBackwardHandler extends SexpHandler  implements IConsoleDispatch {
	
	@Override
	protected int getDirection() {
		return BACKWARD;
	}
	
	/**
	 * @see com.mulgasoft.emacsplus.commands.EmacsPlusNoEditHandler#transform(ITextEditor, IDocument, ITextSelection, ExecutionEvent)
	 */
	protected int transform(ITextEditor editor, IDocument document, ITextSelection currentSelection, ExecutionEvent event)
	throws BadLocationException {
		ITextSelection selection = null;
		// in this context, will often generate unbalanced message, so clear first
		EmacsPlusUtils.clearMessage(editor);
		try {
			ITextSelection nSelection = currentSelection;
			if (checkMark(editor, currentSelection.getOffset(),currentSelection.getLength())) {
				// when expanding mark, always start from the cursor
				nSelection = new TextSelection(document, getCursorOffset(editor,currentSelection), 0);
			}
			selection = getNextSexp(document, nSelection);
			if (selection == null && !isUnbalanced()) {
				// move to front
				selection = new TextSelection(document, 0, 0);
			} 

		} catch (BadLocationException e) {
			selection = null;
		}
		if (selection == null){
			unbalanced(editor,true);
			throw new BadLocationException();
		}	
		return endTransform(editor,selection.getOffset(),currentSelection,selection);
	}

	/**
	 * @see com.mulgasoft.emacsplus.commands.IConsoleDispatch#consoleDispatch(TextConsoleViewer, IConsoleView, ExecutionEvent)
	 */
	public Object consoleDispatch(TextConsoleViewer viewer, IConsoleView activePart, ExecutionEvent event) {

		IDocument document = viewer.getDocument();
		ITextSelection currentSelection = (ITextSelection)viewer.getSelectionProvider().getSelection();
		ITextSelection selection = null;
		try {
			selection = getNextSexp(document, currentSelection);
			if (selection == null) {
				selection = currentSelection;
				unbalanced(activePart,true);
				return null;
			} else {
				return endTransform(viewer, selection.getOffset(), currentSelection, selection);
			}
		} catch (BadLocationException e) {
		}
		return null;
	}	

	Matcher getDotMatcher() {
		return dotBackMatcher;
	}

	/**
	 * When moving backwards, modify args to correct for directionality
	 * 
	 * @see com.mulgasoft.emacsplus.commands.SexpHandler#checkDot(org.eclipse.jface.text.IDocument, int, int)
	 */
	int checkDot(IDocument doc, int start, int end) {
		int result = end;
		// reverse position args 
		if (isDot()) {
			int check = super.checkDot(doc, end, start);
			if (check != start) {
				// position after '.'
				result = ++check;
			}
		}
		return result;
	}
	
	Matcher getUnderMatcher() {
		return (isDot() ? underBackMatcher : underDotBackMatcher);
	}
	
	/**
	 * When moving backward, and we're consuming _'s, see if we're currently positioned by an _
	 * 
	 * @param doc
	 * @param iter
	 * @param pos current word position
	 * @return new offset if word moves past any _'s, else pos   
	 */
	int checkUnder(IDocument doc, BreakIterator iter, int pos) {
		int result = pos;
		try {
			if (!isUnder()) {
				char c = doc.getChar(result);
				IRegion lineInfo = doc.getLineInformationOfOffset(pos);
				if (c == '_') {
					// we've backed over an _
					Matcher matcher = getUnderMatcher();
					// get text including the trailing _
					matcher.reset(doc.get(lineInfo.getOffset(), pos+1 - lineInfo.getOffset()));
					if (matcher.find()) {
						result = lineInfo.getOffset() + matcher.start(1);
					}
				} else if (result > lineInfo.getOffset()){
					// check preceding character
					c = doc.getChar(result-1);
					if (c == '_' || (!isDot() && c == '.')) {
						// we've been stopped by a preceding _ or . 
						result = checkUnder(doc,iter,iter.previous());
					}
				}
			}
		} catch (BadLocationException e) {
		}
		return result;
	}
	
	/**
	 * @see com.mulgasoft.emacsplus.commands.SexpHandler#getNextPosition(org.eclipse.jface.text.IDocument, java.text.BreakIterator, int)
	 */
	@Override
	protected int getNextPosition(IDocument document, BreakIterator iter, int pos) {
		int result = iter.preceding(pos);
		if (result != BreakIterator.DONE) {
			result = checkUnder(document,iter,result);
			result = checkDot(document,pos,result);
		}
		return result;
	}

	/**
	 * @see com.mulgasoft.emacsplus.commands.SexpHandler#getNextPosition(org.eclipse.jface.text.IDocument, java.text.BreakIterator)
	 */
	@Override
	protected int getNextPosition(IDocument document, BreakIterator iter) {
		int pos = iter.current();
		int result = iter.previous();
		if (result != BreakIterator.DONE) {
			result = checkUnder(document,iter,result);			
			result = checkDot(document,pos,result);
		}
		return result;
	}

	/**
	 * @see com.mulgasoft.emacsplus.commands.SexpHandler#isBracket(char)
	 */	 
	@Override
	protected boolean isBracket(char c){
		return isBracket(c,CLOSE);
	}

	/**
	 * @see com.mulgasoft.emacsplus.commands.SexpHandler#isUnbalanced(char, int)
	 */
	protected boolean isUnbalanced(char c,int updown) {
		boolean result = false; 
		if (GNU_SEXP && updown != UP) {
			result = isBracket(c,OPEN);	
		}
		setUnbalanced(result);
		return result;
	}

	@Override
	protected int getBracketPosition(IDocument document, int pos) {
		int result = -1;
		IRegion reg = getBracketMatch(document, pos);
		if (reg != null){
			result = reg.getOffset(); 
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
		SexpHandler forsexp = new SexpForwardHandler();
		result = getNextSexp(document, new TextSelection(document, pos, 0), wordp);
		if (result != null) {
			result = new TextSelection(document, result.getOffset(), result.getText().trim().length());
			String text;
			while ((text = result.getText()).length() == 1 && !Character.isJavaIdentifierPart(text.charAt(0))) {
				result = getNextSexp(document, new TextSelection(document, result.getOffset() - 1, 0), wordp);
				result = forsexp.getNextSexp(document, result, wordp);
				result = new TextSelection(document, result.getOffset(), result.getText().trim().length());
				text = result.getText();				
			}
			// transpose around . if it is a break character
			if (isDot() && text.charAt(result.getLength()-1) == '.') {
				result = new TextSelection(document, result.getOffset(), result.getLength()-1); 
			}
		}
		return result;
	}
}
