/**
 * Copyright (c) 2009, 2010 Mark Feber, MulgaSoft
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package com.mulgasoft.emacsplus.commands;

import java.util.Collection;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.ParameterizedCommand;
import org.eclipse.jface.bindings.Binding;
import org.eclipse.jface.bindings.Trigger;
import org.eclipse.jface.bindings.TriggerSequence;
import org.eclipse.jface.bindings.keys.KeySequence;
import org.eclipse.jface.bindings.keys.KeyStroke;
import org.eclipse.swt.SWT;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.keys.IBindingService;

import com.mulgasoft.emacsplus.EmacsPlusUtils;
import com.mulgasoft.emacsplus.execute.EmacsPlusConsole;

/**
 * List information about Emacs+ help commands
 * The command expects all help commands to have a uniform prefix
 *  
 * @author Mark Feber - initial API and implementation
 */
public class EmacsHelpHandler extends EmacsPlusNoEditHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {

		EmacsPlusConsole console = EmacsPlusConsole.getInstance();
		console.clear();
		console.activate();
		IBindingService bindingService = (IBindingService) PlatformUI.getWorkbench().getService(IBindingService.class);
		String id = (event.getCommand() != null ? event.getCommand().getId() : null);
		if (id != null) {
			try {
				TriggerSequence trigger = bindingService.getBestActiveBindingFor(event.getCommand().getId());
				Trigger[] trigs = trigger.getTriggers();
				KeyStroke key = (KeyStroke)trigs[0];
				Collection<Binding> partials = EmacsPlusUtils.getPartialMatches(bindingService,KeySequence.getInstance(key)).values();

				for (Binding bind : partials) {
					ParameterizedCommand cmd = bind.getParameterizedCommand();
					if (cmd.getId().startsWith(EmacsPlusUtils.MULGASOFT)) {
						console.printBold(bind.getTriggerSequence().toString());
						console.print(SWT.TAB + cmd.getCommand().getName());
						String desc =  cmd.getCommand().getDescription();
						if (desc != null) {
							desc = desc.replaceAll(CR, EMPTY_STR);
							console.print(" - " + desc + CR);	//$NON-NLS-1$ 
						} else {
							console.print(CR);
						}
					}
				}
			} catch (Exception e) {}
			console.setFocus(true);
		}
		return null;
	}

}
