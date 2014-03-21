/**
 * Copyright (c) 2009, 2010 Mark Feber, MulgaSoft
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 */
package com.mulgasoft.emacsplus.minibuffer;

import java.util.SortedMap;

import org.eclipse.core.commands.Command;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.IExecutionListener;
import org.eclipse.core.commands.NotHandledException;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.commands.ICommandService;
import org.eclipse.ui.texteditor.ITextEditor;

import com.mulgasoft.emacsplus.Beeper;
import com.mulgasoft.emacsplus.EmacsPlusActivator;
import com.mulgasoft.emacsplus.EmacsPlusUtils;
import com.mulgasoft.emacsplus.IEmacsPlusCommandDefinitionIds;
import com.mulgasoft.emacsplus.RingBuffer;
import com.mulgasoft.emacsplus.execute.CommandHelp;
import com.mulgasoft.emacsplus.execute.CommandSupport;
import com.mulgasoft.emacsplus.execute.ICommandResult;
import com.mulgasoft.emacsplus.execute.MetaXDialog;
                                                            
/**
 * Minibuffer for reading a valid command, with completion, for execution
 * 
 * @author Mark Feber - initial API and implementation
 */
public class MetaXMinibuffer extends CompletionMinibuffer implements IExecutionListener {

	private static String NOCOMMANDS_MSG = A_MSG + EmacsPlusActivator.getResourceString("MetaX_NoCommands") + Z_MSG; //$NON-NLS-1$  
	private static String BINDINGS = EmacsPlusActivator.getResourceString("Cmd_Bindings");  						 //$NON-NLS-1$  
	
	private CommandSupport commander = null;
	private SortedMap<String, Command> commandList;
	
	// perform completion again on <CR> if needed
	boolean completeOnExit = true;
	// if true, then ignore disabled setting on command
	private boolean ignoreDisabled = false;

	/**
	 * @param executable
	 */
	public MetaXMinibuffer(IMinibufferExecutable executable) {
		super(executable);
		setLowercase(true);
	}

	protected String getMinibufferPrefix() {
		return getCompletionMinibufferPrefix();
	}
	
	/**
	 * @return the ignoreDisabled flag
	 */
	protected boolean isIgnoreDisabled() {
		return ignoreDisabled;
	}

	/**
	 * @param ignoreDisabled
	 */
	public void setIgnoreDisabled(boolean ignoreDisabled) {
		this.ignoreDisabled = ignoreDisabled;
	}

	// Process the command

	private void setResultString(String resultString) {
		setResultString(resultString, false);
	}

	ICommandResult copyCommand(final Command command, final String name) {
		return new ICommandResult() {
			public Command getCommand() {
				return command;
			}
			public String getName() {
				return name;
			}
		};
	}
	
	/**
	 * @see com.mulgasoft.emacsplus.minibuffer.ExecutingMinibuffer#executeResult(org.eclipse.ui.texteditor.ITextEditor, java.lang.Object)
	 */
	protected boolean executeResult(ITextEditor editor, Object command) {
		boolean result = true;
		closeDialog();
		CommandSupport commandControl = getCommandSupport();
		if (commandControl != null) {
			String commandS = (String) command;
			Command com = commandList.get(commandS);
			// Invoke completion on exit, if the string came through uncompleted
			if (completeOnExit && com == null) {
				try {
					SortedMap<String, Command> viewTree = commandControl
							.getCommandSubTree(commandList, commandS, false,
									isIgnoreDisabled());
					if (viewTree.size() == 1) {
						commandS = viewTree.firstKey();
						com = commandList.get(commandS);
					}
				} catch (Exception e) {
					// Could be a java.util.regex.PatternSyntaxException on weird input
					// when looking for a match; just ignore and command will abort
				}
			}
			if (com != null) {
				addToHistory(commandS); // add to command history
				setExecuting(true);
				setResultString(commandS);
				//				setResultMessage(commandS,false);
				try {
					setMxLaunch(true);
					result = exitExecuteResult(getEditor(), copyCommand(com,
							commandS));
				} finally {
					setMxLaunch(false);
				}
			}
		} 
		return result;
	}

	/**************** Specialize minibuffer ******************/
	
	protected void closeDialog() {
		try {
			if (getMiniDialog() != null) {
				((MetaXDialog)getMiniDialog()).shutdown();
				super.closeDialog();
			}
		} catch (Exception e) {}
	}

	private CommandSupport getCommandSupport() {
		if (commander == null) {
			commander = new CommandSupport();
			commandList = commander.getCommandList(getEditor());
			if (commandList.isEmpty()) {
				setResultString(NOCOMMANDS_MSG, true);
				commander = null;
				leave(true);
			}
		}
		return commander;
	}
	
	/**
	 * Expose the command completion dialog
	 */
	protected void showCompletions() {
		
		CommandSupport commandControl = getCommandSupport();
		if (commandControl != null) {
			String command = getMBString();
			SortedMap<String, Command> viewTree;
			if (isSearching() && getSearchStr().length() > 0) {
				@SuppressWarnings("unchecked")	// need local variable for unchecked annotation 
				SortedMap<String, Command> tmpTree = (SortedMap<String,Command>)getSearchResults();
				viewTree = tmpTree;
			} else {
				viewTree = commandControl.getCommandSubTree(commandList, command, false, isIgnoreDisabled());
			}
			if (viewTree != null) {
				if (viewTree.size() > 1) {
					if (getMiniDialog() == null) {
						setMiniDialog(new MetaXDialog(null, this, getEditor()));
					}
					((MetaXDialog) getMiniDialog()).open(viewTree);
					setShowingCompletions(true);
				}
				EmacsPlusUtils.forceStatusUpdate(getEditor());
				String newCommand;
				if (viewTree.size() == 0) {
					updateStatusLine((isSearching() ? EMPTY_STR : command) + NOMATCH_MSG);
				} else if (viewTree.size() == 1) {
					closeDialog();
					newCommand = viewTree.firstKey();
					if (!command.equals(newCommand) && !isSearching()) {
						initMinibuffer(newCommand);
					}
					updateStatusLine(newCommand + COMPLETE_MSG);
				} else if (!isSearching()) {
					if (command.length() > 0) {
						newCommand = commandControl.getCommonString(viewTree, command);
						if (!command.equals(newCommand)) {
							initMinibuffer(newCommand);
						}
					}
					updateStatusLine(getMBString());
				}
			} else {
				updateStatusLine(command + NOMATCH_MSG);
			}
		}
	}

	/**
	 * @see com.mulgasoft.emacsplus.minibuffer.CompletionMinibuffer#getCompletions(java.lang.String)
	 */
	@Override
	protected SortedMap<String,?> getCompletions(String searchSubstr) {
		SortedMap<String,?> result = null;
		CommandSupport commandControl = getCommandSupport();
		if (commandControl != null ) {
			result = commandControl.getCommandSubTree(commandList, RWILD + searchSubstr + RWILD, true, isIgnoreDisabled());
		}
		return result; 
	}
	
	/**
	 * @see com.mulgasoft.emacsplus.minibuffer.CompletionMinibuffer#getCompletions()
	 */
	@Override
	protected SortedMap<String, ?> getCompletions() {
		return null;
	}

	/**************** Specialize minibuffer ******************/

	@Override
	protected boolean initializeBuffer(ITextEditor editor, IWorkbenchPage page) {
		if (!getExecutable().isUniversalPresent()) {
			EmacsPlusUtils.clearMessage(editor);
		}
		return true;
	}

	@Override
	public boolean beginSession(ITextEditor editor, IWorkbenchPage page, ExecutionEvent event) {
		try {
			setMxLaunch(true);
			boolean result = super.beginSession(editor, page, event);
			if (!result){
				super.setResultMessage(NOCOMMANDS_MSG, true);
			}
			return result;
		} finally {
			setMxLaunch(false);
		}
	}
	
	protected void leave(boolean closeDialog) {
		try {
			super.leave(closeDialog);
		} finally {
			this.commandList = null;
		}
	}
	
	protected void crExitKbdMacro() {
		// meta-x doesn't appear in the kbd macro, so we don't register our exit
	}

	/**
	 * @see com.mulgasoft.emacsplus.minibuffer.ExecutingMinibuffer#setResultMessage(java.lang.String, boolean)
	 */
	@Override
	protected void setResultMessage(String message, boolean error) {
		String cmd;
		if (!isExecuting()) {
			if (error) {
				super.setResultMessage(message, true, true);
			} else {
				super.setResultMessage(ABORT_MSG, true, true);				
			}
			Beeper.interrupt();
		} else if ((cmd = getResultString()) != null) {
			Command com = null;
			if ((com = commandList.get(cmd)) != null) {
				// get the active bindings
				String bindings = CommandHelp.getKeyBindingString(com,true);
				if (bindings != null) {
					cmd = String.format(BINDINGS, cmd, bindings);					
				} else {
					cmd = null;
				}
			}
			// some commands want the status area to themselves, so don't update
			if (IEmacsPlusCommandDefinitionIds.statusCommands.get(com.getId())== null) {
				super.setResultMessage(cmd, false, true);
			}
		}
	}

	// Listeners
	
	/**
	 * @see com.mulgasoft.emacsplus.minibuffer.CompletionMinibuffer#addOtherListeners(IWorkbenchPage, ISourceViewer, StyledText)
	 */
	@Override
	protected void addOtherListeners(IWorkbenchPage page, ISourceViewer viewer, StyledText widget) {
		ICommandService commandService = (ICommandService) PlatformUI.getWorkbench().getAdapter(ICommandService.class);
		if (commandService != null) {
			commandService.addExecutionListener(this);
		}
		super.addOtherListeners(page, viewer, widget);
	}
	
	/**
	 * @see com.mulgasoft.emacsplus.minibuffer.CompletionMinibuffer#removeOtherListeners(IWorkbenchPage, ISourceViewer, StyledText)
	 */
	@Override
	protected void removeOtherListeners(IWorkbenchPage page, ISourceViewer viewer, StyledText widget) {
		ICommandService commandService = (ICommandService) PlatformUI.getWorkbench().getAdapter(ICommandService.class);
		if (commandService != null) {
			commandService.removeExecutionListener(this);
		}
		super.removeOtherListeners(page, viewer, widget);
	}

	// ISelectExecute method 
	/**
	 * Execute the command. Called from mouse click in MetaXDialog
	 * 
	 * @see com.mulgasoft.emacsplus.execute.ISelectExecute#execute(java.lang.Object)
	 */
	public void execute(Object selection) {
		String key = (String)selection;
		setExecuting(true);
		executeResult(getEditor(),key);
		leave(true);
	}

	// IFocusListener
	/**
	 * @see FocusListener#focusGained(org.eclipse.swt.events.FocusEvent)
	 */
	@Override
	public void focusGained(FocusEvent e) {
		 if (!isInBegin() && !isExecuting()) {
			 leave();
		 }
	}
	
	// ISelectionChangedListener
	/**
	 * @see com.mulgasoft.emacsplus.minibuffer.CompletionMinibuffer#selectionChanged(org.eclipse.jface.viewers.SelectionChangedEvent)
	 */
	public void selectionChanged(SelectionChangedEvent event) {
		if (!isExecuting()) {
			leave();
		}
	}
	
	// IExecutionListener
	/**
	 * @see org.eclipse.core.commands.IExecutionListener#notHandled(java.lang.String, org.eclipse.core.commands.NotHandledException)
	 */
	public void notHandled(String commandId, NotHandledException exception) {
		leave();
	}

	/**
	 * @see org.eclipse.core.commands.IExecutionListener#postExecuteFailure(java.lang.String, org.eclipse.core.commands.ExecutionException)
	 */
	public void postExecuteFailure(String commandId, ExecutionException exception) {
		if (!isExecuting()) {
			leave();
		}
	}

	/**
	 * @see org.eclipse.core.commands.IExecutionListener#postExecuteSuccess(java.lang.String, java.lang.Object)
	 */
	public void postExecuteSuccess(String commandId, Object returnValue) {
	}

	/**
	 * @see org.eclipse.core.commands.IExecutionListener#preExecute(java.lang.String, org.eclipse.core.commands.ExecutionEvent)
	 */
	public void preExecute(String commandId, ExecutionEvent event) {
	}

	/**** Local RingBuffer: use lazy initialization holder class idiom ****/

	/**
	 * @see com.mulgasoft.emacsplus.minibuffer.HistoryMinibuffer#getHistoryRing()
	 */
	@Override
	@SuppressWarnings("unchecked")	
	protected RingBuffer<String> getHistoryRing() {
		return MetaXRing.ring;
	}

	private static class MetaXRing {
		static final RingBuffer<String> ring = new RingBuffer<String>();
	}
	
}
