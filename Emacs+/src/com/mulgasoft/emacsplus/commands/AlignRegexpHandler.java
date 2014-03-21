/**
 * Copyright (c) 2009, 2013 Mark Feber, MulgaSoft
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package com.mulgasoft.emacsplus.commands;


import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.IRewriteTarget;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.Position;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.texteditor.ITextEditor;

import com.mulgasoft.emacsplus.EmacsPlusActivator;
import com.mulgasoft.emacsplus.execute.ColumnSupport;
import com.mulgasoft.emacsplus.minibuffer.AlignMinibuffer;
import com.mulgasoft.emacsplus.minibuffer.AlignMinibuffer.AlignControl;

/**
 * Align selection based on a regular expression
 * 
 * Align the current region using an ad-hoc rule read from the minibuffer.
 * The selection marks the limits of the region.  This function will prompt
 * for the regexp to align with.  If no prefix arg was specified, you
 * only need to supply the characters to be lined up and any preceding
 * whitespace is replaced.  If a prefix arg was specified, the full
 * regexp with parenthesized whitespace should be supplied; it will also
 * prompt for which parenthesis group within regexp to modify, the amount
 * of spacing to use, and whether or not to repeat the rule throughout
 * the line.
 * 
 * @author Mark Feber - initial API and implementation
 */
public class AlignRegexpHandler extends MinibufferExecHandler {

	private static final String ALIGN_PREFIX = EmacsPlusActivator.getResourceString("Align_Regexp"); //$NON-NLS-1$ 

	/**
	 * @see com.mulgasoft.emacsplus.commands.MinibufferHandler#getMinibufferPrefix()
	 */
	public String getMinibufferPrefix() {
		return ALIGN_PREFIX;
	}
	
	/**
	 * @see com.mulgasoft.emacsplus.commands.EmacsPlusCmdHandler#transform(ITextEditor, IDocument, ITextSelection, ExecutionEvent)
	 */
	@Override
	protected int transform(ITextEditor editor, IDocument document, ITextSelection currentSelection, ExecutionEvent event)
	throws BadLocationException {
		return bufferTransform(new AlignMinibuffer(this, isUniversalPresent()), editor, event);
	}

	/**
	 * ^U specifies complex version of command
	 * 
	 * @see com.mulgasoft.emacsplus.commands.EmacsPlusCmdHandler#isLooping()
	 */
	@Override
	protected boolean isLooping() {
		return false;
	}

	/**
	 * Get the column position of the line at numChars from start
	 * 
	 * @param cs
	 * @param document
	 * @param start
	 * @param numChars
	 * @return the column position of (start+numChars)
	 */
	private int getColumnPosition(ColumnSupport cs, IDocument document, int start, int numChars) {
		return cs.getColumn(document, start, numChars, Integer.MAX_VALUE).getLength();
	}

	private class Alignment {

		public Alignment(int line, int start) {
			l_number = line;
			t_start = start;
		}

		int l_number; // line number in the document
		int l_start;  // line offset in the document
		int t_start;  // line (or segment) text start
		int m_start;  // match start
		int m_len;    // match length
		int g_start;  // (whitespace) group start
		int g_column; // (whitespace) group start column
		int g_len;    // (whitespace) group length
		int r_adjust; // adjust on repeat
		int j_len;    // justify length when group < 0
	}

	protected boolean doExecuteResult(ITextEditor editor, Object minibufferResult) {
		AlignControl ac = (AlignControl)minibufferResult;

		ITextSelection selection = getCurrentSelection(editor);
		IDocument doc = getThisDocument();
		IRewriteTarget rt = (IRewriteTarget) editor.getAdapter(IRewriteTarget.class);		
		Control widget = getTextWidget(editor);

		int startLine = selection.getStartLine();
		int endLine = selection.getEndLine();
		int offset = selection.getOffset();
		int endOffset = selection.getLength() + offset;
		// remember cursor offset through text changes
		Position coff = new Position(getCursorOffset(editor),0);
		try {
			doc.addPosition(coff);
			Pattern pc = checkPattern(ac.getPattern(), editor);
			if (pc == null) {
				return true;
			}
			// use widget to avoid unpleasant scrolling side effects of IRewriteTarget			
			widget.setRedraw(false);
			if (rt != null) {
				rt.beginCompoundChange();
			}
			List<Alignment> alignments = new ArrayList<Alignment>();

			ColumnSupport cs = new ColumnSupport(doc,editor);
			//			for (int i=0,l=startLine; l <=endLine; l++,i++) {
			for (int l=endLine; l >= startLine; l--) {

				IRegion lineInfo = doc.getLineInformation(l);
				int l_offset  = lineInfo.getOffset();
				int s = l_offset;
				int e = s + lineInfo.getLength();
				if (s < offset) {
					s = offset;
				}
				if (e > endOffset) {
					e = endOffset;
				}

				Alignment a = null;
				// match the line with the regexp, ignore any lines with no match
				if ((a = getLineMatch(l,s,e,ac.getGroup(),pc,editor)) != null) {
					alignments.add(a);
					a.l_start = l_offset;
					// determine the column on each line
					a.g_column = getColumnPosition(cs, doc, l_offset, a.g_start - l_offset);
					// the farthest group column is where replacement will start
					if (a.g_column > ac.getMaxColumn()) {
						ac.setMaxColumn(a.g_column);
					}
				}
			}
			while (!alignments.isEmpty()) {
				alignments = alignmentReplace(alignments, ac, cs, pc, editor);
			}
			
			// and position point at correct cursor offset
			selectAndReveal(editor, coff.offset, coff.offset);
		} catch (BadLocationException e) {
			// Shouldn't happen
			e.printStackTrace();
		}
		finally {
			widget.setRedraw(true);			
			doc.removePosition(coff);
			if (rt != null) {
				rt.endCompoundChange();
			}
		}
		// and signal for cleanup
		return true;
	}		
	
	/**
	 * Replace the group segment with the computed spaces.
	 * On repeat, pre-populate the next matched segment information 
	 * 
	 * @param alignments
	 * @param ac control parameters for the alignment
	 * @param cs utility class for column computation
	 * @param pc validate pattern
	 * @param editor
	 * @return the list of next Alignment segments on repeat, or the empty list on no repeat
	 * @throws BadLocationException
	 */
	private List<Alignment> alignmentReplace(List<Alignment> alignments, AlignControl ac, ColumnSupport cs, Pattern pc, ITextEditor editor) throws BadLocationException {

		List<Alignment> nextalignments = new ArrayList<Alignment>();
		IDocument doc = getThisDocument();
		int spacing = ac.getSpacing();
		int maxColumn = ac.getMaxColumn();
		ac.setMaxColumn(-1);	// and reset
		for (Alignment a : alignments) {
			int spaceColumns;
			// if valid absolute column position, use it
			if (spacing < 0 && (spaceColumns = -spacing - a.g_column) >= 0) {
				;
			} else {
				spaceColumns = (maxColumn - a.g_column) + (spacing <0 ? 1 : spacing);
			}
			// column overruns are replaced by a single space
			String spaces = cs.getSpaces(a.g_column, (spaceColumns < 0 ? 1 : spaceColumns));
			// if group < 0 then replace from g_start through initial whitespace only
			int replLen = ((ac.getGroup() < 0) ? a.j_len : a.g_len);
			doc.replace(a.g_start, replLen, spaces);

			// after replacement compute the number of chars from beginning to current match end
			int change = (spaces.length() - a.g_len);
			int matchStartOffsetLen = (a.g_start + a.g_len) - a.l_start; 
			int matchSize = a.m_len;
			a.r_adjust = (change + matchSize) + matchStartOffsetLen;
		}
		if (ac.getRepeat()) {
			for (Alignment a : alignments) {
				IRegion info = doc.getLineInformation(a.l_number); 
				int l_offset = info.getOffset();
				Alignment nextMatch = null;
				// the next match starts from the end of the previous match (r_adjust)
				if ((nextMatch = getLineMatch(a.l_number, l_offset + a.r_adjust,
								l_offset + info.getLength(), ac.getGroup(), pc, editor)) != null) {
					nextalignments.add(nextMatch);
					nextMatch.l_start = l_offset;
					// determine the column on each line
					nextMatch.g_column = getColumnPosition(cs, doc, l_offset, nextMatch.g_start
							- l_offset);
					// the farthest group column is where replacement will start
					if (nextMatch.g_column > ac.getMaxColumn()) {
						ac.setMaxColumn(nextMatch.g_column);
					}
				}
			}
		}
		return nextalignments;
	}
	
	/**
	 * Match the document line against the regular expression.  Populates the 
	 * Alignment class with the group and pattern match locations
	 * 
	 * @param line the line number in the document
	 * @param start the start offset in the line 
	 * @param end the endo offset in the line
	 * @param group the group to modify within the regexp
	 * @param pc validated regexp Pattern
	 * @param editor
	 * @return the populated Alignment class or null if no match 
	 */
	private Alignment getLineMatch(int line, int start, int end, int group, Pattern pc, ITextEditor editor){
		Alignment alignment = null;
		try {
			String input = getThisDocument().get(start,end - start);
			int group_start, group_end;
			boolean justify = group < 0;
			group = (justify ? -group : group);
			if (input != null && input.length() > 0) {
				Matcher m = pc.matcher(input);
				if (m.find() && group <= m.groupCount()) {
					group_start = m.start(group);
					group_end = m.end(group);
					if (!(group_start < 0)) {	// matcher returns -1 if group didn't match
						Alignment a = new Alignment(line, start);
						a.g_start = group_start + a.t_start;
						a.m_start = group_end + a.t_start;
						a.m_len = m.end() - group_end;
						a.g_len = a.m_start - a.g_start;
						alignment = (a.m_len == 0 ? null : a); // disallow group only matches 
						// manually determine initial spaces in group when justifying
						if (alignment != null && justify) {
							int j_len = 0;
							for (int i = group_start; i < group_end - group_start; i++) {
								if (input.charAt(i) <= ' ') {
									j_len = i;
								} else {
									break;
								}
							}
							alignment.j_len = j_len;
						}
					}
				}
			}
		} catch (BadLocationException e) {
			// shouldn't happen
			e.printStackTrace();
		}
		return alignment;
	}

	/**
	 * Perform a 'compilation' check for the regexp
	 * 
	 * @param regexp the String regular expression  
	 * @param editor used for error message if compilation fails
	 * 
	 * @return Pattern if ok, else null
	 */
	private Pattern checkPattern(String regexp, ITextEditor editor) {
		Pattern result = null;
		try {
			result = Pattern.compile(regexp);
		} catch (PatternSyntaxException e) {
			String msg = e.getLocalizedMessage().replaceAll("[\r\n]"," ");	//$NON-NLS-1$ //$NON-NLS-2$
			// check for and remove useless (in this context) pointer
			if (msg.charAt(msg.length()-1) == '^') {
				msg = msg.substring(0, msg.length()-2);
			}
			showResultMessage(editor, msg, true);
		}
		return result;
	}
}
