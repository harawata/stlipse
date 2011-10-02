/*-
 * Copyright (C) 2011 by Iwao AVE!
 * This program is made available under the terms of the MIT License.
 */

package org.eclipselabs.stlipse.ast;

import java.util.List;
import java.util.Map;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.PrimitiveType;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;

/**
 * @author Iwao AVE!
 */
public class BeanPropertyVisitor extends ASTVisitor
{
	private IJavaProject project;

	private String searchStr;

	private boolean prefixMatch;

	private Map<String, ITypeBinding> fields;

	public BeanPropertyVisitor(
		IJavaProject project,
		Map<String, ITypeBinding> fields,
		String searchStr,
		boolean prefixMatch)
	{
		super();
		this.project = project;
		this.fields = fields;
		int bracePos = searchStr.indexOf("[");
		this.searchStr = bracePos > -1 ? searchStr.substring(0, bracePos) : searchStr;
		this.prefixMatch = prefixMatch;
	}

	@Override
	public boolean visit(FieldDeclaration node)
	{
		int modifiers = node.getModifiers();
		if (Modifier.isPublic(modifiers) && !Modifier.isFinal(modifiers))
		{
			// public field
			@SuppressWarnings("unchecked")
			List<VariableDeclarationFragment> fragments = node.fragments();
			for (VariableDeclarationFragment fragment : fragments)
			{
				String fieldName = fragment.getName().toString();
				if (matched(fieldName))
				{
					addType(fields, fieldName, node.getType());
				}
			}
		}
		return false;
	}

	@Override
	public boolean visit(MethodDeclaration node)
	{
		if (Modifier.isPublic(node.getModifiers()))
		{
			String methodName = node.getName().toString();
			Type type = node.getReturnType2();
			if (methodName.startsWith("set") && methodName.length() > 4
				&& type.isPrimitiveType()
				&& PrimitiveType.VOID.equals(((PrimitiveType)type).getPrimitiveTypeCode())
				&& node.parameters().size() == 1)
			{
				// setter
				String fieldName = getFieldNameFromSetter(methodName);
				if (matched(fieldName))
				{
					SingleVariableDeclaration param = (SingleVariableDeclaration)node.parameters()
						.get(0);
					addType(fields, fieldName, param.getType());
				}
			}
		}
		return false;
	}

	@Override
	public void endVisit(TypeDeclaration node)
	{
		// Avoid useless scan.
		if (prefixMatch || fields.size() == 0)
		{
			try
			{
				Type superclassType = node.getSuperclassType();
				if (superclassType != null)
				{
					ITypeBinding binding = superclassType.resolveBinding();
					IType type;
					type = project.findType(binding.getQualifiedName());
					ICompilationUnit unit = (ICompilationUnit)type.getAncestor(IJavaElement.COMPILATION_UNIT);
					if (unit != null && unit.isStructureKnown())
					{
						ASTParser parser = ASTParser.newParser(AST.JLS4);
						parser.setKind(ASTParser.K_COMPILATION_UNIT);
						parser.setSource(unit);
						parser.setResolveBindings(true);
						CompilationUnit astUnit = (CompilationUnit)parser.createAST(null);
						astUnit.accept(new BeanPropertyVisitor(project, fields, searchStr,
							prefixMatch));
					}
				}
			}
			catch (JavaModelException e)
			{
				e.printStackTrace();
			}
		}
	}

	private String getFieldNameFromSetter(String methodName)
	{
		StringBuilder sb = new StringBuilder();
		sb.append(Character.toLowerCase(methodName.charAt(3)));
		if (methodName.length() > 4)
			sb.append(methodName.substring(4));
		return sb.toString();
	}

	private boolean matched(String fieldName)
	{
		return (searchStr == null || searchStr.length() == 0)
			|| (prefixMatch ? fieldName.toLowerCase().startsWith(searchStr.toLowerCase())
				: fieldName.equals(searchStr));
	}

	private void addType(Map<String, ITypeBinding> fields, String name, Type type)
	{
		if (!fields.containsKey(name))
		{
			ITypeBinding binding = type.resolveBinding();
			fields.put(name, binding);
		}
	}
}
