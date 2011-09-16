/*-
 * Copyright (C) 2011 by Iwao AVE!
 * This program is made available under the terms of the MIT License.
 */

package org.eclipselabs.stlipse.validation;

import java.util.Iterator;

import org.eclipse.core.runtime.Status;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.quickassist.IQuickAssistInvocationContext;
import org.eclipse.jface.text.quickassist.IQuickAssistProcessor;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.IAnnotationAccessExtension2;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.jface.text.source.IAnnotationModelExtension2;
import org.eclipse.swt.graphics.Point;
import org.eclipselabs.stlipse.Activator;

public class JspQuickAssistProcessor implements IQuickAssistProcessor
{

	public JspQuickAssistProcessor()
	{
	}

	public String getErrorMessage()
	{
		return null;
	}

	public boolean canFix(Annotation annotation)
	{
		return true;
	}

	public boolean canAssist(IQuickAssistInvocationContext invocationContext)
	{
		return true;
	}

	public ICompletionProposal[] computeQuickAssistProposals(
		IQuickAssistInvocationContext invocationContext)
	{
		ICompletionProposal[] proposals = null;
		Point selectedRange = invocationContext.getSourceViewer().getSelectedRange();
		int offset = selectedRange.x;
		int length = selectedRange.y;
		String fqn = null;

		try
		{
			fqn = getMissingFullyQualifiedName(invocationContext, offset, length);
		}
		catch (BadLocationException e)
		{
			Activator.log(Status.ERROR, "Failed to genarate completion proposal.", e);
		}

		if (fqn != null)
		{
			proposals = new ICompletionProposal[]{
				new NewBeanclassProposal(fqn)
			};
		}

		return proposals;
	}

	private String getMissingFullyQualifiedName(
		IQuickAssistInvocationContext invocationContext, int offset, int length)
		throws BadLocationException
	{
		IAnnotationModel annotationModel = invocationContext.getSourceViewer()
			.getAnnotationModel();
		if (annotationModel == null)
			return null;

		Iterator iterator;
		if (annotationModel instanceof IAnnotationAccessExtension2)
		{
			iterator = ((IAnnotationModelExtension2)annotationModel).getAnnotationIterator(
				offset, length, true, true);
		}
		else
		{
			iterator = annotationModel.getAnnotationIterator();
		}
		while (iterator.hasNext())
		{
			Annotation annotation = (Annotation)iterator.next();
			Position position = annotationModel.getPosition(annotation);
			if (position != null && position.overlapsWith(offset, length)
				&& JspValidator.MARKER_ID.equals(annotation.getType()))
			{
				String value = invocationContext.getSourceViewer()
					.getDocument()
					.get(position.offset, position.length);
				char firstChar = value.charAt(0);
				return firstChar == '\'' || firstChar == '"' ? value.substring(1,
					value.length() - 1) : value;
			}
		}
		return null;
	}
}
