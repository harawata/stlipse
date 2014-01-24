/*-
 * Copyright (C) 2011-2014 by Iwao AVE!
 * This program is made available under the terms of the MIT License.
 */

package org.eclipselabs.stlipse.jspeditor;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.ui.IMarkerResolution;
import org.eclipselabs.stlipse.Activator;

/**
 * @author Iwao AVE!
 */
public class JspMarkerResolution implements IMarkerResolution
{
	public JspMarkerResolution()
	{
		super();
	}

	public String getLabel()
	{
		return "Create the new java class.";
	}

	public void run(IMarker marker)
	{
		IJavaProject project = JavaCore.create(marker.getResource().getProject());
		String fqn = marker.getAttribute("errorValue", "");
		NewBeanclassWizard wizard = new NewBeanclassWizard(project, fqn);
		IType createdType = wizard.create();
		if (createdType != null)
		{
			try
			{
				marker.delete();
			}
			catch (CoreException e)
			{
				Activator.log(Status.ERROR, "Failed to delete the marker.", e);
			}
		}
	}
}
