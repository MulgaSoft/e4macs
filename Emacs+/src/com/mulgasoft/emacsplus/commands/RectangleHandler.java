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
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.texteditor.ITextEditor;

import com.mulgasoft.emacsplus.EmacsPlusUtils;
import com.mulgasoft.emacsplus.execute.RectangleSupport;

/**
 * Base class for rectangle commands that don't use the minibuffer
 * 
 * @author Mark Feber - initial API and implementation
 */
public abstract class RectangleHandler extends EmacsPlusCmdHandler {

	private static final String RECTANGLE_NO_MARK = "Rectangle_No_Mark";	//$NON-NLS-1$  
	
	private int tabWidth = 4;
	private String eol = CR;
	protected RectangleSupport rs;
	private static String[] rectangle = null;
	
	protected abstract int doTransform(ITextEditor editor, IDocument document, ITextSelection currentSelection)
			throws BadLocationException;

	static String[] getLastRectangle() {
		return rectangle;
	}

	static void setLastRectangle(String lastRectangle) {
		RectangleHandler.rectangle = split(lastRectangle);
	}
	
	static void setLastRectangle(String[] lastRectangle) {
		RectangleHandler.rectangle = lastRectangle;
	}

	static String[] split(String rectangle) {
		String[] result = null;
		if (rectangle != null) {
			result = rectangle.split("\r+\n+");	//$NON-NLS-1$			
		}
		return result;
	}
	
	protected String getEol(){
		return eol;
	}
	
	protected int getTabWidth() {
		return tabWidth;
	}
	
	protected void setUp(IDocument document, Control widget) {
		rs = new RectangleSupport(document,widget);
		// set up for length processing
		eol = EmacsPlusUtils.getEol(document);
		tabWidth = ((widget instanceof StyledText) ? ((StyledText)widget).getTabs() : ((Text)widget).getTabs());
	}
	
	/**
	 * @see com.mulgasoft.emacsplus.commands.EmacsPlusCmdHandler#transform(org.eclipse.ui.texteditor.ITextEditor, org.eclipse.jface.text.IDocument, org.eclipse.jface.text.ITextSelection, org.eclipse.core.commands.ExecutionEvent)
	 */
	protected int transform(ITextEditor editor, IDocument document, ITextSelection currentSelection, ExecutionEvent event)
	throws BadLocationException {
		setUp(document,getTextWidget(editor));
		return doTransform(editor,document,currentSelection);
	}

	/**
	 * @see com.mulgasoft.emacsplus.commands.EmacsPlusCmdHandler#undoProtect()
	 */
	protected boolean undoProtect() {
		return true;
	}
	
	/**
	 * @see com.mulgasoft.emacsplus.commands.EmacsPlusCmdHandler#getImpliedSelection(ITextEditor, ITextSelection)
	 */
	protected ITextSelection getImpliedSelection(ITextEditor editor, ITextSelection selection) {
		// if selection length is 0, then if mark and mark != point, set and return as selection
		ITextSelection result = super.getImpliedSelection(editor, selection);
		if (result == null) {
			EmacsPlusUtils.showMessage(editor, RECTANGLE_NO_MARK, true);
		}
		return result;
	}

	/**
	 * Get the insert position of the current line of the rectangle.
	 * Updates the line with spaces if necessary
	 *  
	 * @param editor
	 * @param offset an offset in the current line or -1 to get cursor offset
	 * @param charLen the total length (in space characters) of the line position
	 * @return an IRegion(insert position, charLen)
	 */
	IRegion getInsertPosition(IDocument document, int offset, int column, boolean force) {
		return rs.getInsertPosition(document, offset, column, force);
	}

	/**
	 * Change the contents of the rectangle based on the update string and flag settings
	 * 
	 * @param editor
	 * @param document
	 * @param selection
	 * @param updateStr	if null, then adding or replacing with spaces, else update with string contents
	 * @param replace 	if true, then replace rather than add
	 * @param whitespace	if true, remove whitespace from the front of each line in the rectangle
	 * @return	the new cursor offset
	 */
	int updateRectangle(ITextEditor editor, IDocument document, ITextSelection selection, String updateStr, boolean replace, boolean whitespace) {
		return rs.updateRectangle(editor, document, selection, updateStr, replace,whitespace);
	}
	
	int updateRectangle(ITextEditor editor, IDocument document, ITextSelection selection, String updateStr, boolean replace) {
		return updateRectangle(editor, document, selection, updateStr, replace, false);
	}
	
	/**
	 * Insert space (tabs or spaces as appropriate) into document at off
	 * 
	 * @param document
	 * @param off - the point to begin the insertion
	 * @param count - the length of the insertion
	 * @param replace - replace if true, else add
	 * @return the StringBuilder for length testing
	 * 
	 * @throws BadLocationException
	 */
	String insertSpaces(IDocument document, int column, int off, int count, boolean replace, boolean force) throws BadLocationException {
		return rs.insertSpaces(document, column, off, count, replace, force);
	}
	
	/**
	 * @param editor
	 * @param document
	 * @param lines		the contents of the rectangle
	 * @throws BadLocationException
	 */
	void insertRectangle(ITextEditor editor, IDocument document, String[] lines) throws BadLocationException {
		rs.insertRectangle(editor, document, lines);
	}

}
