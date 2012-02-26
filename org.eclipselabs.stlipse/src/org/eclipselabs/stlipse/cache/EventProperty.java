/*-
 * Copyright (C) 2011-2012 by Iwao AVE!
 * This program is made available under the terms of the MIT License.
 */

package org.eclipselabs.stlipse.cache;

/**
 * @author Iwao AVE!
 */
public class EventProperty
{
	private boolean defaultHandler;

	private String methodName;

	public boolean isDefaultHandler()
	{
		return defaultHandler;
	}

	public void setDefaultHandler(boolean defaultHandler)
	{
		this.defaultHandler = defaultHandler;
	}

	public String getMethodName()
	{
		return methodName;
	}

	public void setMethodName(String methodName)
	{
		this.methodName = methodName;
	}
}
