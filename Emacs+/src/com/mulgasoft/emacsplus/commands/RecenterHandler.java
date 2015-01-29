/**
 * Copyright (c) 2009, 2010 Mark Feber, MulgaSoft
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package com.mulgasoft.emacsplus.commands;

import static com.mulgasoft.emacsplus.IEmacsPlusCommandDefinitionIds.RECENTER_TOP_BOTTOM;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.console.IConsoleView;
import org.eclipse.ui.console.TextConsoleViewer;
import org.eclipse.ui.texteditor.ITextEditor;

import com.mulgasoft.emacsplus.MarkUtils;
import com.mulgasoft.emacsplus.MarkUtils.ICommandIdListener;

/**
 * Emacs recenter-top-bottom
 * 
 * Move current line to window center, top, and bottom, successively.
 * With no prefix argument, the first call redraws the frame and
 *  centers point vertically within the window.  Successive calls
 *  scroll the window, placing point on the top, bottom, and middle
 *  consecutively.  The cycling order is middle -> top -> bottom.
 * 
 * A prefix argument is handled like (Emacs)`recenter':
 *  With numeric prefix arg, move current line to window-line arg.
 * 
 * @author Mark Feber - initial API and implementation
 */
public class RecenterHandler extends EmacsPlusNoEditHandler implements IConsoleDispatch, ICommandIdListener {
	// not supported: With plain `C-u', move current line to window center.

	private interface RecenterState {
		int getLine(int caretLine, int areaHeight, int lineHeight);
	}
	
	private static final RecenterState tState = new RecenterState() {
		public int getLine(int caretLine, int areaHeight, int lineHeight) {
			recenterState = bState;
			return Math.max(0, (caretLine - (areaHeight / lineHeight)+1));
		}
	};
	private static final RecenterState cState = new RecenterState() {
		public int getLine(int caretLine, int areaHeight, int lineHeight) {
			recenterState = tState;
			return caretLine;
		}
	};
	private static final RecenterState bState = new RecenterState() {
		public int getLine(int caretLine, int areaHeight, int lineHeight) {
			recenterState = cState;
			return Math.max(0, (caretLine - (areaHeight / (lineHeight * 2)))); 
		}
	};

	private static RecenterState recenterState = cState;
	
	@Override
	protected boolean isLooping() {
		return false;
	}

	protected int transform(ITextEditor editor, IDocument document, ITextSelection currentSelection,
			ExecutionEvent event) throws BadLocationException {
		Control widget = getTextWidget(editor);
		setCommandId(MarkUtils.getLastCommandId());
		if (widget instanceof StyledText) {
			recenter((StyledText)widget);
		}
		return NO_OFFSET;
	}

	private void recenter(StyledText txtWidget) {
		int topLine = -1;
		int caretLine= txtWidget.getLineAtOffset(txtWidget.getCaretOffset());

		if (isUniversalPresent()) {
			// ^U arg lines from top
			topLine = Math.min(txtWidget.getLineCount()-1, Math.max(0, (caretLine - getUniversalCount())));
			recenterState = bState;				// so next unadorned re-center will center
		} else {
			int areaHeight= txtWidget.getClientArea().height;
			int lineHeight= txtWidget.getLineHeight();
			topLine = recenterState.getLine(caretLine, areaHeight, lineHeight);
		}
		if (topLine >= 0) {
			txtWidget.setTopIndex(topLine);
		}
	}
	
	/**
	 * @see com.mulgasoft.emacsplus.commands.IConsoleDispatch#consoleDispatch(TextConsoleViewer, IConsoleView, ExecutionEvent)
	 */
	public Object consoleDispatch(TextConsoleViewer viewer, IConsoleView activePart, ExecutionEvent event) {
		RecenterState saveState = recenterState;
		try {
			StyledText st = viewer.getTextWidget();
			st.redraw();
			recenter(st);
		} finally {
			recenterState = saveState;
		}
		return null;
	}

	/**
	 * We don't actually set up the listener, as it's unnecessarily expensive
	 * 
	 * @see com.mulgasoft.emacsplus.MarkUtils.ICommandIdListener#setCommandId(java.lang.String)
	 */
	public void setCommandId(String commandId) {
		if (!RECENTER_TOP_BOTTOM.equals(commandId)) {
			// so re-center will center
			recenterState = bState;
		}
	}

}
