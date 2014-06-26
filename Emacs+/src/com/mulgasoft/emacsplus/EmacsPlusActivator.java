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

import java.util.ArrayList;
import java.util.List;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleListener;

import com.mulgasoft.emacsplus.execute.KbdMacroSupport;

/**
 * The main activator class to be used in the desktop.
 *
 * @author Mark Feber - initial API and implementation
 */
public class EmacsPlusActivator extends AbstractUIPlugin {
	// The plug-in ID
	public static final String PLUGIN_ID = "com.mulgasoft.emacsplus";   			  //$NON-NLS-1$
	public static final String PLUGIN_RESOURCE = "com.mulgasoft.emacsplus.EmacsPlus"; //$NON-NLS-1$
	// The shared instance.
	private static EmacsPlusActivator plugin;
	// Resource bundle.
	private ResourceBundle resourceBundle;
	// The ids of  
	private ArrayList<String> emacsIds = null;

	/**
	 * The constructor.
	 */
	public EmacsPlusActivator() {
		super();
		plugin = this;
		try {
			resourceBundle = ResourceBundle.getBundle(PLUGIN_RESOURCE);
		} catch (MissingResourceException x) {
			resourceBundle = null;
		}
	}

	/**
	 * Returns the shared instance.
	 */
	public static EmacsPlusActivator getDefault() {
		return plugin;
	}

	/**
	 * This method is called upon plug-in activation
	 */
	@Override
	public void start(BundleContext context) throws Exception {
		addBundlerListener(context);		
		super.start(context);
		EmacsPlusActivation.getInstance().activateListeners();
	}

	private void addBundlerListener(BundleContext context) {
		// Listen for our STARTED event
		// Alternatively, we can get the bundle from org.osgi.framework.FrameworkUtil
		context.addBundleListener(new BundleListener() {
			public void bundleChanged(BundleEvent event) {
				String name = event.getBundle().getSymbolicName();
				if (name != null && name.equals(PLUGIN_ID)) {
					switch (event.getType()) {
						case BundleEvent.STARTED:
							bundleStarted();
							break;
						case BundleEvent.UNINSTALLED:
							emacsIds = null;
							break;
						default:
							break;
					}
				}
			}
		});
	}
	
	private void bundleStarted() {
		setEmacsIds();
		// must be run in a UI thread
		EmacsPlusUtils.asyncUiRun(new Runnable() {
			public void run() {
				KbdMacroSupport.getInstance().autoLoadMacros();
			}
		});
	}
	
	/**
	 * This method is called when the plug-in is stopped
	 */
	@Override
	public void stop(BundleContext context) throws Exception {
		super.stop(context);
	}

	/**
	 * Returns the string from the plugin's resource bundle,
	 * or 'key' if not found.
	 */
	public static String getResourceString(String key) {
		ResourceBundle bundle = EmacsPlusActivator.getDefault().getResourceBundle();
		try {
			return (bundle != null) ? bundle.getString(key) : key;
		} catch (MissingResourceException e) {
			return key;
		}
	}
	public static String getString(String key) {
		return  getResourceString(key);
	}
	/**
	 * Returns the plugin's resource bundle,
	 */
	public ResourceBundle getResourceBundle() {
		return resourceBundle;
	}
	
	/**
	 * Get the id list of Emacs+ plugin's components
	 * 
	 * @return the list (never <code>null</code>) which may be empty if Emacs+ hasn't finished starting.
	 */
	// is there a better way?
	public List<String> getLoadedList() {
		if (emacsIds == null) {
			return new ArrayList<String>();
		} else {
			return emacsIds;
		}
	}

	// is there a better way?
	private void setEmacsIds() {
		ArrayList<String> tmp = new ArrayList<String>();
		Bundle[] bundles = plugin.getBundle().getBundleContext().getBundles();
		for (Bundle b : bundles) {
			String name = b.getSymbolicName();
			if (name != null && name.startsWith(EmacsPlusUtils.MULGASOFT)) {
				tmp.add(name);
			}
		}
		emacsIds = tmp;
	}
}
