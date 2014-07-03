/**
 * Copyright (c) 2009, 2014 Mark Feber, MulgaSoft
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package com.mulgasoft.emacsplus.e4.commands;

import javax.inject.Named;

import org.eclipse.e4.core.contexts.Active;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.ui.model.application.ui.MElementContainer;
import org.eclipse.e4.ui.model.application.ui.MUIElement;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.internal.e4.compatibility.CompatibilityEditor;

import com.mulgasoft.emacsplus.EmacsPlusUtils;
import com.mulgasoft.emacsplus.commands.EmacsPlusCmdHandler;

/**
 * Dispatch a command against the adjacent frame's window 
 * 
 * @author mfeber - Initial API and implementation
 */
@SuppressWarnings("restriction") // E4 hackery
public class OtherWindowCmd extends E4WindowCmd {
	
		@Execute
		protected void doOtherWindow(@Active MPart apart, @Named(E4CmdHandler.CMD_CTX_KEY)String cmd, @Active EmacsPlusCmdHandler handler) {
			MElementContainer<MUIElement> otherStack = getAdjacentElement(getParentStack(apart).getStack(), true);
			MPart other = (MPart)otherStack.getSelectedElement();
			// TODO An egregious hack that may break at any time
			// Is there a defined way of getting the IEditorPart from an MPart?
			if (other.getObject() instanceof CompatibilityEditor) {
				IEditorPart editor = ((CompatibilityEditor) other.getObject()).getEditor();
				try {
					reactivate(other);
					if (handler.isUniversalPresent()) {
						EmacsPlusUtils.executeCommand(cmd, handler.getUniversalCount(), null, editor);
					} else {
						EmacsPlusUtils.executeCommand(cmd, null, editor);
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
				finally {
					reactivate(apart);
				}
			}
		}

}
