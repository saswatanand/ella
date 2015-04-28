package com.apposcopy.ella;

import java.util.*;
import java.util.jar.*;
import java.util.regex.Pattern;
import java.io.*;
import java.net.URL;
import java.net.URLClassLoader;
import java.lang.reflect.InvocationTargetException;

import org.jf.dexlib2.DexFileFactory;
import org.jf.dexlib2.dexbacked.DexBackedDexFile;
import org.jf.dexlib2.iface.ClassDef;
import org.jf.dexlib2.iface.DexFile;
import org.jf.dexlib2.iface.Method;
import org.jf.dexlib2.iface.Field;
import org.jf.dexlib2.iface.reference.MethodReference;
import org.jf.dexlib2.iface.MethodImplementation;
import org.jf.dexlib2.immutable.ImmutableClassDef;
import org.jf.dexlib2.immutable.ImmutableMethod;
import org.jf.dexlib2.immutable.reference.ImmutableStringReference;
import org.jf.dexlib2.iface.instruction.formats.*;
import org.jf.dexlib2.Opcode;
import com.apposcopy.ella.dexlib2builder.MutableMethodImplementation;
import com.apposcopy.ella.dexlib2builder.BuilderInstruction;
import com.apposcopy.ella.dexlib2builder.instruction.*;


import com.google.common.collect.Lists;

public class Instrument
{
	static MethodReference probeMethRef;

	public static String instrument(String inputFile) throws IOException
	{
		File mergedFile = mergeEllaRuntime(inputFile);

		DexFile dexFile = DexFileFactory.loadDexFile(mergedFile, 15);
		
		probeMethRef = findProbeMethRef(dexFile);
 
		Pattern excludePattern = readExcludePatterns();

        final List<ClassDef> classes = Lists.newArrayList();
 
        for (ClassDef classDef: dexFile.getClasses()) {
            List<Method> methods = Lists.newArrayList(); 
            boolean modifiedMethod = false;
			String className = Util.dottedClassName(classDef.getType());
			if(!className.startsWith("com.apposcopy.ella.runtime")){ 
				if(excludePattern == null || !excludePattern.matcher(className).matches()){
					System.out.println("Instrumenting class "+className);
					for (Method method: classDef.getMethods()) {
						String name = method.getName();
						//System.out.println("processing method '"+method.getDefiningClass()+": "+method.getReturnType()+ " "+ method.getName() + " p: " +  method.getParameters() + "'");
					
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
				} else
					System.out.println("Skipping instrumentation of class "+className);
			} else if(className.equals("com.apposcopy.ella.runtime.Ella")){
				modifiedMethod = true;
				for (Method method: classDef.getMethods()) {
					String name = method.getName();
					if(name.equals("<clinit>")){
						MutableMethodImplementation newCode = injectId(method.getImplementation(), classDef);
						methods.add(new ImmutableMethod(
														method.getDefiningClass(),
														method.getName(),
														method.getParameters(),
														method.getReturnType(),
														method.getAccessFlags(),
														method.getAnnotations(),
														newCode));
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

		File outputDexFile = File.createTempFile("ellaoutputclasses", ".dex");
		String outputFile = outputDexFile.getAbsolutePath();
 
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

	static Pattern readExcludePatterns() throws IOException
	{
		String fileName = Config.g().excludeFile;
		if(fileName == null)
			return null;
		File f = new File(fileName);
		if(!f.exists()){
			System.out.println("Exclusion file "+fileName+" does not exist.");
			return null;
		}
		BufferedReader reader = new BufferedReader(new FileReader(f));
		String line;
		StringBuilder builder = new StringBuilder();
		boolean first = true;
		while((line = reader.readLine()) != null){
			if(first)
				first = false;
			else
				builder.append("|");
			builder.append(line);
		}
		reader.close();
		return Pattern.compile(builder.toString());
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

	static MutableMethodImplementation injectId(MethodImplementation code, ClassDef classDef)
	{
		int regCount = code.getRegisterCount();
		
		//get the reference to the "private static String id" field
		Field idField = null;
		for(Field f : classDef.getStaticFields()){
			if(f.getName().equals("id")){
				idField = f;
				break;
			}
		}

		//get the reference to the "private static String covRecorderClassName" field
		Field covRecorderClassNameField = null;
		for(Field f : classDef.getStaticFields()){
			if(f.getName().equals("covRecorderClassName")){
				covRecorderClassNameField = f;
				break;
			}
		}

		//get the reference to the "private static String covRecorderClassName" field
		Field uploadUrlField = null;
		for(Field f : classDef.getStaticFields()){
			if(f.getName().equals("uploadUrl")){
				uploadUrlField = f;
				break;
			}
		}		

		MutableMethodImplementation newCode = new MutableMethodImplementation(code, regCount+1);

		newCode.addInstruction(0, new BuilderInstruction21c(Opcode.SPUT_OBJECT, regCount, covRecorderClassNameField));
		newCode.addInstruction(0, new BuilderInstruction21c(Opcode.CONST_STRING, regCount, new ImmutableStringReference(Config.g().recorderClassName)));

		newCode.addInstruction(0, new BuilderInstruction21c(Opcode.SPUT_OBJECT, regCount, idField));
		newCode.addInstruction(0, new BuilderInstruction21c(Opcode.CONST_STRING, regCount, new ImmutableStringReference(Config.g().appId)));

		newCode.addInstruction(0, new BuilderInstruction21c(Opcode.SPUT_OBJECT, regCount, uploadUrlField));
		newCode.addInstruction(0, new BuilderInstruction21c(Opcode.CONST_STRING, regCount, new ImmutableStringReference(Config.g().tomcatUrl+"/ella/uploadcoverage")));

		return newCode;
	}

	static File mergeEllaRuntime(String inputFile) throws IOException
	{
		String dxJar = Config.g().dxJar;
		if(dxJar == null)
			throw new RuntimeException("Variable dx.jar not set");
		try{
			//DexMerger.main(new String[]{mergedDex.getAbsolutePath(), inputFile, ellaRuntime});
			URLClassLoader loader = new URLClassLoader(new URL[]{new URL("file://"+dxJar)});
			Class dexMergerClass = loader.loadClass("com.android.dx.merge.DexMerger");
			java.lang.reflect.Method mainMethod = dexMergerClass.getDeclaredMethod("main", (new String[0]).getClass());

			File mergedDex = File.createTempFile("ella",".dex");
			mainMethod.invoke(null, (Object) new String[]{mergedDex.getAbsolutePath(), inputFile, Config.g().ellaRuntime});
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
