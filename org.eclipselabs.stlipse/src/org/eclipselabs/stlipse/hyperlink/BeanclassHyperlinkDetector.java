/*-
 * Copyright (C) 2011 by Iwao AVE!
 * This program is made available under the terms of the MIT License.
 */

package org.eclipselabs.stlipse.hyperlink;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
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
import org.w3c.dom.Element;
import org.w3c.dom.Node;

public class BeanclassHyperlinkDetector extends AbstractHyperlinkDetector
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
					String name = null;
					String value = null;
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
							name = documentRegion.getText(nameRegion);
							value = StringUtils.strip(documentRegion.getText(valueRegion));
						}
					}
					if (name != null && value != null)
					{
						if ("beanclass".equals(name))
						{
							try
							{
								IJavaProject project = getProjectFromDocument(document);
								if (project != null)
								{
									IType type = project.findType(value);
									if (type != null)
									{
										int fqnOffset = documentRegion.getStartOffset()
											+ valueRegion.getStart();
										int fqnLength = valueRegion.getTextLength();
										hyperlinks = new IHyperlink[]{
											new BeanclassHyperlink(type, new Region(fqnOffset,
												fqnLength))
										};
									}
								}
							}
							catch (JavaModelException e)
							{
								ILog log = Activator.getDefault().getLog();
								log.log(new Status(Status.ERROR, Activator.PLUGIN_ID, 0,
									"Failed to create a hyperlink for " + value, e));
							}
						}
					}
				}
			}
		}
		return hyperlinks;
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

	private IJavaProject getProjectFromDocument(IDocument document)
	{
		IStructuredModel model = null;
		String baseLocation = null;
		IJavaProject result = null;

		// try to locate the file in the workspace
		try
		{
			model = StructuredModelManager.getModelManager().getExistingModelForRead(document);
			if (model != null)
			{
				baseLocation = model.getBaseLocation();
			}
		}
		finally
		{
			if (model != null)
				model.releaseFromRead();
		}
		if (baseLocation != null)
		{
			IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
			IPath filePath = new Path(baseLocation);
			IFile file = null;

			if (filePath.segmentCount() > 1)
			{
				file = root.getFile(filePath);
			}
			if (file != null)
			{
				IProject project = file.getProject();
				result = JavaCore.create(project);
			}
		}
		return result;
	}

}
