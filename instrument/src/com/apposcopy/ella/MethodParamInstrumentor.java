package com.apposcopy.ella;

import org.jf.dexlib2.Opcode;
import org.jf.dexlib2.iface.Method;
import org.jf.dexlib2.iface.instruction.formats.*;
import org.jf.dexlib2.util.MethodUtil;

import com.apposcopy.ella.dexlib2builder.MutableMethodImplementation;
import com.apposcopy.ella.dexlib2builder.BuilderInstruction;
import com.apposcopy.ella.dexlib2builder.instruction.*;

import java.util.*;
import java.io.*;

/*
 * @author Saswat Anand
 */
public class MethodParamInstrumentor extends MethodInstrumentor
{
	protected Map<String,List<Pair>> instrInfo = new HashMap();

	public MethodParamInstrumentor()
	{
		super();
		readInstrInfo();
	}

	private static class Pair
	{
		int paramIndex;
		int metadata;
		
		Pair(int paramIndex, int metadata)
		{
			this.paramIndex = paramIndex;
			this.metadata = metadata;
		}
	}

	private void readInstrInfo()
	{
		String instrInfoFileName = Config.g().extras.get("ella.iinfo");
		try{
			BufferedReader reader = new BufferedReader(new FileReader(instrInfoFileName));
			String line;
			while((line = reader.readLine()) != null){
				line = line.trim();
				if(!line.startsWith("METHPARAM "))
					continue;
				String[] tokens = line.split(" ");
				String methSig = tokens[1];
				int paramIndex = Integer.parseInt(tokens[2]);
				int metadata = Integer.parseInt(tokens[3]);
				Pair pair = new Pair(paramIndex, metadata);
				
				List<Pair> pairs = instrInfo.get(methSig);
				if(pairs == null)
					instrInfo.put(methSig, pairs = new LinkedList());
				pairs.add(pair);
			}
			reader.close();
		}catch(IOException e){
			throw new Error(e);
		}
	}

	protected int numRegistersToAdd()
	{
		return 3;
	}

	protected String probeMethName()
	{
		return "v";
	}

	protected String recorderClassName()
	{
		return "com.apposcopy.ella.runtime.ValueRecorder";
	}

	protected void preinstrument(Method method, MutableMethodImplementation code)
	{
	}

	protected void instrument(Method method, MutableMethodImplementation code, int probeRegister)
	{				
		List<Pair> pairs = instrInfo.get(Util.signatureOf(method));
		if(pairs == null)
			return;

		int paramRegister = code.getRegisterCount() - MethodUtil.getParameterRegisterCount(method);
		
		int paramIndex = 0;
		if(!MethodUtil.isStatic(method)){
			for(Pair pair : pairs){
				if(pair.paramIndex == paramIndex){
					addInstruction(code, paramRegister, probeRegister, pair.metadata, false, true);
				}
			}
			paramRegister++;
			paramIndex++;
		}

		for(CharSequence paramType : method.getParameterTypes()){
			boolean refType = false;
			boolean wideType = false;
			int firstChar = paramType.charAt(0);
            if (firstChar == 'J' || firstChar == 'D') 
				wideType = true;
			else if(firstChar == '[' || firstChar == 'L')
				refType = true;
			for(Pair pair : pairs){
				if(pair.paramIndex == paramIndex){
					addInstruction(code, paramRegister, probeRegister, pair.metadata, wideType, refType);
				}
			}
			paramRegister += (wideType ? 2 : 1);
			paramIndex++;
		}
	}
	
	private void addInstruction(MutableMethodImplementation code, int paramRegister, int probeRegister, int metadata, boolean wideType, boolean refType)
	{
		int probeIdNumBits = Instrument.numBits(probeRegister);
		if(probeIdNumBits <= 8)
			code.addInstruction(0, new BuilderInstruction31i(Opcode.CONST, probeRegister, metadata));
		else
				throw new RuntimeException("TODO: Did not find a register in the range [0..2^8] to store the probe id");
		
		if(probeIdNumBits == 4 && Instrument.numBits(paramRegister) == 4){
			code.addInstruction(1, new BuilderInstruction35c(Opcode.INVOKE_STATIC, 2, probeRegister, paramRegister, 0, 0, 0, probeMethRef));
		} else {
			//move the content of paramRegister to the register with index probeRegister+1
			Opcode opcode;
			int numArgRegs = 2;
			if(wideType){
				opcode = Opcode.MOVE_WIDE;
				numArgRegs = 3;
			} else if(refType)
				opcode = Opcode.MOVE_OBJECT;
			else
				opcode = Opcode.MOVE;
			code.addInstruction(1, Instrument.moveRegister(probeRegister+1, paramRegister, opcode));
			code.addInstruction(2, new BuilderInstruction3rc(Opcode.INVOKE_STATIC_RANGE, probeRegister, numArgRegs, probeMethRef));
		}
	}
}
