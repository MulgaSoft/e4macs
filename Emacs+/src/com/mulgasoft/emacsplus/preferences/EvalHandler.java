/**
 * Copyright (c) 2009-2012 Mark Feber, MulgaSoft
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package com.mulgasoft.emacsplus.preferences;

import java.util.Arrays;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.ui.texteditor.ITextEditor;

import com.mulgasoft.emacsplus.EmacsPlusActivator;
import com.mulgasoft.emacsplus.commands.INonEditingCommand;
import com.mulgasoft.emacsplus.commands.PreferenceHandler;
import com.mulgasoft.emacsplus.minibuffer.BooleanMinibuffer;
import com.mulgasoft.emacsplus.minibuffer.ExecutingMinibuffer;
import com.mulgasoft.emacsplus.minibuffer.IMinibufferExecutable;
import com.mulgasoft.emacsplus.minibuffer.IMinibufferState;
import com.mulgasoft.emacsplus.minibuffer.EvalMinibuffer;
import com.mulgasoft.emacsplus.minibuffer.StrictMinibuffer;
import com.mulgasoft.emacsplus.minibuffer.ReadNumberMinibuffer;
import com.mulgasoft.emacsplus.minibuffer.TextMinibuffer;

/**
 * Set preference variables to a value dynamically
 * 
 * @author Mark Feber - initial API and implementation
 */
public class EvalHandler extends PreferenceHandler implements IMinibufferExecutable, INonEditingCommand {

	private final static String PROMPT = ": ";  											//$NON-NLS-1$
	private final static String INIT = "M-";												//$NON-NLS-1$
	private final static String ABORT = EmacsPlusActivator.getResourceString("Exec_Abort"); //$NON-NLS-1$
	private final static String SV = EmacsPlusActivator.getResourceString("Set_Variable"); //$NON-NLS-1$

	// the state object for linked minibuffers
	protected IMinibufferState mbState = null;

	enum EvalType {
		setq(SV), eval;
		EvalType(){}
		EvalType(String prompt){typePrompt = prompt;}
		String typePrompt = null;
		/**
		 * @see java.lang.Enum#toString()
		 */
		@Override
		public String toString() {
			return (typePrompt != null ? typePrompt : super.toString());
		}
	};

	protected String getTypePrompt(EvalType e) {
		return e.name();
	}
	
	/**
	 * @see com.mulgasoft.emacsplus.commands.EmacsPlusCmdHandler#transform(org.eclipse.ui.texteditor.ITextEditor,
	 *      org.eclipse.jface.text.IDocument, org.eclipse.jface.text.ITextSelection,
	 *      org.eclipse.core.commands.ExecutionEvent)
	 */
	@Override
	protected int transform(ITextEditor editor, IDocument document, ITextSelection currentSelection,
			ExecutionEvent event) throws BadLocationException {

		if (isUniversalPresent()) {
			// show only
			mbState = variableState(EvalType.eval);
			mbState.run(editor);
		} else {
			mbState = evalState(INIT+PROMPT);
			return mbState.run(editor);
		}
		return NO_OFFSET;
	}

	private IMinibufferState evalState(final String prompt) {

		return new IMinibufferState() {

			public String getMinibufferPrefix() {
				return prompt;
			}

			public int run(ITextEditor editor) {
				miniTransform(
						new StrictMinibuffer(EvalHandler.this, Arrays.asList(EvalType.eval.name(), EvalType.setq.name()), true) {},
						editor, null);
				return NO_OFFSET;
			}

			public boolean executeResult(ITextEditor editor, Object minibufferResult) {
				EvalType type = null;
				if (minibufferResult != null) {
					try {
						type = EvalType.valueOf((String) minibufferResult);
					} catch (Exception e) {
					} // ignore
				}
				if (type != null) {
					mbState = variableState(type);
					mbState.run(editor);
				} else {
					setResultMessage(ABORT, true, editor);
				}
				return true;
			}
		};
	}

	/**
	 * Get state object to handle variable selection
	 * 
	 * @param prompt the minibuffer prompt
	 * @return the variable minibuffer state object
	 */
	IMinibufferState variableState(final EvalType type) {

		return new IMinibufferState() {

			public String getMinibufferPrefix() {
				return getTypePrompt(type) + PROMPT;
			}

			public int run(ITextEditor editor) {
				miniTransform(new EvalMinibuffer(EvalHandler.this), editor, null);
				return NO_OFFSET;
			}

			public boolean executeResult(ITextEditor editor, Object minibufferResult) {
				boolean result = true;
				if (minibufferResult != null && minibufferResult instanceof PrefVars) {
					PrefVars var = (PrefVars) minibufferResult;
					transitionState(editor, var);
				}
				return result;
			}

			private void transitionState(ITextEditor editor, PrefVars var) {
				switch (type) {
					case eval:
						setResultMessage(var.getValue().toString(), false, editor);
						break;
					case setq:
						switch (var.getType()) {
							case BOOLEAN:
								mbState = trueFalseState(var);
								mbState.run(editor);
								break;
							case INTEGER:
								mbState = numberState(var);
								mbState.run(editor);
								break;
							case RECT:
							case STRING:
								mbState = stringState(var);
								mbState.run(editor);
								break;
							default:
								break;
						}
						break;
				}
			}
		};
	}

	/**
	 * Get state to handle true/false prompt
	 * 
	 * @param var the variable object we're setting
	 * @return true/false minibuffer state object
	 */
	private IMinibufferState trueFalseState(final PrefVars var) {

		return new IMinibufferState() {

			public String getMinibufferPrefix() {
				return getTypePrompt(EvalType.setq) + ' ' + var.name() + PROMPT;
			}

			public int run(ITextEditor editor) {
				miniTransform(new BooleanMinibuffer(EvalHandler.this), editor, null);
				return NO_OFFSET;
			}

			public boolean executeResult(ITextEditor editor, Object minibufferResult) {
				boolean result = true;
				if (minibufferResult != null && minibufferResult instanceof Boolean) {
					var.setValue(minibufferResult);
					setResultMessage(minibufferResult.toString(), false, editor);
				} else {
					setResultMessage(ABORT, true, editor);
				}
				return result;
			}
		};
	}
	
	private IMinibufferState numberState(final PrefVars var) {
		
		return new IMinibufferState() {

			public String getMinibufferPrefix() {
				return getTypePrompt(EvalType.setq) + ' ' + var.name() + PROMPT;
			}

			public int run(ITextEditor editor) {
				miniTransform(new ReadNumberMinibuffer(EvalHandler.this), editor, null);
				return NO_OFFSET;
			}

			public boolean executeResult(ITextEditor editor, Object minibufferResult) {
				boolean result = true;
				if (minibufferResult != null && minibufferResult instanceof Integer) {
					var.setValue(minibufferResult);
					setResultMessage(minibufferResult.toString(), false, editor);
				} else {
					setResultMessage(ABORT, true, editor);
				}
				return result;
			}
			
		};
	}
	
	/**
	 * Return a minibuffer state object for reading and setting a string value.  The string read may be 
	 * free form, or limited by a set of values defined by an enum on the preference declaration.
	 * 
	 * @param var
	 * @return
	 */
	private IMinibufferState stringState(final PrefVars var) {

		return new IMinibufferState() {

			public String getMinibufferPrefix() {
				return EvalType.setq.name() + ' ' + var.name() + PROMPT;
			}

			public int run(ITextEditor editor) {
				String[] pvalues = var.getPossibleValues();
				ExecutingMinibuffer mini = null;
				if (pvalues != null) {
					mini = new StrictMinibuffer(EvalHandler.this, Arrays.asList(pvalues), true) {};
				} else {
					mini = new TextMinibuffer(EvalHandler.this);
				}
				miniTransform(mini, editor, null);				
				return NO_OFFSET;
			}

			public boolean executeResult(ITextEditor editor, Object minibufferResult) {
				boolean result = true;
				if (minibufferResult != null) {
					var.setValue(minibufferResult.toString());
					setResultMessage(minibufferResult.toString(), false, editor);
				} else {
					setResultMessage(ABORT, true, editor);
				}
				return result;
			}
		};
	}

	/**
	 * Dispatch through state object
	 * 
	 * @see com.mulgasoft.emacsplus.commands.MinibufferHandler#getMinibufferPrefix()
	 */
	@Override
	public String getMinibufferPrefix() {
		return mbState.getMinibufferPrefix();
	}

	/**
	 * Dispatch through state object
	 * 
	 * @see com.mulgasoft.emacsplus.minibuffer.IMinibufferExecutable#executeResult(org.eclipse.ui.texteditor.ITextEditor,
	 *      java.lang.Object)
	 */
	public boolean executeResult(ITextEditor editor, Object minibufferResult) {
		return mbState.executeResult(editor, minibufferResult);
	}

	/**
	 * Set and show the result of the last minibuffer in sequence
	 * 
	 * @see com.mulgasoft.emacsplus.minibuffer.IMinibufferExecutable#setResultMessage(java.lang.String,
	 *      boolean)
	 */
	public void setResultMessage(String resultMessage, boolean resultError, ITextEditor editor) {
		super.setResultMessage(resultMessage, resultError);
		showResultMessage(editor);
	}

}
