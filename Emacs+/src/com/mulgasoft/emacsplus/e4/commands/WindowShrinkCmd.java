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
	// so pick a reasonable amount... could be set up as a property.
	private static final int adjustment = 500;  
	private static final int minsize = 500;
	
	public static enum Col {
		BALANCE, SHRINK, ENLARGE;
	}
	
	@Execute
	public Object execute(@Active MPart apart,  @Named(E4CmdHandler.CMD_CTX_KEY)Col stype, @Active EmacsPlusCmdHandler handler) {
		PartAndStack ps = getParentStack(apart);
		MElementContainer<MUIElement> stack = ps.getStack();
		MElementContainer<MUIElement> next = getAdjacentElement(stack, false);
		int count = handler.getUniversalCount();
		if (next != null) {
			switch (stype) {
			case SHRINK:
				adjustContainerData((MUIElement)stack, (MUIElement)next, adjustment * count);
				break;
			case ENLARGE:
				adjustContainerData((MUIElement)next, (MUIElement)stack, adjustment * count);
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
	private void adjustContainerData(MUIElement from, MUIElement to, Integer amount) {
		try {
			int adjust = Integer.parseInt(from.getContainerData());
			int adjustTo = Integer.parseInt(to.getContainerData());
			if (amount != null) {
				if (adjust - amount < minsize) {
					// enforce minimum size					
					amount = adjust - minsize;
				}
				// take from the rich and give to the poor
				from.setContainerData(String.valueOf(adjust - amount));
				to.setContainerData(String.valueOf(adjustTo + amount));
			}
		} catch (NumberFormatException e) {
			// Ignore - someone has messed with the container data
		}
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
