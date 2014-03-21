/**
n * Copyright (c) 2009, 2010 Mark Feber, MulgaSoft
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package com.mulgasoft.emacsplus.execute;

import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsolePageParticipant;
import org.eclipse.ui.contexts.IContextActivation;
import org.eclipse.ui.contexts.IContextService;
import org.eclipse.ui.part.IPageBookViewPage;

/**
 * Emacs+ console activation/deactivation
 * 
 * @author Mark Feber - initial API and implementation
 */
public class EmacsPlusConsoleParticipant implements
		IConsolePageParticipant {

	private IContextActivation fContextActivation;
	private IConsole console = null;
	
	/**
	 * @see org.eclipse.ui.console.IConsolePageParticipant#activated()
	 */
	public void activated() {
        IContextService contextService = (IContextService) PlatformUI.getWorkbench().getAdapter(IContextService.class);
        fContextActivation = contextService.activateContext("org.eclipse.ui.textEditorScope"); //$NON-NLS-1$
        ((EmacsPlusConsole)console).onLine();
	}
	
	/**
	 * @see org.eclipse.ui.console.IConsolePageParticipant#deactivated()
	 */
	public void deactivated() {
        if (fContextActivation != null){
        	IContextService contextService = (IContextService) PlatformUI.getWorkbench().getAdapter(IContextService.class);
        	contextService.deactivateContext(fContextActivation);
        	fContextActivation = null;
        	((EmacsPlusConsole)console).offLine();
        }
	}

	/**
	 * @see org.eclipse.ui.console.IConsolePageParticipant#dispose()
	 */
	public void dispose() {
		this.console = null;
	}

	/**
	 * @see org.eclipse.ui.console.IConsolePageParticipant#init(org.eclipse.ui.part.IPageBookViewPage, org.eclipse.ui.console.IConsole)
	 */
	public void init(IPageBookViewPage page, IConsole console) {
		this.console = console;
	}

	/**
	 * @see org.eclipse.core.runtime.IAdaptable#getAdapter(java.lang.Class)
	 */
	public Object getAdapter(@SuppressWarnings("rawtypes") Class adapter) {
		return null;
	}
}
