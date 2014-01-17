/*-
 * Copyright (C) 2011-2014 by Iwao AVE!
 * This program is made available under the terms of the MIT License.
 */

package org.eclipselabs.stlipse.apt;

import static org.eclipselabs.stlipse.util.StripesClasses.*;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import com.sun.mirror.apt.AnnotationProcessor;
import com.sun.mirror.apt.AnnotationProcessorEnvironment;
import com.sun.mirror.apt.AnnotationProcessorFactory;
import com.sun.mirror.declaration.AnnotationTypeDeclaration;

/**
 * @author Iwao AVE!
 */
public class StlipseAnnotationProcessorFactory implements AnnotationProcessorFactory
{
	public static final List<String> SUPPORTED_ANNOTATIONS = Arrays.asList(
		VALIDATE_NESTED_PROPERTIES, VALIDATE, STRICT_BINDING, BEFORE, AFTER, VALIDATION_METHOD,
		WIZARD);

	public AnnotationProcessor getProcessorFor(Set<AnnotationTypeDeclaration> arg0,
		AnnotationProcessorEnvironment env)
	{
		return new StlipseAnnotationProcessor(env);
	}

	public Collection<String> supportedAnnotationTypes()
	{
		return SUPPORTED_ANNOTATIONS;
	}

	public Collection<String> supportedOptions()
	{
		return Collections.emptyList();
	}

}
