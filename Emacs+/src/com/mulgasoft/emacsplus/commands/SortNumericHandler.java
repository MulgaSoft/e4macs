/**
 * Copyright (c) 2009, 2010 Mark Feber, MulgaSoft
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package com.mulgasoft.emacsplus.commands;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Implement sort-numeric-fields
 * 
 * Sort lines in region numerically by the ARGth field of each line.
 * Fields are separated by whitespace and numbered from 1 up.
 * Specified field must contain the field in each line of the region.
 * The field may begin with "0x" or "0" for hexadecimal and octal values.
 * With a negative ARG, sorts by the ARGth field counted from the right.
 * 
 * @author Mark Feber - initial API and implementation
 */
public class SortNumericHandler extends FieldsHandler {

	private final static String NUMBER_EXP = "-?(0x)[0-9a-f]+|-?(0)[0-7]+|-?[0-9]+";  	   //$NON-NLS-1$
	private final static Pattern pat = Pattern.compile(NUMBER_EXP);
	
	protected FieldsCompare getComparable(String txt, String field) {
		return new Numerical(txt,field);
	}
	
    private  class Numerical extends FieldsCompare {
    	
    	private String text;
    	private Integer value;
    	
    	public Numerical(String txt, String num) {
			text = txt;
			value = 0;	// default if no/bad number
			int neg = 1;
			try {
				Matcher match = pat.matcher(num);
				if (match.find()) {
					String g;
					int val = 0;
					num = num.substring(match.start(),match.end());
					// advance over '-' as -0x123 doesn't parse
					if (num.length() > 0 && num.codePointAt(0) == '-') {
						num = num.substring(1);
						neg = -neg;
					}
					if ((g = match.group(1)) != null) {
						val = Integer.parseInt(num.substring(g.length()),16);
					} else if ((g = match.group(2)) != null) {
						val = Integer.parseInt(num.substring(g.length()),8);
					} else {
						val = Integer.parseInt(num);
					}
					value = neg * val;
				}
			} catch (Exception e) {}
		}

		public String toString() {
		 	return text;
		}
		
		public Integer getValue() {
			return value;
		}

		/**
		 * @see java.lang.Comparable#compareTo(java.lang.Object)
		 */
		public int compareTo(FieldsCompare o) {
			if (o instanceof Numerical) {			
				return value.compareTo(((Numerical)o).getValue());
			} else {
				return -1;
			}
		}
    }

}
