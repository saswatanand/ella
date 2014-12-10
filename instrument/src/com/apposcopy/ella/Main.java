package com.apposcopy.ella;

import java.util.*;
import java.util.zip.*;
import java.io.*;

import org.jf.dexlib2.DexFileFactory;
import org.jf.dexlib2.iface.ClassDef;
import org.jf.dexlib2.iface.DexFile;


public class Main
{
	public static void main(String[] args) throws IOException
	{
		boolean listClasses = false;
		String excludeFile = null;
		String inputFile = null;
		String outputFile = null;
		String ellaRuntime = null;
		String ellaSettingsFile = null;

		int i = 0;
		while(i < args.length){
			String a = args[i];
			if(a.equals("-l")){
				listClasses = true;
			} else if(a.equals("-x")){
				excludeFile = args[i+1];
				i++;
			} else if(a.equals("-ella.runtime")){
				ellaRuntime = args[i+1];
				i++;
			} else if(a.equals("-ella.settings")){
				ellaSettingsFile = args[i+1];
				i++;
			} else if(inputFile == null)
				inputFile = a;
			else{
				assert outputFile == null;
				outputFile = a;
			}
			i++;
		}

		if(inputFile == null){
			System.out.println("New input file to process");
			return;
		}

		if(listClasses){
			listClasses(inputFile);
			return;
		}

		instrument(inputFile, outputFile, ellaRuntime, ellaSettingsFile);
	}

	static void instrument(String inputFile, String outputFile, String ellaRuntime, String ellaSettingsFile) throws IOException
	{
		if(inputFile.endsWith(".apk")){
			assert outputFile.endsWith(".apk");
			String inputDexFile = extractClassesDex(inputFile);
			String outputDexFile = Instrument.instrument(inputDexFile, ellaRuntime, ellaSettingsFile);
			writeOutput(outputDexFile, inputFile, outputFile);

		} else {
			assert inputFile.endsWith(".dex") && outputFile.endsWith(".dex");
			Instrument.instrument(inputFile, outputFile, ellaRuntime, ellaSettingsFile);
		}
	}

	static void writeOutput(String outputDexFile, String inputApkFile, String outputApkFile) throws IOException
	{
		ZipInputStream zin = new ZipInputStream(new BufferedInputStream(new FileInputStream(inputApkFile)));
		ZipOutputStream zout = new ZipOutputStream(new FileOutputStream(outputApkFile));
		ZipEntry ze = null;
		byte[] buffer = new byte[8192];
		int len;
		while ((ze = zin.getNextEntry()) != null) {
			String entryName = ze.getName();
			if(entryName.startsWith("META-INF"))
				continue;
			zout.putNextEntry(new ZipEntry(entryName));
			if (!entryName.equals("classes.dex")) {
				//just copy it
				while ((len = zin.read(buffer)) != -1) {
					zout.write(buffer, 0, len);
				}
			} else {
				//write the instrumented classes.dex
				BufferedInputStream bin = new BufferedInputStream(new FileInputStream(outputDexFile));
				while ((len = bin.read(buffer)) != -1) {
					zout.write(buffer, 0, len);
				}
				bin.close();
			}
			zout.closeEntry();
		}
		zin.close();
		zout.close();
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

	static void listClasses(String inputFile) throws IOException
	{
		DexFile dexFile = DexFileFactory.loadDexFile(inputFile, 15);
		for (ClassDef classDef: dexFile.getClasses()) {
			String className = Util.dottedClassName(classDef.getType());
			System.out.println(className);
		}
	}
}