package com.apposcopy.app;

import java.util.*;
import java.util.zip.*;
import java.io.*;

import org.jf.dexlib2.DexFileFactory;
import org.jf.dexlib2.iface.ClassDef;
import org.jf.dexlib2.iface.DexFile;

import com.google.common.io.Files;

import com.apposcopy.ella.Util;//remove this dependency

/**
 * @author Saswat Anand
 **/
public class App
{
	private List<Component> comps = new ArrayList();
	private Set<String> permissions = new HashSet();

	private String pkgName;
	private String version;
	private String iconPath;

	private DexFile dexFile;
	private String dexFilePath;
	private String inputFile;

	private File manifestFile;
	private String apktoolOutDir;

	public App(String inputFile) throws IOException
	{
		this.inputFile = inputFile;
		if(inputFile.endsWith(".apk")){			
			this.dexFilePath = extractClassesDex(inputFile);
		} else {
			this.dexFilePath = inputFile;
		}
		this.dexFile = DexFileFactory.loadDexFile(dexFilePath, 15);
	}

	public static App readApp(String inputFile, String scratchDir, String apktoolJar) throws IOException
	{
		App app = new App(inputFile);
		if(inputFile.endsWith(".apk")){			
			app.apktoolOutDir = app.runApktool(scratchDir, apktoolJar);
			ParseManifest pmf = new ParseManifest(new File(app.apktoolOutDir, "AndroidManifest.xml"), app);
			app.process(app.apktoolOutDir);
			app.manifestFile = pmf.manifestFile();

		}
		return app;
	}

	static String extractClassesDex(String inputFile) throws IOException
	{
		File inputDexFile = File.createTempFile("inputclasses",".dex");
		OutputStream out = new FileOutputStream(inputDexFile);
		BufferedInputStream bin = new BufferedInputStream(new FileInputStream(inputFile));
		ZipInputStream zin = new ZipInputStream(bin);
		ZipEntry ze = null;
		while ((ze = zin.getNextEntry()) != null) {
			if (ze.getName().equals("classes.dex")) {
				byte[] buffer = new byte[8192];
				int len;
				while ((len = zin.read(buffer)) != -1) {
					out.write(buffer, 0, len);
				}
				out.close();
				break;
			}
		} 
		return inputDexFile.getAbsolutePath();
	}

	public static void signAndAlignApk(File unsignedApk, String signedApkPath, String keyStore, String storePass, String keyPass, String alias, String zipAlignPath)
	{
		String[] argsSign = {"jarsigner", 
							 "-keystore", keyStore,
							 "-storepass", storePass,
							 "-keypass", keyPass,
							 unsignedApk.getAbsolutePath(),
							 alias};						 
		try{
			for(String s : argsSign) System.out.print(" "+s); System.out.println("");
			int exitCode = Runtime.getRuntime().exec(argsSign).waitFor();
			if(exitCode != 0)
				throw new Error("Error in running jarsigner "+exitCode);
		}catch(Exception e){
			throw new Error("Error in running jarsigner", e);
		}
		
		String[] argsAlign = {zipAlignPath, 
							  "-f", "4",
							  unsignedApk.getAbsolutePath(),
							  signedApkPath};
		try{
			for(String s : argsAlign) System.out.print(" "+s); System.out.println("");
			int exitCode = Runtime.getRuntime().exec(argsAlign).waitFor();
			if(exitCode != 0)
				throw new Error("Error in running zipalign");
		}catch(Exception e){
			throw new Error("Error in running zipalign", e);
		}
	}

	private String runApktool(String scratchDir, String apktoolJar)
	{
		String apktoolOutDir = scratchDir+File.separator+"apktool-out";
		String[] args = {"java", "-Xmx1g", "-ea",
						 "-classpath", apktoolJar,
						 "brut.apktool.Main",
						 "d",  "-f", "--frame-path", scratchDir,
						 "-o", apktoolOutDir,
						 "-s", inputFile};
						 
		try{
			int exitCode = Runtime.getRuntime().exec(args).waitFor();
			if(exitCode != 0) {
				System.err.println("java -Xmx1g -ea -classpath " + apktoolJar + " brut.apktool.Main d -f --frame-path " + scratchDir + " -o " + apktoolOutDir + " -s " + inputFile);
				throw new Error("Error in running apktool");
			}
		}catch(Exception e){
			System.err.println("java -Xmx1g -ea -classpath " + apktoolJar + " brut.apktool.Main d -f --frame-path " + scratchDir + " -o " + apktoolOutDir + " -s " + inputFile);
			throw new Error("Error in running apktool");
		}
		return apktoolOutDir;
	}

	public void listClasses()
	{
		for (ClassDef classDef: dexFile.getClasses()) {
			String className = Util.dottedClassName(classDef.getType());
			System.out.println(className);
		}
	}

	public void process(String apktoolOutDir)
	{
		if(iconPath != null){
			if(iconPath.startsWith("@drawable/")){
				String icon = iconPath.substring("@drawable/".length()).concat(".png");
				File f = new File(apktoolOutDir.concat("/res/drawable"), icon);
				if(f.exists())
					iconPath = f.getPath();
				else {
					f = new File(apktoolOutDir.concat("/res/drawable-hdpi"), icon);
					if(f.exists())
						iconPath = f.getPath();
					else
						iconPath = null;
				}
			} else
				iconPath = null;
		}

		List<Component> comps = this.comps;
		this.comps = new ArrayList();

		Set<String> compNames = new HashSet();
		for(Component c : comps){
			//System.out.println("@@ "+c.name);
			compNames.add(c.name);
		}

		filterDead(compNames);

		//System.out.println("^^ "+compNames.size());
		
		for(Component c : comps){
			if(compNames.contains(c.name))
				this.comps.add(c);
		}

	}

	public String dexFilePath()
	{
		return dexFilePath;
	}

	public void updateApk(String newDexFilePath, File newManifestFile, File outputFile, String apktoolJar) throws IOException
	{
		if(!inputFile.endsWith(".apk")){
			assert inputFile.endsWith(".dex") && outputFile.getName().endsWith(".dex");
			Files.copy(new File(newDexFilePath), outputFile);
			return;
		}
		
		assert outputFile.getName().endsWith(".apk");

		Files.copy(newManifestFile, new File(apktoolOutDir, "AndroidManifest.xml"));
		Files.copy(new File(newDexFilePath), new File(apktoolOutDir, "classes.dex"));

		String[] args = {"java", "-Xmx1g", "-ea",
						 "-classpath", apktoolJar,
						 "brut.apktool.Main",
						 "b",  "-o", outputFile.getAbsolutePath(), apktoolOutDir};
						 
		try{
			
			Process p = Runtime.getRuntime().exec(args);
			BufferedReader reader = new BufferedReader(new InputStreamReader(p.getErrorStream()));
			while(reader.readLine() != null){}
			int exitCode = p.waitFor();
			if(exitCode != 0) {
				for(String a : args) System.out.print(a+" ");
				System.out.println("");
				throw new Error("Error in repackaging the apk."); 
			}
		}catch(Exception e){
			for(String a : args) System.out.print(a+" ");
			System.out.println("");
			throw new Error("Error in repackaging the apk.");
		}

		/*
		//stick the new dex file into the output apk
		ZipInputStream zin = new ZipInputStream(new BufferedInputStream(new FileInputStream(inputFile)));
		ZipOutputStream zout = new ZipOutputStream(new FileOutputStream(outputFile));
		ZipEntry ze = null;
		byte[] buffer = new byte[8192];
		int len;
		while ((ze = zin.getNextEntry()) != null) {
			String entryName = ze.getName();
			if(entryName.startsWith("META-INF"))
				continue;
			zout.putNextEntry(new ZipEntry(entryName));
			if(entryName.equals("AndroidManifest.xml")) {
				//write the instrumented classes.dex
				BufferedInputStream bin = new BufferedInputStream(new FileInputStream(newManifestFile));
				while ((len = bin.read(buffer)) != -1) {
					zout.write(buffer, 0, len);
				}
				bin.close();
			} else if(entryName.equals("classes.dex")) {
				//write the instrumented classes.dex
				BufferedInputStream bin = new BufferedInputStream(new FileInputStream(newDexFilePath));
				while ((len = bin.read(buffer)) != -1) {
					zout.write(buffer, 0, len);
				}
				bin.close();
			} else {
				//just copy it
				while ((len = zin.read(buffer)) != -1) {
					zout.write(buffer, 0, len);
				}
			}
			zout.closeEntry();
		}
		zin.close();
		zout.close();
		*/
	}

	public File manifestFile()
	{
		return this.manifestFile;
	}

	public List<Component> components()
	{
		return comps;
	}

	public Set<String> permissions()
	{
		return permissions;
	}

	public void setPackageName(String pkgName)
	{
		this.pkgName = pkgName;
	}

	public String getPackageName()
	{
		return this.pkgName;
	}
	
	public void setVersion(String version)
	{
		this.version = version;
	}

	public String getVersion()
	{
		return this.version;
	}

	public void setIconPath(String icon)
	{
		this.iconPath = icon;
	}
	
	public String getIconPath()
	{
		return iconPath;
	}

	private void filterDead(Set<String> compNames)
	{
		Set<String> compNamesAvailable = new HashSet();

		for (ClassDef classDef : dexFile.getClasses()) {
			String className = classDef.getType();
			if(className.charAt(0) == 'L'){
				int len = className.length();
				assert className.charAt(len-1) == ';';
				className = className.substring(1, len-1);
			}
			className = className.replace('/','.');
			String tmp = className;
			//String tmp = className.replace('$', '.');
			if(compNames.contains(tmp)) {
				compNamesAvailable.add(tmp);
				//System.out.println("%% "+tmp);
			}
		}

		compNames.clear();
		compNames.addAll(compNamesAvailable);
	}
	
	public String toString()
	{
		StringBuilder builder = new StringBuilder("App : {\n");

		builder.append("package: "+pkgName+"\n");
		builder.append("version: "+version+"\n");

		builder.append("comps : {\n");
		for(Component c : comps){
			builder.append("\t"+c.toString()+"\n");
		}
		builder.append("}\n");

		builder.append("perms: {\n");
		for(String perm : permissions){
			builder.append("\t"+perm+"\n");
		}
		builder.append("}\n");

		builder.append("}");
		return builder.toString();
	}
}
