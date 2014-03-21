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
import com.mulgasoft.emacsplus.MarkUtils;

/**
 * Implement: transpose-lines
 * Exchange current line and previous line, leaving point after both.
 * With argument ARG, takes previous line and moves it past ARG lines.
 * With negative argument ARG, takes previous line and moves it up ARG lines.
 * With argument 0, interchanges line point is in with line mark is in.
 *  
 * Stop using ITextEditorActionDefinitionIds.MOVE_LINES_UP, as it has some unfortunate
 * side-effects in formatting windows
 * 
 * @author Mark Feber - initial API and implementation
 */
public class TransposeLineHandler extends EmacsPlusCmdHandler {

	private final static String NOT_SET = "Mark_Not_Set";	//$NON-NLS-1$	
	
	/**
	 * @see com.mulgasoft.emacsplus.commands.EmacsPlusCmdHandler#transform(ITextEditor, IDocument, ITextSelection, ExecutionEvent)
	 */
	@Override
	protected int transform(ITextEditor editor, IDocument document, ITextSelection currentSelection,
			ExecutionEvent event) throws BadLocationException {
		return ((getUniversalCount() == 0) ? transformAtPointAndMark(editor, document, currentSelection, event) : 
			transformAtPoint(editor, document, currentSelection, event));
	}

	protected int transformAtPoint(ITextEditor editor, IDocument document, ITextSelection currentSelection,
			ExecutionEvent event) throws BadLocationException {
		int result = NO_OFFSET;
		int adj = (getUniversalCount() < 0 ? 1 : 2);	// get line/cursor adjustmnent
		try {
			Lines lines = getLinesAtPoint(editor,document);
			int line = document.getLineOfOffset(lines.getLine1().getOffset());
			swapLines(document,lines);
			// position cursor between swapped lines if -, else after
			result = document.getLineOffset(line + adj);
		} catch (Exception e) {
			throw new BadLocationException();
		}
		return result;
	}
	
	protected int transformAtPointAndMark(ITextEditor editor, IDocument document, ITextSelection currentSelection,
			ExecutionEvent event) throws BadLocationException {
		int result = NO_OFFSET;
		int mark = MarkUtils.getMark(editor);
		if (mark == NO_OFFSET) {
			EmacsPlusUtils.showMessage(editor, NOT_SET, true);			
		} else {
			Position p1 = null, p2 = null, mp = null;
			try {
				Lines lines = getLinesAtPointAndMark(editor,document);
				if (lines.isOk()) {
					p1 = new Position(lines.getLine1().getOffset(), lines.getLine1().getLength());
					p2 = new Position(lines.getLine2().getOffset(), lines.getLine2().getLength());
					mp = new Position(mark);
					document.addPosition(mp);
					document.addPosition(p1);
					document.addPosition(p2);
					swapLines(document, lines);
				}
			} catch (Exception e) {
				throw new BadLocationException();
			} finally {
				if (p1 != null) {
					// position cursor, so that repeat invocation swaps lines back to original positions				
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
	
	private void swapLines(IDocument document, Lines lines) throws BadLocationException {

		if (lines.isOk() && (lines.getLine2().getOffset() < document.getLength())) {
			IRegion line1 = lines.getLine1();
			IRegion line2 = lines.getLine2();
			String line1Text = document.get(line1.getOffset(), line1.getLength());
			String line2Text = document.get(line2.getOffset(), line2.getLength());
			// swap the text from bottom up
			updateText(document, line2.getOffset(), line2.getLength(), line1Text);
			updateText(document, line1.getOffset(), line1.getLength(), line2Text);
		}
	}
	
	private Lines getLinesAtPoint(ITextEditor editor, IDocument document) throws BadLocationException {
		int line = document.getLineOfOffset(getCursorOffset(editor));
		if (getUniversalCount() < 0) {
			--line; 
		} 
		IRegion line2 = document.getLineInformation(line);
		IRegion line1 = document.getLineInformation(--line);
		return new Lines(line1,line2);
	}
	
	private Lines getLinesAtPointAndMark(ITextEditor editor, IDocument document) throws BadLocationException {
		int point = getCursorOffset(editor);
		int mark = MarkUtils.getMark(editor);
		if (mark < point) {
			int tmp = mark;
			mark = point;
			point = tmp;
		}
		return new Lines(document.getLineInformationOfOffset(point),document.getLineInformationOfOffset(mark));
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

	private class Lines {
		IRegion line1 = null;	// the upper line
		IRegion line2 = null;	// the lower line
		public Lines(IRegion line1, IRegion line2) {
			this.line1 = line1;
			this.line2 = line2;
		}
		public IRegion getLine1() {
			return line1;
		}
		
		public IRegion getLine2() {
			return line2;
		}
		public boolean isOk() {
			return line1 != null && line2 != null && !line1.equals(line2);
		}
	}
}
