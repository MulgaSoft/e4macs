/**
 * Copyright (c) 2009, 2010 Mark Feber, MulgaSoft
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package com.mulgasoft.emacsplus.commands;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.search.ui.text.FileTextSearchScope;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.texteditor.ITextEditor;

/**
 * occur: Show all lines in the current buffer containing a match for REGEXP.
 *
 * @author Mark Feber - initial API and implementation
 */
public class OccurHandler extends TagsSearchHandler {

	public String getMinibufferPrefix() {
		return TAGS_F_SEARCH;
	}	

	@Override
	protected FileTextSearchScope getInputObject(ITextEditor editor) {
		// TODO Auto-generated method stub
		IResource file = getCurrentResource(editor);
		if (file != null) {
			return FileTextSearchScope.newSearchScope(new IResource[]{file}, new String[]{((IFile)file).getName()}, false);
		} else {
			return super.getInputObject(editor);
		}
	}

	/**
	 * Limit the query to the current editor's file
	 * 
	 * @param editor
	 * @return the IFile resource or null
	 */
	private IResource getCurrentResource(ITextEditor editor) {
		IResource result = null;
		if (editor != null) {
			IEditorInput input = editor.getEditorInput();
			if (input instanceof IFileEditorInput) {
				IFile file = ((IFileEditorInput) input).getFile();
				if (file instanceof IResource) {
					result = (IResource)file;
				}
			}
		}
		return result;
	}
}
