/*-
 * Copyright (C) 2011 by Iwao AVE!
 * This program is made available under the terms of the MIT License.
 */

package org.eclipselabs.stlipse.validation;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.wst.sse.core.StructuredModelManager;
import org.eclipse.wst.sse.core.internal.provisional.IStructuredModel;
import org.eclipse.wst.sse.core.internal.provisional.text.IStructuredDocument;
import org.eclipse.wst.validation.AbstractValidator;
import org.eclipse.wst.validation.ValidationResult;
import org.eclipse.wst.validation.ValidationState;
import org.eclipse.wst.validation.internal.core.ValidationException;
import org.eclipse.wst.validation.internal.provisional.core.IReporter;
import org.eclipse.wst.validation.internal.provisional.core.IValidationContext;
import org.eclipse.wst.validation.internal.provisional.core.IValidator;
import org.eclipse.wst.xml.core.internal.provisional.document.IDOMAttr;
import org.eclipse.wst.xml.core.internal.provisional.document.IDOMDocument;
import org.eclipse.wst.xml.core.internal.provisional.document.IDOMElement;
import org.eclipse.wst.xml.core.internal.provisional.document.IDOMModel;
import org.eclipselabs.stlipse.Activator;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * @author Iwao AVE!
 */
public class JspValidator extends AbstractValidator implements IValidator
{

	public static final String MARKER_ID = "org.eclipselabs.stlipse.stripesJspProblem";

	public static final String MISSING_ACTION_BEAN = "missingActionBean";

	public void cleanup(IReporter reporter)
	{
		// Nothing to do.
	}

	public void validate(IValidationContext helper, IReporter reporter)
		throws ValidationException
	{
		String[] uris = helper.getURIs();
		if (uris != null)
		{
			for (int i = 0; i < uris.length && !reporter.isCancelled(); i++)
			{
				IFile file = ResourcesPlugin.getWorkspace()
					.getRoot()
					.getFile(new Path(uris[i]));
				validateFile(file, reporter);
			}
		}
	}

	public ValidationResult validate(final IResource resource, int kind, ValidationState state,
		IProgressMonitor monitor)
	{
		if (resource.getType() != IResource.FILE)
			return null;
		ValidationResult result = new ValidationResult();
		final IReporter reporter = result.getReporter(monitor);
		validateFile((IFile)resource, reporter);
		return result;
	}

	/**
	 * @param file
	 * @param reporter
	 */
	private void validateFile(IFile file, IReporter reporter)
	{
		IStructuredModel model = null;
		try
		{
			file.deleteMarkers(MARKER_ID, false, IResource.DEPTH_ZERO);
			model = StructuredModelManager.getModelManager().getModelForRead(file);
			IDOMModel domModel = (IDOMModel)model;
			IDOMDocument domDoc = domModel.getDocument();
			NodeList nodes = domDoc.getChildNodes();
			for (int k = 0; k < nodes.getLength(); k++)
			{
				Node child = nodes.item(k);
				if (child instanceof IDOMElement)
				{
					validateElement((IDOMElement)child, file, domDoc.getStructuredDocument(),
						reporter);
				}
			}
		}
		catch (Exception e)
		{
			Activator.log(Status.WARNING, "Error occurred during validation.", e);
		}
		finally
		{
			if (model != null)
			{
				model.releaseFromRead();
			}
		}
	}

	private void validateElement(IDOMElement element, IFile file, IStructuredDocument doc,
		IReporter reporter) throws JavaModelException
	{
		if (element == null)
			return;

		NamedNodeMap attrs = element.getAttributes();
		for (int i = 0; i < attrs.getLength(); i++)
		{
			IDOMAttr attr = (IDOMAttr)attrs.item(i);
			if ("beanclass".equals(attr.getName()))
			{
				String className = attr.getValue().trim();
				// Ignore EL expression.
				if (className.startsWith("$"))
					continue;

				IJavaProject project = JavaCore.create(file.getProject());
				IType type = project.findType(className);
				if (type == null || !type.exists())
				{
					try
					{
						int start = attr.getValueRegionStartOffset();
						int length = attr.getValueRegionText().length();
						int lineNo = doc.getLineOfOffset(start);
						IMarker marker = file.createMarker(MARKER_ID);
						marker.setAttribute(IMarker.SEVERITY, IMarker.SEVERITY_ERROR);
						marker.setAttribute(IMarker.PRIORITY, IMarker.PRIORITY_HIGH);
						marker.setAttribute(IMarker.MESSAGE, "ActionBean not found.");
						marker.setAttribute(IMarker.LINE_NUMBER, lineNo);
						if (start != 0)
						{
							marker.setAttribute(IMarker.CHAR_START, start);
							marker.setAttribute(IMarker.CHAR_END, start + length);
						}
						// Adds custom attributes.
						marker.setAttribute("problemType", MISSING_ACTION_BEAN);
						marker.setAttribute("errorValue", attr.getValue());
					}
					catch (CoreException e)
					{
						Activator.log(Status.ERROR, "Failed to create a custom marker.", e);
					}
				}
			}
		}

		NodeList nodes = element.getChildNodes();
		for (int j = 0; j < nodes.getLength(); j++)
		{
			Node child = nodes.item(j);
			if (child instanceof IDOMElement)
			{
				validateElement((IDOMElement)child, file, doc, reporter);
			}
		}
	}

}
