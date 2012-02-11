/*-
 * Copyright (C) 2012 by Iwao AVE!
 * This program is made available under the terms of the MIT License.
 */

package org.eclipselabs.stlipse.jspeditor;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipselabs.stlipse.Activator;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

/**
 * @author Iwao AVE!
 */
public class StripesTagUtil
{
	private static final List<String> suggestableFormTags = Arrays.asList("checkbox", "file",
		"hidden", "label", "password", "radio", "select", "text", "textarea");

	private static final List<String> hasEventTags = Arrays.asList("url", "link", "useActionBean");

	public static boolean isParamTag(String tagName, String attributeName)
	{
		final String suffix = getStripesTagSuffix(tagName);
		return "param".equals(suffix) && "name".equalsIgnoreCase(attributeName);
	}

	public static boolean isSuggestableFormTag(String tagName, String attributeName)
	{
		final String suffix = getStripesTagSuffix(tagName);
		return isSuggestableFormTagAttribute(attributeName) && suggestableFormTags.contains(suffix);
	}

	public static boolean isSubmitTag(String tagName, String attributeName)
	{
		final String suffix = getStripesTagSuffix(tagName);
		return ("submit".equals(suffix) || "image".equals(suffix))
			&& "name".equalsIgnoreCase(attributeName);
	}

	public static boolean isEventAttribute(String tagName, String attributeName)
	{
		final String suffix = getStripesTagSuffix(tagName);
		return hasEventTags.contains(suffix) && "event".equalsIgnoreCase(attributeName);
	}

	static boolean isSuggestableFormTagAttribute(String attribute)
	{
		return "name".equalsIgnoreCase(attribute) || "for".equalsIgnoreCase(attribute);
	}

	public static String getStripesTagSuffix(String tagName)
	{
		final String prefix = getStripesTagPrefix(tagName);
		if (prefix != null && prefix.length() + 1 < tagName.length())
			return tagName.substring(prefix.length() + 1);
		return null;
	}

	private static String getStripesTagPrefix(String tagName)
	{
		Set<String> prefixes = getPrefixesFromPreference();
		int prefixLength = tagName.indexOf(':');
		if (prefixLength > 0)
		{
			String prefix = tagName.substring(0, prefixLength);
			if (prefixes.contains(prefix))
				return prefix;
		}
		return null;
	}

	static Set<String> getPrefixesFromPreference()
	{
		IPreferenceStore store = Activator.getDefault().getPreferenceStore();
		List<String> list = Arrays.asList(store.getString("tagPrefixes").split(" *, *"));
		Set<String> result = new HashSet<String>();
		result.addAll(list);
		return result;
	}

	public static String getParentBeanclass(Node node, String... targetTags)
	{
		Node parentNode = node.getParentNode();
		while (parentNode != null && !isTargetTag(parentNode.getNodeName(), targetTags))
		{
			parentNode = parentNode.getParentNode();
		}
		return getBeanclassAttribute(parentNode);
	}

	public static String getBeanclassAttribute(Node node)
	{
		if (node != null)
		{
			NamedNodeMap attributes = node.getAttributes();
			Node beanclassAttribute = attributes.getNamedItem("beanclass");
			if (beanclassAttribute != null)
			{
				return beanclassAttribute.getNodeValue();
			}
		}
		return null;
	}

	public static boolean isTargetTag(String tagName, String... targetTags)
	{
		String suffix = getStripesTagSuffix(tagName);
		for (String tag : targetTags)
		{
			if (tag.equals(suffix))
				return true;
		}
		return false;
	}
}
