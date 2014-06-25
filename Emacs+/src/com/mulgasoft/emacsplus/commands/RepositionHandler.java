/**
 * Copyright (c) 2009, 2014 Mark Feber, MulgaSoft
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package com.mulgasoft.emacsplus.commands;

import java.util.Iterator;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.ITextViewerExtension;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.jface.text.TextViewer;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.projection.ProjectionAnnotationModel;
import org.eclipse.jface.text.source.projection.ProjectionViewer;
import org.eclipse.ui.texteditor.ITextEditor;

import com.mulgasoft.emacsplus.MarkUtils;

/**
 * Implement Reposition Window (approximately)
 * Make the current definition visible by revealing as much as possible in the viewer
 * If called with uArg, move point to beginning of defun as well.
 * 
 * NB: This is a noop if the buffer has been narrowed
 * NB: No effort is made to determine the semantic contents of the projection
 * 
 * @author mfeber - Initial API and implementation
 */
public class RepositionHandler extends EmacsPlusNoEditHandler {

	/**
	 * A semi-hack... This uses stuff that may change at any time in Eclipse.  
	 * In the java editor, the projection annotation model contains the collapsible regions which correspond to methods (and other areas
	 * such as import groups).
	 * 
	 * This may work in other editor types as well... TBD
	 */
	protected int transform(ITextEditor editor, IDocument document, ITextSelection currentSelection,
			ExecutionEvent event) throws BadLocationException {
	
		ITextViewerExtension viewer = MarkUtils.getITextViewer(editor);
		if (viewer instanceof ProjectionViewer) {
			ProjectionAnnotationModel projection = ((ProjectionViewer)viewer).getProjectionAnnotationModel();
			@SuppressWarnings("unchecked") // the method name says it all
			Iterator<Annotation> pit = projection.getAnnotationIterator();
			while (pit.hasNext()) {
				Position p = projection.getPosition(pit.next());
				if (p.includes(currentSelection.getOffset())) {
					if (isUniversalPresent()) {
						// Do this here to prevent subsequent scrolling once range is revealed
						MarkUtils.setSelection(editor, new TextSelection(document, p.offset, 0));
					}
					// the viewer is pretty much guaranteed to be a TextViewer
					if (viewer instanceof TextViewer) {
						((TextViewer)viewer).revealRange(p.offset, p.length);
					}
					break;
				}
			}
		}
		return NO_OFFSET;		
	}
	
	@Override
	protected boolean isLooping() {
		return false;
	}

}
