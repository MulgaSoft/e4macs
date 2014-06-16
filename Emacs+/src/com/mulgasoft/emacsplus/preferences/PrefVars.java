/**
 * Copyright (c) 2009-2014 Mark Feber, MulgaSoft
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package com.mulgasoft.emacsplus.preferences;

import static com.mulgasoft.emacsplus.EmacsPlusUtils.getPreferenceStore;
import static com.mulgasoft.emacsplus.preferences.EmacsPlusPreferenceConstants.P_DOT_SEXP;
import static com.mulgasoft.emacsplus.preferences.EmacsPlusPreferenceConstants.P_GNU_SEXP;
import static com.mulgasoft.emacsplus.preferences.EmacsPlusPreferenceConstants.P_REPLACED_TOKILL;
import static com.mulgasoft.emacsplus.preferences.EmacsPlusPreferenceConstants.P_SPLIT_SELF;
import static com.mulgasoft.emacsplus.preferences.EmacsPlusPreferenceConstants.P_UNDER_SEXP;

import static com.mulgasoft.emacsplus.preferences.EmacsPlusPreferenceConstants.P_CLIP_WORD;
import static com.mulgasoft.emacsplus.preferences.EmacsPlusPreferenceConstants.P_CLIP_SEXP;
import static com.mulgasoft.emacsplus.preferences.EmacsPlusPreferenceConstants.P_RING_SIZE;

import java.util.SortedMap;
import java.util.TreeMap;

import org.eclipse.jface.preference.IPreferenceStore;

import com.mulgasoft.emacsplus.Beeper;
import com.mulgasoft.emacsplus.EmacsPlusActivator;

/**
 * Define selected internal/preference variables in an enum
 * 
 * @author Mark Feber - initial API and implementation
 */
public enum PrefVars {

	KILL_RING_MAX(P_RING_SIZE, Ptype.INTEGER, 60),
	DELETE_WORD_TO_CLIPBOARD(P_CLIP_WORD, Ptype.BOOLEAN, false),
	DELETE_SEXP_TO_CLIPBOARD(P_CLIP_SEXP, Ptype.BOOLEAN, true),
	REPLACE_TEXT_TO_KILLRING(P_REPLACED_TOKILL, Ptype.BOOLEAN, false),
	ENABLE_SPLIT_SELF(P_SPLIT_SELF, Ptype.BOOLEAN, true),
	ENABLE_GNU_SEXP(P_GNU_SEXP,Ptype.BOOLEAN,true),
	ENABLE_DOT_SEXP(P_DOT_SEXP,Ptype.BOOLEAN,true),
	ENABLE_UNDER_SEXP(P_UNDER_SEXP,Ptype.BOOLEAN,false),
	KILL_WHOLE_LINE(Ptype.BOOLEAN, false),
	RING_BELL_FUNCTION(Ptype.BOOLEAN, false),
	SHOW_OTHER_HORIZONTAL(Ptype.BOOLEAN, false),
//	SEARCH_EXIT_OPTION(Ptype.STRING, SEOptions.class, SEOptions.t.toString());
	;
	
	private final static String DOC = "_DOC";  //$NON-NLS-1$
	private final static String DASH = "-";  //$NON-NLS-1$
	private final static String UDASH = "_"; //$NON-NLS-1$
	private String prefName;
	private String dispName;
	private Object defVal;
	private Ptype type;
	private Class e_class;
	
	private PrefVars(Ptype type, Object defVal) {
		this(null,null,type,defVal);
	}
	
	private PrefVars(Ptype type, Class values, Object defVal) {
		this(type,defVal);
		e_class = values;
	}
	
	private PrefVars(String prefName, Ptype type, Object defVal) {
		this(prefName,null,type,defVal);
	}

	private PrefVars(String prefName, String dispName, Ptype type, Object defVal) {		
		this.prefName = prefName;
		this.dispName = dispName;
		this.type = type;
		this.defVal = defVal;
	}
	
	private PrefVars(String prefName, String dispName, Class values, Object defVal) {		
		// a string type can have a set of values represented by an enum
		this(prefName,dispName,Ptype.STRING,defVal);
		this.e_class = values;
	}
	
	public static SortedMap<String, PrefVars> getCompletions() {
		SortedMap<String, PrefVars> result = new TreeMap<String, PrefVars>();
		for (PrefVars q : PrefVars.values()) {
			result.put(q.getDisplayName(), q);
		}
		return result;
	}

	public String getPref() {
		return (prefName == null ? name() : prefName);
	}
	
	public String getDisplayName() {
		if (dispName == null) {
			dispName = name().toLowerCase().replaceAll(UDASH,DASH);
		}
		return dispName;
	}

	public String getDescription() {
		return EmacsPlusActivator.getResourceString(this.name() + DOC);
	}
	
	public Object getDefault() {
		return defVal;
	}

	public Ptype getType() {
		return type;
	}

	/**
	 * Set the current value of the preference in the preference store
	 * 
	 * @param val the Object representing the value - converted to correct type based on the preference definition
	 */
	public void setValue(Object val) {
		IPreferenceStore store = getPreferenceStore();
		switch (type) {
			case BOOLEAN:
				if (val instanceof Boolean) {
					store.setValue(getPref(), (Boolean) val);
				} else {
					Beeper.beep();
				}
				break;
			case INTEGER:
				if (val instanceof Integer) {
					store.setValue(getPref(), (Integer) val);
				} else if (val instanceof String) {
					try {
						Integer iv = Integer.parseInt((String) val);
						store.setValue(getPref(), iv);
					} catch (NumberFormatException e) {
						Beeper.beep();
					}
				} else {
					Beeper.beep();
				}
				break;
			case STRING:
				if (val instanceof String) {
					store.setValue(getPref(), (String) val);
				} else {
					Beeper.beep();
				}
				break;
			default:
				break;
		}
	}

	/**
	 * Get the current value of the preference from the preference store
	 * 
	 * @return the Object representing the value
	 */
	public Object getValue() {
		Object result = null;
		IPreferenceStore store = getPreferenceStore();
		switch (type) {
			case BOOLEAN:
				result = store.getBoolean(getPref());
				break;
			case STRING:
				result = store.getString(getPref());
				break;
			case INTEGER:
				result = store.getInt(getPref());
				break;
			default:
				break;
		}
		return result;
	}

	public enum Ptype {
		BOOLEAN, INTEGER, STRING;
	}
	
	public enum SEOptions {
		t, nil, disable;
	}
	
	public String[] getPossibleValues() {
		String[] result = null;
		if (e_class != null){
			Object[] enumConstants = e_class.getEnumConstants();
			if (enumConstants != null) {
				result = new String[enumConstants.length];
				for (int i = 0; i < enumConstants.length; i ++ ) {
					result[i] = enumConstants[i].toString();
				}
			}
		}
		return result;
	}

}
