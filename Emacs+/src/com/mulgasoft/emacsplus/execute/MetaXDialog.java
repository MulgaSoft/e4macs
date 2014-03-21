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
import org.eclipse.core.commands.common.NotDefinedException;
import org.eclipse.jface.bindings.Binding;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.ui.texteditor.ITextEditor;

import com.mulgasoft.emacsplus.EmacsPlusActivator;

/**
 * The command completion dialog window
 * 
 * @author Mark Feber - initial API and implementation
 */
public class MetaXDialog extends SelectionDialog {

	private static String METAX_DESC_HEADING = EmacsPlusActivator.getResourceString("MetaX_DescHeading");	//$NON-NLS-1$
	private static String METAX_KEY_HEADING = EmacsPlusActivator.getResourceString("MetaX_KeyHeading");	//$NON-NLS-1$
	private static String METAX_INFO = EmacsPlusActivator.getResourceString("MetaX_InfoTitle");	//$NON-NLS-1$
	
	private final static String ID_SALAD = "\t- "; //$NON-NLS-1$
	private final static String CID_BEFORE = "\t["; //$NON-NLS-1$
	private final static String CID_AFTER = "]"; //$NON-NLS-1$

	/**
	 * @param parent
	 * @param mini
	 * @param editor
	 */
	public MetaXDialog(Shell parent, ISelectExecute mini, ITextEditor editor) {
		super(parent, mini, editor);
		this.setInfoText(METAX_INFO);
	}

	protected boolean hasToolTips() {
		return true;
	}
	
	void showTip(String txt, ItemPkg tp, Table table)  {
		tip = new Shell((Shell) null, SWT.ON_TOP | SWT.TOOL);
		tip.setLayout(new FillLayout());
		tip.setBackground(table.getBackground());
		createCommandTip(tip, (Command) getSelectables().get(txt));
		Point size = tip.computeSize(SWT.DEFAULT, SWT.DEFAULT);
		Rectangle rect = tp.getBounds();
		Point pt = table.toDisplay(rect.x + getSizeAdjustment(), rect.y
				- size.y);
		tip.setBounds(pt.x, pt.y, size.x, size.y);
		tip.setVisible(true);
	}

	/**
	 * Create the tooltip for the command.
	 * It consists of:
	 *  - Command Name
	 *  - Command Id
	 *  - Description (if present)
	 *  - Key Bindings (if present)
	 *  
	 * @param parent
	 * @param cmd
	 * @return
	 */
	private Control createCommandTip(Composite parent, Command cmd) {

			Composite result = new Composite(parent, SWT.NO_FOCUS);
			if (cmd != null) {
				try {
					Color bg = parent.getBackground();
					result.setBackground(bg);
					GridLayout gridLayout = new GridLayout();
					gridLayout.numColumns = 1;
					result.setLayout(gridLayout);

					StyledText name = new StyledText(result, SWT.READ_ONLY | SWT.HIDE_SELECTION);
					name.setText(cmd.getName());
					name.setBackground(bg);
					name.setCaret(null);
					StyleRange styleBold = new StyleRange(0, cmd.getName().length(), null, bg);
					styleBold.fontStyle = SWT.BOLD;
					name.setStyleRange(styleBold);

					StyledText cid = new StyledText(result, SWT.READ_ONLY | SWT.HIDE_SELECTION);
					cid.setText(ID_SALAD + cmd.getId() + " ");	//$NON-NLS-1$
					cid.setBackground(bg);
					StyleRange styleIt = new StyleRange(0, ID_SALAD.length() + cmd.getId().length(), null, bg);
					styleIt.fontStyle = SWT.ITALIC;
					cid.setStyleRange(styleIt);
					cid.setCaret(null);

					Group descGroup = new Group(result, SWT.SHADOW_ETCHED_IN);
					descGroup.setBackground(bg);
					descGroup.setText(METAX_DESC_HEADING);
					StyledText desc = new StyledText(descGroup, SWT.WRAP | SWT.READ_ONLY | SWT.HIDE_SELECTION);
					String txt = cmd.getDescription();
					desc.setText((txt != null) ? txt : cmd.getName());
					desc.setCaret(null);
					desc.setBackground(bg);

					desc.setLayoutData(getGridData());
					descGroup.setLayout(gridLayout);
					descGroup.setLayoutData(getGridData());

					String[] bindings = getKeyBindingString(cmd);
					if (bindings.length > 0) {
						Group keyGroup = new Group(result, SWT.SHADOW_ETCHED_IN);
						keyGroup.setBackground(bg);
						keyGroup.setText(METAX_KEY_HEADING);
						StyledText binding;
						String key;
						String contextId;
						for (int i = 0; i < bindings.length; i++) {
							key = bindings[i];
							contextId = bindings[++i];
							binding = new StyledText(keyGroup, SWT.READ_ONLY | SWT.HIDE_SELECTION);
							binding.setText(key + CID_BEFORE + contextId + CID_AFTER);
							binding.setBackground(bg);
							styleBold = new StyleRange(0, key.length(), null, bg);
							styleBold.fontStyle = SWT.BOLD;
							styleIt = new StyleRange(key.length() + CID_BEFORE.length(), contextId.length(), null, bg);
							styleIt.fontStyle = SWT.ITALIC;
							binding.setStyleRange(styleBold);
							binding.setStyleRange(styleIt);
							binding.setCaret(null);
						}
						keyGroup.setLayout(gridLayout);
						keyGroup.setLayoutData(getGridData());
						keyGroup.pack();
					}
					result.pack();
				} catch (NotDefinedException e) {}
			}
		return result;
	}
	
	private GridData getGridData() {
		GridData gridData = new GridData();
		gridData.grabExcessHorizontalSpace = true;
		gridData.grabExcessVerticalSpace = true;
		gridData.horizontalAlignment = GridData.FILL;
		gridData.verticalAlignment = GridData.FILL;
		return gridData;
	}

	/**
	 * Get the displayable key-binding information for the command
	 * 
	 * @param com - the Command
	 * @return and array of binding information
	 */
	private String[] getKeyBindingString(Command com) {
		// get all key bindings for the command
		Binding[] bindings = CommandHelp.getBindings(com,false);		
		List<String> bindingInfo = new ArrayList<String>();
		for (Binding bind : bindings) {
			bindingInfo.add(bind.getTriggerSequence().toString());
			bindingInfo.add(bind.getContextId());
		}
		return bindingInfo.toArray(new String[0]);
	}
}
