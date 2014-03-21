/**
 * Copyright (c) 2009, 2010 Mark Feber, MulgaSoft
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package com.mulgasoft.emacsplus.execute;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.texteditor.ITextEditor;

import com.mulgasoft.emacsplus.EmacsPlusUtils;
import com.mulgasoft.emacsplus.MarkUtils;

/**
 * Assorted methods for manipulating rectangles from regular Eclipse selections
 *  
 * @author Mark Feber - initial API and implementation
 */
public class RectangleSupport extends ColumnSupport {

	public RectangleSupport(IDocument document, Control widget) {
		super(document,widget);
	}
	
	public RectangleSupport(IDocument document, ITextEditor editor) {
		this(document,MarkUtils.getTextWidget(editor));
	}
	

	ITextSelection getCurrentSelection(ITextEditor editor){
		ISelectionProvider selectionProvider = editor.getSelectionProvider();
		return (ITextSelection) selectionProvider.getSelection();
	}

	/**
	 * Convert the selection information to a RectangleInfo object with information about the 
	 * offsets, lengths and columns involved
	 * 
	 * @param editor
	 * @param document
	 * @param selection
	 * @return the RectangleInfo object
	 */
	public RectangleInfo getRectangleInfo(ITextEditor editor, IDocument document,ITextSelection selection) {
		RectangleInfo result = null;
		try {
			if (selection != null) {
				result = new RectangleInfo(); 
				// get begin and end offsets
				int beginOff = selection.getOffset();
				int endOff = beginOff + selection.getLength();
				// get begin and end line information
				IRegion bl = document.getLineInformationOfOffset(beginOff);
				IRegion el = document.getLineInformationOfOffset(endOff);
				int blOff = bl.getOffset();
				int elOff = el.getOffset();
				// determine the start and end columns
				int startCol = getColumn(document, blOff, beginOff - blOff,Integer.MAX_VALUE).getLength();
				int endCol = getColumn(document, elOff, endOff - elOff,Integer.MAX_VALUE).getLength();
				if (endCol < startCol) {
					int tmp = endCol;
					endCol = startCol;
					startCol = tmp;
				}
				result.setStartColumn(startCol);
				result.setEndColumn(endCol);
				// for each line in the rectangle, determine the offset and length
				int bline = document.getLineOfOffset(blOff);
				int eline = document.getLineOfOffset(elOff);
				LineInfo[] lines = new LineInfo[eline+1-bline];
				for (int i = 0; i < lines.length; i ++) {
					IRegion line = document.getLineInformation(bline + i);
					IRegion start = getColumn(document,line.getOffset(),line.getLength(),startCol);
					IRegion end = getColumn(document,line.getOffset(),line.getLength(),endCol);
					lines[i] = new LineInfo(start.getOffset(),(end.getOffset()-start.getOffset()),end.getLength());
				}
				result.setLines(lines);
			}
		} catch (BadLocationException e) {
			result = null;
		}
		return result;
	}
	
	/**
	 * Copy (and optionally delete) the rectangle information at selection
	 * 
	 * @param editor
	 * @param document
	 * @param selection	treated as a rectangle
	 * @param remove 	if true, delete the rectangle from the document
	 * @return the rectangle contents as String[]
	 * @throws BadLocationException
	 */
	public String[] copyRectangle(ITextEditor editor, IDocument document,ITextSelection selection, boolean remove) throws BadLocationException {
		String[] result = null;
		RectangleInfo rect = getRectangleInfo(editor, document, selection);
		// fetch the text
		LineInfo[] lines = rect.getLines();
		result = new String[lines.length];
		for (int i=lines.length -1; i >= 0; i--) {
			LineInfo line = lines[i];
			String text = document.get(line.getOffset(), line.getLength());
			// augment with spaces if necessary 
			if (line.getColumn() < rect.getEndColumn()) {
				text += getSpaces(line.getColumn(), rect.getEndColumn() - line.getColumn());
			}
			result[i] = text;
			if (remove) {
				// delete each region
				document.replace(line.getOffset(), line.getLength(), EMPTY_STR);
			}
		}
		return result;
	}
	
	/**
	 * Update the text in the rectangle
	 *  
	 * @param editor
	 * @param document
	 * @param selection
	 * @param updateStr the string to add/replace in rectangle or null to add/replace with spaces
	 * @param replace if true, replace current chars, else move them right 
	 * @return the new cursor offset
	 */
	public int updateRectangle(ITextEditor editor, IDocument document,ITextSelection selection, String updateStr, boolean replace, boolean whitespace) {
		
		int result = EmacsPlusUtils.NO_OFFSET;	// default position is cursor
		RectangleInfo rect = getRectangleInfo(editor, document, selection);
		if (rect != null) {
			Position coff = new Position(MarkUtils.getCursorOffset(editor),0);
			// use widget to avoid unpleasant scrolling side effects of IRewriteTarget			
			Control widget = MarkUtils.getTextWidget(editor);
			try {
				widget.setRedraw(false);
				document.addPosition(coff);
				boolean spacing = (updateStr == null); 
				IRegion[] regs = rect.getRegions();
				// get # of columns of insertion
				int colLen = rect.getEndColumn() - rect.getStartColumn();
				// do in reverse order, so offsets don't need to be tracked as doc length changes
				for (int i=regs.length -1 ; i >= 0; i--) {
					if (spacing) {
						if (whitespace) {
							clearInitialWhitespace(document,regs[i]);
						} else {
							// space adding/replacing
							insertSpaces(document, rect.getStartColumn(), regs[i].getOffset(), colLen, replace, !replace).length();						
						}
					} else {
						IRegion reg = regs[i];
						if (updateStr.length() > 0){
							// insert any necessary space to reach start column
							IRegion treg = getInsertPosition(document, reg.getOffset(), rect.getStartColumn(), true);
							// offset may have changed, so construct a region with old length 
							reg = new Region(treg.getOffset(),reg.getLength());
						}
						document.replace(reg.getOffset(), (replace ? reg.getLength() : 0), updateStr);
					}
				}
				// adjust cursor position depending on command
				if (spacing && replace) {
					// clear:  cursor at beginning last line of selection
					rect = getRectangleInfo(editor,document,getCurrentSelection(editor));
					if (rect != null) {
						regs = rect.getRegions();
						result = regs[regs.length-1].getOffset(); 
					}
				} else if (spacing || (MarkUtils.getMark(editor) == MarkUtils.getCursorOffset(editor))) {
					// open:  cursor at top of selection
					// others: Eclipse puts cursor at mark when cursor was at origin of selection, so restore
					result = regs[0].getOffset();
				}  else if (replace) {
					result = coff.getOffset() - (updateStr == null ? 0 : updateStr.length());
				}
			} catch (Exception e) {}
			finally {
				widget.setRedraw(true);			
				document.removePosition(coff);
			}
		}
		return result;
	}

	/**
	 * Insert a rectangle at the cursor offset
	 * 
	 * @param editor
	 * @param document
	 * @param lines		the contents of the rectangle
	 * @return the offset at the end of the insertion
	 * @throws BadLocationException
	 */
	public int insertRectangle(ITextEditor editor, IDocument document, String[] lines) throws BadLocationException {
		int coff = MarkUtils.getCursorOffset(editor); 
		int result = coff;
		Position caret= null;
					
		if (lines != null) {
			int offset = -1;	// flag initial insert position
			int columnPos = -1;
			int line = document.getLineOfOffset(coff);
			int numLines = document.getNumberOfLines()-1;
			IRegion reg = null;
			
			// determine column position
			columnPos = getInsertPosition(document,coff,-1, false).getLength();
			if (line + lines.length > numLines) {
				// if at eof, add any necessary blank lines
				for (int i=0; i < (line+lines.length) - numLines; i++) {
					reg = document.getLineInformation(numLines + i);
					document.replace(reg.getOffset() + reg.getLength(), 0, getEol());							
				}
			}
			// insert lines backwards as a bug in Eclipse doesn't always update 
			// line position information rapidly enough
			for (int i= lines.length -1; i >= 0; i--) {
				//get beginning offset of  line
				offset = document.getLineOffset(line + i);
				reg = getInsertPosition(document, offset, columnPos, true);
				document.replace(reg.getOffset(), 0, lines[i]);
				if (caret == null) {
					caret= new Position(reg.getOffset() + lines[i].length(), 0);
					// the offset position will be updated by the internals on insertion
					document.addPosition(caret);
				}
			}
			if (caret != null) {
				document.removePosition(caret);
				result = caret.offset;
			}
			// set mark at origin of yanked rectangle
			MarkUtils.setMark(editor,coff);
		}
		return result;
	}
	
	/**
	 * Clear initial whitespace in the rectangle
	 * Emacs ignores the end column on each line and removes all initial whitespace on the line
	 * 
	 * @param document
	 * @param reg
	 * @throws BadLocationException
	 */
	private void clearInitialWhitespace(IDocument document, IRegion reg) throws BadLocationException {
		int j=0;
		IRegion lineInfo = document.getLineInformationOfOffset(reg.getOffset());
		for ( ; j < lineInfo.getLength() - (reg.getOffset() - lineInfo.getOffset()); j++) {
			if (document.get(reg.getOffset()+j,1).charAt(0) > ' ') {
				break;
			}
		}
		if (j > 0) {
			document.replace(reg.getOffset(), j, EMPTY_STR);
		}

	}
	
	// Supporting classes
	
	private class LineInfo {
		int offset;
		int length;
		int column;
		
		/**
		 * @param offset
		 * @param length
		 * @param column
		 */
		public LineInfo(int offset, int length, int column) {
			this.offset = offset;
			this.length = length;
			this.column = column;
		}
		public int getOffset() {
			return offset;
		}

		public int getLength() {
			return length;
		}

		public int getColumn() {
			return column;
		}

	}
	
	public class RectangleInfo {
		int startColumn;
		int endColumn;
		LineInfo[] lines;
		
		public int getStartColumn() {
			return startColumn;
		}
		public void setStartColumn(int startColumn) {
			this.startColumn = startColumn;
		}
		public int getEndColumn() {
			return endColumn;
		}
		public void setEndColumn(int endColumn) {
			this.endColumn = endColumn;
		}
		public IRegion[] getRegions() {
			IRegion[] result = new Region[lines.length];
			for (int i = 0; i < lines.length; i++) {
				result[i] = new Region(lines[i].getOffset(), lines[i].getLength());
			}
			return result;
		}
		
		public void setRegions(IRegion[] regions) {
			lines = new LineInfo[regions.length];
			for (int i=0; i < regions.length; i++) {
				lines[i] = new LineInfo(regions[i].getOffset(),regions[i].getLength(),endColumn);
			}
		}
		
		public void setLines(LineInfo[] lines) {
			this.lines = lines;
		}
		public LineInfo[] getLines() {
			return lines;
		}
	}
}
