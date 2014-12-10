package com.apposcopy.ella;

import java.util.*;
import java.util.jar.*;
import java.io.*;
import java.net.URL;
import java.net.URLClassLoader;
import java.lang.reflect.InvocationTargetException;

import org.jf.dexlib2.DexFileFactory;
import org.jf.dexlib2.dexbacked.DexBackedDexFile;
import org.jf.dexlib2.iface.ClassDef;
import org.jf.dexlib2.iface.DexFile;
import org.jf.dexlib2.iface.Method;
import org.jf.dexlib2.iface.reference.MethodReference;
import org.jf.dexlib2.iface.MethodImplementation;
import org.jf.dexlib2.immutable.ImmutableClassDef;
import org.jf.dexlib2.immutable.ImmutableMethod;

import com.google.common.collect.Lists;

public class Instrument
{
	static MethodReference probeMethRef;

	public static String instrument(String inputFile, String ellaRuntime, String ellaSettingsFile) throws IOException
	{
		return instrument(inputFile, null, ellaRuntime, ellaSettingsFile);
	}

	public static String instrument(String inputFile, String outputFile, String ellaRuntime, String ellaSettingsFile) throws IOException
	{
		File mergedFile = mergeEllaRuntime(inputFile, ellaRuntime, ellaSettingsFile);

		DexFile dexFile = DexFileFactory.loadDexFile(mergedFile, 15);
		
		probeMethRef = findProbeMethRef(dexFile);
 
        final List<ClassDef> classes = Lists.newArrayList();
 
        for (ClassDef classDef: dexFile.getClasses()) {
            List<Method> methods = Lists.newArrayList(); 
            boolean modifiedMethod = false;

			if(!classDef.getType().startsWith("Lcom/apposcopy/ella/runtime/")){  
				System.out.println("processing class *"+classDef.getType());
				for (Method method: classDef.getMethods()) {
					
					String name = method.getName();
					System.out.println("processing method '"+method.getDefiningClass()+": "+method.getReturnType()+ " "+ method.getName() + " p: " +  method.getParameters() + "'");
					
					MethodImplementation implementation = method.getImplementation();
					if (implementation != null) {
						MethodTransformer tr = new MethodTransformer(method);
						MethodImplementation newImplementation = null;
						//if(!method.getName().equals("<init>"))
						newImplementation = tr.transform();
						
						if(newImplementation != null){
							modifiedMethod = true;
							methods.add(new ImmutableMethod(
															method.getDefiningClass(),
															method.getName(),
															method.getParameters(),
															method.getReturnType(),
															method.getAccessFlags(),
															method.getAnnotations(),
															newImplementation));
						} else 
						methods.add(method);
					} else
						methods.add(method);
				}
			}

            if (!modifiedMethod) {
                classes.add(classDef);
            } else {
                classes.add(new ImmutableClassDef(
												  classDef.getType(),
												  classDef.getAccessFlags(),
												  classDef.getSuperclass(),
												  classDef.getInterfaces(),
												  classDef.getSourceFile(),
												  classDef.getAnnotations(),
												  classDef.getFields(),
												  methods));
            }
        }

		if(outputFile == null){
			File outputDexFile = File.createTempFile("outputclasses", ".dex");
			outputFile = outputDexFile.getAbsolutePath();
		}
 
        DexFileFactory.writeDexFile(outputFile, new DexFile() {
				@Override public Set<? extends ClassDef> getClasses() {
					return new AbstractSet<ClassDef>() {
						@Override public Iterator<ClassDef> iterator() {
							return classes.iterator();
						}
 
						@Override public int size() {
							return classes.size();
						}
					};
				}
			});
		return outputFile;
	}
	
	static MethodReference findProbeMethRef(DexFile dexFile)
	{
		for (ClassDef classDef: dexFile.getClasses()) {
			if(classDef.getType().startsWith("Lcom/apposcopy/ella/runtime/Ella;")){  
				for (Method method: classDef.getMethods()) {
					String name = method.getName();
					if(name.equals("m"))
						return method;
				}
			}
		}
		return null;	
	}

	static File mergeEllaRuntime(String inputFile, String ellaRuntime, String ellaSettingsFile) throws IOException
	{
		Properties props = new Properties();
		props.load(new FileInputStream(ellaSettingsFile));
		final String dxJar = props.getProperty("dx.jar");
		if(dxJar == null)
			throw new RuntimeException("Variable dx.jar not set");
		try{
			//DexMerger.main(new String[]{mergedDex.getAbsolutePath(), inputFile, ellaRuntime});
			URLClassLoader loader = new URLClassLoader(new URL[]{new URL("file://"+dxJar)});
			Class dexMergerClass = loader.loadClass("com.android.dx.merge.DexMerger");
			java.lang.reflect.Method mainMethod = dexMergerClass.getDeclaredMethod("main", (new String[0]).getClass());

			File mergedDex = File.createTempFile("ella",".dex");
			mainMethod.invoke(null, (Object) new String[]{mergedDex.getAbsolutePath(), inputFile, ellaRuntime});
			return mergedDex;
		} catch(ClassNotFoundException e){
			throw new Error(e);
		} catch(NoSuchMethodException e){
			throw new Error(e);
		} catch(IllegalAccessException e){
			throw new Error(e);
		} catch(InvocationTargetException e){
			throw new Error(e);
		} 
	}
}
