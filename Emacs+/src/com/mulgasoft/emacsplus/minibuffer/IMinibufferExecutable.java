/**
 * Copyright (c) 2009, 2010 Mark Feber, MulgaSoft
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 */
package com.mulgasoft.emacsplus.minibuffer;

import org.eclipse.ui.texteditor.ITextEditor;

/**
 * An interface used by minibuffer commands to execute against their handler
 * 
 * @author Mark Feber - initial API and implementation
 */
public interface IMinibufferExecutable {
	
	/**
	 * Return the handler specific prefix string 
	 *  
	 * @return the prefix string
	 */
	String getMinibufferPrefix();
	/**
	 * Perform the desired action on the result of the minibuffer input
	 * 
	 * @param editor
	 * @param minibufferResult usually a String
	 *  
	 * @return true if we should exit after the execution
	 */
	boolean executeResult(ITextEditor editor, Object minibufferResult);
	
	/**
	 * Set the result message to be displayed by the handler on completion
	 * 
	 * @param resultMessage the message to display
	 * @param resultError true if an error result, else false
	 */
	void setResultMessage(String resultMessage, boolean resultError);
	
	/**
	 * Return the universal-argument state of the handler
	 * 
	 * @return true if called with an explicit universal-argument, else false
	 */
	boolean isUniversalPresent();
}
