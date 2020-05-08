/**
 * Copyright (c) 2009-2020 Mark Feber, MulgaSoft
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package com.mulgasoft.emacsplus;

import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;

/**
 * Common flasher code
 * 
 * @author mfeber
 */
public interface Flasher {
	
	public void flash(Display display);
	
	static Color invertColor(Color c) {
		RGB rgb = c.getRGB();
		return new Color(c.getDevice(), 255 - rgb.red, 255 - rgb.green, 255 - rgb.blue);
	}

}
