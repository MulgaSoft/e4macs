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

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.commands.Command;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ParameterizedCommand;
import org.eclipse.core.commands.common.NotDefinedException;
import org.eclipse.jface.bindings.Binding;
import org.eclipse.jface.bindings.keys.KeyBinding;
import org.eclipse.jface.bindings.keys.KeySequence;
import org.eclipse.jface.bindings.keys.KeyStroke;
import org.eclipse.jface.bindings.keys.ParseException;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchListener;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.commands.ICommandService;
import org.eclipse.ui.internal.keys.BindingService;
import org.eclipse.ui.keys.IBindingService;
import org.eclipse.ui.texteditor.ITextEditor;

import com.mulgasoft.emacsplus.EmacsPlusActivator;
import com.mulgasoft.emacsplus.EmacsPlusUtils;
import com.mulgasoft.emacsplus.IEmacsPlusCommandDefinitionIds;
import com.mulgasoft.emacsplus.execute.IBindingResult;
import com.mulgasoft.emacsplus.execute.KbdMacroSupport;
import com.mulgasoft.emacsplus.minibuffer.BindingMinibuffer;
import com.mulgasoft.emacsplus.minibuffer.IMinibufferState;
import com.mulgasoft.emacsplus.minibuffer.KbdMacroMinibuffer;
import com.mulgasoft.emacsplus.minibuffer.YesNoMinibuffer;

/**
 * Implement kbd-macro-bind-to-key
 * 
 * Bind the most recently defined keyboard macro to a key sequence (for the duration of the session).
 * 
 * [Emacs+ addition: if called with ^U then prompt for named kbd macro to bind]
 * 
 * If you try to bind to a key sequence with an existing binding (in any keymap), this
 * command asks you for confirmation before replacing the existing binding.
 * 
 * To avoid problems caused by overriding existing bindings, the key
 * sequences `C-x C-k 0' through `C-x C-k 9' and `C-x C-k A' through `C-x
 * C-k Z' are reserved for your own keyboard macro bindings.  In fact, to
 * bind to one of these key sequences, you only need to type the digit or
 * letter rather than the whole key sequences.  For example,
 * 
 *      C-x C-k b 4
 *      
 * will bind the last keyboard macro to the key sequence `C-x C-k 4'.
 * 
 * @see org.eclipse.ui.internal.keys.BindingService
 * @see org.eclipse.jface.bindings.BindingManager#addBinding(Binding)
 * 
 * @author Mark Feber - initial API and implementation
 */
// for cast to internal org.eclipse.ui.internal.keys.BindingService
@SuppressWarnings("restriction")	
public class KbdMacroBindHandler extends KbdMacroDefineHandler {

	private static final String BINDING_PREFIX = EmacsPlusActivator.getResourceString("KbdMacro_Bind_Prefix");  	//$NON-NLS-1$
	private static final String NAMING_PREFIX = EmacsPlusActivator.getResourceString("KbdMacro_BindName_Prefix");   //$NON-NLS-1$

	private static final String REBINDING = EmacsPlusActivator.getResourceString("KbdMacro_ReBinding_Prefix");  	//$NON-NLS-1$
	private static final String DEMAND_ANSWER= EmacsPlusActivator.getResourceString("KbdMacro_ReReBinding_Prefix"); //$NON-NLS-1$
	private static final String KBD_BINDING_ERROR= EmacsPlusActivator.getResourceString("KbdMacro_BadBinding"); 	//$NON-NLS-1$
	
	private static final String BOUND = EmacsPlusActivator.getResourceString("KbdMacro_Bind_Result");   			//$NON-NLS-1$
	private static final String ABORT = EmacsPlusActivator.getResourceString("KbdMacro_Bind_Abort");				//$NON-NLS-1$
	
	private static final String STD_KBD_PREFIX = "CTRL+X CTRL+K";   												//$NON-NLS-1$

	// Name constituent TODO: do we need to improve uniqueness?
	private static int nameid = 0;
	// Name constituent. Not really a lambda, but it's tradition
	private final static String KBD_LNAME = "lambda_";	//$NON-NLS-1$
	
	// store bindings so they can be removed on exit
	private static Map<String,Binding>cacheBindings = new HashMap<String,Binding>();
	// the listener for cleanup on workbench exit
	private static IWorkbenchListener workbenchListener = null;

	public KbdMacroBindHandler() {
		super();
		IWorkbench bench = PlatformUI.getWorkbench();
		bench.addWorkbenchListener(getWorkbenchListener());
	}

	private IWorkbenchListener getWorkbenchListener() {
		if (workbenchListener == null) {
			workbenchListener = new IWorkbenchListener() {
				
				public boolean preShutdown(IWorkbench workbench, boolean forced) {
					// comment on IWorkbenchListener says this is too early
					return true;
				}

				public void postShutdown(IWorkbench workbench) {
					// hopefully, it is not too late
					if (!cacheBindings.isEmpty()) {
						IBindingService service = (IBindingService) PlatformUI.getWorkbench().getService(IBindingService.class);
						if (service instanceof  BindingService) {
							BindingService bindingMgr = (BindingService) service;
							for (Binding b : cacheBindings.values()) {
								bindingMgr.removeBinding(b);
							}
						}
					}
				}
			};
		}
		return workbenchListener;
	}

	/**
	 * @see com.mulgasoft.emacsplus.commands.EmacsPlusCmdHandler#transform(ITextEditor, IDocument, ITextSelection, ExecutionEvent)
	 */
	@Override
	protected int transform(ITextEditor editor, IDocument document, ITextSelection currentSelection,
			ExecutionEvent event) throws BadLocationException {
		boolean named = isUniversalPresent();
		if (KbdMacroSupport.getInstance().hasKbdMacro(named) && !KbdMacroSupport.getInstance().isBusy()) {
			mbState = (named ? nameState() : bindState(null));
			return mbState.run(editor);
		} else {
			asyncShowMessage(editor, NO_MACRO_ERROR, true);
		}
		return NO_OFFSET;
	}

	/**
	 * @see com.mulgasoft.emacsplus.commands.EmacsPlusCmdHandler#isLooping()
	 */
	@Override
	protected boolean isLooping() {
		return false;
	}
	
	/**
	 * Get state to handle prompt for key binding
	 * 
	 * @return binding prompt state
	 */
	private IMinibufferState bindState(final String name) {
	
		return new IMinibufferState() {
			public String getMinibufferPrefix() {
				return BINDING_PREFIX;
			}
			
			public int run(ITextEditor editor) {
				miniTransform(new BindingMinibuffer(KbdMacroBindHandler.this), editor, null);
				return NO_OFFSET;
			}
			
			public boolean executeResult(ITextEditor editor, Object minibufferResult) {
				boolean result = true;
				if (minibufferResult != null) {
					IBindingResult bindingR = (IBindingResult) minibufferResult;
					String keyString = bindingR.getKeyString();
					KeySequence trigger = null; 
					KeyStroke[] keys = null;
					if (bindingR == null || (trigger = bindingR.getTrigger()) == null) {
						// no binding present
						asyncShowMessage(editor, String.format(CMD_NO_RESULT, keyString) + CMD_NO_BINDING, true);
					} else if ((keys = trigger.getKeyStrokes()).length > 0 && keys[keys.length -1].getModifierKeys() != 0) {
						// binding ends with a modifier key
						if (bindingR.getKeyBinding() != null && 
								(IEmacsPlusCommandDefinitionIds.KEYBOARD_QUIT.equals(bindingR.getKeyBinding().getParameterizedCommand().getId()))) {
							// clear status area and process ^G interrupt
							EmacsPlusUtils.clearMessage(editor);
							beep();
						} else {
							// report bad binding present
							asyncShowMessage(editor, KBD_BINDING_ERROR, true);
						}
					} else if (bindingR.getKeyBinding() != null) {				
						// conflicting binding present
						transitionState(editor,bindingR);
					} else if (bindingR.getKeyString().length() == 1){
						// single character was entered, check for C-x C-k <key> binding conflict
						IBindingResult ibr = getBinding(editor,bindingR.getKeyString().charAt(0));
						if (ibr != null) {
							if (ibr.getKeyBinding() != null) {
								transitionState(editor,ibr);
							} else {
								addBinding(editor,ibr,name);	// good to go
							}
						} else {
							asyncShowMessage(editor, String.format(ABORT, bindingR.getKeyString()), true);							
						}
					} else { 
						addBinding(editor, bindingR,name);	// good to go
					}
				}
				return result;
			}
			
			private void transitionState(ITextEditor editor, IBindingResult ibr) {
				// conflicting binding present
				mbState = yesnoState(ibr,name); 
				mbState.run(editor);
			}
		};
	}

	/**
	 * Get state to handle prompt for getting kbd macro name
	 * 
	 * @return naming prompt state
	 */
	private IMinibufferState nameState() {
	
		return new IMinibufferState() {
			public String getMinibufferPrefix() {
				return NAMING_PREFIX;
			}
			
			public int run(ITextEditor editor) {
				miniTransform(new KbdMacroMinibuffer(KbdMacroBindHandler.this),editor,null);
				return NO_OFFSET;
			}
			
			public boolean executeResult(ITextEditor editor, Object minibufferResult) {
				boolean result = true;
				if (minibufferResult != null) {
					String name = (String) minibufferResult;
					if (name == null || name.length() == 0) {
						// no name entered
						asyncShowMessage(editor, String.format(CMD_NO_RESULT, name) + CMD_NO_BINDING, true);
					} else if (KbdMacroSupport.getInstance().getKbdMacro(name) != null) {				
						// macro by that name
						transitionState(editor,name);
					} else {
						// no macro found
						asyncShowMessage(editor, String.format(NO_NAME_UNO, name), true);							
					}
				}
				return result;
			}
			
			private void transitionState(ITextEditor editor, String name) {
				mbState= bindState(name);
				mbState.run(editor);
			}
		};
	}
	
	/**
	 * Get state to handle yes/no prompt
	 * 
	 * @param binding
	 * @param mini
	 * @return yes/no state
	 */
	private IMinibufferState yesnoState(final IBindingResult binding, final String name) {
			
		return new IMinibufferState() {

			IBindingResult bindingResult = binding; 
			YesNoMinibuffer minibuffer = null;

			boolean retry = false;

			public String getMinibufferPrefix() {
				String command = EMPTY_STR;
				try {
					command = binding.getKeyBinding().getParameterizedCommand().getName();
				} catch (NotDefinedException e) {
				}
				if (retry) {
					return String.format(DEMAND_ANSWER,YESORNO_YES,YESORNO_NO);
				} else {
					return String.format(REBINDING,binding.getKeyString(),command,YESORNO_YES,YESORNO_NO);
				}
			}

			public int run(ITextEditor editor) {
				minibuffer = new YesNoMinibuffer(KbdMacroBindHandler.this); 
				miniTransform(minibuffer,editor,null);
				return NO_OFFSET;
			}

			public boolean executeResult(ITextEditor editor, Object minibufferResult) {
				boolean result = true;
				if (minibufferResult != null && minibufferResult instanceof Boolean) { 
					if ((Boolean)minibufferResult) {
						addBinding(editor, bindingResult,name);
					} else {
						asyncShowMessage(editor, String.format(ABORT, bindingResult.getKeyString()), true);
					}
				} else {
					retry = true;
					miniTransform(minibuffer,editor,null);
					result = false;
				}
				return result;
			}
		};
	}
	
	/**
	 * Check for C-x C-k <key> binding conflict
	 * 
	 * @param editor
	 * @param c
	 * @return IBindingResult with C-x C-k <key> information
	 */
	private IBindingResult getBinding(ITextEditor editor, char c) {
		IBindingResult result = null;
		try {
			final KeySequence sequence = KeySequence.getInstance(KeySequence.getInstance(STD_KBD_PREFIX), KeyStroke.getInstance(c));
			final Binding binding = checkForBinding(editor, sequence);
			result = new IBindingResult() {
				public Binding getKeyBinding() { return binding; }
				public String getKeyString() { return sequence.format(); }
				public KeySequence getTrigger() { return sequence; }
			};
		} catch (ParseException e) { }
		return result;
	}
	
	/**
	 * Add the binding to the Emacs+ scheme
	 * 
	 * @param editor
	 * @param bindingResult
	 */
	private void addBinding(ITextEditor editor, IBindingResult bindingResult, String name) {
		IBindingService service = (IBindingService) editor.getSite().getService(IBindingService.class);
		if (service instanceof  BindingService) {
			try {
				BindingService bindingMgr = (BindingService) service;
				if (bindingResult.getKeyBinding() != null) {
					// we're overwriting a binding, out with the old
					bindingMgr.removeBinding(bindingResult.getKeyBinding());
				}
				Command command = null;
				if (name != null) {
					ICommandService ics = (ICommandService) editor.getSite().getService(ICommandService.class);
					String id = EmacsPlusUtils.kbdMacroId(name);
					// check first, as getCommand will create it if it doesn't already exist
					if (ics.getDefinedCommandIds().contains(id)) {
						command = ics.getCommand(id);
					}
				} else {
					// use the unexposed category
					command = nameKbdMacro(KBD_LNAME + nameid++, editor, KBD_GAZONK);
				}
				if (command != null) {
					Binding binding = new KeyBinding(bindingResult.getTrigger(), new ParameterizedCommand(command, null),
							KBD_SCHEMEID, KBD_CONTEXTID, null, null, null, Binding.USER);
					bindingMgr.addBinding(binding);
					asyncShowMessage(editor, String.format(BOUND, bindingResult.getKeyString()), false);
				} else {
					asyncShowMessage(editor, String.format(NO_NAME_UNO, name), true);									
				}
			} catch (Exception e) { 
				asyncShowMessage(editor, String.format(ABORT, bindingResult.getKeyString()), true);				
			}
		}
	}

}
