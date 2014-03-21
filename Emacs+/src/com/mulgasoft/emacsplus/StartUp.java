/**
 * Copyright (c) 2009, 2010 Mark Feber, MulgaSoft
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 */
package com.mulgasoft.emacsplus;

import org.eclipse.ui.IStartup;
import org.eclipse.ui.PlatformUI;

/**
 * Register the necessary listeners to support:
 * 	Kill Ring behavior
 * @author Mark Feber - initial API and implementation
 */
public class StartUp implements IStartup  {

	/**
	 * @see org.eclipse.ui.IStartup#earlyStartup()
	 */
	//@Override
	public void earlyStartup() {

		// get a workbench page to set up global editor listeners
		PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() {
			public void run() {
				EmacsPlusActivation.getInstance().activateListeners();
			}
		});
	}
	
	
}
