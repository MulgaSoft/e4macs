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

import org.eclipse.core.resources.IProject;
import org.eclipse.search.ui.text.FileTextSearchScope;
import org.eclipse.ui.texteditor.ITextEditor;

/**
 * tags-search: Project
 * Search the current Project
 * 
 * @author Mark Feber - initial API and implementation
 *
 */
public class TagsProjectHandler extends TagsSearchHandler {

	public String getMinibufferPrefix() {
		return TAGS_P_SEARCH;
	}	
	
	/**
	 * Set up project search scope
	 * 
	 * @see com.mulgasoft.emacsplus.commands.TagsSearchHandler#getInputObject(org.eclipse.ui.texteditor.ITextEditor)
	 */
	protected FileTextSearchScope getInputObject(ITextEditor editor) {
		IProject project= getCurrentProject(editor);
		if (project != null) {
			return FileTextSearchScope.newSearchScope(new IProject[] { project }, new String[0], false);			
		} else {
			return super.getInputObject(editor);
		}
	}	
}
