package com.mulgasoft.emacsplus.e4.commands;

import javax.inject.Named;

import org.eclipse.e4.core.contexts.Active;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.ui.model.application.ui.MElementContainer;
import org.eclipse.e4.ui.model.application.ui.MUIElement;
import org.eclipse.e4.ui.model.application.ui.basic.MBasicFactory;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.model.application.ui.basic.MPartSashContainerElement;
import org.eclipse.e4.ui.model.application.ui.basic.MPartStack;
import org.eclipse.e4.ui.model.application.ui.basic.MStackElement;
import org.eclipse.ui.IEditorPart;

import com.mulgasoft.emacsplus.Beeper;
import com.mulgasoft.emacsplus.EmacsPlusUtils;
import com.mulgasoft.emacsplus.commands.EmacsPlusCmdHandler;

import static org.eclipse.e4.ui.workbench.modeling.EModelService.RIGHT_OF;
import static org.eclipse.e4.ui.workbench.modeling.EModelService.BELOW;

/**
 * Split the current window using the direction passed in the context
 * 
 * @author mfeber - Initial API and implementation
 */
public class WindowSplitCmd extends E4WindowCmd {
	
	private static final float ratio = 0.5f;

	public static int getDirection(boolean horizontal) {
		return (horizontal ? RIGHT_OF : BELOW);
	}
	
	@Execute
	public Object execute(@Active MPart apart, @Active IEditorPart editor, @Named(E4CmdHandler.CMD_CTX_KEY)int cmd, 
			@Active EmacsPlusCmdHandler handler) {
		if (handler.isUniversalPresent()) {
			// convenience hack
			// change setting without changing preference store
			setSplitSelf(!isSplitSelf());
		}
		split(apart, editor, cmd);
		return null;
	}
	
	/**
	 * Command method for window splitting
	 * 
	 * @param editor
	 */
	protected void split(MPart apart, IEditorPart editor, int location) {
		try {
			preSplit(apart, editor);
			splitIt(apart, location);
			reactivate(apart);
		} catch (Exception e) {
			Beeper.beep();
		}
	}
	
	/**
	 * Perform activities prior to splitting windows
	 * 
	 * @param editor
	 */
	protected void preSplit(MPart apart, IEditorPart editor) {
		if (isSplitSelf()) {
			try {
				//	TODO: Don't clone as Close Other Instances can't tell them apart				
				//				List<MUIElement> children = apart.getParent().getChildren();
				//				MPart clone = (MPart) modelService.cloneElement(apart, null);
				//				children.add(children.indexOf(apart),clone);
				EmacsPlusUtils.executeCommand("org.eclipse.ui.window.newEditor", null, editor);	//$NON-NLS-1$
			} catch (Exception e) {}
		}
	}

	protected void splitIt(MPart apart, int location) {
		PartAndStack ps = getParentStack(apart);
		MElementContainer<MUIElement> pstack = ps.getStack();
		if (pstack.getChildren().size() > 1) {
			MPart newpart = ps.getPart();		

			MPartStack nstack = getStack(newpart, pstack);
			// Let the model service take care of the rest of the split
			modelService.insert(nstack, (MPartSashContainerElement)pstack, location, ratio);
		}
	}
	
	/**
	 * Wrap the editor (MPart) up in a new PartStack
	 * @param apart
	 * @return the wrapped MPart
	 */
	private MPartStack getStack(MPart apart, MElementContainer<MUIElement> parent) {
		MPartStack result = MBasicFactory.INSTANCE.createPartStack();
		MStackElement stackElement = (MStackElement) apart;
		parent.getChildren().remove(apart);
		result.getChildren().add(stackElement);
		result.setSelectedElement(stackElement);
		return result;
	}

}
