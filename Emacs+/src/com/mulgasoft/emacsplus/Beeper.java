/**
 * Copyright (c) 2009-2011 Mark Feber, MulgaSoft
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package com.mulgasoft.emacsplus;

import static com.mulgasoft.emacsplus.EmacsPlusUtils.getPreferenceBoolean;
import static com.mulgasoft.emacsplus.EmacsPlusUtils.getPreferenceStore;
import static com.mulgasoft.emacsplus.preferences.PrefVars.RING_BELL_FUNCTION;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.ui.PlatformUI;

/**
 * Add interrupt behavior to beep() for kbd macro interrupts
 * 
 * @author Mark Feber - initial API and implementation
 */
public class Beeper {
	
	// A global, sticky variable to enable/disable the bell noise, set to true to disable bell
	private static boolean BELL_OFF = getPreferenceBoolean(RING_BELL_FUNCTION.getPref());
	
	static {
		// listen for changes in the property store
		getPreferenceStore().addPropertyChangeListener(
				new IPropertyChangeListener() {
					public void propertyChange(PropertyChangeEvent event) {
						if (RING_BELL_FUNCTION.getPref().equals(event.getProperty())) {
							setRingBell((Boolean)event.getNewValue());
						}
					}
				}
		);
	}

	public static void setRingBell(boolean offon) {
		Beeper.BELL_OFF = offon;
	}
	
	// temporarily disable the beep by setting to false
	private static boolean beepon = true;
	
	/**
	 * @return the enabled state of the beeper
	 */
	public static boolean isBeepon() {
		return beepon;
	}

	/**
	 * Change the enabled state of the beeper
	 * 
	 * @param beepon
	 */
	public static void setBeepon(boolean beepon) {
		Beeper.beepon = beepon;
	}

	private static Set<IBeepListener> interruptListeners = new HashSet<IBeepListener>();
	
	public static void beep() {
		Beeper.interrupt();
		try {
			if (!BELL_OFF && beepon) {
				PlatformUI.getWorkbench().getDisplay().beep();
			}
		} catch (Exception e) {}
	}

	public static void addBeepListener(IBeepListener beeper) {
		interruptListeners.add(beeper);	
	}
	
	public static void removeBeepListener(IBeepListener beeper) {
		interruptListeners.remove(beeper);	
	}

	public static void interrupt() {
		try {
			for (IBeepListener beeper : interruptListeners) {
				beeper.beepInterrupt();
			}
		} catch (Exception e) {}
	}
}
