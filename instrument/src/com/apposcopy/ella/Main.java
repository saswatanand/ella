package com.apposcopy.ella;

import java.io.*;

import com.apposcopy.app.App;

public class Main
{
	public static void main(String[] args) throws IOException
	{
		boolean listClasses = false;
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
				config.load(args[i+1]);
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

		if(config.inputFile == null){
			System.out.println("New input file to process");
			return;
		}

		App app = App.readApp(config.inputFile, config.outDir(), config.apktoolJar);

		if(listClasses){
			app.listClasses();
			return;
		}

		app.updateDexFile(Instrument.instrument(app.dexFilePath()), config.outputFile);

		CoverageId.g().dump();
	}

}