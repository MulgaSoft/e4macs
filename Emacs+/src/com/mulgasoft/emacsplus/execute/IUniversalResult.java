/**
 * Copyright (c) 2009, 2010 Mark Feber, MulgaSoft
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 */
package com.mulgasoft.emacsplus.execute;


/**
 * Return interface for the universal-argument minibuffer
 * 
 * @author Mark Feber - initial API and implementation
 */
public interface IUniversalResult extends IBindingResult {

	/**
	 * @return the count that was entered (or multiple of 4, if plain)
	 */
	int getCount();
	
	/**
	 * @return true if number was entered, false for single ^U
	 */
	boolean isNumeric();
}
