/**
 * Copyright (c) 2009, 2014 Mark Feber, MulgaSoft
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package com.mulgasoft.emacsplus.e4.commands;

import java.util.List;

import javax.inject.Named;

import org.eclipse.e4.core.contexts.Active;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.ui.model.application.ui.MElementContainer;
import org.eclipse.e4.ui.model.application.ui.MUIElement;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.model.application.ui.basic.MTrimmedWindow;
import org.eclipse.e4.ui.model.application.ui.basic.MWindow;
import org.eclipse.e4.ui.workbench.modeling.EPartService.PartState;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.texteditor.ITextEditor;

import com.mulgasoft.emacsplus.Beeper;
import com.mulgasoft.emacsplus.EmacsPlusUtils;
import com.mulgasoft.emacsplus.commands.EmacsPlusCmdHandler;
import com.mulgasoft.emacsplus.commands.MinibufferHandler;
import com.mulgasoft.emacsplus.minibuffer.IMinibufferExecutable;
import com.mulgasoft.emacsplus.minibuffer.SwitchMinibuffer;

/**
 * switch-to-buffer-other-frame: Select buffer in another frame
 * display-buffer-other-frame: Display buffer in another frame without selecting
 * 
 * Prompt for a buffer and select it in another frame. If:
 *  - only one frame, then make a new frame
 *  - multiple frames, move buffer to adjacent frame if not in destination frame
 *  - else simply expose in destination frame
 *  
 * @author mfeber - Initial API and implementation
 */
public class SwitchToBufferFrameCmd extends FrameCreateCmd implements IMinibufferExecutable {

	MWindow window;
	private MPart apart;
	private EmacsPlusCmdHandler handler;
	private boolean displayOnly = false;
	private String prefix = "Buffer%s";	//$NON-NLS-1$
	
	@Execute
	public Object execute(@Active MWindow window, @Active MPart apart, @Active IEditorPart editor, @Active EmacsPlusCmdHandler handler, @Named(E4CmdHandler.CMD_CTX_KEY)boolean display, @Named(E4CmdHandler.PRE_CTX_KEY) String prefix) {
		this.window = window;
		this.handler = handler;
		this.apart = apart;
		this.displayOnly = display;
		this.prefix = prefix;
		try {
			ITextEditor ted = EmacsPlusUtils.getTextEditor(editor, true);
			if (ted != null) {
				MinibufferHandler.bufferTransform(new SwitchMinibuffer(this, true), ted, null);
			}
		} catch (BadLocationException e) {
			// Shouldn't happen
			Beeper.beep();
		}		
		return null;
	}
	
	@SuppressWarnings("unchecked") // for safe cast to (MElementContainer<MUIElement>)
	public boolean executeResult(ITextEditor editor, Object minibufferResult) {
		if (minibufferResult instanceof IEditorPart) {
			// get the MPart from the editor
			MPart miniPart = (MPart) ((IEditorPart) minibufferResult).getSite().getService(MPart.class);
			Object minw = getTopElement(miniPart.getParent());
			// crap - modelService always returns the main window
			MWindow partWindow = (minw instanceof MWindow) ? (MWindow)minw : modelService.getTopLevelWindowFor(miniPart);
			List<MTrimmedWindow> frames = getDetachedFrames();
			if (partWindow == window) {
				if (frames == null || frames.isEmpty()) {
					// case 1: one frame, make a new one
					splitIt(miniPart, 0);
				} else {
					// case 2: multiple frames, move to adjacent frame
					MElementContainer<MUIElement> stack;
					int index = frames.indexOf(partWindow) + 1;
					MUIElement frame;
					if (index == frames.size()) {
						// get editor MArea of main window
						frame = getEditArea(modelService.getTopLevelWindowFor(miniPart));
					} else {
						frame = frames.get((index < 0) ? 0 : index); 
					}
					stack = getStacks((MElementContainer<MUIElement>)frame).get(0);
					modelService.move(miniPart, stack, 0);
				}
			} else if (!alreadyFramed(partWindow, frames)) {
				// case 3: different frames, move to this frame
				MElementContainer<MUIElement> stack = getParentStack(apart).getStack();
				// get the topart's stack
				MElementContainer<MUIElement> mstack = getParentStack(miniPart).getStack();
				if (stack != null && stack != mstack) {
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
	
	private boolean alreadyFramed(MWindow partWindow, List<MTrimmedWindow> frames) {
		// NB: This differs from emacs behavior, but seems much more useful in Eclipse
		// as it allows easily adding a buffer to a frame's stack.
		// buffer is in a frame, and command comes from main window
		return frames.contains(partWindow) && !frames.contains(window);
	}
	
	public boolean isUniversalPresent() {
		return handler.isUniversalPresent();
	}

	public String getMinibufferPrefix() {
		return prefix;
	}

	/**
	 * @see com.mulgasoft.emacsplus.minibuffer.IMinibufferExecutable#setResultMessage(java.lang.String, boolean)
	 */
	public void setResultMessage(String resultMessage, boolean resultError) {
	}

}
