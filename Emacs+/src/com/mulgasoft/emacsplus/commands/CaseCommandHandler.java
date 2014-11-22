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
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.ui.texteditor.ITextEditor;

/**
 * Common methods for case conversion
 * They follow the XEmacs convention of using the selection if present, or the next (or previous) word
 * If a selection is present, then ignore any ^U argument
 * 
 * @author Mark Feber - initial API and implementation
 */
public abstract class CaseCommandHandler extends EmacsPlusCmdHandler {

	private boolean forward = true;
	private ITextSelection initialSel = null;
	
	// unicode regex: [\p{L}[\p{Mn}[\p{Pc}[\p{Nd}[\p{Nl}[\p{Sc}]]]]]]+
	// \p{L}  = \p{Letter}: letter from any language
	// \p{Mn} = \p{Non_Spacing_Mark}: a character intended to be combined with another character without taking up extra space (e.g. accents, umlauts, etc.).
	// \p{Pc} = \p{Connector_Punctuation}: a punctuation character such as an underscore that connects words.
	// \p{Nd} = \p{Decimal_Digit_Number}: a digit zero through nine in any script except ideographic scripts.
	// \p{Nl} = \p{Letter_Number}: a number that looks like a letter, such as a Roman numeral.letter from any language
	// \p{Sc} = \p{Currency_Symbol}: any currency sign.
	
	// word characters preceded by (ignored) non-word or BOL/BOB
	private final static String REVERSE_TOKEN_EXP = "(?<=" + END_TOKEN_REGEX+ "|^)" +  TOKEN_REGEX; //$NON-NLS-1$ //$NON-NLS-2$
	private final static String TOKEN_EXP = TOKEN_REGEX + "(?="+ END_TOKEN_REGEX + "|$)";   		//$NON-NLS-1$ //$NON-NLS-2$
	
	
	/**
	 * @see com.mulgasoft.emacsplus.commands.EmacsPlusCmdHandler#execute(org.eclipse.core.commands.ExecutionEvent)
	 */
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		// reset state before execution
		reset();
		return super.execute(event);
	}

	/**
	 * @see com.mulgasoft.emacsplus.commands.EmacsPlusCmdHandler#transform(ITextEditor, IDocument, ITextSelection, ExecutionEvent)
	 */
	@Override
	protected int transform(ITextEditor editor, IDocument document, ITextSelection currentSelection, ExecutionEvent event)
	throws BadLocationException {
		return currentSelection.getOffset() + (forward ? currentSelection.getLength() : 0); 
	}

	/**
	 * For initial, empty selection support cursor embedded in a token on reverse direction as the FDA code doesn't
	 * @throws ExecutionException 
	 *   
	 * @see com.mulgasoft.emacsplus.commands.EmacsPlusCmdHandler#getNewSelection(org.eclipse.jface.text.IDocument, org.eclipse.jface.text.ITextSelection)
	 */
	protected ITextSelection getNewSelection(IDocument document, ITextSelection selection) throws ExecutionException{

		ITextSelection result = selection;
		if (!forward && selection.getLength() == 0 && initialSel != null && initialSel.equals(selection)) {
			// special code to anchor reverse selection at current position (which may be inside full token)
			ITextSelection fore = getNextSelection(document,selection,true,END_TOKEN_REGEX);
			if (fore != null && fore.getOffset() != selection.getOffset() ) {
				// if we're starting from embedded position, search from end and then prune result to initial position
				result = getNextSelection(document, fore, false, REVERSE_TOKEN_EXP);	
				result = new TextSelection(document,result.getOffset(),result.getLength() - (fore.getOffset() - initialSel.getOffset()));
				if (result.equals(selection)) {
					// if we're back where we started, then not embedded so search back one token
					result = getNextSelection(document, selection, false, REVERSE_TOKEN_EXP);	
				}
			} else if (fore == null && (initialSel.getOffset() >= document.getLength())) {
				// if we're starting at document end
				result = getNextSelection(document,new TextSelection(document,selection.getOffset()-1,0),false,REVERSE_TOKEN_EXP);				
			} else {
				// we're starting eow
				result = getNextSelection(document, selection, false, REVERSE_TOKEN_EXP);					
			}
		} else {
			if (!forward && result.getOffset() > 0) {
				// back up over preceding non-word character
				result = new TextSelection(document, result.getOffset()-1, result.getLength());
			}
			result = getNextSelection(document, result, forward, (forward ? TOKEN_EXP : REVERSE_TOKEN_EXP));
		}
		if (result == null)
			// break out of count 
			throw new ExecutionException(null);
		return result;
	}
	
	protected boolean checkSelection(ITextSelection selection) {
		return selection.getLength() > 0;
	}

	/**
	 * Support inverse operation from the normal command
	 * 
	 * @see com.mulgasoft.emacsplus.commands.EmacsPlusCmdHandler#getInverseId(java.lang.String)
	 */
	protected String getInverseId(String id) { 
		// change direction
		forward = false;
		// and use current command
		return null;
	}

	/**
	 * If a region is selected initially, then don't repeat
	 * 
	 * @see com.mulgasoft.emacsplus.commands.EmacsPlusCmdHandler#isLooping()
	 */
	@Override
	protected boolean isLooping() {
		return (isSelection() ? false : super.isLooping());
	}

	private boolean isSelection() {
		return (initialSel != null && initialSel.getLength() > 0);
	}
	
	@Override
	protected void preTransform(ITextEditor editor, ITextSelection selection) {
		initialSel = selection;
		super.preTransform(editor, selection);
	}

	@Override
	protected void postTransform() {
		if (!forward && !isSelection()) {
			// preserve initial cursor position
			setSelection(getThisEditor(), initialSel);
			
		}
		reset();
		super.postTransform();
	}
	
	private void reset() {
		forward = true;
		initialSel = null;		
	}
}
