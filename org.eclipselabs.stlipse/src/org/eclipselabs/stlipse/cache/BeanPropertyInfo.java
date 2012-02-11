/*-
 * Copyright (C) 2012 by Iwao AVE!
 * This program is made available under the terms of the MIT License.
 */

package org.eclipselabs.stlipse.cache;

import java.util.Map;

/**
 * @author Iwao AVE!
 */
public class BeanPropertyInfo
{
	private Map<String, String> readableFields;

	private Map<String, String> writableFields;

	private Map<String, Boolean> eventHandlers;

	public BeanPropertyInfo(
		Map<String, String> readableFields,
		Map<String, String> writableFields,
		Map<String, Boolean> eventHandlers)
	{
		super();
		this.readableFields = readableFields;
		this.writableFields = writableFields;
		this.eventHandlers = eventHandlers;
	}

	public Map<String, String> getReadableFields()
	{
		return readableFields;
	}

	public Map<String, String> getWritableFields()
	{
		return writableFields;
	}

	public Map<String, Boolean> getEventHandlers()
	{
		return eventHandlers;
	}
}
