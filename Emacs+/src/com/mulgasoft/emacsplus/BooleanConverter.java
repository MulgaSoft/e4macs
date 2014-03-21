/**
 * Copyright (c) 2009, 2010 Mark Feber, MulgaSoft
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package com.mulgasoft.emacsplus;

import org.eclipse.core.commands.AbstractParameterValueConverter;
import org.eclipse.core.commands.ParameterValueConversionException;

/**
 * Minimal Boolean converter
 * 
 * @author Mark Feber - initial API and implementation
 */
public class BooleanConverter extends AbstractParameterValueConverter {
	
	/**
	 * @see org.eclipse.core.commands.AbstractParameterValueConverter#convertToObject(java.lang.String)
	 */
	@Override
	public Object convertToObject(String parameterValue) throws ParameterValueConversionException {
		return Boolean.valueOf(parameterValue);
	}

	/**
	 * @see org.eclipse.core.commands.AbstractParameterValueConverter#convertToString(java.lang.Object)
	 */
	@Override
	public String convertToString(Object parameterValue) throws ParameterValueConversionException {
		return parameterValue.toString(); 
	}

}
