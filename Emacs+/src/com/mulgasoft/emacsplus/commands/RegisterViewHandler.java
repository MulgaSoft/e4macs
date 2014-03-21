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

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.ui.texteditor.ITextEditor;

import com.mulgasoft.emacsplus.IEmacsPlusCommandDefinitionIds;
import com.mulgasoft.emacsplus.IRegisterContents;
import com.mulgasoft.emacsplus.TecoRegister;
import com.mulgasoft.emacsplus.execute.EmacsPlusConsole;

/**
 * Display what is contained in register *R*
 * 
 * @author Mark Feber - initial API and implementation
 *
 */
public class RegisterViewHandler extends RegisterHandler implements INonEditingCommand {
	
	private static Color blueColor = new Color(null,0,0,255);	// blue	
	private static Color redColor = new Color(null,255,0,0);	// red	
	
	/**
	 * @see com.mulgasoft.emacsplus.commands.RegisterHandler#needsSelection()
	 */
	protected boolean needsSelection() {
		return false;
	}

	/**
	 * @see com.mulgasoft.emacsplus.minibuffer.IMinibufferExecutable#getMinibufferPrefix()
	 */
	public String getMinibufferPrefix() {
		return VIEW_PREFIX;
	}

	/**
	 * @see com.mulgasoft.emacsplus.commands.EmacsPlusCmdHandler#getDispatchId(java.lang.String, int)
	 */
	protected String getDispatchId(String id, int arg) {
		String result = null;
		String did = id;
		if (arg > 1) {
			did = IEmacsPlusCommandDefinitionIds.LIST_REGISTERS;
		}
		if (!did.equals(id)) {
			result = did;
		}
		return result;
	}
	
	/**
	 * @see com.mulgasoft.emacsplus.minibuffer.IMinibufferExecutable#executeResult(org.eclipse.ui.texteditor.ITextEditor, java.lang.Object)
	 */
	public boolean doExecuteResult(ITextEditor editor, Object minibufferResult) {
		
		if (minibufferResult != null) {
			String key = (String)minibufferResult;
			TecoRegister.getInstance();
			IRegisterContents contents = TecoRegister.getInstance().getContents(key);
			if (contents != null) {
				EmacsPlusConsole console = EmacsPlusConsole.getInstance();
				console.clear();
				console.activate();
				printContents(console,key,contents);
			} else {
				showResultMessage(editor, NO_REGISTER, true);
			}
		} else {
			showResultMessage(editor, NO_REGISTER, true);
		}
		return true;
	}

	void printContents(EmacsPlusConsole console, String register, IRegisterContents contents) {
		String[] rectangle;
		console.print(String.format(TecoRegister.NAME,register),blueColor,SWT.BOLD);
		if (contents.getNumber() != null) {
			console.print(CR + '<' + TecoRegister.NUMBER + '>' + ' ',redColor,SWT.ITALIC);
			console.print(contents.getNumber().toString());
		} else if (contents.getText() != null) {
			console.print(CR + '<' + TecoRegister.TEXT + '>' + CR,redColor,SWT.ITALIC);
			console.print('\"' + contents.getText() + '\"');
		} else if ((rectangle = contents.getRectangle()) != null) {
			console.print(CR + '<' + TecoRegister.RECTANGLE + '>' + CR,redColor,SWT.ITALIC);
			for (String txt : rectangle) {
				console.print('\"' + txt + '\"' + CR);
			}
		}

		if (contents.getLocation() != null) {
			console.print(CR + '<' + TecoRegister.POINT + '>' + ' ',redColor,SWT.ITALIC);
			console.print(contents.getLocation().toString());
		}
		console.print(CR);
	}
}
