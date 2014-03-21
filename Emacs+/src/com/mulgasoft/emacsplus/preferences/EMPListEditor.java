/**
 * Copyright (c) 2009, 2010 Mark Feber, MulgaSoft
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package com.mulgasoft.emacsplus.preferences;

import java.util.ArrayList;
import java.util.StringTokenizer;

import org.eclipse.swt.widgets.Composite;

/**
 * @author Mark Feber - initial API and implementation
 */
public abstract class EMPListEditor extends MListEditor {

	final static String SEPARATOR = ",";		   //$NON-NLS-1$
	final static String DISPLAY_SEPARATOR = " - "; //$NON-NLS-1$

    protected EMPListEditor(String name, String labelText, Composite parent) {
    	super(name,labelText,parent);
    }
    
    /**
     * Turn the comma separated string into an array of names
     * 
     * @param stringList
     * @return array of names
     */
    public static String[] parseResults(String stringList){
        StringTokenizer st = new StringTokenizer(stringList, SEPARATOR);
        ArrayList<String> strs = new ArrayList<String>();
        while (st.hasMoreElements()) {
        	String next = ((String) st.nextElement()).trim(); 
        	if (next.length() > 0) {
        		strs.add(next);
        	}
        }
        return strs.toArray(new String[0]);    	
    }
}
