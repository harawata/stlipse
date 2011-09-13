/*-
 * Copyright (C) 2011 by Iwao AVE!
 * This program is made available under the terms of the MIT License.
 */

package org.eclipselabs.stlipse.validation;

import java.util.Locale;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
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
import org.eclipse.wst.validation.internal.core.Message;
import org.eclipse.wst.validation.internal.core.ValidationException;
import org.eclipse.wst.validation.internal.provisional.core.IMessage;
import org.eclipse.wst.validation.internal.provisional.core.IReporter;
import org.eclipse.wst.validation.internal.provisional.core.IValidationContext;
import org.eclipse.wst.validation.internal.provisional.core.IValidator;
import org.eclipse.wst.xml.core.internal.provisional.document.IDOMAttr;
import org.eclipse.wst.xml.core.internal.provisional.document.IDOMDocument;
import org.eclipse.wst.xml.core.internal.provisional.document.IDOMElement;
import org.eclipse.wst.xml.core.internal.provisional.document.IDOMModel;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class JspValidator extends AbstractValidator implements IValidator
{

	public void cleanup(IReporter reporter)
	{
		// Nothing to do.
	}

	public void validate(IValidationContext helper, IReporter reporter)
		throws ValidationException
	{
		String[] uris = helper.getURIs();
		reporter.removeAllMessages(this);

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
			e.printStackTrace();
		}
		finally
		{
			if (model != null)
			{
				model.releaseFromRead();
			}
		}
	}

	/**
	 * @param domElem
	 * @param file
	 * @param iStructuredDocument
	 * @param reporter
	 */
	private void validateElement(IDOMElement element, IFile file, IStructuredDocument doc,
		IReporter reporter)
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
				try
				{
					IJavaProject project = JavaCore.create(file.getProject());
					IType type = project.findType(className);
					if (type == null || !type.exists())
					{
						LocalizedMessage message = new LocalizedMessage(IMessage.HIGH_SEVERITY,
							"ActionBean not found.", file);
						int start = element.getStartOffset();
						int length = element.getStartEndOffset() - start;
						int lineNo = doc.getLineOfOffset(start);
						message.setLineNo(lineNo);
						message.setOffset(start);
						message.setLength(length);

						reporter.addMessage(this, message);
					}
				}
				catch (JavaModelException e)
				{
					e.printStackTrace();
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

	protected class LocalizedMessage extends Message
	{

		private String _message = null;

		public LocalizedMessage(int severity, String messageText)
		{
			this(severity, messageText, null);
		}

		public LocalizedMessage(int severity, String messageText, IResource targetObject)
		{
			this(severity, messageText, (Object)targetObject);
		}

		public LocalizedMessage(int severity, String messageText, Object targetObject)
		{
			super(null, severity, null);
			setLocalizedMessage(messageText);
			setTargetObject(targetObject);
		}

		public void setLocalizedMessage(String message)
		{
			_message = message;
		}

		public String getLocalizedMessage()
		{
			return _message;
		}

		public String getText()
		{
			return getLocalizedMessage();
		}

		public String getText(ClassLoader cl)
		{
			return getLocalizedMessage();
		}

		public String getText(Locale l)
		{
			return getLocalizedMessage();
		}

		public String getText(Locale l, ClassLoader cl)
		{
			return getLocalizedMessage();
		}
	}

}
