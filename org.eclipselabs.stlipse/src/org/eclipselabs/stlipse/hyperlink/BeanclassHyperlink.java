/*-
 * Copyright (C) 2011 by Iwao AVE!
 * This program is made available under the terms of the MIT License.
 */

package org.eclipselabs.stlipse.hyperlink;

import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.hyperlink.IHyperlink;
import org.eclipse.ui.PartInitException;
import org.eclipselabs.stlipse.Activator;

/**
 * @author Iwao AVE!
 */
public class BeanclassHyperlink implements IHyperlink
{

	private IType type;

	private IRegion region;

	public BeanclassHyperlink(IType type, IRegion region)
	{
		super();
		this.type = type;
		this.region = region;
	}

	public IRegion getHyperlinkRegion()
	{
		return region;
	}

	public String getTypeLabel()
	{
		return null;
	}

	public String getHyperlinkText()
	{
		return null;
	}

	public void open()
	{
		try
		{
			JavaUI.openInEditor(type);
		}
		catch (PartInitException e)
		{
			ILog log = Activator.getDefault().getLog();
			log.log(new Status(Status.WARNING, Activator.PLUGIN_ID, 0,
				"Failed to open Java editor fir type: " + type.getFullyQualifiedName(), e));
		}
		catch (JavaModelException e)
		{
			ILog log = Activator.getDefault().getLog();
			log.log(new Status(Status.WARNING, Activator.PLUGIN_ID, 0,
				"Failed to open Java editor fir type: " + type.getFullyQualifiedName(), e));
		}
	}

}
