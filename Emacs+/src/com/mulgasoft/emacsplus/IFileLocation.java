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

import org.eclipse.core.resources.IFile;

/**
 * @author Mark Feber - initial API and implementation
 */
public interface IFileLocation {
	IFile getPath();
	int getOffset();
	void setPath(IFile path);
	void setOffset(int offset);
}
