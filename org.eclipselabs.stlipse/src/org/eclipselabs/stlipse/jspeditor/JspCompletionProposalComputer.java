/*-
 * Copyright (C) 2011 by Iwao AVE!
 * This program is made available under the terms of the MIT License.
 */

package org.eclipselabs.stlipse.jspeditor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.contentassist.CompletionProposal;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.wst.sse.core.StructuredModelManager;
import org.eclipse.wst.sse.core.internal.provisional.IStructuredModel;
import org.eclipse.wst.sse.core.internal.provisional.text.IStructuredDocumentRegion;
import org.eclipse.wst.sse.core.internal.provisional.text.ITextRegion;
import org.eclipse.wst.sse.core.internal.provisional.text.ITextRegionList;
import org.eclipse.wst.sse.core.utils.StringUtils;
import org.eclipse.wst.sse.ui.contentassist.CompletionProposalInvocationContext;
import org.eclipse.wst.xml.core.internal.provisional.document.IDOMNode;
import org.eclipse.wst.xml.core.internal.regions.DOMRegionContext;
import org.eclipse.wst.xml.ui.internal.contentassist.AbstractXMLCompletionProposalComputer;
import org.eclipse.wst.xml.ui.internal.contentassist.ContentAssistRequest;
import org.eclipse.wst.xml.ui.internal.contentassist.DefaultXMLCompletionProposalComputer;
import org.eclipselabs.stlipse.Activator;
import org.eclipselabs.stlipse.cache.BeanClassCache;
import org.eclipselabs.stlipse.cache.BeanClassInfo;
import org.eclipselabs.stlipse.cache.BeanPropertyCache;
import org.eclipselabs.stlipse.util.ClassNameUtil;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

/**
 * The super class {@link AbstractXMLCompletionProposalComputer} will be available to API.
 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=310696
 * 
 * @author ave
 */
public class JspCompletionProposalComputer extends DefaultXMLCompletionProposalComputer
{
	protected void addAttributeValueProposals(ContentAssistRequest contentAssistRequest,
		CompletionProposalInvocationContext context)
	{
		IDOMNode node = (IDOMNode)contentAssistRequest.getNode();
		String tagName = node.getNodeName();
		IStructuredDocumentRegion open = node.getFirstStructuredDocumentRegion();
		ITextRegionList openRegions = open.getRegions();
		int i = openRegions.indexOf(contentAssistRequest.getRegion());
		if (i < 0)
			return;
		ITextRegion nameRegion = null;
		while (i >= 0)
		{
			nameRegion = openRegions.get(i--);
			if (nameRegion.getType() == DOMRegionContext.XML_TAG_ATTRIBUTE_NAME)
				break;
		}

		// get the attribute in question (first attr name to the left of the cursor)
		String attributeName = null;
		if (nameRegion != null)
			attributeName = open.getText(nameRegion);

		if (isProposalsAvailable(tagName, attributeName))
		{
			// boolean existingComplicatedValue = contentAssistRequest.getRegion() != null
			// && contentAssistRequest.getRegion() instanceof ITextRegionContainer;
			// if (existingComplicatedValue)
			// {
			// contentAssistRequest.getProposals().clear();
			// contentAssistRequest.getMacros().clear();
			// return;
			// }

			String currentValue = null;
			if (contentAssistRequest.getRegion().getType() == DOMRegionContext.XML_TAG_ATTRIBUTE_VALUE)
				currentValue = contentAssistRequest.getText();
			else
				currentValue = "";

			String matchString = null;
			int start = contentAssistRequest.getReplacementBeginPosition();
			int length = contentAssistRequest.getReplacementLength();
			if (currentValue.length() > StringUtils.strip(currentValue).length()
				&& (currentValue.startsWith("\"") || currentValue.startsWith("'"))
				&& contentAssistRequest.getMatchString().length() > 0)
			{
				// Value is surrounded by (double) quotes.
				matchString = currentValue.substring(1, contentAssistRequest.getMatchString()
					.length());
				start++;
				length = currentValue.length() - 2;
			}
			else
				matchString = currentValue.substring(0, contentAssistRequest.getMatchString()
					.length());

			if ("beanclass".equalsIgnoreCase(attributeName))
			{
				List<ICompletionProposal> classProposals = getBeanclassProposals(context,
					contentAssistRequest, matchString, start, length);
				if (classProposals != null)
				{
					for (ICompletionProposal proposal : classProposals)
					{
						contentAssistRequest.addProposal(proposal);
					}
				}
			}
			else if (isActionBeanProperty(tagName, attributeName))
			{
				Node parentNode = getFormElement(node);
				if (parentNode == null)
				{
					Activator.log(Status.WARNING, "Stripes form tag was not found.");
				}
				else
				{
					NamedNodeMap attributes = parentNode.getAttributes();
					Node beanclassAttribute = attributes.getNamedItem("beanclass");
					if (beanclassAttribute == null)
					{
						Activator.log(Status.WARNING,
							"Stripes form tag does not have a 'beanclass' attribute.");
					}
					else
					{
						String qualifiedName = beanclassAttribute.getNodeValue();
						IResource resource = getResource(contentAssistRequest);
						IJavaProject project = getJavaProject(resource);
						boolean includeReadOnly = "label".equals(getStripesTagSuffix(tagName));
						Map<String, String> fields = BeanPropertyCache.searchFields(project, qualifiedName,
							matchString, includeReadOnly, -1, false, null);
						List<ICompletionProposal> proposals = BeanPropertyCache.buildFieldNameProposal(
							fields, matchString, start, length);
						for (ICompletionProposal proposal : proposals)
						{
							contentAssistRequest.addProposal(proposal);
						}
					}
				}
			}
		}
	}

	protected static List<ICompletionProposal> getBeanclassProposals(
		CompletionProposalInvocationContext context, ContentAssistRequest contentAssistRequest,
		final String input, final int offset, final int replacementLength)
	{
		final List<ICompletionProposal> proposalList = new ArrayList<ICompletionProposal>();
		final IResource resource = getResource(contentAssistRequest);
		final List<BeanClassInfo> beanClassList = BeanClassCache.getBeanClassInfo(getJavaProject(resource));
		final String packageName = ClassNameUtil.getPackage(input);
		final String typeName = ClassNameUtil.getTypeName(input);
		for (BeanClassInfo beanClass : beanClassList)
		{
			if (beanClass.matches(packageName.toCharArray(), typeName.toCharArray()))
			{
				StringBuilder replacementString = new StringBuilder();
				replacementString.append(beanClass.getPackageName())
					.append('.')
					.append(beanClass.getSimpleTypeName());

				StringBuilder displayString = new StringBuilder();
				displayString.append(beanClass.getSimpleTypeName())
					.append(" - ")
					.append(beanClass.getPackageName());

				int replacementOffset = offset;
				int cursorPosition = replacementString.length();
				ICompletionProposal proposal = new CompletionProposal(replacementString.toString(),
					replacementOffset, replacementLength, cursorPosition, Activator.getIcon(),
					displayString.toString(), null, null);
				proposalList.add(proposal);
			}
		}
		return proposalList;
	}

	private static IJavaProject getJavaProject(IResource resource)
	{
		IProject proj = resource.getProject();
		IJavaProject javaProject = JavaCore.create(proj);
		return javaProject;
	}

	private static IResource getResource(ContentAssistRequest request)
	{
		IResource resource = null;
		String baselocation = null;

		if (request != null)
		{
			IStructuredDocumentRegion region = request.getDocumentRegion();
			if (region != null)
			{
				IDocument document = region.getParentDocument();
				IStructuredModel model = null;
				try
				{
					model = StructuredModelManager.getModelManager().getExistingModelForRead(
						document);
					if (model != null)
					{
						baselocation = model.getBaseLocation();
					}
				}
				finally
				{
					if (model != null)
						model.releaseFromRead();
				}
			}
		}

		if (baselocation != null)
		{
			IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
			IPath filePath = new Path(baselocation);
			IFile file = null;

			if (filePath.segmentCount() > 1)
			{
				file = root.getFile(filePath);
			}
			if (file != null)
			{
				resource = file.getProject();
			}
		}
		return resource;
	}

	public static Node getFormElement(Node node)
	{
		Node parentNode = node.getParentNode();
		while (parentNode != null && !isFormTag(parentNode.getNodeName()))
		{
			parentNode = parentNode.getParentNode();
		}
		return parentNode;
	}

	private static boolean isFormTag(String tagName)
	{
		String suffix = getStripesTagSuffix(tagName);
		return "form".equals(suffix);
	}

	private boolean isProposalsAvailable(String tagName, String attributeName)
	{
		if ("beanclass".equalsIgnoreCase(attributeName))
		{
			return true;
		}
		return isActionBeanProperty(tagName, attributeName);
	}

	public static boolean isActionBeanProperty(String tagName, String attributeName)
	{
		String suffix = getStripesTagSuffix(tagName);
		return isSuggestableAttribute(attributeName) && isSuggestableTag(suffix);
	}

	private static boolean isSuggestableAttribute(String attribute)
	{
		return "name".equalsIgnoreCase(attribute) || "for".equalsIgnoreCase(attribute);
	}

	public static boolean isSuggestableTag(String tag)
	{
		final List<String> tags = Arrays.asList("checkbox", "file", "hidden", "label",
			"password", "radio", "select", "text", "textarea");
		return tags.contains(tag);
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

	private static Set<String> getPrefixesFromPreference()
	{
		IPreferenceStore store = Activator.getDefault().getPreferenceStore();
		List<String> list = Arrays.asList(store.getString("tagPrefixes").split(" *, *"));
		Set<String> result = new HashSet<String>();
		result.addAll(list);
		return result;
	}
}
