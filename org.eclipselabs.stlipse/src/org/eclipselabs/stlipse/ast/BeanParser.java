/*-
 * Copyright (C) 2011 by Iwao AVE!
 * This program is made available under the terms of the MIT License.
 */

package org.eclipselabs.stlipse.ast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipselabs.stlipse.Activator;

/**
 * @author Iwao AVE!
 */
public class BeanParser
{
	public static Map<String, ITypeBinding> searchFields(IJavaProject project,
		String qualifiedName, String matchStr, boolean includeReadOnly, int currentIdx,
		boolean isValidation)
	{
		try
		{
			IType type = project.findType(qualifiedName);
			ICompilationUnit unit = (ICompilationUnit)type.getAncestor(IJavaElement.COMPILATION_UNIT);
			return searchFields(project, unit, matchStr, includeReadOnly, currentIdx,
				isValidation);
		}
		catch (JavaModelException e)
		{
			Activator.log(Status.ERROR, "Failed to find type " + qualifiedName, e);
		}
		return Collections.emptyMap();
	}

	public static Map<String, ITypeBinding> searchFields(IJavaProject project,
		ICompilationUnit unit, String matchStr, boolean includeReadOnly, int currentIdx,
		boolean isValidation)
	{
		String searchStr;
		int startIdx = currentIdx + 1;
		int dotIdx = getDotIndex(matchStr, startIdx);
		if (dotIdx == -1)
			searchStr = matchStr.substring(startIdx);
		else
			searchStr = matchStr.substring(startIdx, dotIdx);

		Map<String, ITypeBinding> fieldMap = new LinkedHashMap<String, ITypeBinding>();
		try
		{
			if (unit != null && unit.isStructureKnown())
			{
				ASTParser parser = ASTParser.newParser(AST.JLS4);
				parser.setKind(ASTParser.K_COMPILATION_UNIT);
				parser.setSource(unit);
				parser.setResolveBindings(true);
				CompilationUnit astUnit = (CompilationUnit)parser.createAST(null);
				astUnit.accept(new BeanPropertyVisitor(project, fieldMap, searchStr,
					!isValidation && dotIdx == -1, includeReadOnly));
				if (dotIdx > -1 && fieldMap.size() > 0)
				{
					ITypeBinding binding = fieldMap.values().iterator().next();
					if (binding.isParameterizedType())
					{
						ITypeBinding[] arguments = binding.getTypeArguments();
						return searchFields(project, arguments[0].getQualifiedName(), matchStr,
							includeReadOnly, dotIdx, isValidation);
					}
					else
						return searchFields(project, binding.getQualifiedName(), matchStr,
							includeReadOnly, dotIdx, isValidation);
				}
			}
		}
		catch (JavaModelException e)
		{
			Activator.log(Status.ERROR, "Error while parsing " + unit.getElementName(), e);
		}
		return fieldMap;
	}

	public static List<ICompletionProposal> buildFieldNameProposal(
		Map<String, ITypeBinding> fields, final String input, final int offset,
		final int replacementLength)
	{
		int lastDot = input.lastIndexOf(".");
		String prefix = lastDot > -1 ? input.substring(0, lastDot) : "";
		List<ICompletionProposal> proposalList = new ArrayList<ICompletionProposal>();
		int relevance = fields.size();
		for (Entry<String, ITypeBinding> fieldEntry : fields.entrySet())
		{
			String fieldName = fieldEntry.getKey();
			ITypeBinding fieldType = fieldEntry.getValue();
			StringBuilder replaceStr = new StringBuilder();
			if (lastDot > -1)
				replaceStr.append(prefix).append('.');
			replaceStr.append(fieldName);
			StringBuilder displayStr = new StringBuilder();
			displayStr.append(fieldName).append(" - ").append(fieldType.getQualifiedName());
			ICompletionProposal proposal = new JavaCompletionProposal(replaceStr.toString(),
				offset, replacementLength, replaceStr.length(), Activator.getIcon(),
				displayStr.toString(), null, null, relevance--);
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
}
