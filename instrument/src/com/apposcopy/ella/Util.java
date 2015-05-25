package com.apposcopy.ella;

import org.jf.dexlib2.iface.Method;
import org.jf.dexlib2.iface.instruction.Instruction;
import org.jf.dexlib2.iface.instruction.OneRegisterInstruction;
import org.jf.dexlib2.iface.instruction.TwoRegisterInstruction;
import org.jf.dexlib2.iface.instruction.ThreeRegisterInstruction;
import org.jf.dexlib2.iface.instruction.FiveRegisterInstruction;
import org.jf.dexlib2.iface.instruction.RegisterRangeInstruction;
import org.jf.dexlib2.util.MethodUtil;

import java.io.File;
import java.security.MessageDigest;
import java.util.*;

/*
 * @author Saswat Anand
 */
public class Util
{
	//TODO: replace the use of this with the following one
	public static String signatureOf0(Method method)
	{
		StringBuilder builder = new StringBuilder();
		builder.append("<")
			.append(dottedClassName(method.getDefiningClass()))
			.append(": ")
			.append(method.getReturnType())
			.append(" ")
			.append(method.getName())
			.append("(");
		for(CharSequence paramType : method.getParameterTypes())
			builder.append(paramType);
		builder.append(")>");
		return builder.toString();
	}

	public static String signatureOf(Method method)
	{
		StringBuilder builder = new StringBuilder();
		builder
			.append(method.getName())
			.append("(");
		for(CharSequence paramType : method.getParameterTypes())
			builder.append(paramType);
		builder.append(")");
		builder.append(method.getReturnType());
		builder.append("@");
		builder.append(method.getDefiningClass());
		return builder.toString();
	}

    public static String dottedClassName(String className) 
	{
        className = className.substring(className.indexOf('L') + 1, className.indexOf(';'));
        className = className.replace('/', '.');
        return className;
    }
	
	public static List<Integer> getUsedRegistersNums(Instruction instruction) 
	{
		if(instruction instanceof FiveRegisterInstruction)
			return getUsedRegistersNums((FiveRegisterInstruction) instruction);		
		if(instruction instanceof ThreeRegisterInstruction)
			return getUsedRegistersNums((ThreeRegisterInstruction) instruction);
		if(instruction instanceof TwoRegisterInstruction)
			return getUsedRegistersNums((TwoRegisterInstruction) instruction);
		if(instruction instanceof OneRegisterInstruction)
			return getUsedRegistersNums((OneRegisterInstruction) instruction);
		if(instruction instanceof RegisterRangeInstruction)
			return getUsedRegistersNums((RegisterRangeInstruction) instruction);
		return null;
	}


	private static List<Integer> getUsedRegistersNums(OneRegisterInstruction instruction) 
	{
		List<Integer> regs = new ArrayList<Integer>();
		regs.add(instruction.getRegisterA());
		return regs;
	}

	private static List<Integer> getUsedRegistersNums(TwoRegisterInstruction instruction) 
	{
		List<Integer> regs = new ArrayList<Integer>();
		regs.add(instruction.getRegisterA());
		regs.add(instruction.getRegisterB());
		return regs;
	}

	private static List<Integer> getUsedRegistersNums(ThreeRegisterInstruction instruction) 
	{
		List<Integer> regs = new ArrayList<Integer>();
		regs.add(instruction.getRegisterA());
		regs.add(instruction.getRegisterB());
		regs.add(instruction.getRegisterC());
		return regs;
	}

	/**
	 * Return the indices used in the given instruction.
	 *
	 * @param instruction a range invocation instruction
	 * @return a list of register indices
	 */
	private static List<Integer> getUsedRegistersNums(RegisterRangeInstruction instruction) {
          List<Integer> regs = new ArrayList<Integer>();
          int start = instruction.getStartRegister();
          for (int i = start; i < start + instruction.getRegisterCount(); i++)
              regs.add(i);

          return regs;
	}
      
	/**
	 * Return the indices used in the given instruction.
	 *
	 * @param instruction a invocation instruction
	 * @return a list of register indices
	 */
	private static List<Integer> getUsedRegistersNums(FiveRegisterInstruction instruction) {
          int[] regs = {
              instruction.getRegisterC(),
              instruction.getRegisterD(),
              instruction.getRegisterE(),
              instruction.getRegisterF(),
              instruction.getRegisterG(),
          };
          List<Integer> l = new ArrayList<Integer>();
          for (int i = 0; i < instruction.getRegisterCount(); i++)
              l.add(regs[i]);
          return l;
      }
      
	private static String sha256(String base) {
		try{
		    MessageDigest digest = MessageDigest.getInstance("SHA-256");
		    byte[] hash = digest.digest(base.getBytes("UTF-8"));
		    StringBuffer hexString = new StringBuffer();

		    for (int i = 0; i < hash.length; i++) {
		        String hex = Integer.toHexString(0xff & hash[i]);
		        if(hex.length() == 1) hexString.append('0');
		        hexString.append(hex);
		    }

		    return hexString.toString();
		} catch(Exception ex){
		   throw new RuntimeException(ex);
		}
    }
    
    public static String appPathToAppId(String appPath) {
    	String appId = appPath.replace(File.separatorChar, '_');
    	if(appId.length() > 100) {
    		return sha256(appId);
    	} else {
    		return appId;
    	}
    }
}
