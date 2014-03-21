/**
 * Copyright (c) 2009, 2010 Mark Feber, MulgaSoft
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package com.mulgasoft.emacsplus.minibuffer;


import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.texteditor.ITextEditor;

import com.mulgasoft.emacsplus.EmacsPlusActivator;
import com.mulgasoft.emacsplus.EmacsPlusUtils;
import com.mulgasoft.emacsplus.RingBuffer;

/**
 * Minibuffer for simple and complex alignment prompts
 * 
 * Align the current region using an ad-hoc rule read from the minibuffer.
 * The selection marks the limits of the region.  This function will prompt
 * for the regexp to align with.  If no prefix arg was specified, you
 * only need to supply the characters to be lined up and any preceding
 * whitespace is replaced.  If a prefix arg was specified, the full
 * regexp with parenthesized whitespace should be supplied; it will also
 * prompt for which parenthesis group within regexp to modify, the amount
 * of spacing to use, and whether or not to repeat the rule throughout
 * the line.
 * 
 * @author Mark Feber - initial API and implementation
 */
public class AlignMinibuffer extends TextMinibuffer {
	
	private static final String ALIGN_COMPLEX= EmacsPlusActivator.getResourceString("Align_Complex");   								 //$NON-NLS-1$ 
	private static final String ALIGN_GROUP = EmacsPlusActivator.getResourceString("Align_Group");  									 //$NON-NLS-1$ 
	private static final String ALIGN_SPACES = EmacsPlusActivator.getResourceString("Align_Spaces");									 //$NON-NLS-1$ 
	private static final String ALIGN_REPEAT = String.format(EmacsPlusActivator.getResourceString("Align_Repeat"),YESORNO_Y, YESORNO_N); //$NON-NLS-1$ 

	private final static String INITIAL_EXP = "(\\s*)"; 																				 //$NON-NLS-1$
	
	private final static int GROUP_DEFAULT = 1;
	public final static int SPACE_DEFAULT = 1;
	private final static boolean REPEAT_DEFAULT = false;
	
	private ExecuteState executeState = null;
	private AlignControl alignControl = null;
	
	/**
	 * @param executable
	 */
	public AlignMinibuffer(IMinibufferExecutable executable, boolean isComplex) {
		super(executable);
		if (isComplex) {
			executeState = sComplex;
		} else {
			executeState = sSimple;
		}
	}

	@Override
	public boolean beginSession(ITextEditor editor, IWorkbenchPage page, ExecutionEvent event) {
		boolean result = super.beginSession(editor, page, event);
		// initialize with executeState's default content
		initMinibuffer(executeState.getMinibufferDefault(this));
		if (getHistoryRing().isEmpty()) {
			addToHistory(INITIAL_EXP); // seed command history
		}
		return result;
	}

	/**
	 * @see com.mulgasoft.emacsplus.minibuffer.ExecutingMinibuffer#getMinibufferPrefix()
	 */
	@Override
	protected String getMinibufferPrefix() {
		return executeState.getMinibufferPrefix(this);
	}

	/**
	 * @see com.mulgasoft.emacsplus.minibuffer.WithMinibuffer#handlesCtrl()
	 */
	@Override
	protected boolean handlesAlt() {
		// enable history for Apropos
		return true;
	}

	/**
	 * @see com.mulgasoft.emacsplus.minibuffer.ExecutingMinibuffer#executeResult(org.eclipse.ui.texteditor.ITextEditor, java.lang.Object)
	 */
	@Override
	protected boolean executeResult(ITextEditor editor, Object commandResult) {
		boolean result = true;
		result = executeState.executeResult(editor, commandResult, this);
		if (result) {
			if (getAlignControl() != null) {
				result = super.executeResult(editor, getAlignControl());
			} else {
				// TODO error message?
			}
		} else {
			// transition to next state
			initMinibuffer(executeState.getMinibufferDefault(this));
		}
		return result;
	}

	/**
	 * Dispatch through current execute state if it has a special handler
	 * 
	 * @see com.mulgasoft.emacsplus.minibuffer.WithMinibuffer#charEvent(org.eclipse.swt.events.VerifyEvent)
	 */
	protected void charEvent(VerifyEvent event) {
		if (!executeState.executeCharEvent(this, event)) {
			super.charEvent(event);
		}
	}
	
	AlignControl getAlignControl() {
		return alignControl;
	}

	void setAlignControl(AlignControl alignControl) {
		this.alignControl = alignControl;
	}
	void setExecuteState(ExecuteState executeState) {
		this.executeState = executeState;
	}

	private interface ExecuteState {
		String getMinibufferPrefix(ExecutingMinibuffer mini);
		String getMinibufferDefault(ExecutingMinibuffer mini);
		
		/**
		 * Execution invoked on current state. If evaluation returns
		 *  -true transition to next state (or invoke command's executeResult)
		 *  -false remain in current state  
		 * @param editor
		 * @param minibufferResult
		 * @param mini
		 * @return true to transition, false to remain
		 */
		boolean executeResult(ITextEditor editor, Object minibufferResult, ExecutingMinibuffer mini);
		boolean executeCharEvent(ExecutingMinibuffer mini, VerifyEvent event);
		public <T> RingBuffer<T> getRingHistory(); 
	}
	
	/**
	 * Only state of simple align-regexp
	 * 
	 * Get the ad-hoc part of the regexp rule and
	 * enter the final execution phase
	 */
	private static final ExecuteState sSimple = new ExecuteState() {
		public boolean executeResult(ITextEditor editor, Object minibufferResult, ExecutingMinibuffer mini) {
			boolean result = true;
			AlignMinibuffer am = (AlignMinibuffer)mini;
			AlignControl ac = null;
			String regexp = (String)minibufferResult;
			if (regexp != null && regexp.length() > 0) {
				am.addToHistory(regexp); // add to command history
				ac = am.new AlignControl(INITIAL_EXP + regexp);
				am.setAlignControl(ac);
			}
			return result;
		}
		
		public boolean executeCharEvent(ExecutingMinibuffer mini, VerifyEvent event) {return false;}
		
		public String getMinibufferPrefix(ExecutingMinibuffer mini) {
			return mini.getExecutable().getMinibufferPrefix();
		}
		
		public String getMinibufferDefault(ExecutingMinibuffer mini){
			return EMPTY_STR;
		}
		
		@SuppressWarnings("unchecked")
		public RingBuffer<String> getRingHistory() {
			return AlignRing.ring;
		}
	};
	
	
	/**
	 * First state of complex align-regexp
	 * 
	 * Get the complex regexp from the user (primarily for specifying complex groups) and
	 * transition to the next state 
	 */
	private static final ExecuteState sComplex = new ExecuteState() {
		public boolean executeResult(ITextEditor editor, Object minibufferResult, ExecutingMinibuffer mini) {
			boolean result = false;
			AlignMinibuffer am = (AlignMinibuffer)mini;
			AlignControl ac = null;
			String regexp = (String)minibufferResult;
			if (regexp != null && regexp.length() > 0) {
				am.addToHistory(regexp); // add to command history
				ac = am.new AlignControl(regexp);
				am.setAlignControl(ac);
				am.setExecuteState(sGroup);	// next state
			} else {
				result = true;	// flag for exit
			}
			return result;
		}
		public boolean executeCharEvent(ExecutingMinibuffer mini, VerifyEvent event) {return false;}

		public String getMinibufferPrefix(ExecutingMinibuffer mini) {
			return ALIGN_COMPLEX;
		}
		
		public String getMinibufferDefault(ExecutingMinibuffer mini){
			return INITIAL_EXP;
		}
		
		@SuppressWarnings("unchecked")		
		public RingBuffer<String> getRingHistory() {
			return AlignRing.ring;
		}
	};
	
	/**
	 * Second state of complex align-regexp
	 * 
	 * Get the number of the group of the complex regexp to be used in the alignment and
	 * transition to the next state 
	 */
	private static final ExecuteState sGroup = new ExecuteState() {
		public boolean executeResult(ITextEditor editor, Object minibufferResult, ExecutingMinibuffer mini) {
			boolean result = false;
			AlignMinibuffer am = (AlignMinibuffer)mini;
			String number = (String)minibufferResult;
			if (number != null && number.length() > 0) {
				try {
					AlignControl ac = am.getAlignControl();
					ac.group = EmacsPlusUtils.emacsParseInt(number);
//					am.addToHistory(Integer.toString(ac.group)); // add to command history
					am.addToHistory(ac.group); // add to command history
					am.setExecuteState(sSpaces);	// next state
				} catch (NumberFormatException e) {
					am.setResultMessage(String.format(BAD_NUMBER,number), true, true);	
					am.setAlignControl(null); // clear and 
					result = true;  		  // flag for exit
				}
			}
			return result;
		}
		public boolean executeCharEvent(ExecutingMinibuffer mini, VerifyEvent event) {
			((AlignMinibuffer)mini).numCharEvent(event);
			return true;
		}
		public String getMinibufferPrefix(ExecutingMinibuffer mini) {
			return ALIGN_GROUP;
		}
		public String getMinibufferDefault(ExecutingMinibuffer mini){
			return Integer.toString(GROUP_DEFAULT);
		}
		
		@SuppressWarnings("unchecked")
		public RingBuffer<Integer> getRingHistory() {
			return NumberRing.ring;
		}
	};
	
	/**
	 * Third state of complex align-regexp
	 * 
	 * Get the number of spaces to be used in the alignment and
	 * transition to the next state 
	 */
	private static final ExecuteState sSpaces = new ExecuteState() {
		public boolean executeResult(ITextEditor editor, Object minibufferResult, ExecutingMinibuffer mini) {
			boolean result = false;
			AlignMinibuffer am = (AlignMinibuffer)mini;
			String number = (String)minibufferResult;
			if (number != null && number.length() > 0) {
				try {
					AlignControl ac = am.getAlignControl();
					ac.spacing = EmacsPlusUtils.emacsParseInt(number);
					am.addToHistory(ac.spacing); // add to command history
					am.setExecuteState(sRepeat);	// next state

				} catch (NumberFormatException e) {
					am.setResultMessage(String.format(BAD_NUMBER,number), true, true);				
					am.setAlignControl(null); // clear and 
					result = true;  		  // flag for exit
				}
			}
			return result;
		}
		public boolean executeCharEvent(ExecutingMinibuffer mini, VerifyEvent event) {
			((AlignMinibuffer)mini).numCharEvent(event);
			return true;
		}
		public String getMinibufferPrefix(ExecutingMinibuffer mini) {
			return ALIGN_SPACES;
		}
		public String getMinibufferDefault(ExecutingMinibuffer mini){
			return Integer.toString(SPACE_DEFAULT);
		}

		@SuppressWarnings("unchecked")
		public RingBuffer<Integer> getRingHistory() {
			return NumberRing.ring;
		}

	};
	
	/**
	 * Fourth state of complex align-regexp
	 * 
	 * Get the boolean repeat value (i.e. repeat the alignment throughout each line) and
	 * enter the final execution phase
	 */
	private static final ExecuteState sRepeat = new ExecuteState() {
		private boolean onError = false;
		public boolean executeResult(ITextEditor editor, Object minibufferResult, ExecutingMinibuffer mini) {
			boolean result = true;
			AlignMinibuffer am = (AlignMinibuffer)mini;
			String yesOrNo = (String)minibufferResult;
			if (yesOrNo != null && yesOrNo.length() > 0) {
				try {
					am.getAlignControl().repeat = am.isYesOrNo(yesOrNo);
					am.setResultMessage(EMPTY_STR, true, true);					// clear out error message
				} catch (YesOrNoException e) {
					try {
						onError = true;
						am.setResultMessage(YESORNO_BAD, true, true);
					} finally {
						onError = false;
					}				
					result = false;
				}
			}
			return result;	
		}
		
		public boolean executeCharEvent(ExecutingMinibuffer mini, VerifyEvent event) {
			((AlignMinibuffer)mini).immediateCharEvent(event);
			return true;
		}
		
		public String getMinibufferPrefix(ExecutingMinibuffer mini) {
			if (onError) {
				return EMPTY_STR;
			} else {
				return ALIGN_REPEAT;				
			}
		}
		
		public String getMinibufferDefault(ExecutingMinibuffer mini){
			return EMPTY_STR;
		}
		
		public RingBuffer<? super Object> getRingHistory() {
			return null;
		}
	};

	/**
	 * Utility class to store the result of (possible multiple)
	 * prompt results
	 * 
	 * @author Mark Feber - initial API and implementation
	 */
	public class AlignControl {
		
		public AlignControl(String pattern) {
			this.pattern = pattern;
		}
		private String pattern;
		protected int group = GROUP_DEFAULT;
		protected int spacing = SPACE_DEFAULT;
		protected boolean repeat = REPEAT_DEFAULT;
		
		private int maxColumn = -1;
		
		public int getMaxColumn() {
			return maxColumn;
		}
		public void setMaxColumn(int maxColumn) {
			this.maxColumn = maxColumn;
		}
		
		public String getPattern() {
			return pattern;
		}
		public int getGroup() {
			return group;
		}
		public int getSpacing() {
			return spacing;
		}
		public boolean getRepeat() {
			return repeat;
		}
	}
	
//	/**
//	 * @see FocusListener#focusLost(org.eclipse.swt.events.FocusEvent)
//	 */
//	public void focusLost(FocusEvent e) {
//		// see IPartListener comment
//	}
	
	/**** Local RingBuffer ****/

	/**
	 * Dispatch through state object 
	 * 
	 * @see com.mulgasoft.emacsplus.minibuffer.HistoryMinibuffer#getHistoryRing()
	 */
	@Override
	protected <T> RingBuffer<T> getHistoryRing() {
		return executeState.getRingHistory();
	}
	
	/**** State RingBuffers: use lazy initialization holder class idiom ****/
	
	private static class AlignRing {
		static final RingBuffer<String> ring = new RingBuffer<String>();		
	}
	
	private static class NumberRing {
		static final RingBuffer<Integer> ring = new RingBuffer<Integer>();
	}
}
