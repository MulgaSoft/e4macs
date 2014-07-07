/**
 * Copyright (c) 2009, 2014 Mark Feber, MulgaSoft
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 */
package com.mulgasoft.emacsplus;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import org.eclipse.core.commands.AbstractParameterValueConverter;
import org.eclipse.core.commands.Command;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.IParameter;
import org.eclipse.core.commands.NotEnabledException;
import org.eclipse.core.commands.NotHandledException;
import org.eclipse.core.commands.ParameterType;
import org.eclipse.core.commands.ParameterValueConversionException;
import org.eclipse.core.commands.Parameterization;
import org.eclipse.core.commands.ParameterizedCommand;
import org.eclipse.core.commands.common.CommandException;
import org.eclipse.core.commands.common.NotDefinedException;
import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.jface.bindings.Binding;
import org.eclipse.jface.bindings.BindingManager;
import org.eclipse.jface.bindings.TriggerSequence;
import org.eclipse.jface.bindings.keys.KeySequence;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.BadPositionCategoryException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentExtension4;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.TypedPosition;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Event;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.commands.ICommandService;
import org.eclipse.ui.handlers.IHandlerService;
import org.eclipse.ui.internal.WorkbenchPage;
import org.eclipse.ui.internal.keys.BindingService;
import org.eclipse.ui.keys.IBindingService;
import org.eclipse.ui.part.MultiEditor;
import org.eclipse.ui.part.MultiPageEditorPart;
import org.eclipse.ui.texteditor.ITextEditor;

import com.mulgasoft.emacsplus.execute.KbdMacroSupport;
import com.mulgasoft.emacsplus.preferences.EmacsPlusPreferenceConstants;
import org.eclipse.jface.util.Util;

// Getting an arbitrary widget:
//	Control focus= page.getWorkbenchWindow().getShell().getDisplay().getFocusControl();

/**
 * Assorted utility methods
 * 
 * @author Mark Feber - initial API and implementation
 */
@SuppressWarnings("restriction")	// for cast to internal class org.eclipse.ui.internal.WorkbenchPage 
public class EmacsPlusUtils {
	
	private static final String N_GEN = "\\c";	//$NON-NLS-1$
	private static final String N_NEW = "\\n";	//$NON-NLS-1$
	private static final String N_RET = "\\r";	//$NON-NLS-1$
	private static final String N_TAB = "\\t";	//$NON-NLS-1$
	private static final String N_BS = "\\b";	//$NON-NLS-1$
	private static final String N_FF = "\\f";	//$NON-NLS-1$
	
	// The assumption is that the position categories will be part of this document category
	private static final String DOC_CAT = "__content_types_category";   									  //$NON-NLS-1$
	// In Java at least, these point to collapsible regions in the doc as well as other things 
	public static final String DOC_FNS = "__dflt_position_category";   									      //$NON-NLS-1$
	// java specific comment
	//	public static String JAVA_DOC = org.eclipse.jdt.ui.text.IJavaPartitions.JAVA_DOC;
	// use javadoc string directly to avoid having a dependency of jdt.ui
	public static final String JAVA_DOC = "javadoc";														  //$NON-NLS-1$
	public static final String[] JAVADOC_POS= {JAVA_DOC};
	// The assumption is that generic comment position categories will include 'comment' in their name 
	public static final String[] COMMENT_POS= {"comment",JAVA_DOC}; 										  //$NON-NLS-1$ 
	// The assumption is that string position categories will include 'string' in their name 
	public static final String[] STRING_POS= {"string"};													  //$NON-NLS-1$
	// collect them all for efficiency
	public static final String[] ALL_POS= {"comment",JAVA_DOC,"string"};									  //$NON-NLS-1$ //$NON-NLS-2$
	
	// used for sanity checks on commands (invocation, etc.)
	public static final String MULGASOFT = "com.mulgasoft"; 												  //$NON-NLS-1$
	
	// Emacs+ plugin bundles symbolic names
	public static final String EMP_STR = "com.mulgasoft.emacsplus"; 										  //$NON-NLS-1$
	public static final String EMP_OPT_STR = "com.mulgasoft.emacsplus.optional";							  //$NON-NLS-1$
	public static final String EMP_MACCMD_STR = "com.mulgasoft.emacsplus.maccmd";   						  //$NON-NLS-1$
	public static final String EMP_MACCMD_OPT_STR = "com.mulgasoft.emacsplus.maccmd.optional";  			  //$NON-NLS-1$
	
	public static final String EMP_SCHEMEID = "com.mulgasoft.emacsplusConfiguration";   					  //$NON-NLS-1$
	
	// for defining and executing keyboard macros
	public static final String KBD_MACRO_ID = "emacsplus.keyboard.macro";   								  //$NON-NLS-1$
	private static final String KBD_INTERRUPT = EmacsPlusActivator.getResourceString("KbdMacro_Interrupted"); //$NON-NLS-1$
	
	public static final String EMPTY_STR = "";  															  //$NON-NLS-1$
	/** No-op offset value */
	public static final int NO_OFFSET = -1;
	
	/** the universal-argument name used by Emacs+ */	
	public static final String UNIVERSAL_ARG = "universalArg";  											  //$NON-NLS-1$	
	/** the load kbd macro name argument used by Emacs+ */	
	public static final String KBDMACRO_NAME_ARG = "Name";  												  //$NON-NLS-1$	
	/** force the kbd macro load without any questions */	
	public static final String KBDMACRO_FORCE_ARG = "Force";												  //$NON-NLS-1$
	
	// the getBindingManager method is first exposed in Galileo
	private static final String BM_METHOD_ID = "getBindingManager"; 										  //$NON-NLS-1$
	private static final String BM_MEMBER_ID = "bindingManager";											  //$NON-NLS-1$

	private static final class MacCheck {  
		static final boolean isMac = Util.isMac(); 
	}

	// control whether to disable pre-edit of Non_Spacing_Marks: i.e. characters intended to be 
	// combined with another character without taking up extra space (e.g. accents, umlauts, etc.) 
	private static boolean disableOptionIMEPreferenece = getPreferenceBoolean(EmacsPlusPreferenceConstants.P_DISABLE_INLINE_EDIT);

	private EmacsPlusUtils() {}	// no children/instances allowed
	
	public static boolean getPreferenceBoolean(String key) {
		return getPreferenceStore().getBoolean(key);
	}
	
	public static String getPreferenceString(String key) {
		return getPreferenceStore().getString(key);
	}
	
	public static IPreferenceStore getPreferenceStore() {
		return EmacsPlusActivator.getDefault().getPreferenceStore();
	}
	
	public static boolean isMac() {
		return MacCheck.isMac;
	}	

	public static void setOptionIMEPreferenece(boolean optionIMEPreferenece) {
		disableOptionIMEPreferenece = optionIMEPreferenece;
	}

	public static boolean isDisableOptionIMEPreferenece() {
		return disableOptionIMEPreferenece;
	}

	public static String getTypeCategory(IDocument doc){
		return getTypeCategory(doc, DOC_CAT);
	}
	
	public static String getTypeCategory(IDocument doc, String cat){
		String result = null;
		String[] cats = doc.getPositionCategories();
		for (String icat : cats) {
			if (icat.contains(cat)) {
				result = icat;
				break;
			}
		}
		return result;
	}

	/**
	 * Get all positions of the given position category
	 * In sexp's these positions are typically skipped
	 * 
	 * @param doc
	 * @param pos_names
	 * @return the positions to exclude
	 */
	public static List<Position> getExclusions(IDocument doc, String[] pos_names) {
		List<Position> excludePositions = new LinkedList<Position>();
		String cat = getTypeCategory(doc);
		if (cat != null) {
			try {
				Position[] xpos = doc.getPositions(cat);
				for (int j = 0; j < xpos.length; j++) {
					if (xpos[j] instanceof TypedPosition) {

						for (int jj = 0; jj < pos_names.length; jj++) {
							if (((TypedPosition) xpos[j]).getType().contains(pos_names[jj])) {
								excludePositions.add(xpos[j]);
							}
						}
					}
				}
			} catch (BadPositionCategoryException e) {}
		}
		return excludePositions;
	}
	
	/**
	 * Determine if the offset is in one of the Positions in the Position list
	 * 
	 * @param doc
	 * @param positions
	 * @param offset
	 * @return true if in a Position
	 */
	public static Position inPosition(IDocument doc, List<Position> positions, int offset) {
		for (Position p : positions) {
			if (p.includes(offset)){
				return p;
			}
		}
		return null;
	}

	/**
	 * Greedy comment determinator: Since multiple comments can be adjacent, return the one furthest from the 
	 * from the offset
	 * 
	 * @param doc - what's up
	 * @param positions - the list of semantic comment Positions
	 * @param offset - the point to examine
	 * @param reverse - the direction we're headed
	 * @return - the furthest comment Position
	 */
	public static Position inCommentPosition(IDocument doc,List<Position> positions, int offset, boolean reverse) {
		// Comment positions include the <CR>, so when there are two line comments
		// one after another at line begin, we have to spot both comments to disambiguate
		ListIterator<Position> iter = positions.listIterator((reverse ? positions.size() : 0));
		Position pos;
		Position pos1 = null;
		while (reverse ? iter.hasPrevious() : iter.hasNext() ){
			pos = (reverse ? iter.previous() : iter.next());  
			if (pos1 != null) {
				// check if adjacent comments
				if (reverse ? pos1.includes(pos.getOffset() + pos.getLength()) : pos.includes(pos1.getOffset() + pos1.getLength())){
					pos1 = pos;
				}
				break;
			}
			if (pos.includes(offset)) {
				// offset is just before comment
				if (pos.getOffset() == offset){
					return null;
				} else {
					pos1 = pos;
				}
			}
		}
		return pos1;
	}
	
	public static IDocument getThisDocument(ITextEditor editor) {
		IDocument doc = null;
		if (editor != null) {
			doc = editor.getDocumentProvider().getDocument(editor.getEditorInput());			
		}
		return doc;
	}

	/**
	 * Extract the text editor from the editor.
	 * For multi part editors, try dereferencing via a getAdapter call
	 * 
	 * @param editor
	 * @return the ITextEditor or null
	 */
	public static ITextEditor getActiveTextEditor(IEditorPart editor) {
		ITextEditor result = null;
		if (editor != null) {
			if (editor instanceof ITextEditor) {
				result = (ITextEditor) editor;
			} else {
				// this retrieves the currently active editor from MultiPageEditors & etc.
				result = (ITextEditor) editor.getAdapter(ITextEditor.class);
				// dig further if other (older?) type
				if (result == null && (editor instanceof MultiEditor)) {
					IEditorPart epart = ((MultiEditor) editor).getActiveEditor();
					// trust but verify
					if (epart != editor) {
						result = getActiveTextEditor(epart);
					}
				}
			}
		}
		return result;
	}
	
	/**
	 * If we're a  multi-page editor, try to extract the text editor 
	 * 
	 * @param editor the selected editor
	 * @param activate force the text editor to be the active page
	 * @return a text editor or null
	 */
	public static ITextEditor getTextEditor(IEditorPart editor, boolean activate) {
		ITextEditor result = null;
		if (editor != null) {
			if (editor instanceof ITextEditor) {
				result = (ITextEditor) editor;
			} else {
				// this should retrieve the currently active editor from MultiPageEditors & etc.
				result = (ITextEditor) editor.getAdapter(ITextEditor.class);
				// dig further if multi type
				if (result == null) {
					if (editor instanceof MultiEditor) {
						// this code is ancient - not sure if there are any plain MultiEditors anymore
						IEditorPart epart = ((MultiEditor) editor).getActiveEditor();
						// potentially recurse
						if (epart != editor) {
							result = getTextEditor(epart, activate);
						}
					} else if (editor instanceof MultiPageEditorPart) {
						MultiPageEditorPart med = (MultiPageEditorPart)editor;
						IEditorPart[] eds = med.findEditors(med.getEditorInput());
						for (IEditorPart ep : eds) {
							if (ep instanceof ITextEditor) {
								result = (ITextEditor)ep;
								if (activate) {
									med.setActiveEditor(result);
									IWorkbenchPage wpage = getWorkbenchPage();
									wpage.bringToTop(result);
									wpage.activate(result);
								}
								break;
							}
						}
					} 
				}
			}
		}
		return result;
	}

	// sorted here means by activation order (e4), or reverse activation order (pre-e4)
	public static IEditorReference[] getSortedEditors(IWorkbenchPage page) {
		IEditorReference[] result = null;
		if (page != null && page instanceof WorkbenchPage) {
			result = ((WorkbenchPage) page).getSortedEditors();			
		}
		return result;
	}
	
	public static final String getEol(IDocument document) {
		String eol = "\n"; 	//$NON-NLS-1$
		try {
			if (document instanceof IDocumentExtension4) {
				eol = ((IDocumentExtension4)document).getDefaultLineDelimiter();
			} else {
				eol = document.getLineDelimiter(0);
			}

		} catch (BadLocationException e) {
		}
		return eol;
	}

	/**
	 * Invoke the specified command using the handler service
	 * 
	 * @param commandId
	 * @param event
	 * @param editor
	 * @throws ExecutionException
	 * @throws NotDefinedException
	 * @throws NotEnabledException
	 * @throws NotHandledException
	 * @throws CommandException 
	 */
	public static Object executeCommand(String commandId, Event event)
	throws ExecutionException, NotDefinedException, NotEnabledException, NotHandledException, CommandException {
		return executeCommand(commandId,event,(IHandlerService) PlatformUI.getWorkbench().getService(IHandlerService.class));
	}

	/**
	 * Invoke the specified command using the handler service from the editor site
	 * 
	 * @param commandId
	 * @param event
	 * @param editor
	 * @throws ExecutionException
	 * @throws NotDefinedException
	 * @throws NotEnabledException
	 * @throws NotHandledException
	 * @throws CommandException 
	 */
	public static Object executeCommand(String commandId, Event event, IWorkbenchPart editor)
	throws ExecutionException, NotDefinedException, NotEnabledException, NotHandledException, CommandException {
		return executeCommand(commandId,event,(IHandlerService) editor.getSite().getService(IHandlerService.class));
	}
	
	private static Object executeCommand(String commandId, Event event, IHandlerService service)
	throws ExecutionException, NotDefinedException, NotEnabledException, NotHandledException, CommandException {
		Object result = null;
		if (service != null) {
			try {
				MarkUtils.setIgnoreDispatchId(true);
				result =  service.executeCommand(commandId, event);
			} finally {
				MarkUtils.setIgnoreDispatchId(false);
			}		
		}
		return result;
	}

	/**
	 * Invoke the specified parameterized command using the handler service
	 * 
	 * @param commandId
	 * @param parameters
	 * @param event
	 * @param editor
	 * @return the result of the execution
	 * @throws ExecutionException
	 * @throws NotDefinedException
	 * @throws NotEnabledException
	 * @throws NotHandledException
	 * @throws CommandException
	 */
	public static Object executeCommand(String commandId, Map<String,?> parameters, Event event)
	throws ExecutionException, NotDefinedException, NotEnabledException, NotHandledException, CommandException {
		return executeCommand(commandId, parameters, event,
				(ICommandService) PlatformUI.getWorkbench().getService(ICommandService.class),
				(IHandlerService) PlatformUI.getWorkbench().getService(IHandlerService.class));
	}
	
	public static Object executeCommand(String commandId, Integer count, Event event, IWorkbenchPart editor) 
	throws ExecutionException, NotDefinedException, NotEnabledException, NotHandledException, CommandException {	
			Map<String, Integer> parameters = new HashMap<String, Integer>();
			parameters.put(EmacsPlusUtils.UNIVERSAL_ARG, count);
		return (editor == null) ? executeCommand(commandId, parameters, event) : executeCommand(commandId, parameters, event, editor);
	}
	
	/**
	 * Invoke the specified parameterized command using the handler service from the editor site
	 * 
	 * @param commandId
	 * @param parameters
	 * @param event
	 * @param editor
	 * @return the result of the execution
	 * @throws ExecutionException
	 * @throws NotDefinedException
	 * @throws NotEnabledException
	 * @throws NotHandledException
	 * @throws CommandException
	 */
	public static Object executeCommand(String commandId, Map<String,?> parameters, Event event, IWorkbenchPart editor)
	throws ExecutionException, NotDefinedException, NotEnabledException, NotHandledException, CommandException {
		return executeCommand(commandId,parameters,event,
				(ICommandService) editor.getSite().getService(ICommandService.class),
				(IHandlerService) editor.getSite().getService(IHandlerService.class));
	}

	private static Object executeCommand(String commandId, Map<String,?> parameters, Event event, ICommandService ics, IHandlerService ihs)
	throws ExecutionException, NotDefinedException, NotEnabledException, NotHandledException, CommandException {
		Object result = null;
		if (ics != null && ihs != null) {
			Command command = ics.getCommand(commandId);
			if (command != null) {
				try {
					MarkUtils.setIgnoreDispatchId(true);
					ParameterizedCommand pcommand = generateCommand(command, parameters);
					if (pcommand != null) {
						result = ihs.executeCommand(pcommand, event);
					}
				} finally {
					MarkUtils.setIgnoreDispatchId(false);
				}		
			}
		}
		return result;
	}

	/**
	 * Provide an implementation that is Europa compatible as this method wasn't added to 
	 * ParameterizedCommand until 3.4
	 * 
	 * @see org.eclipse.core.commands.ParameterizedCommand#generateCommand(Command, Map) 
	 */
	private static final ParameterizedCommand generateCommand(Command command, Map<String, ?> parameters) {
		// no parameters
		if (parameters == null || parameters.isEmpty()) {
			return new ParameterizedCommand(command, null);
		}

		try {
			ArrayList<Parameterization> parms = new ArrayList<Parameterization>();
			// iterate over given parameters
			for (String key : parameters.keySet()) {
				IParameter parameter = null;
				// get the parameter from the command
				parameter = command.getParameter(key);

				// if the parameter is defined add it to the parameter list
				if (parameter == null) {
					return null;
				}
				ParameterType parameterType = command.getParameterType(key);
				if (parameterType == null) {
					parms.add(new Parameterization(parameter, (String) parameters.get(key)));
				} else {
					AbstractParameterValueConverter valueConverter = parameterType.getValueConverter();
					if (valueConverter != null) {
						String val = valueConverter.convertToString(parameters.get(key));
						parms.add(new Parameterization(parameter, val));
					} else {
						parms.add(new Parameterization(parameter, (String) parameters.get(key)));
					}
				}
			}
			// convert the parameters to an Parameterization array and create the command
			return new ParameterizedCommand(command, (Parameterization[]) parms.toArray(new Parameterization[parms.size()]));
		} catch (NotDefinedException e) {
		} catch (ParameterValueConversionException e) {
		}
		return null;
	}

	/**
	 * Returns the status line manager of the workbench part.
	 * 
	 * @param part an IEditorPart or IViewPart
	 * 
	 * @return the status line manager of the part
	 */
	public synchronized static IStatusLineManager getStatusLineManager(IWorkbenchPart part){
		IStatusLineManager result = null;
		if (part instanceof IEditorPart) {
			return ((IEditorPart)part).getEditorSite().getActionBars().getStatusLineManager();
		} else if (part instanceof IViewPart) {
			return ((IViewPart)part).getViewSite().getActionBars().getStatusLineManager();
		} 
		return result;
	}

	public static void forceStatusUpdate(IWorkbenchPart editor) {
		forceUpdate(EmacsPlusUtils.getStatusLineManager(editor));
	}

	// maintain silence when executing keyboard macros to increase execution speed
	public static void showMessage(IWorkbenchPart editor, String key, boolean error){
		if (error || !KbdMacroSupport.getInstance().suppressMessages()) {
			if (error) {
				if (key != null && key.length() > 0 && !isMacroMessage(key)) {
					beep();
				}
				setError(editor,key);
			} else {
				setError(editor,EMPTY_STR);
				setMessage(editor,key);			
			}
		}
	}

	public static void clearMessage(IWorkbenchPart editor){
		if (!KbdMacroSupport.getInstance().suppressMessages()) {
			setError(editor,EMPTY_STR);
			setMessage(editor,EMPTY_STR);			
		}
	}
	
	private static void setMessage(IWorkbenchPart editor, String key){
		if (!KbdMacroSupport.getInstance().suppressMessages()) {
			IStatusLineManager ism = getStatusLineManager(editor); 
			if (ism != null) {
				ism.setMessage(EmacsPlusActivator.getResourceString(key));
				forceUpdate(ism);
			}
		}
	}
	
	private static void setError(IWorkbenchPart editor, String key){
		IStatusLineManager ism = getStatusLineManager(editor); 
		if (ism != null) {
			ism.setErrorMessage(EmacsPlusActivator.getResourceString(key));
			forceUpdate(ism);
		}
	}
	
	private static void forceUpdate(IStatusLineManager ism) {
		ism.update(true);
	}

	/**
	 * Causes the <code>run()</code> method of the runnable to
	 * be invoked by the user-interface thread at the next 
	 * reasonable opportunity.
	 *  
	 * @param runner - a Runnable object
	 */
	public static final void asyncUiRun(Runnable runner) {
		PlatformUI.getWorkbench().getDisplay().asyncExec(runner);
	}

	public static void beep() {
		Beeper.beep();
	}	
	
	public static IWorkbenchPage getWorkbenchPage() {
		IWorkbenchPage result = null;
		IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
		if (window != null) {
			result = window.getActivePage();
		}
		return result;
	}
	
	public static ITextEditor getCurrentEditor() {
		ITextEditor result = null;
		IWorkbenchPage page = getWorkbenchPage();
		if (page != null) {
			IEditorPart activeEditor = page.getActiveEditor();
			result = getActiveTextEditor(activeEditor);
		}
		return result;
	}

	/**
	 * Return the context free set of key bindings
	 * 
	 * @return A map of trigger (<code>TriggerSequence</code>) to bindings (
	 *         <code>Collection</code> containing <code>Binding</code>).
	 *         or an empty map
	 */
	public static Map<TriggerSequence,Collection<Binding>> getTotalBindings() {
		Map<TriggerSequence,Collection<Binding>> result = Collections.emptyMap(); 
		BindingService bs = getBindingService();
		if (bs != null) {
			@SuppressWarnings("unchecked")	// @see org.eclipse.jface.bindings.BindingManager#getActiveBindingsDisregardingContext()
			Map<TriggerSequence,Collection<Binding>> bindings = getBindingManager(bs).getActiveBindingsDisregardingContext();
			result = bindings;
		}
		return result;
	}

	public static BindingService getBindingService() {
		BindingService result = null;
		IBindingService bindingService = (IBindingService) PlatformUI.getWorkbench().getService(IBindingService.class);
		if (bindingService instanceof BindingService) {
			result = (BindingService)bindingService;
		}					
		return result;
	}

	// the getBindingManager method is first exposed in Galileo 
	public static BindingManager getBindingManager(BindingService bs) {
		BindingManager result = null;
		if (bs != null) {
			try {
				// check for a public version of the method
				Method method = bs.getClass().getMethod(BM_METHOD_ID, (Class[])null);
				result = (BindingManager)method.invoke(bs, (Object[])null);
			} catch (Exception e) {
				// evil, but backward compatible
				result = (BindingManager) EmacsPlusUtils.getAM(bs, BM_METHOD_ID);
				if (result == null) {
					// even more evil
					result = (BindingManager) EmacsPlusUtils.getAF(bs, BM_MEMBER_ID);
				}
			}
		}
		return result;
	}

	@SuppressWarnings("unchecked")
	public static Map<TriggerSequence,Binding> getPartialMatches(IBindingService ibs, KeySequence ks) {
//		 juno e4 code was hosed, so temporarily use a different path to the results we need
//		return getBindingManager(((ibs instanceof BindingService) ? (BindingService)ibs : getBindingService())).getPartialMatches(ks);
		// original version is supposed to be fixed in Kepler+ - not sure I trust it
		return ibs.getPartialMatches(ks);
	}

	public static String normalizeString(String message, int count) {
		
		String result = message;
		if (message != null && message.length() > 0){
			StringBuilder originalBuf = new StringBuilder(message);
			StringBuilder normalizedBuf = new StringBuilder(message.length());
			int mLen = (count > 0 ? Math.min(count, message.length()) : message.length());
			for (int i=0; i < mLen; i++) {
				String charac;
				int ocp = originalBuf.codePointAt(i);
				if (ocp < ' ') {
					switch (ocp) {
					case SWT.CR:
						charac = N_RET;
						break;
					case SWT.LF:
						charac = N_NEW;
						break;
					case '\t':
						charac = N_TAB;
						break;
					case '\f':
						charac = N_FF;
						break;
					case '\b':
						charac = N_BS;
						break;
					default:
						charac = N_GEN + ocp;
					} 
				} else {
					charac = String.valueOf(originalBuf.charAt(i));
				}
				normalizedBuf.append(charac);
			}
			result = normalizedBuf.toString();
		}
		return result;
	}

	public static boolean isMacroId(String id) {
		return id != null && id.startsWith(KBD_MACRO_ID);
	}

	public static boolean isMacroMessage(String message) {
		return message != null && message.startsWith(KBD_INTERRUPT);
	}

	public static String kbdMacroId(String name) {
		return KBD_MACRO_ID + '.' + name;
	}

	public static String normalizeCharacter(int keyCode) {
		String result = null;
		if (keyCode < ' ') {
			switch (keyCode) {
			case SWT.CR:
				result = N_RET;
				break;
			case SWT.LF:
				result = N_NEW;
				break;
			case '\t':
				result = N_TAB;
				break;
			case '\f':
				result = N_FF;
				break;
			case '\b':
				result = N_BS;
				break;
			default:
				result = N_GEN + keyCode;
			}
		} else {
			result = String.valueOf((char)keyCode);
		}
		return result;
	}

	/**
	 * Add some flexibility to negative Integer string parsing
	 *  -n == -n as expected
	 *  n- == -n
	 *  - == -1
	 * any other - will throw a number format exception
	 *     
	 * @param number string
	 * @return int value of String
	 */
	public static int emacsParseInt(String number) {
		int result = -1;
		int len = number.length();
		if (len > 0 && number.charAt(len-1) == '-') {
			if (len-1 > 0) {
				result = Integer.valueOf(number.substring(0,len-1));
			} else {
				result = 1;
			}
			result = -result;
		} else {
			result = Integer.valueOf(number);		
		}
		return result;
	}

	// Evil code: Provides execution of non-public method
	public static Object getAM(Object o, String methodName) {
		/* Go and invoke the inaccessible field... */
		Class<?> c = o.getClass();
		try {
			do {
				try {
					// assume empty parameter list for our purposes
					Method meth = c.getDeclaredMethod(methodName, new Class<?>[] {});
					boolean access = meth.isAccessible();
					Object obj;
					try {
						if (!access) {
							meth.setAccessible(true);
						}
						obj = meth.invoke(o, new Object[] {});
					} finally {
						if (!access) {
							meth.setAccessible(false);
						}
					}
					return obj;
				} catch (NoSuchMethodException e) {}
				c = c.getSuperclass();
			} while (c != null);
		} catch (SecurityException e) {
		} catch (IllegalArgumentException e) {
		} catch (IllegalAccessException e) {
		} catch (InvocationTargetException e) {
		}
		return null;
	}

	// Evil code: Provides access to non-public members in classes
	public static Object getAF(Object o, String fieldName) {
		/* Check we have valid arguments */
		/* Go and find the private field... */
		Class<?> c = o.getClass();
		try {
			do {
				Field fields[] = c.getDeclaredFields();
				for (int i = 0; i < fields.length; ++i) {
					if (fieldName.equals(fields[i].getName())) {
						boolean access = fields[i].isAccessible();
						Object obj;
						try {
							if (!access) {
								fields[i].setAccessible(true);
							}
							obj = fields[i].get(o);
						} finally {
							if (!access) {
								fields[i].setAccessible(false);
							}
						}
						return obj;
					}
				}
				c = c.getSuperclass();
			} while (c != null);
		} catch (SecurityException e) {
		} catch (IllegalArgumentException e) {
		} catch (IllegalAccessException e) {
		}
		return null;
	}
}
