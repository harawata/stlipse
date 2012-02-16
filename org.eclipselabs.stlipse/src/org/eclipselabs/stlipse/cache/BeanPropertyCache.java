/*-
 * Copyright (C) 2011-2012 by Iwao AVE!
 * This program is made available under the terms of the MIT License.
 */

package org.eclipselabs.stlipse.cache;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
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

	public static void clearBeanPropertyCache(IProject project)
	{
		projectCache.remove(project);
	}

	public static void clearBeanPropertyCache(IProject project, String qualifiedName)
	{
		Map<String, BeanPropertyInfo> beans = projectCache.get(project);
		if (beans != null)
		{
			beans.remove(qualifiedName);
		}
	}

	public static BeanPropertyInfo getBeanPropertyInfo(IJavaProject project, String qualifiedName)
	{
		return getBeanPropertyInfo(project, qualifiedName, null);
	}

	public static BeanPropertyInfo getBeanPropertyInfo(IJavaProject project,
		String qualifiedName, ICompilationUnit compilationUnit)
	{
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
			if (beanProps != null)
			{
				return beanProps;
			}
		}

		if (compilationUnit == null)
		{
			try
			{
				final IType type = project.findType(qualifiedName);
				if (type != null)
				{
					compilationUnit = (ICompilationUnit)type.getAncestor(IJavaElement.COMPILATION_UNIT);
				}
			}
			catch (JavaModelException e)
			{
				Activator.log(Status.ERROR, "Failed to find type " + qualifiedName, e);
			}
		}

		if (compilationUnit != null)
		{
			Map<String, String> readableFields = new LinkedHashMap<String, String>();
			Map<String, String> writableFields = new LinkedHashMap<String, String>();
			Map<String, Boolean> eventHandlers = new LinkedHashMap<String, Boolean>();

			ASTParser parser = ASTParser.newParser(AST.JLS4);
			parser.setKind(ASTParser.K_COMPILATION_UNIT);
			parser.setSource(compilationUnit);
			parser.setResolveBindings(true);
			// parser.setIgnoreMethodBodies(true);
			CompilationUnit astUnit = (CompilationUnit)parser.createAST(null);
			astUnit.accept(new BeanPropertyVisitor(project, readableFields, writableFields,
				eventHandlers));

			beanProps = new BeanPropertyInfo(readableFields, writableFields, eventHandlers);
			beans.put(qualifiedName, beanProps);

			return beanProps;
		}
		return null;
	}

	public static List<String> searchEventHandler(IJavaProject project, String qualifiedName,
		String matchStr, boolean isValidation)
	{
		final List<String> results = new ArrayList<String>();
		final BeanPropertyInfo beanProperty = getBeanPropertyInfo(project, qualifiedName, null);
		if (beanProperty != null)
		{
			for (Entry<String, Boolean> eventHandler : beanProperty.getEventHandlers().entrySet())
			{
				String handlerName = eventHandler.getKey();
				boolean isDefaultHandler = Boolean.TRUE.equals(eventHandler.getValue());
				if (handlerName.startsWith(matchStr) || (isValidation && isDefaultHandler))
				{
					results.add(handlerName);
				}
			}
		}
		return results;
	}

	public static Map<String, String> searchFields(IJavaProject project, String qualifiedName,
		String matchStr, boolean includeReadOnly, int currentIdx, boolean isValidation,
		ICompilationUnit unit)
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

		final BeanPropertyInfo beanProperty = getBeanPropertyInfo(project, qualifiedName, unit);
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
							isValidation, null);
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
