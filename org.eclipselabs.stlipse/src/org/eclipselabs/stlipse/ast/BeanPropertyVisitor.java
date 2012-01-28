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

	private final Map<String, String> readableFields;

	private final Map<String, String> writableFields;

	public BeanPropertyVisitor(
		IJavaProject project,
		Map<String, String> readableFields,
		Map<String, String> writableFields)
	{
		super();
		this.project = project;
		this.readableFields = readableFields;
		this.writableFields = writableFields;
	}

	@Override
	public boolean visit(FieldDeclaration node)
	{
		int modifiers = node.getModifiers();
		if (Modifier.isPublic(modifiers))
		{
			@SuppressWarnings("unchecked")
			List<VariableDeclarationFragment> fragments = node.fragments();
			for (VariableDeclarationFragment fragment : fragments)
			{
				String fieldName = fragment.getName().toString();
				String qualifiedName = getQualifiedNameFromType(node.getType());
				if (Modifier.isFinal(modifiers))
					readableFields.put(fieldName, qualifiedName);
				else
					writableFields.put(fieldName, qualifiedName);
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
			String fieldName = getFieldNameFromAccessor(methodName);
			String qualifiedName = null;
			if (isGetter(node))
			{
				qualifiedName = getQualifiedNameFromType(node.getReturnType2());
				readableFields.put(fieldName, qualifiedName);
			}
			else if (isSetter(node))
			{
				SingleVariableDeclaration param = (SingleVariableDeclaration)node.parameters().get(0);
				qualifiedName = getQualifiedNameFromType(param.getType());
				writableFields.put(fieldName, qualifiedName);
			}
		}
		return false;
	}

	private String getQualifiedNameFromType(Type type)
	{
		String qualifiedName = null;
		ITypeBinding binding = type.resolveBinding();
		if (binding.isParameterizedType())
		{
			ITypeBinding[] arguments = binding.getTypeArguments();
			// Assuming collection.
			// TODO: map?
			qualifiedName = arguments[0].getQualifiedName();
		}
		else
		{
			qualifiedName = binding.getQualifiedName();
		}
		return qualifiedName;
	}

	private boolean isGetter(MethodDeclaration node)
	{
		String methodName = node.getName().toString();
		return methodName.startsWith("get") && methodName.length() > 3 && !isReturnVoid(node)
			&& node.parameters().size() == 0;
	}

	private boolean isSetter(MethodDeclaration node)
	{
		String methodName = node.getName().toString();
		return methodName.startsWith("set") && methodName.length() > 3 && isReturnVoid(node)
			&& node.parameters().size() == 1;
	}

	private boolean isReturnVoid(MethodDeclaration node)
	{
		Type type = node.getReturnType2();
		return type.isPrimitiveType()
			&& PrimitiveType.VOID.equals(((PrimitiveType)type).getPrimitiveTypeCode());
	}

	@Override
	public void endVisit(TypeDeclaration node)
	{
		try
		{
			Type superclassType = node.getSuperclassType();
			if (superclassType != null)
			{
				ITypeBinding binding = superclassType.resolveBinding();
				IType type = project.findType(binding.getQualifiedName());
				ICompilationUnit unit = (ICompilationUnit)type.getAncestor(IJavaElement.COMPILATION_UNIT);
				if (unit != null && unit.isStructureKnown())
				{
					ASTParser parser = ASTParser.newParser(AST.JLS4);
					parser.setKind(ASTParser.K_COMPILATION_UNIT);
					parser.setSource(unit);
					parser.setResolveBindings(true);
					CompilationUnit astUnit = (CompilationUnit)parser.createAST(null);
					astUnit.accept(new BeanPropertyVisitor(project, readableFields, writableFields));
				}
			}
		}
		catch (JavaModelException e)
		{
			e.printStackTrace();
		}
	}

	public static String getFieldNameFromAccessor(String methodName)
	{
		if (methodName == null || methodName.length() < 4)
			return "";
		StringBuilder sb = new StringBuilder();
		sb.append(Character.toLowerCase(methodName.charAt(3)));
		if (methodName.length() > 4)
			sb.append(methodName.substring(4));
		return sb.toString();
	}
}
