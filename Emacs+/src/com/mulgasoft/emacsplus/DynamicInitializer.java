/**
 * Copyright (c) 2009, 2010 Mark Feber, MulgaSoft
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package com.mulgasoft.emacsplus;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.commands.Command;
import org.eclipse.core.commands.IExecutionListener;
import org.eclipse.core.commands.ParameterizedCommand;
import org.eclipse.jface.bindings.Binding;
import org.eclipse.jface.bindings.keys.KeyBinding;
import org.eclipse.jface.bindings.keys.KeySequence;
import org.eclipse.jface.bindings.keys.ParseException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.commands.ICommandService;
import org.eclipse.ui.internal.keys.BindingService;
import org.eclipse.ui.keys.IBindingService;

/**
 * Determine which plugin specific bindings should be added once Emacs+ has started.
 * Currently: java, javascript, pde, c++
 * 
 * @author Mark Feber - initial API and implementation
 */
@SuppressWarnings("restriction")	// for cast to org.eclipse.ui.internal.keys.BindingService since addBinding is not yet API
public class DynamicInitializer {

	private DynamicInitializer() {}
	
	public static class AddBindingsOnce {
		public static boolean bind = addOnce();
	}

	private static boolean addOnce() {
		// kludge this until I find a better way to determine if the optional or other plugins are loaded
		List<String>emacsIds = EmacsPlusActivator.getDefault().getLoadedList();
		if (EmacsPlusUtils.isMac() && emacsIds.contains(EmacsPlusUtils.EMP_MACCMD_OPT_STR) ) {
			addBindings(cmdBindings);
		} else if (emacsIds.contains(EmacsPlusUtils.EMP_OPT_STR) ) {
			addBindings(altBindings);
		}
		addBindings(restBindings);
		return true;
	}
	

	public static void initialize() {
		@SuppressWarnings("unused") // this ensures bindings are added only once
		boolean initKeys = DynamicInitializer.AddBindingsOnce.bind;		
	}
	
	/**
	 * For each, if the command exists in the current system, add the new binding to the Emacs+ scheme
	 *
	 * @param editor
	 * @param bindingResult
	 */
	private static void addBindings(Set<MinderBinder> bindings) {

		IBindingService service = ((IBindingService) PlatformUI.getWorkbench().getService(IBindingService.class));
		if (service instanceof  BindingService) {
			BindingService bindingSvc = (BindingService) service;
			for (MinderBinder mb : bindings) {
				try {
					ICommandService ics = ((ICommandService) PlatformUI.getWorkbench().getService(ICommandService.class));
					// check first, as getCommand will create it if it doesn't already exist
					if (ics.getDefinedCommandIds().contains(mb.getCommandId())) {
						Command cmd = ics.getCommand(mb.getCommandId());
						if (mb.getEnhancer() != null) {
							// enhance the pre-defined command with some Emacs+ behavior
							cmd.addExecutionListener(mb.getEnhancer());
						}
						Binding binding = new KeyBinding(mb.getTrigger(), new ParameterizedCommand(cmd, null),
								mb.getSchemeId(), mb.getContextId(), null, null, null, Binding.SYSTEM);  // Binding.USER
						// this call is scheduled for API promotion sometime (after Helios)
						bindingSvc.addBinding(binding);
					}
				} catch (ParseException e) {
					e.printStackTrace();	// won't happen
				} catch (Exception e) {
					e.printStackTrace();	// won't happen
				}
			}
		}
	}

	private static class MinderBinder {

		String commandId;
		String contextId;
		String schemeId = EmacsPlusUtils.EMP_SCHEMEID;
		String keyString;
		KeySequence trigger;
		IExecutionListener enhancer;

		public MinderBinder(String commandId, String contextId, String keyString) {
			this(commandId,contextId,keyString,null);
		}
		
		public MinderBinder(String commandId, String contextId, String keyString, IExecutionListener enhancer) {
			this.commandId = commandId;
			this.contextId = contextId;
			this.keyString = keyString;
			this.enhancer = enhancer;
		}

		public String getCommandId() {
			return commandId;
		}
		public String getContextId() {
			return contextId;
		}
		public String getSchemeId() {
			return schemeId;
		}
		public IExecutionListener getEnhancer() {
			return enhancer;
		}

		/**
		 * @throws ParseException
		 */
		public KeySequence getTrigger() throws ParseException {
			if (trigger == null) {
				trigger = KeySequence.getInstance(keyString);
			}
			return trigger;
		}

	}

	private static final String JEDITOR = "org.eclipse.jdt.ui.javaEditorScope"; 			  //$NON-NLS-1$
	private static final String JSEDITOR = "org.eclipse.wst.jsdt.ui.javaEditorScope";   	  //$NON-NLS-1$
	private static final String JSVEDITOR = "org.eclipse.wst.jsdt.ui.javascriptViewScope";    //$NON-NLS-1$
	private static final String CEDITOR = "org.eclipse.cdt.ui.cEditorScope";				  //$NON-NLS-1$
	private static final String PEDITOR = "org.eclipse.pde.ui.pdeEditorContext";			  //$NON-NLS-1$	
	private static final String RBYEDITOR = "org.rubypeople.rdt.ui.rubyEditorScope";		  //$NON-NLS-1$
	private static final String XEDITOR = "org.eclipse.wst.sse.ui.structuredTextEditorScope"; //$NON-NLS-1$
	
	@SuppressWarnings("serial")
	private static final Set<MinderBinder> restBindings = new HashSet<MinderBinder>() {{

		// java bindings
		add(new MinderBinder("org.eclipse.jdt.ui.edit.text.java.show.outline",JEDITOR,"CTRL+]"));   											 //$NON-NLS-1$ //$NON-NLS-2$
		add(new MinderBinder("org.eclipse.jdt.ui.edit.text.java.open.hierarchy",JEDITOR,"CTRL+[")); 											 //$NON-NLS-1$ //$NON-NLS-2$
		add(new MinderBinder("org.eclipse.jdt.ui.edit.text.java.toggle.comment",JEDITOR," ESC ;")); 											 //$NON-NLS-1$ //$NON-NLS-2$
		add(new MinderBinder("org.eclipse.jdt.ui.edit.text.java.open.editor",JEDITOR,"CTRL+C CTRL+V CTRL+Y",MarkUtils.getTagListener()));   	 //$NON-NLS-1$ //$NON-NLS-2$
		add(new MinderBinder("org.eclipse.jdt.ui.edit.text.java.open.editor",JEDITOR,"ESC .",MarkUtils.getTagListener()));  					 //$NON-NLS-1$ //$NON-NLS-2$

		// javascript bindings
		add(new MinderBinder("org.eclipse.wst.jsdt.ui.edit.text.java.show.outline", JSEDITOR,"CTRL+]"));										 //$NON-NLS-1$ //$NON-NLS-2$
		add(new MinderBinder("org.eclipse.wst.jsdt.ui.edit.text.java.open.hierarchy",JSEDITOR,"CTRL+["));   									 //$NON-NLS-1$ //$NON-NLS-2$
		add(new MinderBinder("org.eclipse.wst.jsdt.ui.edit.text.java.toggle.comment",JSEDITOR,"ESC ;"));										 //$NON-NLS-1$ //$NON-NLS-2$
		add(new MinderBinder("org.eclipse.wst.jsdt.ui.edit.text.java.open.editor",JSVEDITOR,"ESC .",MarkUtils.getTagListener()));   			 //$NON-NLS-1$ //$NON-NLS-2$
		add(new MinderBinder("org.eclipse.wst.jsdt.ui.edit.text.java.open.editor",JSVEDITOR,"CTRL+C CTRL+V CTRL+Y",MarkUtils.getTagListener())); //$NON-NLS-1$ //$NON-NLS-2$

		// pde bindings
		add(new MinderBinder("org.eclipse.jdt.ui.edit.text.java.toggle.comment",PEDITOR,"ESC ;"));  											 //$NON-NLS-1$ //$NON-NLS-2$
		add(new MinderBinder("org.eclipse.pde.ui.quickOutline",PEDITOR,"CTRL+]"));  															 //$NON-NLS-1$ //$NON-NLS-2$

		// web tools xml bindings
		add(new MinderBinder("org.eclipse.wst.sse.ui.toggle.comment",XEDITOR,"ESC ;")); 														 //$NON-NLS-1$ //$NON-NLS-2$

		// c++ bindings
		add(new MinderBinder("org.eclipse.cdt.ui.edit.open.outline",CEDITOR,"CTRL+]")); 														 //$NON-NLS-1$ //$NON-NLS-2$
		add(new MinderBinder("org.eclipse.cdt.ui.edit.text.c.toggle.comment",CEDITOR,"ESC ;")); 												 //$NON-NLS-1$ //$NON-NLS-2$
		add(new MinderBinder("org.eclipse.cdt.ui.edit.open.quick.type.hierarchy",CEDITOR,"CTRL+["));											 //$NON-NLS-1$ //$NON-NLS-2$
		add(new MinderBinder("org.eclipse.cdt.ui.edit.opendecl",CEDITOR,"ESC .",MarkUtils.getTagListener()));   								 //$NON-NLS-1$ //$NON-NLS-2$
		add(new MinderBinder("org.eclipse.cdt.ui.edit.opendecl",CEDITOR,"CTRL+C CTRL+V CTRL+Y",MarkUtils.getTagListener()));   					 //$NON-NLS-1$ //$NON-NLS-2$

		// ruby bindings (aptana)
		add(new MinderBinder("org.rubypeople.rdt.ui.edit.text.ruby.toggle.comment",RBYEDITOR,"ESC ;")); 										 //$NON-NLS-1$ //$NON-NLS-2$
		add(new MinderBinder("org.rubypeople.rdt.ui.edit.text.ruby.open.editor",RBYEDITOR,"ESC .",MarkUtils.getTagListener())); 				 //$NON-NLS-1$ //$NON-NLS-2$
		add(new MinderBinder("org.rubypeople.rdt.ui.edit.text.ruby.open.editor",RBYEDITOR,"CTRL+C CTRL+V CTRL+Y",MarkUtils.getTagListener())); 	 //$NON-NLS-1$ //$NON-NLS-2$
		
	}};

	@SuppressWarnings("serial")
	private static final Set<MinderBinder> altBindings = new HashSet<MinderBinder>() {{

		// java bindings
		add(new MinderBinder("org.eclipse.jdt.ui.edit.text.java.select.enclosing",JEDITOR,"CTRL+M3+U"));										 //$NON-NLS-1$ //$NON-NLS-2$
		add(new MinderBinder("org.eclipse.jdt.ui.edit.text.java.toggle.comment",JEDITOR,"M3+;"));   											 //$NON-NLS-1$ //$NON-NLS-2$
		add(new MinderBinder("org.eclipse.jdt.ui.edit.text.java.open.editor",JEDITOR,"M3+.",MarkUtils.getTagListener()));   					 //$NON-NLS-1$ //$NON-NLS-2$

		// javascript bindings
		add(new MinderBinder("org.eclipse.wst.jsdt.ui.edit.text.java.select.enclosing",JSEDITOR,"CTRL+M3+U"));  								 //$NON-NLS-1$ //$NON-NLS-2$
		add(new MinderBinder("org.eclipse.wst.jsdt.ui.edit.text.java.toggle.comment",JSEDITOR,"M3+;")); 										 //$NON-NLS-1$ //$NON-NLS-2$
		add(new MinderBinder("org.eclipse.wst.jsdt.ui.edit.text.java.open.editor",JSVEDITOR,"M3+.",MarkUtils.getTagListener()));				 //$NON-NLS-1$ //$NON-NLS-2$

		// pde bindings
		add(new MinderBinder("org.eclipse.jdt.ui.edit.text.java.toggle.comment",PEDITOR,"M3+;"));   											 //$NON-NLS-1$ //$NON-NLS-2$
		
		// web tools xml bindings
		add(new MinderBinder("org.eclipse.wst.sse.ui.toggle.comment",XEDITOR,"M3+;"));  														 //$NON-NLS-1$ //$NON-NLS-2$

		// c++ bindings
		add(new MinderBinder("org.eclipse.cdt.ui.edit.text.c.toggle.comment",CEDITOR,"M3+;"));  												 //$NON-NLS-1$ //$NON-NLS-2$
		add(new MinderBinder("org.eclipse.cdt.ui.edit.opendecl",CEDITOR,"M3+.",MarkUtils.getTagListener()));									 //$NON-NLS-1$ //$NON-NLS-2$

		// ruby bindings (aptana)
		add(new MinderBinder("org.rubypeople.rdt.ui.edit.text.ruby.toggle.comment",RBYEDITOR,"M3+;"));  										 //$NON-NLS-1$ //$NON-NLS-2$
		add(new MinderBinder("org.rubypeople.rdt.ui.edit.text.ruby.open.editor",RBYEDITOR,"M3+.",MarkUtils.getTagListener()));  				 //$NON-NLS-1$ //$NON-NLS-2$
		
	}};

	@SuppressWarnings("serial")
	private static final Set<MinderBinder> cmdBindings = new HashSet<MinderBinder>() {{

		// java bindings
		add(new MinderBinder("org.eclipse.jdt.ui.edit.text.java.select.enclosing",JEDITOR,"CTRL+COMMAND+U"));   								 //$NON-NLS-1$ //$NON-NLS-2$
		add(new MinderBinder("org.eclipse.jdt.ui.edit.text.java.toggle.comment",JEDITOR,"COMMAND+;"));  										 //$NON-NLS-1$ //$NON-NLS-2$
		add(new MinderBinder("org.eclipse.jdt.ui.edit.text.java.open.editor",JEDITOR,"COMMAND+.",MarkUtils.getTagListener()));  				 //$NON-NLS-1$ //$NON-NLS-2$

		// javascript bindings
		add(new MinderBinder("org.eclipse.wst.jsdt.ui.edit.text.java.select.enclosing",JSEDITOR,"CTRL+COMMAND+U")); 							 //$NON-NLS-1$ //$NON-NLS-2$
		add(new MinderBinder("org.eclipse.wst.jsdt.ui.edit.text.java.toggle.comment",JSEDITOR,"COMMAND+;"));									 //$NON-NLS-1$ //$NON-NLS-2$
		add(new MinderBinder("org.eclipse.wst.jsdt.ui.edit.text.java.open.editor",JSVEDITOR,"COMMAND+.",MarkUtils.getTagListener()));   		 //$NON-NLS-1$ //$NON-NLS-2$

		// pde bindings
		add(new MinderBinder("org.eclipse.jdt.ui.edit.text.java.toggle.comment",PEDITOR,"COMMAND+;"));  										 //$NON-NLS-1$ //$NON-NLS-2$
		
		// web tools xml bindings
		add(new MinderBinder("org.eclipse.wst.sse.ui.toggle.comment",XEDITOR,"COMMAND+;")); 													 //$NON-NLS-1$ //$NON-NLS-2$

		// c++ bindings
		add(new MinderBinder("org.eclipse.cdt.ui.edit.text.c.toggle.comment",CEDITOR,"COMMAND+;")); 											 //$NON-NLS-1$ //$NON-NLS-2$
		add(new MinderBinder("org.eclipse.cdt.ui.edit.opendecl",CEDITOR,"COMMAND+.",MarkUtils.getTagListener()));   							 //$NON-NLS-1$ //$NON-NLS-2$

		// ruby bindings (aptana)
		add(new MinderBinder("org.rubypeople.rdt.ui.edit.text.ruby.toggle.comment",RBYEDITOR,"COMMAND+;")); 									 //$NON-NLS-1$ //$NON-NLS-2$
		add(new MinderBinder("org.rubypeople.rdt.ui.edit.text.ruby.open.editor",RBYEDITOR,"COMMAND+.",MarkUtils.getTagListener())); 			 //$NON-NLS-1$ //$NON-NLS-2$
		
	}};

}
