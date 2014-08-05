/**
 * Copyright (c) 2009, 2010 Mark Feber, MulgaSoft
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 */
package com.mulgasoft.emacsplus;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.ui.texteditor.ITextEditorActionDefinitionIds;
/**
 * For convenience, extend the Eclipse interface hierarchy defining command ids
 * 
 * @author Mark Feber - initial API and implementation
 */
@SuppressWarnings({"serial", "deprecation"})
public interface IEmacsPlusCommandDefinitionIds extends ITextEditorActionDefinitionIds {
//and elsewhere, org.eclipse.jdt.ui.actions.IJavaEditorActionDefinitionIds;
	
	// These deprecated ids are used for backward compatibility
	final String EMP_COPY= COPY;			 //$NON-NLS-1$
	final String EMP_CUT= CUT;  			 //$NON-NLS-1$
	final String EMP_DELETE= DELETE;		 //$NON-NLS-1$
	final String EMP_PASTE= PASTE;  		 //$NON-NLS-1$
	final String EMP_REDO= REDO;			 //$NON-NLS-1$
	final String EMP_SELECT_ALL= SELECT_ALL; //$NON-NLS-1$
	final String EMP_UNDO= UNDO;			 //$NON-NLS-1$
	
	// these commands print results to status area and so Meta-X should not update status after execution
	final Map<String,String> statusCommands = new HashMap<String,String>() {
		{
			put(IEmacsPlusCommandDefinitionIds.COUNT_BUFFER,IEmacsPlusCommandDefinitionIds.COUNT_BUFFER);
			put(IEmacsPlusCommandDefinitionIds.COUNT_MATCHES,IEmacsPlusCommandDefinitionIds.COUNT_MATCHES);
			put(IEmacsPlusCommandDefinitionIds.COUNT_REGION,IEmacsPlusCommandDefinitionIds.COUNT_REGION);
			put(IEmacsPlusCommandDefinitionIds.WHAT_CURSOR,IEmacsPlusCommandDefinitionIds.WHAT_CURSOR);
			put(IEmacsPlusCommandDefinitionIds.WHAT_LINE,IEmacsPlusCommandDefinitionIds.WHAT_LINE);
		}
	};
	
	// Simple text manipulation 
	/**
	 * Command definition id of character transposition
	 * Value: <code>"com.mulgasoft.emacsplus.transposechar"</code>
	 */
	final String TRANSPOSE_CHAR= "com.mulgasoft.emacsplus.transposechar";   				   //$NON-NLS-1$
	/**
	 * Command definition id of line transposition
	 * Value: <code>"com.mulgasoft.emacsplus.transposeline"</code>
	 */
	final String TRANSPOSE_LINE= "com.mulgasoft.emacsplus.transposeline";   				   //$NON-NLS-1$
	
	/**
	 * Command definition id of line downcase (lowercase)
	 * Value: <code>"com.mulgasoft.emacsplus.lowercase"</code>
	 */
	final String DOWNCASE_WR= "com.mulgasoft.emacsplus.lowercase";  						   //$NON-NLS-1$

	/**
	 * Command definition id of line upcase (uppercase)
	 * Value: <code>"com.mulgasoft.emacsplus.uppercase"</code>
	 */
	final String UPCASE_WR= "com.mulgasoft.emacsplus.uppercase";							   //$NON-NLS-1$

	/**
	 * Command definition id of line capitalize
	 * Value: <code>"com.mulgasoft.emacsplus.capitalize"</code>
	 */
	final String CAPITALIZE_WR= "com.mulgasoft.emacsplus.capitalize";   					   //$NON-NLS-1$
	

	/**
	 * Command definition id of delete horizontal space
	 * Value: <code>"com.mulgasoft.emacsplus.deletehorizspace"</code>
	 */
	final String DELETE_HORIZONTAL_SPACE= "com.mulgasoft.emacsplus.deletehorizspace";   	   //$NON-NLS-1$
	
		/**
	 * Command definition id of delete horizontal space
	 * Value: <code>"com.mulgasoft.emacsplus.deleteBlankLines"</code>
	 */
	final String DELETE_BLANK_LINES= "com.mulgasoft.emacsplus.deleteBlankLines";			   //$NON-NLS-1$
	
	/**
	 * Command definition id of just one space
	 * Value: <code>"com.mulgasoft.emacsplus.justonespace"</code>
	 */
	final String JUST_ONE_SPACE= "com.mulgasoft.emacsplus.justOneSpace";					   //$NON-NLS-1$
	
	/**
	 * Command definition id of split-line
	 * Value: <code>"com.mulgasoft.emacsplus.splitLine"</code>
	 */
	final String SPLIT_LINE= "com.mulgasoft.emacsplus.splitLine";   						   //$NON-NLS-1$
		
	/**
	 * Command definition id of delete-indentation
	 * Value: <code>"com.mulgasoft.emacsplus.mergeLine"</code>
	 */
	final String MERGE_LINE = "com.mulgasoft.emacsplus.mergeLine";  						   //$NON-NLS-1$

	/**
	 * Command re-definition id of recenter
	 * Value: <code>"com.mulgasoft.emacsplus.recenterTopBottom"</code>
	 */
	final String RECENTER_TOP_BOTTOM = "com.mulgasoft.emacsplus.recenterTopBottom"; 		   //$NON-NLS-1$
	
	/**
	 * Command definition id of new-line-indent
	 * Value: <code>"com.mulgasoft.emacsplus.newLineIndent"</code>
	 */
	final String NEW_LINE_INDENT="com.mulgasoft.emacsplus.newLineIndent";   				   //$NON-NLS-1$
	
	/**
	 * Command definition id of back-to-indent
	 * Value: <code>"com.mulgasoft.emacsplus.backToIndent"</code>
	 */
	final String BACK_TO_INDENT = "com.mulgasoft.emacsplus.backToIndent";   				   //$NON-NLS-1$
	
	/**
	 * Command definition id of indent-for-tab
	 * Value: <code>"com.mulgasoft.emacsplus.indentForTab"</code>
	 */
	final String INDENT_FOR_TAB = "com.mulgasoft.emacsplus.indentForTab";   				   //$NON-NLS-1$
	
	// Block navigation
	/**
	 * Command definition id of forward block
	 * Value: <code>"com.mulgasoft.emacsplus.forwardBlockOfLines"</code>
	 */
	final String FORWARD_BLOCK= "com.mulgasoft.emacsplus.forwardBlockOfLines";  			   //$NON-NLS-1$
	
	/**
	 * Command definition id of backward block
	 * Value: <code>"com.mulgasoft.emacsplus.backwardBlockOfLines"</code>
	 */
	final String BACKWARD_BLOCK= "com.mulgasoft.emacsplus.backwardBlockOfLines";			   //$NON-NLS-1$
	
	// Paragraphs
	/**
	 * Command definition id of forward paragraph
	 * Value: <code>"com.mulgasoft.emacsplus.forwardParagraph"</code>
	 */
	final String FORWARD_PARAGRAPH= "com.mulgasoft.emacsplus.forwardParagraph"; 			   //$NON-NLS-1$
	
	/**
	 * Command definition id of forward mark paragraph
	 * Value: <code>"com.mulgasoft.emacsplus.forwardMarkParagraph"</code>
	 */
	final String FORWARD_MARK_PARAGRAPH= "com.mulgasoft.emacsplus.forwardMarkParagraph";	   //$NON-NLS-1$
	
	/**
	 * Command definition id of select forward paragraph
	 * Value: <code>"com.mulgasoft.emacsplus.forwardSelectParagraph"</code>
	 */
	final String SELECT_FORWARD_PARAGRAPH= "com.mulgasoft.emacsplus.forwardSelectParagraph";   //$NON-NLS-1$

	/**
	 * Command definition id of backward paragraph
	 * Value: <code>"com.mulgasoft.emacsplus.backwardParagraph"</code>
	 */
	final String BACKWARD_PARAGRAPH= "com.mulgasoft.emacsplus.backwardParagraph";   		   //$NON-NLS-1$
	
	/**
	 * Command definition id of mark backward paragraph
	 * Value: <code>"com.mulgasoft.emacsplus.backwardMarkParagraph"</code>
	 */
	final String BACKWARD_MARK_PARAGRAPH= "com.mulgasoft.emacsplus.backwardMarkParagraph";     //$NON-NLS-1$
	
	/**
	 * Command definition id of select backward paragraph
	 * Value: <code>"com.mulgasoft.emacsplus.backwardSelectParagraph"</code>
	 */
	final String SELECT_BACKWARD_PARAGRAPH= "com.mulgasoft.emacsplus.backwardSelectParagraph"; //$NON-NLS-1$

	/**
	 * Command definition id of transpose paragraphs
	 * Value: <code>"com.mulgasoft.emacsplus.transposeParagraph"</code>
	 */
	final String TRANSPOSE_PARAGRAPH= "com.mulgasoft.emacsplus.transposeParagraph"; 		   //$NON-NLS-1$

	// Kill-Ring

	/**
	 * Command definition id of append next kill
	 * Value: <code>"com.mulgasoft.emacsplus.appendNextKill"</code>
	 */
	final String APPEND_NEXT_KILL = "com.mulgasoft.emacsplus.appendNextKill";   			   //$NON-NLS-1$
	
	/**
	 * Command definition id of browse kill ring
	 * Value: <code>"com.mulgasoft.emacsplus.browseKillRing"</code>
	 */
	final String BROWSE_KILL_RING = "com.mulgasoft.emacsplus.browseKillRing";   			   //$NON-NLS-1$
	
	/**
	 * Command definition id of yank
	 * Value: <code>"com.mulgasoft.emacsplus.yank"</code>
	 */
	final String YANK= "com.mulgasoft.emacsplus.yank";  									   //$NON-NLS-1$
	
	/**
	 * Command definition id of yankpop
	 * Value: <code>"com.mulgasoft.emacsplus.yankpop"</code>
	 */
	final String YANK_POP= "com.mulgasoft.emacsplus.yankpop";   							   //$NON-NLS-1$
	
	/**
	 * Command definition id of rotate-yank-pointer
	 * Value: <code>"com.mulgasoft.emacsplus.yankrotate"</code>
	 */
	final String YANK_ROTATE= "com.mulgasoft.emacsplus.yankrotate"; 						   //$NON-NLS-1$

	// S-expression commands 

	/**
	 * Command definition id of word transposition
	 * Value: <code>"com.mulgasoft.emacsplus.transposeword"</code>
	 */
	final String TRANSPOSE_WORD= "com.mulgasoft.emacsplus.transposeword";   				   //$NON-NLS-1$
	
	/**
	 * Command definition id of sexp transposition
	 * Value: <code>"com.mulgasoft.emacsplus.transposesexp"</code>
	 */
	final String TRANSPOSE_SEXP= "com.mulgasoft.emacsplus.transposesexp";   				   //$NON-NLS-1$

	/**
	 * Command definition id of forward balanced expression
	 * Value: <code>"com.mulgasoft.emacsplus.forwardsexp"</code>
	 */
	final String FORWARD_SEXP= "com.mulgasoft.emacsplus.forwardsexp";   					   //$NON-NLS-1$
	final String SELECT_FORWARD_SEXP= "com.mulgasoft.emacsplus.forwardSelectSexp";  		   //$NON-NLS-1$
	final String MARK_FORWARD_SEXP= "com.mulgasoft.emacsplus.forwardMarkSexp";  			   //$NON-NLS-1$

	/**
	 * Command definition id of backward balanced expression
	 * Value: <code>"com.mulgasoft.emacsplus.backwardsexp"</code>
	 */
	final String BACKWARD_SEXP= "com.mulgasoft.emacsplus.backwardsexp"; 					   //$NON-NLS-1$
	final String SELECT_BACKWARD_SEXP= "com.mulgasoft.emacsplus.backwardSelectSexp";		   //$NON-NLS-1$
	final String MARK_BACKWARD_SEXP= "com.mulgasoft.emacsplus.backwardMarkSexp";			   //$NON-NLS-1$

	/**
	 * Command definition id of kill forward balanced expression
	 * Value: <code>"com.mulgasoft.emacsplus.killforwardsexp"</code>
	 */
	final String KILL_FORWARD_SEXP= "com.mulgasoft.emacsplus.killforwardsexp";  			   //$NON-NLS-1$

	/**
	 * Command definition id of kill backward balanced expression
	 * Value: <code>"com.mulgasoft.emacsplus.killbackwardsexp"</code>
	 */
	final String KILL_BACKWARD_SEXP= "com.mulgasoft.emacsplus.killbackwardsexp";			   //$NON-NLS-1$
	
	/**
	 * Command definition id of mark sexp
	 * Value: <code>"com.mulgasoft.emacsplus.markSexp"</code>
	 */
	final String MARK_SEXP= "com.mulgasoft.emacsplus.markSexp"; 							   //$NON-NLS-1$
	
	/**
	 * Command definition id of mark word
	 * Value: <code>"com.mulgasoft.emacsplus.markWord"</code>
	 */
	final String MARK_WORD= "com.mulgasoft.emacsplus.markWord"; 							   //$NON-NLS-1$
	
	// Window Commands

	/**
	 * Command definition id of switch-to-buffer
	 * Value: <code>"com.mulgasoft.emacsplus.switchToBuffer"</code>
	 */
	final String SWITCH_TO_BUFFER = "com.mulgasoft.emacsplus.switchToBuffer";                          //$NON-NLS-1
	
	/**
	 * Command definition id of split window horizontally
	 * Value: <code>"com.mulgasoft.emacsplus.splitHorizontalWindow"</code>
	 */
	final String SPLIT_WINDOW_HORZONTALLY = "com.mulgasoft.emacsplus.splitHorizontalWindow";   //$NON-NLS-1$

	/**
	 * Command definition id of split window vertically
	 * Value: <code>"com.mulgasoft.emacsplus.splitVerticalWindow"</code>
	 */
	final String SPLIT_WINDOW_VERTICALLY = "com.mulgasoft.emacsplus.splitVerticalWindow";      //$NON-NLS-1$

	/**
	 * Command definition id of close other instances
	 * Value: <code>"com.mulgasoft.emacsplus.closeOtherInstances"</code>
	 */
	final String CLOSE_OTHER_INSTANCES = "com.mulgasoft.emacsplus.closeOtherInstances"; 	   //$NON-NLS-1$

	/**
	 * Command definition id of join window
	 * Value: <code>"com.mulgasoft.emacsplus.joinWindow"</code>
	 */
	final String JOIN_WINDOW= "com.mulgasoft.emacsplus.joinWindow"; 						   //$NON-NLS-1$
	
	/**
	 * Command definition id of deactivate window
	 * Value: <code>"com.mulgasoft.emacsplus.deleteWindow"</code>
	 */
	final String DELETE_WINDOW= "com.mulgasoft.emacsplus.deleteWindow"; 						   //$NON-NLS-1$
	
	/**
	 * Command definition id of other window
	 * Value: <code>"com.mulgasoft.emacsplus.otherWindow"</code>
	 */
	final String OTHER_WINDOW= "com.mulgasoft.emacsplus.otherWindow";   					   //$NON-NLS-1$
	
	/**
	 * Command definition id of switch to buffer other window
	 * Value: <code>"com.mulgasoft.emacsplus.switchOtherWindow"</code>
	 */
	final String SWITCH_OTHER_WINDOW= "com.mulgasoft.emacsplus.switchOtherWindow";   					   //$NON-NLS-1$
	
	/**
	 * Command definition id of open declaration other window
	 * Value: <code>"com.mulgasoft.emacsplus.declOtherWindow"</code>
	 */
	final String DECL_OTHER_WINDOW= "com.mulgasoft.emacsplus.declotherWindow";   					   //$NON-NLS-1$
	
	
	/**
	 * Command definition id of display buffer other window
	 * Value: <code>"com.mulgasoft.emacsplus.showOtherWindow"</code>
	 */
	final String SHOW_OTHER_WINDOW = "com.mulgasoft.emacsplus.showOtherWindow";		   //$NON-NLS-1$
	
	/**
	 * Command definition id of shrink window
	 * Value: <code>"com.mulgasoft.emacsplus.shrinkWindow"</code>
	 */
	final String SHRINK_WINDOW= "com.mulgasoft.emacsplus.shrinkWindow"; 						   //$NON-NLS-1$
	
	/**
	 * Command definition id of enlarge window
	 * Value: <code>"com.mulgasoft.emacsplus.enlargeWindow"</code>
	 */
	final String ENLARGE_WINDOW= "com.mulgasoft.emacsplus.enlargeWindow"; 						   //$NON-NLS-1$
	
	/**
	 * Command definition id of balance window
	 * Value: <code>"com.mulgasoft.emacsplus.balanceWindow"</code>
	 */
	final String BALANCE_WINDOW= "com.mulgasoft.emacsplus.balanceWindow"; 						   //$NON-NLS-1$
	
	// Frame commands
	
	final String MAKE_FRAME= "com.mulgasoft.emacsplus.createFrame"; 						   //$NON-NLS-1$
	final String OTHER_FRAME= "com.mulgasoft.emacsplus.otherFrame"; 						   //$NON-NLS-1$
	final String JOIN_FRAME= "com.mulgasoft.emacsplus.joinFrame"; 						   //$NON-NLS-1$
	final String JOIN_FRAMES= "com.mulgasoft.emacsplus.joinFrames"; 						   //$NON-NLS-1$
	final String SWITCH_OTHER_FRAME= "com.mulgasoft.emacsplus.switchOtherFrame"; 						   //$NON-NLS-1$
	final String SHOW_OTHER_FRAME= "com.mulgasoft.emacsplus.showOtherFrame"; 						   //$NON-NLS-1$

	// Point and Mark 
	
	/**
	 * Command replacement so exchange works correctly
	 * Value: <code>"com.mulgasoft.emacsplus.exchangeMark"</code>
	 */
	final String EXCHANGE_POINT_AND_MARK = "com.mulgasoft.emacsplus.exchangeMark";  		   //$NON-NLS-1$

	/**
	 * Command replacement so set mark works correctly
	 * Value: <code>"com.mulgasoft.emacsplus.setMark"</code>
	 */
	final String SET_MARK = "com.mulgasoft.emacsplus.setMark";  							   //$NON-NLS-1$
	
	/**
	 * push mark onto mark ring
	 * Value: <code>"com.mulgasoft.emacsplus.pushMark"</code>
	 */
	final String PUSH_MARK = "com.mulgasoft.emacsplus.pushMark";							   //$NON-NLS-1$
	
	/**
	 * pop mark from mark ring
	 * Value: <code>"com.mulgasoft.emacsplus.popMark"</code>
	 */
	final String POP_MARK = "com.mulgasoft.emacsplus.popMark";  							   //$NON-NLS-1$
	
	/**
	 * pop global mark from mark ring
	 * Value: <code>"com.mulgasoft.emacsplus.popGlobalMark"</code>
	 */
	final String POP_GLOBAL_MARK = "com.mulgasoft.emacsplus.popGlobalMark"; 				   //$NON-NLS-1$
	
	/**
	 * rotate and pop global mark from mark ring
	 * Value: <code>"com.mulgasoft.emacsplus.rotateGlobalMark"</code>
	 */
	final String ROTATE_GLOBAL_MARK = "com.mulgasoft.emacsplus.rotateGlobalMark";   		   //$NON-NLS-1$
	
	/**
	 * Command replacement so mark all works correctly
	 * Value: <code>"com.mulgasoft.emacsplus.markAll"</code>
	 */
	final String MARK_WHOLE_BUFFER = "com.mulgasoft.emacsplus.markAll"; 					   //$NON-NLS-1$
	
	// Search and Replace
	
	final String ISEARCH_FORWARD = "com.mulgasoft.emacsplus.isearchForward";				   //$NON-NLS-1$
	final String ISEARCH_BACKWARD = "com.mulgasoft.emacsplus.isearchBackward";  			   //$NON-NLS-1$
	final String ISEARCH_REGEXP_FORWARD = "com.mulgasoft.emacsplus.isearchRegexpForward";      //$NON-NLS-1$
	final String ISEARCH_REGEXP_BACKWARD = "com.mulgasoft.emacsplus.isearchRegexpBackward";    //$NON-NLS-1$
	final String REPLACE_STRING = "com.mulgasoft.emacsplus.stringReplace";  				   //$NON-NLS-1$
	final String REPLACE_REGEXP = "com.mulgasoft.emacsplus.regexpReplace";  				   //$NON-NLS-1$
	final String QUERY_REPLACE = "com.mulgasoft.emacsplus.queryReplace";					   //$NON-NLS-1$
	final String QUERY_REPLACE_REGEXP = "com.mulgasoft.emacsplus.queryRegexpReplace";   	   //$NON-NLS-1$

	// Extending selections
	final String FORWARD_CHAR="com.mulgasoft.emacsplus.forwardChar";						   //$NON-NLS-1$
	final String BACKWARD_CHAR="com.mulgasoft.emacsplus.backwardChar";  					   //$NON-NLS-1$
	final String FORWARD_WORD="com.mulgasoft.emacsplus.forwardWord";						   //$NON-NLS-1$
	final String BACKWARD_WORD="com.mulgasoft.emacsplus.backwardWord";  					   //$NON-NLS-1$
	final String NEXT_LINE="com.mulgasoft.emacsplus.nextLine";  							   //$NON-NLS-1$
	final String PREVIOUS_LINE="com.mulgasoft.emacsplus.previousLine";  					   //$NON-NLS-1$
	final String BEGIN_LINE="com.mulgasoft.emacsplus.beginLine";							   //$NON-NLS-1$
	final String END_LINE="com.mulgasoft.emacsplus.endLine";								   //$NON-NLS-1$
	final String BEGIN_BUFFER="com.mulgasoft.emacsplus.beginBuffer";						   //$NON-NLS-1$
	final String END_BUFFER="com.mulgasoft.emacsplus.endBuffer";							   //$NON-NLS-1$                         
	final String SCROLL_UP="com.mulgasoft.emacsplus.scrollUp";  							   //$NON-NLS-1$
	final String SCROLL_DOWN="com.mulgasoft.emacsplus.scrollDown";  						   //$NON-NLS-1$                         
	final String SHIFT_SELECT="com.mulgasoft.emacsplus.shiftSelect";						   //$NON-NLS-1$                         

	// Help Commands
	final String APROPOS="com.mulgasoft.emacsplus.commandApropos";  						   //$NON-NLS-1$
	final String BINDINGS="com.mulgasoft.emacsplus.commandBindings";						   //$NON-NLS-1$
	final String WHEREIS="com.mulgasoft.emacsplus.commandWhereIs";  						   //$NON-NLS-1$
	final String DESCRIBE="com.mulgasoft.emacsplus.commandDescribe";						   //$NON-NLS-1$
	final String DESCRIBE_KEY="com.mulgasoft.emacsplus.commandDescribeKey"; 				   //$NON-NLS-1$
	final String DESCRIBE_KEY_BREIF="com.mulgasoft.emacsplus.commandDescribeBriefKey";  	   //$NON-NLS-1$
	
	// Tags 
	final String TAGS_PROJECT="com.mulgasoft.emacsplus.tagsSearchProject";  				   //$NON-NLS-1$ 
	final String TAGS_WORKINGSET="com.mulgasoft.emacsplus.tagsSearchSet";   				   //$NON-NLS-1$  
	final String TAGS_WORKSPACE="com.mulgasoft.emacsplus.tagsSearchSpace";  				   //$NON-NLS-1$  
	
	// Named Registers
	final String COPY_TO_REGISTER="com.mulgasoft.emacsplus.copyToRegister"; 				   //$NON-NLS-1$
	final String RECTANGLE_TO_REGISTER="com.mulgasoft.emacsplus.rectangleToRegister";   	   //$NON-NLS-1$
	final String APPEND_TO_REGISTER="com.mulgasoft.emacsplus.appendToRegister"; 			   //$NON-NLS-1$
	final String PREPEND_TO_REGISTER="com.mulgasoft.emacsplus.prependToRegister";   		   //$NON-NLS-1$
	final String INSERT_REGISTER="com.mulgasoft.emacsplus.insertRegister";  				   //$NON-NLS-1$
	final String POINT_TO_REGISTER="com.mulgasoft.emacsplus.pointToRegister";   			   //$NON-NLS-1$
	final String JUMP_TO_REGISTER="com.mulgasoft.emacsplus.jumpToRegister"; 				   //$NON-NLS-1$
	final String VIEW_REGISTER="com.mulgasoft.emacsplus.viewRegister";  					   //$NON-NLS-1$
	final String LIST_REGISTERS="com.mulgasoft.emacsplus.listRegister"; 					   //$NON-NLS-1$
	
	// Rectangles 
	final String RECTANGLE_KILL="com.mulgasoft.emacsplus.killRectangle";					   //$NON-NLS-1$
	final String RECTANGLE_DELETE="com.mulgasoft.emacsplus.deleteRectangle";				   //$NON-NLS-1$
	final String RECTANGLE_YANK="com.mulgasoft.emacsplus.yankRectangle";					   //$NON-NLS-1$
	final String RECTANGLE_OPEN="com.mulgasoft.emacsplus.openRectangle";					   //$NON-NLS-1$
	final String RECTANGLE_CLEAR="com.mulgasoft.emacsplus.clearRectangle";  				   //$NON-NLS-1$
	final String RECTANGLE_REPLACE="com.mulgasoft.emacsplus.replaceRectangle";  			   //$NON-NLS-1$
	final String RECTANGLE_INSERT="com.mulgasoft.emacsplus.insertRectangle";				   //$NON-NLS-1$
	final String RECTANGLE_WHITESPACE="com.mulgasoft.emacsplus.whitespaceRectangle";		   //$NON-NLS-1$
	final String INDENT_RIGIDLY="com.mulgasoft.emacsplus.indentRigidly";					   //$NON-NLS-1$
	
	// List
	
	/**
	 * Command definition id of up list
	 * Value: <code>"com.mulgasoft.emacsplus.upList"</code>
	 */
	final String UP_LIST = "com.mulgasoft.emacsplus.upList";								   //$NON-NLS-1$
	
	/**
	 * Command definition id of backward up list
	 * Value: <code>"com.mulgasoft.emacsplus.backwardUpList"</code>
	 */
	final String BACKWARD_UP_LIST = "com.mulgasoft.emacsplus.backwardUpList";   			   //$NON-NLS-1$

	/**
	 * Command definition id of down list
	 * Value: <code>"com.mulgasoft.emacsplus.downList"</code>
	 */
	final String DOWN_LIST = "com.mulgasoft.emacsplus.downList";							   //$NON-NLS-1$

	// Keyboard Macros
	
	/**
	 * Command definition for starting a keyboard macro definition
	 * Value: <code>"com.mulgasoft.emacsplus.startKbdMacro"</code>
	 */
	final String KBDMACRO_START = "com.mulgasoft.emacsplus.startKbdMacro";  				   //$NON-NLS-1$
	
	/**
	 * 
	 * Command definition for ending a keyboard macro definition
	 * Value: <code>"com.mulgasoft.emacsplus.endKbdMacro"</code>
	 */
	final String KBDMACRO_END = "com.mulgasoft.emacsplus.endKbdMacro";  					   //$NON-NLS-1$
	
	/**
	 * 
	 * Command definition for ending (or calling) a keyboard macro definition
	 * Value: <code>"com.mulgasoft.emacsplus.endCallKbdMacro"</code>
	 */
	final String KBDMACRO_END_CALL = "com.mulgasoft.emacsplus.endCallKbdMacro"; 			   //$NON-NLS-1$
	
	/**
	 * 
	 * Command definition for executing a keyboard macro definition
	 * Value: <code>"com.mulgasoft.emacsplus.executeKbdMacro"</code>
	 */
	final String KBDMACRO_EXECUTE = "com.mulgasoft.emacsplus.executeKbdMacro";  			   //$NON-NLS-1$

	/**
	 * 
	 * Command definition for cycling to next keyboard macro definition
	 * Value: <code>"com.mulgasoft.emacsplus.nextKbdMacro"</code>
	 */
	final String KBDMACRO_NEXT = "com.mulgasoft.emacsplus.nextKbdMacro";					   //$NON-NLS-1$
	
	/**
	 * 
	 * Command definition for cycling to previous keyboard macro definition
	 * Value: <code>"com.mulgasoft.emacsplus.previousKbdMacro"</code>
	 */
	final String KBDMACRO_PREVIOUS = "com.mulgasoft.emacsplus.previousKbdMacro";			   //$NON-NLS-1$

	/**
	 * 
	 * Command definition for naming a keyboard macro definition
	 * Value: <code>"com.mulgasoft.emacsplus.nameKbdMacro"</code>
	 */
	final String KBDMACRO_NAME = "com.mulgasoft.emacsplus.nameKbdMacro";					   //$NON-NLS-1$
	
	/**
	 * 
	 * Command definition for binding a keyboard macro definition
	 * Value: <code>"com.mulgasoft.emacsplus.kbdMacroBind"</code>
	 */
	final String KBDMACRO_BIND = "com.mulgasoft.emacsplus.kbdMacroBind";					   //$NON-NLS-1$
	
	/**
	 * 
	 * Command definition for saving a keyboard macro definition
	 * Value: <code>"com.mulgasoft.emacsplus.saveKbdMacro"</code>
	 */
	final String KBDMACRO_SAVE = "com.mulgasoft.emacsplus.saveKbdMacro";					   //$NON-NLS-1$
	
	/**
	 * 
	 * Command definition for loading a keyboard macro definition
	 * Value: <code>"com.mulgasoft.emacsplus.loadKbdMacro"</code>
	 */
	final String KBDMACRO_LOAD = "com.mulgasoft.emacsplus.loadKbdMacro";					   //$NON-NLS-1$

	// Misc
	
	/**
	 * Command definition id delete trailing whitespace
	 * Value: <code>"com.mulgasoft.emacsplus.deleteTrailingWhitespace"</code>
	 */
	final String DELETE_WHITESPACE = "com.mulgasoft.emacsplus.deleteTrailingWhitespace";	   //$NON-NLS-1$
	
	/**
	 * Command definition id of allow edit
	 * Value: <code>"com.mulgasoft.emacsplus.toggleAllowEdit"</code>
	 */
	final String ALLOW_EDIT = "com.mulgasoft.emacsplus.toggleAllowEdit";					   //$NON-NLS-1$
	
	/**
	 * Command definition execute extended command
	 * Value: <code>"com.mulgasoft.emacsplus.metaX"</code>
	 */
	final String METAX_EXECUTE = "com.mulgasoft.emacsplus.metaX";   						   //$NON-NLS-1$
	
	/**
	 * Command extension to ^G clears selection 
	 * Value: <code>"com.mulgasoft.emacsplus.keyboardQuit"</code>
	 */
	final String KEYBOARD_QUIT = "com.mulgasoft.emacsplus.keyboardQuit";					   //$NON-NLS-1$
	
	/**
	 * Command definition for universal-argument ^U
	 * Value: <code>"com.mulgasoft.emacsplus.universalArgument"</code>
	 */
	final String UNIVERSAL_ARGUMENT= "com.mulgasoft.emacsplus.universalArgument";   		   //$NON-NLS-1$
	
	/**
	 * Command definition for goto-char
	 * Value: <code>"com.mulgasoft.emacsplus.gotoChar"</code>
	 */
	final String GOTO_CHAR = "com.mulgasoft.emacsplus.gotoChar";							   //$NON-NLS-1$

	/**
	 * Command definition for goto-line
	 * Value: <code>"com.mulgasoft.emacsplus.gotoLine"</code>
	 */
	final String GOTO_LINE = "com.mulgasoft.emacsplus.gotoLine";							   //$NON-NLS-1$

	/**
	 * Command definition for what-line
	 * Value: <code>"com.mulgasoft.emacsplus.whatLine"</code>
	 */
	final String WHAT_LINE = "com.mulgasoft.emacsplus.whatLine";							   //$NON-NLS-1$
	/**
	 * Command definition for what-cursor-position
	 * Value: <code>"com.mulgasoft.emacsplus.cursorPosition"</code>
	 */
	final String WHAT_CURSOR = "com.mulgasoft.emacsplus.cursorPosition";					   //$NON-NLS-1$
	
	/**
	 * Command definition for goto-line
	 * Value: <code>"com.mulgasoft.emacsplus.gotoLine"</code>
	 */
	final String COUNT_MATCHES = "com.mulgasoft.emacsplus.countMatches";					   //$NON-NLS-1$
	
	/**
	 * Command definition for count-lines-buffer
	 * Value: <code>"com.mulgasoft.emacsplus.countLinesBuffer"</code>
	 */
	final String COUNT_BUFFER= "com.mulgasoft.emacsplus.countLinesBuffer";  				   //$NON-NLS-1$
	
	/**
	 * Command definition for count-lines-region
	 * Value: <code>"com.mulgasoft.emacsplus.countLinesRegion"</code>
	 */
	final String COUNT_REGION = "com.mulgasoft.emacsplus.countLinesRegion"; 				   //$NON-NLS-1$
	
	/**
	 * Command definition for enhanced undo
	 * Value: <code>"com.mulgasoft.emacsplus.undoRedo"</code>
	 */
	final String UNDO_REDO = "com.mulgasoft.emacsplus.undoRedo";							   //$NON-NLS-1$
	
	/**
	 * Command definition for join-line
	 * Value: <code>"com.mulgasoft.emacsplus.joinLine"</code>
	 */
	final String JOIN_LINE = "com.mulgasoft.emacsplus.joinLine";							   //$NON-NLS-1$
	
	/**
	 * Command definition for zap-to-char
	 * Value: <code>"com.mulgasoft.emacsplus.zapToChar"</code>
	 */
	final String ZAP_TO_CHAR = "com.mulgasoft.emacsplus.zapToChar"; 						   //$NON-NLS-1$
	
	/**
	 * Command definition for enhanced align-regexp
	 * Value: <code>"com.mulgasoft.emacsplus.alignRegexp"</code>
	 */
	final String ALIGN_REGEXP = "com.mulgasoft.emacsplus.alignRegexp";  					   //$NON-NLS-1$
	
	/**
	 * Command definitions for use when given a negative argument, otherwise hidden 
	 */
	final String BACKWARD_KILL_LINE = "com.mulgasoft.emacsplus.backwardKillLine";   		   //$NON-NLS-1$
	final String KILL_LINE = "com.mulgasoft.emacsplus.killLine";							   //$NON-NLS-1$
	final String BACKWARD_DELETE_LINE = "com.mulgasoft.emacsplus.backwardDeleteLine";   	   //$NON-NLS-1$

	/**
	 * Command definitions for use in the console view context, otherwise hidden 
	 */
	final String CONSOLE_DELETE_CHAR = "com.mulgasoft.emacsplus.deleteChar";				   //$NON-NLS-1$
	final String CONSOLE_DELETE_NEXT = "com.mulgasoft.emacsplus.deleteNextWord";			   //$NON-NLS-1$
	final String CONSOLE_DELETE_PREVIOUS = "com.mulgasoft.emacsplus.deletePreviousWord";	   //$NON-NLS-1$
	final String CONSOLE_CUT = "com.mulgasoft.emacsplus.consoleCut";						   //$NON-NLS-1$
	final String CONSOLE_COPY = "com.mulgasoft.emacsplus.consoleCopy";  					   //$NON-NLS-1$
	final String CONSOLE_PAGE_DOWN = "com.mulgasoft.emacsplus.consolePageDown"; 			   //$NON-NLS-1$
	final String CONSOLE_PAGE_UP = "com.mulgasoft.emacsplus.consolePageUp"; 				   //$NON-NLS-1$

	// Preferences - allows dynamic changes to the runtime values
	
	final String WINDOW_SPLIT_SELF = "com.mulgasoft.emacsplus.windowSplitSelf"; 			   //$NON-NLS-1$	
	
	// Others
	
	/**
	 * As of Ganymede, not yet defined globally 
	 */
	final String COPY_QUALIFIED_NAME="org.eclipse.jdt.ui.edit.text.java.copy.qualified.name";  //$NON-NLS-1$
	final String COPYQUALIFIEDNAME="copyQualifiedName"; 									   //$NON-NLS-1$
}
