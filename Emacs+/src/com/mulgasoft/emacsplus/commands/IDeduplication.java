/**
 * Copyright (c) 2009-2020 Mark Feber, MulgaSoft
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package com.mulgasoft.emacsplus.commands;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;

import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;

import com.mulgasoft.emacsplus.EmacsPlusActivation;

/**
 * Common deduplication code
 * 
 * @author mfeber
 *
 */
public interface IDeduplication {

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
	
	/**
	 * Close all duplicates present in the array of IEditorReferences
	 * 
	 * @param page
	 * @param refs
	 * @return true if any editors were closed
	 * @throws PartInitException
	 */
	public static boolean closeDuplicates(IWorkbenchPage page, IEditorReference[] refs) throws PartInitException {
		boolean closed = false;
		if (refs != null && refs.length > 1) {
			// Dereference if a multi-part editor, returns identity if not
			IEditorReference active = (IEditorReference) page.getReference(page.getActiveEditor());
			HashSet<IEditorReference> closers = new HashSet<IEditorReference>();
			// first get any that match the active editor
			closers.addAll(getOtherInstances(active, refs, 0));
			// then see if any other editors have duplicates within the array of references
			for (int i = 0; i < refs.length; i++) {
				IEditorReference current = refs[i];
				if (current != active && !closers.contains(current)) {
					closers.addAll(getOtherInstances(current, refs, i));
				}
			}
			closed = closeInstances(page, closers, active);
		}
		return closed;
	}

	/**
	 * Close all the editors in the collection of IEditorReferences
	 * 
	 * @param page
	 * @param closers editors to close
	 * @param active the editor to remain active after the operation
	 * @return true if any editors were closed
	 */
	public static boolean closeInstances(IWorkbenchPage page, Collection<IEditorReference> closers, IEditorReference active) {
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
	
	/**
	 * Look for and return any matches in the IEditorReference array with current
	 * 
	 * @param current the editor to match
	 * @param refArray the array of IEditorReferences
	 * @param index the start index within the array of IEditorReferences
	 * @return a collection of all editors that match current (exclusive) 
	 * @throws PartInitException
	 */
	public static ArrayList<IEditorReference> getOtherInstances (IEditorReference current, IEditorReference[] refArray, int index) throws PartInitException {
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
	public static boolean editorsMatch(IEditorReference a, IEditorInput ai, IEditorReference b) throws PartInitException {
		return (a != b  && ai.equals(b.getEditorInput()));
	}
	

}