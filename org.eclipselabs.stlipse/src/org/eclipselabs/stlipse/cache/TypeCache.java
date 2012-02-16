/*-
 * Copyright (C) 2011-2012 by Iwao AVE!
 * This program is made available under the terms of the MIT License.
 */

package org.eclipselabs.stlipse.cache;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;
import org.eclipse.jdt.core.JavaModelException;

/**
 * @author Iwao AVE!
 */
public class TypeCache
{
	private static final String ACTION_BEAN = "net.sourceforge.stripes.action.ActionBean";

	private static final String RESOLUTION = "net.sourceforge.stripes.action.Resolution";

	private static final Map<IJavaProject, Map<String, IType>> typeCache = new ConcurrentHashMap<IJavaProject, Map<String, IType>>();

	public static IType getActionBean(IJavaProject project) throws JavaModelException
	{
		return getType(project, ACTION_BEAN);
	}

	public static IType getResolution(IJavaProject project) throws JavaModelException
	{
		return getType(project, RESOLUTION);
	}

	public static boolean isActionBean(final IJavaProject project, final IType type)
		throws JavaModelException
	{
		final IType actionBean = TypeCache.getActionBean(project);
		return isAssignable(type, actionBean);
	}

	public static boolean isResolution(final IJavaProject project, final IType type)
		throws JavaModelException
	{
		final IType resolution = TypeCache.getResolution(project);
		return isAssignable(type, resolution);
	}

	private static boolean isAssignable(final IType type, final IType targetType)
		throws JavaModelException
	{
		final ITypeHierarchy supertypes = type.newSupertypeHierarchy(new NullProgressMonitor());
		return supertypes.contains(targetType);
	}

	private static IType getType(IJavaProject project, String qualifiedName)
		throws JavaModelException
	{
		IType type = null;
		Map<String, IType> types = typeCache.get(project);
		if (types == null)
		{
			types = new ConcurrentHashMap<String, IType>();
		}
		if (types.containsKey(qualifiedName))
		{
			type = types.get(qualifiedName);
		}
		else
		{
			type = project.findType(qualifiedName);
			if (type != null)
				types.put(qualifiedName, type);
		}
		return type;
	}
}
