/**
 * Copyright (c) 2009, 2014 Mark Feber, MulgaSoft
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package com.mulgasoft.emacsplus.e4.commands;

import static com.mulgasoft.emacsplus.commands.CloseOtherInstancesHandler.closeAllDuplicates;
import static com.mulgasoft.emacsplus.commands.CloseOtherInstancesHandler.closeDuplicates;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import org.eclipse.e4.core.contexts.Active;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.ui.model.application.ui.MElementContainer;
import org.eclipse.e4.ui.model.application.ui.MUIElement;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.model.application.ui.basic.MPartSashContainerElement;
import org.eclipse.e4.ui.model.application.ui.basic.MPartStack;
import org.eclipse.e4.ui.workbench.modeling.EPartService;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;

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
	
	private static final String GET_REF = "getReference";  	//$NON-NLS-1$
	
	@Execute
	public Object execute(@Active MPart apart, @Active IEditorPart editor, @Named(E4CmdHandler.CMD_CTX_KEY)Join jtype,
			@Active EmacsPlusCmdHandler handler) {
		MPart active = apart;
		boolean joined = preJoin(editor);
		switch (jtype) {
		case ONE:
			active = joinOne(apart);
			if (joined) {
				// don't switch buffers on a split self merge
				active = apart;
			}
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
		final MPart ed = active;
		Display.getDefault().asyncExec(() -> {reactivate(ed);});
		return null;
	}

	/**
	 * 
	 * @param editor
	 */
	protected boolean preJoin(IEditorPart editor) {
		return isSplitSelf();
	}
	
	protected void postJoin(IEditorPart editor) {
		// for sub-classes
		editor.setFocus();
	}

	/**
	 * Merge the stack containing the selected part into its neighbor and return the 
	 * element to activate.  This will be the originating buffer on split-self or the
	 * active element in the other stack if not.
	 * 
	 * @param apart
	 * @return the element to select
	 */
	MPart joinOne(MPart apart) {
		// We're going to end up with 1, so avoid the overhead of futzing with the stacks
		if (isSplitSelf() && getOrderedStacks(apart).size() < 3) {
			this.joinAll(apart);
			return apart;
		} else {
			PartAndStack ps = getParentStack(apart);
			MElementContainer<MUIElement> pstack = ps.getStack();
			MPart part = ps.getPart();		
			MElementContainer<MUIElement> adjacent = getAdjacentElement(pstack, part, true);
			MUIElement otherPart = getSelected(adjacent);
			// deduplicate editors across these two stacks
			closeOthers(pstack, adjacent);
			if (pstack == null || join2Stacks(pstack, adjacent, part) == null) {
				// Invalid state 
				Beeper.beep();
			}
			return (otherPart instanceof MPart) ? (MPart)otherPart : apart;
		}
	}	

	/**
	 * Merge all stacks into one
	 * 
	 * @param apart - the selected MPart
	 */
	void joinAll(MPart apart) {
		try {
			// deduplicate editors across all stacks
			closeAllDuplicates(EmacsPlusUtils.getWorkbenchPage());
		} catch (PartInitException e) {
			// Ignore
		}
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
	 * Close any duplicate editors in the supplied part stacks
	 * 
	 * @param stack1
	 * @param stack2
	 * @return true if any duplicates were closed
	 */
	boolean closeOthers(MElementContainer<MUIElement> stack1, 	MElementContainer<MUIElement> stack2) {
		boolean result = false;
		Collection<IEditorReference> editors = getStackEditors(stack1);
		editors.addAll(getStackEditors(stack2));
		try {
			result = closeDuplicates(EmacsPlusUtils.getWorkbenchPage(), editors.toArray(new IEditorReference[editors.size()]));
		} catch (PartInitException e) {
			// Ignore
		}
		return result;
	}
	
	/**
	 * Close any duplicate editors that match this one
	 * 
	 * @param editor
	 * @return true if any were closed
	 */
	boolean closeOthers(IEditorPart editor) {
		IWorkbenchPage page = EmacsPlusUtils.getWorkbenchPage();
		int pre = EmacsPlusUtils.getSortedEditors(page).length;		
		if (isSplitSelf()) {
			try {
				EmacsPlusUtils.executeCommand(IEmacsPlusCommandDefinitionIds.CLOSE_OTHER_INSTANCES, null, editor);
			} catch (Exception e) {} 
		}
		return (pre != EmacsPlusUtils.getSortedEditors(page).length);
	}
	
	/**
	 * Get all the IEditorReferences in a given part stack
	 * 
	 * @param stack
	 * @return the collection of IEditorReferences
	 */
	private Collection<IEditorReference> getStackEditors(MElementContainer<MUIElement> stack) {
		Collection<IEditorReference> editors = new ArrayList<IEditorReference>(); 
		for (MUIElement child : stack.getChildren()) {
			if (child instanceof MPart) {
				// TODO: There must be a better way of getting the editor out of e4, but I can't find it
				Object cEditor = ((MPart) child).getObject();
				if (cEditor != null) {
					try {
						// avoid discouraged access of org.eclipse.ui.internal.e4.compatibility.CompatibilityEditor
						// which is expected cEditor's type in e4
						Method method = cEditor.getClass().getMethod(GET_REF);
						if (method != null) {
							IEditorReference ie = (IEditorReference)method.invoke(cEditor);					
							editors.add(ie);

						}
					} catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException
							| InvocationTargetException | ClassCastException e) {

					}
				}
			}
		}
		return editors;
	}

	void closePart(MPart part) {
		// based on org.eclipse.e4.ui.workbench.renderers.swt.StackRenderer.closePart()
		IEclipseContext context = part.getContext();
		if (context != null && part.isCloseable()) {
			EPartService partService = context.get(EPartService.class);
			partService.hidePart(part,true);
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
				// source is vertical & destination is in a horizontal containing PartSash
				dropStack.getParent().setContainerData(String.valueOf(s1 + getIntData(dropStack.getParent())));
			}
		}
	}

	/**
	 * Semi-workaround for egregious eclipse bug that denies focus on join which, unfortunately,
	 * leaves the cursor invisible.
	 * If just reactivate is called, then the cursor is visible, but the keyboard has lost focus.
	 * A defect has been submitted which covers this (and other cases): 
	 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=441010
	 */
	protected boolean forceFocus() {
		return shell.forceFocus();
	}

}
