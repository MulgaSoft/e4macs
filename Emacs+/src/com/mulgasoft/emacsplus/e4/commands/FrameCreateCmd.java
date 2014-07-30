/**
 * Copyright (c) 2009, 2014 Mark Feber, MulgaSoft
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package com.mulgasoft.emacsplus.e4.commands;

import static com.mulgasoft.emacsplus.EmacsPlusUtils.getPreferenceStore;
import static com.mulgasoft.emacsplus.preferences.PrefVars.FRAME_DEF;
import static com.mulgasoft.emacsplus.preferences.PrefVars.FRAME_INIT;

import java.util.List;

import org.eclipse.e4.ui.model.application.ui.MUIElement;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.model.application.ui.basic.MTrimmedWindow;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Control;

import com.mulgasoft.emacsplus.Beeper;
import com.mulgasoft.emacsplus.EmacsPlusUtils;
import com.mulgasoft.emacsplus.preferences.PrefVars.PRect;

/**
 * make-frame: Make a new frame using the current buffer
 * 
 * @author mfeber - Initial API and implementation
 */
public class FrameCreateCmd extends WindowSplitCmd {

	static Rectangle initialFrameRect = PRect.parseRect(EmacsPlusUtils.getPreferenceString(FRAME_INIT.getPref()));
	static Rectangle defaultFrameRect = PRect.parseRect(EmacsPlusUtils.getPreferenceString(FRAME_DEF.getPref()));
	static final Rectangle EMPTY_RECT = new Rectangle(0,0,0,0);
	static final int WADJ = 11;
	static final int HADJ = 22;
	
	static {
		// listen for changes in the property store
		getPreferenceStore().addPropertyChangeListener(
				new IPropertyChangeListener() {
					public void propertyChange(PropertyChangeEvent event) {
						Rectangle r;
						if (FRAME_INIT.getPref().equals(event.getProperty())) {
							if ((r = PRect.parseRect((String)event.getNewValue())) != null) {
								initialFrameRect = r;				
							}
						} else if (FRAME_DEF.getPref().equals(event.getProperty())) {
							if ((r = PRect.parseRect((String)event.getNewValue())) != null) {
								defaultFrameRect = r;				
							}
						}
					}
				}
		);
	}

	private void fillRect(Rectangle srect, Rectangle drect) {
		drect.x = srect.x;
		drect.y = srect.y;
		if (srect.width != 0){
			drect.width = srect.width - WADJ;					
		}
		if (srect.height != 0){
			drect.height = srect.height - HADJ;					
		}
	}
	
	// on splitSelf, we want to activate the new part
	private MPart splitPart;
 
	/** (non-Javadoc)
	 * @see com.mulgasoft.emacsplus.e4.commands.WindowSplitCmd#splitIt(org.eclipse.e4.ui.model.application.ui.basic.MPart, int)
	 */
	@Override
	protected void splitIt(MPart apart, int location) {
		splitPart = apart;
		Control widget = (Control) apart.getParent().getWidget();
		Rectangle rect = widget.getBounds();
		rect.x = 0;
		rect.y = 0;
		// from org.eclipse.e4.ui.workbench.addons.dndaddon.DetachedDropAgent: 
		// Try to take the window's trim into account
		rect.width += WADJ;
		rect.height += HADJ;
		List<MTrimmedWindow> frames = getDetachedFrames();
		if (isSplitSelf()) {
			// the new editor command creates and activates the copy in the main area
			MUIElement sel = getSelected(getEditArea(application.getChildren().get(0)));		
			if (sel instanceof MPart) {
				splitPart = (MPart)sel;
			}
		} else if (frames.isEmpty() && getParentStack(apart).getStack().getChildren().size() <= 1) {
			// don't detach when only one editor open anywhere 
			Beeper.beep();
			return;
		}
		if (frames.isEmpty() && !EMPTY_RECT.equals(initialFrameRect)) {
			// use initial
			fillRect(initialFrameRect, rect);
		} else if (!EMPTY_RECT.equals(defaultFrameRect)) {
			// use default
			fillRect(defaultFrameRect, rect);
		}

		MPart newpart = getParentStack(splitPart).getPart();		
		// Let the model service take care of the rest
		modelService.detach(newpart, rect.x, rect.y, rect.width, rect.height);
	}

	@Override
	protected void reactivate(MPart apart) {
		super.reactivate(splitPart);
	}

}
