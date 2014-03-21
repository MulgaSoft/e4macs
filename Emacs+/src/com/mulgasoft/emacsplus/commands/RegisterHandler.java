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
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.ui.texteditor.ITextEditor;

import com.mulgasoft.emacsplus.EmacsPlusActivator;
import com.mulgasoft.emacsplus.minibuffer.RegisterMinibuffer;

/**
 * Base class for Register commands
 * 
 * @author Mark Feber - initial API and implementation
 */
public abstract class RegisterHandler extends MinibufferExecHandler {

	protected static final String APPEND_PREFIX = EmacsPlusActivator.getResourceString("Register_Append_Prefix");	//$NON-NLS-1$
	protected static final String COPY_PREFIX = EmacsPlusActivator.getResourceString("Register_Copy_Prefix");	//$NON-NLS-1$
	protected static final String INCREMENT_PREFIX = EmacsPlusActivator.getResourceString("Register_Increment_Prefix");	//$NON-NLS-1$
	protected static final String INSERT_PREFIX = EmacsPlusActivator.getResourceString("Register_Insert_Prefix");	//$NON-NLS-1$
	protected static final String JUMP_PREFIX = EmacsPlusActivator.getResourceString("Register_Jump_Prefix");	//$NON-NLS-1$
	protected static final String NUMBER_PREFIX = EmacsPlusActivator.getResourceString("Register_Number_Prefix");	//$NON-NLS-1$
	protected static final String POINT_PREFIX = EmacsPlusActivator.getResourceString("Register_Point_Prefix");	//$NON-NLS-1$
	protected static final String PREPEND_PREFIX = EmacsPlusActivator.getResourceString("Register_Prepend_Prefix");	//$NON-NLS-1$
	protected static final String VIEW_PREFIX = EmacsPlusActivator.getResourceString("Register_View_Prefix");	//$NON-NLS-1$
	
	protected static final String NO_SELECTION = EmacsPlusActivator.getResourceString("Register_No_Selection");	//$NON-NLS-1$
	protected static final String BAD_INSERT_LOCATION = EmacsPlusActivator.getResourceString("Register_Bad_Insert_Location");	//$NON-NLS-1$
	protected static final String NO_REGISTER = EmacsPlusActivator.getResourceString("Register_No_Register");	//$NON-NLS-1$
	protected static final String NO_TEXT = EmacsPlusActivator.getResourceString("Register_No_Text");	//$NON-NLS-1$
	protected static final String BAD_LOCATION = EmacsPlusActivator.getResourceString("Register_Bad_Location");	//$NON-NLS-1$
	protected static final String INSERTED = EmacsPlusActivator.getResourceString("Register_Inserted");	//$NON-NLS-1$
	protected static final String COPIED = EmacsPlusActivator.getResourceString("Register_Copied");	//$NON-NLS-1$
	protected static final String APPENDED = EmacsPlusActivator.getResourceString("Register_Appended");	//$NON-NLS-1$
	protected static final String PREPENDED = EmacsPlusActivator.getResourceString("Register_Prepended");	//$NON-NLS-1$
	protected static final String LOCATION = EmacsPlusActivator.getResourceString("Register_Location");	//$NON-NLS-1$
	protected static final String LOCATED = EmacsPlusActivator.getResourceString("Register_Located");	//$NON-NLS-1$
	protected static final String NO_LOCATION = EmacsPlusActivator.getResourceString("Register_No_Location");	//$NON-NLS-1$
	protected static final String REGISTER_NUMBER = EmacsPlusActivator.getResourceString("Register_Number");	//$NON-NLS-1$
	protected static final String REGISTER_NO_NUMBER = EmacsPlusActivator.getResourceString("Register_No_Number");	//$NON-NLS-1$
	
	private int callCount = 1;
	
	/**
	 * @return the callCount
	 */
	protected int getCallCount() {
		return callCount;
	}

	/**
	 * @param callCount the callCount to set
	 */
	protected void setCallCount(int callCount) {
		this.callCount = callCount;
	}

	/**
	 * Distinguish between Register commands that need a selection and those that don't
	 * 
	 * @return true if selection required, else false
	 */
	protected boolean needsSelection() {
		return true;
	}
	
	/**
	 * @see com.mulgasoft.emacsplus.commands.EmacsPlusCmdHandler#isLooping()
	 */
	@Override
	protected boolean isLooping() {
		return false;
	}
	
	/**
	 * @see com.mulgasoft.emacsplus.commands.EmacsPlusCmdHandler#transform(org.eclipse.ui.texteditor.ITextEditor, org.eclipse.jface.text.IDocument, org.eclipse.jface.text.ITextSelection, org.eclipse.core.commands.ExecutionEvent)
	 */
	protected int transform(ITextEditor editor, IDocument document, ITextSelection currentSelection,
			ExecutionEvent event) throws BadLocationException {
		if (needsSelection() && (currentSelection ==null || currentSelection.getLength() == 0)) {
			showResultMessage(editor, NO_SELECTION, true);
			return NO_OFFSET;
		} else {
			setCallCount(getUniversalCount());
			return bufferTransform(new RegisterMinibuffer(this), editor, event); 		
		}
	}
}
