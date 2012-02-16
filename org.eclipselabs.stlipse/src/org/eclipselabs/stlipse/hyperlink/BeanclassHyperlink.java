/*-
 * Copyright (C) 2011-2012 by Iwao AVE!
 * This program is made available under the terms of the MIT License.
 */

package org.eclipselabs.stlipse.hyperlink;

import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.hyperlink.IHyperlink;
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
		return "Open ActionBean class in Java editor.";
	}

	public String getHyperlinkText()
	{
		return "Open ActionBean class in Java editor.";
	}

	public void open()
	{
		try
		{
			JavaUI.openInEditor(type);
		}
		catch (Exception e)
		{
			Activator.log(Status.WARNING,
				"Failed to open Java editor for type: " + type.getFullyQualifiedName(), e);
		}
	}

}
