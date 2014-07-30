/**
 * Copyright (c) 2009, 2014 Mark Feber, MulgaSoft
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package com.mulgasoft.emacsplus.e4.commands;

import static com.mulgasoft.emacsplus.EmacsPlusUtils.isMac;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import org.eclipse.e4.core.contexts.Active;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.ui.model.application.ui.MElementContainer;
import org.eclipse.e4.ui.model.application.ui.MUIElement;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.model.application.ui.basic.MPartSashContainerElement;
import org.eclipse.e4.ui.model.application.ui.basic.MPartStack;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorPart;

import com.mulgasoft.emacsplus.Beeper;
import com.mulgasoft.emacsplus.EmacsPlusUtils;
import com.mulgasoft.emacsplus.IEmacsPlusCommandDefinitionIds;
import com.mulgasoft.emacsplus.commands.EmacsPlusCmdHandler;

/**
 * Join frames together based on the context argument
 * 
 * @author mfeber - Initial API and implementation
 */
public class WindowJoinCmd extends E4WindowCmd {

	@Inject private Shell shell;	
	
	public static enum Join {
		ONE, ALL;
	}
	
	@Execute
	public Object execute(@Active MPart apart, @Active IEditorPart editor, @Named(E4CmdHandler.CMD_CTX_KEY)Join jtype,
			@Active EmacsPlusCmdHandler handler) {
		preJoin(editor);
		switch (jtype) {
		case ONE:
			joinOne(apart);
			break;
		case ALL:
			joinAll(apart);
			break;
		}
		postJoin(editor);
		if (handler.isUniversalPresent()) {
			// convenience hack
			// change setting without changing preference store
			setSplitSelf(!isSplitSelf());
		}
		reactivate(apart);
		forceFocus();
		return null;
	}

	/**
	 * Use a generic command to remove duplicates
	 * @param editor
	 */
	protected void preJoin(IEditorPart editor) {
		closeOthers(editor);
	}
	
	protected void postJoin(IEditorPart editor) {
		// for sub-classes
	}

	void closeOthers(IEditorPart editor) {
		if (isSplitSelf()) {
			try {
				EmacsPlusUtils.executeCommand(IEmacsPlusCommandDefinitionIds.CLOSE_OTHER_INSTANCES, null, editor);
			} catch (Exception e) {} 
		}
	}
	
	/**
	 * Merge the stack containing the selected part into its neighbor
	 * 
	 * @param apart
	 */
	void joinOne(MPart apart) {
		PartAndStack ps = getParentStack(apart);
		MElementContainer<MUIElement> pstack = ps.getStack();
		MPart part = ps.getPart();		
		if (pstack == null || join2Stacks(pstack, getAdjacentElement(pstack, part, true), part) == null) {
			// Invalid state 
			Beeper.beep();
		}
	}

	/**
	 * Merge all stacks into one
	 * 
	 * @param apart - the selected MPart
	 */
	void joinAll(MPart apart) {
		List<MElementContainer<MUIElement>> stacks = getOrderedStacks(apart);
		if (stacks.size() > 1) {

			PartAndStack ps = getParentStack(apart);
			MElementContainer<MUIElement> pstack = ps.getStack();
			MPart part = ps.getPart();		

			// check for unexpected result - who knows what Eclipse might do
			if (pstack != null) {
				MElementContainer<MUIElement> dropStack = stacks.get(0);
				for (int i = 1; i < stacks.size(); i++) {
					MElementContainer<MUIElement> stack = stacks.get(i); 
					if (stack == pstack) {
						continue;
					}
					join2Stacks(stacks.get(i), dropStack, null);
				}
				// lastly, join in the selected stack
				if (pstack != dropStack) {
					join2Stacks(pstack, dropStack, part);
				}
			} else {
				Beeper.beep();
			}
		}
	}
	
	/**
	 * Given 2 partStacks, move children of pstack into dropStack
	 * @param pstack - source stack
	 * @param dropStack - destination stack
	 * @param apart - the initiating part
	 * @return the enhanced dropStack
	 */
	protected MElementContainer<MUIElement> join2Stacks(MElementContainer<MUIElement> pstack, MElementContainer<MUIElement> dropStack, MPart apart) {
		if (dropStack != null && ((MPartSashContainerElement)dropStack) instanceof MPartStack) {
			List<MUIElement> eles = pstack.getChildren();
			boolean hasPart = apart != null;
			int offset = 1;
			List<MUIElement> drops = dropStack.getChildren();
			while (eles.size() > (hasPart ? 1 : 0)) {
				MUIElement ele = eles.get(eles.size() - offset);
				if (hasPart && ele == apart) {
					offset++;
					continue;
				}
				eles.remove(ele);
				if (hasPart) {
						drops.add(0,ele);
					} else {
						drops.add(ele);
					}
			}
			if (hasPart) {
				// Move the selected element to the leftmost position
				eles.remove(apart);
				drops.add(0,apart);
				dropStack.setSelectedElement(apart);
			}
			checkSizeData(pstack,dropStack);
		} 
		return dropStack;
	}

	/**
	 * Check if containerData size needs updating.
	 * This should be handled by the Eclipse framework, but apparently not...
	 * @param pstack - source stack
	 * @param dropStack - destination stack
	 */
	protected void checkSizeData(MElementContainer<MUIElement> pstack, MElementContainer<MUIElement> dropStack) {
		if (pstack.getParent().getContainerData() == null) {
			int s1 = getIntData(pstack);;
			if (dropStack.getParent().getContainerData() == null) {
				// stacks are vertically side by side, add their sizes together 
				dropStack.setContainerData(String.valueOf(s1 + getIntData(dropStack)));
			} else {
				// source is vertical & dest is in a horizontal containing PartSash
				dropStack.getParent().setContainerData(String.valueOf(s1 + getIntData(dropStack.getParent())));
			}
		}
	}

	/**
	 * Semi-workaround for egregious eclipse bug that denies focus on join
	 * Unfortunately, the cursor is invisible.  If just reactivate is called, then
	 * the cursor is visible, but the keyboard has lost focus.
	 * I believe this only affects OSX (TBD)
	 * 
	 * @param editor
	 * @param apart
	 */
	protected void forceFocus() {
		if (isMac()) 
			shell.forceFocus();
	}

}
