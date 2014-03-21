/**
 * Copyright (c) 2009, 2010 Mark Feber, MulgaSoft
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 */
package com.mulgasoft.emacsplus;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.IExecutionListener;
import org.eclipse.core.commands.NotHandledException;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuCreator;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.swt.events.HelpListener;
import org.eclipse.swt.widgets.Event;
import org.eclipse.ui.IWindowListener;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.texteditor.IUpdate;

/**
 * Listeners used to implement kill ring capture for cut & copy commands
 * and for the clipboard during activation
 * 
 * @author Mark Feber - initial API and implementation
 */
public class KillRingListeners {
	
	static IWindowListener getActivationListener() {
		return StaticActivation.activationListener;
	}
	
	static IExecutionListener getExecListener(boolean isReverse){
		return (isReverse ?  StaticRevExecection.revexecutionListener : StaticExecection.executionListener);
	}

	static IExecutionListener getCopyExecListener(boolean isReverse) {
		return (isReverse ?  StaticRevCopyExecection.revcopyexecutionListener : StaticCopyExecection.copyexecutionListener);
	}
	
	private static class StaticExecection {
		private final static EmacsExecutionListener executionListener = new EmacsExecutionListener(false);
	}
	
	private static class StaticRevExecection {
		private final static EmacsExecutionListener revexecutionListener = new EmacsExecutionListener(true);
	}
	
	private static class StaticCopyExecection {
		private final static EmacsExecutionListener copyexecutionListener = new EmacsCopyExecutionListener(false);
	}
	
	private static class StaticRevCopyExecection {
		private final static EmacsExecutionListener revcopyexecutionListener = new EmacsCopyExecutionListener(true);
	}

	private static class StaticActivation {
		private final static IWindowListener activationListener = new IWindowListener() {
			// @Override
			public void windowActivated(IWorkbenchWindow window) {
				try {
					KillRing.getInstance().checkClipboard();
					// ignore all errors
				} catch (Exception e) {}
			}
			// @Override
			public void windowClosed(IWorkbenchWindow window) {}
			// @Override
			public void windowDeactivated(IWorkbenchWindow window) {}
			// @Override
			public void windowOpened(IWorkbenchWindow window) {}
		};
	}
	
	private static class EmacsExecutionListener implements IExecutionListener {
		private boolean isReverse = false;

		EmacsExecutionListener(boolean isReverse){
			this.isReverse = isReverse;
		}
		//@Override
		public void notHandled(String commandId, NotHandledException exception) {}

		//@Override
		public void postExecuteFailure(String commandId, ExecutionException exception) {
			KillRing.getInstance().setKillCmd(null);
		}

		//@Override
		public void postExecuteSuccess(String commandId, Object returnValue) {
			KillRing.getInstance().setKillCmd(null);
		}

		//@Override
		public void preExecute(String commandId, ExecutionEvent event) {
			KillRing.getInstance().setKillCmd(commandId);
			KillRing.getInstance().setReverse(isReverse);
		}

	}
	
	private static class EmacsCopyExecutionListener extends EmacsExecutionListener {
		EmacsCopyExecutionListener(boolean isReverse){
			super(isReverse);
		}
		// @Override
		public void postExecuteSuccess(String commandId, Object returnValue) {
			// See if the hack to wrap the menu action has already fired for the copy
			if (KillRing.getInstance().getKillCmd() != null) {
				KillRing.getInstance().addClipboardContents();
				KillRing.getInstance().setKillCmd(null);
			}
		}		
	}
	
	// Used when the command is not invoked from the keyboard (as menu execution takes a different path) 	
	// Delegate Hackery
	
	static class EmacsCopyActionDelegate extends EmacsActionDelegate{
		// retrieves clipboard contents on execution
		public EmacsCopyActionDelegate(IAction action, String commandId) {
			super(action, commandId);
		}

		public void run() {
			KillRing.getInstance().setKillCmd(getCommandId());
			KillRing.getInstance().setReverse(false);
			getAction().run();
			KillRing.getInstance().addClipboardContents();
			KillRing.getInstance().setKillCmd(null);
		}

		public void runWithEvent(Event event) {
			KillRing.getInstance().setKillCmd(getCommandId());
			KillRing.getInstance().setReverse(false);
			getAction().runWithEvent(event);
			KillRing.getInstance().addClipboardContents();
			KillRing.getInstance().setKillCmd(null);
		}

	}
	
	static class EmacsCutActionDelegate extends EmacsActionDelegate{
		// Just flag the kill type command, retrieval is handled by the 
		// kill ring's document change listener
		public EmacsCutActionDelegate(IAction action, String commandId) {
			super(action, commandId);
		}

		public void run() {
			KillRing.getInstance().setKillCmd(getCommandId());
			KillRing.getInstance().setReverse(false);
			getAction().run();
			KillRing.getInstance().setKillCmd(null);
		}

		public void runWithEvent(Event event) {
			KillRing.getInstance().setKillCmd(getCommandId());
			KillRing.getInstance().setReverse(false);
			getAction().runWithEvent(event);
			KillRing.getInstance().setKillCmd(null);
		}

	}

	static class EmacsActionDelegate implements IAction, IUpdate {
		
		private IAction action;
		private String commandId;

		IAction getAction() {
			return action;
		}
		
		String getCommandId() {
			return commandId;
		}
		
		public EmacsActionDelegate(IAction action,String commandId){
			this.action = action;
			this.commandId = commandId;
		}
		
		public void addPropertyChangeListener(IPropertyChangeListener listener) {
			action.addPropertyChangeListener(listener);
		}

		public int getAccelerator() {
			return action.getAccelerator();
		}

		public String getActionDefinitionId() {
			return action.getActionDefinitionId();
		}

		public String getDescription() {
			return action.getDescription();
		}

		public ImageDescriptor getDisabledImageDescriptor() {
			return action.getDisabledImageDescriptor();
		}

		public HelpListener getHelpListener() {
			return action.getHelpListener();
		}

		public ImageDescriptor getHoverImageDescriptor() {
			return action.getHoverImageDescriptor();
		}

		public String getId() {
			return action.getId();
		}

		public ImageDescriptor getImageDescriptor() {
			return action.getImageDescriptor();
		}

		public IMenuCreator getMenuCreator() {
			return action.getMenuCreator();
		}

		public int getStyle() {
			return action.getStyle();
		}

		public String getText() {
			return action.getText();
		}

		public String getToolTipText() {
			return action.getToolTipText();
		}

		public boolean isChecked() {
			return action.isChecked();
		}

		public boolean isEnabled() {
			return action.isEnabled();
		}

		public boolean isHandled() {
			return action.isHandled();
		}

		public void removePropertyChangeListener(IPropertyChangeListener listener) {
			action.removePropertyChangeListener(listener);
		}

		public void run() {
			KillRing.getInstance().setKillCmd(commandId);
			KillRing.getInstance().setReverse(false);
			action.run();
			KillRing.getInstance().setKillCmd(null);
		}

		public void runWithEvent(Event event) {
			KillRing.getInstance().setKillCmd(commandId);
			KillRing.getInstance().setReverse(false);
			action.runWithEvent(event);
			KillRing.getInstance().setKillCmd(null);
		}

		public void setAccelerator(int keycode) {
			action.setAccelerator(keycode);
		}

		public void setActionDefinitionId(String id) {
			action.setActionDefinitionId(id);
		}

		public void setChecked(boolean checked) {
			action.setChecked(checked);
		}

		public void setDescription(String text) {
			action.setDescription(text);
		}

		public void setDisabledImageDescriptor(ImageDescriptor newImage) {
			action.setDisabledImageDescriptor(newImage);
		}

		public void setEnabled(boolean enabled) {
			action.setEnabled(enabled);
		}

		public void setHelpListener(HelpListener listener) {
			action.setHelpListener(listener);
		}

		public void setHoverImageDescriptor(ImageDescriptor newImage) {
			action.setHoverImageDescriptor(newImage);
		}

		public void setId(String id) {
			action.setId(id);
		}

		public void setImageDescriptor(ImageDescriptor newImage) {
			action.setImageDescriptor(newImage);
		}

		public void setMenuCreator(IMenuCreator creator) {
			action.setMenuCreator(creator);
		}

		public void setText(String text) {
			action.setText(text);
		}

		public void setToolTipText(String text) {
			action.setToolTipText(text);
		}
		
		/**
		 * @see org.eclipse.ui.texteditor.IUpdate#update()
		 */
		public void update() {
			if (action instanceof IUpdate) {
				((IUpdate)action).update();
			}
		}

	}
}
