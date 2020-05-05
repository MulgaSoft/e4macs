/**
 * Copyright (c) 2009-2020 Mark Feber, MulgaSoft
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package com.mulgasoft.emacsplus.preferences;

import java.util.Arrays;
import java.util.SortedMap;

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
 * Show/Set preference variables to a value dynamically
 * 
 * @author Mark Feber - initial API and implementation
 */
public class EvalHandler extends PreferenceHandler implements IMinibufferExecutable, INonEditingCommand {

	private final static String PROMPT = ": ";  											//$NON-NLS-1$
	private final static String ABORT = EmacsPlusActivator.getResourceString("Exec_Abort"); //$NON-NLS-1$
	private final static String SV = EmacsPlusActivator.getResourceString("Set_Variable");  //$NON-NLS-1$

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
		return e.toString();
	}

	/**
	 * @see com.mulgasoft.emacsplus.commands.EmacsPlusCmdHandler#transform(org.eclipse.ui.texteditor.ITextEditor,
	 *      org.eclipse.jface.text.IDocument, org.eclipse.jface.text.ITextSelection,
	 *      org.eclipse.core.commands.ExecutionEvent)
	 */
	@Override
	protected int transform(ITextEditor editor, IDocument document, ITextSelection currentSelection,
			ExecutionEvent event) throws BadLocationException {

		mbState = evalOrSetState(getTypePrompt(EvalType.eval)+PROMPT, !isUniversalPresent());
		return mbState.run(editor);
	}

	/**
	 * Select the preference variable to show, or transition to set variable
	 *  
	 * @param prompt for the ui
	 * @param setState true if allowing set variable (setq)
	 */
	private IMinibufferState evalOrSetState(final String prompt, boolean setState) {

		return new IMinibufferState() {

			public String getMinibufferPrefix() {
				return prompt;
			}

			public int run(ITextEditor editor) {
				miniTransform(new EvalMinibuffer(EvalHandler.this) {
					protected SortedMap<String, PrefVars> getCompletions() {
						return PrefVars.getCompletions(setState);
					}
				}, editor, null);
				return NO_OFFSET;
			}

			public boolean executeResult(ITextEditor editor, Object minibufferResult) {
				PrefVars var = null;
				if (minibufferResult != null && minibufferResult instanceof PrefVars) {
					var = (PrefVars)minibufferResult;
					if (var != null) {
						if (var == PrefVars.SETQ) {
							mbState = variableSetState();
							mbState.run(editor);
						} else {
							setResultMessage(var.getDisplayName() + ' ' + var.getDisplayValue().toString(), false, editor);							
						}
					} 
				} else {
					setResultMessage(ABORT, true, editor);
				}				
				return true;
			}
		};
	}

	/**
	 * Get state object to handle the set variable selection 
	 * 
	 * @param prompt the minibuffer prompt
	 * @return the variable minibuffer state object
	 */
	IMinibufferState variableSetState() {

		return new IMinibufferState() {

			public String getMinibufferPrefix() {
				return getTypePrompt(EvalType.setq) + PROMPT;
			}

			public int run(ITextEditor editor) {
				miniTransform(new EvalMinibuffer(EvalHandler.this), editor, null);
				return NO_OFFSET;
			}

			public boolean executeResult(ITextEditor editor, Object minibufferResult) {
				boolean result = true;
				if (minibufferResult != null && minibufferResult instanceof PrefVars) {
					transitionState(editor, (PrefVars)minibufferResult);
				}
				return result;
			}

			private void transitionState(ITextEditor editor, PrefVars var) {
				switch (var.getType()) {
					case BOOLEAN:
						mbState = trueFalseState(var);
						mbState.run(editor);
						break;
					case INTEGER:
					case P_INTEGER:
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
			}
		};
	}

	/**
	 * Get state to handle true/false input
	 * 
	 * @param var the variable object we're setting
	 * @return true/false minibuffer state object
	 */
	private IMinibufferState trueFalseState(final PrefVars var) {

		return new IMinibufferState() {

			public String getMinibufferPrefix() {
				return getTypePrompt(EvalType.setq) + ' ' + var.getDisplayName() + PROMPT;
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

	/**
	 * Get state to handle numeric input
	 * 
	 * @param var the variable object we're setting
	 * @return true/false minibuffer state object
	 */
	private IMinibufferState numberState(final PrefVars var) {

		return new IMinibufferState() {

			public String getMinibufferPrefix() {
				return getTypePrompt(EvalType.setq) + ' ' + var.getDisplayName() + PROMPT;
			}

			public int run(ITextEditor editor) {
				miniTransform(new ReadNumberMinibuffer(EvalHandler.this), editor, null);
				return NO_OFFSET;
			}

			public boolean executeResult(ITextEditor editor, Object minibufferResult) {
				boolean result = true;
				if (minibufferResult != null && minibufferResult instanceof Integer) {
					var.setValue(minibufferResult);
					// A P_INTEGER disallows negative values, so retrieve it for display
					setResultMessage(var.getValue().toString(), false, editor);
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
				return EvalType.setq.name() + ' ' + var.getDisplayName() + PROMPT;
			}

			public int run(ITextEditor editor) {
				String[] pvalues = var.getPossibleValues();
				ExecutingMinibuffer mini = null;
				if (pvalues != null) {
					mini = new StrictMinibuffer(EvalHandler.this, Arrays.asList(pvalues)) {};
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
					setResultMessage(var.getDisplayValue().toString(), false, editor);
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
