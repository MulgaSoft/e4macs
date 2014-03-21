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

import org.eclipse.jface.bindings.Binding;
import org.eclipse.jface.bindings.keys.KeySequence;

/**
 * A minibuffer result interface for key binding information
 * 
 * @author Mark Feber - initial API and implementation
 */
public interface IBindingResult {
	
	/**
	 * Get the Binding object if there was a perfect match
	 * 
	 * @return the Binding or null 
	 */
	Binding getKeyBinding();
	
	/**
	 * Get the printable representation of the key sequence
	 * 
	 * @return the printable representation of the key sequence 
	 */
	String getKeyString();
	
	/**
	 * Get the actual trigger sequence entered
	 * 
	 * @return the KeySequence
	 */
	KeySequence getTrigger();
}
