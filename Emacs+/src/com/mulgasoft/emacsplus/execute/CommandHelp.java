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
	
	private static IBindingService getBS() {
		return (IBindingService) PlatformUI.getWorkbench().getService(IBindingService.class); 
	}
	
	/**
	 * Get the key-binding information for the command
	 * 
	 * @param com
	 * @param activep - if true, return only active bindings
	 * 
	 * @return an array of binding information
	 */
	public static Binding[] getBindings(Command cmd, boolean activep) {
		IBindingService binder = getBS();
		List<Binding> resultBindings = new ArrayList<Binding>();
		TriggerSequence[] trs = binder.getActiveBindingsFor(cmd.getId());
		String platform = SWT.getPlatform();
		for (TriggerSequence ts : trs) {
			Binding b = binder.getPerfectMatch(ts);
			String plat = b.getPlatform(); 
			if ((plat == null || plat.equals(platform)) &&
					(cmd.equals(b.getParameterizedCommand().getCommand()))) {
				resultBindings.add(b);
			}
		}
		return resultBindings.toArray(new Binding[0]);		
	}

	/**
	 * Get the best binding (as determined by Eclipse) for the Command
	 * 
	 * @param cmd
	 * @return the binding or null
	 */
	public static String getBestBinding(Command cmd) {
		return getBS().getBestActiveBindingFormattedFor(cmd.getId());
	}
	
	/**
	 * Get the displayable key-binding information for the command
	 * 
	 * @param com - the command
	 * @param activep - if true, return only active bindings
	 * 
	 * @return a String array of binding sequence binding context information
	 */
	public static String[] getKeyBindingStrings(Command com) {
		return getKeyBindingStrings(new ParameterizedCommand(com,null)); 
	}
	
	/**
	 * Get the displayable key-binding information for the parameterized command
	 * 
	 * @param com the command
	 * @param activep - if true, return only active bindings
	 * @param <>c 
	 * 
	 * @return a String array of binding sequence binding context information
	 */
	public static String[] getKeyBindingStrings(ParameterizedCommand com) {
		// Get platform bindings for the ParameterizedCommand's Command 
		IBindingService binder = getBS();
		TriggerSequence[] trs = binder.getActiveBindingsFor(com);
		List<String> bindingInfo = new ArrayList<String>();
		for (TriggerSequence tr : trs) {
			Binding bind = binder.getPerfectMatch(tr);
			if (com.equals(bind.getParameterizedCommand())) {
				bindingInfo.add(tr.toString());
				bindingInfo.add(bind.getContextId());
			}
		}
		return bindingInfo.toArray(new String[0]);
	}

	/**
	 * Get a string representation of all the applicable bindings for the command
	 * 
	 * @param com
	 * @return a String representation of the bindings
	 */
	public static String getKeyBindingString(Command com) {
		String result = null;
		String[] strBindings = getKeyBindingStrings(com);
		StringBuilder bindingsBuf = new StringBuilder();
		if (strBindings.length > 0) {
			for (int i=0; i < strBindings.length; i+=2) {
				if (i != 0) {
					bindingsBuf.append(COMMA_SEPR);
				}
				bindingsBuf.append(strBindings[i]);
			}
			result = bindingsBuf.toString();
		}
		return result;
	}
}

