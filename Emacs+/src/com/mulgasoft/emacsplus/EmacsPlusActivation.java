/**
 * Copyright (c) 2009, 2010 Mark Feber, MulgaSoft
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 */
package com.mulgasoft.emacsplus;

import static com.mulgasoft.emacsplus.EmacsPlusUtils.getBindingService;
import static com.mulgasoft.emacsplus.EmacsPlusUtils.getTotalBindings;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Hashtable;
import java.util.Map;

import org.eclipse.core.commands.Command;
import org.eclipse.core.commands.ParameterizedCommand;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.bindings.Binding;
import org.eclipse.jface.bindings.TriggerSequence;
import org.eclipse.jface.bindings.keys.KeySequence;
import org.eclipse.jface.bindings.keys.KeyStroke;
import org.eclipse.jface.dialogs.IPageChangeProvider;
import org.eclipse.jface.dialogs.IPageChangedListener;
import org.eclipse.jface.dialogs.PageChangedEvent;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentListener;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IWindowListener;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.commands.ICommandService;
import org.eclipse.ui.internal.keys.BindingService;
import org.eclipse.ui.part.MultiEditor;
import org.eclipse.ui.part.MultiPageEditorPart;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.texteditor.TextEditorAction;

import com.mulgasoft.emacsplus.KillRingListeners.EmacsActionDelegate;
import com.mulgasoft.emacsplus.execute.KbdMacroSupport;
import com.mulgasoft.emacsplus.preferences.EmacsPlusPreferenceConstants;

/**
 * Activate all the pretty listeners
 * 
 * @author Mark Feber - initial API and implementation
 */
@SuppressWarnings("restriction")	// For dangerous use of BindingService.addBinding/removeBinding
//public class EmacsPlusActivation implements IPartListener2 {
public enum EmacsPlusActivation implements IPartListener2 {
	
	ACTIVATION_INSTANCE;
	
	private static Map<String,InstallState> editors;
	private static Map<String,String> actors;
	
	static {
		editors = new Hashtable<String,InstallState>();		
		actors = new Hashtable<String,String>();
		IPreferenceStore store = EmacsPlusActivator.getDefault().getPreferenceStore();
		if (store != null) {
			// Just set the value, it will be interpreted in activateEditor 
			ACTIVATION_INSTANCE.digitArgument = (store.getBoolean(EmacsPlusPreferenceConstants.P_CTRL_DIGIT_ARGUMENT));
		}
	}

	// Has the kill ring been activated?
	private boolean activated = false;
	
	// cache preference value for whether or not to force digit-argument bindings  
	private boolean digitArgument = false;
	private Boolean ctrlBindings = null;

	// the set of character keys used in digit-argument
	private static final char[] digitKeys = {'-', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9'};
	
	// Eclipse forces us to remember the most recently activated editor
	private IEditorPart activatedPart = null;
	
	private EmacsPlusActivation() {};

	public static EmacsPlusActivation getInstance(){
		return ACTIVATION_INSTANCE;
	}
	
	private boolean isActivated() {
		return activated;
	}

	private void setActivated(boolean activated) {
		this.activated = activated;
	}
	
	/**
	 * Activate the listeners necessary to support Emacs+.
	 * Add us as the part listener for the workbench page
	 * Also, add the necessary listeners for the initial active editor on Eclipse startup
	 * 
	 * If the user disables startup activation of the plug-in, then any kills (e.g. word delete, etc.) 
	 * will not be inserted into the ring until after the plug-in has been "manually" activated by 
	 * invoking an Emacs+ specific key binding, opening its preference page, etc. 
	 */
	void activateListeners() {
		if (!isActivated()) {
			setActivated(true);
			IWorkbench bench = PlatformUI.getWorkbench();
			IWorkbenchWindow window = bench.getActiveWorkbenchWindow();
			// detect when the workbench is (re)activated, or a new window is opened
			bench.addWindowListener(KillRingListeners.getActivationListener());
			bench.addWindowListener(getWindowActivationListener());
			activatePage(window,true);
		}
	}

	/**
	 * @param window
 	 * @param onPlugActivation true on plugin activation
	 */
	private void activatePage(IWorkbenchWindow window, boolean onPlugActivation) {
		IWorkbenchPage page = getActivePage(window);
		if (page != null) {
			page.addPartListener(this);
			// simulate part activation on workbench (re)activation
			activateEditor(page.getActiveEditor(),onPlugActivation);
		}
	}

	private void deactivatePage(IWorkbenchWindow window) {
		IWorkbenchPage page = getActivePage(window);
		if (page != null) {
			page.removePartListener(this);
		}
	}

	private IWorkbenchPage getActivePage(IWorkbenchWindow window) {
		IWorkbenchPage page = null;
		if (window != null) {
			page = window.getActivePage();
		}
		if (page == null) {

			// Look for a window and get the page off it!
			IWorkbenchWindow[] windows = PlatformUI.getWorkbench().getWorkbenchWindows();
			for (int i = 0; i < windows.length; i++) {
				if (windows[i] != null) {
					window = windows[i];
					page = windows[i].getActivePage();
					if (page != null)
						break;
				}
			}
		}
		return page;
	}

	private IWindowListener getWindowActivationListener() {
		
		return new IWindowListener() {

			// @Override
			public void windowActivated(IWorkbenchWindow window) {
				activatePage(window, false);
			}

			// @Override
			public void windowClosed(IWorkbenchWindow window) {
				deactivatePage(window);
			}

			// @Override
			public void windowDeactivated(IWorkbenchWindow window) {
				deactivatePage(window);
				KbdMacroSupport.interruptKbdMacro();
			}

			// @Override
			public void windowOpened(IWorkbenchWindow window) {
			}
		};
	}
	

	public void partActivated(IWorkbenchPartReference partRef) {
		if (partRef instanceof IEditorReference) {
			activateEditor(((IEditorReference) partRef).getEditor(false),false);
		}
	}

	public void partDeactivated(IWorkbenchPartReference partRef) {
		if (partRef instanceof IEditorReference) {
			IEditorPart epart = ((IEditorReference) partRef).getEditor(false);
			MarkUtils.removeActivationListeners(getActiveEditor(epart));
			BufferLocal.getInstance().handleDeactivate(epart);
		} else {
			// check for clipped text from other views and add to the kill ring
			KillRing.getInstance().checkClipboard();
		}
	}
	
	public void partBroughtToTop(IWorkbenchPartReference partRef) {
	}

	public void partClosed(IWorkbenchPartReference partRef) {
		
		if (partRef instanceof IEditorReference) {
			try {
				IEditorPart editor = ((IEditorReference) partRef).getEditor(false);
				if (editor instanceof MultiPageEditorPart) {
					// TODO: AFAIK there is no way to capture all the text editors in a multi-page
					// if there is more that one.  This just grabs one, but it is better than nothing
					 editor =  (ITextEditor)editor.getAdapter(ITextEditor.class);
				}
				if (editor instanceof ITextEditor) {
					// Hack alert: Eclipse activates the next editor before closing the previous editor
					// so, if it is looking at the same document, don't remove the kill buffer listener
					if (activatedPart == null || 
							(editor != activatedPart && editor.getEditorInput() != activatedPart.getEditorInput())) {
						IDocumentProvider provider = ((ITextEditor) editor).getDocumentProvider();
						if (provider != null) {
							IDocument document = provider.getDocument(editor.getEditorInput());
							if (document != null) {
								document.removeDocumentListener(getDocListener());
								MarkRing.removeMarks(document);
							}
						}
					}
					removeActionListeners(editor);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			activatedPart = null;
		}
	}

	public void partHidden(IWorkbenchPartReference partRef) {
	}

	public void partInputChanged(IWorkbenchPartReference partRef) {
		if (partRef instanceof IEditorReference) {
			IEditorPart editor = ((IEditorReference) partRef).getEditor(false);
			addListeners(editor);
		}
	}

	public void partOpened(IWorkbenchPartReference partRef) {
		if (partRef instanceof IEditorReference) {
			IEditorPart editor = ((IEditorReference) partRef).getEditor(false);
			addListeners(editor);
		}
	}

	public void partVisible(IWorkbenchPartReference partRef) {}

	private IDocumentListener getDocListener() {
		return KillRing.getInstance();
	}

	private void addListeners(IEditorPart editor) {
		ITextEditor active = getActiveEditor(editor);
		if (active != null) {
			doAddListeners(active);
		}
		if ((editor instanceof MultiPageEditorPart) || (editor instanceof MultiEditor)) {
			// Now, set up listeners for when the page changes within the editor
			if (editor instanceof IPageChangeProvider) {
				((IPageChangeProvider) editor).addPageChangedListener(getPageListener());
			}
		}
	}
	
	private ITextEditor getActiveEditor(IEditorPart editor) {
		return EmacsPlusUtils.getActiveTextEditor(editor);
	}
	
	private static IPageChangedListener pageListener;
	
	private IPageChangedListener getPageListener() {
		if (pageListener == null) {
			pageListener = new IPageChangedListener() {
				public void pageChanged(PageChangedEvent event) {
					Object obj = event.getSelectedPage();
					if (obj instanceof ITextEditor) {
						activateEditor((ITextEditor)obj,false);
					}
				}
			};
		}
		return pageListener;
	}

	/**
	 * Add the basic listeners required on the editor
	 * 
	 * @param editor
	 */
	private void doAddListeners(ITextEditor editor) {
		// Listen for edits on this doc
		addDocumentListeners(editor);
		// Listen for command executions
		addCommandListeners(editor);
	}

	/**
	 * Add the kill ring instance as a document listener
	 * 
	 * @param editor
	 */
	private void addDocumentListeners(ITextEditor editor){
		IDocumentProvider provider = ((ITextEditor) editor).getDocumentProvider();
		if (provider != null) {
			IDocument document = provider.getDocument(editor.getEditorInput());
			if (document != null) {
				document.addDocumentListener(getDocListener());
			}
		}
	}

	/**
	 * Call whenever part (or sub-part) is activated
	 * 
	 * @param epart
	 * @param onPlugActivation true on plugin activation
	 */
	private void activateEditor(IEditorPart epart, boolean onPlugActivation) {
		if (epart != null) {
			activatedPart = epart;
			addListeners(epart);
			MarkUtils.addActivationListeners(getActiveEditor(epart));
			Runnable check = new Runnable() {
				public void run() {
					checkOnActivation(activatedPart);			
				}
			};
			if (onPlugActivation) {
				// when activating Emacs+, run in the UI thread 
				PlatformUI.getWorkbench().getDisplay().asyncExec(check);
			} else {
				check.run();				
			}
		}
	}
	
	/**
	 * Miscellaneous tasks to perform during part activation
	 * 
	 * @param epart
	 */
	private void checkOnActivation(IEditorPart epart) {
		ITextEditor editor = getActiveEditor(epart);
		if (editor != null) {
			KbdMacroSupport.getInstance().continueKbdMacro(editor);
			BufferLocal.getInstance().handleActivate(editor);
			checkIMEListener(editor);			
			// check here as key bindings are not set up until part is activated,
			// and we need the InstallState set up as well (addListeners)
			checkDigitArgument(editor);
		}
	}
	
	/**
	 * Disable Option+<char> in-line pre-edit text areas on Mac OS X
	 * if the preference in set. 
	 * For Mac users that prefer the Option Meta binding, but don't want
	 * Meta+<char> commands to generate the initial character (e.g. typing <Option>+u can
	 * generate a dangling umlaut in addition to upper casing.  
	 * However, <Option>+u u, will still generate the mixed character
	 * org.eclipse.swt.widgets.IME
	 * 
	 * @param editor
	 */
	private static void checkIMEListener(final ITextEditor editor) {
		if (EmacsPlusUtils.isDisableOptionIMEPreferenece() && EmacsPlusUtils.isMac()) {
			StyledText widget = MarkUtils.getStyledWidget(editor);
			if (widget != null && widget.getIME() != null) {
				Listener[] listeners = widget.getIME().getListeners(SWT.ImeComposition);
				if (listeners != null) {
					for (Listener l : listeners) {
						widget.getIME().removeListener(SWT.ImeComposition, l);
					}
				}
			}
		}
	}
	
	private class InstallState {
		boolean digitEnabled = false;
		Collection<Binding> disabledBindings = null;
	}
	
	private boolean isDigitArgument() {
		return digitArgument;
	}

	/**
	 * Sets the preference controlling the runtime enforcement of Ctrl-<number> 
	 * as digit-argument (as implemented by universal-argument)
	 *   
	 * @param digitArgument
	 * @return true if the preference value has changed
	 */
	public boolean setDigitArgument(boolean digitArgument) {
		boolean reset = this.digitArgument != digitArgument;
		this.digitArgument = digitArgument;
		if (reset) {
			IWorkbenchPage page = EmacsPlusUtils.getWorkbenchPage();
			if (page != null) {
				IEditorPart editor = page.getActiveEditor();
				if (editor instanceof ITextEditor) {
					checkDigitArgument(editor, editors.get(editor.getClass().getName()));
				}
			}
		}
		return reset;
	}

	private void checkDigitArgument(IEditorPart editor) {
		ITextEditor active = getActiveEditor(editor);
		if (active != null) {
			checkDigitArgument(active, editors.get(active.getClass().getName()));
		}
	}
	
	/**
	 * Check if we should disable/enable bindings that interfere with digit-argument interpretation
	 *  
	 * @param editor
	 * @param digitState
	 */
	private void checkDigitArgument(final IEditorPart editor, final InstallState digitState) {
		// active only when Ctrl+<number> is bound to universal-argument
		if (digitState != null && hasDigitBindings(false)) {
			final int modKey = (EmacsPlusUtils.isMac() ? SWT.COMMAND : SWT.CTRL);
			// CTRL+<n> is sometimes used as a prefix character in other plugins.  The following code forces
			// the Emacs digit-argument interpretation by using Eclipse internals to remove the offending 
			// bindings automagically.  This is controlled by a user-settable preference (initially disabled)
			// TODO binding disable/enable should more properly be done by context rather than editor type
			if (isDigitArgument()) {
				if (digitState.digitEnabled == false) {
					Collection<Binding> oldDisabledBindings = digitState.disabledBindings;
					Collection<Binding> disabled = disableBindings(editor, modKey, digitKeys, false);
					digitState.disabledBindings = disabled;
					// restore any old bindings
					restoreBindings(oldDisabledBindings,getBindingService());
				}
			} else {
				if (digitState.digitEnabled == true && digitState.disabledBindings != null) {
					// restore disabled bindings
					restoreBindings(digitState.disabledBindings, getBindingService());
					digitState.disabledBindings = null;
				} 
				// check for any Emacs+ commands to disable (e.g. C-1 when in java editor)
				if (digitState.disabledBindings == null) {
					Collection<Binding> disabled = disableBindings(editor, modKey, digitKeys, true);
					digitState.disabledBindings = (disabled != null ? disabled : new ArrayList<Binding>());
				}
			}
			digitState.digitEnabled = isDigitArgument();
		}
	}

	/**
	 * Add the command listeners to support the Emacs+ kill ring to the editor
	 * This only needs to be done at most once per editor class
	 * 
	 * @param editor
	 */
	private void addCommandListeners(IEditorPart editor) {
		addActionListeners(editor);
		
		// install execution listeners once per editor type
		String eclass = editor.getClass().getName(); 
		if ((editors.get(eclass) == null)) {
			ICommandService ics = (ICommandService) editor.getSite().getService(ICommandService.class);
			if (ics != null) { 
				Command com = null;
				// remember listener install state
				editors.put(eclass, new InstallState());
				if ((com = ics.getCommand(IEmacsPlusCommandDefinitionIds.COPY_QUALIFIED_NAME)) != null) {
					com.addExecutionListener(KillRingListeners.getExecListener(false));
				}
				if ((com = ics.getCommand(IEmacsPlusCommandDefinitionIds.KILL_FORWARD_SEXP)) != null) {
					com.addExecutionListener(KillRingListeners.getExecListener(false));
				}
				if ((com = ics.getCommand(IEmacsPlusCommandDefinitionIds.KILL_BACKWARD_SEXP)) != null) {
					com.addExecutionListener(KillRingListeners.getExecListener(true));
				}
				if ((com = ics.getCommand(IEmacsPlusCommandDefinitionIds.EMP_CUT)) != null) {
					com.addExecutionListener(KillRingListeners.getExecListener(false));
				}
				if ((com = ics.getCommand(IEmacsPlusCommandDefinitionIds.EMP_COPY)) != null) {
					com.addExecutionListener(KillRingListeners.getCopyExecListener(false));
				}
				if ((com = ics.getCommand(IEmacsPlusCommandDefinitionIds.CUT_LINE)) != null) {
					com.addExecutionListener(KillRingListeners.getExecListener(false));
				}
				if ((com = ics.getCommand(IEmacsPlusCommandDefinitionIds.CUT_LINE_TO_BEGINNING)) != null) {
					com.addExecutionListener(KillRingListeners.getExecListener(true));
				}
				if ((com = ics.getCommand(IEmacsPlusCommandDefinitionIds.CUT_LINE_TO_END)) != null) {
					com.addExecutionListener(KillRingListeners.getExecListener(false));
				}
				if ((com = ics.getCommand(IEmacsPlusCommandDefinitionIds.DELETE_LINE)) != null) {
					com.addExecutionListener(KillRingListeners.getExecListener(false));
				}
				if ((com = ics.getCommand(IEmacsPlusCommandDefinitionIds.DELETE_LINE_TO_BEGINNING)) != null) {
					com.addExecutionListener(KillRingListeners.getExecListener(true));
				}
				if ((com = ics.getCommand(IEmacsPlusCommandDefinitionIds.DELETE_LINE_TO_END)) != null) {
					com.addExecutionListener(KillRingListeners.getExecListener(false));
				}
				if ((com = ics.getCommand(IEmacsPlusCommandDefinitionIds.DELETE_NEXT_WORD)) != null) {
					com.addExecutionListener(KillRingListeners.getExecListener(false));
				} 
				if ((com = ics.getCommand(IEmacsPlusCommandDefinitionIds.DELETE_PREVIOUS_WORD)) != null) {
					com.addExecutionListener(KillRingListeners.getExecListener(true));
				}
			}
		}
	}

	/**
	 * Disable bindings that would interfere with Ctrl digit-argument bindings
	 * If disableEmacs is true, then disable Emacs+ Ctrl arguments instead
	 * This requires the internal BindingService.removeBinding method
	 * 
	 * @param editor
	 * @param stateMask
	 * @param keys
	 * @param disableEmacs - if true, invert interpretation
	 * @return the collection of disabled bindings
	 */
	private Collection<Binding> disableBindings(IEditorPart editor, int stateMask, char[] keys, boolean disableEmacs) {
		Collection<Binding> removed = null;
		BindingService bs = getBindingService();
		// only works if we get access to internal Binding Service
		if (bs != null) {
			// cache this as its a bit expensive to compute
			Map<TriggerSequence,Collection<Binding>> total = (disableEmacs ? getTotalBindings() : null);
			for (char key : keys) {
				KeySequence trigger = KeySequence.getInstance(KeyStroke.getInstance(stateMask,key));
				Binding b = bs.getPerfectMatch(trigger); 
				boolean ise;
				if (b != null) {
					if ((!(ise = isEmacsBinding(b)) && !disableEmacs) || 
							(ise && disableEmacs && hasVariants(bs,total,trigger))) {
						bs.removeBinding(b);
						if (removed == null) {
							removed = new ArrayList<Binding>();
						}
						removed.add(b);
						b = bs.getPerfectMatch(trigger); 					
					}
				}
				if (!disableEmacs) {
					Map<TriggerSequence, Binding> bindings = EmacsPlusUtils.getPartialMatches(bs,trigger);
					if (!bindings.isEmpty()) {
						Collection<Binding> r = removeBindings(bindings.values(), bs, editor);
						if (r != null) {
							if (removed == null) {
								removed = r;
							} else {
								removed.addAll(r);
							}
						}
					}
				}
			}
		}
		return removed;
	}

	/**
	 * Check if the key sequence is either a prefix sequence or has multiple bindings
	 * (not necessarily conflicts)
	 * 
	 * @param bs
	 * @param bindings
	 * @param trigger
	 * @return true if condition is detected
	 */
	private boolean hasVariants(BindingService bs, Map<TriggerSequence,Collection<Binding>> bindings, KeySequence trigger) {
		boolean result = bs.isPartialMatch(trigger);
		if (!result) {
			Collection<Binding> binds = bindings.get(KeySequence.getInstance(trigger.getKeyStrokes()));
			result = (binds != null && binds.size() > 1);
		}
		return result;
	}
	
	/**
	 * Remove any bindings in the array that do not belong to Emacs+
	 * This requires the internal BindingService.removeBinding method
	 * 
	 * @param bindings
	 * @param editor
	 */
	private Collection<Binding> removeBindings(Collection<Binding> bindings, BindingService bs, IEditorPart editor) {
		ArrayList<Binding> result = null;
		if (bindings != null && bs != null) {
			for (Binding binding : bindings) {
				if (isEmacsBinding(binding)) {
					continue;
				} else {
					try {
						if (result == null) {
							result = new ArrayList<Binding>();
						}
						// smash and grab
						bs.removeBinding(binding);
						result.add(binding);
					} catch (Exception e) {
						// ignore any problems
					}
				}
			}
		}
		return result;
	}

	/**
	 * Attempt to restore bindings in the array
	 * This requires the internal BindingService.addBinding method
	 * 
	 * @param bindings
	 * @param bs
	 */
	private void restoreBindings(Collection<Binding> bindings, BindingService bs) {
		if (bindings != null && bs !=  null) {
			for (Binding binding : bindings) {
				try {
					bs.addBinding(binding);
				} catch (Exception e) {
					// ignore any error
				}
			}
		}
	}
	
	private boolean isEmacsBinding(Binding binding) {
		boolean result = false;
		ParameterizedCommand p = binding.getParameterizedCommand();
		if (p != null) {
			Command c = p.getCommand();
			if (c != null && c.getId().contains(EmacsPlusUtils.MULGASOFT)) {
				result = true;
			}
		}
		return result;
	}

	/**
	 * Check for presence of digit-argument bindings
	 * 
	 * @return true if digit-argument is relevant
	 */
	public boolean hasDigitBindings(boolean force) {
		boolean result = false;
		// The normal binding service returns the 'current' bindings, so we must get the context free set
		if (force || ctrlBindings == null) {
			Map<TriggerSequence,Collection<Binding>> bindings = getTotalBindings();
			if (!bindings.isEmpty()) {
				int modifier = EmacsPlusUtils.isMac() ? SWT.COMMAND : SWT.CTRL; 
				// kludge: just see of <MODIFIER>+0 is bound in our binding scheme (universal-argument)
				Collection<Binding> binds = bindings.get(KeySequence.getInstance(KeyStroke.getInstance(modifier,'0')));
				if (binds != null) {
					for (Binding b : binds) {
						if (IEmacsPlusCommandDefinitionIds.UNIVERSAL_ARGUMENT.equals(b.getParameterizedCommand().getId())) {
							result = true;
							break;
						}
					}
				}
				ctrlBindings = result;
			}
		} else {
			result = ctrlBindings;
		}
		return result;
	}
	
	/**
	 * Restore all the naked Eclipse actions from inside our delegates
	 * 
	 * @param editorpart
	 */
	private void removeActionListeners(IEditorPart editorpart) {
		String inputName = editorpart.getEditorInput().getName();
		if (actors.containsKey(inputName)) {
			actors.remove(inputName);
			ITextEditor editor = (ITextEditor) editorpart;
			removeClipActionListener(editor, IEmacsPlusCommandDefinitionIds.EMP_COPY);
			removeClipActionListener(editor, ActionFactory.COPY.getId());
			removeClipActionListener(editor, IEmacsPlusCommandDefinitionIds.EMP_CUT);
			removeClipActionListener(editor, ActionFactory.CUT.getId());
			removeClipActionListener(editor, IEmacsPlusCommandDefinitionIds.COPY_QUALIFIED_NAME);
			removeClipActionListener(editor, IEmacsPlusCommandDefinitionIds.COPYQUALIFIEDNAME);
		}
	}		
	
	/**
	 * Restore the naked Eclipse action stored inside our delegate
	 * 
	 * @param editor
	 * @param actionName
	 */
	private void removeClipActionListener(ITextEditor editor, String actionName) {
		IAction action = editor.getAction(actionName);
		if (action != null && (action instanceof EmacsActionDelegate)) {
			IAction innerAction = ((EmacsActionDelegate)action).getAction();
			if (innerAction != null) {
				editor.setAction(actionName, innerAction);
			} 
		}
	}
	
	/**
	 * Add (once per editor) the listeners necessary to capture copy and cut actions that 
	 * cannot be intercepted via the command mechanism
	 * 
	 * @param editorpart 
	 */
	private void addActionListeners(IEditorPart editorpart) {
		IAction action;
		if (editorpart instanceof ITextEditor) {
			ITextEditor editor = (ITextEditor) editorpart;
			String inputName = editorpart.getEditorInput().getName();
			if (!actors.containsKey(inputName)) {
				actors.put(inputName, inputName);
				// Egregious hack required because menu actions do not go through
				// the command handler
				addClipActionListener(editor, IEmacsPlusCommandDefinitionIds.EMP_COPY,true);
				addClipActionListener(editor, ActionFactory.COPY.getId(),true);
				addClipActionListener(editor, IEmacsPlusCommandDefinitionIds.EMP_CUT,false);
				addClipActionListener(editor, ActionFactory.CUT.getId(),false);

				// Specifically check for java version of the id
				String qname = IEmacsPlusCommandDefinitionIds.COPY_QUALIFIED_NAME;
				action = editor.getAction(qname);
				if (action == null) {
					// This is the generic form
					qname = IEmacsPlusCommandDefinitionIds.COPYQUALIFIEDNAME;
					action = editor.getAction(qname);
				}
				if (action != null && !(action instanceof KillRingListeners.EmacsActionDelegate)) {
					setAction(editor,qname, new KillRingListeners.EmacsCopyActionDelegate(action, qname));
				}
			}
		}
	}

	/**
	 * Wrap and replace the Eclipse action with our delegate
	 * 
	 * @param editor
	 * @param actionName
	 */
	private void addClipActionListener(ITextEditor editor, String actionName, boolean isCopy) {

		IAction action = editor.getAction(actionName);
		IAction delegateAction;
		if (action != null && (action instanceof TextEditorAction)) {
			if (isCopy) {
				delegateAction = new KillRingListeners.EmacsCopyActionDelegate((TextEditorAction) action, actionName);
			} else {
				delegateAction = new KillRingListeners.EmacsCutActionDelegate((TextEditorAction) action, actionName);
			}
			setAction(editor,actionName, delegateAction);
		}
	}	
	
	/**
	 * It is possible that setAction will call a listener that must be run in the UI thread,  
	 * so post the invocation. This was initially reported in the context of Flex Builder
	 * @param editor
	 * @param actionName
	 * @param action
	 */
	private void setAction(final ITextEditor editor, final String actionName, final IAction action) {
		EmacsPlusUtils.asyncUiRun(new Runnable() {
			public void run() {
				try {
					editor.setAction(actionName,action);
				} catch (Exception e) {
					// Print but ignore any exception at this point
					e.printStackTrace();
				}
			}
		});
	}
}
