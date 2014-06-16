/**
 * Copyright (c) 2009, 2014 Mark Feber, MulgaSoft
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package com.mulgasoft.emacsplus.e4.commands;

import javax.inject.Inject;

import org.eclipse.e4.ui.workbench.modeling.EModelService;
import org.eclipse.e4.ui.workbench.modeling.EPartService;

/**
 * Common base class for E4 commands
 * 
 * @author mfeber - Initial API and implementation
 */
public abstract class E4Cmd {
	
	@Inject protected EPartService partService;
	@Inject protected EModelService modelService;
}
