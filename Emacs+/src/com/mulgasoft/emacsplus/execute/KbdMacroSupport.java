/**
 * Copyright (c) 2009, 2010 Mark Feber, MulgaSoft
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package com.mulgasoft.emacsplus.execute;

import static com.mulgasoft.emacsplus.IEmacsPlusCommandDefinitionIds.KBDMACRO_END;
import static com.mulgasoft.emacsplus.IEmacsPlusCommandDefinitionIds.KBDMACRO_END_CALL;
import static com.mulgasoft.emacsplus.IEmacsPlusCommandDefinitionIds.KBDMACRO_EXECUTE;
import static com.mulgasoft.emacsplus.IEmacsPlusCommandDefinitionIds.KEYBOARD_QUIT;

import java.io.File;
import java.io.FilenameFilter;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.NotHandledException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.bindings.Binding;
import org.eclipse.jface.bindings.keys.KeySequence;
import org.eclipse.jface.bindings.keys.KeyStroke;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentListener;
import org.eclipse.jface.text.ITextViewerExtension;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.VerifyKeyListener;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.widgets.Event;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.commands.ICommandService;
import org.eclipse.ui.keys.IBindingService;
import org.eclipse.ui.texteditor.AbstractTextEditor;
import org.eclipse.ui.texteditor.ITextEditor;

import com.mulgasoft.emacsplus.Beeper;
import com.mulgasoft.emacsplus.EmacsPlusActivator;
import com.mulgasoft.emacsplus.EmacsPlusUtils;
import com.mulgasoft.emacsplus.IBeepListener;
import com.mulgasoft.emacsplus.IEmacsPlusCommandDefinitionIds;
import com.mulgasoft.emacsplus.RingBuffer;
import com.mulgasoft.emacsplus.YankRotate;
import com.mulgasoft.emacsplus.minibuffer.WithMinibuffer;
import com.mulgasoft.emacsplus.preferences.EMPListEditor;
import com.mulgasoft.emacsplus.preferences.EmacsPlusPreferenceConstants;

/**
 * A singleton class to hold and support keyboard macros
 * 
 * @author Mark Feber - initial API and implementation
 */
public class KbdMacroSupport extends RepeatingSupport {
	
	private static KbdMacroSupport instance = null;
	private static KbdMacro kbdMacro = null;
	
	private ISourceViewer viewer = null;
	private ITextEditor editor = null;
	private ICommandService ics = null; 
	
	private boolean isdefining = false;
	private static WithMinibuffer mini = null;
	
	private static final String CTRL_STR = EmacsPlusActivator.getResourceString("KbdMacro_CtrlStr");						   //$NON-NLS-1$	
	private static final String ALT_STR = EmacsPlusActivator.getResourceString("KbdMacro_AltStr");  						   //$NON-NLS-1$	
	private static final String CMD_STR = EmacsPlusActivator.getResourceString("KbdMacro_CmdStr");  						   //$NON-NLS-1$	
	private static final String SHIFT_STR = EmacsPlusActivator.getResourceString("KbdMacro_ShiftStr");  					   //$NON-NLS-1$	
	private static final String EXIT_STR = EmacsPlusActivator.getResourceString("KbdMacro_ExitStr");						   //$NON-NLS-1$	

	private static final String AUTO_LOAD_ERROR = EmacsPlusActivator.getResourceString("EmacsPlusPref_KbdMacroAutoLoadError"); //$NON-NLS-1$
	
	private static final String KBD_DEFINED = "KbdMacro_Defined";   														   //$NON-NLS-1$	
	private static final String KBD_DEFINING = "KbdMacro_Defining"; 														   //$NON-NLS-1$	
	private static final String KBD_ABORTED = "KbdMacro_Aborted";   														   //$NON-NLS-1$	
	private static final String KBD_NONE = "KbdMacro_No_Error"; 															   //$NON-NLS-1$	
	
	// sub-directory where kbd macros are stored
	static final String KBD_SUBDIR = "kbdmacros" + File.separator;  														   //$NON-NLS-1$
	// avoid displaying messages when loading kbd macros
	private boolean suppressWhileLoading = false;
	
	public enum LoadState {
		ALL,
		NONE,
		SOME;
	}
	
	private static LoadState autoLoadState = LoadState.NONE;
	static {
		try {
			IPreferenceStore store = EmacsPlusActivator.getDefault().getPreferenceStore();
			if (store != null) {
				setLoadState(LoadState.valueOf(store.getString(EmacsPlusPreferenceConstants.P_KBD_MACRO_AUTO_LOAD)));
			}
		} catch (Exception e) {
			// if bad value, then leave state at NONE
		}
	}
	
	private KbdMacroSupport() {}
	
	public static KbdMacroSupport getInstance() {
		if (instance == null) {
			instance = new KbdMacroSupport();
		}
		return instance;
	}

	/**
	 * Determine if the status messages should be suppressed
	 * (typically during a macro execution as they slow the execution down unnecessarily)
	 * 
	 * @return true if we should suppress the message
	 */
	public boolean suppressMessages() {
		return suppressWhileLoading || isExecuting();
	}
	
	// keep macros in name order for completion minibuffer commands
	private static TreeMap<String,KbdMacro> namedMacros = new TreeMap<String,KbdMacro>();
	
	public static TreeMap<String,KbdMacro> getCompletionList() {
		return (TreeMap<String,KbdMacro>)namedMacros; 
	}
	
	/**
	 * Interface for kbd macro execution completion listeners
	 * notification occurs on macro completion 
	 */
	public interface IKbdExecutionListener {
		void executionDone();
	}

	public boolean isDefining() {
		return isdefining;
	}

	public boolean isExecuting() {
		return executeCount > 0;
	}
	private int executeCount = 0;

	/**
	 * When defining, add the command binding to the definition 
	 * Should only be called from a minibuffer
	 *  
	 * @param binding
	 * @return true is kbd macro is executing
	 */
	public boolean isExecuting(Binding binding) {
		// If called with binding while defining, look for extraneous 
		// Key event in macro for removal
		if (isDefining() && !kbdMacro.isEmpty()) {
			kbdMacro.checkBinding(binding);
		}
		return isExecuting();
	}
	
	/**
	 * Add a minibuffer exit event directly
	 * Should only be called from a minibuffer  
	 */
	public void exitWhenDefining() {
		if (isDefining()) {
			// flag minibuffer exit
			kbdMacro.addExit();
		}
	}
	
	private void setEditor(ITextEditor editor) {
		this.editor = editor;
	}
	
	private ITextEditor getEditor() {
		return this.editor;
	}
	
	private void setViewer(ISourceViewer viewer) {
		this.viewer= viewer;
	}
	
	private void setRedraw(ISourceViewer view, boolean value) {
		if (view != null) {
			try {
				// some viewers may not have a text widget
				view.getTextWidget().setRedraw(value);
			} catch (Exception e) {}
		}
	}
	
	public void setExecuting(boolean is, ITextEditor editor, VerifyKeyListener vkf) {
		boolean wasExecuting = isExecuting();
		// keep track of nested macro executions
		if (is) {
			++executeCount;
		} else {
			--executeCount;
		}
		if (!wasExecuting && is) {
			whileExecuting = vkf;
			setViewer(findSourceViewer(editor));
			if (viewer != null) {
				setRedraw(viewer,false);
				if (whileExecuting != null
						&& viewer instanceof ITextViewerExtension) {
					((ITextViewerExtension) viewer)
							.prependVerifyKeyListener(whileExecuting);
				}
			}
			setEditor(editor); 
		} else if (!isExecuting() && viewer != null) {
			setRedraw(viewer,true);
			if (whileExecuting != null && viewer instanceof ITextViewerExtension){
				((ITextViewerExtension) viewer).removeVerifyKeyListener(whileExecuting);
				whileExecuting = null;
			}
			setEditor(null); 
		} 
	}
	
	/**
	 * Returns the minibuffer, if we're currently executing within one
	 * 
	 * @return minibuffer or null
	 */
	public static WithMinibuffer getKbdMinibuffer() {
		return mini;
	}

	/**
 	 * Set whether we're currently executing within a minibuffer
 	 * 
	 * @param minibuffer or null
	 */
	public static void setKbdMinibuffer(WithMinibuffer minibuffer) {
		mini = minibuffer;
	}

	public boolean hasKbdMacro() {
		return hasKbdMacro(false);
	}
	
	/**
	 * Are any macros currently defined?
	 * 
	 * @param named if true, then check named macros, else current
	 *  
	 * @return true if found one
	 */
	public boolean hasKbdMacro(boolean named) {
		return (named ? !namedMacros.isEmpty() : kbdMacro != null && !kbdMacro.isEmpty()); 
	}
	
	public boolean hasKbdMacro(String name) {
		return !namedMacros.isEmpty() && namedMacros.get(name) != null; 
	}
	
	public boolean isBusy() {
		return isDefining() || isExecuting(); 
	}
	
	public ArrayList<KbdEvent> getKbdMacroEvents(){
		return (kbdMacro != null ? kbdMacro.getKbdMacro() : null);
	}
	
	public ArrayList<KbdEvent> getKbdMacroEvents(String name){
		ArrayList<KbdEvent> result = null;
		KbdMacro namedMacro = namedMacros.get(name);
		if (namedMacro != null) {
			result = namedMacro.getKbdMacro();			
		}
		return result;
	}
	
	public KbdMacro getKbdMacro(String name){
		return ((name == null) ? kbdMacro : namedMacros.get(name));
	}
	
	/**
	 * Start the definition of a keyboard macro
	 * 
	 * @param editor
	 * @param append - if true, append to the current definition
	 */
	public void startKbdMacro(ITextEditor editor, boolean append) {
		
		if (!isExecuting()) {
			setEditor(editor);
			isdefining = true;
			ics = (ICommandService) editor.getSite().getService(ICommandService.class);
			// listen for command executions
			ics.addExecutionListener(this);
			addDocumentListener(editor);
			if (!append || kbdMacro == null) {
				kbdMacro = new KbdMacro();
			}
			setViewer(findSourceViewer(editor));
			if (viewer instanceof ITextViewerExtension) {
				((ITextViewerExtension) viewer).prependVerifyKeyListener(whileDefining);
			} else {
				viewer = null;
			}
			// add a listener for ^G
			Beeper.addBeepListener(KbdMacroBeeper.beeper);
			currentCommand = null;
		}
	}

	/**
	 * End the definition of a keyboard macro
	 *  
	 * @return true if macro was being defined, else false
	 */
	public boolean endKbdMacro() {
		return endKbdMacro(false);
	}		
	
	/**
	 * End the definition of a keyboard macro
	 *  
	 * @param abort if true, terminate with extreme prejudice
	 * @return true if macro was being defined, else false
	 */
	private boolean endKbdMacro(boolean abort) {
		boolean result = isDefining();
		if (result) {
			if (ics != null) {
				ics.removeExecutionListener(this);
				ics = null;
			}
			if (viewer != null) {
				if (viewer instanceof ITextViewerExtension) {
					((ITextViewerExtension) viewer)
							.removeVerifyKeyListener(whileDefining);
				}
				viewer = null;
			}
			if (abort) {
				// restore last from ring buffer
				restoreFromHistory();
			} else {
				addToHistory(kbdMacro);
			}
			ITextEditor ed = getEditor();
			if (ed != null) {
				removeDocumentListener(ed);			
				EmacsPlusUtils.showMessage(ed, (abort ? KBD_ABORTED : KBD_DEFINED), abort);
			}
			
			// remove a listener for ^G
			Beeper.removeBeepListener(KbdMacroBeeper.beeper);
			setEditor(null);
			isdefining = false;
			currentCommand = null;
		}
		return result;
	}
	
	/**
	 * When defining/executing a macro, during a minibuffer command, move key listener in front minibuffer's 
	 *  
	 * @param mini minibuffer being activated
	 * @param editor using the activated minibuffer
	 */
	public void continueKbdMacro (WithMinibuffer mini, ITextEditor editor) {
		if (isDefining()) {
			prependKeyListener(editor,whileDefining);
		}
		if (isExecuting()) {
			prependKeyListener(editor,whileExecuting);
		}
		KbdMacroSupport.setKbdMinibuffer(mini);
	}
	
	/**
 	 * When defining/executing a macro, remove/add key listeners on appropriate viewers during part activation
	 *  
	 * @param editor being activated
	 */
	public void continueKbdMacro(ITextEditor editor) {
		if (editor != null && (isDefining() || isExecuting())) {
			addDocumentListener(editor);			
			
			ISourceViewer newViewer = findSourceViewer(editor);
			if (isExecuting()) {
				prependKeyListener(newViewer,whileExecuting);
				// adjust redraw when executing
				setRedraw(viewer,true);
				setEditor(editor);
				setRedraw(newViewer,false);
			}
			if (isDefining()) {
				prependKeyListener(newViewer,whileDefining);
				EmacsPlusUtils.showMessage(editor, KBD_DEFINING, false);				
				setEditor(editor);
			}
			viewer = newViewer;
		}  
	}
	
	private void prependKeyListener(ITextEditor editor, VerifyKeyListener key) {
		if (key != null && editor != null) {
			if (viewer == findSourceViewer(editor)) {
				((ITextViewerExtension) viewer).prependVerifyKeyListener(key);
			}
		} 
	}
	
	private void prependKeyListener(ISourceViewer newViewer, VerifyKeyListener key) {
		if (key != null && viewer != newViewer) {
			((ITextViewerExtension) viewer).removeVerifyKeyListener(key);
			((ITextViewerExtension) newViewer).prependVerifyKeyListener(key);
		}		
	}
	
	/**
	 * Save a copy of the current macro under a name for this session only
	 * 
	 * @param name
	 */
	public String nameKbdMacro(String name) {
		return nameKbdMacro(name,kbdMacro.copyMacro(name));
	}

	/**
	 * Save macro under the name for this session only 
	 * 
	 * @param name
	 * @param kbdMacro  
	 * @return id
	 */
	public String nameKbdMacro(String name, KbdMacro kbdMacro) {
		String result = null;
		if (!isDefining() && !isExecuting() && kbdMacro != null) {
			result = EmacsPlusUtils.kbdMacroId(name); 
			namedMacros.put(name,kbdMacro);
		}
		return result;
	}
	
	private Event copyEvent(VerifyEvent event) {
		Event e = new Event();
		e.stateMask = event.stateMask;
		e.keyCode = event.keyCode;
		// display.post() wants the key character sans modifier
		e.character = (event.keyCode != 0 ? (char)event.keyCode : event.character);
//		e.character = event.character;
		e.type = SWT.KeyDown;
		e.doit = true;
		return e;
	}
	
	private Event matchEvent(char c, int keyCode, int mask) {
		Event e = new Event();
		e.keyCode = keyCode;
		e.character = c;
		e.stateMask = mask;
		e.type = SWT.KeyDown;
		e.doit = true;
		return e;
	}

	/**
	 * Add a key event to the macro
	 * This is typically either a character for insert 
	 * or a sub command key binding for a minibuffer
	 *  
	 * @param event
	 */
	private void addToMacro(VerifyEvent event) {
		KbdEvent eKbd = null;
		if (event.stateMask != 0) {
			autoFix(event);
			Event e = copyEvent(event);
			e.character = (char)event.keyCode;
			eKbd = new KbdEvent(e);
			kbdMacro.add(eKbd);
		} else if (event.keyCode != 27) {	// ignore ESC
			eKbd = new KbdEvent(copyEvent(event));
			autoFix(event);
			kbdMacro.add(eKbd);
		}
	}
	
	/**
	 * Add a command id to the macro
	 * @param cmdId
	 */
	private void addToMacro(String cmdId, Map<?,?> parameters, Event trigger) {
		kbdMacro.checkTrigger(cmdId, parameters, trigger,false);
	}
	
	public String toString() {
		return kbdMacro.makeString(false);
	}
	
	public String toBriefString() {
		return kbdMacro.makeString(true);
	}

	// File support
	
	private static String getKbdMacroDirectory() {
		return EmacsPlusActivator.getDefault().getPreferenceStore().getString(EmacsPlusPreferenceConstants.P_KBD_MACRO_DIRECTORY);
	}

	private static IPath getKbdMacroPath(String kbdMacroDirectory) {
		return (kbdMacroDirectory.length() != 0 ? Path.fromOSString(kbdMacroDirectory) : 
			EmacsPlusActivator.getDefault().getStateLocation().append(KBD_SUBDIR));
	}

	/**
	 * Get the map of saved kbd macros from the file system
	 * 
	 * @return a SortedMap of <short name, fileName>
	 */
	public static SortedMap<String,String> getFileMap() {
		return getFileMap(getKbdMacroDirectory());
	}
	
	public static SortedMap<String,String> getFileMap(String directory) {
		SortedMap<String,String> fileMap = new TreeMap<String,String>(); 
		IPath mpath = getKbdMacroPath(directory);
		String[] fileNames = null;
		File dir = mpath.toFile();
		if (dir.exists()) {
			// use the id prefix as the file filter
			fileNames = dir.list(new FilenameFilter() {
				public boolean accept(File dir, String name) {
					boolean result = false;
					if (name.startsWith(EmacsPlusUtils.KBD_MACRO_ID)) {
						result = true;
					} 
					return result;
				}
			});
		}
		if (fileNames != null) {
			int index = EmacsPlusUtils.KBD_MACRO_ID.length()+1;
			for (String f : fileNames) {
				fileMap.put(f.substring(index),f);
			}
		}
		return fileMap;
	}
	
	// Auto loading of kbd macros
	
	static LoadState getLoadState() {
		return autoLoadState;
	}
	
	public static void setLoadState(LoadState value) {
		autoLoadState = value;
	}
	
	public void autoLoadMacros() {
		String[] names = null;
		switch (getLoadState()) {
			case NONE:
				break;
			case SOME:
				String nameStr = EmacsPlusActivator.getDefault().getPreferenceStore().getString(EmacsPlusPreferenceConstants.P_KBD_MACRO_NAME_LOAD);
				names = EMPListEditor.parseResults(nameStr);
				break;
			case ALL:
				SortedMap<String,String> fileMap = getFileMap();
				names = fileMap.keySet().toArray(new String[0]);
				break;
			default:
				break;
		}
		if (names != null) {
			final ArrayList<String> results = new ArrayList<String>();
			for (String name : names) {
				final String n = name;
				// load each macro in a separate runnable so we don't provoke a timeout if many macros are loaded
				EmacsPlusUtils.asyncUiRun(new Runnable() {
					public void run() {
						suppressWhileLoading = true;
						String result = null;
						try {
							Map<String,String> param = new HashMap<String,String>();
							param.put(EmacsPlusUtils.KBDMACRO_NAME_ARG,n);
							param.put(EmacsPlusUtils.KBDMACRO_FORCE_ARG,Boolean.TRUE.toString());
							result = (String)EmacsPlusUtils.executeCommand(IEmacsPlusCommandDefinitionIds.KBDMACRO_LOAD,param,null);
							if (result != null) {
								results.add(result);
							}
						} catch (Exception e) {
							// most load errors should be trapped in the kbdmacro_load command, but if something strange happens...
							result = String.format(AUTO_LOAD_ERROR,n);
							results.add(result);
							System.err.println(result);
							e.printStackTrace();
						} finally {
							suppressWhileLoading = false;
						}
					}
				});
			}
			EmacsPlusUtils.asyncUiRun(new Runnable() {
				public void run() {
					if (!results.isEmpty()) {
						EmacsPlusConsole console = EmacsPlusConsole.getInstance();
						console.clear();
						console.activate();
						for (String r : results) {
							console.print(r + '\n');
						}
					}
				}
			});
		}
	}

	// All the pretty Listeners 
	
	private final static class KbdMacroBeeper {
		
		private final static IBeepListener beeper = new IBeepListener() {
			public void beepInterrupt() {
				Beeper.removeBeepListener(beeper);
				KbdMacroSupport support = KbdMacroSupport.getInstance();
				if (support.isDefining()) {
					support.endKbdMacro(true);
				} else if (support.isExecuting()) {

				}
			}
		}; 
	}

	/**
	 * Interrupt keyboard macro definition or execution
	 */
	public static void interruptKbdMacro() {
		KbdMacroSupport support = KbdMacroSupport.getInstance();
		if (support.isExecuting()) {
			Beeper.beep();			
		}
	}

// VerifyKeyListeners
	
	VerifyKeyListener whileExecuting= null;
	
	VerifyKeyListener whileDefining = new VerifyKeyListener() {
		/**	
		 * @see org.eclipse.swt.custom.VerifyKeyListener#verifyKey(org.eclipse.swt.events.VerifyEvent)
		 */
		public void verifyKey(VerifyEvent event) {
			try {
				if (!event.doit) {
					return;
				}
				if (event.character != 0) { // process typed character
					// ignore if we're in a nested macro execution
					if (whileExecuting == null) {
						charEvent(event);
					}
				} else { // some other key down 
				}
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
			}
		}
	}; 	
	
	private void charEvent(VerifyEvent event) {
		// ignore characters while in M-x, we'll pick up the command id on execution
		if (!isInMetaXCommand()) {
			addToMacro(event);
		}
	}
	
	private String currentCommand = null;
	
	// IExecutionListener methods 
	/**
	 * @see org.eclipse.core.commands.IExecutionListener#notHandled(java.lang.String, org.eclipse.core.commands.NotHandledException)
	 */
	public void notHandled(String commandId, NotHandledException exception) {
		currentCommand = null;
		popEvent();
		KbdMacroBeeper.beeper.beepInterrupt();
	}

	/**
	 * @see org.eclipse.core.commands.IExecutionListener#postExecuteFailure(java.lang.String, org.eclipse.core.commands.ExecutionException)
	 */
	public void postExecuteFailure(String commandId,
			ExecutionException exception) {
		currentCommand = null;
		popEvent();
		KbdMacroBeeper.beeper.beepInterrupt();
	}

	/**
	 * @see org.eclipse.core.commands.IExecutionListener#postExecuteSuccess(java.lang.String, java.lang.Object)
	 */
	public void postExecuteSuccess(String commandId, Object returnValue) {
		// true, if command was invoked by a binding
		ExecutionEvent event = popEvent();
		Event trigger = getTrigger(event); 
		// save cmdId if it immediately follows an M-x or if it was called from a key binding
		boolean addCmd = isInMetaXCommand() || trigger != null;
		if (isInMetaXCommand(commandId)) {
			; // ignore.  Sets flag as side-effect
		} else if (KEYBOARD_QUIT.equals(commandId)) {
			; // ignore. This can happen during appending to a definition			
		} else if (KBDMACRO_END_CALL.equals(commandId)) {
			; // ignore during definition
		} else if (KBDMACRO_END.equals(commandId)) {
			// check if called from minibuffer
			kbdMacro.checkTrigger(commandId, null, trigger,true);
		} else if (KBDMACRO_EXECUTE.equals(commandId)) {
			// check if called from minibuffer
			kbdMacro.checkTrigger(commandId, null, trigger,true);
			// If you enter `C-x e' while defining a macro, the macro is terminated and executed immediately.
			endKbdMacro();
		} else if (addCmd) {
			Map<?,?> parameters = event.getParameters();
			if (parameters != null && parameters.isEmpty()) {
				parameters = null;
			}
			// Add to queue
			addToMacro(commandId, parameters, trigger);
		}
		currentCommand = null;
	}

//	boolean inUniversalArg = false;
	private void popUniversal() {
		kbdMacro.popUniversal();
	}
	
	/**
	 * @see org.eclipse.core.commands.IExecutionListener#preExecute(java.lang.String, org.eclipse.core.commands.ExecutionEvent)
	 */
	public void preExecute(String commandId, ExecutionEvent event) {
		currentCommand = commandId;		
		currentEvent.push(event);
		// Emacs+ commands handle universal argument directly as a parameter, so remove the command
		// other Eclipse commands require the universalArgument command to be part of the macro  
//		if (inUniversalArg && commandId.startsWith(EmacsPlusUtils.MULGASOFT)) {
			Map<?,?> parameters = event.getParameters();
			// when a command has a universal arg parameter, remove the preceding universal arg command
			if (parameters != null && parameters.containsKey(EmacsPlusUtils.UNIVERSAL_ARG)) {
				popUniversal();
			}
//		}
//		inUniversalArg = ((IEmacsPlusCommandDefinitionIds.UNIVERSAL_ARGUMENT.equals(commandId)) ? true : false);

//		// true, if command was invoked by a binding
//		Event trigger = ((event.getTrigger() != null && event.getTrigger() instanceof Event) ? ((Event)event.getTrigger()) : null);
//		// save cmdId if it immediately follows an M-x or if it was called from a key binding
//		boolean addCmd = inMetaXCommand || trigger != null;
//		if (inUniversalArg && commandId.startsWith(EmacsPlusUtils.MULGASOFT)) {
//			popUniversal();
//		}
//		inMetaXCommand = false;
//		inUniversalArg = false;
//		if (IEmacsPlusCommandDefinitionIds.KBDMACRO_END.equals(commandId)) {
//			// check if called from minibuffer
//			kbdMacro.checkTrigger(commandId, null, trigger,true);
//		} else if (IEmacsPlusCommandDefinitionIds.KBDMACRO_EXECUTE.equals(commandId)) {
//			// check if called from minibuffer
//			kbdMacro.checkTrigger(commandId, null, trigger,true);
//			// If you enter `C-x e' while defining a macro, the macro is terminated and executed immediately.
//			endKbdMacro();
//		} else if (IEmacsPlusCommandDefinitionIds.METAX_EXECUTE.equals(commandId)) {
//			inMetaXCommand = true;			
//		} else if (IEmacsPlusCommandDefinitionIds.KEYBOARD_QUIT.equals(commandId)) {
//			; // ignore. This can happen during appending to a definition			
//		} else if (IEmacsPlusCommandDefinitionIds.KBDMACRO_END_CALL.equals(commandId)) {
//			; // ignore during definition
//		} else if (addCmd) {
//			if (IEmacsPlusCommandDefinitionIds.UNIVERSAL_ARGUMENT.equals(commandId)) {
//				inUniversalArg = true;
//			}
//			Map<?,?> parameters = event.getParameters();
//			if (parameters != null && parameters.isEmpty()) {
//				parameters = null;
//			}
//			// Add to queue
//			addToMacro(commandId, parameters, trigger);
//		}
	}
	
	private KbdMacroAutoFix listening = null;
	
	private void addDocumentListener(ITextEditor editor) {
		IDocument document = editor.getDocumentProvider().getDocument(editor.getEditorInput());		
		if (listening != null) {
			listening.document.removeDocumentListener(listening);
		}
		listening = new KbdMacroAutoFix(document);
		document.addDocumentListener(listening);
	}
	
	private void removeDocumentListener(ITextEditor editor) {
		IDocument document = editor.getDocumentProvider().getDocument(editor.getEditorInput());		
		if (listening != null && document == listening.document) {
			listening.document.removeDocumentListener(listening);
			listening = null;
		}
	}
	
	private void autoFix(VerifyEvent event) {
		if (listening != null) {
			listening.nextKey(event);
		}
	}
	
	/**
	 * Attempt to finesse the Eclipse auto-complete for the simplest cases.
	 * Listen for explicit document changes that are not directly part of the macro
	 * being recorded, and work around the internal state that Eclipse secretly maintains 
	 *  
	 * @author Mark Feber - initial API and implementation
	 */
	private class KbdMacroAutoFix implements IDocumentListener {
		
		IDocument document;
		VerifyEvent ev = null;
		
		KbdMacroAutoFix(IDocument document) {
			this.document = document;
		}
		
		public void nextKey(VerifyEvent event){
			ev = event;
		}
		
		/**
		 * Attempt to finesse the Eclipse auto-complete for the simplest cases
		 * - auto-completion of '"' and other identical completions
		 * - auto-insert of '.' or '<text>.'
		 *  
		 * @see org.eclipse.jface.text.IDocumentListener#documentAboutToBeChanged(org.eclipse.jface.text.DocumentEvent)
		 */
		public void documentAboutToBeChanged(DocumentEvent event) {
			try {
				String txt = event.getText();
				// not ready for prime time
//				if (ev != null && txt.length() == 2 && txt.charAt(0) == ev.character && txt.charAt(1) == ev.character) {
//					// looks like it's an auto match on identical character
//					KbdEvent e = new KbdEvent(matchEvent((char)ev.keyCode,ev.keyCode,(ev.stateMask & SWT.MODIFIER_MASK)));
//					e.dontWait = true;
//					// add it and back up
//					kbdMacro.add(e);					
//					addToMacro(IEmacsPlusCommandDefinitionIds.BACKWARD_CHAR,null,null);
//					//			} else if (currentCommand == null && txt.length() > 1 && txt.charAt(txt.length()-1) == '.') {
//				} else {
					// the java completion engine (and others?) will sometimes eat the . character and auto-complete
					if (currentCommand == null && txt.length() > 0 && txt.charAt(txt.length()-1) == '.') {
						if ((ev == null || ev.character != '.')) {
							ISelection selection = editor.getSelectionProvider().getSelection();
							if (selection instanceof TextSelection && (event.getOffset()+event.getLength()) == ((TextSelection)selection).getOffset()) {
								KbdEvent e = new KbdEvent(matchEvent('.',(int)'.',0));
								e.dontWait = true;
								kbdMacro.add(e);					
							}
						}
					}
//				}
			} catch (Exception e) {}
			ev = null;
		}

		/**
		 * @see org.eclipse.jface.text.IDocumentListener#documentChanged(org.eclipse.jface.text.DocumentEvent)
		 */
		public void documentChanged(DocumentEvent event) {
		}
		
	}

	// Kbd Macro related class definitions 

	public static class KbdMacro implements Serializable {

		private static final long serialVersionUID = 5999718072284785054L;
		
		private String name = null;
		private String bindSequence = null;
		ArrayList<KbdEvent> macro = new ArrayList<KbdEvent>();
		
		public void setBindingKeys(String sequence) {
			bindSequence = sequence;
		}
		
		public String getBindingKeys() {
			return bindSequence;
		}
		
		public String getName() {
			return name;
		}
		
		public void setName(String name) {
			this.name = name;
		}

		public void add(KbdEvent event) {
			macro.add(event);
		}

		public ArrayList<KbdEvent> getKbdMacro() {
			return macro;
		}

		public boolean isEmpty() {
			return macro.isEmpty();
		}
		
		private void popUniversal() {
			int size = macro.size();
			for (int i = size -1; i >=0; i--) {
				String cmdId = macro.get(i).getCmd();
				if (cmdId != null) {
					// the first command must be universal; intervening events, if present, will be number characters
					if (IEmacsPlusCommandDefinitionIds.UNIVERSAL_ARGUMENT.equals(cmdId)) {
						for (int j = i; i < size; i++) { 
							macro.remove(j);
						}
					}
					break;
				}
			}
		}

		/**
		 * Make a shallow copy of this macro
		 * 
		 * @return the shallow copy
		 */
		KbdMacro copyMacro(String name) {
			KbdMacro result = new KbdMacro();
			result.setName(name);
			result.macro = new ArrayList<KbdEvent>();
			for (KbdEvent e : macro) {
				result.macro.add(e);
			}
			return result;
		}

		void addExit() {
			if (!macro.isEmpty() && !macro.get(macro.size()-1).isExit()) {
				macro.add(new KbdEvent(true));
			}
		}
		
		/* There are two annoying cases related to minibuffers:
		 * 1) A command binding not handled by the minibuffer is detected:
		 *    - The minibuffer is exited
		 *    - and the command id is executed manually from there
		 *    -- handled by checkBinding (called from within the minibuffer code)
		 *
		 * 2) A binding that is not a full command and is not handled by the minibuffer
		 *    - the minibuffer is exited
		 *    - the event is forwarded on where it may be part of a multi-key binding
		 *    - checkTrigger (attempts to) handle this (called locally)
		 *    
		 * Some future version of the minibuffer may use the command structure to set up sub commands
		 * which would moot this code
		 */

		/**
		 * Check for a full command binding that was executed directly by the minibuffer
		 * Remove the binding entry and add the command id entry
		 * 
		 * @param binding
		 */
		void checkBinding(Binding binding) {
			boolean processed = false;
			int index = macro.size() -1;
			Event keyevent = macro.get(index).getEvent();
			if (keyevent != null) {
				KeyStroke keyStroke = KeyStroke.getInstance(keyevent.stateMask, (int)Character.toUpperCase((char)keyevent.keyCode));
				if (keyStroke.equals(binding.getTriggerSequence().getTriggers()[0])) {
					// remove (possibly partial) binding
					macro.remove(index);
					// flag for minibuffer exit
					addExit();
					// and insert command
					macro.add(new KbdEvent(binding.getParameterizedCommand().getId()));
					processed = true;
				}
			}			
			if (!processed) {
				// then it's a command unto itself (e.g. ARROW_RIGHT) 
				// flag for minibuffer exit
				addExit();
				// and insert command
				macro.add(new KbdEvent(binding.getParameterizedCommand().getId()));
				processed = true;	
			}
		}

		/**
		 * Check for a binding that is not a full command and was not handled by the minibuffer
		 * Remove the partial binding and add the command id entry
		 * 
		 * @param cmdId
		 * @param trigger
		 * @param onExit - it is the endMacro command, ignore id entry
		 */

		void checkTrigger(String cmdId, Map<?,?> parameters, Event trigger, boolean onExit) {
			int index = macro.size() -1;
			if (!macro.isEmpty() && trigger != null && macro.get(index).isSubCmd()) {
				Event event = macro.get(index).getEvent();
				// have trigger and previous entry is a subCmd type 
				KeyStroke key = KeyStroke.getInstance(event.stateMask, Character.toUpperCase(event.keyCode));
				IBindingService bindingService = (IBindingService) PlatformUI.getWorkbench().getService(IBindingService.class);
				Collection<Binding> values = EmacsPlusUtils.getPartialMatches(bindingService,KeySequence.getInstance(key)).values();
				// Check partial binding
				for (Binding v : values) {
					if (cmdId.equals((v.getParameterizedCommand().getId()))) {
						// remove (possibly partial) binding
						macro.remove(index);
						// flag for minibuffer exit
						macro.add(new KbdEvent(true));
						break;
					}
				}
			}
			if (!onExit) {
				macro.add(new KbdEvent(cmdId, parameters));
			}
		}

		public String toString() {
			return makeString(false);
		}

		public String toBriefString() {
			return makeString(true);
		}

		private String makeString(boolean isBrief) {
			StringBuilder mac = new StringBuilder();
			char sepr = (isBrief ? ',' : '\n');
			boolean wasChar = false;
			boolean isNext = false;
			for (KbdEvent e : macro) {
				if (isNext && (!wasChar || (wasChar && (!e.isChar() || e.isSubCmd())))) {
					mac.append(sepr);
				}
				mac.append((isBrief ? e.toBriefString() : e.toString()));
				wasChar = e.isChar();
				isNext = true;
			}
			return mac.toString();
		}
	}
	
	public static final class KbdEvent implements Serializable {

		// define serializable event type
		private class KbdKeyEvent implements Serializable {

			private static final long serialVersionUID = 6407500805253442702L;
			public int type;
			public int keyCode;
			public int stateMask;
			public char character;
		}
		
		private static final long serialVersionUID = 1500697848009159804L;
		KbdKeyEvent event;	// serialized key event
		private transient Event ev;	// cached key event
		String cmdId = null;
		Map<?,?> params = null;
		boolean exitMinibuffer = false;
		boolean dontWait = false;
		
		public KbdEvent(Event keyEvent) {
			if (keyEvent != null) {
				ev = keyEvent;
				event = new KbdKeyEvent();
				event.type = keyEvent.type;
				event.keyCode = keyEvent.keyCode;
				event.stateMask = keyEvent.stateMask;
				event.character = keyEvent.character;
			}
		}
		
		public KbdEvent(String cmdId) {
			this.cmdId = cmdId;
		}

		public KbdEvent(String cmdId, Map<?,?> params) {
			this(cmdId);
			this.params = params;
		}
		
		public KbdEvent(boolean exit) {
			this.exitMinibuffer= exit;
		}
		
		public Event getEvent() {
			Event keyEvent = ev;
			if (keyEvent == null && event != null) {
				keyEvent = new Event();
				keyEvent.type = event.type;
				keyEvent.keyCode = event.keyCode;
				keyEvent.stateMask = event.stateMask;
				keyEvent.character = event.character;
				keyEvent.doit = true;
				ev = keyEvent;
			}
			return keyEvent;
		}
		
		public String getCmd() {
			return cmdId;
		}
		public Map<?,?> getCmdParameters() {
			return params;
		}
		public boolean isChar() {
			return event != null;
		}
		public boolean isSubCmd() {
			return event != null && ((event.stateMask & SWT.MODIFIER_MASK) & ~SWT.SHIFT) != 0;
		}
		public boolean isExit() {
			return exitMinibuffer;
		}
		public boolean isWait() {
			return !dontWait;
		}
		
		public String toBriefString() {
			String result = null;
			if (cmdId != null) {
				result = cmdId;
				int last = cmdId.lastIndexOf('.');
				if (last > -1) {
					result = cmdId.substring(last+1);
				}
			} else {
				result = toString();
			}
			return result;
		}
		
		public String toString() {
			String result = null;
			if (cmdId != null) {
				result = cmdId;
				if (params != null) {
					try {
						@SuppressWarnings("unchecked")
						Set<String> keySet = (Set<String>)params.keySet();
						for (String key : keySet) {
							result += ' ' + key + '=' + params.get(key);
						}
					} catch (Exception e) {}
				}
			} else if (event != null) {
				if (event.stateMask != 0) {
					char c = (char)event.keyCode;
					if ((event.stateMask & SWT.SHIFT) != 0) {
						char uc = Character.toUpperCase(c);
						if (c == uc) {
							result = SHIFT_STR + c;
						} else {
							result = Character.toString(uc);
						}
					} else {
						result = Character.toString(c);
					}
					result = ((event.stateMask & SWT.CTRL) == 0 ? EmacsPlusUtils.EMPTY_STR : CTRL_STR)
					+ ((event.stateMask & SWT.COMMAND) == 0 ? EmacsPlusUtils.EMPTY_STR : CMD_STR)
					+ ((event.stateMask & SWT.ALT) == 0 ? EmacsPlusUtils.EMPTY_STR : ALT_STR)
					+ result;
				} else {
					result = EmacsPlusUtils.normalizeCharacter(event.character);
				}
			} else if (isExit()) {
				result = EXIT_STR;
			}
			return result;
		}
	}

	/* Local KbdMacro history ring */
	
	/**
	 * Rotate and make the next macro from the ring the current definition
	 * @param editor
	 */
	public void setHistoryNext(ITextEditor editor) {
		getFromHistory(editor,YankRotate.FORWARD);
	}
	
	/**
	 * Rotate and make the previous macro from the ring the current definition
	 * @param editor
	 */
	public void setHistoryPrevious(ITextEditor editor) {
		getFromHistory(editor,YankRotate.BACKWARD);
	}
	
	private void addToHistory(KbdMacro macro) {
		MacroRing.ring.putNext(macro);
	}
	
	/**
	 * Rotate make the next/previous macro from the ring the current definition
	 * @param editor
	 * @param dir YankRotate direction
	 */
	private void getFromHistory(ITextEditor editor, YankRotate dir) {
		String result = KBD_NONE;
		if (!isDefining() && !MacroRing.ring.isEmpty()) {		
			kbdMacro = MacroRing.ring.rotateYankPos(dir).get();
			result = kbdMacro.toBriefString();
		}
		EmacsPlusUtils.showMessage(editor, result, false);
	}

	/**
	 * Replace current macro with the top of history (or null, if none) 
	 */
	private void restoreFromHistory() {
		kbdMacro = (MacroRing.ring.isEmpty() ? null : MacroRing.ring.rotateYankPos(YankRotate.EREWHON).get());
	}

	/* Macro RingBuffer: use lazy initialization holder class idiom */
	
	private static class MacroRing {
		static final RingBuffer<KbdMacro> ring = new RingBuffer<KbdMacro>();		
	}
	
	// Totally evil code, as Eclipse has no adapter for accessing the viewer
	
	// The protected method & private field that gives us the source viewer for registration purposes
	private static String RE_METHOD_ID = "getSourceViewer"; //$NON-NLS-1$ 
	private static String RE_MEMBER_ID = "fSourceViewer";   //$NON-NLS-1$

	private ISourceViewer findSourceViewer(ITextEditor editor) {
 		// evil
		ISourceViewer result = null;
		if (editor != null && editor instanceof AbstractTextEditor) {
			result = (ISourceViewer) EmacsPlusUtils.getAM((AbstractTextEditor) editor, RE_METHOD_ID);
			if (result == null) {
				// even more evil
				result = (ISourceViewer) EmacsPlusUtils.getAF((AbstractTextEditor) editor, RE_MEMBER_ID);
			}
		}
		return result;
	}
}
