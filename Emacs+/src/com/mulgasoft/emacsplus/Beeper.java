/**
 * Copyright (c) 2009-2011 Mark Feber, MulgaSoft
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package com.mulgasoft.emacsplus;

import static com.mulgasoft.emacsplus.EmacsPlusUtils.getPreferenceStore;
import static com.mulgasoft.emacsplus.EmacsPlusUtils.getPreferenceString;
import static com.mulgasoft.emacsplus.preferences.PrefVars.RING_BELL_FUNCTION;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.widgets.Display;

import com.mulgasoft.emacsplus.preferences.PrefVars.RingBellOptions;

/**
 * Add interrupt behavior to beep() for kbd macro interrupts
 * 
 * @author Mark Feber - initial API and implementation
 */
public class Beeper {

	private static RingBellOptions ringer;
	static {
		//initialize the ring bell function from our properties
		setRingBellOption(getPreferenceString(RING_BELL_FUNCTION.getPref()));
		// listen for changes in the property store
		getPreferenceStore().addPropertyChangeListener(
				new IPropertyChangeListener() {
					public void propertyChange(PropertyChangeEvent event) {
						if (RING_BELL_FUNCTION.getPref().equals(event.getProperty())) {
							setRingBellOption((String)event.getNewValue());
						}
					}
				}
		);
	}

	private static boolean ringBell() {
		return (ringer != null ? ringer.ringBell() : false);
	}

	private static void setRingBellOption(String option) {
		ringer = RingBellOptions.valueOf(option);
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
		if (beepon && !ringBell()) {
			Display.getCurrent().beep();
		}
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
