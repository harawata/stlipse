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

	public BeanClassInfo(String packageName, String simpleTypeName)
	{
		super();
		if (packageName != null)
			this.packageName = packageName.toCharArray();
		if (simpleTypeName != null)
			this.simpleTypeName = simpleTypeName.toCharArray();
	}

	public char[] getPackageName()
	{
		return packageName;
	}

	public char[] getSimpleTypeName()
	{
		return simpleTypeName;
	}

	public boolean matches(String qualifiedName)
	{
		if (qualifiedName == null || qualifiedName.length() == 0)
			return false;

		final StringBuilder sb = new StringBuilder();
		sb.append(packageName).append('.').append(simpleTypeName);
		return qualifiedName.equals(sb.toString());
	}

	public boolean matches(char[] pkg, char[] type)
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

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode(packageName);
		result = prime * result + Arrays.hashCode(simpleTypeName);
		return result;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		BeanClassInfo other = (BeanClassInfo)obj;
		if (!Arrays.equals(packageName, other.packageName))
			return false;
		if (!Arrays.equals(simpleTypeName, other.simpleTypeName))
			return false;
		return true;
	}
}
