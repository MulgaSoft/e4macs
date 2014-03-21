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

import java.io.File;

import org.eclipse.core.commands.Command;
import org.eclipse.core.commands.IParameter;
import org.eclipse.jface.bindings.Binding;
import org.eclipse.jface.bindings.keys.KeySequence;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.commands.ICommandService;
import org.eclipse.ui.keys.IBindingService;
import org.eclipse.ui.texteditor.ITextEditor;

import com.mulgasoft.emacsplus.EmacsPlusActivator;
import com.mulgasoft.emacsplus.EmacsPlusUtils;
import com.mulgasoft.emacsplus.execute.KbdMacroSupport;
import com.mulgasoft.emacsplus.minibuffer.IMinibufferState;

/**
 * Abstract base class for Kbd Macro commands that name, define, save, load, etc.
 * 
 * @author Mark Feber - initial API and implementation
 */
public abstract class KbdMacroDefineHandler extends MinibufferExecHandler implements INonEditingCommand {

	protected final static String KBD_DESCRIPTION = EmacsPlusActivator.getResourceString("KbdMacro_Description"); //$NON-NLS-1$

	// Support ^U on named/bound macros. kludge: Eclipse has no way to dynamically define a parameter
	final static String PARAMETER_CMD = "com.mulgasoft.emacsplus.executeKbdMacro";  							  //$NON-NLS-1$
	final static String PARAMETER = "universalArg"; 															  //$NON-NLS-1$
	
	// Used for binding kbd macros
	protected final static String KBD_CATEGORY= "emacsplus.keyboard.macros";									  //$NON-NLS-1$
	// hidden category
	protected final static String KBD_GAZONK= "gazonk.del"; 													  //$NON-NLS-1$
	// Text editors only 
	final static String KBD_CONTEXTID = "org.eclipse.ui.textEditorScope";   									  //$NON-NLS-1$
	// Emacs+ scheme id 
	final static String KBD_SCHEMEID = "com.mulgasoft.emacsplusConfiguration";  								  //$NON-NLS-1$
	
	// for yes/no questions
	static final String YESORNO_YES = EmacsPlusActivator.getResourceString("YesOrNo_Yes");  					  //$NON-NLS-1$ 
	static final String YESORNO_NO = EmacsPlusActivator.getResourceString("YesOrNo_No");						  //$NON-NLS-1$ 
	static final String YESORNO_Y = EmacsPlusActivator.getResourceString("YesOrNo_Y");  						  //$NON-NLS-1$ 
	static final String YESORNO_N = EmacsPlusActivator.getResourceString("YesOrNo_N");  						  //$NON-NLS-1$ 
	static final String NO_NAME_UNO = EmacsPlusActivator.getResourceString("KbdMacro_NoName_Error");			  //$NON-NLS-1$
	static final String NO_MACRO_ERROR = "KbdMacro_No_Error";   												  //$NON-NLS-1$

	protected interface IKbdMacroOperation {
		void doOperation(ITextEditor editor, String name, File file);
	}

	// the state object for linked minibuffers
	protected IMinibufferState mbState = null;

	/**
	 * Name and define the Command associated with the kbd macro
	 *  
	 * @param name - the short name
	 * @param editor
	 * @return the Command
	 */
	Command nameKbdMacro(String name, ITextEditor editor) {
		return nameKbdMacro(name, editor, KBD_CATEGORY);
	}
	
	/**
	 * Name and define the Command associated with the kbd macro
	 * 
	 * @param name - the short name
	 * @param editor
	 * @param category - the category to use in the Command definition
	 * @return
	 */
	Command nameKbdMacro(String name, ITextEditor editor, String category) {
		Command command = null;
		if (name != null && name.length() > 0) {
			// Register the named macro with KbdMacroSupport
			String id = KbdMacroSupport.getInstance().nameKbdMacro(name);
			command = defineKbdMacro(editor, id, name, category); 
		}
		return command;
	}
			
	/**
	 * Define the Command to be used when executing the named kbd macro
	 * 
	 * @param editor
	 * @param name - the short name
	 * @return the Command
	 */
	Command defineKbdMacro(ITextEditor editor, String name) {
		return defineKbdMacro(editor,EmacsPlusUtils.kbdMacroId(name), name, KBD_CATEGORY);
	}
	
	/**
	 * Define the Command to be used when executing the named kbd macro
	 * 
	 * @param editor
	 * @param id - the full command id
	 * @param name - the short name
	 * @param category - the category to use in the definition
	 * @return the Command
	 */
	Command defineKbdMacro(ITextEditor editor, String id, String name, String category) {
		Command command = null;
		if (id != null) {
			// Now create the executable command 
			ICommandService ics = (editor != null ) ? (ICommandService) editor.getSite().getService(ICommandService.class) : 
				(ICommandService) PlatformUI.getWorkbench().getService(ICommandService.class);		
			command = ics.getCommand(id);
			IParameter[] parameters = null;
			try {
				// kludge: Eclipse has no way to dynamically define a parameter, so grab it from a known command
				IParameter p = ics.getCommand(PARAMETER_CMD).getParameter(PARAMETER);
				parameters = new IParameter[] { p };
			} catch (Exception e) { 
			}
			command.define(name, String.format(KBD_DESCRIPTION,name), ics.getCategory(category), parameters);
			command.setHandler(new KbdMacroNameExecuteHandler(name));
		}
		return command;
	}

	/**
	 * Check for perfect binding match to key sequence
	 * 
	 * @param editor
	 * @param sequence
	 * @return Binding if perfect match found, else null
	 */
	protected Binding checkForBinding(ITextEditor editor, KeySequence sequence) {
		IBindingService service = (editor != null) ? (IBindingService) editor.getSite().getService(IBindingService.class) :
			(IBindingService) PlatformUI.getWorkbench().getService(IBindingService.class);
		return service.getPerfectMatch(sequence);
	}

	/**
	 * Dispatch through state object
	 * 
	 * @see com.mulgasoft.emacsplus.commands.MinibufferHandler#getMinibufferPrefix()
	 */
	@Override
	public String getMinibufferPrefix() {
		return mbState.getMinibufferPrefix();
	}

	/**
	 * Dispatch through state object
	 * 
	 * @see com.mulgasoft.emacsplus.commands.MinibufferExecHandler#doExecuteResult(org.eclipse.ui.texteditor.ITextEditor, java.lang.Object)
	 */
	@Override
	protected boolean doExecuteResult(ITextEditor editor, Object minibufferResult) {
		return mbState.executeResult(editor, minibufferResult);
	}

	/**
	 * @see com.mulgasoft.emacsplus.commands.EmacsPlusCmdHandler#isLooping()
	 */
	@Override
	protected boolean isLooping() {
		return false;
	}

}
