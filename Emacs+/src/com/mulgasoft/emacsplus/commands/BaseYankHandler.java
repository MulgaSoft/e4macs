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
import org.eclipse.core.commands.common.CommandException;
import org.eclipse.debug.ui.console.IConsole;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.console.IConsoleView;
import org.eclipse.ui.console.TextConsoleViewer;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.handlers.IHandlerService;

import com.mulgasoft.emacsplus.IEmacsPlusCommandDefinitionIds;
import com.mulgasoft.emacsplus.KillRing;

/**
 * @author Mark Feber - initial API and implementation
 *
 */
public abstract class BaseYankHandler extends EmacsPlusCmdHandler implements IConsoleDispatch {

	// Console based commands do not have an editor, so get the eol directly from the widget
	protected String widgetEol = null;

	protected abstract int yankIt(IDocument document, ITextSelection selection) throws BadLocationException; 
	protected abstract void paste(ExecutionEvent event, StyledText widget, boolean isProcess);
	
	protected String getLineDelimiter() {
		String result = widgetEol;
		if (result == null) {
			result = super.getLineDelimiter();
		} 
		return result;
	}

	/**
	 * When called from a console context, use paste
	 * 
	 * @see com.mulgasoft.emacsplus.commands.IConsoleDispatch#consoleDispatch(org.eclipse.ui.console.TextConsoleViewer, org.eclipse.ui.console.IConsoleView, org.eclipse.core.commands.ExecutionEvent)
	 */
	public Object consoleDispatch(TextConsoleViewer viewer, IConsoleView activePart, ExecutionEvent event) {

		StyledText st = viewer.getTextWidget();
		try {
			// set directly from the widget
			widgetEol = st.getLineDelimiter();
			paste(event,st,activePart.getConsole() instanceof IConsole);
		} finally {
			st.redraw();
			widgetEol = null;
		}
		
		return null;
	}

	/**
	 * In the console context, use paste as
	 * in some consoles (e.g. org.eclipse.debug.internal.ui.views.console.ProcessConsole), updateText
	 * will not simulate keyboard input
	 *  
	 * @param event the ExecutionEvent
	 * @param widget The consoles StyledText widget
	 */
	protected void paste(ExecutionEvent event, StyledText widget) {
			IWorkbenchPart apart = HandlerUtil.getActivePart(event);
			if (apart != null) {
				try {
					IWorkbenchPartSite site = apart.getSite();
					if (site != null) {
						IHandlerService service = (IHandlerService) site.getService(IHandlerService.class);
						if (service != null) {
							service.executeCommand(IEmacsPlusCommandDefinitionIds.EMP_PASTE, null);
							KillRing.getInstance().setYanked(true);
						}
					}
				} catch (CommandException e) {
				}
			}
	}
	
	/**
	 * Conditionally remove all EOLs from the end of the paste string
	 * 
	 * @param text
	 * @param stripEol
	 * @return String
	 */
	protected String convertDelimiters(String text, boolean stripEol) {
		String result = super.convertDelimiters(text);
		int len;
		if (stripEol && text != null && (len = text.length()) > 0) {
			for (int i = len-1; i >= 0; i--) {
				char c = text.charAt(i);
				if (c == SWT.CR) {
					continue;
				} else if (c == SWT.LF) {
					continue;
				}
				if (i+1 != len) {
					result = text.substring(0, i+1);	
				}
				break;
			}
		}
		return result;
	}

}
