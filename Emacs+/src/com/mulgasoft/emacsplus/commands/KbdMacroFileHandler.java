/**
 * Copyright (c) 2009, 2010 Mark Feber, MulgaSoft
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package com.mulgasoft.emacsplus.commands;

import java.io.File;
import java.util.SortedMap;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.ui.texteditor.ITextEditor;

import com.mulgasoft.emacsplus.EmacsPlusActivator;
import com.mulgasoft.emacsplus.EmacsPlusUtils;
import com.mulgasoft.emacsplus.execute.KbdMacroSupport;
import com.mulgasoft.emacsplus.minibuffer.IMinibufferState;
import com.mulgasoft.emacsplus.minibuffer.YesNoMinibuffer;
import com.mulgasoft.emacsplus.preferences.EmacsPlusPreferenceConstants;

/**
 * Base class for saving and loading kbd macros
 * 
 * @author Mark Feber - initial API and implementation
 */
public abstract class KbdMacroFileHandler extends KbdMacroDefineHandler {

	private static String kbdMacroDirectory = EmacsPlusActivator.getDefault().getPreferenceStore().getString(EmacsPlusPreferenceConstants.P_KBD_MACRO_DIRECTORY);;
	
	// sub-directory where kbd macros are stored
	final static String KBD_SUBDIR = "kbdmacros" + File.separator;  						//$NON-NLS-1$
	final static String CANCEL = EmacsPlusActivator.getResourceString("KbdMacro_Canceled"); //$NON-NLS-1$  
	final static String NAME_ARG = EmacsPlusUtils.KBDMACRO_NAME_ARG; 
	final static String FORCE_ARG = EmacsPlusUtils.KBDMACRO_FORCE_ARG; 
		
	protected abstract IMinibufferState nameState();
	
	/**
	 * Get state to handle yes/no prompt
	 * 
	 * @param binding
	 * @param mini
	 * @return yes/no state
	 */
	IMinibufferState yesnoState(final String name, final String prompt, final IKbdMacroOperation operation) {
			
		return new IMinibufferState() {
			
			YesNoMinibuffer minibuffer = null;
			String macroName = name;
			File macroFile = null;
			
			public String getMinibufferPrefix() {
				return String.format(prompt,YESORNO_Y,YESORNO_N);
			}
	
			public int run(ITextEditor editor) {
				macroFile = macroFile(macroName);
				if (macroFile.exists()) {
					minibuffer = new YesNoMinibuffer(KbdMacroFileHandler.this,true);
					miniTransform(minibuffer, editor, null);
				} else {
					operation.doOperation(editor,macroName,macroFile);
				}
				return NO_OFFSET;
			}

			public boolean executeResult(ITextEditor editor, Object minibufferResult) {
				boolean result = true;
				if (minibufferResult != null && minibufferResult instanceof Boolean) { 
					if ((Boolean)minibufferResult) {
						operation.doOperation(editor,macroName,macroFile);
					} else {
						yesNoMessage(editor,name);
					}
				} else {
					miniTransform(minibuffer,editor,null);
					result = false;
				}
				return result;
			}
		};
	}
	
	/**
	 * Message to print if user selects no
	 * 
	 * @param editor
	 * @param name
	 */
	protected void yesNoMessage(ITextEditor editor, String name) {
		asyncShowMessage(editor, String.format(CANCEL, name), false);		
	}
	
	/**
	 * Construct the path and file name for a kbd macro
	 * - If the preference path is empty, use the $WORKSPACE/.metadata/.plugins/com.mulgasoft.emacsplus/kbdmacros directory
	 * - Create the directory if necessary 
	 * - Prepend emacsplus.keyboard.macro to the macro name if necessary
	 *  
	 * @param name - the short name of the kbd macro
	 * @return the kbd macro File
	 */
	File macroFile(String name) {
		String filename = (EmacsPlusUtils.isMacroId(name) ? name : EmacsPlusUtils.kbdMacroId(name));
		IPath mpath = getKbdMacroPath();
		File file = mpath.toFile();
		// if this is the first, make the sub directory
		if (!file.exists()) {
			file.mkdir();
		}
		return file = mpath.append(filename).toFile(); 
	}
	
	/**
	 * Get the completions list of named macros currently defined
	 * 
	 * @return a SortedMap of <String, KbdMacro>
	 */
	public SortedMap<String,?> getCompletions() {
		return KbdMacroSupport.getCompletionList();
	}
	
	/**
	 * Get the completions list of saved kbd macros from the file system
	 * 
	 * @return a SortedMap of <name, fileName>
	 */
	SortedMap<String,String> getFileCompletions() {
		return KbdMacroSupport.getFileMap();
	}
		
	protected IPath getKbdMacroPath() {
		return (kbdMacroDirectory.length() != 0 ? Path.fromOSString(kbdMacroDirectory) : 
			EmacsPlusActivator.getDefault().getStateLocation().append(KBD_SUBDIR));
	}
	
	public static String getKbdMacroDirectory() {
		return kbdMacroDirectory;
	}
	
	public static void setKbdMacroDirectory(String dir) {
		kbdMacroDirectory = dir.trim();
	}
	
	/**
	 * @see com.mulgasoft.emacsplus.commands.EmacsPlusCmdHandler#isLooping()
	 */
	protected boolean isLooping() {
		return false;
	}
}
