/*-
 * Copyright (C) 2011 by Iwao AVE!
 * This program is made available under the terms of the MIT License.
 */

package org.eclipselabs.stlipse.javaeditor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.CompletionContext;
import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.IAnnotatable;
import org.eclipse.jdt.core.IAnnotation;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMemberValuePair;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.ui.text.java.ContentAssistInvocationContext;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposalComputer;
import org.eclipse.jdt.ui.text.java.JavaContentAssistInvocationContext;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipselabs.stlipse.Activator;
import org.eclipselabs.stlipse.cache.BeanPropertyCache;
import org.eclipselabs.stlipse.cache.BeanPropertyVisitor;

/**
 * @author Iwao AVE!
 */
public class JavaCompletionProposalComputer implements IJavaCompletionProposalComputer
{

	public void sessionStarted()
	{
		// Nothing todo for now.
	}

	public List<ICompletionProposal> computeCompletionProposals(
		ContentAssistInvocationContext context, IProgressMonitor monitor)
	{
		List<ICompletionProposal> proposals = new ArrayList<ICompletionProposal>();
		if (context instanceof JavaContentAssistInvocationContext)
		{
			JavaContentAssistInvocationContext javaContext = (JavaContentAssistInvocationContext)context;
			CompletionContext coreContext = javaContext.getCoreContext();
			ICompilationUnit unit = javaContext.getCompilationUnit();
			try
			{
				if (unit != null && unit.isStructureKnown())
				{
					int offset = javaContext.getInvocationOffset();
					IJavaElement element = unit.getElementAt(offset);
					if (element != null && element instanceof IAnnotatable)
					{
						if (element.getElementType() == IJavaElement.TYPE)
						{
							// search @StrictBinding
							int replacementLength = getAllowDenyValueLength(offset, (IAnnotatable)element);
							if (replacementLength > -1)
							{
								String input = String.valueOf(coreContext.getToken());
								Map<String, String> fields = BeanPropertyCache.searchFields(javaContext.getProject(),
									unit.getType(element.getParent().getElementName()).getFullyQualifiedName(),
									input,
									false,
									-1, false, unit);
								proposals.addAll(BeanPropertyCache.buildFieldNameProposal(fields, input,
									coreContext.getTokenStart() + 1, replacementLength));
							}
						}
						else
						{
							// search @Validate
							StringBuilder matchStr = resolveBeanPropertyName(element);
							if (matchStr.length() > 0)
							{
								int replacementLength = getFieldValueLength(offset, (IAnnotatable)element);
								if (replacementLength > -1)
								{
									char[] token = coreContext.getToken();
									matchStr.append('.').append(token);
									Map<String, String> fields = BeanPropertyCache.searchFields(
										javaContext.getProject(),
										unit.getType(element.getParent().getElementName())
											.getFullyQualifiedName(), matchStr.toString(), false, -1, false, unit);
									proposals.addAll(BeanPropertyCache.buildFieldNameProposal(fields,
										String.valueOf(token), coreContext.getTokenStart() + 1, replacementLength));
								}
							}
						}
					}
				}
			}
			catch (JavaModelException e)
			{
				Activator.log(Status.ERROR, "Something went wrong.", e);
			}
		}
		return proposals;
	}

	private StringBuilder resolveBeanPropertyName(IJavaElement element) throws JavaModelException
	{
		StringBuilder result = new StringBuilder();
		int elementType = element.getElementType();
		String elementName = element.getElementName();
		if (elementType == IJavaElement.FIELD)
		{
			result.append(elementName);
		}
		else if (elementType == IJavaElement.METHOD && elementName.startsWith("set")
			&& elementName.length() > 3)
		{
			IMethod method = (IMethod)element;
			if (Flags.isPublic(method.getFlags()) && "void".equals(method.getReturnType()))
			{
				result.append(BeanPropertyVisitor.getFieldNameFromAccessor(elementName));
			}
		}
		return result;
	}

	private int getAllowDenyValueLength(int offset, IAnnotatable annotatable)
		throws JavaModelException
	{
		IAnnotation[] annotations = annotatable.getAnnotations();
		for (IAnnotation annotation : annotations)
		{
			int len = getAnnotationValueLength(offset, annotation, "StrictBinding", "allow", "deny");
			if (len > -1)
				return len;
		}
		return -1;
	}

	private int getFieldValueLength(int offset, IAnnotatable annotatable)
		throws JavaModelException
	{
		IAnnotation[] annotations = annotatable.getAnnotations();
		for (IAnnotation annotation : annotations)
		{
			if ("ValidateNestedProperties".equals(annotation.getElementName())
				&& isInRange(annotation.getSourceRange(), offset))
			{
				IMemberValuePair[] valuePairs = annotation.getMemberValuePairs();
				for (IMemberValuePair valuePair : valuePairs)
				{
					if ("value".equals(valuePair.getMemberName())
						&& valuePair.getValueKind() == IMemberValuePair.K_ANNOTATION)
					{
						// the value is an array of IAnnotation
						Object[] items = (Object[])valuePair.getValue();
						for (Object item : items)
						{
							int len = getAnnotationValueLength(offset, (IAnnotation)item, "Validate", "field");
							if (len > -1)
								return len;
						}
					}
				}
			}
		}
		return -1;
	}

	private int getAnnotationValueLength(int offset, IAnnotation annotation,
		String annotationName, String... annotationAttributeNames) throws JavaModelException
	{
		ISourceRange sourceRange = annotation.getSourceRange();
		if (annotationName.equals(annotation.getElementName()) && isInRange(sourceRange, offset))
		{
			int annotationOffset = sourceRange.getOffset();
			String source = annotation.getSource();
			int index = source.indexOf('(', 9);
			if (index > -1)
			{
				int stringValueLength = 0;
				boolean scanningName = true;
				boolean scanningValue = false;
				boolean inStringValue = false;
				boolean inArrayValue = false;
				boolean escaped = false;
				StringBuilder optionName = new StringBuilder();
				for (index++; index < source.length(); index++)
				{
					char c = source.charAt(index);
					if (scanningName)
					{
						if (c == '=')
							scanningName = false;
						else if (c != ' ')
							optionName.append(c);
					}
					else if (!scanningValue)
					{
						if (c != ' ')
						{
							scanningValue = true;
							if (c == '"')
							{
								inStringValue = true;
								stringValueLength = 0;
							}
							else if (c == '{')
								inArrayValue = true;
						}
					}
					else
					{
						// scanning value part
						if (inStringValue)
						{
							if (escaped)
								; // ignore the escaped character
							else if (c == '\\')
								escaped = true;
							else if (c == '"')
							{
								if (annotationOffset + index >= offset)
									break;
								inStringValue = false;
								stringValueLength = 0;
							}
							if (inStringValue)
								stringValueLength++;
						}
						else if (inArrayValue)
						{
							if (c == '"')
								inStringValue = true;
							else if (c == '}')
								inArrayValue = false;
							else
								; // ignore
						}
						else if (c == ',')
						{
							scanningValue = false;
							scanningName = true;
							optionName.setLength(0);
						}
					}
				}
				for (String attributeName : annotationAttributeNames)
				{
					if (attributeName.equals(optionName.toString()))
						return stringValueLength;
				}
			}
		}
		return -1;
	}

	private boolean isInRange(ISourceRange sourceRange, int offset)
	{
		int start = sourceRange.getOffset();
		int end = start + sourceRange.getLength();
		return start <= offset && offset <= end;
	}

	public List<IContextInformation> computeContextInformation(
		ContentAssistInvocationContext context, IProgressMonitor monitor)
	{
		return null;
	}

	public String getErrorMessage()
	{
		return null;
	}

	public void sessionEnded()
	{
		// Nothing todo for now.
	}

}
