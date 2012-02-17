/*-
 * Copyright (C) 2011-2012 by Iwao AVE!
 * This program is made available under the terms of the MIT License.
 */

package org.eclipselabs.stlipse.apt;

import java.util.Collection;

import org.eclipse.jdt.apt.core.env.EclipseAnnotationProcessorEnvironment;
import org.eclipse.jdt.core.IJavaProject;

import com.sun.mirror.apt.AnnotationProcessor;
import com.sun.mirror.apt.AnnotationProcessorEnvironment;
import com.sun.mirror.declaration.AnnotationTypeDeclaration;
import com.sun.mirror.declaration.Declaration;

/**
 * @author Iwao AVE!
 */
public class StlipseAnnotationProcessor implements AnnotationProcessor
{

	private AnnotationProcessorEnvironment environment;

	public StlipseAnnotationProcessor()
	{
		super();
	}

	public StlipseAnnotationProcessor(AnnotationProcessorEnvironment environment)
	{
		super();
		this.environment = environment;
	}

	public void process()
	{
		if (environment instanceof EclipseAnnotationProcessorEnvironment)
		{
			EclipseAnnotationProcessorEnvironment env = (EclipseAnnotationProcessorEnvironment)environment;
			IJavaProject javaProject = env.getJavaProject();
			String actionBeanType = env.getSpecifiedTypeDeclarations()
				.iterator()
				.next()
				.getQualifiedName();
			for (String annotationType : StlipseAnnotationProcessorFactory.SUPPORTED_ANNOTATIONS)
			{
				AnnotationTypeDeclaration annotation = (AnnotationTypeDeclaration)env.getTypeDeclaration(annotationType);
				Collection<Declaration> annotatedProperties = env.getDeclarationsAnnotatedWith(annotation);
				for (Declaration annotatedProperty : annotatedProperties)
				{
					StlipseAnnotationVisitor visitor = new StlipseAnnotationVisitor(annotationType,
						javaProject, actionBeanType, env.getMessager());
					annotatedProperty.accept(visitor);
				}
			}
		}
	}
}
