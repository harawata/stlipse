/*-
 * Copyright (C) 2011 by Iwao AVE!
 * This program is made available under the terms of the MIT License.
 */

package org.eclipselabs.stlipse.jspeditor;

import org.eclipse.core.resources.IMarker;
import org.eclipse.ui.IMarkerResolution;
import org.eclipse.ui.IMarkerResolutionGenerator2;

/**
 * @author Iwao AVE!
 */
public class JspMarkerResolutionGenerator implements IMarkerResolutionGenerator2
{

	public boolean hasResolutions(IMarker marker)
	{
		String problemType = getProblemType(marker);
		return JspValidator.MISSING_ACTION_BEAN.equals(problemType);
	}

	public IMarkerResolution[] getResolutions(IMarker marker)
	{
		return new IMarkerResolution[]{
			new JspMarkerResolution()
		};
	}

	private String getProblemType(IMarker marker)
	{
		return marker.getAttribute("problemType", "");
	}

}
