/**
 * Copyright (c) 2009, 2014 Mark Feber, MulgaSoft
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package com.mulgasoft.emacsplus.e4.commands;

import java.util.List;

import org.eclipse.e4.core.contexts.Active;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.ui.model.application.ui.MUIElement;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.model.application.ui.basic.MTrimmedWindow;
import org.eclipse.e4.ui.model.application.ui.basic.MWindow;
import org.eclipse.ui.IEditorPart;

import com.mulgasoft.emacsplus.commands.EmacsPlusCmdHandler;

/**
 * other_frame: Select the ARGth different visible frame
 * 
 * @author mfeber - Initial API and implementation
 */
public class FrameOtherCmd extends E4WindowCmd {

	@Execute
	public Object execute(@Active MWindow apart, @Active IEditorPart editor, @Active EmacsPlusCmdHandler handler) {
		// This assumes that getDatchedFrames returns them in order
		List<MTrimmedWindow> frames = getDetachedFrames();
		if (frames != null && !frames.isEmpty()) {
			int size = frames.size()+1;
			int count = handler.getUniversalCount();
			int index = frames.indexOf(apart) + (count % size) + 1;
			index = (index < 0 ? index + size : (index < size) ? index : index - size);
			if (index == 0) {
				// just make the main window first in line
				focusIt(getEditArea(application.getChildren().get(0)));
			} else {
				focusIt(frames.get(--index));
			}
		}
		return null;
	}

	/**
	 * If we can find a selected MPart, activate it
	 * 
	 * @param ele
	 */
	private void focusIt(MUIElement ele) {
	 	MUIElement sel = getSelected(ele);
	 	// There's a bug somewhere in eclipse where this could return null, so check
	 	if (sel != null) {
	 		sel.setVisible(true);
	 		if (sel instanceof MPart) {
	 			reactivate((MPart)sel);
	 		}
	 	}
	}
}
