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
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.texteditor.ITextEditor;

import com.mulgasoft.emacsplus.EmacsPlusActivator;
import com.mulgasoft.emacsplus.EmacsPlusUtils;
import com.mulgasoft.emacsplus.IEmacsPlusCommandDefinitionIds;
import com.mulgasoft.emacsplus.KillRing;
import com.mulgasoft.emacsplus.preferences.EmacsPlusPreferenceConstants;

/**
 * Implements: yank-pop
 * 
 * Valid only if called immediately after a `yank' or a `yank-pop'
 * yank-pop deletes previous popped text and replaces it with the 'next' stretch of killed text.
 * 
 * With no argument, then previous kill is inserted
 * With ARG, insert the ARGth 'previous' kill (ARG == 1 is the same as no argument)
 * With negative ARG, insert the ARGth 'next' kill
 * 
 * @author Mark Feber - initial API and implementation
 */
public class YankPopHandler extends BaseYankHandler {
	
	// if true, then auto browse kill ring on invocation with no previous yank
	private static boolean autoBrowse = (Boolean)getPreference(EmacsPlusPreferenceConstants.P_AUTO_BROWSE_KR);
	private static final String YP_DISABLED = "YankPopHandler_0"; 	//$NON-NLS-1$ 
	
	/**
	 * Replace the yanked text with the previous yank and return the new offset 
	 * 
	 * @see com.mulgasoft.emacsplus.commands.EmacsPlusCmdHandler#transform(ITextEditor, IDocument, ITextSelection, ExecutionEvent)
	 */
	@Override
	protected int transform(ITextEditor editor, IDocument document, ITextSelection currentSelection, ExecutionEvent event) throws BadLocationException {
		
		if (KillRing.getInstance().isYanked()) {
			return (currentSelection.getOffset() + yankIt(document,currentSelection));
		} else if (autoBrowse) { 
			// Browse kill ring if true
			try {
				EmacsPlusUtils.executeCommand(IEmacsPlusCommandDefinitionIds.BROWSE_KILL_RING,null,editor);
			} catch (Exception e) {
				EmacsPlusUtils.showMessage(editor, EmacsPlusActivator.getString(YP_DISABLED),false);
			}
		} else {
			EmacsPlusUtils.showMessage(editor, EmacsPlusActivator.getString(YP_DISABLED),false);
		}
		return currentSelection.getOffset();
	}
	
	protected int yankIt(IDocument document, ITextSelection selection) throws BadLocationException {
		int len = 0;
		KillRing kb = KillRing.getInstance();
		String prevText = convertDelimiters(kb.lastYank());
		String yankText = convertDelimiters(kb.yankPop());
		if (yankText != null) {
			int origin = selection.getOffset() - prevText.length();
			// in case selection has been mouse moved
			// Since our mouse listener would have to be on the StyledText widget
			if (prevText == null ||  !prevText.equals(document.get(origin, prevText.length()))) {
				kb.setYanked(false);
				return 0;
			}
			updateText(document, origin, prevText.length(), yankText);
			kb.setYanked(true);
			return yankText.length() - prevText.length();
		}
		return len;
	}
	
	/**
	 *  Simulate yank pop
	 *  
	 * @see com.mulgasoft.emacsplus.commands.BaseYankHandler#paste(org.eclipse.core.commands.ExecutionEvent, org.eclipse.swt.custom.StyledText)
	 */
	protected void paste(ExecutionEvent event, StyledText widget, boolean isProcess) {
		KillRing kb = KillRing.getInstance();
		if (kb.isYanked()) {
			String cacheText = KillRing.getInstance().getClipboardText();
			String prevText = convertDelimiters(kb.lastYank(),isProcess);
			String yankText = convertDelimiters(kb.yankPop(),isProcess);
			try {
				int offset = widget.getCaretOffset();
				if (prevText == null || !prevText.equals(widget.getText(offset-prevText.length(), offset-1))) {
					kb.setYanked(false);
					EmacsPlusUtils.showMessage(HandlerUtil.getActivePart(event), EmacsPlusActivator.getString(YP_DISABLED),false);
					return;
				}
				widget.setRedraw(false);
				widget.setSelection(offset - prevText.length(), offset);
				kb.setClipboardText(yankText);
				super.paste(event, widget);
			} finally {
				if (cacheText != null) {
					kb.setClipboardText(cacheText);
				}
				widget.setRedraw(true);
			}
		} else {
			EmacsPlusUtils.showMessage(HandlerUtil.getActivePart(event), EmacsPlusActivator.getString(YP_DISABLED),false);
		}
	}
	
	/**
	 * Enable or disable auto browse of kill ring
	 * 
	 * @param auto
	 */
	public static void setAutoBrowse(boolean auto) {
		YankPopHandler.autoBrowse = auto;
	}

	private static boolean getPreference(String key) {
		boolean result = false;
		IPreferenceStore store = EmacsPlusActivator.getDefault().getPreferenceStore();
		if (store != null) {
			result = store.getBoolean(key);
		}
		return result;
	}
}
