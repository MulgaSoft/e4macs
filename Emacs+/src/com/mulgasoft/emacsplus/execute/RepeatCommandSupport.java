/**
 * Copyright (c) 2009, 2014 Mark Feber, MulgaSoft
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package com.mulgasoft.emacsplus.execute;

import static com.mulgasoft.emacsplus.EmacsPlusUtils.UNIVERSAL_ARG;
import static com.mulgasoft.emacsplus.IEmacsPlusCommandDefinitionIds.KBDMACRO_EXECUTE;
import static com.mulgasoft.emacsplus.IEmacsPlusCommandDefinitionIds.REPEAT_COMMAND;
import static com.mulgasoft.emacsplus.IEmacsPlusCommandDefinitionIds.UNIVERSAL_ARGUMENT;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.NotEnabledException;
import org.eclipse.core.commands.NotHandledException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.contexts.IContextActivation;
import org.eclipse.ui.contexts.IContextService;

/**
 * A singleton class to hold and support repeating the last command
 * 
 * @author mfeber - Initial API and implementation
 */
public class RepeatCommandSupport extends RepeatingSupport {

	// This context enables an alternate binding (default: the letter Z)
	private final static String RCONTEXT = "com.mulgasoft.emacsplus.repeating";	//$NON-NLS-1$

	private Cmd repeater = null;
	
	boolean repeating = false;
	IContextActivation repeatc = null;

	private static RepeatCommandSupport instance;
	
	private RepeatCommandSupport() {}

	public static RepeatCommandSupport getInstance() {
		if (instance == null) {
			instance = new RepeatCommandSupport();
		}
		return instance;
	}

	public static boolean isRepeatCommand(String commandId) {
		return REPEAT_COMMAND.equals(commandId);
	}
	
	public String getId() {
		return repeater != null ? repeater.getId() : null;
	}
	
	@SuppressWarnings("unchecked")
	public Map<String, Object> getParams() {
		return (Map<String, Object>)(repeater != null ? repeater.getParams() : Collections.emptyMap());
	}
	
	public Integer getCount() {
		Integer result = 1;
		Object p = getParams().get(UNIVERSAL_ARG);
		if (p instanceof String) {
			result = convertCount((String)p);
		} else if (p != null){
			result = (Integer)p;
		}
		return result;
	}
	
	protected int convertCount(String universalArg) {
		int result = 1;
		if (universalArg != null && universalArg.length() > 0) {
			try {
				result = Integer.parseInt(universalArg);
			} catch (NumberFormatException e) {	// if invalid number, proceed with count=1
			}
		} 
		return result;
	}
	
	/**
	 * @see org.eclipse.core.commands.IExecutionListener#preExecute(java.lang.String, org.eclipse.core.commands.ExecutionEvent)
	 */
	public void preExecute(String commandId, ExecutionEvent event) {
		
		if (!excludeFromRepeat(commandId)) {
			if (isRepeatCommand(commandId)) {
				// arm
				this.repeating = true;				
				storeCount(event);
				activateContext();
			} else if (!this.repeating) {
				currentEvent.push(event);
				clearContext();
			}
		}		
	}

	/**
	 * @see org.eclipse.core.commands.IExecutionListener#postExecuteSuccess(java.lang.String, java.lang.Object)
	 */
	public void postExecuteSuccess(String commandId, Object returnValue) {
		if (isRepeatCommand(commandId)) {
			// disarm
			this.repeating = false;
		} else if (!(this.repeating || excludeFromRepeat(commandId))) {
			ExecutionEvent event = popEvent();
			boolean wasMeta = isInMetaXCommand();			
			if (!isInMetaXCommand(commandId) && (wasMeta || getTrigger(event) != null)) {
				storeCommand(commandId, event);
			}
		}
	}

	private void activateContext() {
		if (this.repeatc == null) {
			this.repeatc = ((IContextService)PlatformUI.getWorkbench().getService(IContextService.class)).activateContext(RCONTEXT);
		}

	}
	
	private void clearContext() {
		if (this.repeatc != null) {
			((IContextService)PlatformUI.getWorkbench().getService(IContextService.class)).deactivateContext(repeatc);
			this.repeatc = null;
		}
		
	}
	private void fail(String commandId) {
		if (isRepeatCommand(commandId)) {
			// disarm
			this.repeating = false;
		} else {
			popEvent();			
			clearContext();
		}
	}
	/**
	 * @see org.eclipse.core.commands.IExecutionListener#postExecuteFailure(java.lang.String, org.eclipse.core.commands.ExecutionException)
	 */
	public void postExecuteFailure(String commandId, ExecutionException exception) {
		fail(commandId);
	}

	/**
	 * @see org.eclipse.core.commands.IExecutionListener#notHandled(java.lang.String, org.eclipse.core.commands.NotHandledException)
	 */
	public void notHandled(String commandId, NotHandledException exception) {
		fail(commandId);
	}
	
	@Override
	public void notEnabled(String commandId, NotEnabledException exception) {
		popEvent();
	}

	private class Cmd {
		private String id;
		private Map<?,?> params;
		
		
		private Cmd(String id, Map<?,?> params) {
			this.id = id;
			this.params = params;
		}

		private Cmd(String id, ExecutionEvent event) {
			this(id, event.getParameters());
		}
		
		@SuppressWarnings("serial")
		private Cmd(String id, final Integer count) {
			this(id, new HashMap<String,Object>() {{put(UNIVERSAL_ARG, count); }});
		}
		
		public String getId() {
			return id;
		}
		
		public Map<?, ?> getParams() {
			return params;
		}
		
		public void setParams(Map<?, ?> params) {
			this.params = params;
		}
	}
	
	public void storeCommand(String commandId, Integer count) {
		repeater = new Cmd(commandId, count);
	}
	
	public void storeCommand(String commandId, ExecutionEvent event) {
		repeater = new Cmd(commandId, event);
	}
	
	protected void storeCount(ExecutionEvent event) {
		String universalArg = event.getParameter(UNIVERSAL_ARG);
		if (universalArg != null && universalArg.length() > 0) {
			try {
				Integer count = Integer.parseInt(universalArg);
				if (repeater != null) {
					@SuppressWarnings("unchecked")
					Map<String,Object> params = (Map<String,Object>)repeater.getParams();
					if (params.isEmpty()) {
						params = new HashMap<String,Object>();
						repeater.setParams(params);
					}
					params.put(UNIVERSAL_ARG, count);
				}
			} catch (NumberFormatException e) {}	// ignore if invalid number
		}
	}
	
	/**
	 * Check if we should remember this command
	 * 
	 * @param id command
	 * @return ignore command if true, else remember it
	 */
	private boolean excludeFromRepeat(String id) {
		// Ignore all but the execute command, when in a kbd macro
		boolean inKbd = (KbdMacroSupport.getInstance().isExecuting() && !KBDMACRO_EXECUTE.equals(id));
		return (excludeCmds.get(id) != null ? true : inKbd);
	}

	@SuppressWarnings("serial")
	protected static final HashMap<String,Boolean> excludeCmds = new HashMap<String,Boolean>() {
		{
			put(UNIVERSAL_ARGUMENT, Boolean.TRUE);
		}
	};
}
