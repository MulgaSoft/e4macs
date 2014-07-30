/**
 * Copyright (c) 2009-2012 Mark Feber, MulgaSoft
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 */
package com.mulgasoft.emacsplus.preferences;


import org.eclipse.core.runtime.IProduct;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.ColorFieldEditor;
import org.eclipse.jface.preference.IntegerFieldEditor;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.jface.preference.RadioGroupFieldEditor;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

import com.mulgasoft.emacsplus.EmacsPlusActivation;
import com.mulgasoft.emacsplus.EmacsPlusActivator;
import com.mulgasoft.emacsplus.EmacsPlusUtils;
import com.mulgasoft.emacsplus.KillRing;
import com.mulgasoft.emacsplus.YankRotate;
import com.mulgasoft.emacsplus.commands.BlockHandler;
import com.mulgasoft.emacsplus.commands.BrowseKillRingHandler;
import com.mulgasoft.emacsplus.commands.SexpHandler;
import com.mulgasoft.emacsplus.commands.UndoRedoHandler;
import com.mulgasoft.emacsplus.commands.YankPopHandler;
import com.mulgasoft.emacsplus.e4.commands.E4WindowCmd;
import com.mulgasoft.emacsplus.minibuffer.SearchReplaceMinibuffer;

import static com.mulgasoft.emacsplus.preferences.PrefVars.DELETE_SEXP_TO_CLIPBOARD;
import static com.mulgasoft.emacsplus.preferences.PrefVars.DELETE_WORD_TO_CLIPBOARD;
import static com.mulgasoft.emacsplus.preferences.PrefVars.ENABLE_SPLIT_SELF;
import static com.mulgasoft.emacsplus.preferences.PrefVars.ENABLE_GNU_SEXP;
import static com.mulgasoft.emacsplus.preferences.PrefVars.ENABLE_DOT_SEXP;
import static com.mulgasoft.emacsplus.preferences.PrefVars.ENABLE_UNDER_SEXP;
import static com.mulgasoft.emacsplus.preferences.PrefVars.KILL_RING_MAX;
import static com.mulgasoft.emacsplus.preferences.PrefVars.REPLACE_TEXT_TO_KILLRING;

/**
 * This class represents a preference page that
 * is contributed to the Preferences dialog. By 
 * sub-classing <samp>FieldEditorPreferencePage</samp>, we
 * can use the field support built into JFace that allows
 * us to create a page that is small and knows how to 
 * save, restore and apply itself.
 * <p>
 * This page is used to modify preferences only. They
 * are stored in the preference store that belongs to
 * the main plug-in class. That way, preferences can
 * be accessed directly via the preference store.
 *
 * @author Mark Feber - initial API and implementation
 */
public class EmacsPlusPreferencePage extends EmacsPlusPreferenceBase {

	public EmacsPlusPreferencePage() {
		super(FLAT);
		setPreferenceStore(EmacsPlusActivator.getDefault().getPreferenceStore());
	}
	
	/**
	 * Creates the field editors. Field editors are abstractions of
	 * the common GUI blocks needed to manipulate various types
	 * of preferences. Each field editor knows how to save and
	 * restore itself.
	 */
	public void createFieldEditors() {
		addField(
			new BooleanFieldEditor(
				ENABLE_GNU_SEXP.getPref(),
				EmacsPlusActivator.getString("EmacsPlusPref_GnuSexp"),  									   //$NON-NLS-1$
				getFieldEditorParent()));
		addField(
			new BooleanFieldEditor(
				ENABLE_DOT_SEXP.getPref(),
				EmacsPlusActivator.getString("EmacsPlusPref_DotSexp"),  									   //$NON-NLS-1$
				getFieldEditorParent()));
		addField(
			new BooleanFieldEditor(
				ENABLE_UNDER_SEXP.getPref(),
				EmacsPlusActivator.getString("EmacsPlusPref_UnderSexp"),									   //$NON-NLS-1$
				getFieldEditorParent()));
		addField(
			new BooleanFieldEditor(
				EmacsPlusPreferenceConstants.P_EMACS_UNDO,
				EmacsPlusActivator.getString("EmacsPlusPref_EmacsUndo"),									   //$NON-NLS-1$
				getFieldEditorParent()));
		// only enabled when CTRL+0 is bound to UNIVERSAL_ARGUMENT
		if (EmacsPlusActivation.getInstance().hasDigitBindings(true)) {
			String key = (EmacsPlusUtils.isMac() ? "MacDigitArgument" : "DigitArgument");   				   //$NON-NLS-1$ //$NON-NLS-2$
			addField(
					new BooleanFieldEditor(
							EmacsPlusPreferenceConstants.P_CTRL_DIGIT_ARGUMENT,
							EmacsPlusActivator.getString(key), 
							getFieldEditorParent()));
		}
		// only enabled when running on Max OS X
		if (EmacsPlusUtils.isMac()) {
			addField(
					new BooleanFieldEditor(
							EmacsPlusPreferenceConstants.P_DISABLE_INLINE_EDIT,
							EmacsPlusActivator.getString("EmacsPlusPref_DisableInline"),					   //$NON-NLS-1$
							getFieldEditorParent()));
		}		
		addSpace();
		addField(
			new BooleanFieldEditor(
				EmacsPlusPreferenceConstants.P_GNU_YANK,
				EmacsPlusActivator.getString("EmacsPlusPref_GnuYank"),  									   //$NON-NLS-1$
				getFieldEditorParent()));
		addField(
			new BooleanFieldEditor(
				EmacsPlusPreferenceConstants.P_AUTO_BROWSE_KR,
				EmacsPlusActivator.getString("EmacsPlusPref_AutoBrowse"),   								   //$NON-NLS-1$
				getFieldEditorParent()));
		addField(
				new ColorFieldEditor(
				EmacsPlusPreferenceConstants.P_AUTO_BROWSE_HIGHLIGHT,
				EmacsPlusActivator.getString("EmacsPlusPref_BrowseHighlight"),  							   //$NON-NLS-1$
				getFieldEditorParent()));
		addField(
			new BooleanFieldEditor(
				REPLACE_TEXT_TO_KILLRING.getPref(),
				EmacsPlusActivator.getString("EmacsPlusPref_ReplacedToKill"),  								   //$NON-NLS-1$
				getFieldEditorParent()));
		
		addField(
			new BooleanFieldEditor(
				DELETE_WORD_TO_CLIPBOARD.getPref(),
				EmacsPlusActivator.getString("EmacsPlusPref_WordDeletes"),  								   //$NON-NLS-1$
				getFieldEditorParent()));
		addField(
			new BooleanFieldEditor(
				DELETE_SEXP_TO_CLIPBOARD.getPref(),
				EmacsPlusActivator.getString("EmacsPlusPref_SexpDeletes"),  								   //$NON-NLS-1$
				getFieldEditorParent()));
		IntegerFieldEditor ife =new IntegerFieldEditor(KILL_RING_MAX.getPref(), 
				EmacsPlusActivator.getString("EmacsPlusPref_KillRingLength"), getFieldEditorParent());  	   //$NON-NLS-1$
		ife.setValidRange(1, 100);
		addField(ife);
		RadioGroupFieldEditor radio = new RadioGroupFieldEditor(EmacsPlusPreferenceConstants.P_ROTATE_DIR,
				EmacsPlusActivator.getString("EmacsPlusPref_Rotate"), 2, new String[][] {   				   //$NON-NLS-1$
						{ EmacsPlusActivator.getString("EmacsPlusPref_RotateF"), YankRotate.FORWARD.id() },    //$NON-NLS-1$
						{ EmacsPlusActivator.getString("EmacsPlusPref_RotateB"), YankRotate.BACKWARD.id() } }, //$NON-NLS-1$
				getFieldEditorParent(), true);
		addField(radio);		
		addSpace();
		addField(
			new BooleanFieldEditor(
				ENABLE_SPLIT_SELF.getPref(),
				EmacsPlusActivator.getString("EmacsPlusPref_SplitSelf"),									   //$NON-NLS-1$
				getFieldEditorParent()));
		addSpace();
		addField(new LabelFieldEditor(
						EmacsPlusActivator.getString("EmacsPlusPref_InitialFrame"),							   //$NON-NLS-1$
						getFieldEditorParent()));
		addField(new RectangleFieldEditor(EmacsPlusPreferenceConstants.P_FRAME_INIT, 
				EmacsPlusActivator.getString("EmacsPlusPref_FXY"),											   //$NON-NLS-1$
				StringFieldEditor.UNLIMITED,
				StringFieldEditor.VALIDATE_ON_KEY_STROKE,
				getFieldEditorParent()));
		addSpace();
		addField(new LabelFieldEditor(
						EmacsPlusActivator.getString("EmacsPlusPref_DefaultFrame"),								//$NON-NLS-1$
						getFieldEditorParent()));
		addField(new RectangleFieldEditor(EmacsPlusPreferenceConstants.P_FRAME_DEF, 
				EmacsPlusActivator.getString("EmacsPlusPref_FXY"),												//$NON-NLS-1$
				StringFieldEditor.UNLIMITED,
				StringFieldEditor.VALIDATE_ON_KEY_STROKE,
				getFieldEditorParent()));
		addSpace();		
		ife =new IntegerFieldEditor(EmacsPlusPreferenceConstants.P_BLOCK_MOVE_SIZE, 
				EmacsPlusActivator.getString("EmacsPlusPref_BlockLines"), getFieldEditorParent());  		    //$NON-NLS-1$
		ife.setValidRange(1, 100);
		addField(ife);
 	}

	@Override
	public boolean performOk() {
		boolean result = super.performOk();
		if (result) {
			KillRing killring = KillRing.getInstance();
			
			SearchReplaceMinibuffer.enableGnuSubCommands(getPreferenceStore().getBoolean(EmacsPlusPreferenceConstants.P_GNU_YANK));
			SexpHandler.enableGnuSexp(getPreferenceStore().getBoolean(ENABLE_GNU_SEXP.getPref()));
			SexpHandler.enableDotSexp(getPreferenceStore().getBoolean(ENABLE_DOT_SEXP.getPref()));
			SexpHandler.enableUnderSexp(getPreferenceStore().getBoolean(ENABLE_UNDER_SEXP.getPref()));
			killring.setClipFeature(DELETE_SEXP_TO_CLIPBOARD, getPreferenceStore().getBoolean(DELETE_SEXP_TO_CLIPBOARD.getPref()));
			killring.setClipFeature(DELETE_WORD_TO_CLIPBOARD, getPreferenceStore().getBoolean(DELETE_WORD_TO_CLIPBOARD.getPref()));
			killring.setRotateDirection(getPreferenceStore().getString(EmacsPlusPreferenceConstants.P_ROTATE_DIR));
			
			killring.setSize(getPreferenceStore().getInt(KILL_RING_MAX.getPref()));
			YankPopHandler.setAutoBrowse(getPreferenceStore().getBoolean(EmacsPlusPreferenceConstants.P_AUTO_BROWSE_KR));
			BrowseKillRingHandler.setHighlightColor(
					PreferenceConverter.getColor(getPreferenceStore(), EmacsPlusPreferenceConstants.P_AUTO_BROWSE_HIGHLIGHT));
			killring.setSelectionReplace(getPreferenceStore().getBoolean(REPLACE_TEXT_TO_KILLRING.getPref()));
			
			E4WindowCmd.setSplitSelf(getPreferenceStore().getBoolean(ENABLE_SPLIT_SELF.getPref()));
			BlockHandler.setBlockSize(getPreferenceStore().getInt(EmacsPlusPreferenceConstants.P_BLOCK_MOVE_SIZE));
			UndoRedoHandler.setEmacsUndo(getPreferenceStore().getBoolean(EmacsPlusPreferenceConstants.P_EMACS_UNDO));

			if (EmacsPlusUtils.isMac()) {			
				EmacsPlusUtils.setOptionIMEPreferenece(getPreferenceStore().getBoolean(EmacsPlusPreferenceConstants.P_DISABLE_INLINE_EDIT));
			}	
			boolean restart = EmacsPlusActivation.getInstance().setDigitArgument(getPreferenceStore().getBoolean(EmacsPlusPreferenceConstants.P_CTRL_DIGIT_ARGUMENT));
			if (restart) {
				// request a restart if the digit-argument preference has changed, since,
				// after the first change, any subsequent changes are not cached correctly by the binding mechanism
				requestRestart(EmacsPlusActivator.getString("EmacsPlusPref_RestartDigitArgument")); 	//$NON-NLS-1$ 
			}
		}
		return result;
	}

	// When restart is suggested
	
	/**
	 * Pop up a message dialog to request the restart of the workbench
	 */
	private void requestRestart(String rePreference) {

		String reMessage = EmacsPlusActivator.getString("EmacsPlusPref_RestartMessage");	//$NON-NLS-1$ 
		IProduct product = Platform.getProduct();
		String productName = product != null && product.getName() != null ? product.getName() : 
			EmacsPlusActivator.getString("EmacsPlusPref_DefaultProduct");	//$NON-NLS-1$ 

		final String msg = String.format(reMessage, productName,rePreference);
		final String reTitle = EmacsPlusActivator.getString("EmacsPlusPref_RestartTitle");	//$NON-NLS-1$ 
		
		PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() {
			public void run() {
				if (PlatformUI.getWorkbench().isClosing())
					return;
				// yes == 0, no == 1
				MessageDialog dialog = new MessageDialog(getDefaultShell(),reTitle,null,msg, MessageDialog.QUESTION ,
						new String[] {IDialogConstants.YES_LABEL, IDialogConstants.NO_LABEL}, 0);
				if (dialog.open() != Window.CANCEL) {
					if (dialog.getReturnCode() == 0) {
						// restart workbench
						PlatformUI.getWorkbench().restart();
					}
				}
			}
		});
	}

	private Shell getDefaultShell() {
		Shell result = null;
		IWorkbench workbench = PlatformUI.getWorkbench();

		IWorkbenchWindow window = workbench.getActiveWorkbenchWindow();
		if (window != null) {
			result =  window.getShell();
		} else {
			IWorkbenchWindow[] windows = workbench.getWorkbenchWindows();
			if (windows.length > 0)
				result =  windows[0].getShell();
		}
		return result;
	}
}