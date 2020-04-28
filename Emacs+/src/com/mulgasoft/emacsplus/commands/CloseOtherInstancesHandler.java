/**
 * Copyright (c) 2009, 2020 Mark Feber, MulgaSoft
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 */
package com.mulgasoft.emacsplus.commands;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.handlers.HandlerUtil;

/**
 * Remove all the split windows
 * 
 * @author Mark Feber - initial API and implementation
 */
public class CloseOtherInstancesHandler extends EmacsPlusNoEditHandler implements IDeduplication {

	/**
	 * Execute directly
	 * 
	 * @see com.mulgasoft.emacsplus.commands.EmacsPlusCmdHandler#execute(org.eclipse.core.commands.ExecutionEvent)
	 */
	@Override
	public Object execute(ExecutionEvent event) {
		try {
			IWorkbenchWindow window = HandlerUtil.getActiveWorkbenchWindowChecked(event);
			IWorkbenchPage page = window.getActivePage();
			closeOtherInstances(page);
		} catch (ExecutionException e) {
		} catch (PartInitException e) {
		} catch (Exception e) {
		}
		return null;
	}
	
    /**
	 * Close all other editors on the current editor's input
	 * 
	 * @param page
	 * @throws PartInitException
	 */
	protected void closeOtherInstances(IWorkbenchPage page) throws PartInitException {
		if (page != null) {
			IEditorReference active = (IEditorReference) page.getReference(page.getActiveEditor());
			// Close the others, and guarantee proper (emacs+) activation
			IDeduplication.closeInstances(page, IDeduplication.getOtherInstances(active, page.getEditorReferences(), 0), active);
		}
	}
}
