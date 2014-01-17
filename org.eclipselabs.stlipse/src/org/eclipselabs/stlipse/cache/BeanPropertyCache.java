/*-
 * Copyright (C) 2011-2014 by Iwao AVE!
 * This program is made available under the terms of the MIT License.
 */

package org.eclipselabs.stlipse.cache;

import static org.eclipselabs.stlipse.util.StripesClasses.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.IAnnotation;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipselabs.stlipse.Activator;
import org.eclipselabs.stlipse.javaeditor.JavaCompletionProposal;

/**
 * @author Iwao AVE!
 */
public class BeanPropertyCache
{
	private static final Map<IProject, Map<String, BeanPropertyInfo>> projectCache = new ConcurrentHashMap<IProject, Map<String, BeanPropertyInfo>>();

	public static void clearBeanPropertyCache()
	{
		projectCache.clear();
	}

	public static void clearBeanPropertyCache(IProject project)
	{
		projectCache.remove(project);
	}

	public static void clearBeanPropertyCache(IProject project, String qualifiedName)
	{
		Map<String, BeanPropertyInfo> beans = projectCache.get(project);
		if (beans != null)
		{
			beans.remove(removeExtension(qualifiedName));
		}
	}

	public static BeanPropertyInfo getBeanPropertyInfo(IJavaProject project, String fqn)
	{
		String qualifiedName = removeExtension(fqn);
		Map<String, BeanPropertyInfo> beans = projectCache.get(project.getProject());
		BeanPropertyInfo beanProps = null;
		if (beans == null)
		{
			beans = new ConcurrentHashMap<String, BeanPropertyInfo>();
			projectCache.put(project.getProject(), beans);
		}
		else
		{
			beanProps = beans.get(qualifiedName);
			if (beanProps == null)
			{
				final Map<String, String> readableFields = new LinkedHashMap<String, String>();
				final Map<String, String> writableFields = new LinkedHashMap<String, String>();
				final Map<String, EventProperty> eventHandlers = new LinkedHashMap<String, EventProperty>();
				parseBean(project, qualifiedName, readableFields, writableFields, eventHandlers);
				beanProps = new BeanPropertyInfo(readableFields, writableFields, eventHandlers);
			}
			beans.put(qualifiedName, beanProps);
			return beanProps;
		}
		return null;
	}

	protected static void parseBean(IJavaProject project, String qualifiedName,
		final Map<String, String> readableFields, final Map<String, String> writableFields,
		final Map<String, EventProperty> eventHandlers)
	{
		try
		{
			final IType type = project.findType(qualifiedName);
			if (type != null)
			{
				if (type.isBinary())
				{
					parseBinary(project, type, readableFields, writableFields, eventHandlers);
				}
				else
				{
					parseSource(project, type, readableFields, writableFields, eventHandlers);
				}
			}
		}
		catch (JavaModelException e)
		{
			Activator.log(Status.ERROR, "Failed to find type " + qualifiedName, e);
		}
	}

	protected static void parseBinary(IJavaProject project, final IType type,
		final Map<String, String> readableFields, final Map<String, String> writableFields,
		final Map<String, EventProperty> eventHandlers) throws JavaModelException
	{
		parseBinaryFields(type, readableFields, writableFields);

		parseBinaryMethods(type, readableFields, writableFields, eventHandlers);

		String superclass = Signature.toString(type.getSuperclassTypeSignature());
		if (!Object.class.getName().equals(superclass))
		{
			parseBean(project, superclass, readableFields, writableFields, eventHandlers);
		}
	}

	protected static void parseBinaryMethods(final IType type,
		final Map<String, String> readableFields, final Map<String, String> writableFields,
		final Map<String, EventProperty> eventHandlers) throws JavaModelException
	{
		for (IMethod method : type.getMethods())
		{
			int flags = method.getFlags();
			if (Flags.isPublic(flags))
			{
				final String methodName = method.getElementName();
				final int parameterCount = method.getParameters().length;
				final String returnType = method.getReturnType();
				if (Signature.C_VOID == returnType.charAt(0))
				{
					if (BeanPropertyVisitor.isSetter(methodName, parameterCount))
					{
						String fieldName = BeanPropertyVisitor.getFieldNameFromAccessor(methodName);
						String paramType = method.getParameterTypes()[0];
						writableFields.put(fieldName, Signature.toString(paramType));
					}
				}
				else
				{
					if (BeanPropertyVisitor.isGetter(methodName, parameterCount))
					{
						String fieldName = BeanPropertyVisitor.getFieldNameFromAccessor(methodName);
						readableFields.put(fieldName, Signature.toString(returnType));
					}
					else if (RESOLUTION.equals(Signature.toString(returnType))
						&& parameterCount == 0)
					{
						parseBinaryEventHandler(method, eventHandlers);
					}
				}
			}
		}
	}

	protected static void parseBinaryEventHandler(IMethod method,
		final Map<String, EventProperty> eventHandlers) throws JavaModelException
	{
		boolean isInterceptor = false;
		boolean isDefaultHandler = false;
		String annotationValue = null;
		for (IAnnotation annotation : method.getAnnotations())
		{
			String annotationType = annotation.getElementName();
			if (BEFORE.equals(annotationType) || AFTER.equals(annotationType))
			{
				isInterceptor = true;
				break;
			}
			isDefaultHandler |= DEFAULT_HANDLER.equals(annotationType);
			if (HANDLES_EVENT.equals(annotationType))
			{
				annotationValue = (String)annotation.getMemberValuePairs()[0].getValue();
			}
		}
		if (!isInterceptor)
		{
			String methodName = method.getElementName();
			EventProperty eventProperty = new EventProperty();
			eventProperty.setDefaultHandler(isDefaultHandler);
			eventProperty.setMethodName(methodName);
			eventHandlers.put(annotationValue == null ? methodName : annotationValue, eventProperty);
		}
	}

	protected static void parseBinaryFields(final IType type,
		final Map<String, String> readableFields, final Map<String, String> writableFields)
		throws JavaModelException
	{
		for (IField field : type.getFields())
		{
			int flags = field.getFlags();
			if (Flags.isPublic(flags))
			{
				String fieldName = field.getElementName();
				String qualifiedType = Signature.toString(field.getTypeSignature());
				readableFields.put(fieldName, qualifiedType);
				if (!Flags.isFinal(flags))
				{
					writableFields.put(fieldName, qualifiedType);
				}
			}
		}
	}

	protected static void parseSource(IJavaProject project, final IType type,
		final Map<String, String> readableFields, final Map<String, String> writableFields,
		final Map<String, EventProperty> eventHandlers) throws JavaModelException
	{
		ICompilationUnit compilationUnit = (ICompilationUnit)type.getAncestor(IJavaElement.COMPILATION_UNIT);
		ASTParser parser = ASTParser.newParser(AST.JLS4);
		parser.setKind(ASTParser.K_COMPILATION_UNIT);
		parser.setSource(compilationUnit);
		parser.setResolveBindings(true);
		// parser.setIgnoreMethodBodies(true);
		CompilationUnit astUnit = (CompilationUnit)parser.createAST(null);
		astUnit.accept(new BeanPropertyVisitor(project, readableFields, writableFields,
			eventHandlers));
	}

	public static List<String> searchEventHandler(IJavaProject project, String qualifiedName,
		String matchStr, boolean isValidation, boolean alwaysIncludeDefault)
	{
		final List<String> results = new ArrayList<String>();
		final BeanPropertyInfo beanProperty = getBeanPropertyInfo(project, qualifiedName);
		if (beanProperty != null)
		{
			for (Entry<String, EventProperty> eventHandler : beanProperty.getEventHandlers()
				.entrySet())
			{
				String handlerName = eventHandler.getKey();
				EventProperty eventProperty = eventHandler.getValue();
				if ((alwaysIncludeDefault && eventProperty.isDefaultHandler())
					|| (isValidation && handlerName.equals(matchStr)) || handlerName.startsWith(matchStr))
				{
					results.add(handlerName);
				}
			}
		}
		return results;
	}

	public static Map<String, EventProperty> getEventHandlers(IJavaProject project,
		String qualifiedName)
	{
		final BeanPropertyInfo beanProperty = getBeanPropertyInfo(project, qualifiedName);
		return beanProperty == null ? Collections.<String, EventProperty> emptyMap()
			: beanProperty.getEventHandlers();
	}

	public static Map<String, String> searchFields(IJavaProject project, String qualifiedName,
		String matchStr, boolean includeReadOnly, int currentIdx, boolean isValidation)
	{
		final Map<String, String> results = new LinkedHashMap<String, String>();
		String searchStr;
		final int startIdx = currentIdx + 1;
		final int dotIdx = getDotIndex(matchStr, startIdx);
		if (dotIdx == -1)
			searchStr = matchStr.substring(startIdx);
		else
			searchStr = matchStr.substring(startIdx, dotIdx);
		final int bracePos = searchStr.indexOf("[");
		searchStr = bracePos > -1 ? searchStr.substring(0, bracePos) : searchStr;
		final boolean isPrefixMatch = !isValidation && dotIdx == -1;

		final BeanPropertyInfo beanProperty = getBeanPropertyInfo(project, qualifiedName);
		if (beanProperty != null)
		{
			final Map<String, String> fields = includeReadOnly ? beanProperty.getReadableFields()
				: beanProperty.getWritableFields();

			for (Entry<String, String> entry : fields.entrySet())
			{
				final String fieldName = entry.getKey();
				final String fieldQualifiedName = entry.getValue();

				if (matched(fieldName, searchStr, isPrefixMatch))
				{
					if (dotIdx > -1)
					{
						return searchFields(project, fieldQualifiedName, matchStr, includeReadOnly, dotIdx,
							isValidation);
					}
					else
					{
						results.put(fieldName, fieldQualifiedName);
					}
				}
			}
		}
		return results;
	}

	public static List<ICompletionProposal> buildFieldNameProposal(Map<String, String> fields,
		final String input, final int offset, final int replacementLength)
	{
		List<ICompletionProposal> proposalList = new ArrayList<ICompletionProposal>();
		int lastDot = input.lastIndexOf(".");
		String prefix = lastDot > -1 ? input.substring(0, lastDot) : "";
		int relevance = fields.size();
		for (Entry<String, String> fieldEntry : fields.entrySet())
		{
			String fieldName = fieldEntry.getKey();
			String qualifiedName = fieldEntry.getValue();
			StringBuilder replaceStr = new StringBuilder();
			if (lastDot > -1)
				replaceStr.append(prefix).append('.');
			replaceStr.append(fieldName);
			StringBuilder displayStr = new StringBuilder();
			displayStr.append(fieldName).append(" - ").append(qualifiedName);
			ICompletionProposal proposal = new JavaCompletionProposal(replaceStr.toString(), offset,
				replacementLength, replaceStr.length(), Activator.getIcon(), displayStr.toString(),
				null, null, relevance--);
			proposalList.add(proposal);
		}
		return proposalList;
	}

	private static String removeExtension(String src)
	{
		if (src != null && src.endsWith(".java"))
			return src.substring(0, src.length() - 5);
		else
			return src;
	}

	private static int getDotIndex(String str, int startIdx)
	{
		boolean isIndexedProperty = false;
		for (int i = startIdx; i < str.length(); i++)
		{
			char c = str.charAt(i);
			if (!isIndexedProperty && c == '.')
				return i;
			else if (!isIndexedProperty && c == '[')
				isIndexedProperty = true;
			else if (c == ']')
				isIndexedProperty = false;
		}
		return -1;
	}

	private static boolean matched(String fieldName, String searchStr, boolean prefixMatch)
	{
		return (searchStr == null || searchStr.length() == 0)
			|| (prefixMatch ? fieldName.toLowerCase().startsWith(searchStr.toLowerCase())
				: fieldName.equals(searchStr));
	}

}
