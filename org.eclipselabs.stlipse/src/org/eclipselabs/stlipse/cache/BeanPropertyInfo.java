/*-
 * Copyright (C) 2011-2012 by Iwao AVE!
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

	private Map<String, EventProperty> eventHandlers;

	public BeanPropertyInfo(
		Map<String, String> readableFields,
		Map<String, String> writableFields,
		Map<String, EventProperty> eventHandlers)
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

	public Map<String, EventProperty> getEventHandlers()
	{
		return eventHandlers;
	}
}
