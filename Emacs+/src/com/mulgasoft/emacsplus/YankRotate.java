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

/**
 * Set up rotate-yank-pointer directions for use in RingBuffers
 * 
 * @see com.mulgasoft.emacsplus.RingBuffer
 * 
 * @author Mark Feber - initial API and implementation
 */
public enum YankRotate {
	/** A direction that does not rotate */
	EREWHON (0,"nowhere"), 		  //$NON-NLS-1$
	/** Rotate direction towards earlier entries */
	FORWARD (-1,"forward"),   //$NON-NLS-1$
	/** Rotate direction toward later entries */
	BACKWARD (1, "backward"); //$NON-NLS-1$
	
	private int direction;
	private String id;
	private YankRotate(int dir,String id){this.direction = dir; this.id = id;}
	
	/**
	 * Return the direction of the rotation as an int for use in RingBuffers
	 *  
	 * @return the int direction of rotation 
	 */
	int direction() {return direction;}
	/** 
	 * Get the printable id 
	 * @return the printable id
	 */
	public String id() {return id;}
}
