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
 * @author Mark Feber - initial API and implementation
 */
public class RegexpRingBuffer extends RingBuffer<String> {

	/**
	 * @see com.mulgasoft.emacsplus.RingBuffer#getNewElement()
	 */
	@Override
	protected IRingBufferElement<String> getNewElement() {
		return new RegexpRingBufferElement();
	}

	public RegexpRingBufferElement get() {
		return (RegexpRingBufferElement)getElement();
	}
	
	public class RegexpRingBufferElement extends AbstractRingBufferElement {
		private String regexp;

		/**
		 * @return the regexp
		 */
		public String getRegexp() {
			String result = regexp;
			if (result == null) {
				result = this.get();
			}
			return result;
		}

		/**
		 * @param regexp the regexp to set
		 */
		public void setRegexp(String regexp) {
			this.regexp = regexp;
		}
	}
}
