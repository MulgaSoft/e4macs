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
import org.eclipse.jface.text.Position;
import org.eclipse.ui.texteditor.ITextEditor;

import com.mulgasoft.emacsplus.EmacsPlusUtils;
import com.mulgasoft.emacsplus.IEmacsPlusCommandDefinitionIds;
import com.mulgasoft.emacsplus.MarkUtils;

/**
 * Implements: transpose-paragraphs
 * 
 * Interchange the current paragraph with the next one.
 * With prefix argument ARG a non-zero integer, moves the current
 * paragraph past ARG paragraphs, leaving point after the current paragraph.
 * If ARG is positive, moves the current paragraph forwards
 * If ARG is negative moves it backwards.  
 * If ARG is zero, exchanges the current paragraph with the one containing the mark.
 * 
 * @author Mark Feber - initial API and implementation
 */
public class ParagraphTransposeHandler extends ParagraphHandler {

	private final static String NOT_SET = "Mark_Not_Set";	//$NON-NLS-1$
	private ParagraphForwardHandler ff = new ParagraphForwardHandler(); 
	private ParagraphBackwardHandler bb = new ParagraphBackwardHandler(); 

	/**
	 * @see com.mulgasoft.emacsplus.commands.EmacsPlusCmdHandler#transform(ITextEditor, IDocument, ITextSelection, ExecutionEvent)
	 * 
	 * @param editor
	 * @param document
	 * @param currentSelection
	 * @param event
	 * @return the new offset or -1
	 * @throws BadLocationException
	 */
	protected int transform(ITextEditor editor, IDocument document, ITextSelection currentSelection, ExecutionEvent event)
	throws BadLocationException {
		int result = NO_OFFSET;
		if (getUniversalCount() < 0) {
			try {
				executeCommand(IEmacsPlusCommandDefinitionIds.BACKWARD_PARAGRAPH, null, editor);
				transformAtPoint(editor,document,currentSelection,event);
				// after transform, we're between the two paragraphs, so just move to the front of the line
				result = document.getLineInformationOfOffset(getCurrentSelection(editor).getOffset()).getOffset();
			} catch (Exception e) {
				throw new BadLocationException();
			}
		} else if (getUniversalCount() == 0) {
			result = transformAtPointAndMark(editor,document,currentSelection,event);
		} else {
			result = transformAtPoint(editor,document,currentSelection,event);
		}
		return result;
	}

	/**
 	 * Swap paragraphs around the cursor"
 	 *   case 1: paragraph1 ^ paragraph2 -> paragraph2 ^ paragraph1 
 	 *   case 2: paragraph^1 paragraph2 -> paragraph2 ^ paragraph1 
 	 * 
	 * @param editor
	 * @param document
	 * @param currentSelection
	 * @param event
	 * @return the new offset after the swap
	 * @throws BadLocationException
	 */
	protected int transformAtPoint(ITextEditor editor, IDocument document, ITextSelection currentSelection, ExecutionEvent event) 
	throws BadLocationException {

		int offset = getCursorOffset(editor);
		
		Paragraph para1 = new Paragraph(-1,-1);
		Paragraph para2 = new Paragraph(-1,-1);
		getParagraphsAtPoint(editor, document, currentSelection, offset, para1, para2);
		int result = swapParagraphs(document, para1, para2);
		if (result == -1) {
			// restore previous location
			setCursorOffset(editor, offset);
			throw new BadLocationException();
		}
		return result;
	}
	
	/**
	 * Swap paragraphs between point and mark
	 * Position cursor at beginning of non-marked paragraph 
	 * 
	 * @param editor
	 * @param document
	 * @param currentSelection
	 * @param event
	 * @return the new offset after the swap or NO_OFFSET
	 * @throws BadLocationException
	 */
	protected int transformAtPointAndMark(ITextEditor editor, IDocument document, ITextSelection currentSelection,
			ExecutionEvent event) throws BadLocationException {
		int result = NO_OFFSET;
		int point = getCursorOffset(editor);
		int mark = MarkUtils.getMark(editor);
		if (mark == NO_OFFSET) {
			EmacsPlusUtils.showMessage(editor, NOT_SET, true);			
		} else {
			Paragraph para1 = new Paragraph(-1, -1);
			Paragraph para2 = new Paragraph(-1, -1);
			getParagraphsAtPointAndMark(editor, document, currentSelection, point, para1, para2);
			Position p1 = null, p2 = null, mp = null;
			try {
				if (para1.isOk() && para2.isOk()) {
					p1 = new Position(para1.getBegin(), para1.getLength());
					p2 = new Position(para2.getBegin(), para2.getLength());
					mp = new Position(mark);
					document.addPosition(mp);
					document.addPosition(p1);
					document.addPosition(p2);
					result = swapParagraphs(document, para1, para2);
				}
				if (result == NO_OFFSET) {
					// restore previous location
					setCursorOffset(editor, point);
					throw new BadLocationException();
				}
			} finally {
				if (p1 != null) {
					// position cursor, so that repeat invocation swaps paragraphs back to original positions				
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
	
	private int swapParagraphs(IDocument document, Paragraph para1, Paragraph para2) throws BadLocationException {
		int result = -1;
		if (para1.isOk() && para2.isOk()) {
			// we're cool, so swap
			// check that we're not at a non-blank line (top) before adjusting line 
			int bline = document.getLineOfOffset(para1.getBegin());
			if (isBlank(document,bline)) {
				++bline;
			}
			// check that we're not at a non-blank line (bottom) before adjusting line 
			int eline = document.getLineOfOffset(para2.getEnd());
			if (isBlank(document,eline)) {
				--eline;
			}
			// get the text for paragraph 1
			IRegion line1Begin = document.getLineInformation(bline); 
			IRegion lineEnd = document.getLineInformation(document.getLineOfOffset(para1.getEnd()) -1);
			int para1len = lineEnd.getOffset() + lineEnd.getLength() - line1Begin.getOffset(); 
			String para1text = document.get(line1Begin.getOffset(), para1len);
			// get the text for paragraph 2
			IRegion line2Begin = document.getLineInformation(document.getLineOfOffset(para2.getBegin()) + 1); 
			lineEnd = document.getLineInformation(eline);
			int para2len = lineEnd.getOffset() + lineEnd.getLength() - line2Begin.getOffset(); 
			String para2text = document.get(line2Begin.getOffset(), para2len);
			// swap the text from bottom up
			updateText(document,line2Begin.getOffset(), para2len, para1text);
			updateText(document,line1Begin.getOffset(), para1len, para2text);
			// cursor goes below swapped paragraphs
			result = para2.getEnd();
		}		
		return result;
	}
	
	private void getParagraphsAtPoint(ITextEditor editor, IDocument document, ITextSelection selection, int point, Paragraph para1, Paragraph para2) {
		
		int end= ff.getParagraphOffset(editor, document, selection);
		// update selection - move to beginning of selection/line
		setSelection(editor, end, 0);
		ITextSelection csel = getCurrentSelection(editor);
		int begin = bb.getParagraphOffset(editor, document, csel);
		
		if (begin < point) {
			// in case 2
			if (end != -1 && begin != -1) {
				// then we started in mid paragraph
				para1.setBegin(begin);
				para1.setEnd(end);
				// update selection - move to end of first paragraph
				setSelection(editor, para1.getEnd(), 0);
				csel = getCurrentSelection(editor);
				para2.setEnd(ff.getParagraphOffset(editor, document, csel));
				csel = getCurrentSelection(editor);
				// update selection - move to beginning of selection/line
				setSelection(editor, csel.getOffset(), 0);
				para2.setBegin(bb.getParagraphOffset(editor, document, csel));
			}
		} else if (end != -1) {
			// in case 1
			para2.setBegin(begin);
			para2.setEnd(end);
			csel = getCurrentSelection(editor);
			para1.setBegin(bb.getParagraphOffset(editor, document, csel));
			csel = getCurrentSelection(editor);
			para1.setEnd(ff.getParagraphOffset(editor, document, csel));
		}
	}
	
	private void getParagraphsAtPointAndMark(ITextEditor editor, IDocument document, ITextSelection selection, int point, Paragraph para1, Paragraph para2) {
		
		int mark = MarkUtils.getMark(editor);
		ITextSelection csel = selection;
		if (mark != point) {
			// always start at top paragraph 
			if (mark < point) {
				setSelection(editor,mark,0);
				csel = getCurrentSelection(editor);
				mark = point;
			}
			
			para1.setEnd(ff.getParagraphOffset(editor, document, csel));
			// update selection - move to beginning of selection/line
			setSelection(editor, para1.getEnd(), 0);
			csel = getCurrentSelection(editor);
			para1.setBegin(bb.getParagraphOffset(editor, document, csel));

			setSelection(editor,mark,0);			
			csel = getCurrentSelection(editor);
			para2.setEnd(ff.getParagraphOffset(editor, document, csel));
			// update selection - move to beginning of selection/line
			setSelection(editor, para2.getEnd(), 0);
			csel = getCurrentSelection(editor);
			para2.setBegin(bb.getParagraphOffset(editor, document, csel));
			
			if (para1.equals(para2)) {
				// disable transpose on identity
				para1.setBegin(-1);
			}
		}
	}
	
	/**
	 * Force undo protect
	 * @see com.mulgasoft.emacsplus.commands.EmacsPlusCmdHandler#undoProtect()
	 */
	protected boolean undoProtect() {
		return true;
	}
	
	/**
	 * @see com.mulgasoft.emacsplus.commands.EmacsPlusCmdHandler#isZero()
	 */
	protected boolean isZero() {
		return true;
	}

	private class Paragraph {

		/** The paragraph begin offset */
		private int begin;
		/** The paragraph end offset */
		private int end;
		
		/**
		 * Create a paragraph
		 *
		 * @param begin
		 * @param end
		 */
		public Paragraph(int begin, int end) {
			this.begin = begin;
			this.end = end;
		}

		public int getBegin() {
			return begin;
		}
		private void setBegin(int begin) {
			this.begin = begin;
		}
		
		public int getEnd() {
			return end;
		}
		private void setEnd(int end) {
			this.end = end;
		}		

		public int getLength() {
			return (isOk() ? end - begin : -1);
		}
		
		public boolean isOk() {
			return begin != -1 && end != -1;
		}
		
		/**
		 * @see java.lang.Object#equals(java.lang.Object)
		 */
		public boolean equals(Object o) {
			boolean result = o == this;
			if (!result && o instanceof Paragraph) {
				result = ((Paragraph)o).getBegin() == begin && ((Paragraph)o).getEnd() == end;
			}
			return result;
		}

		/**
		 * @see java.lang.Object#hashCode()
		 */
		public int hashCode() {
			int result = 17;
			result = 31 * result + begin;
			result = 31 * result + end;
			return result;
		}
		
		/**
		 * @see java.lang.Object#toString()
		 */
		public String toString() {
			return "begin=" + begin + ", end=" + end; //$NON-NLS-1$ //$NON-NLS-2$;
		}
	}

}
