/**
 * Copyright (c) 2009, 2014 Mark Feber, MulgaSoft
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package com.mulgasoft.emacsplus.commands;

import static com.mulgasoft.emacsplus.EmacsPlusUtils.showMessage;

import org.eclipse.core.commands.Command;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.common.NotDefinedException;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.ui.commands.ICommandService;
import org.eclipse.ui.texteditor.ITextEditor;

import com.mulgasoft.emacsplus.EmacsPlusActivator;
import com.mulgasoft.emacsplus.execute.RepeatCommandSupport;

/**
 * Implement repeat:
 *
 * Repeat most recently executed command. With prefix arg, apply new prefix arg to that command; otherwise,
 * use the prefix arg that was used before (if any).
 *
 * Since Eclipse does not really support dynamically adding and removing bindings we enable a context which has
 * a single key binding associated with it (default Z) which will repeat until a non-repeat command is invoked.
 * 
 * @author mfeber - Initial API and implementation
 */
public class RepeatHandler extends EmacsPlusCmdHandler {

	private final static String REPEAT = EmacsPlusActivator.getResourceString("Repeat_Command");   	   //$NON-NLS-1$
	private final static String UREPEAT = EmacsPlusActivator.getResourceString("Repeat_UCommand");   	   //$NON-NLS-1$

	/* 
	 * @see com.mulgasoft.emacsplus.commands.EmacsPlusCmdHandler#transform(org.eclipse.ui.texteditor.ITextEditor, org.eclipse.jface.text.IDocument, org.eclipse.jface.text.ITextSelection, org.eclipse.core.commands.ExecutionEvent)
	 */
	@Override
	protected int transform(ITextEditor editor, IDocument document, ITextSelection currentSelection, ExecutionEvent event) throws BadLocationException {
		RepeatCommandSupport repeat = RepeatCommandSupport.getInstance();
		String id = repeat.getId();

		try {
			Integer count = repeat.getCount();
			Command c = ((ICommandService)editor.getSite().getService(ICommandService.class)).getCommand(id);
			showMessage(editor, (count != 1) ? String.format(UREPEAT, count, c.getName()) : String.format(REPEAT, c.getName()), false);
		} catch (NotDefinedException e) {
			// won't happen
		}		
		Object result = NO_OFFSET;
		result = repeatLast(editor, id, repeat.getParams());
		return (result instanceof Integer ? (Integer)result : NO_OFFSET);
	}

	@Override
	protected boolean isLooping() {
		return false;
	}

}
