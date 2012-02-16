/*-
 * Copyright (C) 2011-2012 by Iwao AVE!
 * This program is made available under the terms of the MIT License.
 */

package org.eclipselabs.stlipse.jspeditor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
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
import org.eclipselabs.stlipse.javaeditor.JavaCompletionProposal;
import org.eclipselabs.stlipse.util.ClassNameUtil;

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
				matchString = currentValue.substring(1, contentAssistRequest.getMatchString().length());
				start++;
				length = currentValue.length() - 2;
			}
			else
				matchString = currentValue.substring(0, contentAssistRequest.getMatchString().length());

			if ("beanclass".equalsIgnoreCase(attributeName))
			{
				proposeBeanclass(contentAssistRequest, context, matchString, start, length);
			}
			else if (StripesTagUtil.isSuggestableFormTag(tagName, attributeName))
			{
				String beanclass = StripesTagUtil.getParentBeanclass(node, "form");
				boolean includeReadOnly = "label".equals(StripesTagUtil.getStripesTagSuffix(tagName));
				proposalField(contentAssistRequest, matchString, start, length, beanclass,
					includeReadOnly);
			}
			else if (StripesTagUtil.isParamTag(tagName, attributeName))
			{
				String beanclass = StripesTagUtil.getParentBeanclass(node, "url", "link");
				proposalField(contentAssistRequest, matchString, start, length, beanclass, false);
			}
			else if (StripesTagUtil.isSubmitTag(tagName, attributeName))
			{
				String beanclass = StripesTagUtil.getParentBeanclass(node, "form");
				proposeEvent(contentAssistRequest, node, matchString, start, length, beanclass);
			}
			else if (StripesTagUtil.isEventAttribute(tagName, attributeName))
			{
				String beanclass = StripesTagUtil.getBeanclassAttribute(node);
				proposeEvent(contentAssistRequest, node, matchString, start, length, beanclass);
			}
		}
	}

	private void proposeEvent(ContentAssistRequest contentAssistRequest, IDOMNode node,
		String matchString, int start, int length, String beanclass)
	{
		if (beanclass != null)
		{
			IResource resource = getResource(contentAssistRequest);
			IJavaProject project = getJavaProject(resource);
			List<String> events = BeanPropertyCache.searchEventHandler(project, beanclass,
				matchString, false);
			int relevance = events.size();
			for (String event : events)
			{
				ICompletionProposal proposal = new JavaCompletionProposal(event, start, length,
					event.length(), Activator.getIcon(), event, null, null, relevance--);
				contentAssistRequest.addProposal(proposal);
			}
		}
	}

	private void proposalField(ContentAssistRequest contentAssistRequest, String matchString,
		int start, int length, String beanclass, boolean includeReadOnly)
	{
		if (beanclass != null)
		{
			IResource resource = getResource(contentAssistRequest);
			IJavaProject project = getJavaProject(resource);
			Map<String, String> fields = BeanPropertyCache.searchFields(project, beanclass,
				matchString, includeReadOnly, -1, false, null);
			List<ICompletionProposal> proposals = BeanPropertyCache.buildFieldNameProposal(fields,
				matchString, start, length);
			for (ICompletionProposal proposal : proposals)
			{
				contentAssistRequest.addProposal(proposal);
			}
		}
	}

	private void proposeBeanclass(ContentAssistRequest contentAssistRequest,
		CompletionProposalInvocationContext context, String matchString, int start, int length)
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

	protected static List<ICompletionProposal> getBeanclassProposals(
		CompletionProposalInvocationContext context, ContentAssistRequest contentAssistRequest,
		final String input, final int offset, final int replacementLength)
	{
		final List<ICompletionProposal> proposalList = new ArrayList<ICompletionProposal>();
		final IResource resource = getResource(contentAssistRequest);
		final String packageName = ClassNameUtil.getPackage(input);
		final String typeName = ClassNameUtil.getTypeName(input);
		final List<BeanClassInfo> beanClassList = BeanClassCache.getBeanClassInfo(getJavaProject(resource));
		synchronized (beanClassList)
		{
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
					model = StructuredModelManager.getModelManager().getExistingModelForRead(document);
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

	private boolean isProposalsAvailable(String tagName, String attributeName)
	{
		return "beanclass".equalsIgnoreCase(attributeName)
			|| StripesTagUtil.isSuggestableFormTag(tagName, attributeName)
			|| StripesTagUtil.isParamTag(tagName, attributeName)
			|| StripesTagUtil.isSubmitTag(tagName, attributeName)
			|| StripesTagUtil.isEventAttribute(tagName, attributeName);
	}
}
