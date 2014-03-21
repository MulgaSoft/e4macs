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
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.search.ui.ISearchQuery;
import org.eclipse.search.ui.NewSearchUI;
import org.eclipse.search.ui.text.FileTextSearchScope;
import org.eclipse.search.ui.text.TextSearchQueryProvider;
import org.eclipse.search.ui.text.TextSearchQueryProvider.TextSearchInput;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.ITextEditor;

import com.mulgasoft.emacsplus.EmacsPlusActivator;
import com.mulgasoft.emacsplus.IEmacsPlusCommandDefinitionIds;
import com.mulgasoft.emacsplus.MarkUtils;
import com.mulgasoft.emacsplus.execute.ISearchResult;
import com.mulgasoft.emacsplus.minibuffer.SearchExecuteMinibuffer;

/**
 * Implements: tags-search
 * 
 * Emacs+ version of tags-search: search for string in either project, current working set, or workspace
 * The initial search string will be the first full word forward closest to point, if it is on the same line
 * Otherwise, the empty string.  The input string can be edited in the minibuffer
 * 
 * Support typing `M-c' within a tags-search to toggle the case sensitivity of that search.  
 * The effect does not extend beyond the current search to the next one, but it does override 
 * the effect of adding or removing an upper-case letter in the current search.
 *  
 * @author Mark Feber - initial API and implementation
 */
public abstract class TagsSearchHandler extends MinibufferExecHandler implements INonEditingCommand {

	protected static String TAGS_P_SEARCH = EmacsPlusActivator.getResourceString("Tags_Project") + KOLON; //$NON-NLS-1$
	protected static String TAGS_WS_SEARCH = EmacsPlusActivator.getResourceString("Tags_Set") + KOLON;    //$NON-NLS-1$
	protected static String TAGS_W_SEARCH = EmacsPlusActivator.getResourceString("Tags_Space") + KOLON;   //$NON-NLS-1$
	protected static String TAGS_F_SEARCH = EmacsPlusActivator.getResourceString("Tags_File") + KOLON;    //$NON-NLS-1$

	/**
	 * @see com.mulgasoft.emacsplus.commands.EmacsPlusCmdHandler#transform(ITextEditor, IDocument, ITextSelection, ExecutionEvent)
	 */
	@Override
	protected int transform(final ITextEditor editor, IDocument document, ITextSelection currentSelection, final ExecutionEvent event) 
	throws BadLocationException {
		final ITextSelection initText = initText(editor, document, currentSelection);
		final SearchExecuteMinibuffer mini = new SearchExecuteMinibuffer(this); 
		// remember original cursor location in the tags ring
		MarkUtils.setTagMark(editor,getCursorOffset(editor,currentSelection));
		
		PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() {
			public void run() {
				boolean ok = mini.beginSession(editor, PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage(),event);
				if (ok){
					mini.initMinibufferSelection(initText);
				}
			}
		});
		return NO_OFFSET;
	}

	/**
	 * @see com.mulgasoft.emacsplus.commands.MinibufferExecHandler#doExecuteResult(org.eclipse.ui.texteditor.ITextEditor, java.lang.Object)
	 */
	public boolean doExecuteResult(ITextEditor editor, Object minibufferResult) {
		TextSearchQueryProvider provider= TextSearchQueryProvider.getPreferred();
		ISearchResult result = (ISearchResult)minibufferResult; 
		try {
			ISearchQuery query= provider.createQuery(getTextInput(result.getSearchStr(),result.isCaseSensitive(),getInputObject(editor)));
			if (query != null) {
				// and execute the search
				NewSearchUI.runQueryInBackground(query);
			}	
		} catch (OperationCanceledException ex) {
			// action canceled
		} catch (CoreException e) {}
		return true;
	}
	
	/**
	 * @see com.mulgasoft.emacsplus.commands.EmacsPlusCmdHandler#getDispatchId(java.lang.String, int)
	 */
	protected String getDispatchId(String id, int arg) {
		String result = null;
		String did = id;
		if (arg < 4) {
			did = IEmacsPlusCommandDefinitionIds.TAGS_PROJECT;
		} else if (arg < 16) {
			did = IEmacsPlusCommandDefinitionIds.TAGS_WORKINGSET;
		} else {
			did = IEmacsPlusCommandDefinitionIds.TAGS_WORKSPACE;
		}
		if (!did.equals(id)) {
			result = did;
		}
		return result;
	}

	/**
	 * @see com.mulgasoft.emacsplus.commands.EmacsPlusCmdHandler#isLooping()
	 */
	@Override
	protected boolean isLooping() {
		return false;
	}

	/**
	 * The initial search string will be the closest full word forward, if it is on the same line
	 * 
	 * @param document
	 * @param selection
	 * @return the initial search string
	 * 
	 * @throws BadLocationException
	 */
	protected ITextSelection initText(ITextEditor editor, IDocument document, ITextSelection selection) throws BadLocationException {
		ITextSelection result = null;

		if (selection != null) {
			if (selection.getLength() == 0) {
				int line = document.getLineOfOffset(selection.getOffset());
				// get the proximate word
				try {
					ITextSelection tmp = new SexpForwardHandler().getTransSexp(document, selection.getOffset(), true);
					// make sure we're still on the same line
					if (tmp != null && line == document.getLineOfOffset(tmp.getOffset())) {
						selection = tmp;
					}
					// ignore 
				} catch (BadLocationException e) {}
			}
			if (selection != null && selection.getLength() > 0 && selection.getText() != null) {
				result = selection;
			}
		}
		return result;
	}
	
	/**
	 * Search workspace by default
	 * 
	 * @param editor
	 * @return a workspace scope object
	 */
	protected FileTextSearchScope getInputObject(ITextEditor editor) {
		return FileTextSearchScope.newWorkspaceScope(new String[0], false);
	}	

	/**
	 * Set up the search object
	 * 
	 * @param searchStr
	 * @param scope
	 * @return
	 */
	private TextSearchInput getTextInput(final String searchStr, final boolean caseSensitive, final FileTextSearchScope scope) {
		return new TextSearchInput() {

			public String getSearchText() {
				return searchStr;
			}

			public boolean isCaseSensitiveSearch() {
				return caseSensitive;
			}

			public boolean isRegExSearch() {
				return true;
			}

			public FileTextSearchScope getScope() {
				return scope;
			}
		};
	}
		protected IProject getCurrentProject(ITextEditor editor) {
		if (editor != null) {
			IEditorInput input = editor.getEditorInput();
			if (input instanceof IFileEditorInput) {
				return ((IFileEditorInput) input).getFile().getProject();
			}
		}
		return null;
	}
}
