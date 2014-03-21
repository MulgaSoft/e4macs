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
import org.eclipse.ui.texteditor.ITextEditor;

import com.mulgasoft.emacsplus.EmacsPlusActivator;
import com.mulgasoft.emacsplus.EmacsPlusUtils;

/**
 * Implement: what-cursor-position
 * 
 * Print info on cursor position (on screen and within buffer)
 * 
 * @author Mark Feber - initial API and implementation
 */
public class WhatCursorPosition extends EmacsPlusNoEditHandler {
	
	private final static String CURSOR_POSITION = EmacsPlusActivator.getResourceString("What_Cursor");	//$NON-NLS-1$
	private final static String EOL_POSITION = EmacsPlusActivator.getResourceString("What_Cursor_EOL");	//$NON-NLS-1$
	private final static String EOB_POSITION = EmacsPlusActivator.getResourceString("What_Cursor_EOB");	//$NON-NLS-1$
	
	static final String N_GEN = "\\c";	//$NON-NLS-1$
	static final String N_NEW = "\\n";	//$NON-NLS-1$
	static final String N_RET = "\\r";	//$NON-NLS-1$
	static final String N_TAB = "\\t";	//$NON-NLS-1$
	static final String N_BS = "\\b";	//$NON-NLS-1$
	static final String N_FF = "\\f";	//$NON-NLS-1$
	static final String N_SPC = "SPC";	//$NON-NLS-1$
	
	/**
	 * @see com.mulgasoft.emacsplus.commands.EmacsPlusNoEditHandler#transform(org.eclipse.ui.texteditor.ITextEditor, org.eclipse.jface.text.IDocument, org.eclipse.jface.text.ITextSelection, org.eclipse.core.commands.ExecutionEvent)
	 */
	@Override
	protected int transform(ITextEditor editor, IDocument document, ITextSelection currentSelection,
			ExecutionEvent event) throws BadLocationException {
		
		String msg = null;
		
		int offset = getCursorOffset(editor,currentSelection);
		int docLen = document.getLength();
		IRegion line = document.getLineInformationOfOffset(offset); 

		if (offset >= docLen) {
			msg = String.format(EOB_POSITION, offset,docLen);
		} else {
			char curChar = document.getChar(offset);
			String sChar = "";	//$NON-NLS-1$
			int percent = new Float(((offset * 100) / docLen) + .5).intValue();

			if (offset == line.getOffset() + line.getLength()){
				String ld = document.getLineDelimiter(document.getLineOfOffset(offset));
				char[] points = ld.toCharArray();
				for (int i=0; i<points.length; i++) {
					sChar += normalizeChar(points[i]);
				}
				msg = String.format(EOL_POSITION, sChar,offset,docLen,percent);
			} else {

				int curCode = (int) curChar;
				sChar = (curChar <= ' ' ? normalizeChar(curChar) : String.valueOf(curChar));
				msg = String.format(CURSOR_POSITION, sChar, curCode, curCode, curCode, offset, docLen, percent);
			}
		}
		EmacsPlusUtils.showMessage(editor, msg, false);
		setCmdResult(new Integer(offset));
		return super.transform(editor, document, currentSelection, event);

	}
	
	private String normalizeChar(char cc) {
		String result = null;
		switch (cc) {
		case ' ':
			result = N_SPC;
			break;
		case '\r':
			result = N_RET;
			break;
		case '\n':
			result = N_NEW;
			break;
		case '\t':
			result = N_TAB;
			break;
		case '\f':
			result = N_FF;
			break;
		case '\b':
			result = N_BS;
			break;
		default:
			result = N_GEN + cc;
		}
		return result;
	}
	
}
