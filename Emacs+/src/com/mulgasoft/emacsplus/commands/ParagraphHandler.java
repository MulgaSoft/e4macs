/**
 * Copyright (c) 2009, 2010 Mark Feber, MulgaSoft
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package com.mulgasoft.emacsplus.commands;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IFindReplaceTarget;
import org.eclipse.jface.text.IFindReplaceTargetExtension3;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.swt.graphics.Point;
import org.eclipse.ui.texteditor.ITextEditor;

import com.mulgasoft.emacsplus.IEmacsPlusCommandDefinitionIds;
import com.mulgasoft.emacsplus.MarkUtils;
import java.util.regex.Pattern;

/**
 * Implement simple version of paragraph commands suitable for programming languages
 * 
 * We don't pay attention to paragraph start, just paragraph separate
 * Since, as in Emacs major modes for programs, paragraphs begin and end only at blank
 * lines.  This makes the paragraph commands useful, even though there are no paragraphs 
 * as such in a program.
 * 
 * @author Mark Feber - initial API and implementation
 */
public abstract class ParagraphHandler extends EmacsPlusCmdHandler {

	final static String PARAGRAPH_START = "^[ \t\f\r\n]*$"; //$NON-NLS-1$
	final static String REGEX_BOL_HACK = "^[\\s\\S]";   	//$NON-NLS-1$
	
	final static Pattern BLANK_CHECK = Pattern.compile(PARAGRAPH_START);
	
	protected IFindReplaceTarget getTarget(ITextEditor editor) {
		return (IFindReplaceTarget)editor.getAdapter(IFindReplaceTarget.class);
	}

	protected int getParagraphOffset(ITextEditor editor, boolean forward) {
		if (findNext(editor, forward) == -1) {
			findNextEmpty(editor, forward);
		}
		// get correct empty line in model coords
		return getCurrentSelection(editor).getOffset();
	}
	
	// Work around bug in FindReplaceDocumentAdapter (we use this because it provides support for narrowed regions, etc.)
	// First, look for normal pattern with findNext, but it will return -1 on ^$ lines
	// so, if -1 then look for empty lines with findNextEmpty
	
	/**
	 * Find the next occurrence of empty line(s)
	 * Will match all valid lines except single ^$
	 *   
	 * @param editor
	 * @param forward true if moving forward
	 * @return -1 if not found, else offset of match in widget coords
	 */
	protected int findNext(ITextEditor editor, boolean forward) {

		IFindReplaceTarget frt = getTarget(editor);
		Point selection = frt.getSelection();
		// don't move past eob
		int offset = Math.min(MarkUtils.getCharCount(editor), Math.max(0,MarkUtils.getCaretOffset(editor) + (forward ? 1 : -2)));
		int result = ((IFindReplaceTargetExtension3)frt).findAndSelect(offset, PARAGRAPH_START, forward, false, false, true);		
		if (result != -1) {
			if ((forward && offset > result) || (!forward && offset < result)) {
				// reset if we've wrapped around in the search
				MarkUtils.setWidgetSelection(editor, selection.x, selection.x + selection.y);
				result = -1;
			}
		}
		return result;
	}
	
	/**
	 * Find the next occurrence of empty line consisting of ^$
	 * Since findNext will find multiple sequential ^$'s, we only need find one
	 * 
	 * @param editor
	 * @param forward
	 * @return offset of match in widget coords
	 */
	protected int findNextEmpty(ITextEditor editor, boolean forward) {
		IFindReplaceTarget frt = getTarget(editor);
		Point selection = frt.getSelection();
		int count = MarkUtils.getCharCount(editor);
		int coffset = MarkUtils.getCaretOffset(editor); 
		// don't move past EOB
		int offset = Math.min(count,coffset + (forward ? 1 : -1)); //(forward ? selection.x + selection.y+1 : selection.x-2);	
		int next = 0;
		int prev = -1;
		do {
			next = ((IFindReplaceTargetExtension3)frt).findAndSelect(offset, REGEX_BOL_HACK, forward, false, false, true);
			if (next == -1 || prev == next){
				// we've run out of buffer
				offset = (forward ? count : 0); 
				MarkUtils.setWidgetSelection(editor, offset, offset);
				return offset;
			} else if (forward && offset > next) {
				// protect against overrun
				MarkUtils.setWidgetSelection(editor, count, count);
				return count;
			} else if (!forward && offset < next) {
				// protect against overrun
				MarkUtils.setWidgetSelection(editor, 0, 0);
				return 0;
			} else {
				selection = frt.getSelection();	
				// Eclipse internals (frda) return different regions depending on whether the file has a 
				// single or double line delimiter e.g. \r\n or \n.  In the former it returns a 0 length
				// region, and in the latter a length of 1
				if (selection.y == 0 || (!forward && selection.x == 0)) {
					// found it, or reached the top
					return selection.x;
				} if (selection.y == 1) {
					// check for single line delimiter 
					char c = frt.getSelectionText().charAt(0);
					if (c == '\n' || c == '\r'){
						return selection.x;
					}
				}
				// the widget count could change if a folded region expands automatically
				count = MarkUtils.getCharCount(editor);
				// don't move past EOB				
				offset = Math.min(count,(forward ? selection.x + selection.y+1 : selection.x-1));
				prev = next;
			}
		} while (next != -1);
		return next;
	}	

	/**
	 * Check if the specified line is blank
	 * 
	 * @param document
	 * @param line
	 * @return true if blank, else false
	 * 
	 * @throws BadLocationException
	 */
	boolean isBlank(IDocument document, int line) throws BadLocationException {
		IRegion lineInfo = document.getLineInformation(line);
		return lineInfo.getLength() == 0 || BLANK_CHECK.matcher(document.get(lineInfo.getOffset(), lineInfo.getLength())).matches(); 
	}
	
	/**
	 * Check if we're at buffer top (or top of narrowed region)
	 * 
	 * @param editor
	 * @param selection
	 * @return true is condition satisfied, else false
	 */
	boolean isAtTop(ITextEditor editor, ITextSelection selection) {
		// if we're not at buffer top (or top of narrowed region)
		return (selection.getOffset() == 0 || 
				(editor.showsHighlightRangeOnly() && selection.getOffset() == editor.getHighlightRange().getOffset()));
	}
	
	boolean isAtBottom(ITextEditor editor, IDocument document, ITextSelection selection) {
		int offset = selection.getOffset() + selection.getLength();
		// TODO account for EOL length
		return (offset+1 >= document.getLength());
	}
	
	/**
	 * Called when we're expanding a mark selection
	 * 
	 * @param editor
	 * @param offset
	 * @param markSelection
	 * @return NO_OFFSET
	 */
	protected int selectTransform(ITextEditor editor, int offset, ITextSelection markSelection) {
		// we're expanding a mark selection
		int mark = getMark(editor);
		if (mark == -1 || 
				((markSelection != null && !checkMark(mark,markSelection.getOffset(),markSelection.getLength())))){
			try {
				executeCommand(IEmacsPlusCommandDefinitionIds.SET_MARK, null, editor);
				mark = getMark(editor);
			} catch (Exception e) {
			}
		} 
		selectAndReveal(editor,offset,mark);
		// set mark flag if we're back at ground zero, else clear
		setFlagMark(getCurrentSelection(editor).getLength() == 0);
		return NO_OFFSET;
	}	
}
