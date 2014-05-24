/**
 * Copyright (c) 2009, 2014 Mark Feber, MulgaSoft
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package com.mulgasoft.emacsplus.e4.commands;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.ui.texteditor.ITextEditor;

import com.mulgasoft.emacsplus.commands.INonEditingCommand;

/**
 * Group the E4 Window commands
 * 
 * @author mfeber - Initial API and implementation
 * @param <T>
 */
public abstract class E4WindowHandler<T> extends E4CmdHandler<T>  implements INonEditingCommand {
	
	public E4WindowHandler(Class<T> clazz) {
		super(clazz);
	}	
	
	@Override
	// A non-edit command never modifies the offset
	protected int transform(ITextEditor editor, IDocument document, ITextSelection currentSelection,
			ExecutionEvent event) throws BadLocationException {
		return NO_OFFSET;
	}

}
