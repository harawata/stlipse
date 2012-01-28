/*-
 * Copyright (C) 2012 by Iwao AVE!
 * This program is made available under the terms of the MIT License.
 */

package org.eclipselabs.stlipse.cache;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Status;
import org.eclipse.emf.common.util.EList;
import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jdt.core.search.SearchPattern;
import org.eclipse.jdt.core.search.TypeNameRequestor;
import org.eclipse.jst.j2ee.common.ParamValue;
import org.eclipse.jst.j2ee.web.componentcore.util.WebArtifactEdit;
import org.eclipse.jst.j2ee.webapplication.Filter;
import org.eclipse.jst.j2ee.webapplication.WebApp;
import org.eclipselabs.stlipse.Activator;

/**
 * @author Iwao AVE!
 */
public class BeanClassCache
{
	private static final Map<IProject, List<BeanClassInfo>> projectCache = new ConcurrentHashMap<IProject, List<BeanClassInfo>>();

	public static List<BeanClassInfo> getBeanClassInfo(IJavaProject project)
	{
		List<BeanClassInfo> beanClassList = projectCache.get(project.getProject());
		if (beanClassList == null)
			beanClassList = buildBeanClassCache(project);

		return beanClassList;
	}

	public static void clearBeanClassCache(IProject project)
	{
		projectCache.remove(project);
	}

	public static void add(IProject project, String packageName, String simpleTypeName)
	{
		List<BeanClassInfo> beanClassList = projectCache.get(project);
		if (beanClassList != null)
		{
			BeanClassInfo beanClassInfo = new BeanClassInfo(packageName.toCharArray(),
				simpleTypeName.toCharArray());
			beanClassList.remove(beanClassInfo);
			beanClassList.add(0, beanClassInfo);
		}
	}

	public static void remove(IProject project, String packageName, String simpleTypeName)
	{
		List<BeanClassInfo> beanClassList = projectCache.get(project);
		if (beanClassList != null)
		{
			beanClassList.remove(new BeanClassInfo(packageName.toCharArray(),
				simpleTypeName.toCharArray()));
		}
	}

	private static List<BeanClassInfo> buildBeanClassCache(IJavaProject project)
	{
		final List<BeanClassInfo> beanClassList = new ArrayList<BeanClassInfo>();
		projectCache.put(project.getProject(), beanClassList);

		final List<String> packageList = getActionResolverPackages(project.getProject());
		if (packageList.size() == 0)
		{
			// Returns an empty list if no package root for action beans is defined.
			return beanClassList;
		}

		IType actionBeanInterface;
		try
		{
			actionBeanInterface = project.findType("net.sourceforge.stripes.action.ActionBean");
			IJavaSearchScope scope = SearchEngine.createHierarchyScope(actionBeanInterface);
			TypeNameRequestor requestor = new TypeNameRequestor()
			{
				@Override
				public void acceptType(int modifiers, char[] packageName, char[] simpleTypeName,
					char[][] enclosingTypeNames, String path)
				{
					// Ignore abstract classes.
					if (Flags.isAbstract(modifiers))
						return;
					// Should be in action resolver packages.
					if (!isInTargetPackage(packageList, String.valueOf(packageName)))
						return;

					BeanClassInfo beanClass = new BeanClassInfo(packageName, simpleTypeName);
					beanClassList.add(beanClass);
				}

				private boolean isInTargetPackage(List<String> targetPackages, String packageToTest)
				{
					if (targetPackages == null || packageToTest == null)
						return false;

					for (String targetPackage : targetPackages)
					{
						if (packageToTest.startsWith(targetPackage))
							return true;
					}
					return false;
				}
			};

			SearchEngine searchEngine = new SearchEngine();
			searchEngine.searchAllTypeNames(null, SearchPattern.R_EXACT_MATCH, null,
				SearchPattern.R_EXACT_MATCH, IJavaSearchConstants.DECLARATIONS
					| IJavaSearchConstants.IMPLEMENTORS, scope, requestor,
				IJavaSearchConstants.WAIT_UNTIL_READY_TO_SEARCH, null);
		}
		catch (CoreException e)
		{
			Activator.log(Status.ERROR, "Error occurred while creating proposals.", e);
		}
		return beanClassList;
	}

	private static List<String> getActionResolverPackages(IProject project)
	{
		List<String> packageList = new ArrayList<String>();
		WebArtifactEdit artifactEdit = WebArtifactEdit.getWebArtifactEditForRead(project);
		WebApp webApp = artifactEdit.getWebApp();
		EList<Filter> filters = webApp.getFilters();
		for (Filter filter : filters)
		{
			EList<ParamValue> initParamValues = filter.getInitParamValues();
			for (ParamValue paramValue : initParamValues)
			{
				if ("ActionResolver.Packages".equals(paramValue.getName()))
				{
					String packages = paramValue.getValue();
					if (packages != null)
					{
						String[] pkgArray = packages.split(",");
						for (String pkg : pkgArray)
						{
							String trimmed = pkg.trim();
							if (trimmed.length() > 0)
								packageList.add(trimmed);
						}
					}
				}
			}
		}

		if (packageList.size() == 0)
		{
			Activator.log(Status.WARNING,
				"The filter init-param 'ActionResolver.Packages' was not found in your web.xml. ");
		}

		return packageList;
	}
}
