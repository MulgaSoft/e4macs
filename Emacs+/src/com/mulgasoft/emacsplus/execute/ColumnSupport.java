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
import org.eclipse.jface.text.Region;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.editors.text.EditorsUI;
import org.eclipse.ui.texteditor.AbstractDecoratedTextEditorPreferenceConstants;
import org.eclipse.ui.texteditor.ITextEditor;

import com.mulgasoft.emacsplus.EmacsPlusUtils;
import com.mulgasoft.emacsplus.MarkUtils;

/**
 * @author Mark Feber - initial API and implementation
 */
public class ColumnSupport {

	protected final static String CR = "\n";	//$NON-NLS-1$
	protected final static String EMPTY_STR = "";	//$NON-NLS-1$

	private int tabWidth = 4;
	private String eol = CR;
	private boolean insertSpaces = false;
	
	public ColumnSupport(IDocument document, Control widget) {
		setUp(document,widget);
	}
	
	public ColumnSupport(IDocument document, ITextEditor editor) {
		this(document,MarkUtils.getTextWidget(editor));
	}
	
	// It is not possible AFAIK to unwind the various preferences in Eclipse that determine this
	// and set; some of the known preferences are:
	//		Preferences -> Editors -> Text Editors; 
	//		Preferences -> Java -> Code Style -> Formatter
	//		C++, Javascript and other Language specific formatters
	// so use Eclipse global preference
	
	/**
	 * Get the Eclipse global preference value that controls spacing/tabs
	 * 
	 * @return true if spaces set
	 */
	public static final boolean isSpacesForTabs() {
		boolean result = false;
		// This is the global editor preference
		result = EditorsUI.getPreferenceStore().getBoolean(AbstractDecoratedTextEditorPreferenceConstants.EDITOR_SPACES_FOR_TABS);
		return result;
	}
	
	protected void setUp(IDocument document, Control widget) {
		// set up for length processing
		eol = EmacsPlusUtils.getEol(document);
		tabWidth = ((widget instanceof StyledText) ? ((StyledText)widget).getTabs() : ((Text)widget).getTabs());
		insertSpaces = isSpacesForTabs();
	}

	public String getEol(){
		return eol;
	}

	public int getTabWidth() {
		return tabWidth;
	}
	
	/**
	 * Insert whitespace (tabs or spaces as appropriate) into document at off
	 * When replacing, emacs clears the segment if the line length is less than the column
	 * 
	 * @param document
	 * @param column 	the current column of offset
	 * @param offset	the point to begin the insertion
	 * @param colLen 	the length, in columns, of the insertion
	 * @param replace 	replace if true, else add
	 * 
	 * @return the StringBuilder for length testing
	 * 
	 * @throws BadLocationException
	 */
	public String insertSpaces(IDocument document, int column, int offset, int colLen, boolean replace, boolean force) throws BadLocationException {

		String spaces;
		IRegion lineInfo = document.getLineInformationOfOffset(offset);
		// get the correct number of characters to traverse
		int seglen = lineInfo.getLength() - (offset - lineInfo.getOffset());
		// get the correct offset/column  
		IRegion colEnd = getColumn(document, offset, seglen, colLen);

		if (replace && colEnd.getLength() < colLen )  {
			if (colEnd.getOffset() != offset+seglen) {
				// line is long enough but spacing in line does not match with required column
				spaces = getSpaces(column, colEnd.getLength()).toString(); 
			} else {
				spaces = EMPTY_STR;
			}
		} else {
			spaces = getSpaces(column, colLen).toString();
		}
		// insert into document at offset
		document.replace(offset, (replace ? colEnd.getOffset() - offset : 0), spaces.toString());
		return spaces;
	}

	/**
	 * Get whitespace as appropriate
	 * 
	 * @param column	the starting column
	 * @param colLen	the column length
	 * @return whitespace of column length
	 */
	public String getSpaces(int column, int colLen) {

		int count = colLen;
		int tabWidth = getTabWidth();
		StringBuilder spaces = new StringBuilder(count);
		int prefix = column % tabWidth;
		if (prefix != 0) {
			prefix = tabWidth - prefix;
			if (count > prefix) {
				for (int i = 0; i < prefix; i++) {
					spaces.append(' ');
				}
				count -= prefix;
			}
		}
		int x = count / tabWidth; 
		for (int i=0; i < x; i++) {
			if (insertSpaces) {
				for (int j=0; j < tabWidth; j++) {
					spaces.append(' ');		
				}
			} else {
				spaces.append('\t');
			}				
		}
		x = count % tabWidth;
		for (int i=0; i < x; i++) {
			spaces.append(' ');
		}
		return spaces.toString();
	}

	/**
	 * Get the insert position of the current line of the rectangle.
	 * Updates the line with spaces if necessary
	 *  
	 * @param document
	 * @param offset an offset in the current line; cursor offset on initialization request
	 * @param column the start column position of the rectangle; or -1 on initialization request
	 * @return an IRegion(insert position, charLen)
	 */
	public IRegion getInsertPosition(IDocument document, int offset, int column, boolean force) {
		IRegion result = null;
		try {
			IRegion reg = document.getLineInformationOfOffset(offset);
			int off = reg.getOffset();
			int numChars;
			if (column == -1) {
				column = Integer.MAX_VALUE;
				numChars = offset - off;
			} else {
				numChars = reg.getLength();
			}

			result = getColumn(document, off, numChars, column,true);
		} catch (BadLocationException e) {
			result = null;
		}
		return result;
	}
	
	/**
	 * @param document
	 * @param offset - the initial offset on the line
	 * @param numChars - the number of characters from offset to eol
	 * @param column - the target column or Integer.MAX_VALUE to compute it
	 * @return a region containing the target offset and the column count from offset
	 */
	public IRegion getColumn(IDocument document, int offset, int numChars, int column) {
		return getColumn(document, offset, numChars, column, false);
	}	
	
	/**
	 * Determine the offset and column that corresponds to the column passed in
	 * If the line doesn't contain enough columns, or the offset position doesn't
	 * correspond to the column return closest offset/column information less than column
	 * Unless force is true, in which case fill the document to the length
	 *  
	 * @param document
	 * @param offset 	initial offset on the line
	 * @param numChars 	number of characters to eol
	 * @param column 	target column (if Integer.MAX_VALUE, then result is the computed column)
	 * @param force 	flag to force justify
	 * @return the offset and number of columns from initial offset of the nearest or corresponding position
	 */
	protected IRegion getColumn(IDocument document, int offset, int numChars, int column, boolean force) {
		int tabWidth = getTabWidth();
		int count = 0;
		int prevCount = 0;
		int off = offset;
		int lastOff = offset;
		try {
			for (int i=0; i < numChars; i++, off++) {
				lastOff = off;
				prevCount = count;
				switch (document.getChar(off)) {
				case '\t':
					int tabIncr = (tabWidth - count) % tabWidth;
					count += tabWidth - tabIncr;
					break;
				case (char) -1:
					break;
				default:
					count++;
					break;
				} 
				lastOff++;
				if (count == column) {
					break;
				} else if (count > column) {
					lastOff--;
					count = prevCount;
					break;
				}
			}
			// require region to include the necessary spacing
			if (force && count < column && column != Integer.MAX_VALUE) {
				lastOff += insertSpaces(document, count, lastOff, column - count, false, force).length();
				count = column;
			}
		} catch (BadLocationException e) {
			count = 0;
		}
		return new Region(lastOff, count);
	}
}
