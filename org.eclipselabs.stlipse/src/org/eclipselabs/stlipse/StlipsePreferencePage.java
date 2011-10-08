/*-
 * Copyright (C) 2011 by Iwao AVE!
 * This program is made available under the terms of the MIT License.
 */

package org.eclipselabs.stlipse;

import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

public class StlipsePreferencePage extends FieldEditorPreferencePage implements
	IWorkbenchPreferencePage
{
	public void init(IWorkbench workbench)
	{
		setPreferenceStore(Activator.getDefault().getPreferenceStore());
		setDescription("Stlipse preferences");
	}

	@Override
	protected void createFieldEditors()
	{
		addField(new StringFieldEditor("tagPrefixes", "Tag prefixes", getFieldEditorParent()));
	}
}
