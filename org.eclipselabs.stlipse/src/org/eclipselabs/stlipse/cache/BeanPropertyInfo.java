/*-
 * Copyright (C) 2012 by Iwao AVE!
 * This program is made available under the terms of the MIT License.
 */

package org.eclipselabs.stlipse.cache;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Iwao AVE!
 */
public class BeanPropertyInfo
{
	private Map<String, String> readableFields = new HashMap<String, String>();

	private Map<String, String> writableFields = new HashMap<String, String>();

	public BeanPropertyInfo(Map<String, String> readableFields, Map<String, String> writableFields)
	{
		super();
		this.readableFields = readableFields;
		this.writableFields = writableFields;
	}

	public Map<String, String> getReadableFields()
	{
		return readableFields;
	}

	public Map<String, String> getWritableFields()
	{
		return writableFields;
	}
}
