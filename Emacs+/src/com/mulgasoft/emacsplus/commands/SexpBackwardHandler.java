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
 * @author Mark Feber - initial API and implementation
 *
 */
public class SexpBackwardHandler extends SexpBaseBackwardHandler {
	
	/**
	 * @see com.mulgasoft.emacsplus.commands.SexpHandler#endTransform(org.eclipse.ui.texteditor.ITextEditor, int, org.eclipse.jface.text.ITextSelection, org.eclipse.jface.text.ITextSelection)
	 */
	protected int endTransform(ITextEditor editor, int offset, ITextSelection origSelection,ITextSelection selection) {
		return noSelectTransform(editor,offset,selection,false);
	}
	
	protected int endTransform(TextConsoleViewer viewer, int offset, ITextSelection origSelection,ITextSelection selection) {
		return noSelectTransform(viewer,offset,selection,false);
	}
}
