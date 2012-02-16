/*-
 * Copyright (C) 2011-2012 by Iwao AVE!
 * This program is made available under the terms of the MIT License.
 */

package org.eclipselabs.stlipse;

import java.net.URL;

import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

/**
 * The activator class controls the plug-in life cycle
 */
public class Activator extends AbstractUIPlugin
{

	// The plug-in ID
	public static final String PLUGIN_ID = "org.eclipselabs.stlipse"; //$NON-NLS-1$

	public static final String IMAGE_ID = "stripes.image"; //$NON-NLS-1$

	public static final String DEFAULT_TAG_PREFIXES = "stripes, ss, sd";

	// The shared instance
	private static Activator plugin;

	private IResourceChangeListener resourceChangeListener;

	/**
	 * The constructor
	 */
	public Activator()
	{
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext context) throws Exception
	{
		super.start(context);
		plugin = this;

		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		resourceChangeListener = new ResourceChangeListener();
		workspace.addResourceChangeListener(resourceChangeListener);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext context) throws Exception
	{
		plugin = null;
		super.stop(context);

		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		if (workspace != null && resourceChangeListener != null)
			workspace.removeResourceChangeListener(resourceChangeListener);
	}

	@Override
	protected void initializeImageRegistry(ImageRegistry reg)
	{
		Bundle bundle = Platform.getBundle(PLUGIN_ID);
		IPath path = new Path("icons/stripes.png");
		URL url = FileLocator.find(bundle, path, null);
		ImageDescriptor desc = ImageDescriptor.createFromURL(url);
		getImageRegistry().put(IMAGE_ID, desc);
		super.initializeImageRegistry(reg);
	}

	/**
	 * Returns the shared instance
	 * 
	 * @return the shared instance
	 */
	public static Activator getDefault()
	{
		return plugin;
	}

	public static void log(Status status)
	{
		if (getDefault() != null && getDefault().getLog() != null)
			getDefault().getLog().log(status);
	}

	public static void log(int severity, String message)
	{
		log(new Status(severity, PLUGIN_ID, message));
	}

	public static void log(int severity, String message, Throwable t)
	{
		log(severity, 0, message, t);
	}

	public static void log(int severity, int code, String message, Throwable t)
	{
		log(new Status(severity, PLUGIN_ID, code, message, t));
	}

	public static Image getIcon()
	{
		return Activator.getDefault()
			.getImageRegistry()
			.getDescriptor(Activator.IMAGE_ID)
			.createImage();
	}
}
