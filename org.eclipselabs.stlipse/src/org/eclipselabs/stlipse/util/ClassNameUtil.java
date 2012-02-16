/*-
 * Copyright (C) 2011-2012 by Iwao AVE!
 * This program is made available under the terms of the MIT License.
 */

package org.eclipselabs.stlipse.util;

/**
 * @author Iwao AVE!
 */
public class ClassNameUtil
{
	public static String getPackage(String fqn)
	{
		if (fqn == null)
			return "";
		int lastDot = fqn.lastIndexOf('.');
		return lastDot > -1 ? fqn.substring(0, lastDot) : "";
	}

	public static String getTypeName(String fqn)
	{
		if (fqn == null)
			return "";
		int lastDot = fqn.lastIndexOf('.');
		return lastDot > -1 ? fqn.substring(lastDot + 1) : fqn;
	}
}
