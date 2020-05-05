/**
 * Copyright (c) 2009, 2020 Mark Feber, MulgaSoft
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package com.mulgasoft.emacsplus.commands;

import static com.mulgasoft.emacsplus.EmacsPlusUtils.getPreferenceInt;
import static com.mulgasoft.emacsplus.EmacsPlusUtils.getPreferenceStore;
import static com.mulgasoft.emacsplus.IEmacsPlusCommandDefinitionIds.RECENTER_TOP_BOTTOM;
import static com.mulgasoft.emacsplus.preferences.PrefVars.SCROLL_MARGIN;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
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
 * If scroll-margin > 0 then leave that number of lines as a margin
 * 
 * A prefix argument is handled like (Emacs)`recenter':
 *  - With numeric prefix arg, move current line to screen line arg relative to the 
 *    current window.  If arg is negative, count up from the bottom of the window.
 *  - With plain `C-u', move current line to window center.
 *  
 * @author Mark Feber - initial API and implementation
 */
public class RecenterHandler extends EmacsPlusNoEditHandler implements IConsoleDispatch, ICommandIdListener {

	static int scrollMargin = 0;
	static {
		// initialize the scroll margin from our properties
		setScrollMargin(getPreferenceInt(SCROLL_MARGIN.getPref()));
		// listen for changes in the property store
		getPreferenceStore().addPropertyChangeListener(
				new IPropertyChangeListener() {
					public void propertyChange(PropertyChangeEvent event) {
						if (SCROLL_MARGIN.getPref().equals(event.getProperty())) {
							setScrollMargin((Integer)event.getNewValue());
						}
					}
				}
		);
	}	
	
	public static void setScrollMargin(int sm) {
		scrollMargin = (sm < 0 ? 0 : sm);
	}
	
	private CS state = CS.C;

	// Center, Top, Bottom
	private static enum CS {
		C,T,B;
		private CS next;
		static {
			C.next = T;
			T.next = B;
			B.next = C;
		}
		int argLines = 0;

		/**
		 * Recenter the widget:
		 *    - cycle through center/top/bottom
		 *    - with ^U arg, position arg lines from top
		 *    - with ^U, center regardless of cycle
		 *    - only scroll within the current view area 
		 * 
		 * @param widget our StyledText widget
		 * @param withArg true if called with some variant of ^U
		 * @return the next recenter state in sequence
		 */
		private CS recenter(StyledText widget, boolean withArg) {
			int topLine = -1;
			int caretLine= widget.getLineAtOffset(widget.getCaretOffset());
			int areaLines = (widget.getClientArea().height / widget.getLineHeight()); 
			if (withArg) {
				int newLine = caretLine - argLines;
				// ensure we never scroll out of the currently displayed area
				if (argLines < 0) {
					topLine = Math.min(newLine - areaLines, caretLine);
				} else {
					topLine = Math.max(caretLine - areaLines, newLine);
				}
			} else {
				switch (this) {
					case B:
						// ensure we never scroll out of the currently displayed area
						topLine = Math.min((caretLine - areaLines + (scrollMargin > 0 ? scrollMargin : 1)), caretLine);
						break;
					case T:
						// ensure we never scroll out of the currently displayed area
						topLine =  Math.max((caretLine - scrollMargin), (caretLine - areaLines + 1));
						break;
					case C:
					default:
						topLine =  caretLine - (areaLines / 2); 
				}
			}
			widget.setTopIndex(Math.max(0, topLine));
			return next;
		}
	}

	protected int transform(ITextEditor editor, IDocument document, ITextSelection currentSelection,
			ExecutionEvent event) throws BadLocationException {
		setCommandId(MarkUtils.getLastCommandId());
		Control widget = getTextWidget(editor);
		if (widget instanceof StyledText) {
			recenter((StyledText)widget);
		}
		return NO_OFFSET;
	}

	private void recenter(StyledText txtWidget) {
		if (isUniversalPresent()) {
			state = CS.C;
			if (isNumericUniversal()) {
				// ^U arg lines from top (or bottom on -)
				state.argLines = getUniversalCount();
				state = state.recenter(txtWidget, true);
			} else {
				// Naked ^U always centers
				state = state.recenter(txtWidget, false);
			}
		} else {
			state = state.recenter(txtWidget, false);
		}
	}
	
	/**
	 * @see com.mulgasoft.emacsplus.commands.IConsoleDispatch#consoleDispatch(TextConsoleViewer, IConsoleView, ExecutionEvent)
	 */
	public Object consoleDispatch(TextConsoleViewer viewer, IConsoleView activePart, ExecutionEvent event) {
		CS saveState = state;
		try {
			state = CS.C;
			StyledText st = viewer.getTextWidget();
			st.redraw();
			recenter(st);
		} finally {
			state = saveState;
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
			// always start from the center
			state = CS.C;
		}
	}

	@Override
	protected boolean isLooping() {
		return false;
	}	
}
