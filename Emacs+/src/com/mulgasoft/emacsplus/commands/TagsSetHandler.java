/**
 * Copyright (c) 2009, 2010 Mark Feber, MulgaSoft
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 */
package com.mulgasoft.emacsplus.commands;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.search.ui.text.FileTextSearchScope;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.IWorkingSetManager;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.ITextEditor;

/**
 * tags-search: Working Set
 * Search the current Working Set, or Workspace if no Working Set 
 * 
 * @author Mark Feber - initial API and implementation
 *
 */
public class TagsSetHandler extends TagsSearchHandler {

	String w_or_ws = TAGS_WS_SEARCH; 
	IWorkingSet[] ws = null;
	
	public String getMinibufferPrefix() {
		return w_or_ws;
	}

	// cache, so we don't have to search twice
	private IWorkingSet wset = null;
	// and clear on completion
	protected void postExecute() {
		wset = null;
		super.postExecute();
	}
	
	/**
	 * @see com.mulgasoft.emacsplus.commands.TagsSearchHandler#transform(org.eclipse.ui.texteditor.ITextEditor, org.eclipse.jface.text.IDocument, org.eclipse.jface.text.ITextSelection, org.eclipse.core.commands.ExecutionEvent)
	 */
	@Override
	protected int transform(ITextEditor editor, IDocument document, ITextSelection currentSelection,
			ExecutionEvent event) throws BadLocationException {
		wset = getWorkingSet(editor);
		if (wset != null) {
			w_or_ws = String.format(TAGS_WS_SEARCH,wset.getName()); 
		} else {
			w_or_ws = TAGS_W_SEARCH; 
		}
		return super.transform(editor, document, currentSelection, event);
	}

	/**
	 * Set up working set scope, if present, else workspace
	 * 
	 * @see com.mulgasoft.emacsplus.commands.TagsSearchHandler#getInputObject(org.eclipse.ui.texteditor.ITextEditor)
	 */
	protected FileTextSearchScope getInputObject(ITextEditor editor) {
		IWorkingSet set = (wset != null ? wset : getWorkingSet(editor));
		if (set != null) {
			return FileTextSearchScope.newSearchScope(new IWorkingSet[] { set }, new String[0], false);			
		} else {
			return super.getInputObject(editor);
		}
	}	
	
	/**
	 * Get the correct working set, if working sets are present in environment
	 * The Eclipse UI's default 'Other Projects' entry is not represented in the manager
	 * so, if a file from there is selected a Workspace search will result. 
	 * 
	 * @param editor
	 * @return current IWorkingSet or null
	 */
	private IWorkingSet getWorkingSet(ITextEditor editor) {
		IWorkingSet result = null;
		IWorkbenchPage page = getWorkbenchPage();
		if (page != null) {
			IWorkingSetManager manager = PlatformUI.getWorkbench().getWorkingSetManager();
			IWorkingSet[] workingSets = manager.getRecentWorkingSets();
			if (workingSets != null && workingSets.length > 0) {
				// return most recent working set
				result = workingSets[0];
			} else {
				// For some lame reason, recent working sets is often not set in the manager
				// so paw through them manually
				workingSets = manager.getAllWorkingSets();
				if (workingSets != null && workingSets.length > 0) {
					IProject proj = this.getCurrentProject(editor);
					for(IWorkingSet set : workingSets) {
						for (IAdaptable ele : set.getElements()) {
							Object adapt = ele.getAdapter(IProject.class);
							if (adapt != null && adapt.equals(proj)){
								return set;
							}
						}
					}
				}
			}
		}
		return result;
	}
}
