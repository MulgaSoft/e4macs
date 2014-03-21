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

import java.util.HashMap;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.common.CommandException;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.LineBackgroundEvent;
import org.eclipse.swt.custom.LineBackgroundListener;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.console.TextConsoleViewer;
import org.eclipse.ui.internal.WorkbenchWindow;
import org.eclipse.ui.texteditor.ITextEditor;

import com.mulgasoft.emacsplus.EmacsPlusActivator;
import com.mulgasoft.emacsplus.EmacsPlusUtils;
import com.mulgasoft.emacsplus.IEmacsPlusCommandDefinitionIds;
import com.mulgasoft.emacsplus.KillRing;
import com.mulgasoft.emacsplus.MarkUtils;
import com.mulgasoft.emacsplus.RingBuffer.IRingBufferElement;
import com.mulgasoft.emacsplus.execute.EmacsPlusConsole;
import com.mulgasoft.emacsplus.execute.IEmacsPlusConsoleKey;
import com.mulgasoft.emacsplus.preferences.EmacsPlusPreferenceConstants;

/**
 * Browse the kill ring in the Emacs+ console window
 * Background highlight alternates on alternate entries
 * 
 * Supports sub-commands:
 *	 i - insert
 *	 y - insert
 *	 <SPC> - insert
 *	 <RET> - insert and quit (activate text editor)
 *	 n - next entry
 *	 p - previous entry
 *	 g - refresh kill ring display
 *	 q - quit (activate text editor)
 *	 U - undo last operation in the text editor
 *
 * @author Mark Feber - initial API and implementation
 */
@SuppressWarnings("restriction")	// for import and cast to internal org.eclipse.ui.internal.WorkbenchWindow
public class BrowseKillRingHandler extends EmacsPlusNoEditHandler implements LineBackgroundListener, IEmacsPlusConsoleKey {

	private final static String KR_CONSOLE= EmacsPlusActivator.getResourceString("KillRing_Console");	//$NON-NLS-1$
	private final static int OFF_COLOR = SWT.COLOR_LIST_BACKGROUND;
	private static RGB highlightColor = new RGB(237,237,252);
	
	private HashMap<Integer,KilledText> offsetHash;
	KilledText[] ringEntries = null; 
	
	// Unfortunately, IAutoEditStrategy doesn't work on IOConsoles AFAIK
	
	public BrowseKillRingHandler() {
		super();
		BrowseKillRingHandler.setHighlightColor(PreferenceConverter.getColor(EmacsPlusActivator.getDefault().getPreferenceStore(),
			EmacsPlusPreferenceConstants.P_AUTO_BROWSE_HIGHLIGHT));
	}

	/**
	 * @see com.mulgasoft.emacsplus.commands.EmacsPlusNoEditHandler#transform(ITextEditor, IDocument, ITextSelection, ExecutionEvent)
	 */
	@Override
	protected int transform(ITextEditor editor, IDocument document, ITextSelection currentSelection,
			ExecutionEvent event) throws BadLocationException {
		updateBrowseRing();
		return super.transform(editor, document, currentSelection, event);
	}
	
	/**
	 * @see com.mulgasoft.emacsplus.execute.IEmacsPlusConsoleKey#handleKey(org.eclipse.swt.events.VerifyEvent, org.eclipse.ui.console.TextConsoleViewer)
	 */
	public void handleKey(VerifyEvent event, TextConsoleViewer viewer) {

		// mask away any extraneous modifier characters for any direct equality tests. see SWT.MODIFIER_MASK
		int sm = event.stateMask & SWT.MODIFIER_MASK;
		if (viewer != null && (sm == 0 || sm == SWT.SHIFT)) {// && 'i' == event.character) {
			int offset;
			boolean reactivate = false;
			switch (event.character){
			case 'i':	// insert
			case 'y':
			case ' ':
				reactivate = true;
			case '\r':
			case '\n':
				event.doit = false;
				offset = getLineOffset(viewer);
				if (offset >= 0) {
					KilledText kill = offsetHash.get(offset);
					if (kill != null) {
						insertFromBrowseRing(kill.text);
					}
				}
				if (reactivate) {
					EmacsPlusConsole.getInstance().setFocus(false);
				}
				break;
			case 'n':	// next
				event.doit = false;
				browseRing(viewer,FORWARD);
				break;
			case 'p':	// previous
				event.doit = false;
				browseRing(viewer,BACKWARD);
				break;
			case 'g':	// refresh
				event.doit = false;
				updateBrowseRing();
				break;
			case 'q':	// quit
				activateEditor();
				break;
			case 'U':	// undo
				event.doit = false;
				undoEditor();
				break;
			}
		}
	}
	
	
	/**
	 * Create or update console view of the kill ring
	 */
	private void updateBrowseRing() {
		final KillRing kr = KillRing.getInstance();
		Display display = PlatformUI.getWorkbench().getDisplay();
		final Color onColor = new Color(display,highlightColor);
		final Color offColor = display.getSystemColor(OFF_COLOR);

		cleanup();
		if (!kr.isEmpty()) {
			final EmacsPlusConsole console = EmacsPlusConsole.getInstance();
			final BrowseKillRingHandler handler = this;
			console.clear();
			console.activate();
			console.setName(KR_CONSOLE);
			offsetHash = new HashMap<Integer,KilledText>(); 
			// run asynchronously to ensure widget has been set up
			EmacsPlusUtils.asyncUiRun(new Runnable() { 
				public void run() {
					int count = kr.length();
					ringEntries = new KilledText[count];
					boolean flip = false;
					int plen=0,len = 0;
					Color color = offColor;
					console.addBackground(handler);
					console.setKeyHandler(handler);
					IRingBufferElement<String> e = kr.yankElement();
					for (int i = 0; i < count; i++) {
						String text = e.get();
						int tlen = text.length();
						KilledText kt = new KilledText(text,color);
						offsetHash.put(len,kt);
						ringEntries[i]=kt;
						kt.begin = plen;
						kt.end = plen + tlen;
						plen = kt.end + 1;
						console.print(text + (i == count-1 ? EMPTY_STR : CR));
						if (flip = !flip) {
							color = onColor;
						} else {
							color = offColor;
						}
						e = kr.rotateYankPos();
						len += 1 + tlen;
					}
					console.setFocus(false);
				}});
		}
	}
	
	/**
	 * Insert text from kill ring entry into the most recently activated text editor
	 * 
	 * @param text - the text from the kill ring entry
	 */
	//	@SuppressWarnings("restriction")	// for cast to internal org.eclipse.ui.internal.WorkbenchWindow
	private void insertFromBrowseRing(String text) {
		// insert into most recently active editor
		IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
		RecentEditor recent = getRecentEditor();
		// use widget to avoid unpleasant scrolling side effects of IRewriteTarget		
		Control widget = MarkUtils.getTextWidget(recent.editor);
		if (recent.editor != null) {
			try {
				// cache for ancillary computations
				setThisEditor(recent.editor);
				// reduce the amount of unnecessary work
				if (window instanceof WorkbenchWindow) {
					((WorkbenchWindow) window).largeUpdateStart();
				}
				widget.setRedraw(false);
				recent.page.activate(recent.epart);
				insertText(recent.editor.getDocumentProvider().getDocument(recent.editor.getEditorInput()),
						(ITextSelection)recent.editor.getSelectionProvider().getSelection(), text);
			} catch (Exception e) {
			} finally {
				widget.setRedraw(true);
				setThisEditor(null);
				if (window instanceof WorkbenchWindow) {
					((WorkbenchWindow) window).largeUpdateEnd();
				}
			}
		} else {
			beep();
		}
	}
	
	/**
	 * Undo the last command in the the most recently activated text editor
	 */
	private void undoEditor() {
		RecentEditor recent = getRecentEditor();
		if (recent != null) {
			try {
				setThisEditor(recent.editor);
				recent.page.activate(recent.epart);
				this.executeCommand(IEmacsPlusCommandDefinitionIds.EMP_UNDO, null, recent.editor);
			} catch (ExecutionException e) {
			} catch (CommandException e) {
			} finally {
				setThisEditor(null);
				EmacsPlusConsole.getInstance().setFocus(false);
			}
		}
	}
	
	/**
	 * Activate the most recently activated text editor
	 */
	private void activateEditor() {
		RecentEditor recent = getRecentEditor();
		if (recent != null) {
			try {
				setThisEditor(recent.editor);
				recent.page.activate(recent.epart);
			} finally {
				setThisEditor(null);
			}
		}
	}

	/**
	 * Get the most recent activated text editor
	 * 
	 * @return editor and activation info
	 */
	private RecentEditor getRecentEditor() {
		ITextEditor result = null;
		IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
		IWorkbenchPage page = window.getActivePage();
		IEditorPart epart = page.getActiveEditor();
		result = (ITextEditor)(epart != null ?  epart.getAdapter(ITextEditor.class) : epart);
		
		return new RecentEditor(result,page,epart);
	}
	
	/**
	 * Navigate up or down the ring entry by entry
	 * 
	 * @param viewer the viewer on the console
	 * @param dir FORWARD or BACKWARD
	 */
	private void browseRing(TextConsoleViewer viewer, int dir) {
		IDocument doc = viewer.getDocument(); 
		StyledText st = viewer.getTextWidget();
		if (doc != null && st != null) {
			int lines = doc.getNumberOfLines();
			int off = st.getCaretOffset();
			
			try {
				int l = doc.getLineOfOffset(off);
				KilledText okill = offsetHash.get(doc.getLineOffset(l));
				KilledText nkill = null;
				int noff = -1;
				while ((l = l+dir) > -1 && l < lines){
					off = doc.getLineOffset(l);
					KilledText tkill = offsetHash.get(off);
					if (nkill == null) {
						if (tkill != null && tkill != okill) {
							nkill = offsetHash.get(off);
							noff = off;
							if (dir == FORWARD) {
								break;
							}
						}
					} else {
						if (tkill != null && tkill != nkill){
							break;
						} else {
							noff = off;
						}
					}
				}
				if (noff > -1) {
					st.setCaretOffset(noff);
					viewer.revealRange(noff, 0);
				}
			}catch (BadLocationException e) {
			}
		}
	}
	
	/** 
	 * Get the line offset of the cursor
	 * 
	 * @param viewer the console viewer
	 * @return the line offset
	 */
	private int getLineOffset(TextConsoleViewer viewer) {
		int result = -1;
		IDocument doc = viewer.getDocument(); 
		StyledText st = viewer.getTextWidget();
		if (doc != null && st != null) {
			int off = st.getCaretOffset();
			try {
				IRegion info = doc.getLineInformationOfOffset(off);
				result = info.getOffset();
			}catch (BadLocationException e) {
			}
		}
		return result;
	}
	
	/**
	 * Return the correct background highlight for the kill ring entry line offset
	 * 
	 * @see org.eclipse.swt.custom.LineBackgroundListener#lineGetBackground(org.eclipse.swt.custom.LineBackgroundEvent)
	 */
	public void lineGetBackground(LineBackgroundEvent event) {
		KilledText kt = offsetHash.get(event.lineOffset);
		if (kt == null) {
			for (KilledText k : ringEntries) {
				if (event.lineOffset >= k.begin && event.lineOffset <= k.end) {
					offsetHash.put(event.lineOffset, k);
					kt = k;
					break;
				}
			}
		}
		if (kt != null) {
			event.lineBackground = kt.color;
		}
	}
	
	/**
	 * Utility class to wrap the necessary kill ring entry information
	 * 
	 * @author Mark Feber - initial API and implementation
	 */
	private class KilledText {
		int begin = 0;
		int end = 0;
		Color color;
		String text;
		KilledText(String text, Color color) {
			this.color = color;
			this.text = text;
		}
	}
	
	/**
	 * Utility class to wrap recent editor and activation information
	 *  
	 * @author Mark Feber - initial API and implementation
	 */
	private class RecentEditor {
		ITextEditor editor;
		IWorkbenchPage page;
		IEditorPart epart;
		RecentEditor(ITextEditor editor,IWorkbenchPage page, IEditorPart epart) {
			this.editor = editor; this.page = page; this.epart = epart;
		}
	}
	
	void cleanup() {
		ringEntries = null;
		offsetHash = null;
	}
	
	/**
	 * Set the alternate highlight color from preferences
	 * 
	 * @param color RGB from preference setting
	 */
	public static void setHighlightColor(RGB color) {
		if (color != null) {
			highlightColor = color;
		}
	}
}
