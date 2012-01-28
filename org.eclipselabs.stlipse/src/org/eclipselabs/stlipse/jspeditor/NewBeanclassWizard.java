/*-
 * Copyright (C) 2011 by Iwao AVE!
 * This program is made available under the terms of the MIT License.
 */

package org.eclipselabs.stlipse.jspeditor;

import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.ui.wizards.NewClassCreationWizard;
import org.eclipse.jdt.ui.wizards.NewClassWizardPage;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.eclipselabs.stlipse.Activator;
import org.eclipselabs.stlipse.util.ClassNameUtil;
import org.eclipselabs.stlipse.util.ProjectUtil;

/**
 * @author Iwao AVE!
 */
public class NewBeanclassWizard
{
	private IJavaProject project;

	private String fqn;

	public NewBeanclassWizard(IJavaProject project, String fqn)
	{
		super();
		this.project = project;
		this.fqn = fqn;
	}

	public IType create()
	{
		// Create a new wizard page.
		NewClassWizardPage page = createWizardPage();

		// Create wizard with the page.
		NewClassCreationWizard wizard = new NewClassCreationWizard(page, true);
		wizard.init(PlatformUI.getWorkbench(), null);

		// Create wizard dialog.
		WizardDialog dialog = createWizardDialog(wizard);

		IType createdType = null;
		if (dialog.open() == Window.OK)
		{
			createdType = (IType)wizard.getCreatedElement();
		}
		return createdType;
	}

	private WizardDialog createWizardDialog(NewClassCreationWizard wizard)
	{
		Shell shell = Display.getDefault().getActiveShell();
		WizardDialog dialog = new WizardDialog(shell, wizard);
		PixelConverter converter = new PixelConverter(JFaceResources.getDialogFont());
		dialog.setMinimumPageSize(converter.convertWidthInCharsToPixels(70),
			converter.convertHeightInCharsToPixels(20));
		dialog.create();
		dialog.getShell().setText("Create New ActionBean");
		return dialog;
	}

	private NewClassWizardPage createWizardPage()
	{
		NewClassWizardPage page = new NewClassWizardPage();
		if (fqn != null)
		{
			page.setTypeName(ClassNameUtil.getTypeName(fqn), false);
			try
			{
				IPackageFragmentRoot pkgRoot = ProjectUtil.getFirstSourceRoot(project);
				page.setPackageFragmentRoot(pkgRoot, true);
				IPackageFragment pkgFragment = pkgRoot.createPackageFragment(
					ClassNameUtil.getPackage(fqn), false, null);
				page.setPackageFragment(pkgFragment, false);
			}
			catch (JavaModelException e)
			{
				Activator.log(Status.ERROR, "Error occurred while resolving package fragment.",
					e);
			}
		}
		return page;
	}
}
