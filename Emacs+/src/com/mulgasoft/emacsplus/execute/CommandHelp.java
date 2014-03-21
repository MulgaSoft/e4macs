/**
 * Copyright (c) 2009, 2010 Mark Feber, MulgaSoft
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 */
package com.mulgasoft.emacsplus.execute;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.commands.Command;
import org.eclipse.core.commands.ParameterizedCommand;
import org.eclipse.jface.bindings.Binding;
import org.eclipse.jface.bindings.TriggerSequence;
import org.eclipse.swt.SWT;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.keys.IBindingService;

/**
 * @author Mark Feber - initial API and implementation
 */
public class CommandHelp {

	private static final String COMMA_SEPR = ", ";	//$NON_NLS-1$
	
	/**
	 * Get the key-binding information for the command
	 * 
	 * @param com
	 * @param activep - if true, return only active bindings
	 * 
	 * @return an array of binding information
	 */
	public static Binding[] getBindings(Command com, boolean activep) {
		
		IBindingService binder = (IBindingService) PlatformUI.getWorkbench().getService(IBindingService.class);
		Binding[] bindings = binder.getBindings();
		List<Binding> vbindings = new ArrayList<Binding>();
		String platform = SWT.getPlatform();
		for (Binding bind : bindings) {
			ParameterizedCommand pc = bind.getParameterizedCommand();
			if (pc != null && com.equals(pc.getCommand())) {
				// Only return binding info for applicable platforms
				String plat = bind.getPlatform();
				if (plat == null || platform.equals(plat)) {
					vbindings.add(bind);
				}
			}
		}
		if (activep && !vbindings.isEmpty()) {
			TriggerSequence[] atrigs= binder.getActiveBindingsFor(com.getId()); 
			List<Binding> abindings = new ArrayList<Binding>();
			for (TriggerSequence trig : atrigs) {
				for (Binding bind : vbindings) {
					if (bind.getTriggerSequence().equals(trig)) {
						abindings.add(bind);
						break;
					}
				}
			}
			bindings = abindings.toArray(new Binding[0]);
		} else {
			bindings = vbindings.toArray(new Binding[0]);
		}
		
		return bindings;
	}
	
	/**
	 * Get the best binding (as determined by Eclipse) for the Command
	 * 
	 * @param cmd
	 * @return the binding or null
	 */
	public static String getBestBinding(Command cmd) {
		String result = null;
		IBindingService binder = (IBindingService) PlatformUI.getWorkbench().getService(IBindingService.class);
		TriggerSequence bindingFor = binder.getBestActiveBindingFor(cmd.getId());
		if (bindingFor != null) {
			result = bindingFor.format(); 
		}
		return result;
	}
	
	/**
	 * Get the displayable key-binding information for the command
	 * 
	 * @param com - the command
	 * @param activep - if true, return only active bindings
	 * 
	 * @return a String array of binding sequence binding context information
	 */
	public static String[] getKeyBindingStrings(Command com, boolean activep) {
		String id = com.getId();

		TriggerSequence trigger;
		// Get platform bindings for Command 
		Binding[] bindings = getBindings(com,activep);

		List<String> bindingInfo = new ArrayList<String>();
		ParameterizedCommand c;
		for (Binding bind : bindings) {
			c = bind.getParameterizedCommand();
			if (c != null && c.getId().equals(id)) {
				trigger = bind.getTriggerSequence();
				bindingInfo.add(trigger.toString());
				bindingInfo.add(bind.getContextId());
			}
		}
		return bindingInfo.toArray(new String[0]);
	}
	
	/**
	 * Get the displayable key-binding information for the parameterized command
	 * 
	 * @param com the command
	 * @param activep - if true, return only active bindings
	 * 
	 * @return a String array of binding sequence binding context information
	 */
	public static String[] getKeyBindingStrings(ParameterizedCommand com, boolean activep) {
		TriggerSequence trigger;
		// Get platform bindings for the ParameterizedCommand's Command 
		Binding[] bindings = getBindings(com.getCommand(),activep);

		List<String> bindingInfo = new ArrayList<String>();
		ParameterizedCommand c;
		for (Binding bind : bindings) {
			c = bind.getParameterizedCommand();
			if (c != null && c.equals(com)) {
				trigger = bind.getTriggerSequence();
				bindingInfo.add(trigger.toString());
				bindingInfo.add(bind.getContextId());
			}
		}
		return bindingInfo.toArray(new String[0]);
	}

	public static String getKeyBindingString(Command com, boolean activep) {
		String result = null;
		String[] bindings = getKeyBindingStrings(com, activep);
		StringBuilder bindingsBuf = new StringBuilder();
		if (bindings.length > 0) {
			for (int i=0; i < bindings.length; i+=2) {
				if (i != 0) {
					bindingsBuf.append(COMMA_SEPR);
				}
				bindingsBuf.append(bindings[i]);
			}
			result = bindingsBuf.toString();
		}
		return result;
	}
}

