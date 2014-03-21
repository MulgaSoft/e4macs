/**
 * Copyright (c) 2009, 2010 Mark Feber, MulgaSoft
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package com.mulgasoft.emacsplus.commands;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.FindReplaceDocumentAdapter;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.texteditor.ITextEditor;

import com.mulgasoft.emacsplus.EmacsPlusActivator;
import com.mulgasoft.emacsplus.IEmacsPlusCommandDefinitionIds;
import com.mulgasoft.emacsplus.KillRing;
import com.mulgasoft.emacsplus.MarkUtils;
import com.mulgasoft.emacsplus.minibuffer.ZapMinibuffer;

/**
 * Implement: zap-to-char
 * 
 * The command `M-z' (`zap-to-char') combines killing with searching:
 * it reads a character and kills from point up to (and including) the
 * next occurrence of that character in the buffer.  A numeric argument
 * acts as a repeat count; a negative argument means to search backward
 * and kill text before point.
 * 
 * @author Mark Feber - initial API and implementation
 */
public class ZapToCharHandler extends MinibufferExecHandler {

	private static final String ZAP_PREFIX = EmacsPlusActivator.getResourceString("Zap_ToChar"); //$NON-NLS-1$ // Zap to char: 
	private static final String ZAP_FAIL= EmacsPlusActivator.getResourceString("Zap_Fail"); //$NON-NLS-1$ // Zap to char: 
	private static final String ZAP_FAIL_COUNT= EmacsPlusActivator.getResourceString("Zap_FailCount"); //$NON-NLS-1$ // Zap to char: 

	/**
	 * @see com.mulgasoft.emacsplus.commands.MinibufferHandler#getMinibufferPrefix()
	 */
	public String getMinibufferPrefix() {
		return ZAP_PREFIX;
	}
	
	/**
	 * Instead of looping entire command, just jump the appropriate number of searches
	 * 
	 * @see com.mulgasoft.emacsplus.commands.EmacsPlusCmdHandler#isLooping()
	 */
	@Override
	protected boolean isLooping() {
		return false;
	}
	
	/**
	 * @see com.mulgasoft.emacsplus.commands.EmacsPlusCmdHandler#transform(org.eclipse.ui.texteditor.ITextEditor, org.eclipse.jface.text.IDocument, org.eclipse.jface.text.ITextSelection, org.eclipse.core.commands.ExecutionEvent)
	 */
	@Override
	protected int transform(ITextEditor editor, IDocument document,
			ITextSelection currentSelection, ExecutionEvent event)
			throws BadLocationException {
		return bufferTransform(new ZapMinibuffer(this), editor, event);
	}

	/**
	 * @see com.mulgasoft.emacsplus.commands.MinibufferExecHandler#doExecuteResult(org.eclipse.ui.texteditor.ITextEditor, java.lang.Object)
	 */
	@Override
	protected boolean doExecuteResult(ITextEditor editor,
			Object minibufferResult) {
		
		String searchStr = (String)minibufferResult;
		int count = getUniversalCount();
		if (searchStr.length() > 0) {
			int modelOffset = MarkUtils.getCursorOffset(editor);
			boolean forward = (count < 0 ? false : true);
			int searchOffset = modelOffset + (forward ? 0 : -1); // separate search from result offset because of a bug in FRDA. 
			int resultOffset = modelOffset; 					 // Also forces an extra bounds check here. See comment below
			if (!(searchOffset <0)) {
				// use widget to avoid unpleasant scrolling side effects of IRewriteTarget				
				Control widget = MarkUtils.getTextWidget(editor);
				try {
					widget.setRedraw(false);
					// flag for kill ring
					KillRing.getInstance().setKill(IEmacsPlusCommandDefinitionIds.ZAP_TO_CHAR,forward);
					IDocument doc = getThisDocument(editor);
					FindReplaceDocumentAdapter frda = new FindReplaceDocumentAdapter(doc);
					int counter = Math.abs(count);
					for (int i = 0; i < counter; i++) {
						IRegion reg = frda.find(searchOffset, searchStr, forward, true, false, false);
						if (reg != null) {
							resultOffset = reg.getOffset() + (forward ? reg.getLength() : 0);
							// There's a ?bug? in org.eclipse.jface.text.FindReplaceDocumentAdapter that causes improper overlap on reverse search: 
							// while (found && fFindReplaceMatcher.start() + fFindReplaceMatcher.group().length() <= fFindReplaceMatchOffset + 1) 
							searchOffset = (forward ? resultOffset : resultOffset -1);
						} else {
							// back to original cursor offset on failure
							selectAndReveal(editor, modelOffset, modelOffset);
							fail(editor, searchStr, ++i, counter);
							return true;
						}
					}
					int length = Math.abs(resultOffset - modelOffset);
					doc.replace((forward ? modelOffset : resultOffset), length, EMPTY_STR);
				} catch (BadLocationException e) {
					e.printStackTrace();
				} finally {
					KillRing.getInstance().setKill(null,false);
					widget.setRedraw(true);
				}
			}
		} else {
			fail(editor,searchStr);
		}
		
		return true;
	}
	private void fail(ITextEditor editor, String message, int index, int count) {
		this.showResultMessage(editor, String.format(ZAP_FAIL_COUNT, message, index, count), true);
	}
	private void fail(ITextEditor editor, String message) {
		this.showResultMessage(editor, String.format(ZAP_FAIL, message), true);
	}
}
