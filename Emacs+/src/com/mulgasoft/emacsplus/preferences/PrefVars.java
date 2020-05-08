/**
 * Copyright (c) 2009-2020 Mark Feber, MulgaSoft
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package com.mulgasoft.emacsplus.preferences;

import static com.mulgasoft.emacsplus.EmacsPlusUtils.getPreferenceStore;
import static com.mulgasoft.emacsplus.preferences.EmacsPlusPreferenceConstants.P_CLIP_SEXP;
import static com.mulgasoft.emacsplus.preferences.EmacsPlusPreferenceConstants.P_CLIP_WORD;
import static com.mulgasoft.emacsplus.preferences.EmacsPlusPreferenceConstants.P_DOT_SEXP;
import static com.mulgasoft.emacsplus.preferences.EmacsPlusPreferenceConstants.P_FRAME_DEF;
import static com.mulgasoft.emacsplus.preferences.EmacsPlusPreferenceConstants.P_FRAME_INIT;
import static com.mulgasoft.emacsplus.preferences.EmacsPlusPreferenceConstants.P_GNU_SEXP;
import static com.mulgasoft.emacsplus.preferences.EmacsPlusPreferenceConstants.P_REPLACED_TOKILL;
import static com.mulgasoft.emacsplus.preferences.EmacsPlusPreferenceConstants.P_RING_SIZE;
import static com.mulgasoft.emacsplus.preferences.EmacsPlusPreferenceConstants.P_SPLIT_SELF;
import static com.mulgasoft.emacsplus.preferences.EmacsPlusPreferenceConstants.P_UNDER_SEXP;
import static com.mulgasoft.emacsplus.preferences.EmacsPlusPreferenceConstants.PV_FLASH_MODE_LINE;
import static com.mulgasoft.emacsplus.preferences.EmacsPlusPreferenceConstants.PV_SCROLL_MARGIN;

import java.util.SortedMap;
import java.util.TreeMap;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.graphics.Rectangle;

import com.mulgasoft.emacsplus.Beeper;
import com.mulgasoft.emacsplus.EmacsPlusActivator;
import com.mulgasoft.emacsplus.ModeLineFlasher;
import com.mulgasoft.emacsplus.ScreenFlasher;

/**
 * Define selected internal/preference variables in an enum
 * 
 * @author Mark Feber - initial API and implementation
 */
public enum PrefVars {

	DELETE_SEXP_TO_CLIPBOARD(Ptype.BOOLEAN, P_CLIP_SEXP, true),
	DELETE_WORD_TO_CLIPBOARD(Ptype.BOOLEAN, P_CLIP_WORD, false),
	ENABLE_DOT_SEXP(Ptype.BOOLEAN, P_DOT_SEXP, true),
	ENABLE_GNU_SEXP(Ptype.BOOLEAN, P_GNU_SEXP, true),
	ENABLE_SPLIT_SELF(Ptype.BOOLEAN, P_SPLIT_SELF, true),
	ENABLE_UNDER_SEXP(Ptype.BOOLEAN, P_UNDER_SEXP, false),
	FRAME_DEF(Ptype.RECT, P_FRAME_DEF, PRect.DEFAULT),
	FRAME_INIT(Ptype.RECT, P_FRAME_INIT, PRect.DEFAULT),
	KILL_RING_MAX(Ptype.INTEGER, P_RING_SIZE, 60),
	KILL_WHOLE_LINE(Ptype.BOOLEAN, false),
	REPLACE_TEXT_TO_KILLRING(Ptype.BOOLEAN, P_REPLACED_TOKILL, false),
	RING_BELL_FUNCTION(Ptype.STRING, RingBellOptions.nil),
	SCROLL_MARGIN(Ptype.P_INTEGER, PV_SCROLL_MARGIN, 0),
	SEARCH_EXIT_OPTION(Ptype.STRING, SearchExitOption.t),
	SHOW_OTHER_HORIZONTAL(Ptype.BOOLEAN, false),
	SETQ(Ptype.BOOLEAN, true),
	VISIBLE_BELL(Ptype.BOOLEAN, false),	
	;
	
	private final static String DOC = "_DOC";	//$NON-NLS-1$
	private final static String DASH = "-";		//$NON-NLS-1$
	private final static String UDASH = "_";	//$NON-NLS-1$
	private Ptype type;
	private String prefName;
	private String dispName;
	private Object defVal;
	private DisplayOption defOption;
	
	private PrefVars(Ptype type, Object defVal) {
		this(type,null,null,defVal);
	}
	
	private <T extends Enum<T>> PrefVars(Ptype type, DisplayOption defOption) {
		this(type,defOption.toString());
		this.defOption = defOption;
	}

	private <T extends Enum<T>> PrefVars(Ptype type, DisplayOption defOption, Object defVal) {
		this(type,defVal);
		this.defOption = defOption;
	}
	
	private PrefVars(Ptype type, String prefName, Object defVal) {
		this(type,prefName,null,defVal);
	}

	private PrefVars(Ptype type, String prefName, String dispName, Object defVal) {		
		this.prefName = prefName;
		this.dispName = dispName;
		this.type = type;
		this.defVal = defVal;
	}

	public static SortedMap<String, PrefVars> getCompletions(boolean withSetq) {
		SortedMap<String, PrefVars> result = new TreeMap<String, PrefVars>();
		for (PrefVars q : PrefVars.values()) {
			if (withSetq || q != SETQ) {
				result.put(q.getDisplayName(), q);
			}
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
	
	public boolean isEnum() {
		return defOption != null;
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
		case P_INTEGER:
			Integer iVal = null;
			if (val instanceof Integer) {
				iVal = (Integer)val;
			} else if (val instanceof String) {
				try {
					iVal = Integer.parseInt((String) val);
				} catch (NumberFormatException e) {}
			} 
			if (iVal != null) {
				if ((type == Ptype.P_INTEGER) && iVal < 0) {
					Beeper.beep();
				} else {
					store.setValue(getPref(),iVal);
				}
			} else {
				Beeper.beep();
			}
			break;
		case RECT: 
			if (val instanceof String && PRect.parseRect((String)val) != null) {
				store.setValue(getPref(), (String) val);
			} else {
				Beeper.beep();
			}
			break;
		case STRING:
			if (val instanceof String) {
				String newVal = (String) val;
				if (isEnum()) {
					newVal = newVal.replaceAll(DASH, UDASH);
				}
				store.setValue(getPref(), newVal);
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
			case RECT:
			case STRING:
				result = store.getString(getPref());
				break;
			case INTEGER:
			case P_INTEGER:
				result = store.getInt(getPref());
				break;
			default:
				break;
		}
		return result;
	}

	public Object getDisplayValue() {
		if (isEnum()) {
			IPreferenceStore store = getPreferenceStore();
			String name = store.getString(getPref());
			return ((DisplayOption)defOption.getValue(name)).getDisplayName();
		} else { 
			return getValue();
		}
	}
	public enum Ptype {
		BOOLEAN, INTEGER, P_INTEGER, RECT, STRING;
	}
	
	public interface DisplayOption {
		public String getDisplayName();
		public Enum<? extends DisplayOption> getValue(String name);
	}
	
	public enum RingBellOptions implements DisplayOption {
		nil, 
		ignore,
		flash_mode_line(PV_FLASH_MODE_LINE),
		;

		private String displayName = null;
		private RingBellOptions() {}
		private RingBellOptions(String displayName) {
			this.displayName = displayName;
		}

		public boolean ringBell() {
			boolean result = false;
			switch (this) {
				case ignore:
					result = true;
					break;
				case flash_mode_line:
					result = ModeLineFlasher.ring();
					break;
				default:
					break;
			}
			return result;
		}

		public String getDisplayName() {
			return (displayName != null ? displayName : toString());
		}
		
		public Enum<RingBellOptions> getValue(String name) {
			return valueOf(name);
		}
	}
	
	public enum SearchExitOption implements DisplayOption {
		disable, nil, t;
		public String getDisplayName() {
			return toString();
		}
		
		public Enum<SearchExitOption> getValue(String name) {
			return valueOf(name);
		}
	}
	
	public String[] getPossibleValues() {
		String[] result = null;
		if (defOption != null){
			DisplayOption[] enumConstants = defOption.getClass().getEnumConstants();
			if (enumConstants != null) {
				result = new String[enumConstants.length];
				for (int i = 0; i < enumConstants.length; i ++ ) {
					result[i] = enumConstants[i].getDisplayName();
				}
			}
		}
		return result;
	}
	
	public static class PRect  {

		private final static String DEFAULT = "0,0,0,0";	//$NON-NLS-1$
		private static int MIN_RECT_SIZE = 100;

		public static Rectangle parseRect(String rect) {
			Rectangle r = null;
			try {
				String tokens[] = rect.split(",");			//$NON-NLS-1$
				if (tokens.length == 4) {
					int ints[] = new int[4];
					for (int i = 0; i < 4; i++) {
						int x = Integer.parseInt(tokens[i]);
						if (x >= 0) {
							if (i > 1 && x != 0 && x < MIN_RECT_SIZE) {
								return r;
							}
							ints[i]	= x;					
						} else {
							return r;
						}
					}
					r = new Rectangle(ints[0],ints[1],ints[2],ints[3]);
				}
			} catch (Exception e) {
				// bad entry
			}
			return r;
		}

	}

}
