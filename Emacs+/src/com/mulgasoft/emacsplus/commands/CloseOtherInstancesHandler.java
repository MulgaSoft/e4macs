/**
 * Copyright (c) 2009, 2020 Mark Feber, MulgaSoft
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 */
package com.mulgasoft.emacsplus.commands;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.handlers.HandlerUtil;

import com.mulgasoft.emacsplus.EmacsPlusActivation;

/**
 * Remove all the split windows
 * 
 * @author Mark Feber - initial API and implementation
 */
public class CloseOtherInstancesHandler extends EmacsPlusNoEditHandler {

	/**
	 * Execute directly
	 * 
	 * @see com.mulgasoft.emacsplus.commands.EmacsPlusCmdHandler#execute(org.eclipse.core.commands.ExecutionEvent)
	 */
	@Override
	public Object execute(ExecutionEvent event) {
		try {
			IWorkbenchWindow window = HandlerUtil.getActiveWorkbenchWindowChecked(event);
			IWorkbenchPage page = window.getActivePage();
			closeOtherInstances(page);
		} catch (ExecutionException e) {
		} catch (PartInitException e) {
		} catch (Exception e) {
		}
		return null;
	}
	
    /**
	 * Close all other editors on the current editor's input
	 * 
	 * @param page
	 * @throws PartInitException
	 */
	protected void closeOtherInstances(IWorkbenchPage page) throws PartInitException {
		if (page != null) {
			IEditorReference active = (IEditorReference) page.getReference(page.getActiveEditor());
			// Close the others, and guarantee proper (emacs+) activation
			closeInstances(page, getOtherInstances(active, page, page.getEditorReferences(), 0), active);
		}
	}
	
    /**
	 * Close all duplicates when joining after one or more split commands 
	 * 
	 * @param page
	 * @throws PartInitException
	 */
	public static boolean closeAllDuplicates(IWorkbenchPage page) throws PartInitException {
		boolean closed = false;		
		if (page != null) {
			IEditorReference[] refs = page.getEditorReferences();
			closed = closeDuplicates(page, refs);
		} 
		return closed;
	}
	
	public static boolean closeDuplicates(IWorkbenchPage page, IEditorReference[] refs) throws PartInitException {
		boolean closed = false;
		if (refs != null && refs.length > 1) {
			// Dereference if a multi-part editor, returns identity if not
			IEditorReference active = (IEditorReference) page.getReference(page.getActiveEditor());
			HashSet<IEditorReference> closers = new HashSet<IEditorReference>();
			// first get any that match the active editor
			closers.addAll(getOtherInstances(active, page, refs, 0));
			// then see if any other editors have duplicates within the array of references
			for (int i = 0; i < refs.length; i++) {
				IEditorReference current = refs[i];
				if (current != active && !closers.contains(current)) {
					closers.addAll(getOtherInstances(current, page, refs, i));
				}
			}
			closed = closeInstances(page, closers, active);
		}
		return closed;
	}

	private static boolean closeInstances(IWorkbenchPage page, Collection<IEditorReference> closers, IEditorReference active) {
		boolean result = false;
		if (result = !closers.isEmpty()) {
			// don't prompt to save contents, as the final instance is still open
			page.closeEditors(closers.toArray(new IEditorReference[0]), false);
			// We have to manually call our activation code, as closing the identity editors
			// results in our document listeners being removed, and since the part is already 
			// active, a simple call to page.activate is a no-op.  
			EmacsPlusActivation.getInstance().partActivated(active);
		}
		return result;
	}
	
	private static ArrayList<IEditorReference> getOtherInstances(IEditorReference current, IWorkbenchPage page, IEditorReference[] refArray, int index) throws PartInitException {
		ArrayList<IEditorReference> result = new ArrayList<IEditorReference>();
		if (refArray != null && refArray.length > 1) {
			IEditorInput ci = current.getEditorInput();
			if (ci != null) {
				for (int i = index; i < refArray.length; i++) {
					try {
						if (editorsMatch(current, ci, refArray[i])) {
							result.add(refArray[i]);
						}
					} catch (Exception e) {
						// Trap Exception (most likely PartInitException) so that we can get the instances that don't fail
					}
				}
			}
		}
		return result;
	}
	
	/**
	 * Compare two editor instances
	 * @param a
	 * @param b
	 * @return true if different editor instances are looking at the same content 
	 * @throws PartInitException
	 */
	private static boolean editorsMatch(IEditorReference a, IEditorInput ai, IEditorReference b) throws PartInitException {
		return (a != b  && ai.equals(b.getEditorInput()));
	}
	
}
