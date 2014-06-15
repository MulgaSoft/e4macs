/**
 * Copyright (c) 2009, 2014 Mark Feber, MulgaSoft
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package com.mulgasoft.emacsplus.e4.commands;

import static com.mulgasoft.emacsplus.EmacsPlusUtils.getPreferenceStore;
import static com.mulgasoft.emacsplus.preferences.PrefVars.ENABLE_SPLIT_SELF;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.e4.ui.model.application.ui.MElementContainer;
import org.eclipse.e4.ui.model.application.ui.MUIElement;
import org.eclipse.e4.ui.model.application.ui.advanced.MArea;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.model.application.ui.basic.MPartSashContainer;
import org.eclipse.e4.ui.model.application.ui.basic.MPartSashContainerElement;
import org.eclipse.e4.ui.model.application.ui.basic.MPartStack;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;

import com.mulgasoft.emacsplus.EmacsPlusUtils;

/**
 * A base class for all split and join window commands (and their associates)
 * Use E4 injection
 *
 * @author mfeber - Initial API and implementation
 */
public abstract class E4WindowCmd extends E4Cmd {

	/**
	 * This is the default "size" for the layout, but for some reason it is not exposed and neither is a method
	 * for getting the total size by computation.  
	 */
	public static final int TOTAL_SIZE = 10000;
	
	// split editor in two when true, else just rearrange editors in stack
	private static boolean splitSelf = EmacsPlusUtils.getPreferenceBoolean(ENABLE_SPLIT_SELF.getPref());
	
	static {
		// listen for changes in the property store
		getPreferenceStore().addPropertyChangeListener(
				new IPropertyChangeListener() {
					public void propertyChange(PropertyChangeEvent event) {
						if (ENABLE_SPLIT_SELF.getPref().equals(event.getProperty())) {
							E4WindowCmd.setSplitSelf((Boolean)event.getNewValue());
						}
					}
				}
		);
	}

	/**
	 * @param splitSelf the splitSelf to set
	 */
	public static void setSplitSelf(boolean splitSelf) {
		E4WindowCmd.splitSelf = splitSelf;
	}

	/**
	 * @return the splitSelf
	 */
	public boolean isSplitSelf() {
		return splitSelf;
	}

	/**
	 * Activate the cursor in the part
	 * @param apart
	 */
	protected void reactivate(MPart apart) {
		partService.activate(null);
		partService.activate(apart, true);
	}

	/**
	 * Simple cons
	 * @author mfeber - Initial API and implementation
	 */
	class PartAndStack {
		PartAndStack(MPart part, MElementContainer<MUIElement> stack) {
			this.part = part;
			this.stack = stack;
		}
		private MPart part;
		private MElementContainer<MUIElement> stack;
		public MPart getPart() {return part;}
		public MElementContainer<MUIElement> getStack() {return stack;}
	}
	
	/**
	 * The parent of the apart is typically a PartStack, but it could be something under an MCompositePart, so look up
	 * @param apart the original selection
	 * @return the MPart and PartStack
	 */
	protected PartAndStack getParentStack(MPart apart) {
		MPartSashContainerElement part = apart;
		MElementContainer<MUIElement> stack = apart.getParent();
		try {
			while (!((MPartSashContainerElement)stack instanceof MPartStack)) {
				if ((MPartSashContainerElement)stack instanceof MArea) {
					// some unexpected structure
					stack = null;
					part = apart;
					break;
				} else {
					part = (MPartSashContainerElement)stack;
					stack = stack.getParent();
				}
			}
		} catch (Exception e) {
			// Bail on anything unexpected - will just make the command a noop
			stack = null;
			part = apart;
		}
		return new PartAndStack((MPart)part, stack);
	}

	/**
	 * Get the ordered list of stacks
	 * @param apart
	 * @return a list of MElementContainer<MUIElement> representing all the PartStacks
	 */
	protected List<MElementContainer<MUIElement>> getOrderedStacks(MPart apart) {
		List<MElementContainer<MUIElement>> result = new ArrayList<MElementContainer<MUIElement>>();
		MElementContainer<MUIElement> parent = apart.getParent();
		// get the outer container
		while (!((MPartSashContainerElement)parent instanceof MArea)) {
			parent = parent.getParent();
		}
		// first part stack is the destination of all the others
		getStacks(result, parent);
		return result;
	}
	
	private void getStacks(List<MElementContainer<MUIElement>> result, MElementContainer<MUIElement> container) {
		for (MUIElement child : container.getChildren()) {
			@SuppressWarnings("unchecked") // We type check all the way down
			MElementContainer<MUIElement> c = (MElementContainer<MUIElement>)child;
			if (child instanceof MPartStack) {
				result.add(c);
			} else {
				getStacks(result,c);
			}
		}
	}

	/**
	 * Find the first stack with which we should join within the current sash
	 * 
	 * @param dragStack the stack to join
	 * @return the target stack 
	 */
	@SuppressWarnings("unchecked")  // for safe cast to MElementContainer<MUIElement>
	protected MElementContainer<MUIElement> getAdjacentElement(MElementContainer<MUIElement> dragStack, boolean stackp) {
		MElementContainer<MUIElement> result = null;
		if (dragStack != null) {
			MElementContainer<MUIElement> psash = dragStack.getParent();
			// Trust but verify
			if ((MPartSashContainerElement)psash instanceof MPartSashContainer) {
				List<MUIElement> children = psash.getChildren();
				int size = children.size(); 
				if (size > 1) {
					int index = children.indexOf(dragStack)+1;
					result = (MElementContainer<MUIElement>)children.get((index == size) ? index - 2 : index);
					if (stackp) {
						result =  findTheStack(result);
					}
				}
			}
		}
		return result;
	}
	
	/**
	 * If parent is a sash, keep looking until we find a PartStack
	 * 
	 * @param parent a sash of PartStack
	 * @return the first PartStack we find
	 */
	@SuppressWarnings("unchecked")  // for safe cast to MElementContainer<MUIElement>
	private MElementContainer<MUIElement> findTheStack(MElementContainer<MUIElement> parent) {
		MElementContainer<MUIElement> result = parent;
		if ((MPartSashContainerElement)parent instanceof MPartSashContainer) {
			List<MUIElement> children = parent.getChildren();
			result = findTheStack((MElementContainer<MUIElement>)children.get(0));
		}
		return result;
	}

	/**
	 * Rotate through stacks by <count> elements
	 *  
	 * @param part the source part
	 * @param stack the source stack
	 * @param count the number to rotate by
	 * @return the destination stack
	 */
	protected MElementContainer<MUIElement> findNextStack(MPart part, MElementContainer<MUIElement> stack, int count) {
		MElementContainer<MUIElement> nstack = null;		
		List<MElementContainer<MUIElement>> stacks = getOrderedStacks(part);		
		int size = stacks.size();
		if (size > 1) {
			int index = stacks.indexOf(stack) + (count % size);
			nstack = (index < 0) ? stacks.get(size + index) : (index < size) ? stacks.get(index) : stacks.get(index - size); 
		}
		return nstack;
	}
	
	/**
	 * @param apart the selected part
	 * @return the most distant parent just below the MArea
	 */
	protected MElementContainer<MUIElement> getTopArea (MPart apart) {
		MElementContainer<MUIElement> parent = apart.getParent();
		while (!((MPartSashContainerElement)parent instanceof MArea)) {
			parent = parent.getParent();
		}
		return parent;
	}

	protected int getTotalSize(MPart apart) {
		int result = 0;
		List<MUIElement> topParts = getTopArea(apart).getChildren();
		for (MUIElement mui : topParts) {
			result += sizeIt(mui);
		}
		return (result == 0 ? TOTAL_SIZE : result);
	}
	
	private int sizeIt(MUIElement ele) {
		int result = 0;
		if (ele.getContainerData() != null) {
			result = getIntData(ele);
		} else if (ele instanceof MElementContainer) {
			@SuppressWarnings("unchecked") //checked
			List<MUIElement> mlist = ((MElementContainer<MUIElement>)ele).getChildren();
			for (MUIElement mui : mlist) {
				result += sizeIt(mui);
			}
		}
		return result;
	}
	
	protected int getIntData(MUIElement mui) {
		try {
			return Integer.parseInt(mui.getContainerData());
		} catch (NumberFormatException e) {
			// Ignore - someone has messed with the container data
			return 0;
		}
	}
}
