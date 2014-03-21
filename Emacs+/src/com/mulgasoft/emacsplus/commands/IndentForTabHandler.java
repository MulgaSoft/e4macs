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
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.common.CommandException;
import org.eclipse.jface.bindings.Binding;
import org.eclipse.jface.bindings.keys.KeySequence;
import org.eclipse.jface.bindings.keys.KeyStroke;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.Position;
import org.eclipse.swt.SWT;
import org.eclipse.ui.keys.IBindingService;
import org.eclipse.ui.texteditor.ITextEditor;

import com.mulgasoft.emacsplus.EmacsPlusUtils;
import com.mulgasoft.emacsplus.IEmacsPlusCommandDefinitionIds;
import com.mulgasoft.emacsplus.execute.ColumnSupport;

/**
 * Implement a generic indent handler: indent-for-tab
 * Looks for an indent command bound to ^I and invokes it.
 * If no command found or if called with ^U rigidly indent with tab (or tabWidth spaces)
 * 
 * @author Mark Feber - initial API and implementation
 */
public class IndentForTabHandler extends EmacsPlusCmdHandler {

	// TODO: this is the usual case for an emacs binding, but could be different
	// so potentially set these up as preferences 
	private static char indentKey = 'i';
	private static int indentMode = (EmacsPlusUtils.isMac() ? SWT.COMMAND : SWT.CTRL);
	private static String indentExp = ".*[Ii]ndent.*";	//$NON-NLS-1$ 
	
	@Override
	protected boolean isLooping() {
		return false;
	}

	@Override
	protected boolean undoProtect() {
		// force undo wrapping when transform is called
		return true;
	}

	/**
	 * @see com.mulgasoft.emacsplus.commands.EmacsPlusCmdHandler#execute(org.eclipse.core.commands.ExecutionEvent)
	 */
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		Object result = null;
		ITextEditor editor = getTextEditor(event);
		extractUniversalCount(event);		// side effect sets up isUniversalPresent
		Binding indent = null;
		// ^U or no binding results in transform behavior
		if (!isUniversalPresent() && (indent = getBinding(editor,indentKey,indentMode)) != null) {
			String id = indent.getParameterizedCommand().getId(); 
			if (id != null && id.matches(indentExp) && !id.equals(IEmacsPlusCommandDefinitionIds.INDENT_FOR_TAB)) {
				try {
					result = executeCommand(id, null, editor);
				} catch (CommandException e) {
				}
			} else {
				super.execute(event);
			}
		} else {
			super.execute(event);
		}
		return result;
	}

	/**
	 * Insert tab/spaces at selection offset and each subsequent line origin
	 * 
	 * @see com.mulgasoft.emacsplus.commands.EmacsPlusCmdHandler#transform(ITextEditor, IDocument, ITextSelection, ExecutionEvent)
	 */
	@Override
	protected int transform(ITextEditor editor, IDocument document, ITextSelection currentSelection, ExecutionEvent event)
	throws BadLocationException {
		// if we're here, either ^U or no relevant ^I
		ColumnSupport cs = new ColumnSupport(document,editor); 
		String tab = cs.getSpaces(0,cs.getTabWidth());
		int off = currentSelection.getOffset();
		Position coff = new Position(getCursorOffset(editor,currentSelection),0);
		try {
			document.addPosition(coff);
			int begin = document.getLineOfOffset(off);
			int end = document.getLineOfOffset(off+ currentSelection.getLength());
			if (begin != end) {
				while (++begin <= end) {
					document.replace(document.getLineOffset(begin), 0, tab);
				}
			}
			document.replace(off, 0, tab);
		} finally {
			document.removePosition(coff);
		} 
		return coff.offset;
	}

	/**
	 * Return the exact binding if it exists and is enabled, else null
	 * 
	 * @param editor
	 * @param keyCode
	 * @param mode
	 * @return binding if it exists and is enabled, else null
	 */
	private Binding getBinding(ITextEditor editor, char keyCode, int mode) {

		Binding result = null;
		// ensure key is upper case
		IBindingService bindingService = (IBindingService) editor.getSite().getService(IBindingService.class);
		KeyStroke key = KeyStroke.getInstance(mode, Character.toUpperCase(keyCode));
		result = bindingService.getPerfectMatch(KeySequence.getInstance(key));
		if (result != null && !result.getParameterizedCommand().getCommand().isEnabled()) {
			result = null;
		}
		return result;
	}
}
