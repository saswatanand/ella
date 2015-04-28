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
		boolean listClasses = false;
		String ellaSettingsFile = null;
		Config config = Config.g();

		int i = 0;
		while(i < args.length){
			String a = args[i];
			if(a.equals("-l")){
				listClasses = true;
			} else if(a.equals("-x")){
				config.excludeFile = args[i+1];
				i++;
			} else if(a.equals("-ella.dir")){
				config.ellaDir = args[i+1];
				i++;				
			} else if(a.equals("-ella.runtime")){
				config.ellaRuntime = args[i+1];
				i++;
			} else if(a.equals("-ella.settings")){
				ellaSettingsFile = args[i+1];
				i++;
			} else if(a.equals("-ella.apktool")){
				config.apktoolJar = args[i+1];
				i++;
			} else if(config.inputFile == null)
				config.inputFile = a;
			else{
				assert config.outputFile == null;
				config.outputFile = a;
			}
			i++;
		}

		config.load(ellaSettingsFile);

		if(config.inputFile == null){
			System.out.println("New input file to process");
			return;
		}
		
		File inputFile = new File(config.inputFile);
		if(!inputFile.exists()){
			System.out.println("Input file "+config.inputFile+" does not exist.");
			return;
		}

		String appPath = inputFile.getCanonicalPath();
		config.appId = Util.appPathToAppId(appPath);

		App app = App.readApp(config.inputFile, config.outDir(), config.apktoolJar);

		if(listClasses){
			app.listClasses();
			return;
		}

		if(config.outputFile == null){
			config.outputFile = config.outDir()+File.separator+"instrumented.apk";
		}

		File updatedManifestFile = updateManifest(app.manifestFile());

		File unsignedOutputFile = File.createTempFile("ellainstrumented", ".apk");
		app.updateApk(Instrument.instrument(app.dexFilePath()), updatedManifestFile, unsignedOutputFile, config.apktoolJar);

		//sign the apk
		if(config.outputFile.endsWith(".apk"))
			app.signAndAlignApk(unsignedOutputFile, config.outputFile, config.keyStore, config.storePass, config.keyPass, config.alias);

		CoverageId.g().dump();

		System.out.println("\n********************************************************************************");
		System.out.println("Install the following instrumented apk and then execute the app");
		System.out.println(config.outputFile);
		System.out.println("********************************************************************************\n");
		
		System.out.println("\n********************************************************************************");
		System.out.println("After coverage data is collected, view the coverage report at the following URL.");
		System.out.println(config.tomcatUrl+"/viewcoverage?apppath="+appPath);
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
