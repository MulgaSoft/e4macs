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

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.Map.Entry;
import java.util.regex.PatternSyntaxException;

import org.eclipse.core.commands.Category;
import org.eclipse.core.commands.Command;
import org.eclipse.core.commands.IParameter;
import org.eclipse.core.commands.common.NotDefinedException;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.commands.ICommandService;
import org.eclipse.ui.contexts.IContextService;
import org.eclipse.ui.texteditor.ITextEditor;

import com.mulgasoft.emacsplus.EmacsPlusActivator;
import com.mulgasoft.emacsplus.preferences.CommandCategoryEditor;
import com.mulgasoft.emacsplus.preferences.EmacsPlusPreferenceConstants;

/**
 * Command & Category support 
 * 
 * @author Mark Feber - initial API and implementation
 */
public class CommandSupport {
	
	// \p{L}  = \p{Letter}: letter from any language
	// \p{Mn} = \p{Non_Spacing_Mark}: a character intended to be combined with another character without taking up extra space (e.g. accents, umlauts, etc.).
	// \p{Nd} = \p{Decimal_Digit_Number}: a digit zero through nine in any script except ideographic scripts.
	private static final String IDENT_REGEX= "[\\p{L}[\\p{Mn}[\\p{Nd}]]]"; //$NON-NLS-1$
	private static final String DASH = "-"; 							   //$NON-NLS-1$
	private static final String STAR = "*"; 							   //$NON-NLS-1$
	// May use preference for this later
	// support y-p -> yank-pop completion
	boolean partialCompletionMode = true;

	private static String[] catIncludes = null;
	static {
		IPreferenceStore store = EmacsPlusActivator.getDefault().getPreferenceStore();
		if (store != null) {
			catIncludes = CommandCategoryEditor.parseResults(store.getString(EmacsPlusPreferenceConstants.P_COMMAND_CATEGORIES));
		}
	}
		
	private static HashSet<Category> catHash = new HashSet<Category>();
	private static TreeMap<String,Command> commandTree;
	
	/**
	 * Used by preference page to update changes to include categories
	 * @param newCats
	 */
	public static void setCategories(String[] newCats){
		catIncludes = newCats;
		catHash = new HashSet<Category>();		
	}
	
	/**
	 * Get the current set of defined categories corresponding to the included category names
	 * 
	 * @param ics
	 * @return the filtered set of categories
	 * 
	 * @throws NotDefinedException
	 */
	private HashSet<Category> getCategories(ICommandService ics) throws NotDefinedException 
	{
		if (catHash.isEmpty() && catIncludes != null) {
			Category[] cats = ics.getDefinedCategories();
			for (int i = 0; i < cats.length; i++) {
				for (int j = 0; j < catIncludes.length; j++) {
					if (catIncludes[j].equals(cats[i].getId())) {
						catHash.add(cats[i]);
						break;
					}
				}
			}
		}
		return catHash;
	}
	
	public void getContexts(ITextEditor editor) {
		IContextService contextService = (IContextService) editor.getSite().getService(IContextService.class);
		@SuppressWarnings("unchecked")	// Eclipse documents the collection type
		Collection<String> col = contextService.getActiveContextIds();
		Iterator<String> it = col.iterator();
		while (it.hasNext()) {
			System.out.println("context: " + it.next());		//$NON-NLS-1$
		}
	}
	/**
	 * Compute the list of commands appropriate for the current editor
	 * The result list is filtered by:
	 *  - The set of the defined non-exclusion categories
	 *  - Having a defined handler
	 *  - All parameters optional
	 *  
	 * @param editor
	 * 
	 * @return the sorted tree of appropriate commands
	 */
	public TreeMap<String, Command> getCommandList(IEditorPart editor) {
		return getCommandList(editor,false);
	}
	
	public TreeMap<String, Command> getCommandList(IEditorPart editor, boolean all) {
		ICommandService ics = (ICommandService) editor.getSite().getService(ICommandService.class);
		Command[] commands = ics.getDefinedCommands();
		commandTree = new TreeMap<String, Command>();
		try {
			HashSet<Category>catHash = (all ? null : getCategories(ics));
			boolean isOk = all;
			for (int i = 0; i < commands.length; i++) {
				if (!isOk && catHash.contains(commands[i].getCategory())) {
					IParameter[] params = commands[i].getParameters(); 
					isOk = commands[i].isHandled();
					// if the command has parameters, they must all be optional
					if (isOk && params != null) {
						for (int j = 0; j < params.length; j++) {
							if (!(isOk = params[j].isOptional())) {
								break;
							}
						}
					}
				}
				if (isOk) {
					commandTree.put(fixName(commands[i].getName()), commands[i]);
				}
				isOk = all;
			}
		} catch (NotDefinedException e) {}
//		getContexts(editor);
		return commandTree;
	}

	// TODO: Clean up
	/**
	 * Compute the command subtree given the current user input
	 * 
	 * @param map
	 * @param subString
	 * @return the computed sub map
	 */
	public SortedMap<String, Command> getCommandSubTree(SortedMap<String, Command> map, String subString) {
		return getCommandSubTree(map,subString,false); 
	}
	
	/**
	 * @param map
	 * @param subString
	 * @param isRegex - true if subString already converted to a regex
	 * 
	 * @return the computed sub map
	 */
	public SortedMap<String, Command> getCommandSubTree(SortedMap<String, Command> map, String subString, boolean isRegex) {
		return this.getCommandSubTree(map, subString, isRegex, false);
	}
	
	/**
	 * @param map
	 * @param subString
	 * @param isRegex - true if subString already converted to a regex
	 * @param ignoreEnabled - true to return all appropriate commands
	 * 
	 * @return the computed sub map
	 */
	public SortedMap<String, Command> getCommandSubTree(SortedMap<String, Command> map, String subString, boolean isRegex, boolean ignoreEnabled) {
		SortedMap<String, Command> result = null;

		Set<String> keySet = map.keySet();
		String fromKey = null;
		String toKey = null;

		String searchStr = (isRegex ? subString : toRegex(subString));
		Iterator<String> it = keySet.iterator();
		String key;
		if (isRegex || !searchStr.equals(subString)) {
			// we have to build the map up one by one on regex search
			result = new TreeMap<String,Command>();
			try {
				while (it.hasNext()) {
					key = it.next();
					if (key.matches(searchStr)) {  
						Command c = map.get(key);
						if (ignoreEnabled || c.isEnabled()) {	// make sure it's enabled
							result.put(key, c);
						}
					}
				}
			} catch (PatternSyntaxException e) {
				// ignore bad pattern - will show as no match
			}
		} else {
			while (it.hasNext()) {
				key = it.next();
				if (key.startsWith(subString)) {
					if (fromKey == null) {
						fromKey = key;
					}
				} else if (fromKey != null) {
					toKey = key;
					break;
				}
			}
			// too bad we can't use 1.6
			if (fromKey != null) {
				if (toKey == null) {
					result = map.tailMap(fromKey);
				} else {
					result = map.subMap(fromKey, toKey);
				}
			}
			if (!ignoreEnabled && result != null) {
				// enforce enabled for the current position/selection
				SortedMap<String, Command> firstResult = result;
				result = new TreeMap<String, Command>();
				Set<Entry<String, Command>> entrySet = firstResult.entrySet();
				Iterator<Entry<String, Command>> itr = entrySet.iterator();
				while (itr.hasNext()) {
					Entry<String, Command> e = itr.next();
					if (e.getValue().isEnabled()) {
						result.put(e.getKey(), e.getValue());
					}
				}
			} 
		}
		if (result == null && partialCompletionMode && !isRegex) {
			// recurse once with modified search string

			// searchStr.replace("-", "\\w*-") + "\\w*";
		  	searchStr = searchStr.replace(DASH, IDENT_REGEX + STAR + DASH) + IDENT_REGEX + STAR; 
			result = getCommandSubTree(map, searchStr, true, ignoreEnabled);
		}
		return result;
	}
	

	/**
	 * Replace all spaces in the name with dashes a la emacs
	 * Also, avoid any name collisions by adding an index if necessary
	 *  
	 * @param name
	 * @return fixed name
	 */
	private String fixName(String name){
		String result = name.trim();
		result = result.toLowerCase().replace(" ","-");	//$NON-NLS-1$ //$NON-NLS-2$
		// avoid collisions
		if (commandTree.get(result) != null) {
			int i = 1;
			String tmp = result + "(" + i + ")"; 	//$NON-NLS-1$ //$NON-NLS-2$
			while (commandTree.get(tmp)!= null){
				tmp = result + "(" + ++i + ")";		//$NON-NLS-1$ //$NON-NLS-2$
			}
			result = tmp;
		}
		return result;
	}
	
	/**
	 * Determine the longest common command name substring that starts with the current substring
	 *  
	 * @param subTree
	 * @param subString
	 * @return the longest common name
	 */
	public String getCommonString(SortedMap<String, Command> subTree, String subString) {
		String result = (isWildCarded(subString) ? "" : subString);	//$NON-NLS-1$
		Set<String> keySet = subTree.keySet();
		Iterator<String> it = keySet.iterator();

		String key;
		String possible;

		key = it.next();
		do {
			if (key.length() > result.length()) {
				possible = key.substring(0, result.length()+1);
				while (it.hasNext()) {
					key = it.next();
					if (!key.startsWith(possible)) {
						return result;
					}
				}
				result = possible;
				it = keySet.iterator();
				key = it.next();
			}
		} while (result.length() < key.length());
		return result;
	}
	
	/**
	 * Does the command string contain wild cards?
	 * 
	 * @param searchStr
	 * @return true if wildcards present
	 */
	protected boolean isWildCarded(String searchStr){
		return (searchStr.matches(".*[\\?|\\*].*"));	//$NON-NLS-1$
	}
		
	/**
	 * Convert command string to simple regex
	 * 
	 * @param searchStr
	 * @return searchStr with simple wildcards replaced with regexp syntax
	 */
	protected String toRegex(String searchStr){
		String result = searchStr; 
		if (searchStr != null && isWildCarded(searchStr)) {
			result = searchStr.replace("*", ".*"); //$NON-NLS-1$ //$NON-NLS-2$
			result = result.replace("?", ".");     //$NON-NLS-1$ //$NON-NLS-2$
		}
		return result;
	}
}
