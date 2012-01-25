/*-
 * Copyright (C) 2012 by Iwao AVE!
 * This program is made available under the terms of the MIT License.
 */

package org.eclipselabs.stlipse;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Status;
import org.eclipselabs.stlipse.cache.StlipseCache;

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
				if (resource.getType() == IResource.FILE
					&& ("web.xml".equals(resource.getName()) || "java".equalsIgnoreCase(resource.getFileExtension())))
				{
					StlipseCache.clearBeanClassCache(resource);
					return false;
				}
				return true;
			}
		};

		try
		{
			delta.accept(visitor);
		}
		catch (CoreException e)
		{
			Activator.log(Status.ERROR, e.getMessage(), e);
		}
	}
}
