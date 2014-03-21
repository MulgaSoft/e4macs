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

import java.io.IOException;

import org.eclipse.jface.text.IDocument;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.LineBackgroundListener;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.custom.VerifyKeyListener;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleView;
import org.eclipse.ui.console.IOConsole;
import org.eclipse.ui.console.IOConsoleOutputStream;
import org.eclipse.ui.console.TextConsolePage;
import org.eclipse.ui.console.TextConsoleViewer;
import org.eclipse.ui.internal.console.IOConsoleViewer;
import org.eclipse.ui.part.IPageBookViewPage;
import org.osgi.framework.Bundle;

import com.mulgasoft.emacsplus.EmacsPlusActivator;
import com.mulgasoft.emacsplus.EmacsPlusUtils;

/**
 * Extend a IOConsole for use as the Emacs+ console
 * 
 * @author Mark Feber - initial API and implementation
 */
@SuppressWarnings("restriction")	// for cast to internal class org.eclipse.ui.internal.console.IOConsoleViewer
public class EmacsPlusConsole extends IOConsole implements VerifyKeyListener {
	
	private static String WI_CONSOLE= EmacsPlusActivator.getResourceString("Cmd_Console");	//$NON-NLS-1$
	
	private static EmacsPlusConsole instance = null;
	
	static boolean pre33 = false;
	
	
	public Color highlightColor = new Color(null,237,237,252); // pale background
	private Color contextColor = new Color(null,0,0,255);      // blue
	private Color defaultColor = new Color(null,0,0,0); 	   // black
	
	private StyledText myWidget = null;
	private TextConsolePage myPage = null;
	private TextConsoleViewer myViewer = null;
	
	private IEmacsPlusConsoleKey keyHandler = null;
	
	/**
	 * Singleton
	 */
	private EmacsPlusConsole() {
		super(WI_CONSOLE,null);
	}
	
	public static EmacsPlusConsole getInstance() {
		if (instance == null) {
			pre33 = checkBundle();	// see kludge comment below
			instance = new EmacsPlusConsole();
			ConsolePlugin.getDefault().getConsoleManager().addConsoles(new IConsole[]{ instance });
		}
		return instance;
	}

	/**
	 * @see org.eclipse.ui.console.IOConsole#dispose()
	 */
	protected void dispose() {
		clear();
		super.dispose();
	}
	
	/**
	 * Clear console content.
	 */
	public void clear() {
		setName(WI_CONSOLE);
		removeBackground();
		setKeyHandler(null);
		if (!pre33) {
			clearConsole();	
		}
		IDocument document = getDocument();
		if (document != null) {
			document.set(EmacsPlusUtils.EMPTY_STR);
		}
		getWidget();
	}

	/**
	 * Expose setName functionality 
	 * 
	 * @see org.eclipse.ui.console.AbstractConsole#setName(java.lang.String)
	 */
	public void setName(String name) {
		super.setName(name);
	}
	
    /**
     * @see org.eclipse.ui.console.IConsole#createPage(org.eclipse.ui.console.IConsoleView)
     */
	public IPageBookViewPage createPage(IConsoleView view) {
    	IPageBookViewPage page = super.createPage(view);
        if (page instanceof TextConsolePage) {
        	myPage = (TextConsolePage)page;
        }
        return page;
    }

	public void setFocus(final boolean reset) {
		super.activate();
		if (getWidget() != null) {
			if (reset) {
				myViewer.revealRange(0, 0);
				myWidget.setTopIndex(0);
			}
			myWidget.setFocus();
		} 
	}

	private StyledText getWidget() {
		// we have to delay this, as the widget/viewer are not set up 
		// until after the console has finished all its delayed Jobs
		if (myWidget == null && myPage != null) {
			myViewer = myPage.getViewer();
			if (myViewer != null) {
				myWidget = myPage.getViewer().getTextWidget();
				myWidget.setEditable(false);
				// restricted: It is lame that we have to cast to internal class to remove this behavior
				if (myViewer instanceof IOConsoleViewer) {
					((IOConsoleViewer)myViewer).setAutoScroll(false);
				}
			}
		}
		return myWidget;
	}
	
	/**
	 * Print command help in the console.
	 */
	public void print(final String message, final Color c, final int style) {
		printMessage(message, c, style);
	}
	
	public void printContext(String context) {
		print(context,contextColor,SWT.ITALIC);
	}
	
	public void printBinding(String binding) {
		printBold(binding);
	}
	
	public void printBold(String str) {
		print(str,defaultColor,SWT.BOLD);		
	}
	
	public void printItalic(String str) {
		print(str,defaultColor,SWT.ITALIC);		
	}
	
	public void print(String message, int style) {
		this.print(message,null,style);
	}
	
	public void print(String message, Color c) {
		this.print(message,c,SWT.DEFAULT);
	}
	
	public void print(String message) {
		this.print(message,SWT.DEFAULT);
	}

	private IOConsoleOutputStream getOutputStream() {
		return newOutputStream();			
	}
	
	protected void printMessage(String message, Color c, int style) {

		if (message != null) {
			IOConsoleOutputStream outputStream = getOutputStream();
			outputStream.setActivateOnWrite(true);
			if (c != null) {
				outputStream.setColor(c);
			}
			outputStream.setFontStyle(style);
			try {
				outputStream.write(message);
				outputStream.flush();
				outputStream.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public int getLine() {
		int result = 0;
		StyledText st = getWidget();
		if (st != null ) {
			result = st.getLineAtOffset(st.getCaretOffset());
		}
		return result;
	}
	
	public int getOffset() {
		int result = 0;
		StyledText st = getWidget();
		if (st != null ) {
			result = st.getCaretOffset();
		}
		return result;
	}
	
	private LineBackgroundListener background = null;
	
	public void addBackground(LineBackgroundListener listener) {
		if (getWidget() != null && !myWidget.isDisposed()) {
			myWidget.addLineBackgroundListener(listener);
			background = listener;
		}
	}
	
	private void removeBackground() {
		if (background != null) {
			removeBackground(background);
			background = null;
		}
	}
	
	public void removeBackground(LineBackgroundListener listener) {
		if (getWidget() != null && !myWidget.isDisposed()) {
			myWidget.removeLineBackgroundListener(listener);
		}
	}
	
	/*
	 * Permit the command handler to specialize the console's key handling
	 */
	
	public void setKeyHandler(IEmacsPlusConsoleKey handler) {
		keyHandler = handler;
	}
	
	/**
	 * PageParticipant activation
	 */
	void onLine() {
		if (getWidget() != null) {
			if (keyHandler != null) {
				myViewer.appendVerifyKeyListener(this);
			}
		}
	}
	
	/**
	 * PageParticipant deactivation
	 */
	void offLine() {
		if (getWidget() != null) {
			myViewer.removeVerifyKeyListener(this);
		}
		
	}

	/**
	 * @see org.eclipse.swt.custom.VerifyKeyListener#verifyKey(org.eclipse.swt.events.VerifyEvent)
	 */
	public void verifyKey(VerifyEvent event) {
		handleKey(event);
	}
	
	/**
	 * Dispatch the key handling to the command
	 * 
	 * @param event
	 */
	protected void handleKey(VerifyEvent event) {
		if (!event.doit)
			return;

		if (keyHandler != null && event.character != 0) { // process typed character
			keyHandler.handleKey(event,myViewer);
		}
	}

	// kludge: (totally) due to Europa <-> Ganymede changes
	// There was an incompatible change in bug 202564 (https://bugs.eclipse.org/bugs/show_bug.cgi?id=202564)
	// Before the change: the change listener invoked by document.set("") would clear the partitioning information
	// After the change: clearConsole() needs to be called to effect this
	// But: calling clearConsole in the 3.2 version 'hangs' the console and nothing is printed
	// TODO: There must be a better way to either 1) clear the partition information or 2) get the bundle version
	private static boolean checkBundle() {
		boolean result = false;
		Bundle bundle = EmacsPlusActivator.getDefault().getBundle();
		for (Bundle b : bundle.getBundleContext().getBundles()) {
			if ("org.eclipse.ui.console".equals(b.getSymbolicName())) {			  //$NON-NLS-$
				Object v = b.getHeaders().get("Bundle-Version");   				  //$NON-NLS-$ 
				result = (v != null && v instanceof String && ((String)v).startsWith("3.2")); //$NON-NLS-$
				break;
			}
		}
		return result;
	}
}