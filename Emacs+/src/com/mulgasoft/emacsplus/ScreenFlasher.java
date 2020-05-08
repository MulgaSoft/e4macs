/**
 * Copyright (c) 2009-2020 Mark Feber, MulgaSoft
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package com.mulgasoft.emacsplus;

import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Display;

/**
 * Flash the editor window once
 * 
 * @author mfeber
 */
public class ScreenFlasher implements Flasher {
	
	private static int flashCount = 1;
	private static int waitTime = 25;
	
	private Color flash = null;
	private Color background = null;
	
	private static ScreenFlasher instance;
	private ScreenFlasher() {}
	
	/**
	 * Singleton pattern
	 * 
	 * @return the cached instance
	 */
	public static ScreenFlasher getInstance() {
		if (instance == null) {
			instance = new ScreenFlasher();
		}
		return instance;
	}

	public static boolean ring() {
		getInstance().flash(Display.getCurrent());
		return true;
	}

	private void getColors(StyledText widget) {
		background = widget.getBackground();
		if (background != null) {
			flash = Flasher.invertColor(background);
		}
	}

	public void flash(Display display) {
		StyledText widget = MarkUtils.getStyledWidget(EmacsPlusUtils.getCurrentEditor());
		getColors(widget);
		if (background != null) {
			Display.getDefault().asyncExec(() -> {
				runFlasher(flashCount, widget);
			});
		}
	}
	
	private void runFlasher(final int count, final StyledText item) {
		if (count > 0) {
			Display.getDefault().asyncExec(() -> {
				item.setBackground(flash);					
				item.setVisible(true);
				item.redraw();
				item.update();
				Display.getDefault().syncExec(() -> {
					try {
						Thread.sleep(waitTime);
					} catch (InterruptedException e) {}
					item.setBackground(background);					
					item.redraw();
					item.update();
					runFlasher(count -1, item);
				});
			});
		} 
	}

}
