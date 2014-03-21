/**
 * Copyright (c) 2009, 2010 Mark Feber, MulgaSoft
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 */
package com.mulgasoft.emacsplus.commands;

import org.eclipse.jface.text.ITextSelection;
import org.eclipse.ui.console.TextConsoleViewer;
import org.eclipse.ui.texteditor.ITextEditor;

/**
 * Implement: select-backward-balanced-expression
 * 
 * This command is normally 'hidden' by category and is not intended to 
 * be called by the user.  It is used to provide mark enabled selection behavior
 * 
 * @author Mark Feber - initial API and implementation
 */
public class SexpSelectBackwardHandler extends SexpBaseBackwardHandler {
	
	/**
	 * @see com.mulgasoft.emacsplus.commands.SexpHandler#endTransform(ITextEditor, int, ITextSelection, ITextSelection)
	 */
	protected int endTransform(ITextEditor editor, int offset, ITextSelection origSelection, ITextSelection selection) {
		// we're expanding a mark selection
		return selectTransform(editor,offset,origSelection,selection);
	}
	
	/**
	 * @see com.mulgasoft.emacsplus.commands.SexpHandler#endTransform(TextConsoleViewer, int, ITextSelection, ITextSelection)
	 */
	protected int endTransform(TextConsoleViewer viewer, int offset, ITextSelection origSelection, ITextSelection selection) {
		// we're expanding a mark selection
		return selectTransform(viewer,offset,origSelection,selection);
	}
}
