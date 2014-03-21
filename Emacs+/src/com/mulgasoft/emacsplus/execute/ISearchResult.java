/**
 * Copyright (c) 2009, 2010 Mark Feber, MulgaSoft
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package com.mulgasoft.emacsplus.execute;

/**
 * A minibuffer result interface for string searches
 * 
 * @author Mark Feber - initial API and implementation
 */
public interface ISearchResult {

	/** Get the string for which to search (may be regexp) */
	String getSearchStr();
	/** Return true if search should be case sensitive */
	boolean isCaseSensitive();
}
