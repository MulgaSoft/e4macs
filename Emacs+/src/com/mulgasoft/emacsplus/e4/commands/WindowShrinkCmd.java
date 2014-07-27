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

import javax.inject.Named;

import org.eclipse.e4.core.contexts.Active;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.ui.model.application.ui.MElementContainer;
import org.eclipse.e4.ui.model.application.ui.MUIElement;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;

import com.mulgasoft.emacsplus.commands.EmacsPlusCmdHandler;

/**
 * Make selected frame <adjustment> amount smaller or larger within it's PartSash
 * If the PartSash is horizontal, then the frame is made shorter or taller
 * else narrower or wider.  Differs from strict gnu emacs.
 * 
 * @author mfeber - Initial API and implementation
 */
public class WindowShrinkCmd extends E4WindowCmd {
	
	// The use of container data to establish size does not directly correspond to columns,
	// so pick a reasonable amount as a fraction of the total "size"
	private static final float adjustment = 0.025f;  
	private static final float minsize = 0.05f;
	
	public static enum Col {
		BALANCE, SHRINK, ENLARGE;
	}
	
	@Execute
	public Object execute(@Active MPart apart,  @Named(E4CmdHandler.CMD_CTX_KEY)Col stype, @Active EmacsPlusCmdHandler handler) {
		PartAndStack ps = getParentStack(apart);
		MElementContainer<MUIElement> stack = ps.getStack();
		MElementContainer<MUIElement> next = getAdjacentElement(stack, ps.getPart(), false);
		
		int count = handler.getUniversalCount();
		if (next != null) {
			switch (stype) {
			case SHRINK:
				adjustContainerData((MUIElement)stack, (MUIElement)next, count, getTotalSize(apart));
				break;
			case ENLARGE:
				adjustContainerData((MUIElement)next, (MUIElement)stack, count, getTotalSize(apart));
				break;
			case BALANCE:
				balancePartSash(stack);
				break;
			}
		}	
		return null;
	}

	/**
	 * Change the size of from relative to to by amount, but do not go below
	 * a minimum size.
	 * 
	 * @param from selected PartStack
	 * @param to adjacent PartStack
	 * @param amount by which to adjust each
	 */
	private void adjustContainerData(MUIElement from, MUIElement to, int count, int size) {
		int adjust = getIntData(from);;
		int adjustTo = getIntData(to);;
		int amount = (Math.round(size * adjustment)) * count;
		int minim = Math.round(size * minsize);
		if (adjust - amount < minim) {
			// enforce minimum size					
			amount = adjust - minim;
		}
		// take from the rich and give to the poor
		from.setContainerData(String.valueOf(adjust - amount));
		to.setContainerData(String.valueOf(adjustTo + amount));
	}	
	
	/**
	 * Just balance all the elements in the current PartSash
	 * TODO: This works well with simple arrangements, but not with complex trees
	 * 
	 * @param stack
	 */
	private void balancePartSash(MElementContainer<MUIElement> stack) {
		if (stack != null) {
			try {
				List<MUIElement> children = stack.getParent().getChildren();
				int adjust = 0;
				if (children.size() > 1) {
					for (MUIElement child : children) {
						adjust += Integer.parseInt(child.getContainerData());					
					}
					String adjustData = String.valueOf(adjust / children.size());
					for (MUIElement child : children) {
						child.setContainerData(adjustData);
					}
				}
			} catch (NumberFormatException e) {
				// Ignore - someone has messed with the container data
			}
		}
	}
}
