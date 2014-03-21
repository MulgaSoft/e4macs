/**
 * Copyright (c) 2009, 2010, Mark Feber, MulgaSoft
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 */
package com.mulgasoft.emacsplus.commands;

import java.io.File;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.jface.bindings.TriggerSequence;
import org.eclipse.jface.bindings.keys.KeySequence;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.ui.keys.IBindingService;
import org.eclipse.ui.texteditor.ITextEditor;

import com.mulgasoft.emacsplus.EmacsPlusActivator;
import com.mulgasoft.emacsplus.EmacsPlusUtils;
import com.mulgasoft.emacsplus.execute.KbdMacroSupport;
import com.mulgasoft.emacsplus.execute.KbdMacroSupport.KbdMacro;
import com.mulgasoft.emacsplus.minibuffer.IMinibufferState;
import com.mulgasoft.emacsplus.minibuffer.KbdMacroMinibuffer;

// Location user = Platform.getUserLocation();
// Location config = Platform.getConfigurationLocation();

/**
 * Implement save-kbd-macro
 * 
 * Save the named kbd macro to an external file 
 * 
 * @author Mark Feber - initial API and implementation
 */
public class KbdMacroSaveHandler extends KbdMacroFileHandler {

	private static String KBD_SAVE_PREFIX = EmacsPlusActivator.getResourceString("KbdMacro_Save_Prefix"); //$NON-NLS-1$  
	private static String QUESTION = EmacsPlusActivator.getResourceString("KbdMacro_File_Exists");  	  //$NON-NLS-1$  
	private static String SAVED = EmacsPlusActivator.getResourceString("KbdMacro_Saved");   			  //$NON-NLS-1$  
	private static String ABORT_SAVE = EmacsPlusActivator.getResourceString("KbdMacro_Abort_Save"); 	  //$NON-NLS-1$  
	
	/**
	 * @see com.mulgasoft.emacsplus.commands.EmacsPlusCmdHandler#transform(ITextEditor, IDocument, ITextSelection, ExecutionEvent)
	 */
	@Override
	protected int transform(ITextEditor editor, IDocument document, ITextSelection currentSelection,
			ExecutionEvent event) throws BadLocationException {
		
		if (KbdMacroSupport.getInstance().hasKbdMacro(true)) {
			mbState = nameState();
			return mbState.run(editor);
		} else {
			asyncShowMessage(editor, NO_MACRO_ERROR, true);
		}
		return NO_OFFSET;
	}


	/**
	 * Get state to handle prompt for getting kbd macro name
	 * 
	 * @return naming prompt state
	 */
	protected IMinibufferState nameState() {
	
		return new IMinibufferState() {
			public String getMinibufferPrefix() {
				return KBD_SAVE_PREFIX;
			}
			
			public int run(ITextEditor editor) {
				miniTransform(new KbdMacroMinibuffer(KbdMacroSaveHandler.this),editor,null);
				return NO_OFFSET;
			}
			
			public boolean executeResult(ITextEditor editor, Object minibufferResult) {
				boolean result = true;
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
				return result;
			}
			
			private void transitionState(ITextEditor editor, String name) {
				mbState = yesnoState(name,QUESTION, new IKbdMacroOperation() {
					public void doOperation(ITextEditor editor, String name, File file) {
						saveMacro(editor,name,file);
					}
				});
				mbState.run(editor);
			}
		};
	}

	private void saveMacro(ITextEditor editor,String name, File file) {
		IBindingService service = (IBindingService) editor.getSite().getService(IBindingService.class);
		KbdMacro kbdMacro = KbdMacroSupport.getInstance().getKbdMacro(name);
		TriggerSequence sequence = service.getBestActiveBindingFor(EmacsPlusUtils.kbdMacroId(name));
		if (sequence != null && sequence instanceof KeySequence) {
			kbdMacro.setBindingKeys(((KeySequence)sequence).toString());
		}
		writeMacro(editor,file,kbdMacro);
		kbdMacro.setBindingKeys(null);
	}
			
	private void writeMacro(ITextEditor editor, File file, KbdMacro kbdMacro) {
		try {
			file.createNewFile();
			FileOutputStream fos = new FileOutputStream(file);
			ObjectOutputStream oos = new ObjectOutputStream(fos);
			oos.writeObject(kbdMacro);
			oos.flush();
			oos.close();
			asyncShowMessage(editor, String.format(SAVED, kbdMacro.getName()), false);
		} catch (Exception e) {
			asyncShowMessage(editor, String.format(ABORT_SAVE, file.toString(), e.getMessage()), true);
		}		
	}

}
