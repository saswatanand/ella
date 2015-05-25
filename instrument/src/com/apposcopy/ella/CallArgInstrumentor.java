package com.apposcopy.ella;

import org.jf.dexlib2.Opcode;
import org.jf.dexlib2.iface.Method;
import org.jf.dexlib2.iface.reference.MethodReference;
import org.jf.dexlib2.iface.instruction.formats.*;
import org.jf.dexlib2.util.MethodUtil;

import com.apposcopy.ella.dexlib2builder.MutableMethodImplementation;
import com.apposcopy.ella.dexlib2builder.BuilderInstruction;
import com.apposcopy.ella.dexlib2builder.MethodLocation;
import com.apposcopy.ella.dexlib2builder.instruction.*;

import java.util.*;
import java.io.*;

/*
 * @author Saswat Anand
 */
public class CallArgInstrumentor extends MethodInstrumentor
{
	protected Map<String,List<Trio>> instrInfo = new HashMap();
	protected Map<Trio,BuilderInstruction> trioToInstruction;

	public CallArgInstrumentor()
	{
		super();
		readInstrInfo();
	}

	private static class Trio
	{
		int offset;
		int argIndex;
		int metadata;
		
		Trio(int offset, int argIndex, int metadata)
		{
			this.offset = offset;
			this.argIndex = argIndex;
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
				String[] tokens = line.split(" ");
				String methSig = tokens[0];
				int offset = Integer.parseInt(tokens[1]);
				int argIndex = Integer.parseInt(tokens[2]);
				int metadata = Integer.parseInt(tokens[3]);
				Trio trio = new Trio(offset, argIndex, metadata);
				
				List<Trio> trios = instrInfo.get(methSig);
				if(trios == null)
					instrInfo.put(methSig, trios = new ArrayList());
				trios.add(trio);
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
		trioToInstruction = new HashMap();

		List<Trio> trios = instrInfo.get(Util.signatureOf(method));
		if(trios == null)
			return;

		List<BuilderInstruction> instructions = code.getInstructions();
		for(BuilderInstruction i : instructions){
			MethodLocation loc = i.getLocation();
			int codeAddress = loc.getCodeAddress();
			for(Trio trio : trios){
				if(codeAddress == trio.offset){
					if(i instanceof Instruction11x){
						Opcode opcode = i.getOpcode();
						if(opcode == Opcode.MOVE_RESULT ||
						   opcode == Opcode.MOVE_RESULT_WIDE ||
						   opcode == Opcode.MOVE_RESULT_OBJECT){
							i = instructions.get(loc.getIndex()-1);
						}
					}
					if((i instanceof Instruction35c) || (i instanceof Instruction3rc)){
						trioToInstruction.put(trio, i);
					} 	
					else
						assert false : i.toString() + " "+i.getOpcode();
				}
			}
		}
	}

	protected void instrument(Method method, MutableMethodImplementation code, int probeRegister)
	{				
		for(Map.Entry<Trio,BuilderInstruction> entry : trioToInstruction.entrySet()){
			Trio trio = entry.getKey();
			BuilderInstruction instruction = entry.getValue();

			int argRegister = -1;
			boolean refType = false;
			boolean wideType = false;
			if(trio.argIndex >= 0){
				if(instruction instanceof Instruction35c){
					Instruction35c invkInstruction = (Instruction35c) instruction;
					Method callee = (Method) invkInstruction.getReference();
					CharSequence paramType;
					
					if(!MethodUtil.isStatic(callee) && trio.argIndex == 0)
						refType = true; //this parameter
					else {
						if(MethodUtil.isStatic(callee))
							paramType = callee.getParameterTypes().get(trio.argIndex);
						else
							paramType = callee.getParameterTypes().get(trio.argIndex-1);
						char firstChar = paramType.charAt(0);
						if(firstChar == 'J' || firstChar == 'D')
							wideType = true;
						else if(firstChar == '[' || firstChar == 'L')
							refType = true;
					}

					assert trio.argIndex < invkInstruction.getRegisterCount();
					switch(trio.argIndex){
					case 0: 
						argRegister = invkInstruction.getRegisterC();
						break;
					case 1: 
						argRegister = invkInstruction.getRegisterD();
					break;
					case 2:
						argRegister = invkInstruction.getRegisterE();
						break;
					case 3:
						argRegister = invkInstruction.getRegisterF();
						break;
					case 4:
						argRegister = invkInstruction.getRegisterG();
						break;
					default:
						assert false;
					}
				} else if(instruction instanceof Instruction3rc){
					Instruction3rc invkInstruction = (Instruction3rc) instruction;
					assert trio.argIndex < invkInstruction.getRegisterCount();
					Method callee = (Method) invkInstruction.getReference();
					argRegister = invkInstruction.getStartRegister();
					if(!MethodUtil.isStatic(callee) && trio.argIndex == 0)
						refType = true;
					else {
						int argCount = MethodUtil.isStatic(callee) ? 0 : 1;
						for(CharSequence paramType : callee.getParameterTypes()){
							int firstChar = paramType.charAt(0);
							argRegister += ((firstChar == 'J' || firstChar == 'D') ? 2 : 1);
							if(argCount == trio.argIndex){
								if(firstChar == 'J' || firstChar == 'D')
									wideType = true;
								else if(firstChar == '[' || firstChar == 'L')
									refType = true;
								break;
							}
						}
					}
				}
			}

			int index = instruction.getLocation().getIndex();
			index++;
			//check if the next instruction is a move-result*
			BuilderInstruction nextInstruction = code.getInstructions().get(index);
			int resultReg = -1;
			if(nextInstruction instanceof Instruction11x){
				Opcode opcode = nextInstruction.getOpcode();
				if(opcode == Opcode.MOVE_RESULT ||
				   opcode == Opcode.MOVE_RESULT_WIDE ||
				   opcode == Opcode.MOVE_RESULT_OBJECT){
					index++;
					if(trio.argIndex == -1){
						argRegister = ((Instruction11x) nextInstruction).getRegisterA();
						wideType = opcode ==  Opcode.MOVE_RESULT_WIDE;
						refType = opcode == Opcode.MOVE_RESULT_OBJECT;
					}
				}
			}
			
			assert argRegister >= 0;
			assert !wideType || !refType;
			
			//insert the probe statement at index
			int probeIdNumBits = Instrument.numBits(probeRegister);
			if(probeIdNumBits <= 8)
				code.addInstruction(index, new BuilderInstruction31i(Opcode.CONST, probeRegister, trio.metadata));
			else
				throw new RuntimeException("TODO: Did not find a register in the range [0..2^8] to store the probe id");

			if(probeIdNumBits == 4 && Instrument.numBits(argRegister) == 4){
				code.addInstruction(index+1, new BuilderInstruction35c(Opcode.INVOKE_STATIC, 2, probeRegister, argRegister, 0, 0, 0, probeMethRef));
			} else {
				//move the content of argRegister to the register with index probeRegister+1
				Opcode opcode;
				int numArgRegs = 2;
				if(wideType){
					opcode = Opcode.MOVE_WIDE;
					numArgRegs = 3;
				} else if(refType)
					opcode = Opcode.MOVE_OBJECT;
				else
					opcode = Opcode.MOVE;
				code.addInstruction(index+1, Instrument.moveRegister(probeRegister+1, argRegister, opcode));
				code.addInstruction(index+2, new BuilderInstruction3rc(Opcode.INVOKE_STATIC_RANGE, probeRegister, numArgRegs, probeMethRef));
			}
		}
	}
}
