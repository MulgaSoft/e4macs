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
 * If only one frame, rotate through the editors it contains
 * 
 * @author mfeber - Initial API and implementation
 */
public class WindowOtherCmd extends E4WindowCmd {

	@Execute
	public Object execute(@Active MPart apart, @Active IEditorPart editor, @Active EmacsPlusCmdHandler handler) {
		PartAndStack ps = getParentStack(apart);
		MElementContainer<MUIElement> stack = ps.getStack();
		MPart part = ps.getPart();		
		
		List<MElementContainer<MUIElement>> stacks = getOrderedStacks(part);
		int count = handler.getUniversalCount();
		int size = stacks.size();
		if (size > 1) {
			int index = stacks.indexOf(stack) + (count % size);
			MElementContainer<MUIElement> nstack = (index < 0) ? stacks.get(size + index) : (index < size) ? stacks.get(index) : stacks.get(index - size); 
			reactivate((MPart)nstack.getSelectedElement());
		} else {
			List<MUIElement> children = stack.getChildren();
			size = children.size();
			int index = children.indexOf(part) + (count % size);
			MUIElement npart = (index < 0) ? children.get(size + index) : (index < size) ? children.get(index) : children.get(index - size);
			reactivate((MPart)npart);
		}
		return null;
	}

}
