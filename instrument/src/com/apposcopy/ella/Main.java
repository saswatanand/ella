package com.apposcopy.ella;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
 
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.*;

import com.apposcopy.app.App;

public class Main
{
	public static void main(String[] args) throws IOException
	{
		Config config = Config.g();

		boolean listClasses = args[0].equals("l");
		
		App app = App.readApp(config.inputFile, config.outDir, config.apktoolJar);

		if(listClasses){
			app.listClasses();
			return;
		}

		if(config.outputFile == null){
			config.outputFile = config.outDir+File.separator+"instrumented.apk";
		}

		File updatedManifestFile = updateManifest(app.manifestFile());

		File unsignedOutputFile = File.createTempFile("ellainstrumented", ".apk");
		app.updateApk(new Instrument().instrument(app.dexFilePath()), updatedManifestFile, unsignedOutputFile, config.apktoolJar);

		//sign the apk
		if(config.outputFile.endsWith(".apk"))
			app.signAndAlignApk(unsignedOutputFile, config.outputFile, config.keyStore, config.storePass, config.keyPass, config.alias, config.zipAlign);

		CoverageId.g().dump();

		System.out.println("\n********************************************************************************");
		System.out.println("Install the following instrumented apk and then execute the app.");
		System.out.println(config.outputFile);
		System.out.println("********************************************************************************\n");
		
		System.out.println("\n********************************************************************************");
		System.out.println("Coverage data will be stored in the following directory.");
		System.out.println(config.outDir);
		//System.out.println("View the coverage report at the following URL.");
		//System.out.println(config.tomcatUrl+"/viewcoverage?apppath="+appPath);
		System.out.println("********************************************************************************\n");
	}
	
	static File updateManifest(File manifestFile)
	{
		try{
			Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(manifestFile);
			
			Element applicationNode = (Element) document.getElementsByTagName("application").item(0);
			
			//insert ella broadcast receiver
			Element rcvrElement = document.createElement("receiver");
			rcvrElement.setAttribute("android:name", "com.apposcopy.ella.runtime.BroadcastReceiver");
			applicationNode.appendChild(rcvrElement);

			Element filterElement = document.createElement("intent-filter");
			rcvrElement.appendChild(filterElement);

			Element actionElement = document.createElement("action");
			actionElement.setAttribute("android:name", "com.apposcopy.ella.COVERAGE");
			filterElement.appendChild(actionElement);

			//add internet permission
			Element permissionElement = document.createElement("uses-permission");
			permissionElement.setAttribute("android:name", "android.permission.INTERNET");
			applicationNode.getParentNode().appendChild(permissionElement);
			
			if(Config.g().useAndroidDebug){
				//add external-storage write permission
				permissionElement = document.createElement("uses-permission");
				permissionElement.setAttribute("android:name", "android.permission.WRITE_EXTERNAL_STORAGE");
				applicationNode.getParentNode().appendChild(permissionElement);
			}
				

			/*
			//insert ella upload service
			Element svcElement = document.createElement("service");
			svcElement.setAttribute("android:name", "com.apposcopy.ella.runtime.UploadService");
			svcElement.setAttribute("android:exported", "false");
			applicationNode.appendChild(svcElement);
			*/

			// write the content into xml file
			File updatedManifest = File.createTempFile("stamp_android_manifest", null, null);
			updatedManifest.deleteOnExit();

 			TransformerFactory.newInstance().newTransformer().transform(new DOMSource(document), new StreamResult(updatedManifest));
 
			return updatedManifest;
		} catch(Exception e){
			throw new Error(e);
		}
	}
}

