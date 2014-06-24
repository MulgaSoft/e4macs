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
import org.eclipse.e4.ui.model.application.ui.MElementContainer;
import org.eclipse.e4.ui.model.application.ui.MUIElement;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.ui.IEditorPart;

import com.mulgasoft.emacsplus.commands.EmacsPlusCmdHandler;

/**
 * Other-Window: Rotate forward (or backward if uArg < 0) through the selected element of each frame (PartStack)
 * If only one frame, rotate through the editors it contains by stack order
 * 
 * @author mfeber - Initial API and implementation
 */
public class WindowOtherCmd extends E4WindowCmd {
	@Execute
	public Object execute(@Active MPart apart, @Active IEditorPart editor, @Active EmacsPlusCmdHandler handler) {
		int count = handler.getUniversalCount();
		PartAndStack ps = getParentStack(apart);
		MPart part = ps.getPart();		
		MElementContainer<MUIElement> stack = ps.getStack();
		
		MElementContainer<MUIElement> nstack = findNextStack(apart, stack, count);
		if (nstack != null) {
			part = (MPart)nstack.getSelectedElement();
		} else {
			List<MUIElement> children = stack.getChildren();
			int size = children.size();
			int index = children.indexOf(part) + (count % size);
			part = (MPart)((index < 0) ? children.get(size + index) : (index < size) ? children.get(index) : children.get(index - size));
		}
		reactivate(part);
		return null;
	}

}
