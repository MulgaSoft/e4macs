/**
 * Copyright (c) 2009, 2010 Mark Feber, MulgaSoft
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 */
package com.mulgasoft.emacsplus;

import static com.mulgasoft.emacsplus.EmacsPlusUtils.EMPTY_STR;
import static com.mulgasoft.emacsplus.EmacsPlusUtils.getPreferenceStore;
import static com.mulgasoft.emacsplus.IEmacsPlusCommandDefinitionIds.YANK;
import static com.mulgasoft.emacsplus.preferences.PrefVars.DELETE_SEXP_TO_CLIPBOARD;
import static com.mulgasoft.emacsplus.preferences.PrefVars.DELETE_WORD_TO_CLIPBOARD;
import static com.mulgasoft.emacsplus.preferences.PrefVars.KILL_RING_MAX;
import static com.mulgasoft.emacsplus.preferences.PrefVars.REPLACE_TEXT_TO_KILLRING;

import java.util.Hashtable;
import java.util.Map;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocumentListener;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.texteditor.ITextEditor;

import com.mulgasoft.emacsplus.preferences.PrefVars;

/**
 * Kill Ring Buffer implementation - Singleton
 * 
 * @author Mark Feber - initial API and implementation
 */
public class KillRing extends RingBuffer<String> implements IDocumentListener {

	// Singleton
	private volatile static KillRing ring = null;
	
	// the append state of the kill command
	private boolean append = false;
	// the override the append state of the kill command
	private boolean forceAppend = false;
	// the direction of kill
	private boolean reverse = false;
	// override the direction of kill
	private boolean forceReverse = false;
	// store replaced selection on kill ring when true
	private boolean selectionReplace = false;
	// mark current kill command
	private String killCmd = null;
	// store the clipboard text from the last activation
	private String lastActivationText = null;
	// constrain clipboard copy to these commands 	
	private Map<String,String> clipCommands = new Hashtable<String,String>();
	// on forceAppend, back-copy the appended string to the clipboard
	private Map<String,String> forceClipCommands = new Hashtable<String,String>();
	{
		forceClipCommands.put(ActionFactory.COPY.getId(), ActionFactory.COPY.getId());		
		forceClipCommands.put(IEmacsPlusCommandDefinitionIds.EMP_COPY, IEmacsPlusCommandDefinitionIds.EMP_COPY);
		forceClipCommands.put(IEmacsPlusCommandDefinitionIds.COPY_QUALIFIED_NAME, IEmacsPlusCommandDefinitionIds.COPY_QUALIFIED_NAME);
	}
	
	// flag kill-ring deactivation (s/b temporary)
	private boolean deactivate = false;

	public static KillRing getInstance() {
		if (ring == null) {
			initialize();
		}
		return ring;
	}

	private KillRing(int size) {
		super(size);
	}
	
	/**
	 * Initialize the kill ring with settings from the preference store 
	 */
	private static synchronized void initialize() {
		if (ring == null) {
			Boolean clipword = false;
			Boolean clipsexp = true;
			int ringsize = LARGE_RING_SIZE;
			IPreferenceStore store = EmacsPlusActivator.getDefault().getPreferenceStore();
			if (store != null) {
				ringsize = store.getInt(KILL_RING_MAX.getPref());
				clipword = store.getBoolean(DELETE_WORD_TO_CLIPBOARD.getPref());
				clipsexp = store.getBoolean(DELETE_SEXP_TO_CLIPBOARD.getPref());
			}
			ring = new KillRing(ringsize);
			ring.setClipFeature(DELETE_WORD_TO_CLIPBOARD, clipword);
			ring.setClipFeature(DELETE_SEXP_TO_CLIPBOARD, clipsexp);
			
			getPreferenceStore().addPropertyChangeListener(
				new IPropertyChangeListener() {
					public void propertyChange(PropertyChangeEvent event) {
						String prop = event.getProperty();
						if (REPLACE_TEXT_TO_KILLRING.getPref().equals(prop)) {
							KillRing.getInstance().setSelectionReplace((Boolean)event.getNewValue());
						} else if (DELETE_WORD_TO_CLIPBOARD.getPref().equals(prop)) {
							KillRing.getInstance().setClipFeature(DELETE_WORD_TO_CLIPBOARD, (Boolean)event.getNewValue());
						} else if (DELETE_SEXP_TO_CLIPBOARD.getPref().equals(prop)) {
							KillRing.getInstance().setClipFeature(DELETE_SEXP_TO_CLIPBOARD, (Boolean)event.getNewValue());
						} else if (KILL_RING_MAX.getPref().equals(prop)) {
							KillRing.getInstance().setSize((Integer)event.getNewValue());
						}
					}
				}
		);

		}
	}

	// is this a reverse kill command?
	protected boolean isReverse() {
		return reverse || forceReverse;
	}

	// note the direction of the kill command
	protected void setReverse(boolean reverse) {
		this.reverse = reverse;
	}
	
	// override the direction of the kill command
	protected void setForceReverse(boolean reverse) {
		this.forceReverse = reverse;
	}

	// are we in the append state?
	protected boolean isAppend() {
		return append || isForceAppend();
	}

	private boolean isForceAppend() {
		return forceAppend;
	}
	
	// note the append state of the kill command
	protected void setAppend(boolean append) {
		this.append = append;
	}
	
	// force the append state of the kill command
	public void setForceAppend(boolean append) {
		forceAppend = append;
	}
	
	public boolean isDeactivated() {
		return deactivate;
	}

	public void setDeactivated(boolean deactivate) {
		this.deactivate = deactivate;
	}

	public void setSelectionReplace(boolean selectionReplace) {
		this.selectionReplace = selectionReplace;
	}
	
	public boolean isSetSelectionReplace() {
		return selectionReplace;
	}
	
	public synchronized IRingBufferElement<String> putNext(String text, int offset) {
		KillRingBufferElement result = null; 
		if (!isDeactivated() && text != null && text.length() > 0) {
			// if appending, check if text is where the last kill left off
			if (isAppend() && !isEmpty() && ((result = getElement()) != null)
					&& (isForceAppend() || (offset != NO_POS && (result.getOffset() == (isReverse() ? offset + text.length() : offset))))) {
				if (isReverse()) {
					result.set(text + result.get());
					result.setOffset(offset);
				} else {
					result.set(result.get() + text);
					if (isForceAppend()) {
						result.setOffset(offset);	
					}
				}
			} else {
				// if element doesn't yet exist will call overridden getNewElement()
				result = (KillRingBufferElement) super.putNext(text);
				result.setOffset(offset);	
			}
			if (isClipCommand(killCmd)) {
				setClipboardContents();
			}
			setAppend(true);
			setForceAppend(false);
			setReverse(false);
		}
		return result;
	}

	public void setKill(String cmdId, boolean reverse) {
		setKillCmd(cmdId);
		setForceReverse(reverse);
	}
	
	// note the current command kill command
	protected void setKillCmd(String currentCmd) {
		this.killCmd = currentCmd;
	}
	
	protected String getKillCmd() {
		return this.killCmd;
	}

	/**
	 * Enable which commands copy their kill contents to the clipboard as well as the kill ring
	 * This expands the feature available in the default eclipse implementation.
	 * 
	 * @param feature
	 * @param value
	 */
	public void setClipFeature(PrefVars feature, Boolean value){
		switch(feature) {
			case DELETE_WORD_TO_CLIPBOARD:
				if (value){
					addClipCommand(IEmacsPlusCommandDefinitionIds.DELETE_NEXT_WORD);
					addClipCommand(IEmacsPlusCommandDefinitionIds.DELETE_PREVIOUS_WORD);
				}else {
					removeClipCommand(IEmacsPlusCommandDefinitionIds.DELETE_NEXT_WORD);
					removeClipCommand(IEmacsPlusCommandDefinitionIds.DELETE_PREVIOUS_WORD);
				}
				break;
			case DELETE_SEXP_TO_CLIPBOARD:
				if (value){
					addClipCommand(IEmacsPlusCommandDefinitionIds.KILL_FORWARD_SEXP);
					addClipCommand(IEmacsPlusCommandDefinitionIds.KILL_BACKWARD_SEXP);
				} else {
					removeClipCommand(IEmacsPlusCommandDefinitionIds.KILL_FORWARD_SEXP);
					removeClipCommand(IEmacsPlusCommandDefinitionIds.KILL_BACKWARD_SEXP);
				}
				break;
			default:
				break;
		}
	}

	
	public void addClipCommand(String clipCommand) {
		clipCommands.put(clipCommand,clipCommand);
	}
	
	public void removeClipCommand(String clipCommand) {
		clipCommands.remove(clipCommand);
	}

	private boolean isClipCommand(String command){
		boolean result = false;
		if (command != null) {
			result = clipCommands.containsKey(command);
			if (!result && forceAppend) {
				result = forceClipCommands.containsKey(command);
			}
		} 
		return result;
	}
	
	/**
	 * Propagate the killed text to the clipboard 
	 */
	private void setClipboardContents(){
		setClipboardText(getElement().get());
	}
	
	/**
	 * On a COPY command, retrieve the text from the clipboard and add to kill ring
	 */
	void addClipboardContents(){
		String clipText = getClipboardText(); 
		if (clipText != null && clipText.length() > 0) {
			putNext(clipText, NO_POS);
			documentChanged(null); // clear any flags
		}
	}
	
	/** 
	 * Store clipboard text if it has changed on activation.
	 * This method is used by listeners exclusively.
	 */
	void checkClipboard() {
		documentChanged(null);
		String clipText = getClipboardText(); 
		if (clipText != null && !isWhitespace(clipText)){
			if (!clipText.equals(lastActivationText) && !clipText.equals(get(getYankpos()))) {
				// is it the same as the current text?
				String t;
				if ((t = get(getPos())) != null) {
					// clipboard text may be left over from an append-next-kill, so this covers
					// either end as well
					if (t.startsWith(clipText) || t.endsWith(clipText)) {
						return;
					}
				}
				lastActivationText = clipText;
				putNext(clipText,NO_POS);		
			}
		}
	}
	
	/**
	 * Get the text from the system clipboard
	 * 
	 * @return the system clipboard content as a String
	 */
	public String getClipboardText() {
		Clipboard clipboard = new Clipboard(Display.getCurrent());
		TextTransfer plainTextTransfer = TextTransfer.getInstance();
		String cliptxt = (String)clipboard.getContents(plainTextTransfer, DND.CLIPBOARD);
		clipboard.dispose();
		return cliptxt;
	}
	
	public void setClipboardText(String text){
		Clipboard clipboard = new Clipboard(Display.getCurrent());
		TextTransfer plainTextTransfer = TextTransfer.getInstance();
		clipboard.setContents(new Object[] {text},new Transfer[]{plainTextTransfer});
		clipboard.dispose();
	}
	
	private boolean isWhitespace(String text) {
		byte[] bytes = text.getBytes();
		for (int i=0; i < bytes.length; i++) {
			if (bytes[i] > ' ') {
				return false;
			}
		}
		return true;
	}
	
	/* Listener behavior */
	
	/**
	 * @see org.eclipse.jface.text.IDocumentListener#documentAboutToBeChanged(org.eclipse.jface.text.DocumentEvent)
	 */
	public void documentAboutToBeChanged(DocumentEvent event) {
		// add the text to the kill ring
		if (killCmd != null || isSelectionReplace(event)) {
			try {
				putNext((event.getDocument().get(event.getOffset(), event.getLength())), event.getOffset());
			} catch (BadLocationException e) {}
		}
	}
	
	/**
	 * Determine if a selection is being replaced by non-emacs+ behavior (or YANK), and save the
	 * replaced content in the kill ring. This captures the Eclipse (but not emacs) behavior where
	 * typing/pasting into a selection replaces the old with the new, so it is appropriate to save
	 * the old text to the kill ring.
	 * 
	 * @param event the DocumentEvent containing the IDocument, offset, and length
	 * @return true if the non-zero length region matches the current selection in the editor
	 */
	private boolean isSelectionReplace(DocumentEvent event) {
		int len = event.getLength();
		// ignore plain insertion or any emacs+ (except YANK) command invocation
		if (selectionReplace &&  len > 0 && shouldSave()) {
			ITextEditor editor = EmacsPlusUtils.getCurrentEditor();
			// otherwise, if we can get the selection, see if it matches the replace region
			if (editor != null && editor.getDocumentProvider().getDocument(editor.getEditorInput()) == event.getDocument()) {
				ISelection isel = editor.getSelectionProvider().getSelection();
				if (isel instanceof ITextSelection) {
					ITextSelection selection = (ITextSelection)isel;
					boolean result = selection.getOffset() == event.getOffset() && selection.getLength() == len;
					return result;
				}
			}
		}
		return false;
	}
	
	private boolean shouldSave() {
		String cmd = MarkUtils.getCurrentCommand();
		return (cmd == null || cmd.equals(YANK));
	}

	/**
	 * @see org.eclipse.jface.text.IDocumentListener#documentChanged(org.eclipse.jface.text.DocumentEvent)
	 */
	public void documentChanged(DocumentEvent event) {
		// clear flags when non-kill command detected
		if (killCmd == null) {
			setAppend(false);
			setForceAppend(false);			
			setReverse(false);
		}
		setYanked(false);
	}
	
	/* Add offset information to the kill ring element */
	
	/**
	 * @see com.mulgasoft.emacsplus.RingBuffer#getElement()
	 */
	@Override
	protected KillRingBufferElement getElement() {
		return (KillRingBufferElement)super.getElement();
	}	
	
	/**
	 * @see com.mulgasoft.emacsplus.RingBuffer#getNewElement()
	 */
	@Override
	protected IRingBufferElement<String> getNewElement() {
		return new KillRingBufferElement();
	}

	/**
	 * Add offset information to the kill ring element 
	 * Holds the text and the document offset of the initial character
	 * 
	 * @author Mark Feber - initial API and implementation
	 */
	protected class KillRingBufferElement extends AbstractRingBufferElement {
		
		public KillRingBufferElement() {
			set(EMPTY_STR);
		}
		
		private int offset = -1;

		private int getOffset() {
			return offset;
		}

		private void setOffset(int offset) {
			this.offset = offset;
		}
	}
}
