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
import java.util.LinkedList;
import java.util.Map;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.BadPositionCategoryException;
import org.eclipse.jface.text.DefaultPositionUpdater;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.Position;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.part.MultiPageEditorPart;
import org.eclipse.ui.texteditor.ITextEditor;

/**
 * A Mark List to support Mark Ring operations 
 * Use Eclipse Document positions/position category to auto update as text is changed
 * 
 * @author Mark Feber - initial API and implementation
 */
public class MarkRing {

	/** The global ring of mark locations where a find-tag (i.e. open-declaration) was initiated */
	private static MarkList tagMarks = new MarkList(RingBuffer.getDefaultSize() * 2);
	/** The global ring of mark locations used by the global mark methods */
	private static MarkList globalMarks = new MarkList(RingBuffer.getDefaultSize() * 2);
	/** Local mark rings indexed by document */
	private static Map<IDocument,MarkList> localMarks = new HashMap<IDocument,MarkList>();

	public static final String MARK_CATEGORY = "Emacs+Mark";	//$NON-NLS-1$
	static DefaultPositionUpdater updater = new DefaultPositionUpdater(MARK_CATEGORY) {
		@Override
		protected boolean notDeleted() {
			return true;
		}
	};
	
	/**
	 * Add the previous mark to the Mark Ring
	 * 
	 * @param editor - the current editor
	 * @param document - the editor's document for position updating
	 * @param localMark - the previous mark location to save
	 * @param globalMark - the new mark location; a potential global candidate 
	 */
	public static void addMark(ITextEditor editor, IDocument document, int localMark, int globalMark) {
		MarkList local = localMarks.get(document);
		if (local == null) {
			local = new MarkList(RingBuffer.getDefaultSize());
			localMarks.put(document, local);
			document.addPositionCategory(MARK_CATEGORY);
			document.addPositionUpdater(updater);
		}
		local.addMark(document, localMark);
		// check to see if we should save globally
		globalMarks.addMark(editor,document,globalMark,true);
	}

	/**
	 * Remove the document's mark buffer from the local Mark list
	 * Also, remove the position updating information from the document
	 * 
	 * @param document
	 */
	static void removeMarks(IDocument document) {
		MarkList local = localMarks.remove(document);
		if (local != null || document.containsPositionCategory(MARK_CATEGORY)) {
			document.removePositionUpdater(updater);
			try {
				// this also removes all the positions
				document.removePositionCategory(MARK_CATEGORY);
			} catch (BadPositionCategoryException e) {
			}
		}
	}
	
	/**
	 * Pop the position from the front of the mark buffer and append the 
	 * mark to the end
	 * 
	 * @param document
	 * @return a Position containing (mark offset, popped mark offset) or null
	 */
	public static Position popMark(IDocument document, int mark) {
		Position result = null;
		MarkList local = localMarks.get(document);
		if (local != null) {
			Position pos = local.popAppendMark(document, mark);
			result = new Position(mark,pos.getOffset());
		}
		return result;
	}
	
	public static int getMarkOffset(IDocument document) {
		int result = -1;
		MarkList local = localMarks.get(document);
		if (local != null && !local.isEmpty()) {
			result = local.getFirst().getOffset();
		}
		return result;
	}
	
	/**
	 * Get the next position from the global mark buffer
	 * 
	 * @return the 'popped' global mark position 
	 */
	public static IBufferLocation popGlobalMark(boolean norotate) {
		return globalMarks.popMark(norotate);
	}

	/**
	 * Get the next position from the tag buffer
	 * 
	 * @return the 'popped' mark position 
	 */
	public static IBufferLocation popTagMark() {
		return tagMarks.popMark(true);
	}

	/**
	 * Add the location to the tag mark ring
	 *  
	 * @param editor
	 * @param document
	 * @param mark
	 */
	public static void addTagMark(ITextEditor editor, IDocument document, int mark) {
		tagMarks.addMark(editor,document,mark,false);
	}

	@SuppressWarnings({ "serial" })
	private static class MarkList extends LinkedList<IBufferLocation> {
		
		private int bufferSize = RingBuffer.getDefaultSize();
		
		MarkList(int length) {
			this.bufferSize = length;
		}

		/**
		 * Add Mark used by local mark ring
		 * 
		 * @param document
		 * @param mark
		 * @return the newly created Position or null, if mark == -1 or other error
		 */
		Position addMark(IDocument document, int mark) {
			Position result = null;
			if (mark != -1) {
				// avoid needless duplication
				if (isEmpty() || (peek().getOffset() != mark && getLast().getOffset() != mark)) {
					try {
						result = new Position(mark);
						document.addPosition(MARK_CATEGORY, result);
						pushElement(document, new MarkElement(result));
					} catch (BadLocationException e) {
					} catch (BadPositionCategoryException e) {
					}
				}
			}			
			return result;
		}
		
		/**
		 * Add Mark used by the global mark ring, which requires the editor
		 * If the last global mark pushed is not in the editor.
		 * 
		 * @param editor
		 * @param document
		 * @param mark
		 * @return the newly created Position or null, if global add not required or other error
		 */
		Position addMark(ITextEditor editor, IDocument document, int mark, boolean checkIt) {
			Position globalPos = null;
			if (mark != -1 && (!checkIt || checkNewMark(editor))) {
				globalPos = new Position(mark);
				try {
					document.addPosition(MARK_CATEGORY, globalPos);
				} catch (BadLocationException e) {
				} catch (BadPositionCategoryException e) {
				}					
				pushElement(document, new MarkElement(editor,globalPos));
			}
			return globalPos;
		}
		
		/**
		 * Add new element to the front of the ring.  If the length is 
		 * exceeded, drop the last element 
		 * 
		 * @param document
		 * @param element
		 */
		void pushElement(IDocument document, IBufferLocation element) {
			addFirst(element);
			if (size() > bufferSize) {
				IBufferLocation old = removeLast();
				try {
					removeMarkPosition(document,((MarkElement)old).getPosition());
				} catch (BadPositionCategoryException e) {
				}
			}
		}

		/**
		 * Append the mark to the end of the buffer and pop the front
		 * 
		 * @param document
		 * @param mark
		 * @return the popped position
		 */
		Position popAppendMark(IDocument document, int mark) {
			Position result = null;
			MarkElement element = null;
			if (mark != -1) {
				try {
					result = new Position(mark);
					document.addPosition(MARK_CATEGORY, result);
					element = new MarkElement(result);
					addLast(element);
					element = (MarkElement) removeFirst();
					result = element.getPosition();
					// remove from document as internal Mark code will update it
					removeMarkPosition(document, result);
				} catch (BadLocationException e) {
				} catch (BadPositionCategoryException e) {
				}
			}			
			return result;
		}

		/**
		 * Pop the mark location and push it on the end
		 * used by global marks
		 *  
		 * @return the popped location
		 */
		IBufferLocation popMark(boolean norotate) {
			IBufferLocation element = null;
			if (!isEmpty()) {
				element = (norotate ? removeFirst() : removeLast());
				// are we still alive?
				if (checkEditor(element)) {
					// then add
					if (norotate) {
						addLast(element);
					} else {
						addFirst(element);
					}
				} else {
					element.setEditor(null);
					// keep popping
					element = popMark(norotate);
				}
			}
			return element;
		}

		/**
		 * Check the front of the buffer
		 * 
		 * @param editor
		 * @return true if editor is not at the front of the buffer
		 */
		private boolean checkNewMark(ITextEditor editor) {
			boolean result = true;
			if (!isEmpty()) {
				result = editor != peek().getEditor();
			}
			return result;
		}
		
		private void removeMarkPosition(IDocument document, Position mark) throws BadPositionCategoryException {
			document.removePosition(MARK_CATEGORY, mark);
		}
		
		/**
		 * Verify that the editor in the location is still in use
		 * 
		 * @param location
		 * @return true if editor is valid
		 */
		private boolean checkEditor(IBufferLocation location) {
			boolean result = false;
			if (location != null) {
				ITextEditor editor = location.getEditor(); 
				if (editor != null) {
					IEditorInput input = editor.getEditorInput();
					// check all the editor references that match the input for a match
					IEditorReference[] refs = EmacsPlusUtils.getWorkbenchPage().findEditors(input,null, IWorkbenchPage.MATCH_INPUT); 
					for (int i=0; i< refs.length; i++) {
						IEditorPart ed = refs[i].getEditor(false);
						// multi page annoyance
						if (ed instanceof MultiPageEditorPart) {
							IEditorPart[] eds = ((MultiPageEditorPart)ed).findEditors(input);
							for (int j=0; j < eds.length; j++) {
								if (eds[i] == editor) {
									result = true;
									break;
								}
							}
							if (result) {
								break;
							}
						} else {
							if (ed == editor) {
								result = true;
								break;
							}
						}
					}
				}
			}
			return result;
		}
		
		// for debugging
		@SuppressWarnings("unused")
		private void printMarks(String header) {
			int i = 0;
			System.out.println(header);
			for (IBufferLocation loc : this) {
				String ed = "\t";	//$NON-NLS-1$
				if (loc.getEditor() != null) {
					ed = ed + loc.getEditor().getEditorInput().getName()+ ' ';
				}
				System.out.println(i++ + ed + '<' + loc.getOffset() + '>');
			} 
		}
	}
	
	static class MarkElement implements IBufferLocation {

		private Position position = null;
		private ITextEditor editor = null;
		
		MarkElement() {}
		
		MarkElement(Position position) {
			this.position = position;
		}
		
		MarkElement(ITextEditor editor, Position position) {
			this(position);
			this.editor = editor;
		}
		/**
		 * @see com.mulgasoft.emacsplus.IBufferLocation#getEditor()
		 */
		public ITextEditor getEditor() {
			return editor;
		}

		/**
		 * @see com.mulgasoft.emacsplus.IBufferLocation#getOffset()
		 */
		public int getOffset() {
			int result = -1;
			if (position != null) {
				result = position.getOffset();
			}
			return result;
		}

		/**
		 * @see com.mulgasoft.emacsplus.IBufferLocation#setEditor(org.eclipse.ui.texteditor.ITextEditor)
		 */
		public void setEditor(ITextEditor editor) {
			this.editor = editor;
		}

		/**
		 * @see com.mulgasoft.emacsplus.IBufferLocation#setPosition(org.eclipse.ui.texteditor.ITextEditor, org.eclipse.jface.text.Position)
		 */
		public void setPosition(ITextEditor editor, Position position) {
			setEditor(editor);
			this.position = position;
		}
		
		public Position getPosition() {
			return position;
		}
	}
}
