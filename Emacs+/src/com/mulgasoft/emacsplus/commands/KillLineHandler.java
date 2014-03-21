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

import static com.mulgasoft.emacsplus.EmacsPlusUtils.getPreferenceBoolean;
import static com.mulgasoft.emacsplus.EmacsPlusUtils.getPreferenceStore;
import static com.mulgasoft.emacsplus.preferences.PrefVars.KILL_WHOLE_LINE;
import static org.eclipse.ui.texteditor.ITextEditorActionDefinitionIds.CUT_LINE;
import static org.eclipse.ui.texteditor.ITextEditorActionDefinitionIds.CUT_LINE_TO_END;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.ui.console.IConsoleView;
import org.eclipse.ui.console.TextConsoleViewer;
import org.eclipse.ui.texteditor.ITextEditor;

import com.mulgasoft.emacsplus.IEmacsPlusCommandDefinitionIds;
import com.mulgasoft.emacsplus.KillRing;

/**
 * Version of CUT_LINE_TO_END for use with universal-argument to replicate Emacs semantics of
 * kill-line
 * 
 * @author Mark Feber - initial API and implementation
 */
public class KillLineHandler extends ConsoleCmdHandler {

	private static boolean WHOLE_LINE = getPreferenceBoolean(KILL_WHOLE_LINE.getPref());
	
	static {
		// listen for changes in the property store
		getPreferenceStore().addPropertyChangeListener(
				new IPropertyChangeListener() {
					public void propertyChange(PropertyChangeEvent event) {
						if (KILL_WHOLE_LINE.getPref().equals(event.getProperty())) {
							setKillWholeLine((Boolean)event.getNewValue());
						}
					}
				}
		);
	}

	public static void setKillWholeLine(boolean val) {
		WHOLE_LINE = val;
	}
	
	public static boolean isKillWholeLine() {
		return WHOLE_LINE;
	}
	
	/**
	 * @see com.mulgasoft.emacsplus.commands.EmacsPlusCmdHandler#transform(ITextEditor, IDocument,
	 *      ITextSelection, ExecutionEvent)
	 */
	@Override
	protected int transform(ITextEditor editor, IDocument document, ITextSelection currentSelection,
			ExecutionEvent event) throws BadLocationException {
		int uArg = getUniversalCount();
		int offset = getCursorOffset(editor, currentSelection);
		int offsetLine = document.getLineOfOffset(offset);
		if (uArg == 1) {
			try {
				// gnu emacs: If the variable `kill-whole-line' is non-`nil', `C-k' at the very
				// beginning of a line kills the entire line including the following newline.
				boolean killWhole = isKillWholeLine() && (offset == document.getLineOffset(offsetLine));
				executeCommand(killWhole ? CUT_LINE : CUT_LINE_TO_END, null, editor);
			} catch (Exception e) {}
		} else {
			try {
				// flag us as a kill command
				KillRing.getInstance().setKill(IEmacsPlusCommandDefinitionIds.KILL_LINE, false);
				int lastOffset = offset;
				// note that line numbers start from 0
				int maxLine = document.getNumberOfLines() - 1;
				int endLine = uArg + document.getLineOfOffset(offset);
				// if range includes eof
				if (endLine >= maxLine) {
					// delete through to last character
					lastOffset = document.getLineOffset(maxLine) + document.getLineLength(maxLine);
				} else {
					// delete by whole lines
					lastOffset = document.getLineOffset(Math.min(Math.max(endLine, 0), maxLine));
				}
				updateText(document, ((lastOffset >= offset ? offset : lastOffset)), Math.abs(lastOffset - offset),
						EMPTY_STR);
			} finally {
				// clear kill command flag
				KillRing.getInstance().setKill(null, false);
			}
		}
		return NO_OFFSET;
	}

	/**
	 * When called from a console context, will use ST.CUT
	 * 
	 * @see com.mulgasoft.emacsplus.commands.ConsoleCmdHandler#consoleDispatch(TextConsoleViewer,
	 *      IConsoleView, ExecutionEvent)
	 */
	public Object consoleDispatch(TextConsoleViewer viewer, IConsoleView activePart, ExecutionEvent event) {
		if (viewer.isEditable()) {
			IDocument doc = viewer.getDocument();
			StyledText st = viewer.getTextWidget();
			int offset = st.getCaretOffset();
			try {
				IRegion info = doc.getLineInformationOfOffset(offset);
				int noffset = info.getOffset() + info.getLength();
				if (offset == noffset) {
					int line = doc.getLineOfOffset(offset);
					if (++line < doc.getNumberOfLines()) {
						noffset = doc.getLineOffset(line);
						if (noffset == doc.getLength()) {
							noffset = offset;
						}
					}
				}
				if (offset != noffset) {
					st.redraw();
					st.setSelection(offset, noffset);
					KillRing.getInstance().setKill(CUT_LINE_TO_END, false);
					return super.consoleDispatch(viewer, activePart, event);
				}
				viewer.refresh();
			} catch (BadLocationException e) {
			}
		}
		return null;
	}

	/**
	 * @see com.mulgasoft.emacsplus.commands.EmacsPlusCmdHandler#isLooping()
	 */
	protected boolean isLooping() {
		return false;
	}

	/**
	 * CID for CUT to simulate CUT_TO_LINE_END in console
	 * 
	 * @see com.mulgasoft.emacsplus.commands.ConsoleCmdHandler#getId(org.eclipse.core.commands.ExecutionEvent,
	 *      org.eclipse.ui.console.TextConsoleViewer)
	 */
	protected String getId(ExecutionEvent event, TextConsoleViewer viewer) {
		return IEmacsPlusCommandDefinitionIds.EMP_CUT;
	}
}
