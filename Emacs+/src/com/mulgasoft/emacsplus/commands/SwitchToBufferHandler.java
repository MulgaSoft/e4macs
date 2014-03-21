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

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.common.CommandException;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.part.MultiPageEditorPart;
import org.eclipse.ui.texteditor.ITextEditor;

import com.mulgasoft.emacsplus.EmacsPlusActivator;
import com.mulgasoft.emacsplus.minibuffer.SwitchMinibuffer;
/**
 * Implement switch-to-buffer
 * 
 * @author Mark Feber - initial API and implementation
 */
public class SwitchToBufferHandler extends MinibufferExecHandler implements INonEditingCommand {

	private static final String SWITCH_PREFIX = EmacsPlusActivator.getResourceString("Switch_Buffer"); //$NON-NLS-1$ 
	private static final String FALLBACK = "org.eclipse.ui.window.openEditorDropDown"; //$NON-NLS-1$ 
	
	/**
	 * @see com.mulgasoft.emacsplus.commands.MinibufferHandler#getMinibufferPrefix()
	 */
	public String getMinibufferPrefix() {
		return SWITCH_PREFIX;
	}
	
	protected boolean isWindowCommand(){
		return true;
	}
	
	/**
	 * @see com.mulgasoft.emacsplus.commands.EmacsPlusCmdHandler#transform(org.eclipse.ui.texteditor.ITextEditor, org.eclipse.jface.text.IDocument, org.eclipse.jface.text.ITextSelection, org.eclipse.core.commands.ExecutionEvent)
	 */
	@Override
	protected int transform(ITextEditor editor, IDocument document, ITextSelection currentSelection,
			ExecutionEvent event) throws BadLocationException {
		try {
			IEditorPart eventEditor = this.getEditor(event);
			if (eventEditor != editor && eventEditor instanceof MultiPageEditorPart) {
				// this activates the page containing the editor
				((MultiPageEditorPart) eventEditor).setActiveEditor(editor);						
				IWorkbenchPage wpage = getWorkbenchPage();
				wpage.bringToTop(editor);
				wpage.activate(editor);
			}				
			return bufferTransform(new SwitchMinibuffer(this),editor, event);
		} catch (ExecutionException e) {
		}
		// don't use a separate async thread, as we may miss a typed character
		return NO_OFFSET;
	}

	@Override
	public Object checkExecute(ExecutionEvent event) {
		Object result = super.checkExecute(event); 
		if (result == Check.Fail){
			try {
				IEditorPart ed = getEditor(event);
				result = (ed == null ) ? executeCommand(FALLBACK, null) : executeCommand(FALLBACK, null, ed);
			} catch (ExecutionException e) {
			} catch (CommandException e) {
			}
		}
		return result;
	}
	
	/**
	 * Bring the part passed in minibufferResult to the top
	 *  
	 * @see com.mulgasoft.emacsplus.commands.MinibufferExecHandler#doExecuteResult(org.eclipse.ui.texteditor.ITextEditor, java.lang.Object)
	 */
	@Override
	protected boolean doExecuteResult(ITextEditor editor, Object minibufferResult) {
		if (minibufferResult != null && minibufferResult instanceof IWorkbenchPart) {
			activatePart((IWorkbenchPart)minibufferResult);
		}
		return true;
	}
	
	private void activatePart(IWorkbenchPart part) {
		IWorkbenchPage page = getWorkbenchPage();
		page.bringToTop(part);
		page.activate(part);		
	}
}
