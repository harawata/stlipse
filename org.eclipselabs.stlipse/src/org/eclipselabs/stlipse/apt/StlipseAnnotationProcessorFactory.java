/*-
 * Copyright (C) 2011-2012 by Iwao AVE!
 * This program is made available under the terms of the MIT License.
 */

package org.eclipselabs.stlipse.apt;

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

	public static final String STRICT_BINDING = "net.sourceforge.stripes.action.StrictBinding";

	public static final String VALIDATE_NESTED_PROPERTIES = "net.sourceforge.stripes.validation.ValidateNestedProperties";

	public static final String VALIDATE = "net.sourceforge.stripes.validation.Validate";

	public static final String BEFORE = "net.sourceforge.stripes.action.Before";

	public static final String AFTER = "net.sourceforge.stripes.action.After";

	public static final String VALIDATION_METHOD = "net.sourceforge.stripes.validation.ValidationMethod";

	public static final String WIZARD = "net.sourceforge.stripes.action.Wizard";

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
