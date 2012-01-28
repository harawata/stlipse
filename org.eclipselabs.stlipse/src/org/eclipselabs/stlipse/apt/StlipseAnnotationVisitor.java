/*-
 * Copyright (C) 2011 by Iwao AVE!
 * This program is made available under the terms of the MIT License.
 */

package org.eclipselabs.stlipse.apt;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipselabs.stlipse.Activator;
import org.eclipselabs.stlipse.cache.BeanPropertyCache;
import org.eclipselabs.stlipse.cache.BeanPropertyVisitor;

import com.sun.mirror.apt.Messager;
import com.sun.mirror.declaration.AnnotationMirror;
import com.sun.mirror.declaration.AnnotationTypeElementDeclaration;
import com.sun.mirror.declaration.AnnotationValue;
import com.sun.mirror.declaration.ClassDeclaration;
import com.sun.mirror.declaration.Declaration;
import com.sun.mirror.declaration.FieldDeclaration;
import com.sun.mirror.declaration.MethodDeclaration;
import com.sun.mirror.util.SimpleDeclarationVisitor;

/**
 * @author Iwao AVE!
 */
public class StlipseAnnotationVisitor extends SimpleDeclarationVisitor
{
	private IJavaProject project;

	private String qualifiedName;

	private Messager messager;

	public StlipseAnnotationVisitor(IJavaProject project, String qualifiedName, Messager messager)
	{
		super();
		this.project = project;
		this.qualifiedName = qualifiedName;
		this.messager = messager;
	}

	@Override
	public void visitClassDeclaration(ClassDeclaration d)
	{
		validateStrictBinding(d);
	}

	@Override
	public void visitFieldDeclaration(FieldDeclaration d)
	{
		String propertyName = d.getSimpleName();
		validateValidateField(d, propertyName);
	}

	@Override
	public void visitMethodDeclaration(MethodDeclaration d)
	{
		String propertyName = BeanPropertyVisitor.getFieldNameFromAccessor(d.getSimpleName());
		validateValidateField(d, propertyName);
	}

	private void validateStrictBinding(ClassDeclaration d)
	{
		Collection<AnnotationMirror> mirrors = d.getAnnotationMirrors();
		for (AnnotationMirror mirror : mirrors)
		{
			try
			{
				Map<AnnotationTypeElementDeclaration, AnnotationValue> valueMap = mirror.getElementValues();
				Set<Entry<AnnotationTypeElementDeclaration, AnnotationValue>> valueSet = valueMap.entrySet();
				for (Entry<AnnotationTypeElementDeclaration, AnnotationValue> valueEntry : valueSet)
				{
					String optionName = valueEntry.getKey().getSimpleName();
					if ("allow".equals(optionName) || "deny".equals(optionName))
					{
						@SuppressWarnings("unchecked")
						List<AnnotationValue> properties = (List<AnnotationValue>)valueEntry.getValue()
							.getValue();
						for (AnnotationValue property : properties)
						{
							Map<String, String> matched = BeanPropertyCache.searchFields(project, qualifiedName,
								property.getValue().toString(), false, -1, true, null);
							if (matched.size() == 0)
							{
								messager.printError(property.getPosition(), "No writable property found: "
									+ property.getValue().toString());
							}
						}
					}
				}
			}
			catch (IllegalStateException e)
			{
				// This seems to happen when the annotation value is missing or incomplete.
				// Might be an Eclipse bug.
				Activator.log(Status.INFO, "Exception thrown while processing @StrictBinding", e);
			}
		}
	}

	private void validateValidateField(Declaration d, String propertyName)
	{
		Collection<AnnotationMirror> mirrors = d.getAnnotationMirrors();
		for (AnnotationMirror mirror1 : mirrors)
		{
			Map<AnnotationTypeElementDeclaration, AnnotationValue> valueMap = mirror1.getElementValues();
			Set<Entry<AnnotationTypeElementDeclaration, AnnotationValue>> valueSet = valueMap.entrySet();
			for (Entry<AnnotationTypeElementDeclaration, AnnotationValue> nestedOpts : valueSet)
			{
				// Though @ValidateNestedProperties has only one property
				// that is a list of @Validate, this returns String
				// when the annotation value is missing or incomplete.
				Object values = nestedOpts.getValue().getValue();
				if (values instanceof List<?>)
				{
					@SuppressWarnings("unchecked")
					List<AnnotationValue> validateAnnotations = (List<AnnotationValue>)values;
					for (AnnotationValue validate : validateAnnotations)
					{
						AnnotationMirror mirror2 = (AnnotationMirror)validate.getValue();
						Map<AnnotationTypeElementDeclaration, AnnotationValue> validateOpts = mirror2.getElementValues();
						for (Entry<AnnotationTypeElementDeclaration, AnnotationValue> validateOpt : validateOpts.entrySet())
						{
							if ("field".equals(validateOpt.getKey().getSimpleName()))
							{
								StringBuilder searchStr = new StringBuilder(propertyName);
								AnnotationValue annotationValue = validateOpt.getValue();
								searchStr.append('.').append(annotationValue);
								Map<String, String> matched = BeanPropertyCache.searchFields(project, qualifiedName,
									searchStr.toString(), false, -1, true, null);
								if (matched.size() == 0)
								{
									messager.printError(annotationValue.getPosition(),
										"No writable property found: " + searchStr.toString());
								}
							}
						}
					}
				}
			}
		}
	}

}
