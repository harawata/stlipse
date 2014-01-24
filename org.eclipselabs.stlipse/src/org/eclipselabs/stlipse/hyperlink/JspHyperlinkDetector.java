/*-
 * Copyright (C) 2011-2012 by Iwao AVE!
 * This program is made available under the terms of the MIT License.
 */

package org.eclipselabs.stlipse.hyperlink;

import java.util.Map;

import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.hyperlink.AbstractHyperlinkDetector;
import org.eclipse.jface.text.hyperlink.IHyperlink;
import org.eclipse.wst.sse.core.StructuredModelManager;
import org.eclipse.wst.sse.core.internal.provisional.IStructuredModel;
import org.eclipse.wst.sse.core.internal.provisional.IndexedRegion;
import org.eclipse.wst.sse.core.internal.provisional.text.IStructuredDocument;
import org.eclipse.wst.sse.core.internal.provisional.text.IStructuredDocumentRegion;
import org.eclipse.wst.sse.core.internal.provisional.text.ITextRegion;
import org.eclipse.wst.sse.core.internal.provisional.text.ITextRegionList;
import org.eclipse.wst.sse.core.utils.StringUtils;
import org.eclipse.wst.xml.core.internal.regions.DOMRegionContext;
import org.eclipselabs.stlipse.Activator;
import org.eclipselabs.stlipse.cache.BeanPropertyCache;
import org.eclipselabs.stlipse.cache.EventProperty;
import org.eclipselabs.stlipse.jspeditor.StripesTagUtil;
import org.eclipselabs.stlipse.util.ProjectUtil;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * @author Iwao AVE!
 */
@SuppressWarnings("restriction")
public class JspHyperlinkDetector extends AbstractHyperlinkDetector
{

	public IHyperlink[] detectHyperlinks(ITextViewer textViewer, IRegion region,
		boolean canShowMultipleHyperlinks)
	{
		IHyperlink[] hyperlinks = null;
		if (textViewer != null && region != null)
		{
			IDocument document = textViewer.getDocument();
			if (document != null)
			{
				Node currentNode = getCurrentNode(document, region.getOffset());
				if (currentNode != null && currentNode.getNodeType() == Node.ELEMENT_NODE)
				{
					Element element = (Element)currentNode;
					IStructuredDocumentRegion documentRegion = ((IStructuredDocument)document).getRegionAtCharacterOffset(region.getOffset());
					ITextRegion textRegion = documentRegion.getRegionAtCharacterOffset(region.getOffset());
					ITextRegion nameRegion = null;
					ITextRegion valueRegion = null;
					String tagName = element.getTagName();
					String attrName = null;
					String attrValue = null;
					if (DOMRegionContext.XML_TAG_ATTRIBUTE_VALUE.equals(textRegion.getType()))
					{
						ITextRegionList regions = documentRegion.getRegions();
						/*
						 * Could use 2, but there needs to be the tag open and name regions
						 */
						int index = regions.indexOf(textRegion);
						if (index >= 4)
						{
							nameRegion = regions.get(index - 2);
							valueRegion = textRegion;
							attrName = documentRegion.getText(nameRegion);
							attrValue = StringUtils.strip(documentRegion.getText(valueRegion));
						}
					}
					if (attrName != null && attrValue != null)
					{
						if ("beanclass".equals(attrName))
						{
							hyperlinks = createHyperlink(document, documentRegion, valueRegion, attrValue);
						}
						else if (StripesTagUtil.isEventAttribute(tagName, attrName))
						{
							String beanclass = StripesTagUtil.getBeanclassAttribute(currentNode);
							hyperlinks = createHyperlink(document, documentRegion, valueRegion, beanclass,
								attrValue);
						}
						else
						{
							String beanclass = StripesTagUtil.getParentBeanclass(currentNode, "form", "url",
								"link");
							if (beanclass != null)
							{
								hyperlinks = createHyperlink(document, documentRegion, valueRegion, beanclass,
									attrValue);
							}
						}
					}
				}
			}
		}
		return hyperlinks;
	}

	private IHyperlink[] createHyperlink(IDocument document,
		IStructuredDocumentRegion documentRegion, ITextRegion valueRegion, String beanclass)
	{
		return createHyperlink(document, documentRegion, valueRegion, beanclass, null);
	}

	private IHyperlink[] createHyperlink(IDocument document,
		IStructuredDocumentRegion documentRegion, ITextRegion valueRegion, String beanclass,
		String attrValue)
	{
		IHyperlink[] hyperlinks = null;
		try
		{
			IJavaProject project = ProjectUtil.getProjectFromDocument(document);
			if (project != null)
			{
				IType actionBean = project.findType(beanclass);
				if (actionBean != null)
				{
					int fqnOffset = documentRegion.getStartOffset() + valueRegion.getStart();
					int fqnLength = valueRegion.getTextLength();
					Map<String, EventProperty> events = BeanPropertyCache.getEventHandlers(project,
						beanclass);
					IJavaElement target = searchElement(actionBean, attrValue, events);
					hyperlinks = new IHyperlink[]{
						new JspHyperlink(target, new Region(fqnOffset, fqnLength))
					};
				}
			}
		}
		catch (JavaModelException e)
		{
			Activator.log(Status.WARNING, "Failed to create a hyperlink for " + beanclass, e);
		}
		return hyperlinks;
	}

	private IJavaElement searchElement(IType actionBean, String attrValue,
		Map<String, EventProperty> eventHandlers)
	{
		IJavaElement target = null;
		if (attrValue != null)
		{
			String elementName = attrValue.replaceAll("[^a-zA-Z\\.\\[]", "");
			if (elementName.startsWith("actionBean."))
				elementName = elementName.substring(11);
			final int dotPos = elementName.indexOf('.');
			final int bracePos = elementName.indexOf('[');
			if (dotPos > 0 && (bracePos == -1 || dotPos < bracePos))
				elementName = elementName.substring(0, dotPos);
			else if (bracePos > 0 && (dotPos == -1 || bracePos < dotPos))
				elementName = elementName.substring(0, bracePos);
			// Resolve event name
			EventProperty eventProperty = eventHandlers.get(elementName);
			if (eventProperty != null)
			{
				elementName = eventProperty.getMethodName();
			}
			// Search method
			target = actionBean.getMethod(elementName, null);
			if (target == null || !target.exists())
			{
				// Search field
				target = actionBean.getField(elementName);
			}
		}
		return target == null ? actionBean : target;
	}

	/**
	 * Returns the node the cursor is currently on in the document. null if no node is selected
	 * 
	 * @param offset
	 * @return Node either element, doctype, text, or null
	 */
	private Node getCurrentNode(IDocument document, int offset)
	{
		// get the current node at the offset (returns either: element,
		// doctype, text)
		IndexedRegion inode = null;
		IStructuredModel sModel = null;
		try
		{
			sModel = StructuredModelManager.getModelManager().getExistingModelForRead(document);
			if (sModel != null)
			{
				inode = sModel.getIndexedRegion(offset);
				if (inode == null)
				{
					inode = sModel.getIndexedRegion(offset - 1);
				}
			}
		}
		finally
		{
			if (sModel != null)
				sModel.releaseFromRead();
		}

		if (inode instanceof Node)
		{
			return (Node)inode;
		}
		return null;
	}

}
