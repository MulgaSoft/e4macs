/**
 * Copyright (c) 2009, 2010 Mark Feber, MulgaSoft
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package com.mulgasoft.emacsplus.commands;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.text.undo.DocumentUndoManagerRegistry;
import org.eclipse.text.undo.IDocumentUndoManager;
import org.eclipse.ui.console.IConsoleView;
import org.eclipse.ui.console.TextConsoleViewer;
import org.eclipse.ui.texteditor.ITextEditor;

import com.mulgasoft.emacsplus.KillRing;

/**
 * Base class for Console augmentation 
 * 
 * @author Mark Feber - initial API and implementation
 */
public abstract class ConsoleCmdHandler extends EmacsPlusCmdHandler implements IConsoleDispatch {

	abstract protected String getId(ExecutionEvent event, TextConsoleViewer viewer);
	
	/**
	 * In this context, a no-op
	 * 
	 * @see com.mulgasoft.emacsplus.commands.EmacsPlusCmdHandler#transform(ITextEditor, IDocument, ITextSelection, ExecutionEvent)
	 */
	@Override
	protected int transform(ITextEditor editor, IDocument document, ITextSelection currentSelection, ExecutionEvent event)
	throws BadLocationException {
		return NO_OFFSET;
	}

	/**
	 * @see com.mulgasoft.emacsplus.commands.IConsoleDispatch#consoleDispatch(org.eclipse.ui.console.TextConsoleViewer, IConsoleView, ExecutionEvent)
	 */
	public Object consoleDispatch(TextConsoleViewer viewer, IConsoleView activePart, ExecutionEvent event) {
		IDocument doc = viewer.getDocument();
		int action = -1;
		try {
			StyledText st = viewer.getTextWidget();
			action = getDispatchId(getId(event, viewer));
			if (action > -1) {
				// set up for kill ring
				doc.addDocumentListener(KillRing.getInstance());
//  			setUpUndo(viewer);
				st.invokeAction(action);
			}
		} finally {
			// remove kill ring behavior
			if (action > -1) {
				doc.removeDocumentListener(KillRing.getInstance());
			}
			KillRing.getInstance().setKill(null, false);
		}
		return null;
	}

	protected void setUpUndo(TextConsoleViewer viewer) {
		 IDocumentUndoManager undoer = DocumentUndoManagerRegistry.getDocumentUndoManager(viewer.getDocument());
		 if (undoer == null) {
			 DocumentUndoManagerRegistry.connect(viewer.getDocument());
//			 undoer = DocumentUndoManagerRegistry.getDocumentUndoManager(viewer.getDocument());
//			 viewer.setUndoManager((IUndoManager) undoer);
		 }
	}
	
	/**
	 * Fetch the correct dispatch id
	 *  
	 * @return return new id or -1
	 */
	protected int getDispatchId(String id) {
		Integer dispatch = dispatchCmdIds.get(id);
		if (dispatch == null) {
			dispatch = -1;
		}
		return dispatch;
	}
}
