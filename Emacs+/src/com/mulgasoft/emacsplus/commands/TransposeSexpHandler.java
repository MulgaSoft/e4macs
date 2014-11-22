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
import org.eclipse.ui.texteditor.ITextEditor;

import com.mulgasoft.emacsplus.EmacsPlusUtils;
import com.mulgasoft.emacsplus.IEmacsPlusCommandDefinitionIds;
import com.mulgasoft.emacsplus.MarkUtils;
import org.eclipse.jface.text.Position;

/**
 * Interchange balanced expressions around point, leaving point at end of them.
 * 
 * @author Mark Feber - initial API and implementation
 */
public class TransposeSexpHandler extends EmacsPlusCmdHandler {

	private final static String UNBALANCED = "Sexp_Unbalanced"; //$NON-NLS-1$ 
	private final static String NOT_SET = "Mark_Not_Set";   	//$NON-NLS-1$ 
	
	private SexpForwardHandler foresexp = new SexpForwardHandler();
	private SexpBackwardHandler backsexp = new SexpBackwardHandler();	

	/**
	 * @see com.mulgasoft.emacsplus.commands.EmacsPlusCmdHandler#transform(ITextEditor, IDocument, ITextSelection, ExecutionEvent)
	 */
	@Override
	protected int transform(ITextEditor editor, IDocument document, ITextSelection currentSelection,
			ExecutionEvent event) throws BadLocationException {
		try {
			return transform(editor,document,currentSelection,event,false);
		} catch (BadLocationException e) {
			EmacsPlusUtils.showMessage(editor, UNBALANCED, true);
			throw e;
		}
	}

	/**
	 * Swap sexps (or words)
	 * 
	 * @param editor
	 * @param document
	 * @param currentSelection
	 * @param event
	 * @return the new offset or -1
	 * @throws BadLocationException
	 */
	protected int transform(ITextEditor editor, IDocument document, ITextSelection currentSelection,
			ExecutionEvent event, boolean wordp) throws BadLocationException {
		int result = NO_OFFSET;
		
		// clear any selection before embarking on the kill as depending on the selection, it's offset may not be the cursor offset
		ITextSelection sexpSelection = MarkUtils.setSelection(editor, getCursorOffset(editor), 0);

		int offset = sexpSelection.getOffset();
		if (getUniversalCount() < 0) {
			try {
				executeCommand(IEmacsPlusCommandDefinitionIds.BACKWARD_SEXP, null, editor);
				ITextSelection csel = getCurrentSelection(editor);
				// make sure the backward movement accomplished something
				if (offset != csel.getOffset()) {
					result = transformAtPoint(editor,document,csel,event,wordp);
					// after transform, we're between the two sexps, so just get the correct offset
					result = getCurrentSelection(editor).getOffset();
				}
			} catch (Exception e) {
				throw new BadLocationException();
			}
		} else if (getUniversalCount() == 0) {
			result = transformAtPointAndMark(editor,document,sexpSelection,event,wordp);
		} else {
			result = transformAtPoint(editor,document,sexpSelection,event,wordp);
		}
		return result;
	}
	
	/**
	 * Swap sexps (or words) around the cursor"
	 *   - sexp1 ^ sexp2 -> sexp2 ^ sexp1 
	 *   - sexp^1 sexp2 -> sexp2 ^ sexp1 
	 *   
	 * @param editor
	 * @param document
	 * @param currentSelection
	 * @param event
	 * @param wordp
	 * @return the offset after the swap
	 * @throws BadLocationException
	 */
	protected int transformAtPoint(ITextEditor editor, IDocument document, ITextSelection currentSelection, ExecutionEvent event, boolean wordp) 
	throws BadLocationException {

		int result = NO_OFFSET;
		Sexps sexps = getSexpsAtPoint(editor,document,currentSelection,wordp);
		if ((result = swapSexps(editor,document,sexps)) == NO_OFFSET) {
			EmacsPlusUtils.showMessage(editor, UNBALANCED, true);
			throw new BadLocationException();
		}
		return result;
	}
	
	/**
	 * Swap sexps between point and mark
 	 * Position cursor at beginning of non-marked sexp 
	 * 
	 * @param editor
	 * @param document
	 * @param currentSelection
	 * @param event
	 * @param wordp
	 * @return the offset after the swap or NO_OFFSET
	 * @throws BadLocationException
	 */
	protected int transformAtPointAndMark(ITextEditor editor, IDocument document, ITextSelection currentSelection, ExecutionEvent event, boolean wordp) 
	throws BadLocationException {
		int result = NO_OFFSET;
		int mark = MarkUtils.getMark(editor);		
		if (mark == NO_OFFSET) {
			asyncShowMessage(editor, NOT_SET, true);
		} else {
			Sexps sexps = getSexpsAtPointAndMark(editor, document, currentSelection, wordp);
			Position p1, p2, mp;
			if (sexps != null) {
				mp = new Position(mark,0);
				p1 = new Position(sexps.getForward().getOffset(), sexps.getForward().getLength());
				p2 = new Position(sexps.getBackward().getOffset(), sexps.getBackward().getLength());
				document.addPosition(p1);
				document.addPosition(p2);
				try {
					if ((result = swapSexps(editor, document, sexps)) == NO_OFFSET) {
						asyncShowMessage(editor, UNBALANCED, true);
						throw new BadLocationException();
					}
				} finally {
					// position cursor, so that repeat invocation swaps sexps back to original positions				
					mark = mp.getOffset();
					if (MarkUtils.getMark(editor) == NO_OFFSET) {	// Eclipse may have removed the mark
						MarkUtils.setMark(editor, mark);
					}
					if (mark >= p1.getOffset() && mark <= p1.getOffset() + p1.getLength()) {
						result = p2.getOffset();
					} else {
						result = p1.getOffset();
					}
					document.removePosition(mp);
					document.removePosition(p1);
					document.removePosition(p2);
				}
			}
		}
		return result;
	}

	/**
	 * Get the previous and following sexps around point
	 * @param editor
	 * @param document
	 * @param selection
	 * @param wordp
	 * @return A Sexps structure containing the two sexps as ITextSelections
	 * 
	 * @throws BadLocationException
	 */
	private Sexps getSexpsAtPoint(ITextEditor editor, IDocument document, ITextSelection selection, boolean wordp) throws BadLocationException {
		ITextSelection forward =foresexp.getTransSexp(document, selection.getOffset(), wordp);
		ITextSelection backward = null;
		if (forward != null) {
			// check if cursor was inside sexp1
			if (forward.getOffset() < selection.getOffset()) {
				backward = foresexp.getTransSexp(document, forward.getOffset() + forward.getLength(), wordp);
				ITextSelection tmp = forward;
				forward = backward;
				backward = tmp;
			} else {
				backward = backsexp.getTransSexp(document, forward.getOffset(), wordp);
			}
		}
		return new Sexps(forward,backward);
	}
	
		/**
	 * Get the previous and following sexps around point
	 * @param editor
	 * @param document
	 * @param selection
	 * @param wordp
	 * @return A Sexps structure containing the two sexps as ITextSelections
	 * 
	 * @throws BadLocationException
	 */
	private Sexps getSexpsAtPointAndMark(ITextEditor editor, IDocument document, ITextSelection selection, boolean wordp) 
	throws BadLocationException {

		Sexps result = null;
		int fore = MarkUtils.getMark(editor);
		int back = getCursorOffset(editor);
		if (fore < back) {
			int tmp = back;
			back = fore;
			fore = tmp;
		}
		ITextSelection forward = foresexp.getTransSexp(document, fore, wordp);
		ITextSelection backward = foresexp.getTransSexp(document, back, wordp);;
		if (forward != null && backward != null) { 
			result = new Sexps(forward,backward);; 
			if (result.intersectP()) {
				result = null;
			}
		}
		return result;
	}

	private int swapSexps(ITextEditor editor, IDocument document, Sexps sexps) throws BadLocationException {
		int result = NO_OFFSET;
		ITextSelection forward = sexps.getForward();
		ITextSelection backward = sexps.getBackward();
		if (forward != null && backward != null && !sexps.intersectP()) {
			result = forward.getOffset() + forward.getLength();
			String sexp1text = backward.getText();
			String sexp2text = forward.getText();
			// swap the text from bottom up
			updateText(document,forward, sexp1text);
			updateText(document,backward, sexp2text);
		}
		return result;
	}

	/**
	 * @see com.mulgasoft.emacsplus.commands.EmacsPlusCmdHandler#isZero()
	 */
	protected boolean isZero() {
		return true;
	}

	/**
	 * Force undo protect
	 * @see com.mulgasoft.emacsplus.commands.EmacsPlusCmdHandler#undoProtect()
	 */
	protected boolean undoProtect() {
		return true;
	}
	
	private class Sexps {
		ITextSelection forward;  // the sexp following point
		ITextSelection backward; // the sexp preceding point
		public Sexps (ITextSelection forward, ITextSelection backward){
			this.forward = forward;
			this.backward = backward;
		}
		public ITextSelection getForward() {
			return forward;
		}
		public ITextSelection getBackward() {
			return backward;
		}
		
		public boolean equals(Object o) {
			boolean result = o == this;
			if (!result && o instanceof Sexps) {
				Sexps sexp = (Sexps)o;
				result = forward.getOffset() == sexp.getForward().getOffset() && 
				forward.getLength() == sexp.getForward().getLength() && 
				backward.getOffset() == sexp.getBackward().getOffset() && 
				backward.getLength() == sexp.getBackward().getLength();
			}
			return result;
		}
		
		public int hashCode() {
			int result = 17;
			if (forward != null) {
				result = 31 * result + forward.getOffset();
				result = 31 * result + forward.getLength();
			}
			if (backward != null) {
				result = 31 * result + backward.getOffset();
				result = 31 * result + backward.getLength();
			}
			return result;
		}
		
		public boolean intersectP() {
			boolean result = false;
			if (forward != null && backward != null) {
				int abegin = forward.getOffset();
				int aend = abegin + forward.getLength();
				int bbegin = backward.getOffset();
				int bend = bbegin + backward.getLength();
				
				result = ((abegin >= bbegin) && (abegin < bend)) || 
				((bbegin >= abegin) && (bbegin <= aend)) || 
				(forward.getOffset() == backward.getOffset() && forward.getLength() == backward.getLength()) ;
			}
			return result;
		}
	}
}
