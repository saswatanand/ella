package com.apposcopy.app;

import java.util.*;
import javax.xml.xpath.*;
import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;

public class PersonalNamespaceContext implements NamespaceContext 
{
	public String getNamespaceURI(String prefix) {
		if (prefix == null) throw new NullPointerException("Null prefix");
		else if ("android".equals(prefix)) return "http://schemas.android.com/apk/res/android";
		else if ("xml".equals(prefix)) return XMLConstants.XML_NS_URI;
			return XMLConstants.NULL_NS_URI;
	}
	
	// This method isn't necessary for XPath processing.
	public String getPrefix(String uri) {
		throw new UnsupportedOperationException();
	}
	
	// This method isn't necessary for XPath processing either.
	public Iterator getPrefixes(String uri) {
		throw new UnsupportedOperationException();
	}
	
}

