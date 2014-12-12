/**
 * Copyright (c) 2009, 2014 Mark Feber, MulgaSoft
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package com.mulgasoft.emacsplus.execute;

import static com.mulgasoft.emacsplus.IEmacsPlusCommandDefinitionIds.METAX_EXECUTE;

import java.util.EmptyStackException;
import java.util.Stack;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.IExecutionListenerWithChecks;
import org.eclipse.core.commands.NotEnabledException;
import org.eclipse.core.commands.common.NotDefinedException;
import org.eclipse.swt.widgets.Event;

/**
 * Common functionality for command repetitions
 * 
 * @author mfeber - Initial API and implementation
 */
public abstract class RepeatingSupport implements IExecutionListenerWithChecks {
	
	boolean inMetaXCommand = false;

	boolean isInMetaXCommand(String commandId) {
		inMetaXCommand = (METAX_EXECUTE.equals(commandId));
		return inMetaXCommand;
	}
	
	boolean isInMetaXCommand() {
		return inMetaXCommand;
	}

	Stack<ExecutionEvent> currentEvent = new Stack<ExecutionEvent>();
	ExecutionEvent popEvent() {
		ExecutionEvent result;
		try {
			result = currentEvent.pop();
		} catch (EmptyStackException e) {
			// ignore, just return an empty event
			result = new ExecutionEvent();
		}
		return result;
	}
	
	Event getTrigger(ExecutionEvent event) {
		return (event != null && (event.getTrigger() != null && event.getTrigger() instanceof Event) ? 
				((Event) event.getTrigger()) : null);
	}	

	/* (non-Javadoc)
	 * @see org.eclipse.core.commands.IExecutionListenerWithChecks#notDefined(java.lang.String, org.eclipse.core.commands.common.NotDefinedException)
	 */
	public void notDefined(String commandId, NotDefinedException exception) {
	}

	/* (non-Javadoc)
	 * @see org.eclipse.core.commands.IExecutionListenerWithChecks#notEnabled(java.lang.String, org.eclipse.core.commands.NotEnabledException)
	 */
	public void notEnabled(String commandId, NotEnabledException exception) {
	}

}
