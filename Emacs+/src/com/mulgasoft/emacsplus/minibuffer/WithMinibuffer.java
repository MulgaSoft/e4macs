/**
 * Copyright (c) 2009, 2013 Mark Feber, MulgaSoft
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 */
package com.mulgasoft.emacsplus.minibuffer;

import static com.mulgasoft.emacsplus.EmacsPlusUtils.isMac;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.NotEnabledException;
import org.eclipse.core.commands.NotHandledException;
import org.eclipse.core.commands.ParameterizedCommand;
import org.eclipse.core.commands.common.NotDefinedException;
import org.eclipse.jface.bindings.Binding;
import org.eclipse.jface.bindings.keys.KeySequence;
import org.eclipse.jface.bindings.keys.KeyStroke;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentExtension4;
import org.eclipse.jface.text.IFindReplaceTarget;
import org.eclipse.jface.text.ITextListener;
import org.eclipse.jface.text.ITextViewerExtension;
import org.eclipse.jface.text.TextEvent;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.custom.VerifyKeyListener;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.widgets.Event;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.IHandlerService;
import org.eclipse.ui.keys.IBindingService;
import org.eclipse.ui.texteditor.AbstractTextEditor;
import org.eclipse.ui.texteditor.IStatusFieldExtension;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.texteditor.StatusLineContributionItem;

import com.mulgasoft.emacsplus.EmacsPlusUtils;
import com.mulgasoft.emacsplus.MarkUtils;
import com.mulgasoft.emacsplus.execute.KbdMacroSupport;

/**
 * Commands that want to read from the minibuffer should use of subclass of this
 *  
 * Based on a true story: org.eclipse.ui.texteditor.IncrementalFindTarget
 * 
 * @author Mark Feber - initial API and implementation
 */
public abstract class WithMinibuffer implements FocusListener, ISelectionChangedListener, ITextListener, MouseListener, VerifyKeyListener {

	protected final static String EMPTY_STR = "";   		  //$NON-NLS-1$ 
	
	// The element before which to insert our status updates
	private static final String POSITION_ID = "ElementState"; //$NON-NLS-1$ 
	// The identifier for the StatusLineContributionItem 
	private static final String STATUS_ID = "minibuffer";     //$NON-NLS-1$ 
	
	static final String N_GEN = "\\c";  					  //$NON-NLS-1$
	static final String N_NEW = "\\n";  					  //$NON-NLS-1$
	static final String N_RET = "\\r";  					  //$NON-NLS-1$
	static final String N_TAB = "\\t";  					  //$NON-NLS-1$
	static final String N_BS = "\\b";   					  //$NON-NLS-1$
	static final String N_FF = "\\f";   					  //$NON-NLS-1$
	static final char CR = '\r';
	static final char LF = '\n';
	
	private ITextEditor editor;
	private IWorkbenchPage page;
	private IDocument document;
	
	private boolean installed = false;
	
	private static StatusLineContributionItem statusItem;
	
	/* mini-buffer commands lose their identity once the mini-buffer setup is complete, so remember it here */
	private String commandId = null;	// the command id of the invoker
	private String resultString = null; // final message on completion
	private boolean resultError = false; // final status type on completion
	

	private boolean inBegin = false;
	private boolean executing = false;
	
	private MinibufferImpl minibufferStringImpl;
	
	private StyledText widget = null;
	private ISourceViewer viewer = null;
	
	private boolean lowercase = false;
	
	private int lastKeyCode = -1;
	private String eol = null;
	
	private static boolean mxLaunch = false;
	// we don't want to clear the M-x status result on M-x launch
	private boolean initStatusMsg() {
		return !mxLaunch;
	}
	protected abstract void addOtherListeners(IWorkbenchPage page,ISourceViewer viewer, StyledText widget);
	protected abstract void removeOtherListeners(IWorkbenchPage page,ISourceViewer viewer, StyledText widget);
	
	/**
	 * Perform any minibuffer setup.  Called from beginSession before any actions are taken
	 * 
	 * @param editor
	 * @param page
	 * @return true if initialization was successful, else false
	 */
	protected abstract boolean initializeBuffer(ITextEditor editor, IWorkbenchPage page);
	
	/**
	 * Perform the desired action on the result of the minibuffer input
	 * 
	 * @param editor
	 * @param minibufferResult usually a String
	 *  
	 * @return true if we should exit after the execution
	 */
	protected abstract boolean executeResult(ITextEditor editor, Object minibufferResult);
	
	/**
	 * Get the prefix for the minibuffer prompt
	 * This may be computed based on the current state of input in the minibuffer
	 * 
	 * @return the prefix String
	 */
	protected abstract String getMinibufferPrefix();	
	
	// we use this to temporarily disable key filters on Ctrl
	protected IBindingService bindingService;

	protected boolean isInBegin() {return inBegin;}
	
	// TODO: move to ExecutingMinibuffer
	protected boolean isExecuting() {return executing;}
	
	protected void setExecuting(boolean executing) {
		this.executing = executing;
	}
	protected boolean isCompleting() {
		return false;
	}
	
	protected void showCompletions() {}
	
	/**
	 * @return the installed state
	 */
	protected boolean isInstalled() {
		return installed;
	}
	
	/**
	 * minibuffer impl will force lower case if true
	 * 
	 * @param lowercase 
	 */
	protected void setLowercase(boolean lowercase) {
		this.lowercase = lowercase;
	}
	protected boolean isLowercase() {
		return lowercase;
	}

	/**
	 * Set when a minibuffer command is launched from M-x
	 * 
	 * @param true iff launched from M-x
	 */
	static void setMxLaunch(boolean mxLaunch) {
		WithMinibuffer.mxLaunch = mxLaunch;
	}

	/**
	 * @return the editor
	 */
	protected ITextEditor getEditor() {
		return editor;
	}
	
	protected IDocument getDocument() {
		if (document == null) {
			document = editor.getDocumentProvider().getDocument(editor.getEditorInput());
		}
		return document;
	}
	/**
	 * @return the page
	 */
	protected IWorkbenchPage getPage() {
		return page;
	}
	
	protected StyledText getTextWidget() {
		return widget;
	}
	
	protected ISourceViewer getViewer() {
		return viewer;
	}
	
	protected IBindingService getBindingService() {
		return bindingService;
	}
	
	protected MinibufferImpl getMB() {
		return minibufferStringImpl;
	}
	
	public void setCommandId(String commandId) {
		this.commandId = commandId;
	}
	
	public String getCommandId() {
		return commandId;
	}
	
	protected void beep() {
		EmacsPlusUtils.beep();
	}
	
	public boolean beginSession(ITextEditor editor, IWorkbenchPage page, ExecutionEvent event) {
		boolean ok = false;
		this.editor = editor;
		this.page = page;
		// TODO: Hack?  When the command view was double clicked, then M-x executing
		// the editor doesn't really have focus, so force it.
		try {
			inBegin = true;
			editor.setFocus();
		} finally {
			inBegin = false;
		}
		minibufferStringImpl = new MinibufferImpl(getDocument(),lowercase);
		if (initializeBuffer(editor,page)) {
			ok = install();
			if (ok) {
				if (handlesCtrl() || handlesAlt()){
					setKeyFilter(false);
				}
				if (initStatusMsg()) {
					// clear any normal/error message
					doSetResultMessage(EMPTY_STR, true);
					doSetResultMessage(EMPTY_STR, false);
				}
				// initialize our status message
				updateStatusLine(EMPTY_STR);
			} else {
				endSession();
			}
		}
		return ok;
	}
	
	public void endSession() {
		if (this.editor != null) {
			leave();
		}
	}
	
	/**
	 * Installs this target. I.e. adds all required listeners.
	 */
	private boolean install() {
		if (editor instanceof AbstractTextEditor && !isInstalled()) {
			bindingService = (IBindingService) PlatformUI.getWorkbench().getService(IBindingService.class);
			viewer = findSourceViewer(editor);
			if (viewer != null) {
				widget = viewer.getTextWidget();
				if (widget == null || widget.isDisposed()) {
					viewer = null;
					widget = null;
					return false;
				}
				addStatusContribution(editor);
				widget.addMouseListener(this);
				widget.addFocusListener(this);
				viewer.addTextListener(this);

				ISelectionProvider selectionProvider = viewer.getSelectionProvider();
				if (selectionProvider != null)
					selectionProvider.addSelectionChangedListener(this);

				if (viewer instanceof ITextViewerExtension){
					((ITextViewerExtension) viewer).prependVerifyKeyListener(this);
					KbdMacroSupport.getInstance().continueKbdMacro(this,editor);
				} else {
					widget.addVerifyKeyListener(this);
				}
				addOtherListeners(page,viewer, widget);
				installed = true;
			}
		}
		return installed;
	}
	
	/**
	 * Uninstalls itself. I.e. removes all listeners installed in
	 * <code>install</code>.
	 */
	private void uninstall() {
		try {
			if (isInstalled()) {
				setKeyFilter(true);
				if (viewer != null) {
					removeStatusContribution(editor);
					viewer.removeTextListener(this);

					ISelectionProvider selectionProvider = viewer.getSelectionProvider();
					if (selectionProvider != null)
						selectionProvider.removeSelectionChangedListener(this);

					if (widget != null && !widget.isDisposed()) {
						widget.removeMouseListener(this);
						widget.removeFocusListener(this);
					}

					if (viewer instanceof ITextViewerExtension) {
						((ITextViewerExtension) viewer).removeVerifyKeyListener(this);
						KbdMacroSupport.setKbdMinibuffer(null);
					} else {
						if (widget != null && !widget.isDisposed())
							widget.removeVerifyKeyListener(this);
					}
					removeOtherListeners(page, viewer, widget);
				}
			}
		} finally {
			widget = null;
			page = null;	// TODO: elsewhere?
//			bindingService = null;
			installed = false;
		}
	}
	
	private boolean left = false;
	protected void leave() { leave(false);}
	
	protected void leave(boolean closeDialog) {
		try {
			if (!left) {
				uninstall();
				if (closeDialog || !isExecuting()) {
					closeDialog();
				}
				String resultStr = getResultString();
				setResultMessage(resultStr != null ? resultStr : EMPTY_STR, getResultError());
			}
		} finally {
			MarkUtils.setCurrentCommand(null);
			setExecuting(false);
			editor = null;
			left = true;
		}		
	}

	protected void closeDialog() {}
	
	/**
	 * Write the message to the minibuffer part of the status line
	 */
	public void updateStatusLine() {
		updateStatusLine(prevMessage);
	}
	
	private String prevMessage = EMPTY_STR;
	
	protected void updateStatusLine(String message) {
		if (statusItem != null && (!KbdMacroSupport.getInstance().isExecuting()) ) {
			{
				prevMessage = message;
				String normalizedMessage = normalizeString(message);
				statusItem.setText(getMinibufferPrefix() + normalizedMessage);
				((IStatusFieldExtension) statusItem).setVisible(true);
				// make sure we're still active 
				if (editor != null) {
					EmacsPlusUtils.forceStatusUpdate(editor);
				}
			}
		}
	}
	
	/**
	 * Normalize message display - the default behavior is to return it unchanged
	 * @param message
	 * @return the message
	 */
	protected String normalizeString(String message) {
		return message;
	}
	
	protected String normalizeChar(char ocp) {
		String result = null;
		if (ocp < ' ') {
			switch (ocp) {
			case CR:
				result = N_RET;
				break;
			case LF:
				result = N_NEW;
				break;
			case '\t':
				result = N_TAB;
				break;
			case '\f':
				result = N_FF;
				break;
			case '\b':
				result = N_BS;
				break;
			default:
				result = N_GEN + ocp;
			}
		} else {
			result = String.valueOf(ocp);;
		}
		return result;
	}
	
	/************ result message ******************/
	
	protected void setResultString(String resultString, boolean resultError) {
		this.resultString = resultString;
		this.resultError = resultError;
	}

	protected String getResultString() {
		return resultString;
	}
	
	protected boolean getResultError() {
		return resultError;
	}
	

	protected void setResultMessage(String message) {
		doSetResultMessage(message,resultError);
	}

	/**
	 * Write the message to the standard part of the status line
	 * 
	 * @param message
	 * @param error
	 */
	protected void setResultMessage(String message, boolean error) {
		doSetResultMessage(message,error);
	}
	
	private void doSetResultMessage(String message, boolean error) {
		String mes = EMPTY_STR;
		if (message != null) {
			mes = ((message.length() > 0) ? getMinibufferPrefix() : EMPTY_STR) + message;
		}
		if (editor != null) {
			if (page != null) {
				page.activate(editor);
			}
			EmacsPlusUtils.showMessage(editor, mes, error);
		}
	}
	
 	private synchronized void removeStatusContribution(IWorkbenchPart part) {
		statusItem.setVisible(false);
		EmacsPlusUtils.getStatusLineManager(part).remove(statusItem);
		EmacsPlusUtils.forceStatusUpdate(part);
		statusItem.setText(EMPTY_STR);
	}

	private synchronized void addStatusContribution(IWorkbenchPart editor) {
		statusItem = getStatusLineItem();
		try {
			EmacsPlusUtils.getStatusLineManager(editor).insertBefore(POSITION_ID, statusItem);
		} catch (IllegalArgumentException e) {
			EmacsPlusUtils.getStatusLineManager(editor).add(statusItem);
		}
		statusItem.setVisible(true);
		statusItem.setText(EMPTY_STR);
	}

	private synchronized StatusLineContributionItem getStatusLineItem() {
		if (statusItem == null) {
			statusItem = new StatusLineContributionItem(STATUS_ID, true, getStatusLineLength());
		}
		return statusItem;
	}

	// TODO: compute a reasonable length
	protected int getStatusLineLength() {
		return 80 + 3;
	}
	
	/* ***************** key handling *******************/
	
	protected abstract boolean handlesCtrl(); 
	protected abstract boolean handlesAlt();
	protected abstract boolean handlesTab();

	/**
	 * @return the lastKeyCode
	 */
	protected int getLastKeyCode() {
		return lastKeyCode;
	}
	
	// Added to enable TRAVERSE key workaround (see HistoryMinibuffer)
	protected void setLastKeyCode(int keyCode) {
		lastKeyCode = keyCode;
	}

	protected void handleKey(VerifyEvent event) {
		try {
			if (!event.doit)
				return;

			if (event.character != 0) { // process typed character
				charEvent(event);
			} else { // some other key down 
				noCharEvent(event);
			}
		} catch (Exception e) {
			System.out.println(e);
			e.printStackTrace();
		} finally {
			setLastKeyCode(event.keyCode);
		}
	}
	protected final static Character QUOTING = 'Q';
	protected boolean isQuoting(VerifyEvent event){
		return (event.data instanceof Character && (Character)event.data == QUOTING && (event.stateMask & SWT.CTRL) != 0);
	}
	
	protected void charEvent(VerifyEvent event) {

		switch (event.character) {

		case 0x0D: // CR - execute command (if complete) \r
			if (isQuoting(event)) {
				event.doit = false;
				dispatchCtrl(event);
				break;
			}
			executeCR(event);
			break;
		case 0x1B: // ESC - another way to leave
			// TODO - could also be ^[
			KbdMacroSupport.getInstance().exitWhenDefining();				
			leave(true);
			event.doit = false;
			break;

		case 0x08: // BS
			backSpaceChar(event);
			break;
		case 0x7F: // DEL
			deleteChar(event);
			break;
		case SWT.TAB: // disable after tab traversal interception
			if (handlesTab()){
				dispatchTab(event);
			}
			event.doit = false;
			break;
			//case '?': // ? completion disabled as its used as a simple wildcard
		case ' ': // space completion
			if (isCompleting()) {
				showCompletions();
				event.doit = false;
				break;
			}
		default:
			// If we're on a mac, then treat ALT & COMMAND the same in the minibuffer
			boolean ismac = isMac();
			// mask away any extraneous modifier characters for any direct equality tests. see SWT.MODIFIER_MASK
			// make ALT and COMMAND behave equivalently on the mac for Ctrl or Alt dispatch
			int sm = event.stateMask & SWT.MODIFIER_MASK;
			if (checkControl(event)) {
				if (dispatchCtrl(event)) {
					event.doit = false;
					break;
				} 
			} else if (checkAlt(event)) {
				if (dispatchAlt(event)) {
					event.doit = false;
					break;
				}
			} else if (checkAltCtrl(event) && dispatchAltCtrl(event)) {
					event.doit = false;
					break;
			} else {
				// SWT.ALT | SWT.CTRL covers AltGraph - used in international keyboards (see Eclipse bug 43049)
				// Also special chars on MacOs (see Eclipse bug 272994)					
				// Although, testing on a mac shows that the Option-<char>, comes in as keyCode == 0 and no modifiers
				boolean special = (ismac ? ((sm == (SWT.ALT | SWT.SHIFT)) || sm == SWT.ALT) : sm == (SWT.ALT | SWT.CTRL));
				// but if the key has a command binding associated, then leave and process 
				if (special && hasBinding(event)) {
					ITextEditor ed = editor;					
					leave();
					executeBinding(ed,event);
					event.doit = false;
				} else if (sm == 0 || sm == SWT.SHIFT || special) {
					event.doit = false;
					if (event.keyCode != 0 || (ismac && event.character != 0)) {
						addIt(event);
					}
				}
			}
		}
	}
	
	/**
	 * Is this a Ctrl VerifyEvent we want to handle
	 * 
	 * @param event
	 * @return true if yes, else false
	 */
	boolean checkControl(VerifyEvent event) {
		return (event.stateMask & SWT.CTRL) != 0 && ((event.stateMask & SWT.ALT) == 0 && (!isMac() || (event.stateMask & SWT.COMMAND) == 0))
		&& handlesCtrl();
	}
	
	/**
	 * Is this an Alt VerifyEvent we want to handle
	 * 
	 * @param event
	 * @return true if yes, else false
	 */
	boolean checkAlt(VerifyEvent event) {
		return ((event.stateMask & SWT.ALT) != 0 || (isMac() && (event.stateMask & SWT.COMMAND) != 0)) && (event.stateMask & SWT.CTRL) == 0 
		&& handlesAlt();
	}
	
	/**
	 * Is this a Ctrl Alt VerifyEvent we want to handle
	 * 
	 * @param event
	 * @return true if yes, else false
	 */
	boolean checkAltCtrl(VerifyEvent event) {
		int sm = event.stateMask & SWT.MODIFIER_MASK;
		return (sm == (SWT.ALT | SWT.CTRL) || (isMac() && sm == (SWT.COMMAND | SWT.CTRL)));
	}
	
	/**
	 * @param event
	 */
	protected void executeCR(VerifyEvent event) {
		boolean shouldLeave = true;
		try {
			shouldLeave = executeResult(editor, getMBString());
		} finally {
			event.doit = false;
			if (shouldLeave) {
				crExitKbdMacro();
				leave(true);
			}
		}
	}
	
	/**
	 * Normally, store exit of the minibuffer on CR
	 * However a minibuffer such as MetaXMinibuffer, that chains execution, should not
	 */
	protected void crExitKbdMacro() {
		KbdMacroSupport.getInstance().exitWhenDefining();
	}
	
	protected void noCharEvent(VerifyEvent event) {

		switch (event.keyCode) {
		case SWT.CTRL:	
			if (handlesCtrl()) {
				break;
			}
			closeDialog();	// else close dialog and leave 
			leave();
			break;
		case SWT.ALT:
			if (handlesAlt()) {
				break;
			}
			closeDialog();	// else close dialog and leave 
			leave();
			break;
		case SWT.PAGE_DOWN:	// leave
		case SWT.PAGE_UP:
		case SWT.ARROW_DOWN:
		case SWT.ARROW_UP:
			leave();
			break;

			// minimal support for in line editing
		case SWT.HOME:
			getMB().toBegin();
			event.doit = false;
			break;

		case SWT.END:
			getMB().toEnd();
			event.doit = false;
			break;

		case SWT.ARROW_LEFT:
			getMB().toLeft();
			event.doit = false;
			break;

		case SWT.ARROW_RIGHT:
			getMB().toRight();			
			event.doit = false;
			break;
		}
	}
	
	/**
	 * Allow children to handle Ctrl+<X> verify key events
	 * Default implementation calls generic dispatch 
	 * 
 	 * @param event
 	 * @return true
	 */
	protected boolean dispatchCtrl(VerifyEvent event) {
		return defaultDispatch(event);
	}
	
	/**
	 * Allow children to handle <TAB>
	 * Default does nothing
	 * 
 	 * @param event
 	 * @return false
	 */
	protected boolean dispatchTab(VerifyEvent event) {
		return false;
	}

	/**
	 * Allow children to handle Alt+<X> verify key events
	 * Default implementation calls generic dispatch 
	 * 
 	 * @param event
 	 * @return true
	 */
	protected boolean dispatchAlt(VerifyEvent event) {
		return defaultDispatch(event);
	}
	
	/**
	 * Allow children to handle Alt+Ctrl+<X> verify key events
	 * Default implementation returns false
	 * 
	 * @param event
	 * @return true if Alt+Ctrl event handled
	 */
	protected boolean dispatchAltCtrl(VerifyEvent event) {
		return false;
	}
	
	/** 
	 * Default handler for Ctrl+<X> and Alt+<X> verify key events
	 * Look for a binding and send it if bound, and leave
	 * 
	 * @param event
	 * @return true (always leave)
	 */
	protected boolean defaultDispatch(VerifyEvent event) {
		// default behavior queue binding and leaves
		ITextEditor ed = editor;
		leave();
		executeBinding(ed,event);
		return true;
	}

	protected boolean hasBinding(KeyEvent event) {
		return hasBinding(event,((event.stateMask & SWT.MODIFIER_MASK) == 0 ? SWT.MOD3 : event.stateMask));
	}
	
	protected boolean hasBinding(KeyEvent event, int mode) {
		boolean result = false;
		// ensure key is upper case
		KeyStroke key = KeyStroke.getInstance(mode, Character.toUpperCase(event.keyCode));
		boolean isFilterDisabled = !getKeyFilter();
		try {
			if (isFilterDisabled) {
				setKeyFilter(true);
			}
			result = bindingService.isPerfectMatch(KeySequence.getInstance(key));
		} finally {
			if (isFilterDisabled) {
				setKeyFilter(false);
			}
		}
		return result;
	}

	/**
	 * Based on the KeyEvent, get the perfect match binding
	 * 
	 * @param event
	 * @param checkEnabled if true, the command in the binding must be enabled
	 * @return a binding that perfectly matches the KeyEvent, or null
	 */
	// TODO explain why always force ALT in normal case?
	private Binding getBinding(KeyEvent event, int mode, boolean checkEnabled) {

		Binding result = null;
		// ensure key is upper case
		KeyStroke key = KeyStroke.getInstance(mode, Character.toUpperCase(event.keyCode));
		boolean isFilterDisabled = !getKeyFilter();
		try {
			if (isFilterDisabled) {
				setKeyFilter(true);
			}
			// Shadowed commands shouldn't be enabled, but they are ... so protect
			// (e.g. C-x will perfect match and be enabled even though shadowed by C-x C-x)
			if (checkEnabled && bindingService.isPartialMatch(KeySequence.getInstance(key))) {
				result = null;
			} else {
				result = bindingService.getPerfectMatch(KeySequence.getInstance(key));
				if (result != null && checkEnabled && !result.getParameterizedCommand().getCommand().isEnabled()) {
					result = null;
				} 
			}
		} finally {
			if (isFilterDisabled) {
				setKeyFilter(false);
			}
		}
		return result;
	}
	
	/**
	 * Execute the binding if possible, else resend the event
	 * 
	 * @param ed
	 * @param event from which to determine the binding
	 * @return true if binding executed 
	 */
	protected boolean executeBinding(ITextEditor ed, VerifyEvent event) {
		return executeBinding(ed,((event.stateMask & SWT.MODIFIER_MASK)== 0 ? SWT.MOD3 : event.stateMask),event);
	}
	
	protected boolean executeBinding(ITextEditor ed, int mode, VerifyEvent event) {
		boolean result = false;
		Binding binding = getBinding(event, mode, true);
		if (binding != null && ed != null) { 
			// inform kbd macro of direct execution by minibuffer
			if (KbdMacroSupport.getInstance().isExecuting(binding)) {
				// when kbd macro is executing, do it now
				callBinding(ed,binding);
			} else {
				// else 'schedule' it for when this command completes
				asyncCallBinding(ed,binding);
			}
			event.doit = false;
			result = true;
		} else {
			// queue event
			resendEvent(event);
		}
		return result;
	}
		
	/**
	 * Add an asynchronous call to execute the command after current command completes
	 */
	protected void asyncCallBinding(final ITextEditor editor, final Binding binding) {
		if (binding != null) {
			PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() {
				public void run() {
					callBinding(editor, binding);
				}
			});
		}
	}
	
	protected void asyncPostEvent(final KeyEvent event) {
		if (event != null) {
			PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() {
				public void run() {
					resendEvent(event);
				}
			});
		}
	}	
	
	/**
	 * @param editor
	 * @param binding
	 * @throws ExecutionException
	 * @throws NotDefinedException
	 * @throws NotEnabledException
	 * @throws NotHandledException
	 */
	private void callBinding(final ITextEditor editor,
			final Binding binding) {
		try {
			IHandlerService service = (IHandlerService) editor.getSite().getService(IHandlerService.class);
			ParameterizedCommand pcommand = binding.getParameterizedCommand();
			service.executeCommand(pcommand, null);
		} catch (Exception e) {
			// Shouldn't happen, but fail quietly
		} 
	}

	protected final String getEol() {
		IDocument document = getDocument();
		if (eol == null) {
			try {
				if (document instanceof IDocumentExtension4) {
					eol = ((IDocumentExtension4)document).getDefaultLineDelimiter();
				} else {
					eol = document.getLineDelimiter(0);
				}

			} catch (BadLocationException e) {
			}
		}
		return eol;
	}

	/**
	 * Synthesize and re-send a sendable event
	 * 
	 * @param event
	 */
	void resendEvent(KeyEvent event) {
		boolean isFilterDisabled = !getKeyFilter();
		try {
			if (isFilterDisabled) {
				setKeyFilter(true);
			}
			Event synthEvent = new Event();
			synthEvent.stateMask = event.stateMask;
			// display.post() wants the key character sans control
			synthEvent.character = (char)event.keyCode;
			synthEvent.keyCode = event.keyCode;
			synthEvent.doit = true;
			synthEvent.type = SWT.KeyDown;
			event.display.post(synthEvent);
		} finally {
			if (isFilterDisabled) {
				setKeyFilter(false);
			}
		}	
	}

	protected boolean getKeyFilter() {
		return (bindingService.isKeyFilterEnabled());
	}
	
	protected void setKeyFilter(boolean val) {
		bindingService.setKeyFilterEnabled(val);		
	}

	protected IFindReplaceTarget getTarget() {
		return (IFindReplaceTarget)getEditor().getAdapter(IFindReplaceTarget.class);
	}

	/* ***************** All the many listeners ***************** */
	
	//FocusListener
	
	/*
	 * @see FocusListener#focusGained(org.eclipse.swt.events.FocusEvent)
	 */
	public void focusGained(FocusEvent e) {
		leave();
	}

	/*
	 * @see FocusListener#focusLost(org.eclipse.swt.events.FocusEvent)
	 */
	public void focusLost(FocusEvent e) {
		leave();
	}
	
	// ISelectionChangedListener
	public void selectionChanged(SelectionChangedEvent event) {
		leave();
	}
	
	// ITextListener
	/*
	 * @see ITextListener#textChanged(TextEvent)
	 */
	public void textChanged(TextEvent event) {
		if (event.getDocumentEvent() != null)
			leave(true);
	}
	
	// MouseListener 
	/*
	 * @see MouseListener##mouseDoubleClick(MouseEvent)
	 */
	public void mouseDoubleClick(MouseEvent e) {
		leave(true);
	}

	/*
	 * @see MouseListener#mouseDown(org.eclipse.swt.events.MouseEvent)
	 */
	public void mouseDown(MouseEvent e) {
		leave(true);
	}

	/*
	 * @see MouseListener#mouseUp(org.eclipse.swt.events.MouseEvent)
	 */
	public void mouseUp(MouseEvent e) {
		leave(true);
	}

	// VerifyKeyListener
	
	/*
	 * @see VerifyKeyListener#verifyKey(VerifyEvent)
	 */
	public void verifyKey(VerifyEvent event) {
		if (!cmdinit) {
			// store the correct minibuffer command on first character as the Eclipse command
			// system has called the ICommandService listener's postExecute (which removed the setting)
			MarkUtils.setCurrentCommand(getCommandId());
			cmdinit = true;
		}
		handleKey(event);
	}
			
	private boolean cmdinit = false;
	
	/* ***************** minibuffer string *******************/
	
	protected int[] getEolChars() {
		return getMB().getEolChars();
	}

	protected void addIt(VerifyEvent event) {
		updateStatusLine(getMB().addChar(event.character));
		event.doit = false;
	}
	
	protected void backSpaceChar(VerifyEvent event) {
		updateStatusLine(getMB().bsChar());
		event.doit = false;
	}
	
	protected void addIt(String addStr) {
		getMB().append(addStr);
	}
	
	protected void deleteChar(VerifyEvent event) {
		updateStatusLine(getMB().delChar());
		event.doit = false;
	}

	protected void initMinibuffer(String newString) {
		updateStatusLine(setMBString(newString));
	}

	protected String getMBString() {
		return getMB().getString();
	}
	
	protected String setMBString(String newString) {
		return getMB().init(newString);
	}
	
	protected String getSearchString() {
		return getMBString();
	}
	
	protected int getMBLength() {
		return getMB().getLength();
	}
	
	protected void setMBLength(int length) {
		getMB().setLength(length);
	}
	
	/* ***************** unfortunate evil *******************/

	// The protected method & private field that gives us the editor viewer for registration purposes
	private static String RE_METHOD_ID = "getSourceViewer"; //$NON-NLS-1$ 
	private static String RE_MEMBER_ID = "fSourceViewer";   //$NON-NLS-1$
	
	// Totally evil code, as Eclipse has no adapter for accessing the viewer
	private ISourceViewer findSourceViewer(ITextEditor editor) {
 		// evil
		ISourceViewer result = null;
		if (editor != null && editor instanceof AbstractTextEditor) {
			result = (ISourceViewer) EmacsPlusUtils.getAM((AbstractTextEditor) editor, RE_METHOD_ID); //$NON-NLS-1$
			if (result == null) {
				// even more evil
				result = (ISourceViewer) EmacsPlusUtils.getAF((AbstractTextEditor) editor, RE_MEMBER_ID); //$NON-NLS-1$
			}
		}
		return result;
	}
}
