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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.ui.texteditor.ITextEditor;

/**
 * Implement indent-rigidly
 * 
 * Indent all lines <b>starting</b> in the region sideways by ARG columns.
 * You can remove all indentation from a region by giving a large negative ARG.
 *
 * @author Mark Feber - initial API and implementation
 */
public class IndentRigidlyHandler extends RectangleHandler {

	/**
	 * @see com.mulgasoft.emacsplus.commands.RectangleHandler#doTransform(org.eclipse.ui.texteditor.ITextEditor, org.eclipse.jface.text.IDocument, org.eclipse.jface.text.ITextSelection)
	 */
	@Override
	protected int doTransform(ITextEditor editor, IDocument document, ITextSelection currentSelection)
			throws BadLocationException {
		int result = NO_OFFSET;
		int count = this.getUniversalCount();
		// get full explicit or point/mark selection if explicit.length == 0
		ITextSelection selection = getImpliedSelection(editor, document, currentSelection);
		if (selection != null && count != 0) {
			Pattern nonSpace = Pattern.compile("\\S");	//$NON-NLS-1$
			IRegion[] regions = getRegions(document,selection);
			// iterate over lines from bottom up, so offsets remain consistent
			for (int i = regions.length; i > 0; i--) {
				IRegion reg = regions[i-1];
				String text = document.get(reg.getOffset(),reg.getLength());
				Matcher matcher = nonSpace.matcher(text);
				// get offset on the line where spaces end 
				int endOffset = reg.getOffset() + ((matcher.find()) ? matcher.end() - 1 : reg.getLength());
				// get the column number where spaces end
				IRegion endCol = rs.getColumn(document, reg.getOffset(), endOffset - reg.getOffset(), Integer.MAX_VALUE);
				// get the length of the replacement on the line
				int length = endOffset-reg.getOffset(); 
				// get then new length (in columns) of the replacement
				int columns = ((count > 0) ? endCol.getLength()+count : Math.max(endCol.getLength()+count,0));
				document.replace(reg.getOffset(), length, rs.getSpaces(0, columns));
			}
		}
		return result;
	}

	/**
	 * Limit the selection to lines that have their beginning included in the original (implied) selection
	 * 
	 * @param editor
	 * @param document
	 * @param currentSelection
	 * @return selection starting on first line begin, or null if none
	 * 
	 * @throws BadLocationException
	 */
	private ITextSelection getImpliedSelection(ITextEditor editor, IDocument document, ITextSelection currentSelection) throws BadLocationException{
		ITextSelection result = getImpliedSelection(editor, currentSelection);
		if (result != null) {
			IRegion reg = document.getLineInformationOfOffset(result.getOffset());
			// line must include beginning of line
			if (reg.getOffset() != result.getOffset()) {
				// goto next line
				reg = document.getLineInformationOfOffset(document.getLineOfOffset(reg.getOffset())+1);
				int offset = reg.getOffset();
				if (offset > result.getOffset() + result.getLength()) {
					// if we don't even include a single begin line
					result = null;
				} else {
					// last line information
					reg = document.getLineInformationOfOffset(offset + (result.getLength() - (offset - result.getOffset())));
					// get full rectangle (including end of last line) 
					result = new TextSelection(document,offset,(reg.getOffset() + reg.getLength()) - offset);
				}
			}
		}
		return result;
	}
	
	/**
	 * Get the line information for each line in the region
	 * 
	 * @param document
	 * @param selection
	 * @return the array of line regions
	 * 
	 * @throws BadLocationException
	 */
	private IRegion[] getRegions(IDocument document, ITextSelection selection) throws BadLocationException {

		int bline = document.getLineOfOffset(selection.getOffset());
		int eline = document.getLineOfOffset(selection.getOffset()+selection.getLength());
		IRegion[] lines = new IRegion[eline+1-bline];
		for (int i = 0; i < lines.length; i++) {
			lines[i] = document.getLineInformation(bline + i);
		}
		return lines;	
	}
	
	/**
	 * @see com.mulgasoft.emacsplus.commands.EmacsPlusCmdHandler#isLooping()
	 */
	protected boolean isLooping() {
		return false;
	}
	
}
