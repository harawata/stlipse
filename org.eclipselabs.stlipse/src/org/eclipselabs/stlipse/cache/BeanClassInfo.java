/*-
 * Copyright (C) 2012 by Iwao AVE!
 * This program is made available under the terms of the MIT License.
 */

package org.eclipselabs.stlipse.cache;

import java.util.Arrays;

import org.eclipse.jdt.core.compiler.CharOperation;

/**
 * @author Iwao AVE!
 */
public class BeanClassInfo
{
	private char[] packageName;

	private char[] simpleTypeName;

	public BeanClassInfo(char[] packageName, char[] simpleTypeName)
	{
		super();
		this.packageName = packageName;
		this.simpleTypeName = simpleTypeName;
	}

	public char[] getPackageName()
	{
		return packageName;
	}

	public char[] getSimpleTypeName()
	{
		return simpleTypeName;
	}

	public boolean isMatch(char[] pkg, char[] type)
	{
		if (type.length == 0)
		{
			return CharOperation.prefixEquals(pkg, packageName);
		}
		else
		{
			if (!CharOperation.prefixEquals(pkg, packageName))
			{
				return false;
			}
			else if (type[0] < 'A' || type[0] > 'Z')
			{
				return CharOperation.prefixEquals(type, simpleTypeName, false);
			}
			else
			{
				return CharOperation.camelCaseMatch(type, simpleTypeName);
			}
		}
	}

	@Override
	public String toString()
	{
		return "BeanClassInfo [packageName=" + Arrays.toString(packageName) + ", simpleTypeName="
			+ Arrays.toString(simpleTypeName) + "]";
	}
}
