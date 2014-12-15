package com.apposcopy.app;

public class Data
{
	public String scheme;
	public String host;
	public String port;
	public String path;
	public String pathPattern;
	public String pathPrefix;
	public String mimeType;
	
	public String toString()
	{
		StringBuilder builder = new StringBuilder();
		builder
			.append("{")
			.append(scheme == null ? "" : "scheme: "+scheme)
			.append(host == null ? "" : "host: "+host)
			.append(port == null ? "" : "port: "+port)
			.append(path == null ? "" : "path: "+path)
			.append(pathPattern == null ? "" : "pathPattern: "+pathPattern)
			.append(pathPrefix == null ? "" : "pathPrefix: "+pathPrefix)
			.append(mimeType == null ? "" : "mimeType: "+mimeType)
			.append("}");
		return builder.toString();
	}
}