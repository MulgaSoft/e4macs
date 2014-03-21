/**
 * Copyright (c) 2009, 2010 Mark Feber, MulgaSoft
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 */
package com.mulgasoft.emacsplus.minibuffer;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentExtension4;

/**
 * Wrap up a StringBuilder with some additional functionality
 * 
 * @author Mark Feber - initial API and implementation
 */
public class MinibufferImpl {
	
	private StringBuilder minibufferString = new StringBuilder();
	private int cpos = -1;
	private int[] eolcp = null;
	private String eol;
	private boolean lowercase = false;
	
	public MinibufferImpl(IDocument document) {
		setEolChars(document);
	}
	
	public MinibufferImpl(IDocument document, boolean lowercase) {
		this(document);
		this.setLowercase(lowercase);
	}
	
	public int[] getEolChars() {
		return eolcp;
	}
	
	public String getEol() {
		return eol;
	}
	
	private void setEolChars(IDocument document) {
		try {
			if (document instanceof IDocumentExtension4) {
				eol = ((IDocumentExtension4)document).getDefaultLineDelimiter();
			} else {
				 eol = document.getLineDelimiter(0);
			}
			int eolLen = eol.length();
			eolcp = new int[eolLen];
			for (int i=0; i<eolLen; i++) {
				eolcp[i] = eol.codePointAt(i);
			}
		} catch (BadLocationException e) {
		}
	}

	/**	
	 * @return the lower case
	 */
	private boolean isLowercase() {
		return lowercase;
	}
	/**
	 * force lower case if true
	 * 
	 * @param lowercase 
	 */
	public void setLowercase(boolean lowercase) {
		this.lowercase = lowercase;
	}
	
	/**
	 * Append a string to the contents of the minibuffer
	 * 
	 * @param addStr
	 * @return the resulting string
	 */
	public String append(String addStr) {
		String str = (isLowercase() ? addStr.toLowerCase() : addStr);
		cpos += str.length();
		minibufferString.append(str);
		return minibufferString.toString();
	}

	/**
	 * Append a string to the contents of the minibuffer
	 * 
	 * @param c
	 * @return the resulting string
	 */
	public String addChar(char c) {
		c = (isLowercase() ? Character.toLowerCase(c) : c);
		minibufferString.insert(++cpos,c);
		return minibufferString.toString();
	}

	/**
	 * Backup one 'character' in the minibuffer
	 * Backs over a complete 'eol' which may be more than one character
	 * 
	 * @return the resulting string
	 */
	public String bsChar() {
		int len = minibufferString.length();
		if (len > 0 && cpos > -1) {
			boolean isEol = false;
			int[] eol = getEolChars();
			if (eol.length > 0 && cpos - (eol.length -1) > -1){
				isEol = true;
				for (int j=0; j<eol.length ; j++) {
					if (minibufferString.codePointAt(cpos-j) != eol[eol.length-(j+1)]) {
						isEol = false;
						break;
					}
				}
			}
			if (isEol) {
				for (int i=1; i < eol.length; i++)
					minibufferString.deleteCharAt(cpos--);
			} 
			minibufferString.deleteCharAt(cpos--);
		}
		return minibufferString.toString();
	}
	
	// TODO needs to pay attention to forward delete of eol
	
	public String delChar() {
		int len = minibufferString.length();
		if (len > 0 && cpos < len) {
			minibufferString.deleteCharAt(cpos);
			if (cpos > -1 && cpos == len - 1){
				cpos--;
			}
		}
		return minibufferString.toString();
	}
	
	public String init(String newString) {
		minibufferString = new StringBuilder(newString);
		cpos = minibufferString.length() -1;
		return newString;
	}

	public String getString() {
		return minibufferString.toString();
	}
	
	public int getLength() {
		return minibufferString.length();
	}
	
	public void setLength(int length) {
		minibufferString.setLength(length);
		cpos = length -1;
	}

	public char charAt(int pos) {		
		char result = 0;
		int gpos = (pos < 0 ? cpos + pos + 1 : pos);
		if (-1 < gpos && gpos <= cpos) {
			result = minibufferString.charAt(gpos);
		}
		return result;
	}
	
	public void toBegin() {
		cpos = -1;
	}
	
	public void toEnd() {
		cpos = minibufferString.length() -1;
	}
	
	public void toLeft() {
		if (cpos > 0){
			cpos--;
		}
	}
	
	public void toRight() {
		if (cpos < minibufferString.length() -1){
			cpos++;
		}
	}
}
