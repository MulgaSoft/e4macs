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

import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.part.MultiPageEditorPart;
import org.eclipse.ui.part.MultiPageEditorSite;
import org.eclipse.ui.texteditor.ITextEditor;

import com.mulgasoft.emacsplus.IRegisterLocation;
import com.mulgasoft.emacsplus.TecoRegister;

/**
 * Implements - jump-to-register 
 * Jump to the position and buffer saved in register R
 * 
 * @author Mark Feber - initial API and implementation
 */
public class RegisterJumpToHandler extends RegisterHandler implements INonEditingCommand {

	protected boolean needsSelection() {
		return false;
	}
	
	/**
	 * @see com.mulgasoft.emacsplus.minibuffer.IMinibufferExecutable#getMinibufferPrefix()
	 */
	public String getMinibufferPrefix() {
		return JUMP_PREFIX;
	}
	
	/**
	 * @see com.mulgasoft.emacsplus.minibuffer.IMinibufferExecutable#executeResult(org.eclipse.ui.texteditor.ITextEditor, java.lang.Object)
	 */
	public boolean doExecuteResult(ITextEditor editor, Object minibufferResult) {

		if (minibufferResult != null) {
			String key = (String)minibufferResult;
			IRegisterLocation location = TecoRegister.getInstance().getLocation(key);
			if (location != null) {
				IWorkbenchPage page = getWorkbenchPage();
				IEditorPart part = location.getEditor(); 
				int offset = location.getOffset();
				if (part != null) {
					// move to the correct page
					IEditorPart apart = part;
					IEditorSite esite = part.getEditorSite();
					if (esite instanceof MultiPageEditorSite) {
						apart = ((MultiPageEditorSite)esite).getMultiPageEditor();
						// handle multi page by activating the correct part within the parent
						if (apart instanceof MultiPageEditorPart) {
							((MultiPageEditorPart)apart).setActiveEditor(part);
						}
					}
					// now activate
					page.activate(apart);
					page.bringToTop(apart);
				} else {
					// restore the resource from the file system
					if (location.getPath() != null) {
						try {
							// loads and activates
							part = IDE.openEditor(page, location.getPath(), true);
							if (part instanceof IEditorPart) {
								if (part instanceof MultiPageEditorPart) {
									IEditorPart[] parts = ((MultiPageEditorPart)part).findEditors(part.getEditorInput());
									//  TODO this will only work on the first load of a multi page
									// There is no supported way to determine the correct sub part in this case
									// Investigate org.eclipse.ui.PageSwitcher (used in org.eclipse.ui.part.MultiPageEditorPart)
									// as a means for locating the correct sub page at this level
									for (int i = 0; i < parts.length; i++) {
										if (parts[i] instanceof ITextEditor) {
											((MultiPageEditorPart)part).setActiveEditor(parts[i]);
											part = parts[i];
											break;
										}
									}
								}
								location.setEditor((ITextEditor)part);
							}
						} catch (PartInitException e) {
							showResultMessage(editor, String.format(BAD_LOCATION,key + ' ' + e.getLocalizedMessage()), true);				
						}
					} else {
						showResultMessage(editor, String.format(NO_LOCATION,key), true);				
					}
				}
				if (part instanceof ITextEditor) {
					((ITextEditor) part).selectAndReveal(offset, 0);
					showResultMessage(editor, String.format(LOCATED, key), false);
				} else {
				
				}
			} else {
				showResultMessage(editor, NO_REGISTER, true);
			}
		}
		return true;
	}
	
}
