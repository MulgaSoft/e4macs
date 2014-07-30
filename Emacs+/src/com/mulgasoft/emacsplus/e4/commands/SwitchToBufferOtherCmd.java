/**
 * Copyright (c) 2009, 2014 Mark Feber, MulgaSoft
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package com.mulgasoft.emacsplus.e4.commands;

import static com.mulgasoft.emacsplus.EmacsPlusUtils.getPreferenceBoolean;
import static com.mulgasoft.emacsplus.EmacsPlusUtils.getPreferenceStore;
import static com.mulgasoft.emacsplus.preferences.PrefVars.SHOW_OTHER_HORIZONTAL;

import javax.inject.Named;

import org.eclipse.e4.core.contexts.Active;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.ui.model.application.ui.MElementContainer;
import org.eclipse.e4.ui.model.application.ui.MUIElement;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.workbench.modeling.EPartService.PartState;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.texteditor.ITextEditor;

import com.mulgasoft.emacsplus.Beeper;
import com.mulgasoft.emacsplus.EmacsPlusUtils;
import com.mulgasoft.emacsplus.commands.EmacsPlusCmdHandler;
import com.mulgasoft.emacsplus.commands.MinibufferHandler;
import com.mulgasoft.emacsplus.minibuffer.IMinibufferExecutable;
import com.mulgasoft.emacsplus.minibuffer.SwitchMinibuffer;

/**
 * Implements Select/Display buffer in another window
 *  
 * Prompt for a buffer and select/show it in another frame. If:
 *  - only one frame, then split and place in new buffer
 *  - multiple frames, move buffer to adjacent frame if not in destination frame,
 *  - else simply select/show in destination frame
 *  
 *  When splitting, split horizontally unless called with ^U
 * 
 * @author mfeber - Initial API and implementation
 */
public class SwitchToBufferOtherCmd extends WindowSplitCmd implements IMinibufferExecutable {

	private MPart apart;
	private EmacsPlusCmdHandler handler;
	private boolean displayOnly = false;
	private String prefix = "Buffer%s";	//$NON-NLS-1$
	
	// A global, sticky variable to set the default direction of split
	private static boolean DISPLAY_HORIZONTAL = getPreferenceBoolean(SHOW_OTHER_HORIZONTAL.getPref());

	static {
		// listen for changes in the property store
		getPreferenceStore().addPropertyChangeListener(
				new IPropertyChangeListener() {
					public void propertyChange(PropertyChangeEvent event) {
						if (SHOW_OTHER_HORIZONTAL.getPref().equals(event.getProperty())) {
							setHorizontal((Boolean)event.getNewValue());
						}
					}
				}
		);
	}
	
	public static void setHorizontal(boolean horizontal) {
		DISPLAY_HORIZONTAL = horizontal;
	}
	
	@Execute
	public Object execute(@Active MPart apart, @Active IEditorPart editor, @Active EmacsPlusCmdHandler handler, @Named(E4CmdHandler.CMD_CTX_KEY)boolean display, @Named(E4CmdHandler.PRE_CTX_KEY) String prefix) {
		this.handler = handler;
		this.apart = apart;
		this.displayOnly = display;
		this.prefix = prefix;
		try {
			ITextEditor ted = EmacsPlusUtils.getTextEditor(editor, true);
			if (ted != null) {
				MinibufferHandler.bufferTransform(new SwitchMinibuffer(this), ted, null);
			}
		} catch (BadLocationException e) {
			// Shouldn't happen
			Beeper.beep();
		}		
		return null;
	}

	public boolean isUniversalPresent() {
		return handler.isUniversalPresent();
	}

	public String getMinibufferPrefix() {
		return prefix;
	}

	public boolean executeResult(ITextEditor editor, Object minibufferResult) {
		if (minibufferResult instanceof IEditorPart) {
			// get the MPart from the editor
			MPart miniPart = (MPart) ((IEditorPart) minibufferResult).getSite().getService(MPart.class);
			if (getOrderedStacks(apart).size() == 1) {
				// case 1: 1 frame, split with miniPart
				// convenience hack: change direction on uArg
				splitIt(miniPart, getDirection((isUniversalPresent()) ? !DISPLAY_HORIZONTAL : DISPLAY_HORIZONTAL));
			} else {
				// case 2: multiple stacks, move to adjacent stack
				// get the starting stack
				MElementContainer<MUIElement> stack = getParentStack(apart).getStack();
				// get the topart's stack
				MElementContainer<MUIElement> tstack = getParentStack(miniPart).getStack();
				stack = findNextStack(apart, stack, 1);
				if (stack != null && stack != tstack) {
					modelService.move(miniPart, stack, 0);
				}
			}
			if (displayOnly) {
				// brings to top
				partService.showPart(miniPart, PartState.VISIBLE);
				reactivate(apart);
			} else {
				reactivate(miniPart);				
			}
		}
		return false;
	}

	public void setResultMessage(String resultMessage, boolean resultError) {
	}
}
