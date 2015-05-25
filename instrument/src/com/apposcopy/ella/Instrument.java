package com.apposcopy.ella;

import java.util.*;
import java.util.jar.*;
import java.util.regex.Pattern;
import java.io.*;
import java.net.URL;
import java.net.URLClassLoader;
import java.lang.reflect.InvocationTargetException;

import org.jf.dexlib2.Opcode;
import org.jf.dexlib2.DexFileFactory;
import org.jf.dexlib2.dexbacked.DexBackedDexFile;
import org.jf.dexlib2.iface.ClassDef;
import org.jf.dexlib2.iface.DexFile;
import org.jf.dexlib2.iface.Method;
import org.jf.dexlib2.iface.Field;
import org.jf.dexlib2.iface.MethodImplementation;
import org.jf.dexlib2.iface.reference.MethodReference;
import org.jf.dexlib2.iface.instruction.formats.*;
import org.jf.dexlib2.immutable.ImmutableClassDef;
import org.jf.dexlib2.immutable.ImmutableMethod;
import org.jf.dexlib2.immutable.reference.ImmutableStringReference;
import org.jf.dexlib2.util.MethodUtil;
import com.apposcopy.ella.dexlib2builder.MutableMethodImplementation;
import com.apposcopy.ella.dexlib2builder.BuilderInstruction;
import com.apposcopy.ella.dexlib2builder.instruction.*;

import com.google.common.collect.Lists;

/*
 * @author Saswat Anand
 */
public class Instrument
{
	private DexFile dexFile;
	private List<MethodInstrumentor> methInstrumentors = new ArrayList();
	private String recorderClassName;

	public String instrument(String inputFile) throws IOException
	{
		File mergedFile = mergeEllaRuntime(inputFile);

		this.dexFile = DexFileFactory.loadDexFile(mergedFile, 15);
		
		initMethInstrumentors();
 
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
							MethodImplementation newImplementation = null;
							//if(!method.getName().equals("<init>"))
							newImplementation = instrument(method);
							
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

	public MethodImplementation instrument(Method method)
	{
		MethodImplementation originalCode = method.getImplementation();
        if (originalCode == null)
            throw new RuntimeException("error: no code for method "+ method.getName());

		int numRegisters = originalCode.getRegisterCount();
        int numParameterRegisters = MethodUtil.getParameterRegisterCount(method);
		int numNonParameterRegisters = numRegisters - numParameterRegisters;

		//System.out.println("numRegisters = "+numRegisters+" numParameterRegisters = "+ numParameterRegisters);

		int numParamRegistersToClone = numParameterRegisters;
		
		//if(numParamRegistersToClone > 0)
		//	System.out.println("numParamRegistersToClone: "+numParamRegistersToClone);

		// add (numParamRegistersToClone + 1) new registers
		// +1 represents the register that we will use to store coverage id
		// at [numNonParameterRegisters...(numNonParameterRegister+numParamRegistersToClone)]
		
		int numRegistersToAdd = 0;
		for(MethodInstrumentor methInstrumentor : methInstrumentors)
			numRegistersToAdd = Math.max(numRegistersToAdd, methInstrumentor.numRegistersToAdd());

		int numNewRegisters = numParamRegistersToClone + numRegistersToAdd;

        MutableMethodImplementation code = new MutableMethodImplementation(originalCode, numNewRegisters + numRegisters);

		for(MethodInstrumentor methInstrumentor : methInstrumentors)
			methInstrumentor.preinstrument(method, code);

		int dest = numNonParameterRegisters;
		BuilderInstruction newInstruction;
		
		if(!MethodUtil.isStatic(method)){
			newInstruction = moveRegister(dest, dest + numNewRegisters, Opcode.MOVE_OBJECT);
			code.addInstruction(0, newInstruction);
			dest++;
		}

		for(CharSequence paramType : method.getParameterTypes()){
			int firstChar = paramType.charAt(0);
            if (firstChar == 'J' || firstChar == 'D') {
				newInstruction = moveRegister(dest, dest + numNewRegisters, Opcode.MOVE_WIDE);
				dest += 2;
			} else {
				if(firstChar == '[' || firstChar == 'L')
					newInstruction = moveRegister(dest, dest + numNewRegisters, Opcode.MOVE_OBJECT);
				else
					newInstruction = moveRegister(dest, dest + numNewRegisters, Opcode.MOVE);
				dest++;
			}
			code.addInstruction(0, newInstruction);
		}

		for(MethodInstrumentor methInstrumentor : methInstrumentors)
			methInstrumentor.instrument(method, code, dest);

        return code;
    }

	private Pattern readExcludePatterns() throws IOException
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

	private void initMethInstrumentors()
	{
		for(String instrumentorClassName : Config.g().instrumentorClassNames.split(",")){
			try{
				MethodInstrumentor mi = (MethodInstrumentor) Class.forName(instrumentorClassName).newInstance();
				methInstrumentors.add(mi);
				mi.setProbeMethRef(probeMethodRef(mi.probeMethName()));
				if(recorderClassName != null)
					assert recorderClassName.equals(mi.recorderClassName());
				recorderClassName = mi.recorderClassName();
			} catch(ClassNotFoundException e){
				throw new Error(e);
			} catch(InstantiationException e){
				throw new Error(e);
			} catch(IllegalAccessException e){
				throw new Error(e);
			} 
		}
	}

	private MethodReference probeMethodRef(String probeMethName)
	{
		for (ClassDef classDef: dexFile.getClasses()) {
			if(classDef.getType().startsWith("Lcom/apposcopy/ella/runtime/Ella;")){  
				for (Method method: classDef.getMethods()) {
					String name = method.getName();
					if(name.equals(probeMethName))
						return method;
				}
			}
		}
		return null;	
	}

	private MutableMethodImplementation injectId(MethodImplementation code, ClassDef classDef)
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
			if(f.getName().equals("recorderClassName")){
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

		assert recorderClassName != null;
		newCode.addInstruction(0, new BuilderInstruction21c(Opcode.SPUT_OBJECT, regCount, covRecorderClassNameField));
		newCode.addInstruction(0, new BuilderInstruction21c(Opcode.CONST_STRING, regCount, new ImmutableStringReference(recorderClassName)));

		newCode.addInstruction(0, new BuilderInstruction21c(Opcode.SPUT_OBJECT, regCount, idField));
		newCode.addInstruction(0, new BuilderInstruction21c(Opcode.CONST_STRING, regCount, new ImmutableStringReference(Config.g().appId)));

		newCode.addInstruction(0, new BuilderInstruction21c(Opcode.SPUT_OBJECT, regCount, uploadUrlField));
		newCode.addInstruction(0, new BuilderInstruction21c(Opcode.CONST_STRING, regCount, new ImmutableStringReference(Config.g().tomcatUrl+"/ella/uploadcoverage")));

		return newCode;
	}

	public static BuilderInstruction moveRegister(int dest, int src, Opcode opcode)
	{
		int destNumBits = numBits(dest);
		int srcNumBits = numBits(src);

		if(destNumBits == 4)
			if(srcNumBits == 4)
				return new BuilderInstruction12x(opcode, dest, src);
			else
				return new BuilderInstruction22x(opcode_FROM16(opcode), dest, src);
		if(destNumBits == 8)
			return new BuilderInstruction22x(opcode_FROM16(opcode), dest, src);
		if(destNumBits == 16)
			return new BuilderInstruction32x(opcode_16(opcode), dest, src);
		throw new RuntimeException("Unexpected: "+destNumBits+" "+srcNumBits);
	}

	public static Opcode opcode_FROM16(Opcode opcode)
	{
		switch(opcode){
		case MOVE:
			return Opcode.MOVE_FROM16;
		case MOVE_OBJECT:
			return Opcode.MOVE_OBJECT_FROM16;
		case MOVE_WIDE:
			return Opcode.MOVE_WIDE_FROM16;
		default:
			throw new RuntimeException("unexpected "+ opcode);
		}
	}

	public static Opcode opcode_16(Opcode opcode)
	{
		switch(opcode){
		case MOVE:
			return Opcode.MOVE_16;
		case MOVE_OBJECT:
			return Opcode.MOVE_OBJECT_16;
		case MOVE_WIDE:
			return Opcode.MOVE_WIDE_16;
		default:
			throw new RuntimeException("unexpected "+ opcode);
		}
	}

	public static int numBits(int reg)
	{
		if(reg < 0x0000000F)
			return 4;
		else if(reg < 0x000000FF)
			return 8;
		else if(reg < 0x0000FFFF)
			return 16;
		else 
			throw new RuntimeException("More than 16 bits is required to encode register "+reg);
	}

	private File mergeEllaRuntime(String inputFile) throws IOException
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
