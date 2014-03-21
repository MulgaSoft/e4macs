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

import java.util.Hashtable;
import java.util.Map;

import org.eclipse.jface.text.IRegion;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.texteditor.ITextEditor;

/**
 * set, get, kill, killAll, variables, hasVariable
 * 
 * @author Mark Feber - initial API and implementation
 */
public class BufferLocal {
	
	public static final String NARROW_REGION = "narrow"; 	//$NON_NLS_1$
	
	private static Map<ITextEditor,Map<String, Object>> bufferlocal = new Hashtable<ITextEditor,Map<String,Object>>();
	private static BufferLocal instance;
	
	/**
	 * Singleton 
	 */
	private BufferLocal() {};
	
	public static BufferLocal getInstance() {
		if (instance == null) {
			instance = new BufferLocal();
		}
		return instance;
	}

	public void set(ITextEditor editor, String variable, Object value) {
		if (editor != null && variable != null && value != null) {
			Map<String, Object> table = bufferlocal.get(editor);
			if (table == null) {
				table = new Hashtable<String, Object>();
				bufferlocal.put(editor, table);
			}
			table.put(variable, value);
		}
	}
	
	public Object get(ITextEditor editor, String variable) {
		Object result = null;
		if (editor != null && variable != null) {
			Map<String, Object> table = bufferlocal.get(editor);
			if (table != null) {
				result = table.get(variable);
			}
		}		
		return result;
	}
	
	public boolean hasVariable(IEditorPart editor, String variable) {
		boolean result = false;
		if (editor != null && variable != null) {
			Map<String, Object> table = bufferlocal.get(editor);
			if (table != null) {
				result = table.containsKey(variable);
			}
		}		
		
		return result;
	}

	public boolean kill(ITextEditor editor, String variable) {
		boolean result = true;
		if (editor != null && variable != null) {
			Map<String, Object> table = bufferlocal.get(editor);
			if (table != null) {
				result = table.containsKey(variable);
				if (result) {
					table.remove(variable);
				}
			}
		}		
		
		return result;
	}
	
	public boolean killAll(ITextEditor editor) {
		boolean result = true;
		
		if (editor != null) {
			Map<String, Object> table = bufferlocal.get(editor);
			if (table != null) {
				result = true;
				bufferlocal.remove(editor);
			}
		}		
		return result;
	}
	
	/************ For testing ****************/
	
	public void handleActivate(IEditorPart epart) {
		if (epart instanceof ITextEditor) {
			handleNarrowActivate((ITextEditor)epart);
		}
	}
	
	public void handleDeactivate(IEditorPart epart) {
		if (hasVariable(epart, NARROW_REGION)) {
			ITextEditor editor = (ITextEditor)epart;
			IRegion region = (IRegion)get(editor, NARROW_REGION);				
			IRegion cregion = editor.getHighlightRange();
			if (!region.equals(cregion)) {
				set(editor,NARROW_REGION,cregion);
			}
		}
	}
	
	private void handleNarrowActivate(ITextEditor editor) {
		IRegion region = (IRegion)get(editor, NARROW_REGION);	
		if (region != null) {
			IRegion cregion = editor.getHighlightRange();
			if (!region.equals(cregion)) {
				// if (java) global flag is set, and used from outline
				// permit the new range to override the narrowed range
				region = cregion;
			}
			// narrow to selection
			editor.resetHighlightRange();
			editor.showHighlightRangeOnly(true);
			editor.setHighlightRange(region.getOffset(), region.getLength(), true);
		}
	}
}
