/**
 * Copyright (c) 2009-2020 Mark Feber, MulgaSoft
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package com.mulgasoft.emacsplus;

import static org.eclipse.ui.texteditor.ITextEditorActionConstants.STATUS_CATEGORY_ELEMENT_STATE;

import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.texteditor.StatusLineContributionItem;

/**
 * Workaround for multiple status line contribution managers 
 *  
 * @author mfeber
 */
public abstract class StatusItemSupport {

	protected static String POSITION_ID = STATUS_CATEGORY_ELEMENT_STATE;
	
	protected abstract StatusLineContributionItem initStatusLineItem(); 
	
	/**
 	 * Each editor type adds their own org.eclipse.jface.action.SubContributionManager with a parallel
	 * set of items to the other editors.  We need to add ourselves to each set as we encounter them.  
	 * Eclipse uses the exact same initialization code to modify the EditAction menu, while duplicating 
	 * the StausLineManager setup (in text editors afaik).
	 *  
	 * @param editor - the current editor
	 * @param placeId - add the status line item before this
	 */
	protected synchronized void addStatusContribution(ITextEditor editor, String placeId) {
		IStatusLineManager slm = EmacsPlusUtils.getStatusLineManager(editor);
		IContributionItem item = initStatusLineItem();
		IContributionItem fItem = slm.find(item.getId());		
		if (fItem == null  || !hasInnerItem(slm, item)) {
			System.out.println("Adding " + item.getId() + " at " + placeId + " for " + editor.toString());
			if (slm.find(placeId) != null) {
				slm.insertBefore(placeId, item);
			} else {
				slm.add(item);
			}
		}
		item.setVisible(true);
	}	
	
	/**
	 * We have to paw through the new SubContributionManager's items to see if we've already been
	 * added.  Fortunately, the list only has a few items.
	 * 
	 * @param slm - the SubContributionManager
	 * @param item - our statusLine item 
	 * @return true - if we're already present
	 */
	private boolean hasInnerItem(IStatusLineManager slm, IContributionItem item) {
		boolean result = false;
		String id = item.getId();
		// get the most local items
		IContributionItem[] items = slm.getItems();
		for (IContributionItem i : items) {
			if (id.equals(i.getId())) {
				result = true;
				break;
			}
		}
		return result;
	}
}
