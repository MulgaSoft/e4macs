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

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.texteditor.ITextEditor;

/**
 * @author Mark Feber - initial API and implementation
 */
public class BufferDialog extends SelectionDialog {

	/**
	 * @param parent
	 * @param mini
	 * @param editor
	 */
	public BufferDialog(Shell parent, ISelectExecute mini, ITextEditor editor) {
		super(parent, mini, editor);
	}

	protected boolean hasToolTips() {
		return true;
	}
	
	void showTip(String txt, ItemPkg tp, Table table)  {
		tip = new Shell((Shell) null, SWT.ON_TOP | SWT.TOOL);
		tip.setLayout(new FillLayout());
		tip.setBackground(table.getBackground());
		createBufferTip(tip, (IEditorReference)getSelectables().get(txt));
		Point size = tip.computeSize(SWT.DEFAULT, SWT.DEFAULT);
		Rectangle rect = tp.getBounds();
		Point pt = table.toDisplay(rect.x + getSizeAdjustment(), rect.y
				- size.y);
		tip.setBounds(pt.x, pt.y, size.x, size.y);
		tip.setVisible(true);
	}

	private Control createBufferTip(Composite parent, IEditorReference ref) {

		Composite result = new Composite(parent, SWT.NO_FOCUS);
		Color bg = parent.getBackground();
		result.setBackground(bg);
		GridLayout gridLayout = new GridLayout();
		gridLayout.numColumns = 1;
		result.setLayout(gridLayout);

		StyledText name = new StyledText(result, SWT.READ_ONLY | SWT.HIDE_SELECTION);
		// italics results in slightly clipped text unless extended
		String text = ref.getTitleToolTip() + ' ';
		name.setText(text);
		name.setBackground(bg);
		name.setCaret(null);
		StyleRange styleIt = new StyleRange(0, text.length(), null, bg);
		styleIt.fontStyle = SWT.ITALIC;
		name.setStyleRange(styleIt);
		
		return result;
	}
}
