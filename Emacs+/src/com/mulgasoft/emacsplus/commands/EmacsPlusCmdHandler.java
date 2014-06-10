/**
 * Copyright (c) 2009, 2010, Mark Feber, MulgaSoft
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 */
package com.mulgasoft.emacsplus.commands;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.Command;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.IHandler;
import org.eclipse.core.commands.common.CommandException;
import org.eclipse.core.commands.common.NotDefinedException;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.FindReplaceDocumentAdapter;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.IRewriteTarget;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.ITextViewerExtension;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ST;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.commands.ICommandService;
import org.eclipse.ui.console.IConsoleView;
import org.eclipse.ui.console.TextConsolePage;
import org.eclipse.ui.console.TextConsoleViewer;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.part.IPage;
import org.eclipse.ui.part.PageBookView;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.texteditor.ITextEditorExtension;
import org.eclipse.ui.texteditor.ITextEditorExtension2;

import com.mulgasoft.emacsplus.EmacsPlusUtils;
import com.mulgasoft.emacsplus.IEmacsPlusCommandDefinitionIds;
import com.mulgasoft.emacsplus.MarkUtils;

/**
 * Base class of all Emacs+ command handlers
 * 
 * @author Mark Feber - initial API and implementation
 */
public abstract class EmacsPlusCmdHandler extends AbstractHandler implements IHandler {

	final static String BAD_LOCATION_ERROR = "Bad_Location_Error";  		  //$NON-NLS-1$
	private final static String INEDITABLE_BUFFER = "Cmd_Buffer_Error"; 	  //$NON-NLS-1$
	
	// word characters terminated by (ignored) non-word or EOL/EOB
	// \p{L}  = \p{Letter}: letter from any language
	// \p{Mn} = \p{Non_Spacing_Mark}: a character intended to be combined with another character without taking up extra space (e.g. accents, umlauts, etc.).
	// \p{Nd} = \p{Decimal_Digit_Number}: a digit zero through nine in any script except ideographic scripts.
	protected static final String IDENT_REGEX= "[\\p{L}[\\p{Mn}[\\p{Nd}]]]";  //$NON-NLS-1$
	protected static final String TOKEN_REGEX= IDENT_REGEX + "++";  		  //$NON-NLS-1$
	protected static final String END_TOKEN_REGEX= "[^\\p{L}\\p{Mn}\\p{Nd}]"; //$NON-NLS-1$
	protected static final String EMPTY_STR = "";   						  //$NON-NLS-1$
	protected static final String SPACE_STR = " ";  						  //$NON-NLS-1$
	protected static final String KOLON = ": "; 							  //$NON-NLS-1$
	protected static final String CR = "\n";								  //$NON-NLS-1$
	
	/** the universal-argument name used by Emacs+ */
	static final String UNIVERSAL = EmacsPlusUtils.UNIVERSAL_ARG;
	/** the shift select argument name used by Emacs+ movement commands*/
	static final String SHIFT_ARG = "shiftArg"; 							  //$NON-NLS-1$
	enum ShiftState {
		CLEAR,	// clear shift state
		SET,	// set the shift state
		NONE;	// no-op
	}

	/** No-op return value from transform method */
	public final static int NO_OFFSET = EmacsPlusUtils.NO_OFFSET;

	String thisExtension;
	private boolean isEditable = true;
	
	/** The ITextEditor in which this command was invoked */
	private ITextEditor editor;
	/** The IDocument on which this command was invoked */
	private IDocument document;
	private IEditorPart lastPart = null;
	
	enum Check { Fail };
	
	/** hold the current universal-argument value */
	private int universalCount = 1;
	/** remember if the universal-argument was present */
	private boolean universalPresent = false;
	// can be set to true to turn on for all (non-Emacs+) commands (which is probably a bad idea)
	private boolean alwaysUniversal = false;

	/** allow editing commands if true - simulates non-buffer local C-x C-q */
	private static boolean forceEditable = false;

	// Abstract edit/etc. transformation method
	/**
	 * Handlers override this method to implement their specific behavior.
	 *  
	 * NO_OFFSET is returned if either the command took care of adjusting the selection/offset
	 * in the body of the method or if the offset has not changed.
	 * 
	 * @param editor the ITextEditor as determined from the Event information
	 * @param document the IDocument as determined from the editor's document provider
	 * @param currentSelection the ITextSelection as determined from the editor's selection provider
	 * @param event the ExecutionEvent from the originating execute method
	 * @return the new offset in the document, or NO_OFFSET
	 * @throws BadLocationException
	 */
	protected abstract int transform(ITextEditor editor, IDocument document, ITextSelection currentSelection, ExecutionEvent event) 
	throws BadLocationException;
	
	/**
	 * Hook for post transformation behavior. Command handlers may override this to do something 
	 * once all normal edit/selection activity has occurred
	 */
	protected void postTransform() {}
	
	/**
	 * Commands that perform simple transformations return true, else (e.g. executable minibuffer commands) false 
	 * @return true for simple transform, else false
	 */
	protected boolean isTransform() {return true;}
	
	/**
	 * Get the current active editor
	 * @param event
	 * @return the current active editor, else null if some other part is active
	 * 
	 * @throws ExecutionException
	 */
	protected IEditorPart getEditor(ExecutionEvent event) throws ExecutionException {

		IEditorPart epart = HandlerUtil.getActiveEditorChecked(event);
		if (HandlerUtil.getActivePart(event) != epart) {
			// When something other than the editor is active (e.g. the JavaStackTraceConsole)
			// Then don't return the editor
			return null;
		}
		return epart;
	}
	
	/**
	 * Get the TextEditor for the associated event
	 * 
	 * @param event
	 * @return the ITextEditor or null
	 * @throws ExecutionException
	 */
	protected ITextEditor getTextEditor(ExecutionEvent event) throws ExecutionException {
		ITextEditor result = null;
		try {
			result = getTextEditor(getEditor(event));
		} catch (ExecutionException e) {
			// HandlerUtil will throw this if it can't find the editor
		}
		return result;
	}
	
	protected ITextEditor getTextEditor(IEditorPart editor) {
		return EmacsPlusUtils.getActiveTextEditor(editor);
	}
	
	/**
	 * Eclipse forces us to check this ourselves
	 * 
	 * @return true if the editor is modifiable
	 */
	private boolean getEditable() {
		boolean result = false;
		ITextEditor editor= getThisEditor();
		if (editor != null) {
			if (editor instanceof ITextEditorExtension2)
				result = ((ITextEditorExtension2) editor).isEditorInputModifiable();
			else if (editor instanceof ITextEditorExtension)
				result = !((ITextEditorExtension) editor).isEditorInputReadOnly();
			else 
				result = editor.isEditable();
		} 
		return result;
	}
	
	/**
	 * Check if this command is being invoked inappropriately in a non-editing context
	 * 
	 * @return true if command should be blocked, else false
	 */
	private boolean isBlocked() {
		return !isEditable() && !(this instanceof INonEditingCommand);
	}

	/**
	 * Is the buffer editable?
	 * 
	 * @return true if editable, else false
	 */
	protected boolean isEditable() {
		return isEditable || forceEditable;
	}
	
	/**
	 * Check if the force buffer editable flag has been set.
	 * 
	 * @return true if the forcing flag is on, else false
	 */
	public boolean isForceEditable() {
		return forceEditable;
	}

	/**
	 * Force the buffer to be editable, regardless of the actual buffer state.
	 * This will allow some commands that would otherwise be blocked.
	 */
	protected void setForceEditable(boolean forceEditable) {
		EmacsPlusCmdHandler.forceEditable = forceEditable;
	}

	/**
	 * If the universal argument has been passed as a parameter to this command then
	 * extract it and set the universal count appropriately.
	 * 
	 * @param event
	 * @return the value of the universal-argument if set, else 1
	 */
	protected int extractUniversalCount(ExecutionEvent event) {
		int result = 1;
		setUniversalPresent(false);
		// Retrieve the universal-argument parameter value if passed 
		String universalArg = event.getParameter(UNIVERSAL);
		if (universalArg != null && universalArg.length() > 0) {
			try {
				result = Integer.parseInt(universalArg);
				setUniversalPresent(true);
			} catch (NumberFormatException e) {	// if invalid number, proceed with count=1
			}
		} 
		setUniversalCount(result);
		return result;
	}
	
	/**
	 * Determine whether it is allowed for the command to be invoked outside the context of a text editor
	 * 
	 * @return true if command can be invoked outside the context of a text editor
	 */
	protected boolean isWindowCommand(){
		return false;
	}

	/**
	 * Determine whether this command is appropriate for a TextConsole
	 * 
	 * @return true if command can be invoked against a TextConsole
	 */
	protected boolean isConsoleCommand(){
		return (this instanceof IConsoleDispatch);
	}
	
	/**
	 * @see org.eclipse.core.commands.AbstractHandler#execute(org.eclipse.core.commands.ExecutionEvent)
	 */
	public Object execute(ExecutionEvent event) throws ExecutionException {
		ITextEditor editor = getTextEditor(event);
		if (editor == null) { 
			if (isWindowCommand()) {
				Object result = checkExecute(event); 
				if (result == Check.Fail) {
					beep();
					result = null;
				}
				return result; 
			} else if (isConsoleCommand()) {
				// intercept and dispatch execution if console supported and used in a console view
				IWorkbenchPart activePart = HandlerUtil.getActivePart(event);
				if (activePart != null && (activePart instanceof IConsoleView) && (activePart instanceof PageBookView)) {
					IPage textPage = ((PageBookView)activePart).getCurrentPage();
					if (textPage instanceof TextConsolePage) {
						return ((IConsoleDispatch)this).consoleDispatch(((TextConsolePage)textPage).getViewer(),(IConsoleView)activePart,event);
					}				
				}
			}
		}
		try {
			setThisEditor(editor);
			isEditable = getEditable();
			if (editor == null || isBlocked()) {
				beep();
				asyncShowMessage(editor, INEDITABLE_BUFFER, true);
				return null;
			}
			
			// Retrieve the universal-argument parameter value if passed 
			int count = extractUniversalCount(event);
			if (count != 1) {
				// check if we should dispatch a related command based on the universal argument
				String dispatchId = checkDispatchId(event.getCommand().getId());
				if (dispatchId != null) {
					// recurse on new id (inverse or arg value driven)
					return dispatchId(editor, dispatchId, event);
				}
			}
			
			setThisDocument(editor.getDocumentProvider().getDocument(editor.getEditorInput()));

			// Get the current selection
			ISelectionProvider selectionProvider = editor.getSelectionProvider();
			ITextSelection selection = (ITextSelection) selectionProvider.getSelection();
			preTransform(selection);
			return transformWithCount(editor,getThisDocument(),selection,event);
			
		} finally {
			// normal commands clean up here
			if (isTransform()) {
				postExecute();
			}
		}
	}

	/**
	 * Add any extra processing once the handler is set up prior to the transform call
	 * For use by sub-classes
	 * @param selection
	 */
	protected void preTransform(ITextSelection selection) {
		// default does nothing
	}
	
	/**
	 * Add any extra processing once the handler has called transform
	 * For use by sub-classes
	 */
	protected void postExecute() {
		postTransform();	 // do any command specific any transformation activities
		setThisEditor(null); // and clean up
		setThisDocument(null);
		thisExtension = null;
		isEditable = true;
		setUniversalCount(1);
	}

	/**
	 * Activate the 'active' text editor from some other view part.
	 * If the original execute was called from outside the context of a text editor
	 * attempt to find and activate the appropriate 'active' editor and dispatch 
	 * the command again in the new context
	 * 
	 * @param event
	 * @return Object
	 */
	protected Object checkExecute(ExecutionEvent event) {
		Object result = null;
		try {
			IWorkbenchPage wpage = this.getWorkbenchPage();
			IEditorPart epart = EmacsPlusUtils.getTextEditor(HandlerUtil.getActiveEditor(event), true);
			// we're in a loop
			if (epart != null && epart == lastPart) {
				epart = null;
				lastPart = null;
			} else {
				lastPart = epart;
			}
			// if we're now looking at a new part, activate it and re-dispatch command
			if (epart != null  && HandlerUtil.getActivePart(event) != epart)  {
				wpage.bringToTop(epart);
				wpage.activate(epart);
				@SuppressWarnings("unchecked")
				Map<String,?> params = (Map<String,?>)event.getParameters();
				result = executeCommand(event.getCommand().getId(), params, null, epart);
				lastPart = null;
			} else {
				// else notify of failure
				result = Check.Fail;
			}
		} catch (ExecutionException e) {
		} catch (CommandException e) {
		}
		return result;
	}

	/**
	 * Call the transform method universal-argument times if it is a looping command, else once.
	 * Wraps it up with an undoer and suppresses redraw as appropriate
	 * 
	 * @param editor
	 * @param document
	 * @param selection
	 * @param event
	 * @return the Object returned by the transform
	 */
	protected Object transformWithCount(ITextEditor editor,IDocument document,ITextSelection selection,ExecutionEvent event) { 

		Control widget = null;
		IRewriteTarget rt = null;
		boolean undoProtect = undoProtect();
		int ucount = Math.abs(getUniversalCount());
		int newOffset = NO_OFFSET;
		if (ucount == 0 && isZero()) {
			// enable commands that support ^U 0 via count
			ucount = 1;
		}
		// Now transform the selection
		try {
			armResult();
			if (undoProtect) {
				rt = (IRewriteTarget) editor.getAdapter(IRewriteTarget.class);
				if (rt != null) {
					rt.beginCompoundChange();
				}
				// use widget to avoid unpleasant scrolling side effects of IRewriteTarget
				widget = getTextWidget(editor);
				setRedraw(widget,false);
			}
			ITextSelection newSelection = selection;
			for (int i = 0; i < ucount; i++) {
				newSelection = getCmdSelection(editor,newSelection);
				if (newSelection != null && checkSelection(newSelection)) {
					try {
						newOffset = transform(editor, document, newSelection, event);
						// if NO_OFFSET then selection hasn't changed or already moved
						//				if (!(newOffset == NO_OFFSET)){
						if (!(newOffset < 0)) {
							// 	Set the selection to the end of the modified text
							newSelection= new TextSelection(document, newOffset, 0);
							setSelection(editor, newSelection);
						}
					} catch (BadLocationException e) {
						newOffset = NO_OFFSET;
						beep();
						break;
					} 
					if (!isLooping()) {
						break;
					}
				}
			}
		} catch (Exception e) {
			// allow generic break out of count iteration
			newOffset = NO_OFFSET;
		} finally {
			if (undoProtect) {
				setRedraw(widget, true);
				if (rt != null) {
					rt.endCompoundChange();
				}
			}
		}
		// if the command set the result specifically, use that, 
		// otherwise just the offset position (which may be NO_OFFSET)
		return (hasResult() ? getCmdResult() : new Integer(newOffset));
	}

	private Object cmdResult = null;
	private boolean hasResult = false;
	
	/**
	 * Set the return value of this command
	 * 
	 * @param result the Object to be returned from this call
	 */
	protected void setCmdResult(Object result) {
		cmdResult = result;
		hasResult = true;
	}
	
	/**
	 * reset the command result state 
	 */
	private void armResult() {
		cmdResult = null;
		hasResult = false;
	}
	
	/**
	 * What the command result set?
	 * @return true if set, else false
	 */
	private boolean hasResult() {
		return hasResult;
	}
	
	/**
	 * Get the command result
	 * @return the command result
	 */
	private Object getCmdResult() {
		return cmdResult;
	}
	
	/**
	 * If no text is selected, see if the handler wants to get one
	 * 
	 * @param editor
	 * @param selection
	 * @return new selection, if handler modified it, else selection
	 * @throws ExecutionException 
	 */
	protected ITextSelection getCmdSelection(ITextEditor editor,ITextSelection selection) throws ExecutionException {
		ITextSelection 	newSelection = selection;
		if (selection.getLength() <= 0) {
			newSelection = getNewSelection(getThisDocument(), selection);
			if (newSelection != null && newSelection != selection) {
				selection = newSelection;
				setSelection(editor,newSelection);
			} else {
				newSelection = selection;
			}
		}
		return newSelection;
	}
	
	protected void selectAndReveal(ITextEditor editor, int offset, int end) {
		Control widget = getTextWidget(editor);
		try {
			setRedraw(widget,false);
			// editor.selectAndReveal(mark, offset-mark);
			MarkUtils.setSelection(editor, end, offset-end);
			// and reveal to cursor if necessary
			MarkUtils.revealRange(editor,offset,0);
		} finally {
			setRedraw(widget,true);
		}
	}

	protected Control getTextWidget(ITextEditor editor) {
		return MarkUtils.getTextWidget(editor);
	}
	
	protected Point getWidgetSelection(ITextEditor editor) {
		return MarkUtils.getWidgetSelection(editor);
	}
	
	/**
	 * Get the editor's line delimiter
	 * 
	 * @return delimiter as String
	 */
	protected String getLineDelimiter() {
		return MarkUtils.getWidgetLineDelimiter(getThisEditor()); 
	}

	protected boolean isLineDelimiter(int offset) {
		return isLineDelimiter(offset,getThisDocument());
	}
	
	// This is based on code in StyledText - so setting the caret offset
	// does not throw an exception.
	private static boolean isLineDelimiter(int offset,IDocument document) {
		
		boolean result = false;
		IRegion reg;
		try {
			reg = document.getLineInformationOfOffset(offset);
		int lineOffset = reg.getOffset();
		int offsetInLine = offset - lineOffset;
		// offsetInLine will be greater than line length if the line 
		// delimiter is longer than one character and the offset is set
		// in between parts of the line delimiter.
		result =  offsetInLine >= reg.getLength();
		} catch (BadLocationException e) {
		}
		return result;
	}

	/**
	 * On paste, Eclipse StyledText converts EOLs to the buffer local value
	 * Provide the same feature for yank
	 *  
	 * @param text
	 * @return text with line delimiters converted to buffer local value
	 */
	protected String convertDelimiters(String text) {
		String result = text;
		int len;
		if (text != null && (len = text.length()) > 0) {
			String eol = getLineDelimiter();
			StringBuilder dest = new StringBuilder(len);
			boolean atEol = false;
			for (int i = 0; i < len; i++) {
				char c = text.charAt(i);
				if (c == SWT.CR) {
					if (atEol) {
						dest.append(eol);						
					}
					atEol = true;
					continue;
				} else if (c == SWT.LF) {
					atEol = false;
					dest.append(eol);
					continue;
				}
				if (atEol) {
					atEol = false;
					dest.append(eol);
				}
				dest.append(c);
			}
			if (atEol) {
				dest.append(eol);
			}
			result = dest.toString();
		}
		return result;
	}
	
	protected int insertText(IDocument document, ITextSelection selection, String text) throws BadLocationException {
		int result = 0;
		String iText = convertDelimiters(text);
		if (iText != null){
			result = iText.length(); 
			updateText(document, selection, iText);
		}
		return result;
	}
	
	ITextEditor getThisEditor() {
		return editor;
	}
	
	void setThisEditor(ITextEditor editor) {
		this.editor = editor;
	}
	
	protected IDocument getThisDocument() {
		return document;
	}
	
	protected void setThisDocument(IDocument document) {
		this.document = document;
	}

	protected IDocument getThisDocument(ITextEditor editor) {
		IDocument doc = getThisDocument();
		if (doc == null && editor != null) {
			doc = editor.getDocumentProvider().getDocument(editor.getEditorInput());			
		}
		return doc;
	}

	protected String getCurrentExtension() {
		if (thisExtension == null && getThisEditor() != null) {
			String name = getThisEditor().getTitle();
			if (name != null) {
				int pos = name.lastIndexOf('.') + 1;
				if (pos > 0) {
					thisExtension = name.substring(pos);
				}
			}
		}
		return thisExtension;
	}

	private String[] xmlExtensions = {"xml", "mxml" }; //$NON-NLS-1$ //$NON-NLS-2$
	
	protected boolean isTypeXml(){
		boolean result = false;
		String ext = getCurrentExtension(); 
		if (ext != null) {
			for (int i = 0; i < xmlExtensions.length; i++) {
				if (xmlExtensions[i].equalsIgnoreCase(ext)) {
					result = true;
					break;
				}
			}
		}
		return result;
	}

	protected boolean checkSelection(ITextSelection selection){
		return true;
	}
	
	/**
	 * Return the editor's current selection
	 * Never returns null
	 * 
	 * @param editor
	 * @return the current selection or (0,0)
	 */
	protected ITextSelection getCurrentSelection(ITextEditor editor){
		ISelectionProvider sp = editor.getSelectionProvider();
		// never return null
		return (sp != null ? (ITextSelection) sp.getSelection() : new TextSelection(0,0));
	}

	/**
	 * Potentially get a new selection based on the current selection
	 * The default implementation just returns the supplied selection
	 * 
	 * @param document
	 * @param selection
	 * @return a selection 
	 * @throws ExecutionException 
	 */
	protected ITextSelection getNewSelection(IDocument document, ITextSelection selection) throws ExecutionException{
		return selection;
	}
	
	/**
	 * Determine the explicit or implicit (point/mark) selection
	 * 
	 * @param editor
	 * @param selection
	 * @return the selection or null
	 */
	protected ITextSelection getImpliedSelection(ITextEditor editor, ITextSelection selection){
		// if selection length is 0, then if mark and mark != point, set and return as selection
		ITextSelection result = selection;
		if (selection.getLength() == 0) {
			int mark = getMark(editor);
			int coff = selection.getOffset();
			if (isMarkSet(mark) && mark != coff) {
				setSelection(editor,coff, mark - coff);
				result = getCurrentSelection(editor);
			} else {
				result = null;
			}
		}
		return result;
	}
	
	protected void setSelection(ITextEditor editor,ITextSelection selection){
		MarkUtils.setSelection(editor, selection);
	}
	
	protected void setSelection(ITextEditor editor,int offset, int length){
		MarkUtils.setSelection(editor, offset, length);
	}
	
	protected void updateText(IDocument document,ITextSelection selection,String newText) throws BadLocationException{
		updateText(document, selection.getOffset(), selection.getLength(),newText);
	}
	
	protected void updateText(IDocument document,int offset, int length, String newText) throws BadLocationException{
		// if the text doesn't end with an eol, then it can provoke a bug(?) in java.text.RuleBasedBreakIterator.checkOffset()
		// where it throws an exception if offset >= endindex [s/b? offset > endindex] 
		if (offset == document.getLength()) {
			String eol = getLineDelimiter();
			if (!newText.endsWith(eol)){
				newText += eol;
			}
		}
		document.replace(offset,length,newText);
	}
	
	/**
	 * @see com.mulgasoft.emacsplus.commands.EmacsPlusCmdHandler#getNextSelection(IDocument,ITextSelection,boolean,String)
	 *
	 * @param document
	 * @param selection
	 */
	protected ITextSelection getNextSelection(IDocument document, ITextSelection selection) {
		return getNextSelection(document, selection, true, TOKEN_REGEX);
	}
	
	/**
	 * @see com.mulgasoft.emacsplus.commands.EmacsPlusCmdHandler#getNextSelection(IDocument,ITextSelection,boolean,String)
	 *
	 * @param document
	 * @param selection
	 */
	protected ITextSelection getPrevSelection(IDocument document, ITextSelection selection) {
		return getNextSelection(document, selection, false, TOKEN_REGEX);
	}
	
	/**
	 * Find the next token in the document and return it as a selection

	 * @param document
	 * @param selection
	 * @param forward
	 * @param regExp
	 * @return the newly selected text
	 */
	protected ITextSelection getNextSelection(IDocument document, ITextSelection selection, boolean forward, String regExp) {
		ITextSelection result = null;
		FindReplaceDocumentAdapter fda =  new FindReplaceDocumentAdapter(document);
		try {
			IRegion reg = fda.find(selection.getOffset(), regExp, forward, false, false, true);
			if (reg != null) {
				result = new TextSelection(document, reg.getOffset(), reg.getLength());
			}
		} catch (BadLocationException e) {}
		return result;
	}
	
	/**
	 * Invoke the specified command using the handler service
	 * 
	 * @param commandId
	 * @param event
	 * @param editor
	 * @return execution result
	 * @throws CommandException 
	 */
	protected Object executeCommand(String commandId,Event event, IEditorPart editor) throws ExecutionException, CommandException {
		return EmacsPlusUtils.executeCommand(commandId, event, editor);
	}
	
	/**
	 * Invoke the specified command using the generic handler service
	 * 
	 * @param commandId
	 * @param event
	 * @param editor
	 * @return execution result
	 * @throws CommandException 
	 */
	protected Object executeCommand(String commandId,Event event) throws ExecutionException, CommandException {
		return EmacsPlusUtils.executeCommand(commandId, event);
	}
	
	/**
	 * Invoke the specified parameterized command using the handler service
	 * @param commandId
	 * @param params
	 * @param event
	 * @param editor
	 * @return execution result
	 * @throws ExecutionException
	 * @throws CommandException
	 */
	protected Object executeCommand(String commandId, Map<String,?> params, Event event, IEditorPart editor) throws ExecutionException, CommandException {
		return EmacsPlusUtils.executeCommand(commandId, params, event, editor);
	}
	
	/**
	 * Get the active editor of the page
	 * Dereference multi-part editor 
	 * @param page
	 * @return the active editor or null
	 */
	protected IEditorPart getActiveEditor(IWorkbenchPage page) {
		IEditorPart result = page.getActiveEditor();
		IEditorPart part = (IEditorPart) result.getAdapter(ITextEditor.class);
		if (part != null) {
			result = part;
		}
		return result;
	}
	
	protected int getCursorOffset(){
		return getCursorOffset(getThisEditor());
	}
	
	/**
	 * Get the absolute cursor offset in model coords
	 * @see com.mulgasoft.emacsplus.MarkUtils#getCursorOffset(ITextEditor)  
	 * 
	 * @return the offset
	 */
	protected int getCursorOffset(ITextEditor editor){
		return getCursorOffset(editor,null);
	}

	/**
	 * Get the absolute cursor offset in model coords
	 * If the selection length is 0, use the selection offset, else get from the widget 
	 * 
	 * @return the offset
	 */
	protected int getCursorOffset(ITextEditor editor, ITextSelection currentSelection){
		return (currentSelection != null && currentSelection.getLength() ==  0 ? currentSelection.getOffset() : MarkUtils.getCursorOffset(editor));
	}
	
	/**
	 * Set the cursor offset using the editors selection provider
	 * 
	 * @param editor
	 * @param offset in model coords
	 */
	protected void setCursorOffset(ITextEditor editor,int offset){
		MarkUtils.setCursorOffset(editor,offset);
	}
	
	public static boolean isChanged = false;
	
	/** if true, the mark is enabled (even if the selection is zero length) */
	private static boolean flagMark = false;
	
	/**
	 * @return the true if mark has been 
	 */
	protected static boolean isFlagMark() {
		return flagMark;
	}

	/**
	 * @param flagMark the flagMark to set
	 */
	public static void setFlagMark(boolean flagMark) {
		EmacsPlusCmdHandler.flagMark = flagMark;
	}

	/** 
	 * Check if mark offset is at either end of the region
	 * 
	 * @param mark -mark offset
	 * @param off - region offset
	 * @param len - region length
	 * @return true if mark offset is at either end of the region
	 */
	protected boolean checkMark(int mark, int off, int len){
		return (mark == off || mark == off+len); 
	}

	/**
	 * Check if editor's mark offset is at either end of the region
	 * 
	 * @param editor
	 * @param off - region offset
	 * @param len - region length
	 * @return true if mark offset is at either end of the region
	 */
	protected boolean checkMark(ITextEditor editor, int off, int len){
		return checkMark(getMark(editor),off,len);
	}
	
	/**
	 * @see com.mulgasoft.emacsplus.MarkUtils#setMark(ITextEditor)
	 */
	protected int setMark(ITextEditor editor){
		return MarkUtils.setMark(editor);
	}
	
	/**
	 * Set the mark at offset and save previous mark in Mark Rings
	 * 
	 * @param editor
	 * @param offset in model coords
	 * @return  the mark position in model coords
	 */
	protected int setMark(ITextEditor editor, int offset){
		return setMark(editor,offset,true);
	}
	
	/**
	 * Set the mark at offset
	 * 
	 * @param editor
	 * @param offset in model coords
	 * @param save if true, save previous mark in the Mark Rings
	 * 
	 * @return  the mark position in model coords
	 */
	protected int setMark(ITextEditor editor, int offset, boolean save){
		// hack optimization so we don't have to keep getting the viewer
		if (lastEditor != editor) {
			lastEditor = editor;
			ve = MarkUtils.getITextViewer(editor);
		}
		return MarkUtils.setMark(editor, ve, offset, save);
	}
	
	/**
	 * Check if the mark has been set in the buffer
	 * 
	 * @param editor
	 * @return true if mark != -1
	 */
	protected boolean isMarkSet(ITextEditor editor) {
		return isMarkSet(getMark(editor));
	}
	
	/**
	 * Check if the mark has been set in the buffer
	 * 
	 * @param mark the value of the mark offset
	 * @return true if mark != -1
	 */
	protected boolean isMarkSet(int mark) {
		return mark != -1;
	}

	/**
	 * Check that all conditions for mark enablement are met
	 * 
	 * @param editor
	 * @param selection (typically currentSelection)
	 * 
	 * @return true if mark is set in the context of the selection
	 */
	protected boolean isMarkEnabled(ITextEditor editor, ITextSelection selection) {
		int mark = getMark(editor);	// current mark position
		int len = selection.getLength();
		int off = selection.getOffset();
		// if mark set and in a marked region, or mark has just been set
		return (isMarkSet(mark) && checkMark(mark,off,len) && (len > 0 || isFlagMark()));
	}
	
	/**
	 * Check that all conditions for mark enablement are met on a TextConsole
	 * Support single mark only
	 *  
	 * @param viewer
	 * @param selection
	 * @return true if mark is set in the context of the selection
	 */
	protected boolean isMarkEnabled(TextConsoleViewer viewer, ITextSelection selection) {
		int mark = viewer.getMark();	// current mark position
		int len = selection.getLength();
		int off = selection.getOffset();
		// if mark set and in a marked region, or mark has just been set
		return (isMarkSet(mark) && checkMark(mark,off,len)); 
	}
	
	private ITextEditor lastEditor;
	private ITextViewerExtension ve;
	
	/**
	 * Get the current Mark position
	 * 
	 * @param editor
	 * @return the Mark position in model coords (-1 if not set)
	 */
	protected int getMark(ITextEditor editor) {
		// hack optimization so we don't have to keep getting the viewer
		int result = -1;
		if (lastEditor != editor) {
			lastEditor = editor;
			ve = MarkUtils.getITextViewer(editor);
		}
		if (ve != null) {
			result = ve.getMark();
		}
		return result;
	}
	
	protected int getMark() {
		return getMark(getThisEditor());
	}
	
	protected void beep() {
		EmacsPlusUtils.beep();
	}
	
	protected IWorkbenchPage getWorkbenchPage() {
		return EmacsPlusUtils.getWorkbenchPage();
	}

	/**
	 * Add an asynchronous call to show the message
	 */
	protected void asyncShowMessage(final IWorkbenchPart wpart, final String message, final boolean error) {
		PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() {
			public void run() {
				EmacsPlusUtils.showMessage(wpart, message, error);
			}});
	}

	// **************** Universal arg command processing methods ****************
	
	/**
	 * Return the inverse operation for the specified command id.
	 * The inverse operation, if present, is used when a negative universal argument is detected.
	 *  
	 * @param id
	 * @return the inverse of the command id or null
	 */
	protected String getInverseId(String id) {
		// is it implemented by a non-Emacs+ command?
		String result = InverseUniversalCmds.get(id); 
		// if not, check the Emacs+ commands
		return (result != null ? result : MarkUtils.getInverseId(id));
	}

	/**
	 * Is this the ids of standard Eclipse commands that we adapt to calling with the C-u argument 
	 * @param id
	 * @return true if we want to provide support for C-u, else false
	 */
	private boolean isUniversalCmd(String id) {
		return UniversalCmds.get(id) != null;
	}
	
	/**
	 * Get the value of the universal-argument; defaults to 1 if not supplied by command invocation.
	 * 
	 * @return the universalCount
	 */
	public int getUniversalCount() {
		return universalCount;
	}
	
	/**
	 * Set the value of the universal-argument
	 * 
	 * @param universalCount the universalCount to set
	 */
	protected void setUniversalCount(int universalCount) {
		this.universalCount = universalCount;
	}
	
	/**
	 * Return true if the command has been invoked with a universal-argument, else false
	 * 
	 * @return true if universal-argument supplied, else false
	 */
	public boolean isUniversalPresent() {
		return universalPresent;
	}
	
	/**
	 * Set that the command has been invoked with a universal-argument
	 * 
	 * @param universalPresent the universalPresent to set
	 */
	private void setUniversalPresent(boolean universalPresent) {
		this.universalPresent = universalPresent;
	}

	/**
	 * Commands that need to specify arg-value based processing should override this
	 * 
	 * @return true is command should loop 
	 */
	protected boolean isLooping() {
		// by default, all commands are looping.
		// also filtered by presence of universalArg
		// individual commands can override
		return true;
	}
	
	/**
	 * Commands that need to specify ^U 0 processing should override this
	 * 
	 * @return true is command can be called with 0 universal arg 
	 */
	protected boolean isZero() {
		// by default, no commands support 0 directly.
		return false;
	}
	
	/**
	 * If the universal-argument is > 1 and the command supports looping, then return true.
	 * 
	 * @return true if we should wrap in an undo protect, else false
	 */
	protected boolean undoProtect() {
		return getUniversalCount() != 1 && isLooping();
	}
	
	/**
	 * If the Command accepts the universal-argument, then invoke it appropriately
	 * otherwise simply execute it with no arguments.
	 * 
	 * To accept the universal-argument, the Command must either:
	 *   1) Have a parameter named "universalArg" [for Emacs+ commands] - or -
	 *   2) Be present in the universal command hash table [for pre-existing commands]
	 *   
	 * @param editor
	 * @param count
	 * @param cmd
	 * @param isNumeric true if argument was entered as a number (rather than plain ^Us)
	 *         - which we don't handle yet
	 * @throws NotDefinedException
	 * @throws ExecutionException
	 * @throws CommandException
	 */
	void executeUniversal(ITextEditor editor, Command cmd, Event event, int count, boolean isNumeric)
	throws NotDefinedException,	ExecutionException, CommandException {
		String id = cmd.getId();
		String did = null;
		// pass universal arg if non-default and cmd accepts it
		if (cmd.getParameter(UNIVERSAL) != null) {
			Map<String, String> args = new HashMap<String, String>();
			args.put(UNIVERSAL, Integer.toString(count));
			executeCommand(id, args, event, editor);
		} else if ((did = getInternalCmd(id)) != null) {
			// Emacs+ internal commands should support +- universal-argument
			Map<String, String> args = new HashMap<String, String>();
			args.put(UNIVERSAL, Integer.toString(count));
			executeCommand(did, args, event, editor);
		} else if (count != 1 && (isUniversalCmd(id) || (alwaysUniversal && !id.startsWith(EmacsPlusUtils.MULGASOFT)))) {
			// only non-Emacs+ commands should be invoked here
			executeWithDispatch(editor, getUniversalCmd(id), count);
		} else {
			executeCommand(id, event, editor);
		}
	}

	private String getInternalCmd(String id) {
		return InternalCmds.get(id);
	}
	
	/**
	 * Check for a dispatch command before invoking with count
	 * For a command that has already been determined to support the universal-argument
	 * 
	 * @param editor
	 * @param id
	 * @param count
	 */
	protected void executeWithDispatch(ITextEditor editor, String id, int count) {
		try {
			// set for dispatch check
			setUniversalCount(count);
			// check if we should dispatch a related command based on the universal argument
			String dispatchId = checkDispatchId(id);
			executeWithCount(editor, getThisDocument(editor), (dispatchId != null ? dispatchId : id), Math.abs(count));
		} finally {
			// restore count to default
			setUniversalCount(1);
		}
	}
	
	/**
	 * Execute the command count times
	 * Wraps it up with an undoer and suppresses redraw as appropriate
	 * 
	 * @param editor
	 * @param document
	 * @param id
	 * @param count
	 * @return the result of executing the command
	 */
 	private Object executeWithCount(ITextEditor editor, IDocument document, String id, int count) {
		Object result = null;
		IRewriteTarget rt = null;		
		Control widget = null;
		// Now execute the command - count will always be positive at this point
		try {
			if (count > 1) {
				rt = (IRewriteTarget) editor.getAdapter(IRewriteTarget.class);
				if (rt != null) {
					rt.beginCompoundChange();
				}
				// use widget to avoid unpleasant scrolling side effects of IRewriteTarget
				widget = getTextWidget(editor);
				setRedraw(widget,false);
			}
			for (int i = 0; i < count; i++) {
				try {
					result = executeCommand(id, null, editor);
				} catch (ExecutionException e) {
					break;
				} catch (CommandException e) {
					break;
				} 
			}
		} finally {
			setRedraw(widget,true);
			if (rt != null) {
				rt.endCompoundChange();
			}
		}
		return result;
	}
	
 	protected void setRedraw(Control c, boolean redraw) {
 		if (c != null) {
 			c.setRedraw(redraw);
 		}
 	}
 	
	/**
	 * Execute the command id (with universal-argument, if appropriate)
	 * 
	 * @param id
	 * @param event
	 * @return execution result
	 */
	private Object dispatchId(ITextEditor editor, String id, ExecutionEvent event) {
		Object result = null;
		if (id != null) {
			ICommandService ics = (ICommandService) editor.getSite().getService(ICommandService.class);
			if (ics != null) {
				Command command = ics.getCommand(id);
				if (command != null) {
					try {
						// check if the dispatch command also takes the parameter
						if (command.getParameter(UNIVERSAL) != null) {
							@SuppressWarnings("unchecked")
							Map<String,Object> params = (Map<String,Object>)event.getParameters();
							if (params == java.util.Collections.EMPTY_MAP) {
								params = new HashMap<String, Object>();
							}
							params.put(UNIVERSAL, Integer.toString(getUniversalCount()));
							result = (executeCommand(id, params, null, getThisEditor()));
						} else {
							result = executeCommand(id, null, getThisEditor());
						}
					} catch (CommandException e) {}
				}
			}
		}
		return result;
	}
	
	/**
	 * Emacs+ commands that have an arg value based dispatch should 
	 * override this method
	 *  
	 * @param checkId
	 * @param arg
	 * @return the arg value dispatch id, or null
	 */
	protected String getDispatchId(String checkId, int arg) {
		String result = null;
		if (arg == 0) {
			result = ZeroCmds.get(checkId);
		}
		return result;
	}

	/**
	 * Look to see if there is a different id that should be dispatched based on the
	 * universal-argument
	 * 
	 * This can occur if:
	 * 1) The universal-argument is negative, and there is an inverse operation
	 * 2) If not, check to see if there is a ^U key id (e.g. for command X, it
	 *    would return X1 if ^U, X2 if ^U ^U, etc).
	 *    
	 * @param checkId
	 * @return a dispatch id or null
	 */
	protected String checkDispatchId(String checkId) {
		String result = null;
		String id = null;
		int count = getUniversalCount(); 
		// if arg is negative, see if there is an inverse operation to call
		if (count < 0) {
			// check for dispatch based on count inversion
			if ((id = getInverseId(checkId)) != null) {
				setUniversalCount(-getUniversalCount());
				result = id;
			}
		}
		if (id == null && count != 1) {
			// if arg is not 1 (i.e. 0 or greater than 1) and no id,  
			// check for dispatch id based on argument value
			// handles the non-looping ^U command style invocation
			id = getDispatchId(checkId, count);
			if (id != null && !id.equals(checkId)) {
				result = id;
			}
		}
		return result;
	}

	private String getUniversalCmd(String id) {
		return UniversalCmds.get(id);
	}
	
	/**
	 * Relates command id to action in StyledText widget 
	 */
	@SuppressWarnings("serial")
	protected static final HashMap<String,Integer> dispatchCmdIds = new HashMap<String,Integer>() {
		{
			// Eclipse ids
			put(IEmacsPlusCommandDefinitionIds.EMP_SELECT_ALL, ST.SELECT_ALL);
			put(IEmacsPlusCommandDefinitionIds.LINE_UP, ST.LINE_UP);
			put(IEmacsPlusCommandDefinitionIds.LINE_DOWN,ST.LINE_DOWN);
			put(IEmacsPlusCommandDefinitionIds.LINE_START,ST.LINE_START);
			put(IEmacsPlusCommandDefinitionIds.LINE_END,ST.LINE_END);
			put(IEmacsPlusCommandDefinitionIds.COLUMN_PREVIOUS,ST.COLUMN_PREVIOUS);
			put(IEmacsPlusCommandDefinitionIds.COLUMN_NEXT,ST.COLUMN_NEXT);
			put(IEmacsPlusCommandDefinitionIds.WORD_PREVIOUS,ST.WORD_PREVIOUS);
			put(IEmacsPlusCommandDefinitionIds.WORD_NEXT,ST.WORD_NEXT);
			put(IEmacsPlusCommandDefinitionIds.TEXT_START,ST.TEXT_START);
			put(IEmacsPlusCommandDefinitionIds.TEXT_END,ST.TEXT_END);
			put(IEmacsPlusCommandDefinitionIds.WINDOW_START,ST.WINDOW_START);
			put(IEmacsPlusCommandDefinitionIds.WINDOW_END,ST.WINDOW_END);
			// Emacs+ ids
			put(IEmacsPlusCommandDefinitionIds.SCROLL_DOWN,ST.PAGE_UP);
			put(IEmacsPlusCommandDefinitionIds.SCROLL_UP,ST.PAGE_DOWN);
			put(IEmacsPlusCommandDefinitionIds.PREVIOUS_LINE, ST.LINE_UP);
			put(IEmacsPlusCommandDefinitionIds.NEXT_LINE,ST.LINE_DOWN);
			put(IEmacsPlusCommandDefinitionIds.BEGIN_LINE,ST.LINE_START);
			put(IEmacsPlusCommandDefinitionIds.END_LINE,ST.LINE_END);
			put(IEmacsPlusCommandDefinitionIds.BACKWARD_CHAR,ST.COLUMN_PREVIOUS);
			put(IEmacsPlusCommandDefinitionIds.FORWARD_CHAR,ST.COLUMN_NEXT);

			put(IEmacsPlusCommandDefinitionIds.BACKWARD_WORD,ST.WORD_PREVIOUS);
			put(IEmacsPlusCommandDefinitionIds.FORWARD_WORD,ST.WORD_NEXT);
			put(IEmacsPlusCommandDefinitionIds.BEGIN_BUFFER,ST.TEXT_START);
			put(IEmacsPlusCommandDefinitionIds.END_BUFFER,ST.TEXT_END);
			put(IEmacsPlusCommandDefinitionIds.DELETE_NEXT,ST.DELETE_NEXT);
			put(IEmacsPlusCommandDefinitionIds.DELETE_NEXT_WORD,ST.DELETE_WORD_NEXT);
			put(IEmacsPlusCommandDefinitionIds.DELETE_PREVIOUS_WORD,ST.DELETE_WORD_PREVIOUS);
			put(IEmacsPlusCommandDefinitionIds.EMP_CUT,ST.CUT);
		}
	};
	
	/**
	 * Relates command id to action in StyledText widget 
	 */
	@SuppressWarnings("serial")
	protected static final HashMap<String,Integer> dispatchSelectIds = new HashMap<String,Integer>() {
		{
			// Eclipse ids
			put(IEmacsPlusCommandDefinitionIds.EMP_SELECT_ALL, ST.SELECT_ALL);
			put(IEmacsPlusCommandDefinitionIds.LINE_UP, ST.SELECT_LINE_UP);
			put(IEmacsPlusCommandDefinitionIds.LINE_DOWN,ST.SELECT_LINE_DOWN);
			put(IEmacsPlusCommandDefinitionIds.LINE_START,ST.SELECT_LINE_START);
			put(IEmacsPlusCommandDefinitionIds.LINE_END,ST.SELECT_LINE_END);
			put(IEmacsPlusCommandDefinitionIds.COLUMN_PREVIOUS,ST.SELECT_COLUMN_PREVIOUS);
			put(IEmacsPlusCommandDefinitionIds.COLUMN_NEXT,ST.SELECT_COLUMN_NEXT);
			put(IEmacsPlusCommandDefinitionIds.PAGE_UP,ST.SELECT_PAGE_UP);
			put(IEmacsPlusCommandDefinitionIds.PAGE_DOWN,ST.SELECT_PAGE_DOWN);
			put(IEmacsPlusCommandDefinitionIds.WORD_PREVIOUS,ST.SELECT_WORD_PREVIOUS);
			put(IEmacsPlusCommandDefinitionIds.WORD_NEXT,ST.SELECT_WORD_NEXT);
			put(IEmacsPlusCommandDefinitionIds.TEXT_START,ST.SELECT_TEXT_START);
			put(IEmacsPlusCommandDefinitionIds.TEXT_END,ST.SELECT_TEXT_END);
			put(IEmacsPlusCommandDefinitionIds.WINDOW_START,ST.SELECT_WINDOW_START);
			put(IEmacsPlusCommandDefinitionIds.WINDOW_END,ST.SELECT_WINDOW_END);
			// Emacs+ ids			
			put(IEmacsPlusCommandDefinitionIds.PREVIOUS_LINE, ST.SELECT_LINE_UP);
			put(IEmacsPlusCommandDefinitionIds.NEXT_LINE,ST.SELECT_LINE_DOWN);
			put(IEmacsPlusCommandDefinitionIds.BEGIN_LINE,ST.SELECT_LINE_START);
			put(IEmacsPlusCommandDefinitionIds.END_LINE,ST.SELECT_LINE_END);
			put(IEmacsPlusCommandDefinitionIds.BACKWARD_CHAR,ST.SELECT_COLUMN_PREVIOUS);
			put(IEmacsPlusCommandDefinitionIds.FORWARD_CHAR,ST.SELECT_COLUMN_NEXT);
			put(IEmacsPlusCommandDefinitionIds.BACKWARD_WORD,ST.SELECT_WORD_PREVIOUS);
			put(IEmacsPlusCommandDefinitionIds.FORWARD_WORD,ST.SELECT_WORD_NEXT);
			put(IEmacsPlusCommandDefinitionIds.BEGIN_BUFFER,ST.SELECT_TEXT_START);
			put(IEmacsPlusCommandDefinitionIds.END_BUFFER,ST.SELECT_TEXT_END);
		}
	};

	// Some commands do something different on 0 argument
	@SuppressWarnings("serial")
	protected static final HashMap<String,String> ZeroCmds = new HashMap<String,String>() {
		{
			// Eclipse ids
			put(IEmacsPlusCommandDefinitionIds.CUT_LINE_TO_END, IEmacsPlusCommandDefinitionIds.CUT_LINE_TO_BEGINNING);
		}
	};

	// Override kill behavior on ^U
	@SuppressWarnings("serial")	
	private static final HashMap<String,String> InternalCmds = new HashMap<String,String>() {
		{
			// translate to internal command with appropriate behavior
			put(IEmacsPlusCommandDefinitionIds.CUT_LINE_TO_END, IEmacsPlusCommandDefinitionIds.KILL_LINE);
			// translate to internal command with appropriate behavior
			put(IEmacsPlusCommandDefinitionIds.CUT_LINE_TO_BEGINNING, IEmacsPlusCommandDefinitionIds.BACKWARD_KILL_LINE);
		}
	};
	
	// These are the ids of standard Eclipse commands that we adapt to calling with the C-u argument 
	@SuppressWarnings("serial")
	private static final HashMap<String,String> UniversalCmds = new HashMap<String,String>() {
		{
			put(IEmacsPlusCommandDefinitionIds.DELETE_NEXT, IEmacsPlusCommandDefinitionIds.DELETE_NEXT);
			put(IEmacsPlusCommandDefinitionIds.DELETE_PREVIOUS, IEmacsPlusCommandDefinitionIds.DELETE_PREVIOUS);
			put(IEmacsPlusCommandDefinitionIds.DELETE_NEXT_WORD,IEmacsPlusCommandDefinitionIds.DELETE_NEXT_WORD);
			put(IEmacsPlusCommandDefinitionIds.DELETE_PREVIOUS_WORD,IEmacsPlusCommandDefinitionIds.DELETE_PREVIOUS_WORD);
			put(IEmacsPlusCommandDefinitionIds.SMART_ENTER_INVERSE, IEmacsPlusCommandDefinitionIds.SMART_ENTER_INVERSE);
			put(IEmacsPlusCommandDefinitionIds.SMART_ENTER, IEmacsPlusCommandDefinitionIds.SMART_ENTER);
			
			put(IEmacsPlusCommandDefinitionIds.EMP_DELETE, IEmacsPlusCommandDefinitionIds.EMP_DELETE);
			put(IEmacsPlusCommandDefinitionIds.DELETE_NEXT, IEmacsPlusCommandDefinitionIds.DELETE_NEXT);
			put(IEmacsPlusCommandDefinitionIds.DELETE_PREVIOUS, IEmacsPlusCommandDefinitionIds.DELETE_PREVIOUS);
			
			put(IEmacsPlusCommandDefinitionIds.DELETE_LINE, IEmacsPlusCommandDefinitionIds.DELETE_LINE);
			put(IEmacsPlusCommandDefinitionIds.MOVE_LINES_UP, IEmacsPlusCommandDefinitionIds.MOVE_LINES_UP);
			put(IEmacsPlusCommandDefinitionIds.MOVE_LINES_DOWN, IEmacsPlusCommandDefinitionIds.MOVE_LINES_DOWN);

			put(IEmacsPlusCommandDefinitionIds.COLUMN_NEXT, IEmacsPlusCommandDefinitionIds.COLUMN_NEXT);
			put(IEmacsPlusCommandDefinitionIds.COLUMN_PREVIOUS, IEmacsPlusCommandDefinitionIds.COLUMN_PREVIOUS);
			
			put(IEmacsPlusCommandDefinitionIds.SCROLL_LINE_UP, IEmacsPlusCommandDefinitionIds.SCROLL_LINE_UP);
			put(IEmacsPlusCommandDefinitionIds.SCROLL_LINE_DOWN, IEmacsPlusCommandDefinitionIds.SCROLL_LINE_DOWN);
			
			put(IEmacsPlusCommandDefinitionIds.SHIFT_RIGHT, IEmacsPlusCommandDefinitionIds.SHIFT_RIGHT);
			put(IEmacsPlusCommandDefinitionIds.SHIFT_LEFT, IEmacsPlusCommandDefinitionIds.SHIFT_LEFT);
		}
	};	
	
	// These are the ids of standard Eclipse commands that we adapt to calling with a negative C-u argument 
	@SuppressWarnings("serial")
	private static final HashMap<String,String> InverseUniversalCmds = new HashMap<String,String>() {
		{
			put(IEmacsPlusCommandDefinitionIds.DELETE_NEXT, IEmacsPlusCommandDefinitionIds.DELETE_PREVIOUS);
			put(IEmacsPlusCommandDefinitionIds.DELETE_PREVIOUS, IEmacsPlusCommandDefinitionIds.DELETE_NEXT);
			put(IEmacsPlusCommandDefinitionIds.DELETE_NEXT_WORD,IEmacsPlusCommandDefinitionIds.DELETE_PREVIOUS_WORD);
			put(IEmacsPlusCommandDefinitionIds.DELETE_PREVIOUS_WORD,IEmacsPlusCommandDefinitionIds.DELETE_NEXT_WORD);
			put(IEmacsPlusCommandDefinitionIds.SMART_ENTER_INVERSE, IEmacsPlusCommandDefinitionIds.SMART_ENTER);
			put(IEmacsPlusCommandDefinitionIds.SMART_ENTER, IEmacsPlusCommandDefinitionIds.SMART_ENTER_INVERSE);
			
			put(IEmacsPlusCommandDefinitionIds.EMP_DELETE, IEmacsPlusCommandDefinitionIds.DELETE_PREVIOUS);
			put(IEmacsPlusCommandDefinitionIds.DELETE_NEXT, IEmacsPlusCommandDefinitionIds.DELETE_PREVIOUS);
			put(IEmacsPlusCommandDefinitionIds.DELETE_PREVIOUS, IEmacsPlusCommandDefinitionIds.DELETE_NEXT);
			
			put(IEmacsPlusCommandDefinitionIds.DELETE_LINE, IEmacsPlusCommandDefinitionIds.BACKWARD_DELETE_LINE);
			put(IEmacsPlusCommandDefinitionIds.MOVE_LINES_UP, IEmacsPlusCommandDefinitionIds.MOVE_LINES_DOWN);
			put(IEmacsPlusCommandDefinitionIds.MOVE_LINES_DOWN, IEmacsPlusCommandDefinitionIds.MOVE_LINES_UP);
			
			put(IEmacsPlusCommandDefinitionIds.COLUMN_NEXT, IEmacsPlusCommandDefinitionIds.COLUMN_PREVIOUS);
			put(IEmacsPlusCommandDefinitionIds.COLUMN_PREVIOUS, IEmacsPlusCommandDefinitionIds.COLUMN_NEXT);
			
			put(IEmacsPlusCommandDefinitionIds.SCROLL_LINE_UP, IEmacsPlusCommandDefinitionIds.SCROLL_LINE_DOWN);
			put(IEmacsPlusCommandDefinitionIds.SCROLL_LINE_DOWN, IEmacsPlusCommandDefinitionIds.SCROLL_LINE_UP);
			
			put(IEmacsPlusCommandDefinitionIds.SHIFT_RIGHT, IEmacsPlusCommandDefinitionIds.SHIFT_LEFT);
			put(IEmacsPlusCommandDefinitionIds.SHIFT_LEFT, IEmacsPlusCommandDefinitionIds.SHIFT_RIGHT);
			
		}
	};	
}
