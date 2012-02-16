/*-
 * Copyright (C) 2011-2012 by Iwao AVE!
 * This program is made available under the terms of the MIT License.
 */

package org.eclipselabs.stlipse;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipselabs.stlipse.cache.BeanClassCache;
import org.eclipselabs.stlipse.cache.BeanPropertyCache;
import org.eclipselabs.stlipse.cache.TypeCache;

/**
 * @author Iwao AVE!
 */
public class ResourceChangeListener implements IResourceChangeListener
{

	public void resourceChanged(IResourceChangeEvent event)
	{
		if (event.getType() != IResourceChangeEvent.POST_CHANGE)
			return;

		IResourceDelta delta = event.getDelta();

		IResourceDeltaVisitor visitor = new IResourceDeltaVisitor()
		{
			public boolean visit(IResourceDelta delta) throws CoreException
			{
				IResource resource = delta.getResource();
				if (resource.getType() == IResource.FILE)
				{
					int flags = delta.getFlags();
					int kind = delta.getKind();
					IProject project = resource.getProject();

					if (isWebXml(resource))
					{
						BeanClassCache.clear(project);
					}
					else if ("java".equals(resource.getFileExtension()))
					{
						final IFile file = (IFile)resource;
						final ICompilationUnit compilationUnit = (ICompilationUnit)JavaCore.create(file);
						final String elementName = compilationUnit.getElementName();
						final String simpleTypeName = elementName.substring(0, elementName.length() - 5);
						final IType type = compilationUnit.getType(simpleTypeName);
						final String packageName = type.getPackageFragment().getElementName();

						switch (kind)
						{
							case IResourceDelta.ADDED:
								if (isActionBean(compilationUnit, type))
									BeanClassCache.add(project, packageName, simpleTypeName);
								break;
							case IResourceDelta.REMOVED:
								// No need to check type when removed.
								BeanClassCache.remove(project, packageName, simpleTypeName);
								break;
							case IResourceDelta.CHANGED:
								if ((flags & IResourceDelta.CONTENT) != 0
									|| (flags & IResourceDelta.MOVED_TO) != 0
									|| flags == IResourceDelta.NO_CHANGE)
								{
									try
									{
										if (isActionBean(compilationUnit, type))
											BeanClassCache.add(project, packageName, simpleTypeName);
										// Remove bean property cache.
										String qualifiedName = type.getFullyQualifiedName();
										BeanPropertyCache.clearBeanPropertyCache(project, qualifiedName);
									}
									catch (JavaModelException e)
									{
										BeanClassCache.clear(project);
										BeanPropertyCache.clearBeanPropertyCache(project);
									}
								}
								break;
							default:
								break;
						}
					}
					return false;
				}
				return true;
			}

			private boolean isWebXml(IResource resource)
			{
				return "web.xml".equals(resource.getName());
			}

			private boolean isActionBean(final ICompilationUnit compilationUnit, final IType type)
				throws JavaModelException
			{
				int typeFlags = type.getFlags();
				if (Flags.isPublic(typeFlags) && !Flags.isAbstract(typeFlags)
					&& !Flags.isInterface(typeFlags))
				{
					final ITypeHierarchy supertypes = type.newSupertypeHierarchy(new NullProgressMonitor());
					final IType actionBean = TypeCache.getActionBean(compilationUnit.getJavaProject());
					return actionBean == null ? false : supertypes.contains(actionBean);
				}
				return false;
			}
		};

		try
		{
			if (delta != null)
				delta.accept(visitor);
		}
		catch (CoreException e)
		{
			Activator.log(Status.ERROR, e.getMessage(), e);
		}
	}
}
