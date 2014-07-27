/**
 * Copyright (c) 2009, 2014 Mark Feber, MulgaSoft
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package com.mulgasoft.emacsplus.e4.commands;

import java.util.Collection;
import java.util.List;

import org.eclipse.e4.ui.model.application.ui.MElementContainer;
import org.eclipse.e4.ui.model.application.ui.MUIElement;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.model.application.ui.basic.MTrimmedWindow;
import org.eclipse.e4.ui.workbench.modeling.EModelService;

/**
 * join-other-frames: Join all frames to the main frame
 * join-frame: Join frame to the main frame
 * 
 * @author mfeber - Initial API and implementation
 */
public class FrameJoinCmd extends WindowJoinCmd {
	
	@Override
	void joinAll(MPart apart) {
		List<MTrimmedWindow> frames = getDetachedFrames();
		MElementContainer<MUIElement> last = getTopElement((MElementContainer<MUIElement>) apart.getParent());
		for (MTrimmedWindow mt : frames) {
			if (!mt.equals(last)) {
				joinOne(mt);
			}
		}
		// only join apart if its top element is a frame
		if ((Object)last instanceof MTrimmedWindow) {
			joinOne(apart);
		}
	}
	
	void joinOne(MTrimmedWindow mt) {
		// As long as it has any editor MParts, we can join it
		List<MPart> parts = getParts(mt, EModelService.IN_ANY_PERSPECTIVE);
		if (!parts.isEmpty()) {
			joinOne(parts.get(0));
		}
	}

	private List<MPart> getParts(MUIElement element, int location) {
		return modelService.findElements(element, null, MPart.class, EDITOR_TAG, location);		
	}

	protected MElementContainer<MUIElement> join2Stacks(MElementContainer<MUIElement> pstack, MElementContainer<MUIElement> dropStack, MPart apart) {
		// pstack may have been merged away by our getAdjacentElement, so fetch current one if necessary
		MElementContainer<MUIElement> stack = (pstack.getChildren().isEmpty() ? getParentStack(apart).getStack() : pstack);	
		return super.join2Stacks(stack, dropStack, apart);
	}	
	
	/**
	 * @see com.mulgasoft.emacsplus.e4.commands.E4WindowCmd#getAdjacentElement(org.eclipse.e4.ui.model.application.ui.MElementContainer, boolean, org.eclipse.e4.ui.model.application.ui.basic.MPart)
	 */
	protected MElementContainer<MUIElement> getAdjacentElement(MElementContainer<MUIElement> dragStack, MPart part, boolean stackp) {
		MElementContainer<MUIElement> result = null;
		if (dragStack != null) {
			MElementContainer<MUIElement> psash = dragStack.getParent();
			MElementContainer<MUIElement> top = getTopElement(psash); 
			if ((Object)top instanceof MTrimmedWindow) {
				// if we contain splits, remove them first
				if (top != psash) {
					super.joinAll(part);
				}
				Collection<MPart> parts = getParts(application.getChildren().get(0), EModelService.IN_SHARED_AREA);
				for (MPart p : parts) {
					List<MElementContainer<MUIElement>> all = getOrderedStacks(p);
					// if it has a PartStack, it sh/c/ould be an editor stack
					if (!all.isEmpty()) {
						result = all.get(0);
						break;
					};
				};
			}
		}
		return result;
	}

	protected void checkSizeData(MElementContainer<MUIElement> pstack, MElementContainer<MUIElement> dropStack) {
		// no-op don't change anything on frame merge
	}

}
