/*-
 * Copyright (C) 2011-2012 by Iwao AVE!
 * This program is made available under the terms of the MIT License.
 */

package org.eclipselabs.stlipse.jspeditor;

import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipselabs.stlipse.Activator;
import org.eclipselabs.stlipse.util.ClassNameUtil;
import org.eclipselabs.stlipse.util.ProjectUtil;

/**
 * @author Iwao AVE!
 */
public class NewBeanclassProposal implements ICompletionProposal
{
	private String fqn;

	public NewBeanclassProposal(String fqn)
	{
		super();
		this.fqn = fqn;
	}

	public void apply(IDocument document)
	{
		IJavaProject project = ProjectUtil.getProjectFromDocument(document);
		NewBeanclassWizard wizard = new NewBeanclassWizard(project, fqn);
		IType createdType = wizard.create();
	}

	public Point getSelection(IDocument document)
	{
		return null;
	}

	public String getAdditionalProposalInfo()
	{
		StringBuilder sb = new StringBuilder();
		sb.append("Opens the new class wizard to create the type.")
			.append("<br><br>")
			.append("Package: ")
			.append(ClassNameUtil.getPackage(fqn))
			.append("<br>")
			.append("public class ")
			.append(ClassNameUtil.getTypeName(fqn))
			.append(" {<br>}");
		return sb.toString();
	}

	public String getDisplayString()
	{
		return "Create the new java class.";
	}

	public Image getImage()
	{
		return Activator.getIcon();
	}

	public IContextInformation getContextInformation()
	{
		return null;
	}

}
