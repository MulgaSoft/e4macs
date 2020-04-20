/**
 * Copyright (c) 2009-2020 Mark Feber, MulgaSoft
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package com.mulgasoft.emacsplus;

import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.StatusLineLayoutData;
import org.eclipse.jface.resource.ColorRegistry;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.texteditor.StatusLineContributionItem;

/**
 * Flashing mode line ring-bell-function
 * 
 * @author mfeber
 */
public class ModeLineFlasher extends StatusItemSupport {
	
	private static ModeLineFlasher instance = null;
	private ModeLineFlasher() {};
	
	/**
	 * Singleton pattern
	 * 
	 * @return the cached instance
	 */
	public static ModeLineFlasher getInstance() {
		if (instance == null) {
			instance = new ModeLineFlasher();
		}
		return instance;
	}

	private static final String FLASH_ID = "flash_mode";     //$NON-NLS-1$

	private static FlashLineContributionItem flashItem = null;
	private static int flashCount = 3;
	private static int waitTime = 25;

	private static String foregroundKey = "org.eclipse.ui.editors.foregroundColor";	//$NON-NLS-1$
	private static String backgroundKey = "org.eclipse.ui.editors.backgroundColor";	//$NON-NLS-1$

	private static Color[] backs = { PlatformUI.getWorkbench().getDisplay().getSystemColor(SWT.COLOR_RED), PlatformUI.getWorkbench().getDisplay().getSystemColor(SWT.COLOR_BLACK)}; 

	public static boolean ring() {
		getInstance().flashModeLine(Display.getCurrent());
		return true;
	}

	private Color invertColor(Color c) {
		RGB rgb = c.getRGB();
		return new Color(c.getDevice(), 255 - rgb.red, 255 - rgb.green, 255 - rgb.blue);
	}

	private void setColors() {
		ColorRegistry colorRegistry = PlatformUI.getWorkbench().getThemeManager().getCurrentTheme().getColorRegistry();
		backs[0] = invertColor(colorRegistry.get(backgroundKey));
		backs[1] = invertColor(colorRegistry.get(foregroundKey));
	}

	private void asyncForce(final ITextEditor editor) {
		Display.getDefault().asyncExec(() -> {
			EmacsPlusUtils.forceStatusUpdate(editor);
		});
	}
	
	private void clearIt(IContributionItem item, ITextEditor editor) {
		flashItem.setVisible(false);
		asyncForce(editor);
	}

	protected StatusLineContributionItem initStatusLineItem() {
		if (flashItem == null) {
			setColors();
			flashItem = new FlashLineContributionItem(FLASH_ID, true, 83);;
		}		
		flashItem.setBackground(backs[flashCount % backs.length]);
		return flashItem;
	}

	/**
	 * Flash an area on the mode line a number of times
	 * 
	 * This is somewhat expensive due to all the asynchronous UI threads, but there seems to be 
	 * no other way to get the desired behavior.
	 * 
	 * @param display
	 */
	public void flashModeLine(Display display) {
		final ITextEditor editor = EmacsPlusUtils.getCurrentEditor();
		addStatusContribution(editor);
		asyncForce(editor);
		Display.getDefault().asyncExec(() -> {
			runFlash(flashCount, flashItem, editor);
		});
	}	

	private void runFlash(final int count, FlashLineContributionItem item, final ITextEditor editor) {
		if (count > 0) {
			Display.getDefault().syncExec(() -> {
				item.setBackground(backs[count % backs.length]);					
				item.setVisible(true);
				try {
					Thread.sleep(waitTime);
				} catch (InterruptedException e) {
					// ignore
				}
				Display.getDefault().asyncExec(() -> {
					clearIt(item, editor);
					runFlash(count -1, item, editor);
				});
			});
		} 
	}

	/**
	 * Add a background to a simplified StatusLineContributionItem
	 */
	private class FlashLineContributionItem extends StatusLineContributionItem {

		private static final int INDENT= 3;
		private int fFixedWidth= -1;
		private int fFixedHeight= -1;
		private int fWidthInChars;
		private Color backgroundColor = null;

		/** The status line label widget */
		private CLabel fLabel;
		private Label sep;		

		/**
		 * Creates a new item with the given attributes.
		 *
		 * @param id the item's id
		 * @param visible the visibility of this item
		 * @param widthInChars the width in characters
		 * @since 3.0
		 */
		public FlashLineContributionItem(String id, boolean visible, int widthInChars) {
			super(id);
			fWidthInChars= widthInChars;
		}

		public void setBackground(Color color) {
			backgroundColor = color;
		}

		@Override
		public void fill(Composite parent) {

			sep= new Label(parent, SWT.SEPARATOR);
			fLabel= new CLabel(parent, SWT.SHADOW_NONE);

			StatusLineLayoutData data= new StatusLineLayoutData();
			data.widthHint= getWidthHint(parent);
			fLabel.setLayoutData(data);

			data= new StatusLineLayoutData();
			data.heightHint= getHeightHint(parent);
			sep.setLayoutData(data);

			updateMessageLabel();
		}

		@Override
		public void dispose() {
			sep.dispose();
			fLabel.dispose();
			super.dispose();
		}

		@Override
		public void update() {
			updateMessageLabel();
			super.update();
		}

		/**
		 * Updates the message label widget.
		 *
		 * @since 3.0
		 */
		private void updateMessageLabel() {
			if (fLabel != null && !fLabel.isDisposed()) {
				if (backgroundColor != null) {
					fLabel.setBackground(backgroundColor);
				}
				fLabel.setForeground(fLabel.getParent().getForeground());
			}
		}

		/**
		 * Returns the width hint for this label.
		 *
		 * @param control the root control of this label
		 * @return the width hint for this label
		 * @since 2.1
		 */
		private int getWidthHint(Composite control) {
			if (fFixedWidth < 0) {
				GC gc= new GC(control);
				gc.setFont(control.getFont());
				fFixedWidth = (int) gc.getFontMetrics().getAverageCharacterWidth() * fWidthInChars;
				fFixedWidth += INDENT * 2;
				gc.dispose();
			}
			return fFixedWidth;
		}

		/**
		 * Returns the height hint for this label.
		 *
		 * @param control the root control of this label
		 * @return the height hint for this label
		 * @since 3.0
		 */
		private int getHeightHint(Composite control) {
			if (fFixedHeight < 0) {
				GC gc= new GC(control);
				gc.setFont(control.getFont());
				fFixedHeight= gc.getFontMetrics().getHeight();
				gc.dispose();
			}
			return fFixedHeight;
		}
	}	
}
