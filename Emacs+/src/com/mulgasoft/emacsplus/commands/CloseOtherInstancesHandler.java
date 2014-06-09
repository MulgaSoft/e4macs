/**
 * Copyright (c) 2009, 2014 Mark Feber, MulgaSoft
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 */
package com.mulgasoft.emacsplus.commands;

import java.util.ArrayList;

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
			ArrayList<IEditorReference> otherInstances = getOtherInstances(active, page);
			// Close the others, and guarantee proper (emacs+) activation
			if (!otherInstances.isEmpty()) {
				IEditorReference[] otherEditors = otherInstances.toArray(new IEditorReference[0]);;
				// don't prompt to save contents, as the final instance is still open
				page.closeEditors(otherEditors, false);
				// We have to manually call our activation code, as closing the identity editors
				// results in our document listeners being removed, and since the part is already 
				// active, a simple call to page.activate is a no-op.  
				EmacsPlusActivation.getInstance().partActivated(active);
			}
		}
	}

	private ArrayList<IEditorReference> getOtherInstances(IEditorReference active,IWorkbenchPage page) throws PartInitException {
		ArrayList<IEditorReference> result = new ArrayList<IEditorReference>();
		if (page != null) {
			IEditorReference[] refArray = page.getEditorReferences();
			if (refArray != null && refArray.length > 1) {
				// Dereference if a multi-part editor, returns identity if not
				IEditorReference dactive = (IEditorReference) page.getReference(getActiveEditor(page));
				IEditorInput ai = active.getEditorInput();
				if (ai != null) {
					if (dactive != null && active != dactive) {
						IEditorInput dai = dactive.getEditorInput();
						if (dai != null) {
							for (int i = 0; i < refArray.length; i++) {
								try {
									if (editorsMatch(active, ai, refArray[i]) || (editorsMatch(dactive, dai, refArray[i]))) {
										result.add(refArray[i]);
									}
								} catch (Exception e) {
									// Trap (most likely PartInitException) Exception so that we can get the instances that don't fail
								}
							}
						}
					} else {
						for (int i = 0; i < refArray.length; i++) {
							try {
								if (editorsMatch(active, ai, refArray[i])) {
									result.add(refArray[i]);
								}
							} catch (Exception e) {
								// Trap (most likely PartInitException) Exception so that we can get the instances that don't fail
							}
						}
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
	private boolean editorsMatch(IEditorReference a, IEditorInput ai, IEditorReference b) throws PartInitException {
		return (a != b  && ai.equals(b.getEditorInput()));
	}
	
}
