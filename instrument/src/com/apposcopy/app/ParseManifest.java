package com.apposcopy.app;

import javax.xml.*;
import javax.xml.parsers.*;
import javax.xml.xpath.*;
import javax.xml.namespace.*;
import org.w3c.dom.*;
import org.xml.sax.*;
import java.io.*;
import java.util.*;

/*
* reads AndroidManifest.xml to find out several info about the app
* @author Saswat Anand
*/
public class ParseManifest
{
	private App app;
	private XPath xpath;
	private Document document;

	public ParseManifest(File manifestFile, App app)
	{
		this.app = app;

		try{
			File tmpFile = File.createTempFile("stamp_android_manifest", null, null);
			tmpFile.deleteOnExit();
			UTF8ToAnsiUtils.main(new String[]{manifestFile.getAbsolutePath(), tmpFile.getAbsolutePath()});
			manifestFile = tmpFile;

			DocumentBuilder builder =
				DocumentBuilderFactory.newInstance().newDocumentBuilder();
			document = builder.parse(manifestFile);
			
			xpath = XPathFactory.newInstance().newXPath();
			xpath.setNamespaceContext(new PersonalNamespaceContext());
			
			//find package name and version
			Node node = (Node)
				xpath.evaluate("/manifest", document, XPathConstants.NODE);
			app.setPackageName(node.getAttributes().getNamedItem("package").getNodeValue());
			Node versionNameAttribute = node.getAttributes().getNamedItem("android:versionName");
			if(versionNameAttribute != null) {
				app.setVersion(versionNameAttribute.getNodeValue());
			} else {
				app.setVersion("unknown");
			}

			//find icon path
			node = (Node)
				xpath.evaluate("/manifest/application", document, XPathConstants.NODE);
			if(node.getAttributes().getNamedItem("android:icon") != null) {
                String icon = node.getAttributes().getNamedItem("android:icon").getNodeValue();
                app.setIconPath(icon);
            }

			
			readComponentInfo();
			readPermissionInfo();			
		}catch(Exception e){
			throw new Error(e);
		}		
	}

	private void readPermissionInfo() throws Exception
	{
		Set<String> permissions = app.permissions();
		NodeList nodes = (NodeList)
			xpath.evaluate("/manifest/uses-permission", document, XPathConstants.NODESET);
		for (int i = 0; i < nodes.getLength(); i++) {
			Node node = nodes.item(i);
			Attr attr = ((Element) node).getAttributeNode("android:name");
			permissions.add(attr.getValue());
		}
	}

	private void readComponentInfo() throws Exception
	{		
		//find activities
		findComponents(xpath, document, Component.Type.activity);
		findComponents(xpath, document, Component.Type.service);
		findComponents(xpath, document, Component.Type.receiver);
		
		Node node = (Node)
			xpath.evaluate("/manifest/application", document, XPathConstants.NODE);
		
		//backup agent
		Node backupAgent = node.getAttributes().getNamedItem("android:backupAgent");
		if(backupAgent != null)
			addComp(new Component(fixName(backupAgent.getNodeValue())));
			
		//application class
		Node application = node.getAttributes().getNamedItem("android:name");
		if(application != null)
			addComp(new Component(fixName(application.getNodeValue())));
	}

	private String fixName(String comp)
	{
		String pkgName = app.getPackageName();
		if(comp.startsWith("."))
			comp = pkgName + comp;
		else if(comp.indexOf('.') < 0)
			comp = pkgName + "." + comp;
		return comp;
	}


	private void findComponents(XPath xpath, Document document, Component.Type componentType) throws Exception
	{
		NodeList nodes = (NodeList)
			xpath.evaluate("/manifest/application/"+componentType, document, XPathConstants.NODESET);
		for (int i = 0; i < nodes.getLength(); i++) {
			Node node = nodes.item(i);

			//find the name of component
			NamedNodeMap nnm = node.getAttributes();
			String name = null;
			for(int j = 0; j < nnm.getLength(); j++){
				Node n = nnm.item(j);
				if(n.getNodeName().equals("android:name")){
					name = n.getNodeValue();
					break;
				}
				//System.out.println(n.getNodeName() + " " + );
			}			
			assert name != null : node.getNodeName();
			
			Component comp = addComp(new Component(fixName(name), componentType));			
			setIntentFilters(comp, node);

			boolean exported;
			Attr attr = ((Element) node).getAttributeNode("android:exported");
			if(attr != null){
				if(attr.getValue().equals("true"))
					exported = true;
				else if(attr.getValue().equals("false"))
					exported = false;
				else
					throw new RuntimeException("Unexpected exported attribute "+attr.getValue()+" "+name);
			} else{
				//http://developer.android.com/guide/topics/manifest/activity-element.html#exported
				exported = comp.intentFilters.size() != 0;
			}
			comp.exported = exported;
		}
	}

	private void setIntentFilters(Component comp, Node compNode)
	{
		for(Node ifNode = compNode.getFirstChild(); ifNode != null; ifNode = ifNode.getNextSibling()){
			if(ifNode.getNodeName().equals("intent-filter")){
				IntentFilter intentFilter = new IntentFilter();
				comp.addIntentFilter(intentFilter);

				Attr attr = ((Element) ifNode).getAttributeNode("android:priority");
				if(attr != null){
					intentFilter.setPriority(attr.getValue());
				}
				
				Node ifChildNode = ifNode.getFirstChild();
				while(ifChildNode != null){
					String nodeName = ifChildNode.getNodeName();
					if(nodeName.equals("action")){
						Attr actNameNode = ((Element) ifChildNode).getAttributeNode("android:name");
						if(actNameNode != null){
							intentFilter.addAction(actNameNode.getValue());
						}
					} else if(nodeName.equals("category")){
						Attr catNameNode = ((Element) ifChildNode).getAttributeNode("android:name");
						if(catNameNode != null){
							intentFilter.addCategory(catNameNode.getValue());
						}
					} else if(nodeName.equals("data")){
						Data data = new Data();
						intentFilter.addData(data);
						Attr schemeNode = ((Element) ifChildNode).getAttributeNode("android:scheme");
						if(schemeNode != null){
							data.scheme = schemeNode.getValue();
						}
						Attr hostNode = ((Element) ifChildNode).getAttributeNode("android:host");
						if(hostNode != null){
							data.host = hostNode.getValue();
						}
						Attr portNode = ((Element) ifChildNode).getAttributeNode("android:port");
						if(portNode != null){
							data.port = portNode.getValue();
						}
						Attr pathNode = ((Element) ifChildNode).getAttributeNode("android:path");
						if(pathNode != null){
							data.path = pathNode.getValue();
						}
						Attr pathPatternNode = ((Element) ifChildNode).getAttributeNode("android:pathPattern");
						if(pathPatternNode != null){
							data.pathPattern = pathPatternNode.getValue();
						}
						Attr pathPrefixNode = ((Element) ifChildNode).getAttributeNode("android:pathPrefix");
						if(pathPrefixNode != null){
							data.pathPrefix = pathPrefixNode.getValue();
						}
						Attr mimeTypeNode = ((Element) ifChildNode).getAttributeNode("android:mimeType");
						if(mimeTypeNode != null){
							data.mimeType = mimeTypeNode.getValue();
						}
					}
					ifChildNode = ifChildNode.getNextSibling();
				}
				
			}
		}
	}
	

	private Component addComp(Component c)
	{
		List<Component> comps = app.components();
		for(Component comp : comps){
			if(comp.name.equals(c.name)){
				assert c.type.equals(comp.type);
				return comp;
			}
		}
		comps.add(c);
		return c;
	}
}
