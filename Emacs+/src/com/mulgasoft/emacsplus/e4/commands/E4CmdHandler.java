/**
 * Copyright (c) 2009, 2014 Mark Feber, MulgaSoft
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package com.mulgasoft.emacsplus.e4.commands;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.e4.core.contexts.ContextInjectionFactory;
import org.eclipse.e4.core.contexts.EclipseContextFactory;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.ui.PlatformUI;

import com.mulgasoft.emacsplus.commands.EmacsPlusCmdHandler;

/**
 * Based on info in the article: http://eclipsesource.com/blogs/tutorials/eclipse-4-e4-tutorial-soft-migration-from-3-x-to-eclipse-4-e4
 * by Jonas Helming <jhelming@eclipsesource.com>
 * Copyright (c) 2012 EclipseSource M�nchen GmbH and others.
 *  
 * @author mfeber - Initial API and implementation
 * @param <T>
 */
public abstract class E4CmdHandler<T> extends EmacsPlusCmdHandler {

	private T e4cmd;
	public final static String CMD_CTX_KEY = "com.mulgasoft.emacsplus.cmdId";
	public final static String PRE_CTX_KEY = "com.mulgasoft.emacsplus.prefix";

	public E4CmdHandler(Class<T> clazz) {
		e4cmd = ContextInjectionFactory.make(clazz, getContext());
	}

	private IEclipseContext getContext() {
		return ((IEclipseContext)PlatformUI.getWorkbench().getService(IEclipseContext.class)).getActiveLeaf();
	}

	public Object execute(ExecutionEvent event) throws ExecutionException {
		extractUniversalCount(event);	// initialize uArg
		// Pass the non-e4 handler so it can be injected into the command if necessary
		IEclipseContext ctx = EclipseContextFactory.create();
		addToContext(ctx);
		return ContextInjectionFactory.invoke(e4cmd, Execute.class, getContext(), ctx, null);
	}
	
	protected void addToContext(IEclipseContext ctx) {
		ctx.set(EmacsPlusCmdHandler.class, this);		
	}
}

