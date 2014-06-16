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
import java.util.Iterator;
import java.util.Map;
import java.util.TreeSet;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.BadPositionCategoryException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.Position;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.texteditor.ITextEditor;

/**
 * Teco Register - Emulates Emacs' emulation of TECO's register feature 
 * 
 * From register.el: 
 *  "This package of functions emulates and somewhat extends the <b>venerable</b>
 *  <b><i>TECO's</b></i> `register' feature, which permits you to save various useful
 *  pieces of buffer state to named variables."
 * 
 * @author Mark Feber - initial API and implementation
 */
public class TecoRegister {
	
	public static final String NAME = EmacsPlusActivator.getResourceString("Register_View_Name");   			//$NON-NLS-1$
	public static final String POINT = EmacsPlusActivator.getResourceString("Register_Contents_Point"); 		//$NON-NLS-1$
	public static final String NUMBER = EmacsPlusActivator.getResourceString("Register_Contents_Number");   	//$NON-NLS-1$
	public static final String TEXT = EmacsPlusActivator.getResourceString("Register_Contents_Text");   		//$NON-NLS-1$
	public static final String RECTANGLE = EmacsPlusActivator.getResourceString("Register_Contents_Rectangle"); //$NON-NLS-1$
	private final static String CR = "\n";  																	//$NON-NLS-1$
	
	private static Map<String,IRegisterContents> register = new HashMap<String,IRegisterContents>();
	private static TecoRegister instance = null;
		
	private TecoRegister() {}
	
	/**
	 * TecoRegister is a singleton
	 * 
	 * @return global register instance
	 */
	public static TecoRegister getInstance() {
		if (instance == null) {
			instance = new TecoRegister();
		}
		return instance;
	}
	
	public  Iterator<String> iterator() {
		TreeSet<String> tset = new TreeSet<String>(register.keySet());
		return tset.iterator();
	}
	
	/**
	 * Insert text for register
	 * 
	 * @param key - register id
	 * @param text - new text
	 */
	public void put(String key,String text) {
		if (text != null) {
			RegisterContents contents = (RegisterContents)register.get(key);  
			if (contents != null) {
				contents.setText(text);
			} else {
				register.put(key, new RegisterContents(text));
			}
		}
	}
	
	/**
	 * Get the text associated with this register
	 * If text is null, then if a number is present return it as a string
	 * 
	 * @param key - register id
	 * @return the text, or null if no register or text
	 */
	public String getText(String key) {
		String result = null;
		IRegisterContents contents = register.get(key); 
		if (contents != null) {
			result = contents.getText();
			if (result == null) {
				Integer i = contents.getNumber();
				if (i != null) {
					result = i.toString();
				}
			}
		}
		return result;
	}
	
	/**
	 * Insert rectangle into register
	 * 
	 * @param key - register id
	 * @param rectangle - new rectangle
	 */
	public void put(String key,String[] rectangle) {
		if (rectangle != null) {
			RegisterContents contents = (RegisterContents)register.get(key);  
			if (contents != null) {
				contents.setRectangle(rectangle);
			} else {
				register.put(key, new RegisterContents(rectangle));
			}
		}
	}
	
	/**
	 * Get the rectangle associated with this register
	 * 
	 * @param key - register id
	 * @return the rectangle, or null if no register or not rectangle
	 */
	public String[] getRectangle(String key) {
		String[] result = null;
		IRegisterContents contents = register.get(key); 
		if (contents != null) {
			result = contents.getRectangle();
			if (result == null) {
			}
		}
		return result;
	}
	
	/**
	 * Insert number into register 
	 * 
	 * @param key - register id
	 * @param number new number
	 */
	public void put(String key,Integer number) {
		if (number != null) {
			RegisterContents contents = (RegisterContents)register.get(key);  
			if (contents != null) {
				contents.setNumber(number);
			} else {
				register.put(key, new RegisterContents(number));
			}
		}
	}
	
	/**
	 * Insert number into register 
	 * 
	 * @param key - register id
	 * @param number new number
	 */
	public void put(String key,int number) {
		put(key, new Integer(number));
	}
	
	/**
	 * Get the number from the register
	 * 
	 * @param key - register id
	 * @return the number or null if no register or number
	 */
	public Integer getNumber(String key) {
		Integer result = null;
		IRegisterContents contents = register.get(key); 
		if (contents != null) {
			result = contents.getNumber();
		}
		return result;
	}

	public void put(String key,ITextEditor editor, int offset) {
		RegisterContents contents = clearLocation(key);
		if (contents != null) {
			IRegisterLocation loc = contents.getLocation();
			if (loc != null) {
				((RegisterLocation)loc).setPosition(editor,offset);
			} else {
				contents.setLocation(new RegisterLocation(editor,offset));
			}
		} else {
			contents = new RegisterContents(editor,offset);
			register.put(key, contents);
		}
	}

	/**
	 * Insert the position in the register
	 * 
	 * @param key - register id
	 * @param path - the resource path
	 * @param offset - the offset in the part
	 */
	public void put(String key, IFile path, int offset) {
		RegisterContents contents = clearLocation(key);
		if (contents != null) {
			IRegisterLocation loc = contents.getLocation();
			if (loc != null) {
				loc.setPath(path);
				loc.setOffset(offset);
			} else {
				contents.setLocation(new RegisterLocation(path,offset));
			}
		} else {
			contents = new RegisterContents(path,offset);
			register.put(key, contents);
		}
	}
	
	/**
	 * Get the location from the register
	 * 
	 * @param key - register id
	 * @return the location or null if no register or location
	 */
	public IRegisterLocation getLocation(String key) {
		IRegisterLocation result = null;
		IRegisterContents contents = register.get(key); 
		if (contents != null) {
			result = contents.getLocation();
		}
		return result;
	}
	
	/**
	 * Increment the number in the register by one
	 * 
	 * @param key - register id
	 * @return the new number, or null if register did not contain a number
	 */
	public Integer increment(String key, int incr) {
		Integer number = getNumber(key);
		if (number != null) {
			number = new Integer(number + incr);
			put(key,number);
		}
		return number;
	}
	
	/**
	 * Get the raw contents of the register
	 * 
	 * @param key - register id
	 * @return and IRegisterContents object or null
	 */
	public IRegisterContents getContents(String key) {
		return register.get(key);
	}

	private IFile convertToPath(IEditorPart editor) {
		IFile path = null;
		if (editor != null && editor instanceof IEditorPart) {
			IEditorInput input = ((IEditorPart) editor).getEditorInput();
			if (input instanceof IFileEditorInput) {
				path = ((IFileEditorInput) input).getFile();
			}
		}
		return path;
	}
	
	private RegisterContents clearLocation(String key) {
		// make sure the part and any listeners are removed when location is changed
		IRegisterContents contents = register.get(key);
		if (contents != null) {
			if (contents.getLocation() != null) {
				contents.getLocation().setPosition(null,null);
			}
		}
		return (RegisterContents)contents;
	}
	
	/**
	 * inner class to hold the register contents
	 * Can hold a location and either text or a number
	 */
	private class RegisterContents implements IRegisterContents {

		private Integer number = null;
		private String text = null;
		private String[] rectangle = null;
		private IRegisterLocation location;
		
		public RegisterContents(String text) {
			this.text = text;
		}
		
		public RegisterContents(String[] rectangle) {
			this.rectangle = rectangle;
		}
		
		public RegisterContents(Integer number) {
			this.number = number;
		}
		
		public RegisterContents(ITextEditor editor, int offset) {
			this.location = new RegisterLocation(editor,offset);
		}
		
		public RegisterContents(IFile path, int offset) {
			this.location = new RegisterLocation(path,offset);
		}
		
		/**
		 * @see com.mulgasoft.emacsplus.IRegisterContents#getNumber()
		 */
		public Integer getNumber() {
			return number;
		}
		
		void setNumber(Integer number) {
			if (number != null) {
				setText(null);
				setRectangle(null);
			}
			this.number = number;
		}

		/**
		 * @see com.mulgasoft.emacsplus.IRegisterContents#getText()
		 */
		public String getText() {
			return text;
		}
		
		void setText(String text) {
			if (text != null) {
				setNumber(null);
				setRectangle(null);
			}
			this.text = text;
		}
		
		public String[] getRectangle() {
			return rectangle;
		}
		
		void setRectangle(String[] rectangle) {
			if (rectangle != null) {
				setNumber(null);
				setText(null);
			}
			this.rectangle = rectangle;
		}
		
		public IRegisterLocation getLocation() {
			return location;
		}
		
		void setLocation(IRegisterLocation location) {
			this.location= location;
		}
		
		public String toString() {
			StringBuilder results = new StringBuilder();
				results.append(CR);
			if (number != null) {
				results.append('<');
				results.append(NUMBER);
				results.append('>');
				results.append(' ');
				results.append(number);
				results.append(CR);
			} else if (text != null) {
				results.append('<');
				results.append(TEXT);
				results.append('>');
				results.append(CR);
				results.append('\"');
				results.append(text);
				results.append('\"');
				results.append(CR);
			} else if (rectangle != null) {
				results.append('<');
				results.append(RECTANGLE);
				results.append('>');
				results.append(CR);
				for (String txt : rectangle) {
					results.append('\"');
					results.append(txt);
					results.append('\"');
					results.append(CR);
				}
				results.append(CR);
			}

			if (location != null) {
				results.append('<');
				results.append(POINT);
				results.append('>');
				results.append(' ');
				results.append(location.toString());
				results.append(CR);
			} 
			
			return results.toString();
		}
	}
	
	private class RegisterLocation implements IRegisterLocation {

		private IFile path = null;
		private ITextEditor editor = null;
		private Position position = null;
		private EditorListener listener = null;
		
		RegisterLocation(ITextEditor editor, int offset) {
			this.setPosition(editor,offset);
		}
		
		RegisterLocation(IFile path, int offset) {
			setPath(path);
			this.position = new Position(offset,0);
		}

		/**
		 * @see com.mulgasoft.emacsplus.IBufferLocation#getOffset()
		 */
		public int getOffset() {
			return (position != null ? position.getOffset() : 0);
		}

		public ITextEditor getEditor() {
			return editor;
		}

		/**
		 * @see com.mulgasoft.emacsplus.IBufferLocation#getPath()
		 */
		public IFile getPath() {
			return path;
		}

		/**
		 * @see com.mulgasoft.emacsplus.IBufferLocation#setOffset(int)
		 */
		public void setOffset(int offset) {
			if (position != null) {
				position.setOffset(offset);
			} else {
				position = new Position(offset,0);
			}
		}

		
		/**
		 * @see com.mulgasoft.emacsplus.IBufferLocation#setOffset(int)
		 */
		public void setPosition(ITextEditor editor,Position position) {
			if (this.editor != null && this.position != null) {
				// remove the old position from the document updater
				removePosition(this.editor,this.position);
			}
			this.editor = editor;
			this.position = position;
			if (position != null && editor != null) {
				// add it to the document updater
				addPosition(editor,position);
			}
		}

		public void setPosition(ITextEditor editor,int offset) {
			this.setPosition(editor,new Position(offset,0));
		}
		
		public Position getPosition() {
			return position;
		}
		
		private void addPosition(ITextEditor editor, Position position) {
			addListener();
			IDocument document = editor.getDocumentProvider().getDocument(editor.getEditorInput());
			// add position to document
			try {
				if (!document.containsPositionCategory(MarkRing.MARK_CATEGORY)) {
					document.addPositionCategory(MarkRing.MARK_CATEGORY);
					document.addPositionUpdater(MarkRing.updater);
				}
				document.addPosition(MarkRing.MARK_CATEGORY, position);
			} catch (BadLocationException e) {
			} catch (BadPositionCategoryException e) {
			}
		}
		
		private void removePosition(ITextEditor editor, Position position) {
			removeListener();
			IDocument document = editor.getDocumentProvider().getDocument(editor.getEditorInput());
			try {
				document.removePosition(MarkRing.MARK_CATEGORY,position);
			} catch (BadPositionCategoryException e) {
			}			
		}
		
		/**
		 * @see com.mulgasoft.emacsplus.IBufferLocation#setPath(java.lang.String)
		 */
		public void setPath(IFile path) {
			this.path = path;
			if (this.path != null) {
				setEditor(null);
			}
		}

		/**
		 * @see com.mulgasoft.emacsplus.IBufferLocation#setEditor(org.eclipse.ui.texteditor.ITextEditor)
		 */
		public void setEditor(ITextEditor editor) {
			removeListener();
			this.editor = editor;
			if (this.editor != null) {
				setPath(null);
				addListener();
			}
		}
		
		private void addListener() {
			listener = new EditorListener(this);
			listener.addListener(listener);
		}
		
		private void removeListener() {
			if (listener != null) { 
				listener.removeListener(listener);
				clearListener();
			}			
		}
		
		void clearListener() {
			listener = null;
		}
		
		public String toString() {
			String result = null;
			if (getPosition() != null) {
				if (getEditor() != null) {
					result = getEditor().getTitle() + ' ' + getPosition().getOffset();
				} else if (getPath() != null) {
					result = getPath().getFullPath().toString() + ' ' + getPosition().getOffset();
				}
			}
			return result;
		}
	}
	
	// And now for the listener
	private class EditorListener implements IPartListener2 { 

		RegisterLocation location = null;
		EditorListener(RegisterLocation location) {
			this.location = location;
		}
		
		public void removeListener(IPartListener2 listener) {
			IWorkbenchPage page = EmacsPlusUtils.getWorkbenchPage(); 
			if (page != null && location != null) {
				page.removePartListener(listener);
			}
			location.clearListener();
			location = null;
		}
		
		public void addListener(IPartListener2 listener) {
			IWorkbenchPage page = EmacsPlusUtils.getWorkbenchPage(); 
			if (page != null) {
				page.addPartListener(listener);
			}
		}
		
		/* (non-Javadoc)
		 * @see org.eclipse.ui.IPartListener2#partClosed(org.eclipse.ui.IWorkbenchPartReference)
		 */
		public void partClosed(IWorkbenchPartReference partRef) {
			
			if (partRef instanceof IEditorReference) {
				IEditorPart epart = ((IEditorReference) partRef).getEditor(false);
				ITextEditor editor = (location != null ? location.getEditor() : null);
				if (editor == EmacsPlusUtils.getTextEditor(epart, false)) {
					RegisterLocation loc = location;
					// we're out of here
					removeListener(this);
					IDocument document = editor.getDocumentProvider().getDocument(editor.getEditorInput());
					// remove position category, if still present
					if (document.containsPositionCategory(MarkRing.MARK_CATEGORY)) {
						try {
							document.removePositionUpdater(MarkRing.updater);
							document.removePositionCategory(MarkRing.MARK_CATEGORY);
						} catch (BadPositionCategoryException e) {
						}
					}
					// convert to path
					loc.setPath(convertToPath(editor));
				}
			}
		}

		public void partActivated(IWorkbenchPartReference partRef) {}

		public void partBroughtToTop(IWorkbenchPartReference partRef) {}

		public void partDeactivated(IWorkbenchPartReference partRef) {}

		public void partHidden(IWorkbenchPartReference partRef) {}

		public void partInputChanged(IWorkbenchPartReference partRef) {}

		public void partOpened(IWorkbenchPartReference partRef) {}

		public void partVisible(IWorkbenchPartReference partRef) {}
	}
}
