/**
 * Copyright (c) 2009, 2010 Mark Feber, MulgaSoft
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package com.mulgasoft.emacsplus.commands;

/**
 * Implement: sort-fields
 * 
 * Sort lines in region lexicographically by the ARGth field of each line.
 * Fields are separated by whitespace and numbered from 1 up.
 * With a negative ARG, sorts by the ARGth field counted from the right.
 * 
 * The variable `SortFoldCase' determines whether alphabetic case affects
 * the sort order.
 * 
 * @author Mark Feber - initial API and implementation
 */
public class SortFieldsHandler extends FieldsHandler {

	protected FieldsCompare getComparable(String txt, String field) {
		return new Lexigraphical(txt,(String)field);
	}

	private class Lexigraphical extends FieldsCompare {

		String text;
		String field;
		
		public Lexigraphical(String txt, String fld) {
			text = txt;
			field = fld;
			
		}
		
		public String getField() {
			return field;
		}
		
		public String toString() {
			return text;
		}
		
		/**
		 * @see java.lang.Comparable#compareTo(java.lang.Object)
		 */
		public int compareTo(FieldsCompare o) {
			if (o instanceof Lexigraphical) {
				String fld = ((Lexigraphical) o).getField();
				return (SortFoldCase ? String.CASE_INSENSITIVE_ORDER.compare(field, fld) : field.compareTo(fld));
			} else {
				return -1;
			}
		}
		
	}
}
