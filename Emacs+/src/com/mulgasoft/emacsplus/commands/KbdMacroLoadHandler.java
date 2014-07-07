/**
 * Copyright (c) 2009, 2014 Mark Feber, MulgaSoft
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package com.mulgasoft.emacsplus.commands;

import java.io.File;
import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.util.Collection;
import java.util.SortedMap;

import org.eclipse.core.commands.Command;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.ParameterizedCommand;
import org.eclipse.jface.bindings.Binding;
import org.eclipse.jface.bindings.keys.KeyBinding;
import org.eclipse.jface.bindings.keys.KeySequence;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.commands.ICommandService;
import org.eclipse.ui.internal.keys.BindingService;
import org.eclipse.ui.keys.IBindingService;
import org.eclipse.ui.texteditor.ITextEditor;

import com.mulgasoft.emacsplus.EmacsPlusActivator;
import com.mulgasoft.emacsplus.EmacsPlusUtils;
import com.mulgasoft.emacsplus.execute.KbdMacroSupport;
import com.mulgasoft.emacsplus.execute.KbdMacroSupport.KbdEvent;
import com.mulgasoft.emacsplus.execute.KbdMacroSupport.KbdMacro;
import com.mulgasoft.emacsplus.minibuffer.IMinibufferState;
import com.mulgasoft.emacsplus.minibuffer.KbdMacroMinibuffer;
import static com.mulgasoft.emacsplus.EmacsPlusUtils.getTotalBindings;

/**
 * Implement load-kbd-macro
 *   
 * Load a Kbd Macro into the current Eclipse session
 * 
 * @author Mark Feber - initial API and implementation
 */
@SuppressWarnings("restriction") // Need internal class: org.eclipse.ui.internal.keys.BindingService
public class KbdMacroLoadHandler extends KbdMacroFileHandler {

	private final static String KBD_LOAD_PREFIX = EmacsPlusActivator.getResourceString("KbdMacro_Load_Prefix");   //$NON-NLS-1$  
	private final static String MACRO_QUESTION = EmacsPlusActivator.getResourceString("KbdMacro_Name_Exists");    //$NON-NLS-1$  
	private final static String BINDING_QUESTION = EmacsPlusActivator.getResourceString("KbdMacro_Exists"); 	  //$NON-NLS-1$  
	private final static String LOADED = EmacsPlusActivator.getResourceString("KbdMacro_Loaded");   			  //$NON-NLS-1$  
	private final static String LOADED_AND_BOUND = EmacsPlusActivator.getResourceString("KbdMacro_Bound_Loaded"); //$NON-NLS-1$  
	private final static String ABORT_LOAD = EmacsPlusActivator.getResourceString("KbdMacro_Abort_Load");   	  //$NON-NLS-1$  
	private final static String MACRO_MISSING = EmacsPlusActivator.getResourceString("KbdMacro_Missing");   	  //$NON-NLS-1$  
	private final static String BAD_CMD = EmacsPlusActivator.getResourceString("KbdMacro_Bad_Cmd"); 			  //$NON-NLS-1$  
	private final static String BAD_MACRO_STATE = "KbdMacro_Bad_State"; 										  //$NON-NLS-1$	
	private final static String FORMAT_S = "%s";																  //$NON-NLS-1$

	private SortedMap<String,String> fileCompletions = null;
	
	private String resultString = null;
	
	public Object execute(ExecutionEvent event) throws ExecutionException {
		try {
			if (KbdMacroSupport.getInstance().suppressMessages()) {
				try {
					ITextEditor editor = null;
					IWorkbenchPage page = EmacsPlusUtils.getWorkbenchPage();
					if (page != null) {
						IEditorPart epart = page.getActiveEditor();
						if (epart != null) {
							editor = (ITextEditor) epart.getAdapter(ITextEditor.class);
						}
					}
					// short circuit on auto-load invocation
					transform(editor,null,null,event);
				} catch (BadLocationException e) {
					// won't happen: not a location style command
				}
			} else {
				super.execute(event);
			}
			return resultString;
		} finally {
			resultString = null;
		} 
	}
	
	protected void asyncShowMessage(final IWorkbenchPart wpart, final String message, final boolean error) {
		resultString = message;
		if (!KbdMacroSupport.getInstance().suppressMessages()) {
			super.asyncShowMessage(wpart,message,error);
		}
	}
	
	/**
	 * @see com.mulgasoft.emacsplus.commands.EmacsPlusCmdHandler#transform(ITextEditor, IDocument, ITextSelection, ExecutionEvent)
	 */
	@Override
	protected int transform(ITextEditor editor, IDocument document, ITextSelection currentSelection,
			ExecutionEvent event) throws BadLocationException {
		
		fileCompletions = null;
		if (!KbdMacroSupport.getInstance().isDefining() && !KbdMacroSupport.getInstance().isExecuting()) {
			String macroName = event.getParameter(NAME_ARG);
			boolean forceIt = (event.getParameter(FORCE_ARG) != null ? Boolean.valueOf(event.getParameter(FORCE_ARG)) : false);
			if (macroName != null && macroName.length() > 0) {
				File file = macroFile(macroName);
				if (file.exists()) {
					loadMacro(editor,macroName,file,forceIt);
				} else {
					asyncShowMessage(editor, String.format(MACRO_MISSING, macroName), true);					
				}
			} else {
				mbState = this.nameState();
				return mbState.run(editor);
			}
		} else {
			asyncShowMessage(editor, BAD_MACRO_STATE, true);
		}
		return NO_OFFSET;
	}

	public SortedMap<String,?> getCompletions() {
		if (fileCompletions == null) {
			fileCompletions = getFileCompletions(); 	
		}
		return fileCompletions;
	}

	/**
	 * Get state to handle prompt for getting kbd macro name
	 * 
	 * @return naming prompt state
	 */
	protected IMinibufferState nameState() {
	
		return new IMinibufferState() {
			public String getMinibufferPrefix() {
				return KBD_LOAD_PREFIX;
			}
			
			public int run(ITextEditor editor) {
				miniTransform(new KbdMacroMinibuffer(KbdMacroLoadHandler.this),editor,null);
				return NO_OFFSET;
			}
			
			public boolean executeResult(ITextEditor editor, Object minibufferResult) {
				boolean result = true;
				String name = (String) minibufferResult;
				if (name == null || name.length() == 0) {
					// no name entered
					asyncShowMessage(editor, CANCEL, true);
				}else {
					File file = macroFile(name);
					if (file.exists()) {
						if (KbdMacroSupport.getInstance().getKbdMacro(name) != null) {				
							// macro by that name, request confirmation
							transitionState(editor,name);
						} else {
							loadMacro(editor,name,file,false);
						}
					} else {
						// no macro found
						asyncShowMessage(editor, String.format(NO_NAME_UNO, name), true);
					}
				}
				return result;
			}
			
			private void transitionState(ITextEditor editor, String name) {
				mbState = yesnoState(name,MACRO_QUESTION, new IKbdMacroOperation() {
					public void doOperation(ITextEditor editor, String name, File file) {
						loadMacro(editor,name,file,false);
					}
				});
				mbState.run(editor);
			}
		};
	}
	
	/**
	 * Load the macro from the File
	 * If the macro was saved with its binding, then restore that as well
	 * unless there is a conflict and the user decides against it
	 * 
	 * @param editor
	 * @param name - the kbd macro's name
	 * @param file - the file where the keyboard macro resides
	 */
	private void loadMacro(ITextEditor editor, String name, File file, boolean forceIt) {
 		try {
			FileInputStream fis = new FileInputStream(file);
			ObjectInputStream ois = new ObjectInputStream(fis);
			KbdMacro kbdMacro = (KbdMacro) ois.readObject();
			ois.close();
			String keyString = kbdMacro.getBindingKeys();
			final KeySequence sequence = (keyString != null ? KeySequence.getInstance(keyString) : null);  
			kbdMacro.setBindingKeys(null);	// housekeeping: binding string is added on save
			String badCommand = null;
			if ((badCommand = checkMacro(editor, kbdMacro)) == null) {
				final Command command = defineKbdMacro(editor,name);
				// register loaded macro
				KbdMacroSupport.getInstance().nameKbdMacro(name, kbdMacro);
				// now see if we have a binding also
				if (sequence != null) {
					final String msg = String.format(LOADED_AND_BOUND, name, keyString);
					final Binding oldBinding = checkForBinding(editor,sequence); 
					if (!forceIt && oldBinding != null) {
						// macro's binding is already present, ask for advice
						mbState = yesnoState(name,String.format(BINDING_QUESTION,keyString,FORMAT_S,FORMAT_S), new IKbdMacroOperation() {
							public void doOperation(ITextEditor editor, String name, File file) {
								bindMacro(editor,command,sequence,oldBinding);
								asyncShowMessage(editor, msg, false);						
							}
						});
						mbState.run(editor);
					} else {
						bindMacro(editor,command,sequence,oldBinding);
						asyncShowMessage(editor, msg, false);						
					}
				} else {
					asyncShowMessage(editor, String.format(LOADED, name), false);
				}
			} else {
				asyncShowMessage(editor, String.format(BAD_CMD,badCommand), true);	
			}
		} catch (Exception e) {
			String msg = e.getMessage();
			asyncShowMessage(editor, String.format(ABORT_LOAD, name, (msg != null ? msg : e.toString())), true);
		}
	}

	/**
	 * Bind the loaded macro to its previous key binding, removing any conflicts
	 * 
	 * @param editor
	 * @param command - the new kbd macro command
	 * @param sequence - key sequence for binding
	 * @param previous - conflicting binding
	 */
	private void bindMacro(ITextEditor editor, Command command, KeySequence sequence, Binding previous) {
		if (command != null && sequence != null) {
			
			IBindingService service = (editor != null) ? (IBindingService) editor.getSite().getService(IBindingService.class) :
				(IBindingService) PlatformUI.getWorkbench().getService(IBindingService.class);
			if (service instanceof  BindingService) {
				BindingService bindingMgr = (BindingService) service;
				if (previous != null) {
					bindingMgr.removeBinding(previous);
				}
				ParameterizedCommand p = new ParameterizedCommand(command, null);
				Binding binding = new KeyBinding(sequence, p,
						KBD_SCHEMEID, KBD_CONTEXTID, null, null, null, Binding.USER);
				bindingMgr.addBinding(binding);
				// check for conflicts independent of the current Eclipse context
				checkConflicts(bindingMgr,sequence,binding);
			}
		}				
	}
	
	/**
	 * Check for binding conflicts independent of the current Eclipse context
	 * If the load is called from a non-editing context, any potential binding conflict will
	 * not be detected; so look for conflicts in a context independent set of bindings.  
	 * 
	 * @param service
	 * @param sequence
	 * @param binding
	 */
	private void checkConflicts(BindingService service, KeySequence sequence, Binding binding) {
		Collection<Binding> conflicts = getTotalBindings().get(sequence);
		if (conflicts != null) {
			for (Binding conflict : conflicts) {
				if (conflict != binding
						&& binding.getContextId().equals(conflict.getContextId())
						&& binding.getSchemeId().equals(conflict.getSchemeId())) {
					service.removeBinding(conflict);
				}
			}
		}
	}
	
	/**
	 * Verify statically that this macro will execute properly
	 * - Ensure the current Eclipse defines the commands used by the macro 
	 * 
	 * @param editor
	 * @param kbdMacro
	 * @return true if validates, else false
	 */
	private String checkMacro(ITextEditor editor, KbdMacro kbdMacro) {
		String result = null;
		ICommandService ics = (editor != null ) ? (ICommandService) editor.getSite().getService(ICommandService.class) : 
			(ICommandService) PlatformUI.getWorkbench().getService(ICommandService.class);		
		@SuppressWarnings("unchecked")	// Eclipse documents the type 
		Collection<String> cmdIds = (Collection<String>)ics.getDefinedCommandIds();
		for (KbdEvent e : kbdMacro.getKbdMacro()) {
			String cmdId;
			if ((cmdId = e.getCmd()) != null) {
				if (!cmdIds.contains(cmdId)) {
					result = cmdId;
					break;
				}
			}
		}
		return result;
	}

}

