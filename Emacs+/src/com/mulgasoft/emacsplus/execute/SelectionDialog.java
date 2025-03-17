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

import java.util.SortedMap;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.PopupDialog;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseTrackListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.ui.contexts.IContextService;
import org.eclipse.ui.texteditor.ITextEditor;

// with help from org.eclipse.ui.internal.keys.KeyAssistDialog;
/**
 * @author Mark Feber - initial API and implementation
 */
public class SelectionDialog extends PopupDialog {

	// TODO: there should be a way to determine this value
	private static int SIZE_ADJUST = 15;
	private static int MIN_COLUMNS = 3;
	
	private Point sizeHint; 
	private boolean clicked = false;
	private boolean mouseIn = false;
	
	protected ITextEditor editor;
	protected ISelectExecute minibuffer;
	
	private SortedMap<String,?> selectables = null;
	protected Shell tip = null;	
	
	protected void setSelectables(SortedMap<String,?> selectables) {
		this.selectables = selectables;
	}
	
	protected SortedMap<String,?> getSelectables() {
		return selectables;
	}
	
	protected boolean hasSelectables() {
		return selectables != null && selectables.size() > 0;
	}
	
	protected void clearSelectables() {
		selectables = null;
	}
	
	protected String[] getSelectableKeys() {
		return ((selectables != null) ? selectables.keySet().toArray(new String[0]) : null);
	}

    //		super(parent, shellStyle, takeFocusOnOpen, persistSize, persistLocation, showDialogMenu, showPersistActions, titleText, infoText);
	@SuppressWarnings("deprecation")	// backward compatibility
	public SelectionDialog(Shell parent, ISelectExecute mini, ITextEditor editor) {
		// Europa compatible constructor
		super((Shell) null, PopupDialog.HOVER_SHELLSTYLE, false, false, false, false, false, null, null);
		this.editor = editor;
		this.minibuffer = mini;
	}
	
	public void shutdown() {
		this.editor = null;
		this.minibuffer = null;
		clearSelectables();
		close();
	}
	
	// TODO rework..as super.open seems to repeat the create!
	public final int open(SortedMap<String, ?> selectables) {
		
		setSelectables(selectables);
		
		Shell shell = getShell();
		if (shell != null) {
			close();
		}
		sizeHint = configureSize();
		create();
		// Configure the size and location.
		configureLocation(sizeHint);
		// and open the dialog
		return super.open();
	}
	
	@Override
	public boolean close() {
		// make sure we're clean
		disposeTip(); 
		return super.close();
	}
	
	public boolean mouseIn() {
		return mouseIn;
	}
	
	private void disposeTip() {
		if (tip != null && !tip.isDisposed()) {
			tip.dispose();
			tip = null;
		}
	}
	
	/**
	 * behavior on mouse click
	 */
	protected void execute(String key) {
		minibuffer.execute(key);
	}

	protected int getSizeAdjustment() {
		return SIZE_ADJUST;
	}

	/**
	 * Sets the size for the dialog.
	 *  
	 * The width is slightly less the workbench window's width. 
	 * The dialog's height is the packed height of the dialog up to a 
	 * maximum of half the height of the workbench window.
	 * 
	 * @return The size of the dialog
	 */
	private final Point configureSize() {

		int maxW = 0;
		int maxH = 0;
		// Enforce maximum sizing.
		Shell workbenchWindowShell = editor.getEditorSite().getShell();
		if (workbenchWindowShell != null) {
			Point workbenchWindowSize = workbenchWindowShell.getSize();
			// TODO: just a guess for now.  I'd like to determine the current borders and subtract them
			//			int maxW = workbenchWindowSize.x -25;
			maxW = workbenchWindowShell.getClientArea().width - getSizeAdjustment();
			maxH = workbenchWindowSize.y / 2;

		}
		return new Point(maxW, maxH);
	}
	
	/**
	 * Sets the position for the dialog based on the position of the workbench
	 * window. The dialog is flush with the bottom right corner of the workbench
	 * window. However, the dialog will not appear outside of the display's
	 * client area.
	 * 
	 * @param size
	 *            The final size of the dialog; must not be <code>null</code>.
	 */
	private final void configureLocation(final Point size) {
		final Shell shell = getShell();
		
		final Shell workbenchWindowShell = editor.getEditorSite().getShell();
		final int xCoord;
		final int yCoord;
		if (workbenchWindowShell != null) {
			/*
			 * Position the shell at the bottom right corner of the workbench
			 * window
			 */
			// TODO: The constants are just guesses
			final Rectangle workbenchWindowBounds = workbenchWindowShell
					.getBounds();
			xCoord = workbenchWindowBounds.x + workbenchWindowBounds.width - size.x - 10;
			yCoord = workbenchWindowBounds.y + workbenchWindowBounds.height - size.y - 35;

		} else {
			xCoord = 0;
			yCoord = 0;

		}
		final Rectangle bounds = new Rectangle(xCoord, yCoord, size.x, size.y);
		shell.setBounds(getConstrainedShellBounds(bounds));
	}
	
		/**
	 * Creates the content area for the key assistant. This creates a table and
	 * places it inside the composite. The composite will contain a list of all
	 * the key bindings.
	 * 
	 * @param parent
	 *            The parent composite to contain the dialog area; must not be
	 *            <code>null</code>.
	 */
	protected final Control createDialogArea(final Composite parent) {
		// First, register the shell type with the context support
		registerShellType();

		// Create a composite for the dialog area.
		final Composite composite = new Composite(parent, SWT.NONE);
		composite.setBackground(parent.getBackground());
		createTableDialogArea(composite);

		return composite;
	}

	private final void createTableDialogArea(final Composite parent) {

		String[] inputKeys = getSelectableKeys();
		int columnCount = 0;
		Point dimens = getColumnCount( parent, inputKeys, sizeHint.x); 
		int count = dimens.x;

		GridLayout compositeLayout = new GridLayout(count,true);
		parent.setLayout(compositeLayout);
		parent.setLayoutData(new GridData(GridData.FILL_BOTH));
		
		Table table = new Table(parent, SWT.V_SCROLL | SWT.HORIZONTAL | SWT.WRAP | SWT.FULL_SELECTION); //| SWT.MULTI);
		GridData gridData = new GridData(GridData.FILL_BOTH);
		table.setLayoutData(gridData);
		table.setBackground(parent.getBackground());
		table.setLinesVisible(true);
		table.setHeaderVisible(false);
		
		int columnWidth = (sizeHint.x - getSizeAdjustment()) / count;
		
		TableColumn[] columns = new TableColumn[count];
		for (int i = 0; i < count; i++) {
			columns[i] = new TableColumn(table, SWT.LEFT, columnCount++);
			columns[i].setWidth(columnWidth);
		}
		TableColumnLayout layout = new TableColumnLayout();
		for (int i = 0; i < count; i++) {
			layout.setColumnData(columns[i], new ColumnWeightData(100/count,columnWidth,false));
		}
		parent.setLayout(layout);
		
		int len = inputKeys.length;
		int rowCount = len / columnCount;
		if ((len - rowCount * columnCount) > 0) {
			rowCount++;
		}
		for (int i = 0; i < rowCount; i++) {
			String[] row = new String[columnCount];
			for (int j = 0; j < columnCount; j++) {
				int sourceIndex = i * columnCount + j;
				row[j] = (sourceIndex < len ? (String) inputKeys[sourceIndex] : ""); //$NON-NLS-1$ 
			}
			TableItem item = new TableItem(table, SWT.NULL);
			item.setText(row);
		}

		table.pack();
		sizeHint.y = Math.min(table.getBounds().height + getSizeAdjustment(),sizeHint.y);

		Dialog.applyDialogFont(parent);
		addTableListeners(table);
	}
	
	/**
	 * Determine the appropriate number of columns.  If the number of items is less than
	 * the computed number, choose the smaller.
	 * 
	 * @param parent
	 * @param vals
	 * @param width
	 * @return
	 */
	private Point getColumnCount(Composite parent, Object[] vals, int width) {
		Point result = new Point(0,0);
		int iWidth = MIN_COLUMNS;
		int len = vals.length;
		int size = 0;
		Label l = null;
		try {
			l = new Label(parent, SWT.NULL);
			for (int i = 0; i < len; i++) {
				l.setText((String) vals[i]);
				size = Math.max(size, l.computeSize(SWT.DEFAULT, SWT.DEFAULT).x);
			}
			result = l.getSize();
			l.dispose();

			result.x = Math.max(iWidth, width / (size + 5));
			result.x = Math.min(result.x, vals.length);
		} catch (Exception e) {
			result = new Point(0,0);
		}
		return result;
	}

	// Listeners
	
	class ItemPkg {
		int index = 0;
		TableItem item = null;
		ItemPkg(TableItem item, int index){
			this.item = item;
			this.index = index;
		}
		String getText() {
			return item.getText(index);
		}
		Rectangle getBounds() {
			return item.getBounds(index);
		}
	}
	
	private ItemPkg getCell(Event event, Table table) {
		ItemPkg result = null;
		TableItem item = table.getItem(new Point(event.x,event.y));

		if (item != null) {
			for (int i = 0; i < table.getColumnCount(); i++) {
				if (item.getBounds(i).contains(event.x, event.y)) {
					result = new ItemPkg(item,i);
					break;
				}
			}
		}
		return result;
	}
	private String getCellText(Event event, Table table) {
		String result = null;
		ItemPkg item = getCell(event,table);
		if (item != null){
			result = item.getText();
		}
		return result;
	}
	
	private ItemPkg getCell(MouseEvent event, Table table) {
		ItemPkg result = null;
		TableItem item = table.getItem(new Point(event.x,event.y));

		if (item != null) {
			for (int i = 0; i < table.getColumnCount(); i++) {
				if (item.getBounds(i).contains(event.x, event.y)) {
					result = new ItemPkg(item,i);
					break;
				}
			}
		}
		return result;
	}
	
	protected boolean hasToolTips() {
		return false;
	}
	
	void showTip(String txt, ItemPkg tp, Table table) {}

	private void addTableListeners(final Table table) {

		final Color fc = table.getForeground();
		final Color bc = table.getBackground();

		if (hasToolTips()) {
			addToolTipListener(table);
		}
		
		// We need to listen to enter/exit so we know on
		// minibuffer focusLost whether the focus was 
		// lost to the selection dialog, or some other part
		table.addListener(SWT.MouseEnter, new Listener() {

			public void handleEvent(Event event) {
				mouseIn = true;
			}
		});

		table.addListener(SWT.MouseExit, new Listener() {

			public void handleEvent(Event event) {
				mouseIn = false;
			}
		});
		
		table.addListener(SWT.MouseDown, new Listener() {

			public void handleEvent(Event event) {
				clicked = true;
			}
		});

		table.addListener(SWT.MouseDoubleClick, new Listener() {

			public void handleEvent(Event event) {
				String t = getCellText(event, table);
				if (t != null) {
					disposeTip();
					execute(t);
				}
			}
		});

		// TODO: Can we do better?
		// Disable lame eclipse selection behavior
		table.addListener(SWT.EraseItem, new Listener() {
			public void handleEvent(Event event) {
				if ((event.detail & SWT.SELECTED) != 0) {
					//						Color ofc = event.gc.getForeground();
					Color obc = event.gc.getBackground();
					event.gc.setForeground(fc);
					event.gc.setBackground(bc);
					TableItem item = (TableItem) event.item;
					//						Rectangle r = item.getBounds(event.index);
					event.gc.setBackground(item.getBackground(event.index));
					event.gc.setForeground(item.getForeground(event.index));
					//							event.gc.drawText(" ", r.x, r.y, false);
					//							event.gc.drawText(item.getText(), r.x, r.y, false);

					//						 event.gc.drawString(event.text, event.x, event.y);
					event.detail &= ~SWT.SELECTED;
					event.detail &= ~SWT.BACKGROUND;
					event.detail &= ~SWT.FOCUSED;
					event.detail &= ~SWT.HOT;
					//							event.doit = false;
					//							event.gc.setForeground(ofc);
					event.gc.setBackground(obc);
				}
			}
		});
	}

	/**
	 * If sub class support tool tips, than add generic tip handling
	 * 
	 * @param table
	 */
	private void addToolTipListener(final Table table) {
		table.addMouseTrackListener(new MouseTrackListener() {

			public void mouseEnter(MouseEvent e) {
				disposeTip();
			}

			public void mouseExit(MouseEvent e) {
				disposeTip();
				if (clicked) {
					close();
				}
			}
			
			public void mouseHover(MouseEvent e) {
				ItemPkg tp = getCell(e, table);
				if (tp != null) {
					String t = tp.getText();
					disposeTip();
					if (t != null) {
						showTip(t,tp,table);
					}
				}
			}
		});
	}

	// TODO: What does it all mean Mr. Natural?
	/**
	 * Registers the shell as the same type as its parent with the context
	 * support. This ensures that it does not modify the current state of the
	 * application.
	 */
	private final void registerShellType() {
		final Shell shell = getShell();
		
		final IContextService contextService = (IContextService) editor.getEditorSite().getWorkbenchWindow().getWorkbench().getService(IContextService.class);
		contextService.registerShell(shell, contextService.getShellType((Shell) shell.getParent()));
	}

}
