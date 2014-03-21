/**
 * Copyright (c) 2009, 2010 Mark Feber, MulgaSoft
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package com.mulgasoft.emacsplus.minibuffer;

import org.eclipse.ui.texteditor.ITextEditor;

/**
 * An interface to use for generic minibuffer state objects when multiple
 * minibuffers are required from a single handler
 *  
 * @author Mark Feber - initial API and implementation
 */
public interface IMinibufferState {

	int run(ITextEditor editor);
	String getMinibufferPrefix();	
	boolean executeResult(ITextEditor editor, Object minibufferResult);

}
