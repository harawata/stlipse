/*-
 * Copyright (C) 2011 by Iwao AVE!
 * This program is made available under the terms of the MIT License.
 */

package org.eclipselabs.stlipse.ast;

import java.util.LinkedHashMap;
import java.util.Map;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ITypeBinding;

/**
 * @author Iwao AVE!
 */
public class BeanParser
{
	public static Map<String, ITypeBinding> searchFields(IJavaProject project,
		String qualifiedName, String matchStr, int currentIdx) throws JavaModelException
	{
		String searchStr;
		int startIdx = currentIdx + 1;
		int dotIdx = getDotIndex(matchStr, startIdx);
		if (dotIdx == -1)
		{
			searchStr = matchStr.substring(startIdx);
		}
		else
		{
			searchStr = matchStr.substring(startIdx, dotIdx);
		}
		Map<String, ITypeBinding> fieldMap = new LinkedHashMap<String, ITypeBinding>();
		IType type = project.findType(qualifiedName);
		ICompilationUnit unit = (ICompilationUnit)type.getAncestor(IJavaElement.COMPILATION_UNIT);
		if (unit != null && unit.isStructureKnown())
		{
			ASTParser parser = ASTParser.newParser(AST.JLS4);
			parser.setKind(ASTParser.K_COMPILATION_UNIT);
			parser.setSource(unit);
			parser.setResolveBindings(true);
			CompilationUnit astUnit = (CompilationUnit)parser.createAST(null);
			astUnit.accept(new BeanPropertyVisitor(project, fieldMap, searchStr, dotIdx == -1));
			if (dotIdx > -1)
			{
				ITypeBinding binding = fieldMap.values().iterator().next();
				if (binding.isParameterizedType())
				{
					ITypeBinding[] arguments = binding.getTypeArguments();
					return searchFields(project, arguments[0].getQualifiedName(), matchStr,
						dotIdx);
				}
				else
					return searchFields(project, binding.getQualifiedName(), matchStr, dotIdx);
			}
		}
		return fieldMap;
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
