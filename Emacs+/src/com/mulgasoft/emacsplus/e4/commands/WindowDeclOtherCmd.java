/**
 * Copyright (c) 2009, 2014 Mark Feber, MulgaSoft
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package com.mulgasoft.emacsplus.e4.commands;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.commands.Command;
import org.eclipse.e4.core.contexts.Active;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.ui.model.application.ui.MUIElement;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.model.application.ui.basic.MWindow;
import org.eclipse.jface.bindings.TriggerSequence;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.commands.ICommandService;
import org.eclipse.ui.keys.IBindingService;

import com.mulgasoft.emacsplus.Beeper;
import com.mulgasoft.emacsplus.EmacsPlusUtils;
import com.mulgasoft.emacsplus.commands.EmacsPlusCmdHandler;

/**
 * Implements: open-declaration-other-window
 * 
 * @author mfeber - Initial API and implementation
 */
public class WindowDeclOtherCmd extends SwitchToBufferOtherCmd {

	private static final String OD = "Open Declaration";	//$NON-NLS-1$

	@Execute
	public Object execute(@Active MPart apart, @Active IEditorPart editor, @Active MWindow mwin, @Active EmacsPlusCmdHandler handler) {

		try {
			this.apart = apart;
			this.handler = handler;
			Command ex = getOpenCmd(editor);
			if (ex != null) {
				MUIElement area = getEditArea(application.getChildren().get(0));
				MUIElement osel = getSelected(area);
				EmacsPlusUtils.executeCommand(ex.getId(), null);
				MUIElement sel = getSelected(area);
				// see if it was able to open a declaration
				if (sel instanceof MPart && sel != apart && sel != osel) {
					// another way of determining if we're main or frame
					if (!mwin.getTags().contains(TOP_TAG) && !handler.isUniversalPresent()) {
						// If they're in different frames, move copy to source frame first
						modelService.move(sel, getParentStack(apart).getStack(), 0);
					};
					switchTo((MPart)sel);
				}
			}
		} catch (Exception e) {
			// Shouldn't happen
			Beeper.beep();
		}
		return null;
	}

	/**
	 * A bit of hackery that tries to get the correct open-declaration command for the buffer 
	 * @param editor
	 * @return the best match Command definition
	 */
	private Command getOpenCmd(IEditorPart editor) {
		Command result = null;
		try {
			IBindingService bs = (IBindingService) PlatformUI.getWorkbench().getService(IBindingService.class);
			ICommandService ics = (ICommandService) editor.getSite().getService(ICommandService.class);
			List<Command> cans = new ArrayList<Command>();
			List<Command> nocans = new ArrayList<Command>();
			Command[] commands = ics.getDefinedCommands();
			for (Command c : commands) {
				if (OD.equals(c.getName()) && c.isEnabled()) {
					TriggerSequence[] b = bs.getActiveBindingsFor(c.getId());
					if (b.length > 0) {
						cans.add(c);
					} else {
						nocans.add(c);
					}
				}
			}
			// TODO: when > 1 try to refine the result heuristically?
			// if Eclipse returned more than one, then chose a bound command over non-bound
			if (cans.isEmpty()) {
				if (!nocans.isEmpty()) {
					// just grab first one
					result = nocans.get(0);
				}
			} else {
				// grab the first bound one
				result = cans.get(0);
			}
		} catch (Exception e) { }
		return result;
	}
}
