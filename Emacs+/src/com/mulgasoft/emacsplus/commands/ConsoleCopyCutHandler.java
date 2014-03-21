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
import org.eclipse.jface.text.IDocument;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.console.IConsoleView;
import org.eclipse.ui.console.TextConsoleViewer;
import org.eclipse.ui.handlers.IHandlerService;

import com.mulgasoft.emacsplus.IEmacsPlusCommandDefinitionIds;
import com.mulgasoft.emacsplus.KillRing;
import com.mulgasoft.emacsplus.MarkUtils;

/**
 * Implements: Cut & Copy for the console
 * 
 * @author Mark Feber - initial API and implementation
 */
public class ConsoleCopyCutHandler extends ConsoleCmdHandler {

	/**
	 * @see com.mulgasoft.emacsplus.commands.ConsoleCmdHandler#getId(ExecutionEvent, TextConsoleViewer)
	 */
	@Override
	protected String getId(ExecutionEvent event, TextConsoleViewer viewer) {
		String result = null;
		if (IEmacsPlusCommandDefinitionIds.CONSOLE_CUT.equals(event.getCommand().getId())){
			result = IEmacsPlusCommandDefinitionIds.EMP_CUT;
			KillRing.getInstance().setKill(result, false);
		} else if (IEmacsPlusCommandDefinitionIds.CONSOLE_COPY.equals(event.getCommand().getId())){
			result = IEmacsPlusCommandDefinitionIds.EMP_COPY;
		}
		return result;
	}

	/**
	 * @see com.mulgasoft.emacsplus.commands.ConsoleCmdHandler#consoleDispatch(TextConsoleViewer, IConsoleView, ExecutionEvent)
	 */
	@Override
	public Object consoleDispatch(TextConsoleViewer viewer, IConsoleView activePart, ExecutionEvent event) {
		Object result = null;
		IDocument doc = viewer.getDocument();
		try {
			IWorkbenchPartSite site = activePart.getSite();
			if (site != null) {
				IHandlerService service = (IHandlerService) site.getService(IHandlerService.class);
				if (doc != null && service != null) {
					doc.addDocumentListener(KillRing.getInstance());
					String cmdId = getId(event, viewer);
					if (cmdId != null) {
						result = service.executeCommand(cmdId, null);
					}
				}
			}
		} catch (CommandException e) {
			// Shouldn't happen as the Command id will be null or valid
			e.printStackTrace();
		} finally {
			if (doc != null) {
				doc.removeDocumentListener(KillRing.getInstance());
			}
			// clear kill command flag
			KillRing.getInstance().setKill(null, false);
		}
		
		MarkUtils.clearConsoleMark(viewer);
		return result;
	}
}
