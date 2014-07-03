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

import java.util.HashMap;

import org.eclipse.core.commands.Command;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.IExecutionListener;
import org.eclipse.core.commands.NotHandledException;
import org.eclipse.core.runtime.ListenerList;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentListener;
import org.eclipse.jface.text.IMarkRegionTarget;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.ITextViewerExtension;
import org.eclipse.jface.text.ITextViewerExtension5;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.jface.text.TextViewer;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.commands.ICommandService;
import org.eclipse.ui.console.TextConsoleViewer;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.texteditor.AbstractTextEditor;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.ITextEditor;

import com.mulgasoft.emacsplus.commands.EmacsMovementHandler;
import com.mulgasoft.emacsplus.commands.EmacsPlusCmdHandler;

/**
 * Utilities and Listeners associated with correcting the broken Eclipse
 * behavior of Point and Mark
 * 
 * @author Mark Feber - initial API and implementation
 */
@SuppressWarnings("serial")
public class MarkUtils {

	private static final String MARK_SET = "Mark_Set";	//$NON-NLS-1$
	
	private static IExecutionListener copyCmdExecListener;
	private static IExecutionListener execExecListener;
	private static IDocumentListener docListener = null;
	private static String lastCommand = null;
	
	private static String currentCommand = null;

	private MarkUtils() {}	// no children/instances allowed
	
	// A hash of commands (and their inverse if appropriate) that should not clear the shift flag 
	private static HashMap<String, String> shiftMarkHash = new HashMap<String, String>() {
		{
			put(IEmacsPlusCommandDefinitionIds.FORWARD_CHAR, IEmacsPlusCommandDefinitionIds.BACKWARD_CHAR);
			put(IEmacsPlusCommandDefinitionIds.BACKWARD_CHAR, IEmacsPlusCommandDefinitionIds.FORWARD_CHAR);
			
			put(IEmacsPlusCommandDefinitionIds.FORWARD_WORD, IEmacsPlusCommandDefinitionIds.BACKWARD_WORD);
			put(IEmacsPlusCommandDefinitionIds.BACKWARD_WORD, IEmacsPlusCommandDefinitionIds.FORWARD_WORD);
			
			put(IEmacsPlusCommandDefinitionIds.MARK_FORWARD_SEXP, IEmacsPlusCommandDefinitionIds.MARK_BACKWARD_SEXP);
			put(IEmacsPlusCommandDefinitionIds.SELECT_FORWARD_SEXP, IEmacsPlusCommandDefinitionIds.SELECT_BACKWARD_SEXP);
			put(IEmacsPlusCommandDefinitionIds.MARK_BACKWARD_SEXP, IEmacsPlusCommandDefinitionIds.MARK_FORWARD_SEXP);
			put(IEmacsPlusCommandDefinitionIds.SELECT_BACKWARD_SEXP, IEmacsPlusCommandDefinitionIds.SELECT_FORWARD_SEXP);
			
			put(IEmacsPlusCommandDefinitionIds.SELECT_FORWARD_PARAGRAPH, IEmacsPlusCommandDefinitionIds.BACKWARD_MARK_PARAGRAPH);
			put(IEmacsPlusCommandDefinitionIds.SELECT_BACKWARD_PARAGRAPH, IEmacsPlusCommandDefinitionIds.FORWARD_MARK_PARAGRAPH);
			put(IEmacsPlusCommandDefinitionIds.FORWARD_MARK_PARAGRAPH, IEmacsPlusCommandDefinitionIds.BACKWARD_MARK_PARAGRAPH);
			put(IEmacsPlusCommandDefinitionIds.BACKWARD_MARK_PARAGRAPH, IEmacsPlusCommandDefinitionIds.FORWARD_MARK_PARAGRAPH);
			
			put(IEmacsPlusCommandDefinitionIds.FORWARD_BLOCK, IEmacsPlusCommandDefinitionIds.BACKWARD_BLOCK);
			put(IEmacsPlusCommandDefinitionIds.BACKWARD_BLOCK, IEmacsPlusCommandDefinitionIds.FORWARD_BLOCK);
			
			put(IEmacsPlusCommandDefinitionIds.NEXT_LINE, IEmacsPlusCommandDefinitionIds.PREVIOUS_LINE);
			put(IEmacsPlusCommandDefinitionIds.PREVIOUS_LINE, IEmacsPlusCommandDefinitionIds.NEXT_LINE);
			put(IEmacsPlusCommandDefinitionIds.BEGIN_LINE, IEmacsPlusCommandDefinitionIds.BEGIN_LINE);
			put(IEmacsPlusCommandDefinitionIds.END_LINE, IEmacsPlusCommandDefinitionIds.END_LINE);
			
			put(IEmacsPlusCommandDefinitionIds.SCROLL_UP, IEmacsPlusCommandDefinitionIds.SCROLL_DOWN);
			put(IEmacsPlusCommandDefinitionIds.SCROLL_DOWN, IEmacsPlusCommandDefinitionIds.SCROLL_UP);
			
			put(IEmacsPlusCommandDefinitionIds.SELECT_COLUMN_PREVIOUS,IEmacsPlusCommandDefinitionIds.SELECT_COLUMN_NEXT);
			put(IEmacsPlusCommandDefinitionIds.SELECT_LINE_DOWN, IEmacsPlusCommandDefinitionIds.SELECT_LINE_UP);
			put(IEmacsPlusCommandDefinitionIds.SELECT_WORD_PREVIOUS, IEmacsPlusCommandDefinitionIds.SELECT_WORD_NEXT);
			put(IEmacsPlusCommandDefinitionIds.SELECT_LINE_START, IEmacsPlusCommandDefinitionIds.SELECT_LINE_END);
			put(IEmacsPlusCommandDefinitionIds.SELECT_TEXT_START, IEmacsPlusCommandDefinitionIds.SELECT_TEXT_END);
			put(IEmacsPlusCommandDefinitionIds.SELECT_LINE_END, IEmacsPlusCommandDefinitionIds.SELECT_LINE_START);
			put(IEmacsPlusCommandDefinitionIds.SELECT_TEXT_END, IEmacsPlusCommandDefinitionIds.SELECT_TEXT_START);
			put(IEmacsPlusCommandDefinitionIds.SELECT_COLUMN_NEXT,IEmacsPlusCommandDefinitionIds.SELECT_COLUMN_PREVIOUS);
			put(IEmacsPlusCommandDefinitionIds.SELECT_LINE_UP, IEmacsPlusCommandDefinitionIds.SELECT_LINE_DOWN);
			put(IEmacsPlusCommandDefinitionIds.SELECT_WORD_NEXT, IEmacsPlusCommandDefinitionIds.SELECT_WORD_PREVIOUS);
			put(IEmacsPlusCommandDefinitionIds.SELECT_PAGE_UP, IEmacsPlusCommandDefinitionIds.SELECT_PAGE_DOWN);
			put(IEmacsPlusCommandDefinitionIds.SELECT_PAGE_DOWN, IEmacsPlusCommandDefinitionIds.SELECT_PAGE_UP);
			
			put(IEmacsPlusCommandDefinitionIds.BACK_TO_INDENT, IEmacsPlusCommandDefinitionIds.BACK_TO_INDENT);
			put(IEmacsPlusCommandDefinitionIds.UNIVERSAL_ARGUMENT, IEmacsPlusCommandDefinitionIds.UNIVERSAL_ARGUMENT);
		}
	};
	
	// A hash of commands (and their inverse if appropriate) that should not clear the mark flag 
	private static HashMap<String, String> markHash = new HashMap<String, String>(shiftMarkHash) {
		{
			put(IEmacsPlusCommandDefinitionIds.BEGIN_BUFFER, IEmacsPlusCommandDefinitionIds.END_BUFFER);
			put(IEmacsPlusCommandDefinitionIds.END_BUFFER, IEmacsPlusCommandDefinitionIds.BEGIN_BUFFER);
			// non-movement commands
			put(IEmacsPlusCommandDefinitionIds.SET_MARK, IEmacsPlusCommandDefinitionIds.SET_MARK);
			// NB: the command Meta-X calls, may clear the flag
			put(IEmacsPlusCommandDefinitionIds.METAX_EXECUTE, IEmacsPlusCommandDefinitionIds.METAX_EXECUTE);
		}
	};

	public static boolean isShiftCommand(String cmdId) {
		return shiftMarkHash.containsKey(cmdId);
	}
	
	/**
	 * Return the inverse operation for the specified Emacs+ command Id.
	 * The inverse operation, if present, is used when a negative universal argument is detected.
	 * 
	 * @param commandId
	 * @return the inverse of the command id or null
	 */
	public static String getInverseId(String commandId) {
		String result = markHash.get(commandId);
		if (result != null && result.equals(commandId)) {
			result = null;
		}
		return result;
	}
	
	public static String getCurrentCommand() {
		return currentCommand;
	}

	public static void setCurrentCommand(String commandId) {
		currentCommand = commandId;
	}
	
	/**
	 * Get the current Mark position
	 * 
	 * @param editor
	 * @return the Mark position in model coords (-1 if not set)
	 */
	public static int getMark(ITextEditor editor) {
		int result = -1;
		ITextViewerExtension ive = getITextViewer(editor);
		if (ive != null) {
			result = ive.getMark();
		}
		return result;
	}

	/**
	 * Set Mark at current cursor position and push the previous Mark on the
	 * Mark Ring
	 * 
	 * @param editor
	 * @return the mark position in document coords
	 */
	public static int setMark(ITextEditor editor) {
		IMarkRegionTarget markTarget = (IMarkRegionTarget) editor.getAdapter(IMarkRegionTarget.class);
		int localMark = getMark(editor);
		markTarget.setMarkAtCursor(true);
		int newMark = getMark(editor);
		MarkRing.addMark(editor, editor.getDocumentProvider().getDocument(editor.getEditorInput()), localMark, newMark);
		EmacsPlusUtils.showMessage(editor, MARK_SET, false);
		return newMark;
	}

	public static int setMark(ITextEditor editor, int offset) {
		return setMark(editor, MarkUtils.getITextViewer(editor), offset, true);
	}

	/**
	 * Set Mark at offset, and potentially save the marks in the Mark Rings
	 * 
	 * @param editor
	 * @param ve
	 * @param offset - the offset in document (absolute) coords
	 * @param save - true if we're (potentially) saving in the Mark Rings
	 * @return the mark position in document coords
	 */
	public static int setMark(ITextEditor editor, ITextViewerExtension ve, int offset, boolean save) {
		int result = -1;
		if (ve != null) {
			int localMark = ve.getMark();
			ve.setMark(offset);
			result = ve.getMark();
			if (save) {
				MarkRing.addMark(editor, ve.getRewriteTarget().getDocument(), localMark, result);
				EmacsPlusUtils.showMessage(editor, MARK_SET, false);
			}
		}
		return result;
	}

	/**
	 * Pop the current buffer's Mark from the Mark Ring
	 * 
	 * @param editor
	 * @return the offset of the Mark
	 */
	public static Position popMark(ITextEditor editor) {
		return popMark(editor, editor.getDocumentProvider().getDocument(editor.getEditorInput()));
	}

	/**
	 * Pop the current buffers Mark from the Mark Ring
	 * 
	 * @param editor
	 * @return the offset of the Mark
	 */
	public static Position popMark(ITextEditor editor, IDocument document) {
		return MarkRing.popMark(document, getMark(editor));
	}

	/**
	 * Pop the current global Mark
	 * 
	 * @return the location of the Mark
	 */
	public static IBufferLocation popGlobalMark(boolean norotate) {
		return MarkRing.popGlobalMark(norotate);
	}

	/**
	 * Support simple clear mark/selection on TextConsole
	 *
	 * @param viewer
	 */
	public static void clearConsoleMark(TextConsoleViewer viewer) {
		if (viewer != null) {
			StyledText st = viewer.getTextWidget();
			st.setSelection(st.getCaretOffset());
			viewer.setMark(-1);
		}
	}
	
	/**
	 * Get the selection point from the text widget
	 * 
	 * @param editor
	 * 
	 * @return the selection point iff it is a (Styled)Text widget
	 * x is the offset of the first	selected character, 
	 * y is the offset after the last selected character.
	 */
	public static int getCharCount(ITextEditor editor) {
		int result = -1;
		Control text = (Control) editor.getAdapter(Control.class);
		if (text instanceof StyledText) {
			result = ((StyledText) text).getCharCount();
		} else if (text instanceof Text) {
			result = ((Text) text).getCharCount();
		}
		return result;
	}
	
	/**
	 * Get the current caret offset in widget coords
	 * 
	 * @param editor
	 * @return the caret position
	 */
	public static int getCaretOffset(ITextEditor editor) {
		int result = 0;
		Control text = getTextWidget(editor);
		if (text instanceof StyledText) {
			result = ((StyledText) text).getCaretOffset();
		} else if (text instanceof Text) {
			result = ((Text) text).getCaretPosition();
		}
		return result;
	}
	
	/**
	 * Get the current cursor offset in model coords
	 * 
	 * @param editor
	 * @return the cursor position
	 */
	public static int getCursorOffset(ITextEditor editor) {
		int result = 0;
		int len = 0;
		Control text = getTextWidget(editor);
		if (text instanceof StyledText) {
			result = ((StyledText) text).getCaretOffset();
			len = ((StyledText) text).getCharCount();
		} else if (text instanceof Text) {
			result = ((Text) text).getCaretPosition();
		}
		ITextViewerExtension5 ve5;
		try {
			int off = result;
			ve5 = (ITextViewerExtension5) MarkUtils.getITextViewer(editor);
			result = ve5.widgetOffset2ModelOffset(result);
			// end of buffer computations differ in different (non standard) viewers
			// so, if out of bounds returned, back up one
			if (result == -1 && off == len) {
				result = ve5.widgetOffset2ModelOffset(off - 1);
			}
		} catch (Exception e) {
		}

		return result;
	}

	/**
	 * Set the cursor offset using the editors selection provider
	 * 
	 * @param editor
	 * @param offset - in model coords
	 */
	public static void setCursorOffset(ITextEditor editor, int offset) {
		IDocument document = editor.getDocumentProvider().getDocument(editor.getEditorInput());
		setSelection(editor, new TextSelection(document, offset, 0));
	}

	// Note: ITextViewerExtension5 is not available on all TextViewers

	public static int widget2ModelOffset(ISourceViewer viewer, int widgetOffset) {
		int result = widgetOffset;
		try {
			if (viewer instanceof ITextViewerExtension5) {
				result = ((ITextViewerExtension5) viewer).widgetOffset2ModelOffset(result);
			}
		} catch (Exception e) {
		}
		return result;
	}

	public static int model2WidgetOffset(ISourceViewer viewer, int modelOffset) {
		int result = modelOffset;
		try {
			if (viewer instanceof ITextViewerExtension5) {
				result = ((ITextViewerExtension5) viewer).modelOffset2WidgetOffset(result);
			}
		} catch (Exception e) {
		}
		return result;
	}

	public static void revealRange(ITextEditor editor, int start, int length) {
		findSourceViewer(editor).revealRange(start, length);
	}

	/**
	 * @param editor
	 * @param start
	 *            - in model coords
	 * @param length
	 *            - in model coords
	 */
	public static void setSelection(ITextEditor editor, int start, int length) {

		TextViewer tv = findTextViewer(editor);
		ITextSelection selection = new TextSelection(null, start, length);
		if (tv != null) {
			tv.setSelection(selection, false);
		} else {
			setSelection(editor, selection);
		}
	}

	/**
	 * Wrap selection provider setSelection within a block disabling highlight
	 * range to protect against the JavaEditor from changing the narrow focus in
	 * - org.eclipse.jdt.internal.ui.javaeditor.TogglePresentationAction -
	 * 
	 * @param editor
	 * @param selection
	 *            in model coords
	 */
	public static void setSelection(ITextEditor editor, ITextSelection selection) {
		boolean isNarrow = editor.showsHighlightRangeOnly();
		// Use the text widget, as the IRewriteTarget has unpleasant scrolling side effects 
		Control text = getTextWidget(editor);
		try {
			text.setRedraw(false);
			if (isNarrow) {
				editor.showHighlightRangeOnly(false);
			}
			editor.getSelectionProvider().setSelection(selection);
		} finally {
			if (isNarrow) {
				editor.showHighlightRangeOnly(isNarrow);
			}
			text.setRedraw(true);
		}
	}

	/**
	 * Get the widget associated with the editor
	 * 
	 * @param editor
	 * @return a Control iff it is a (Styled)Text widget
	 */
	public static Control getTextWidget(ITextEditor editor) {
		Control result = null;
		Control text = (Control) editor.getAdapter(Control.class);
		if (text instanceof StyledText) {
			result = (StyledText) text;
		} else if (text instanceof Text) {
			result = (Text) text;
		}
		return result;
	}

	/**
	 * Get the selection point from the text widget
	 * 
	 * @param editor
	 * 
	 * @return the selection point iff it is a (Styled)Text widget
	 * x is the offset of the first	selected character, 
	 * y is the offset after the last selected character.
	 */
	public static Point getWidgetSelection(ITextEditor editor) {
		Point result = null;
		Control text = (Control) editor.getAdapter(Control.class);
		if (text instanceof StyledText) {
			result = ((StyledText) text).getSelection();
		} else if (text instanceof Text) {
			result = ((Text) text).getSelection();
		}
		return result;
	}

	/**
	 * set the selection in the text widget
	 * 
	 * @param editor
	 * @param start
	 *            - the start of selection in widget coords
	 * @param end
	 *            - the end of selection in widget coords
	 */
	public static void setWidgetSelection(ITextEditor editor, int start, int end) {
		Control text = (Control) editor.getAdapter(Control.class);
		if (text instanceof StyledText) {
			((StyledText) text).setSelection(start, end);
		} else if (text instanceof Text) {
			((Text) text).setSelection(start, end);
		}
	}

	public static String getWidgetLineDelimiter(ITextEditor editor) {

		String result = "\n"; //$NON-NLS-1$
		Control w = (Control) editor.getAdapter(Control.class);
		if (w instanceof StyledText) {
			result = ((StyledText) w).getLineDelimiter();
		} else if (w instanceof Text) {
			result = ((Text) w).getLineDelimiter();
		}
		return result;
	}

	public static StyledText getStyledWidget(ITextEditor editor) {
		StyledText result = null;
		Control w = (Control) editor.getAdapter(Control.class);
		if (w instanceof StyledText) {
			result = (StyledText) w;
		}		
		return result;
	}
	
	/*************************** Tag Mark handling ***************************/

	/**
	 * Set the current tag Mark 
	 * 
	 * @return the location of the Mark
	 */
	public static int setTagMark(ITextEditor editor, int offset) {
		int result = (offset == -1) ? setMark(editor) : setMark(editor,offset);
		if (result!= -1) {
			// add to the global tag mark ring
			MarkRing.addTagMark(editor,editor.getDocumentProvider().getDocument(editor.getEditorInput()),result);
		}
		return result;
	}

	/**
	 * Pop the current tag Mark
	 * 
	 * @return the location of the Mark
	 */
	public static IBufferLocation popTagMark() {
		return MarkRing.popTagMark();
	}
	
	static IExecutionListener getTagListener() {
		return tagMarker;
	}
	
	private static IExecutionListener tagMarker = new IExecutionListener() {

		public void notHandled(String commandId, NotHandledException exception) { }
		public void postExecuteFailure(String commandId, ExecutionException exception) { }
		public void postExecuteSuccess(String commandId, Object returnValue) { }

		public void preExecute(String commandId, ExecutionEvent event) {
			try {
				ITextEditor editor = EmacsPlusUtils.getActiveTextEditor(HandlerUtil.getActiveEditorChecked(event));
				if (editor != null) {
					MarkUtils.setTagMark(editor,-1);
				}
			} catch (ExecutionException e) {
				// ignore any (unlikely) error 
			}			
		}
	};
	
	/*************************** Enhanced Mark handling ***************************/

	private static boolean ignoreDispatchId = false;
	
	// We need to ignore commands we invoke from other commands
	public static void setIgnoreDispatchId(boolean state) {
		ignoreDispatchId = state;
	}
	
	public static interface ICommandIdListener {
		void setCommandId(String commandId);
	}
	
	private static ListenerList commandIdListeners = new ListenerList();
	public static void addCommandIdListener(ICommandIdListener listener) {
		commandIdListeners.add(listener);
	}
	public static void removeCommandIdListener(ICommandIdListener listener) {
		commandIdListeners.remove(listener);
	}
	
	/*************************** Enhanced Mark handling ***************************/

	// Special command listeners to enforce emacs selection behavior in relation to mark
	public static void addActivationListeners(ITextEditor editor) {
		if (editor != null) {
			addExecutionListeners(editor);
			addDocumentListeners(editor);
		}
	}

	public static void removeActivationListeners(ITextEditor editor) {
		if (editor != null) {
			removeExecutionListeners(editor);
			removeDocumentListeners(editor);
		}
	}

	public static String getLastCommandId() {
		return lastCommand;
	}
	
	private static void setLastCommand(String commandId) {
		for (Object listener : commandIdListeners.getListeners()) {
			if (!ignoreDispatchId && listener instanceof ICommandIdListener) {
				((ICommandIdListener)listener).setCommandId(commandId);
			}
		}
		lastCommand = commandId;
	}
	
	// Copy command should clear mark & region
	private static void addExecutionListeners(final ITextEditor editor) {

		// handle multi-part editors which don't call deactivate for individual parts
		MarkUtils.removeExecutionListeners(editor);
		lastCommand = null;	// initialize command id state
		ICommandService ics = (ICommandService) editor.getSite().getService(ICommandService.class);
		if (ics != null) {
			// Add a listener for every command on this editor to clear the mark region
			// when the text changes
			execExecListener = new IExecutionListener() {
				ITextEditor cEditor = editor;

				public void notHandled(String commandId, NotHandledException exception) {
					currentCommand = null;
				}

				public void postExecuteFailure(String commandId, ExecutionException exception) {
					currentCommand = null;					
				}

				public void postExecuteSuccess(String commandId, Object returnValue) {
					if (EmacsPlusCmdHandler.isChanged) {
						clearMarkRegion(cEditor);
						EmacsPlusCmdHandler.isChanged = false;
					}
					if (!markHash.containsKey(commandId)) {
						// clear the mark flag
						EmacsPlusCmdHandler.setFlagMark(false);
					} 

					// remember command id of last command executed
					setLastCommand(commandId);
					currentCommand = null;
				}

				public void preExecute(String commandId, ExecutionEvent event) {
					if (notYank(commandId)) {
						// Fix (possible regression?) to disallow yank-pop after non-yank
						KillRing.getInstance().setYanked(false);						
					}
					EmacsPlusCmdHandler.isChanged = false;
					currentCommand = commandId;					
				}
				
				private boolean notYank(String commandId) {
					return	(!(IEmacsPlusCommandDefinitionIds.YANK.equals(commandId) || 
							   IEmacsPlusCommandDefinitionIds.YANK_POP.equals(commandId)|| 
							   IEmacsPlusCommandDefinitionIds.METAX_EXECUTE.equals(commandId)
					));
				}
			};
			ics.addExecutionListener(execExecListener);

			Command com = ics.getCommand(IEmacsPlusCommandDefinitionIds.EMP_COPY);
			if (com != null) {
				// Add a listener to COPY command to always clear the mark region
				copyCmdExecListener = new IExecutionListener() {
					ITextEditor cEditor = editor;

					public void notHandled(String commandId, NotHandledException exception) {
					}

					public void postExecuteFailure(String commandId, ExecutionException exception) {
					}

					public void postExecuteSuccess(String commandId, Object returnValue) {
						clearMarkRegion(cEditor);
					}

					public void preExecute(String commandId, ExecutionEvent event) {
					}
				};
				com.addExecutionListener(copyCmdExecListener);
			}
		}
	}

	private static void removeExecutionListeners(ITextEditor editor) {
		ICommandService ics = (ICommandService) editor.getSite().getService(ICommandService.class);
		if (ics != null) {
			if (execExecListener != null) {
				ics.removeExecutionListener(execExecListener);
			}
			if (copyCmdExecListener != null) {
				Command com = ics.getCommand(IEmacsPlusCommandDefinitionIds.EMP_COPY);
				if (com != null) {
					com.removeExecutionListener(copyCmdExecListener);
				}
			}
		}
		copyCmdExecListener = null;
		execExecListener = null;
	}

	// If the document changes, flag it for the command listener
	private static void addDocumentListeners(ITextEditor editor) {
		// handle multi-part editors which don't call deactivate for individual parts
		MarkUtils.removeDocumentListeners(editor);
		IDocumentProvider idp = editor.getDocumentProvider();
		IDocument document;
		// add null check for document, due to an unreproducible NPE reported by a clojure user
		if (idp != null && (document = idp.getDocument(editor.getEditorInput())) != null) {
			docListener = new IDocumentListener() {
				public void documentAboutToBeChanged(DocumentEvent event) {
				}

				public void documentChanged(DocumentEvent event) {
					EmacsPlusCmdHandler.isChanged = true;
				}
			};
			document.addDocumentListener(docListener);
		}
	}

	private static void removeDocumentListeners(ITextEditor editor) {
		if (docListener != null) {
			IDocumentProvider idp = editor.getDocumentProvider();
			IDocument document;
			// add null check for document, due to an unreproducible NPE reported by a clojure user
			if (idp != null && (document = idp.getDocument(editor.getEditorInput())) != null) {
				document.removeDocumentListener(docListener);
			}
		}
		docListener = null;
	}

	private static void clearMarkRegion(ITextEditor editor) {
		int markOffset = getMark(editor);
		if (markOffset != -1) {
			// clear shift flag as well
			EmacsMovementHandler.clearShifted();
			ISelectionProvider isp = editor.getSelectionProvider();
			ITextSelection sel = (ITextSelection) isp.getSelection();

			if (sel.getOffset() <= markOffset && markOffset <= sel.getOffset() + sel.getLength()) {
				int offset = getCursorOffset(editor);
				// clear the selection
				setCursorOffset(editor, offset);
			}
		}
	}

	public static int model2WidgetOffset(ITextEditor editor, int pos) {
		return MarkUtils.model2WidgetOffset(findSourceViewer(editor), pos);
	}

	// Totally Evil
	// The protected method & private field that gives us the editor viewer for registration purposes
	private static String RE_METHOD_ID = "getSourceViewer"; //$NON-NLS-1$ 
	private static String RE_MEMBER_ID = "fSourceViewer";   //$NON-NLS-1$

	public static ITextViewerExtension getITextViewer(ITextEditor editor) {
		ITextViewerExtension result = null;
		ISourceViewer viewer = findSourceViewer(editor);
		if ((viewer instanceof ITextViewerExtension)) {
			result = ((ITextViewerExtension) viewer);
		}
		return result;
	}

	private static ISourceViewer findSourceViewer(ITextEditor editor) {
		// evil
		ISourceViewer result = null;
		if (editor != null && editor instanceof AbstractTextEditor) {
			result = (ISourceViewer) EmacsPlusUtils.getAM((AbstractTextEditor) editor, RE_METHOD_ID);
			if (result == null) {
				// even more evil
				result = (ISourceViewer) EmacsPlusUtils.getAF((AbstractTextEditor) editor, RE_MEMBER_ID);
			}
		}
		return result;
	}

	private static TextViewer findTextViewer(ITextEditor editor) {
		// evil
		TextViewer result = null;
		ISourceViewer sv = null;
		if (editor != null && editor instanceof AbstractTextEditor) {
			sv = (ISourceViewer) EmacsPlusUtils.getAM((AbstractTextEditor) editor, RE_METHOD_ID);
			if (sv == null) {
				// even more evil
				sv = (ISourceViewer) EmacsPlusUtils.getAF((AbstractTextEditor) editor, RE_MEMBER_ID);
			}
		}
		if (sv instanceof TextViewer) {
			result = (TextViewer) sv;
		}
		return result;
	}
}
