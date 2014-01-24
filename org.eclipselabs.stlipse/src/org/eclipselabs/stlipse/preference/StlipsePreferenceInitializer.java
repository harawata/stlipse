/*-
 * Copyright (C) 2011-2014 by Iwao AVE!
 * This program is made available under the terms of the MIT License.
 */

package org.eclipselabs.stlipse.preference;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipselabs.stlipse.Activator;

/**
 * @author Iwao AVE!
 */
public class StlipsePreferenceInitializer extends AbstractPreferenceInitializer
{

	@Override
	public void initializeDefaultPreferences()
	{
		IPreferenceStore store = Activator.getDefault().getPreferenceStore();
		store.setDefault("tagPrefixes", Activator.DEFAULT_TAG_PREFIXES);
	}

}
