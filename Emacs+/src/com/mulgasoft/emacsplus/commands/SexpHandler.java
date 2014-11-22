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

import static com.mulgasoft.emacsplus.EmacsPlusUtils.getPreferenceStore;
import static com.mulgasoft.emacsplus.preferences.PrefVars.ENABLE_GNU_SEXP;
import static com.mulgasoft.emacsplus.preferences.PrefVars.ENABLE_DOT_SEXP;
import static com.mulgasoft.emacsplus.preferences.PrefVars.ENABLE_UNDER_SEXP;

import java.text.BreakIterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.console.TextConsoleViewer;
import org.eclipse.ui.texteditor.ITextEditor;

import com.mulgasoft.emacsplus.EmacsPlusUtils;
import com.mulgasoft.emacsplus.IEmacsPlusCommandDefinitionIds;
import com.mulgasoft.emacsplus.SexpCharacterPairMatcher;

// TODO: Handle < > properly when in math expression	

/**
 * Base class with common methods used to determine s-expressions
 * 
 * @author Mark Feber - initial API and implementation
 */
public abstract class SexpHandler extends EmacsPlusNoEditHandler {

	protected final static String UNBALANCED = "Sexp_Unbalanced";	//$NON-NLS-1$ 
	protected final static String PREMATURE = "Sexp_Premature";	//$NON-NLS-1$ 
	
	protected final static char[] sexpBrackets = { '{', '}', '[', ']', '(', ')' };
	protected final static char[] xmlSexpBrackets = { '{', '}', '[', ']', '(', ')', '<', '>' };
	protected final static char[] lispBrackets = {'(', ')'};
	
	private char[] cSexpBrackets = null;
	
	private boolean isLispOnly = false;
	private boolean unbalanced = false;

	protected static final int OPEN = 0;
	protected static final int CLOSE = 1;
	
	protected static final int NONE = 0;
	protected static final int DOWN = 1;
	protected static final int UP = -1;
		
	protected static char SQUOTE_CHAR = '"';
	protected static char CQUOTE_CHAR = '\'';
	protected static char QQUOTE_CHAR = '\\';

	/* ******** Support gnu sexps and different language syntax for sexps ******** */
	
	// preference that controls interpretation of unbalanced sexps 
	// if true, then stop when unbalanced sexp is detected, else move into outer sexp
	static boolean GNU_SEXP = EmacsPlusUtils.getPreferenceBoolean(ENABLE_GNU_SEXP.getPref());
	// specify . as a word break character in sexps
	static boolean DOT_SEXP = EmacsPlusUtils.getPreferenceBoolean(ENABLE_DOT_SEXP.getPref());
	// specify _ as a word break character in sexps
	static boolean UNDER_SEXP = EmacsPlusUtils.getPreferenceBoolean(ENABLE_UNDER_SEXP.getPref());
	
	static {
		// listen for changes in the property store
		getPreferenceStore().addPropertyChangeListener(
				new IPropertyChangeListener() {
					public void propertyChange(PropertyChangeEvent event) {
						if (ENABLE_DOT_SEXP.getPref().equals(event.getProperty())) {
							enableDotSexp((Boolean)event.getNewValue());
						} else if (ENABLE_UNDER_SEXP.getPref().equals(event.getProperty())) {
							enableUnderSexp((Boolean)event.getNewValue());
						} else if (ENABLE_GNU_SEXP.getPref().equals(event.getProperty())) {
							enableGnuSexp((Boolean)event.getNewValue());
						}
					}
				}
		);
	}

	// look for . in word when moving forward, so we can break 
	String dotBreak = IDENT_REGEX + "(\\.)" + IDENT_REGEX;	//$NON-NLS-1$
	Matcher dotBreakMatcher = Pattern.compile(dotBreak).matcher(EMPTY_STR);
	
	// look for . in word when moving backward, so we can break 
	String dotBack = "[" + IDENT_REGEX + ".]*" + IDENT_REGEX + "(\\.)" + IDENT_REGEX + "+.*+$";	//$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
	Matcher dotBackMatcher = Pattern.compile(dotBack).matcher(EMPTY_STR);
	
	// look for _ in word when moving forward, so we can pass over
	String underBreak = "[._]([_"+ IDENT_REGEX + "]*).*";	//$NON-NLS-1$ //$NON-NLS-2$
	Matcher underBreakMatcher = Pattern.compile(underBreak).matcher(EMPTY_STR);
	
	// look for _ in word when moving backward, so we can pass over
	String underBack = "([_"+ IDENT_REGEX + "]*)_$";	//$NON-NLS-1$ //$NON-NLS-2$
	Matcher underBackMatcher = Pattern.compile(underBack).matcher(EMPTY_STR);

	// look for _ and . in word when moving backward, so we can pass over
	String underDotBack = "([._"+ IDENT_REGEX + "]*)_$";	//$NON-NLS-1$ //$NON-NLS-2$
	Matcher underDotBackMatcher = Pattern.compile(underDotBack).matcher(EMPTY_STR);

	Matcher getUnderMatcher() {
		return underBreakMatcher;
	}
	
	Matcher getDotMatcher() {
		return dotBreakMatcher;
	}
	
	/**
	 * Is the character the (directional) beginning of a sexp
	 *  
	 * @param c
	 * @return true if it is, else false
	 */
	protected abstract boolean isBracket(char c);
	
	/**
	 * Is the character the (directional) end of a containing sexp
	 *  
	 * @param c
	 * @return true if unbalanced character detected, else false
	 */
	protected abstract boolean isUnbalanced(char c, int updown);

	/**
	 * Return the first boundary following the specified offset.
     * The value returned is always greater than the offset or the value BreakIterator.DONE
	 * Emacs+ layers optional (non)boundary characters on top of the default functionality
	 * @see java.text.BreakIterator#following(int)
	 * 
	 * @param document
	 * @param iter
     * @return the first boundary following the specified offset or BreakIterator.DONE
	 */
	protected abstract int getNextPosition(IDocument document, BreakIterator iter, int pos);

	/**
	 * Return the boundary following the current boundary.  
	 * Emacs+ layers optional (non)boundary characters on top of the default functionality
	 * @see java.text.BreakIterator#next()
	 * 
	 * @param document
	 * @param iter
	 * @return the index of the next boundary from the current position or BreakIterator.DONE
	 */
	protected abstract int getNextPosition(IDocument document, BreakIterator iter);

	protected abstract int getBracketPosition(IDocument document, int pos);

	protected abstract int getDirection();

	protected abstract int endTransform(ITextEditor editor, int offset, ITextSelection origSelection,ITextSelection selection);
	protected abstract int endTransform(TextConsoleViewer viewer, int offset, ITextSelection origSelection, ITextSelection selection);
		
	@Override
	protected void preTransform(ITextEditor editor, ITextSelection selection) {
		// in this context, will often generate unbalanced message, so clear first
		EmacsPlusUtils.clearMessage(editor);
		super.preTransform(editor, selection);
	}

	/**
	 * Get the transposition sexp, i.e: - a complete sexp at current position -
	 * ignore non-identifier constituents
	 * 
	 * @param document
	 * @param pos
	 *            - starting position to determine sexp
	 * @param wordp
	 *            - words only if true
	 * @return the sexp selection
	 * 
	 * @throws BadLocationException
	 */
	abstract ITextSelection getTransSexp(IDocument document, int pos, boolean wordp) throws BadLocationException;

	// When invoked with ^U, movement can expand the selection
	// so, check each time
	@Override
	protected ITextSelection getCmdSelection(ITextEditor editor,
			ITextSelection selection) throws ExecutionException {
		ITextSelection cSelection = getCurrentSelection(editor);
		if (!cSelection.equals(selection)) {
			return cSelection;
		} else {
			return super.getCmdSelection(editor, selection);
		}
	}

	boolean isLispOnly() {
		return isLispOnly;
	}

	/**
	 * Check if we encountered an unbalanced sexp
	 * 
	 * @return true if last movement attempted to move to an illegal position
	 */
	boolean isUnbalanced() {
		return unbalanced;
	}
	
	/**
	 * Flag that command attempted to move to an illegal position
	 * 
	 * @param unbalanced
	 */
	void setUnbalanced(boolean unbalanced) {
		this.unbalanced = unbalanced;
	}

	/**
	 * Force Gnu semantics for sexp movement
	 * beep/error when attempting to move into next outer sexp
	 * 
	 * @param gnuSexp
	 */
	public static void enableGnuSexp(boolean gnuSexp) {
		SexpHandler.GNU_SEXP = gnuSexp;	
	}

	public static boolean isGnuSexp() {
		return SexpHandler.GNU_SEXP;	
	}
	
	public static void enableDotSexp(boolean gnuSexp) {
		SexpHandler.DOT_SEXP = gnuSexp;	
	}
	
	public static boolean isDotSexp() {
		return SexpHandler.DOT_SEXP;
	}
	
	boolean isDot() {
		return isDotSexp();
	}
	
	public static void enableUnderSexp(boolean gnuSexp) {
		SexpHandler.UNDER_SEXP = gnuSexp;	
	}
	
	public static boolean isUnderSexp() {
		return SexpHandler.UNDER_SEXP;
	}
	
	boolean isUnder() {
		return isUnderSexp();
	}
	
	/* The default BreakIterator uses RuleBasedBreakIterator
	 * It appears to be too onerous to modify the rules depending on the language context
	 * so add some simple checks to allow us to optionally:
	 *  - treat . (period) as a word break character
	 *  - no treat _ (underscore) as a word break character
	 *  
	 * as the default rules do not treat single embedded . as breaks which is
	 * not the way Emacs behaves in java
	 * and always treat multiple sequential . and _ (or any combination) as breaks
	 * which is inconvenient in languages like python (e.g. __identifier__) 
	 */
	/**
	 * If a . is a word break character, see if the BreakIterator moved us past one
	 * 
	 * @param doc
	 * @param start
	 * @param end
	 * @return new offset if word moved past any ., else pos
	 */
	int checkDot(IDocument doc, int start, int end) {
		int result = end;
		try {
			if (isDot()) {
				Matcher matcher = getDotMatcher();
				matcher.reset(doc.get(start, end - start));
				if (matcher.find()) {
					result = start + matcher.start(1);
				}
			}
		} catch (BadLocationException e) {
		}
		return result;
	}
	
	/**
	 * If a _ is not a word break character, see if the BreakIterator stopped on one
	 * 
	 * @param doc
	 * @param pos
	 * @return new offset if word moves past any _'s, else pos
	 */
	int checkUnder(IDocument doc, int pos) {
		int result = pos;
		try {
			if (!isUnder()) {
				char c = doc.getChar(pos);
				if (!isDot() || c != '.') {
					IRegion lineInfo = doc.getLineInformationOfOffset(pos);
					int p = pos;
					// if we're at or just moved over an _
					if (c == '_' || (--p >= lineInfo.getOffset() && doc.getChar(p) == '_')) {
						int end = (lineInfo.getOffset() + lineInfo.getLength()); 
						if (end > p) {
							Matcher matcher = getUnderMatcher();
							matcher.reset(doc.get(p, end - p));
							if (matcher.matches()) {
								result = p + matcher.end(1);
							}
						}
					}
				}
			}
		} catch (BadLocationException e) {
		}
		return result;
	}

	protected char[] getSexpBrackets() {
		if (cSexpBrackets == null) {
			if (isLispOnly()) {
				cSexpBrackets = lispBrackets;
			} else if (isTypeXml()) {
				cSexpBrackets = xmlSexpBrackets;
			} else {
				cSexpBrackets = sexpBrackets;
			}
		}
		return cSexpBrackets;
	}
	
	public Object execute(ExecutionEvent event) throws ExecutionException {
		cSexpBrackets = null;
		return super.execute(event);
	}

	/**
	 * Determine if the character is in the current bracket set
	 * @param c
	 * @param adj
	 * @return true if bracket
	 */
	protected boolean isBracket(char c, int adj) {

		for (int i = 0 + adj; i < getSexpBrackets().length; i = i + 2) {
			if (c == getSexpBrackets()[i]) {
				return true;
			}
		}
		return false;
	}

	protected boolean isOpen(char c) {
		boolean result = false;
		char[] pairs = getSexpBrackets();
		for (int i= 0; i < pairs.length; i += 2) {
			if (pairs[i] == c) {
				result = true;
			}
		}
		return result;
	}
	
	protected boolean isClose(char c) {
		boolean result = false;
		char[] pairs = getSexpBrackets();
		for (int i= 0; i < pairs.length; i += 2) {
			if (pairs[i+1] == c) {
				result = true;
			}
		}
		return result;
	}

	/**
	 * Determine if we can skip the character
	 * 
	 * @param c
	 * @param wordp
	 * @return true to skip
	 */
	protected boolean skipChar(char c, boolean wordp) {
		return (c <= ' ' || 
				(!Character.isJavaIdentifierPart(c) && c != CQUOTE_CHAR && 
						(wordp || (c != SQUOTE_CHAR && !isBracket(c) && !isUnbalanced(c,NONE)))));
	}

	/**
	 * Use the character pair matcher to find returns the minimal region of the document that contains both characters
	 * 
	 * @param document
	 * @param pos
	 * @return IRegions
	 */
	protected IRegion getBracketMatch(IDocument document, int pos) {
		// here we gobble entire sub expressions
		SexpCharacterPairMatcher dpm = new SexpCharacterPairMatcher(getSexpBrackets());
		IRegion reg = dpm.match(document, pos);
		dpm.dispose();
		return reg;
	}

	/**
	 * @see com.mulgasoft.emacsplus.commands.SexpHandler#getNextSexp(IDocument,ITextSelection, boolean)
	 */
	protected ITextSelection getNextSexp(IDocument document, ITextSelection currentSelection)
			throws BadLocationException {
		return getNextSexp(document, currentSelection, false);
	}
	
	/**
	 * @see com.mulgasoft.emacsplus.commands.SexpHandler#getNextSexp(IDocument,ITextSelection, boolean,int)
	 */
	protected ITextSelection getNextSexp(IDocument document, ITextSelection currentSelection, boolean wordp)
			throws BadLocationException {
		return getNextSexp(document, currentSelection, wordp, NONE);
	}

	/**
	 * Find the next sexp selection, skipping comments if appropriate
	 * 
	 * @param document
	 * @param currentSelection
	 *            - starting selection
	 * @param wordp
	 *            - advance word sexp's if true
	 * @param updown
	 *            - only advance into list if true
	 * @return the next sexp selection or null if we've fallen off the edge
	 * @throws BadLocationException
	 */
	protected ITextSelection getNextSexp(IDocument document, ITextSelection currentSelection, boolean wordp, int updown)
			throws BadLocationException {
		boolean startsInComment = false;
		int dir = getDirection();
		boolean backwardp = ((dir == FORWARD) ? false : true);
		ITextSelection selection = currentSelection;
		// get the list of comment Positions in this document
		List<Position> positions = EmacsPlusUtils.getExclusions(document, EmacsPlusUtils.COMMENT_POS);
		// determine if we are starting in the middle of a comment, so we don't jump over it
		if (positions.size() > 0) {
			if (EmacsPlusUtils.inCommentPosition(document, positions, currentSelection.getOffset(), backwardp) != null) {
				startsInComment = true;
			}
		}
		selection = advanceSexp(document, currentSelection, wordp, updown);
		if (selection != null) {
			Position pos = null;
			// jump over comments if we didn't start in it and we're not advancing by word
			if (!startsInComment && (positions.size() > 0) && !wordp
					&& ((pos = EmacsPlusUtils.inCommentPosition(document, positions, selection.getOffset()
							+ (backwardp ? 0 : selection.getLength()), backwardp)) != null)) {
				// get the next sexp after the comment
				selection =  getNextSexp(document, new TextSelection(document, pos.getOffset()
						+ ((dir == BACKWARD) ? 0 : pos.getLength()), 0), wordp, updown);
				// if edge case - return the comment as the selection
				if (selection == null && !isUnbalanced()) {
					selection = new TextSelection(document, pos.getOffset(), pos.getLength());
				}
			}
			// check for invalid selection(?)
			if (selection != null && (selection.getOffset() < 0 || selection.getOffset() + selection.getLength() > document.getLength())) {
				selection = null;
			}
		}
		return selection;
	}
	
	/**
	 * Advance one sexp (or word sexp, if wordp==true)
	 * 
	 * @param document
	 * @param selection
	 *            - starting selection
	 * @param wordp
	 *            - ignore string and bracket delimiters
	 * @param updown
	 *            - only advance into list if true
	 * @return the next sexp selection, or null if we fall off the end
	 * @throws BadLocationException
	 */
	private ITextSelection advanceSexp(IDocument document, ITextSelection selection, boolean wordp, int updown)
			throws BadLocationException {

		int dir = getDirection();
		int start = selection.getOffset();
		int cpos = start;
		int next = 0;
		char c = ' ' + 1;
		int gobble = -1;
		setUnbalanced(false);
		BreakIterator sexpWordNext = BreakIterator.getWordInstance();
		sexpWordNext.setText(document.get());
		// Go to the first boundary position by the specified position
		try {
			next = getNextPosition(document, sexpWordNext, start);
		} catch (IllegalArgumentException e) {
			// if called from console, may throw this if at doc end
			if (dir == BACKWARD && start == document.getLength()) {
				next = start+dir;
				c = document.getChar(next);
				while (next > 0 && skipChar((c = document.getChar(next)),wordp)){
					next += dir;
				}
			} else {
				throw new BadLocationException(e.getMessage());					
			}
		}
		try {
			cpos = ((dir == FORWARD) ? next - dir : next);
			c = document.getChar(cpos);
		} catch (BadLocationException e) {		}

		// Gobble any white space and (for java) most non-identifier chars
		while (skipChar(c, wordp) && (next != gobble)) {
			gobble = next;
			start = gobble;
			// Move the iterator one boundary
			try {
				next = getNextPosition(document, sexpWordNext);
			} catch (IllegalArgumentException e) {
				throw new BadLocationException(e.getMessage());
			}
			if (next == BreakIterator.DONE) {
				return null;
			}
			cpos = ((dir == FORWARD) ? next - dir : next);
			c = document.getChar(cpos);
		}
		// Emacs treats ' ' as 'brackets', which results in inappropriate behavior when used
		// in comments.  Here we preserve the positive from emacs, while fixing the negative.
		if (c == CQUOTE_CHAR) {
			// check if in a language (e.g. python) that treats ' as a string bracket 
			int spos = inString(document,dir,cpos,c);
			if (spos != cpos) {
				next = spos;
				cpos = next;
			} else {
				// step over char constant if present 
				int qpos = ((dir == FORWARD) ? cpos + dir : cpos + dir * 2);
				char tc = document.getChar(qpos);
				qpos =  cpos + dir * ((tc == QQUOTE_CHAR) ? 3 : 2); 
				tc = document.getChar(qpos);
				if (tc == CQUOTE_CHAR){
					c = tc;
					cpos = qpos;
					next = ((dir == FORWARD) ? qpos + dir : qpos);
				}
			}
		}
		if (c == SQUOTE_CHAR) {
			int spos = inString(document,dir,cpos,c);
			if (spos != cpos) {
				next = spos;
				cpos = next;
			}
		}
		if (isBracket(c)) {
			if (updown != NONE) {
				// when moving down in a sexp, fail on close encounter
				if (updown == DOWN && isClose(c)) {
					return null;
				}
			} else {
				// here we gobble entire sub expressions
				next = getBracketPosition(document, cpos + 1);
			}
		} else if (isUnbalanced(c, updown)) {
			return null;
		}
		
		TextSelection result = null;
		if (next > -1) {
			if (dir == FORWARD) {
				result = new TextSelection(document, start, next - start);
			} else {
				result = new TextSelection(document, next, start - next);
			}
		}
		return result;
	}
	
	/**
	 * Check if we're in a String (assuming the editor supports string categories)
	 *  
	 * @param document
	 * @param dir
	 * @param cpos
	 * @param stringChar
	 * @return the offset at the correct end of the string, or the current pos if not in string
	 */
	private int inString(IDocument document, int dir, int cpos, char stringChar) {
		int result = cpos;
		List<Position> positions = EmacsPlusUtils.getExclusions(document, EmacsPlusUtils.STRING_POS);
		if (positions.size() > 0) {
			Position pos;
			if ((pos = EmacsPlusUtils.inPosition(document, positions, cpos)) != null) {
				try {
					// check if we're looking at the same string syntax and get the end of the string
					if (document.get(pos.getOffset(),1).charAt(0) == stringChar) {
						result = ((dir == FORWARD) ? pos.getOffset() + pos.getLength() : pos.getOffset());
					}
				} catch (BadLocationException e) {}
			}
		}
		return result;
	}
	
	protected int unbalanced(IWorkbenchPart editor, boolean error) {
		EmacsPlusUtils.showMessage(editor, PREMATURE, error);
		return NO_OFFSET;	
	}
	
	/**
	 * @param editor
	 * @param offset
	 * @param origSelection
	 * @param selection
	 * @return NO_OFFSET
	 */
	protected int selectTransform(ITextEditor editor, int offset, ITextSelection origSelection, ITextSelection selection) {
		// we're expanding a mark selection
		int mark = getMark(editor);
		if (mark == -1 || 
				(!checkMark(mark,selection.getOffset(),selection.getLength())
						&& (origSelection != null && !checkMark(mark,origSelection.getOffset(),origSelection.getLength())))){
			try {
				executeCommand(IEmacsPlusCommandDefinitionIds.SET_MARK, null, editor);
				mark = getMark(editor);
			} catch (Exception e) {
			}
		}
		selectAndReveal(editor, offset, mark);
		return NO_OFFSET;
	}
	
	protected int noSelectTransform(ITextEditor editor, int offset, ITextSelection selection, boolean moveit) {
		// move the cursor if moveit == true	
		int newOffset = selection.getOffset();
		newOffset = (moveit ? newOffset + selection.getLength() : newOffset);
		selectAndReveal(editor, newOffset, newOffset);
		
		return NO_OFFSET;
	}
	
	protected int selectTransform(TextConsoleViewer viewer, int offset, ITextSelection origSelection, ITextSelection selection) {
		// we're expanding a mark selection
		int mark = viewer.getMark();
		if (mark != -1) {
			viewer.setSelectedRange(mark, offset - mark);
		}
		return NO_OFFSET;
	}
	
	protected int noSelectTransform(TextConsoleViewer viewer, int offset, ITextSelection selection, boolean moveit) {
		// move the cursor if moveit == true	
		int newOffset = selection.getOffset();
		newOffset = (moveit ? newOffset + selection.getLength() : newOffset);
		viewer.setSelectedRange(newOffset, 0);
		
		return NO_OFFSET;
	}

}
