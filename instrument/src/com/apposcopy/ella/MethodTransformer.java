package com.apposcopy.ella;

import org.jf.dexlib2.iface.Method;
import org.jf.dexlib2.iface.MethodImplementation;
import org.jf.dexlib2.iface.instruction.Instruction;
import org.jf.dexlib2.iface.instruction.OneRegisterInstruction;
import org.jf.dexlib2.iface.instruction.TwoRegisterInstruction;
import org.jf.dexlib2.iface.instruction.ThreeRegisterInstruction;
import org.jf.dexlib2.iface.instruction.FiveRegisterInstruction;
import org.jf.dexlib2.iface.instruction.RegisterRangeInstruction;
import org.jf.dexlib2.util.MethodUtil;
import org.jf.dexlib2.iface.instruction.formats.*;
import org.jf.dexlib2.Opcode;
import com.apposcopy.ella.dexlib2builder.MutableMethodImplementation;
import com.apposcopy.ella.dexlib2builder.BuilderInstruction;
import com.apposcopy.ella.dexlib2builder.instruction.*;

import java.util.*;

public class MethodTransformer
{
	protected final Method method;
	private int numRegisters;
	private int numParameterRegisters;
	private int numNonParameterRegisters;
	private int numParamRegistersToClone = 0;
	private int numNewRegisters = 0;
	private Set<Integer> constrainedParamRegisters = new HashSet();

	public MethodTransformer(Method method)
	{
		this.method = method;
	}
	
	public MethodImplementation transform()
	{
		MethodImplementation code = method.getImplementation();
        if (code == null)
            throw new RuntimeException("error: no code for method "+ method.getName());

		numRegisters = code.getRegisterCount();
        numParameterRegisters = MethodUtil.getParameterRegisterCount(method);
		numNonParameterRegisters = numRegisters - numParameterRegisters;

		System.out.println("numRegisters = "+numRegisters+" numParameterRegisters = "+ numParameterRegisters);

		//for (Instruction instruction : code.getInstructions()) {
		//	visit(instruction);
        //}

		numParamRegistersToClone = numParameterRegisters;
		
		//if(numParamRegistersToClone > 0)
		//	System.out.println("numParamRegistersToClone: "+numParamRegistersToClone);

		// add (numParamRegistersToClone + 1) new registers
		// +1 represents the register that we will use to store coverage id
		// at [numNonParameterRegisters...(numNonParameterRegister+numParamRegistersToClone)]
		
		numNewRegisters = numParamRegistersToClone + 1;

		/*
		for (Instruction instruction : code.getInstructions()) {
			if(!update(instruction)){
				System.out.println("notok");
				System.out.println(instruction.getOpcode().name);
				List<Integer> regs = Util.getUsedRegistersNums(instruction);
				System.out.print("regs: ");
				for(Integer r : regs){
					System.out.print(r+" ");
				}
				System.out.println("");
				
			}
        }
		*/

		return modifyMethod(code);
	}

	private MethodImplementation modifyMethod(MethodImplementation implementation) 
	{
        MutableMethodImplementation impl = new MutableMethodImplementation(implementation, numNewRegisters + numRegisters);
		/*
		List<Instruction> instructions = mutableImplementation.getInstructions();
 
        for (int i=0; i<instructions.size(); i++) {
            Instruction instruction = instructions.get(i);
			Instruction newInstruction = modifyInstruction(instruction);
			mutableImplementation.replaceInstruction(i, newInstruction);
		} 
		*/
		
		//assuming for now that we clone every parameter register
		//for(int i = 0; i < numParameterRegisters; i++){

		int dest = numNonParameterRegisters;
		
		if(!MethodUtil.isStatic(method)){
			impl.addInstruction(0, moveRegister(dest, dest + numNewRegisters, Opcode.MOVE_OBJECT));
			dest++;
		}

		for(CharSequence paramType : method.getParameterTypes()){
			BuilderInstruction newInstruction;
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
			impl.addInstruction(0, newInstruction);
		}

		int id = CoverageId.g().idFor(Util.signatureOf(method));
		int probeIdNumBits = numBits(dest);
		if(probeIdNumBits == 4){
			impl.addInstruction(0, new BuilderInstruction35c(Opcode.INVOKE_STATIC, 1, dest, 0, 0, 0, 0, Instrument.probeMethRef));
		} else {
			impl.addInstruction(0, new BuilderInstruction3rc(Opcode.INVOKE_STATIC_RANGE, dest, 1, Instrument.probeMethRef));
		}
		if(probeIdNumBits <= 8)
			impl.addInstruction(0, new BuilderInstruction31i(Opcode.CONST, dest, id));
		else
			throw new RuntimeException("TODO: Did not find a 8-bit reg to store the probe id");
        return impl;
    }

	private BuilderInstruction moveRegister(int dest, int src, Opcode opcode)
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

	private Opcode opcode_FROM16(Opcode opcode)
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

	private Opcode opcode_16(Opcode opcode)
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

	private int numBits(int reg)
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

	private boolean isInRange(int reg, int numBits)
	{
		assert numBits == 4 || numBits == 8 || numBits == 16 : numBits+"";
		int mask = 0xFFFFFFFF << numBits;
		return (reg & mask) == 0;
	}

	private boolean isInRange(FiveRegisterInstruction i, int regCWidth, int regDWidth, int regEWidth, int regFWidth, int regGWidth)
	{
 		return 
			isInRange(newRegister(i.getRegisterC()), regCWidth) && 
			isInRange(newRegister(i.getRegisterD()), regDWidth) &&
			isInRange(newRegister(i.getRegisterE()), regEWidth) && 
			isInRange(newRegister(i.getRegisterF()), regFWidth) &&
			isInRange(newRegister(i.getRegisterG()), regGWidth);
	}

	private boolean isInRange(ThreeRegisterInstruction i, int regAWidth, int regBWidth, int regCWidth)
	{
		return 
			isInRange(newRegister(i.getRegisterA()), regAWidth) && 
			isInRange(newRegister(i.getRegisterB()), regBWidth) &&
			isInRange(newRegister(i.getRegisterC()), regCWidth);
	}

	private boolean isInRange(TwoRegisterInstruction i, int regAWidth, int regBWidth)
	{
		int regA = i.getRegisterA();
		int regB = i.getRegisterB();
		return isInRange(newRegister(regA), regAWidth) && isInRange(newRegister(regB), regBWidth);
	}

	private boolean isInRange(OneRegisterInstruction i, int regAWidth)
	{
		return isInRange(newRegister(i.getRegisterA()), regAWidth);
	}

	private boolean isInRange(RegisterRangeInstruction i, int regWidth)
	{
		return isInRange(newRegister(i.getStartRegister()), regWidth);
	}
	//abcdefghijklmopqrstuvwxyz
	//rrishi baba mama
	private boolean update(Instruction i)
	{
		if(i instanceof Instruction12x)
			return isInRange((TwoRegisterInstruction) i, 4, 4);
		else if(i instanceof Instruction22x)
			return isInRange((TwoRegisterInstruction) i, 8, 16);
		else if(i instanceof Instruction32x)
			return isInRange((TwoRegisterInstruction) i, 16, 16);
		else if(i instanceof Instruction12x)
			return isInRange((TwoRegisterInstruction) i, 4, 4);
		else if(i instanceof Instruction11x)
			return isInRange((OneRegisterInstruction) i, 8);
		else if(i instanceof Instruction11n)
			return isInRange((OneRegisterInstruction) i, 4);
		else if(i instanceof Instruction21s)
			return isInRange((OneRegisterInstruction) i, 8);
		else if(i instanceof Instruction31i)
			return isInRange((OneRegisterInstruction) i, 8);
		else if(i instanceof Instruction51l)
			return isInRange((OneRegisterInstruction) i, 8);
		else if(i instanceof Instruction21ih)
			return isInRange((OneRegisterInstruction) i, 8);
		else if(i instanceof Instruction21lh)
			return isInRange((OneRegisterInstruction) i, 8);
		else if(i instanceof Instruction21c)
			return isInRange((OneRegisterInstruction) i, 8);
		else if(i instanceof Instruction22c)
			return isInRange((TwoRegisterInstruction) i, 4, 4);
		else if(i instanceof Instruction22c)
			return isInRange((TwoRegisterInstruction) i, 4, 4);
		else if(i instanceof Instruction35c)
			return isInRange((FiveRegisterInstruction) i, 4, 4, 4, 4, 4);
		else if(i instanceof Instruction3rc)
			return isInRange((RegisterRangeInstruction) i, 16);//TODO
		else if(i instanceof Instruction31t)
			return isInRange((OneRegisterInstruction) i, 8);
		else if(i instanceof Instruction23x)
			return isInRange((ThreeRegisterInstruction) i, 8, 8, 8);		
		else if(i instanceof Instruction22t)
			return isInRange((TwoRegisterInstruction) i, 4, 4);
		else if(i instanceof Instruction21t)
			return isInRange((OneRegisterInstruction) i, 8);
		else if(i instanceof Instruction22s)
			return isInRange((TwoRegisterInstruction) i, 4, 4);
		else if(i instanceof Instruction22b)
			return isInRange((TwoRegisterInstruction) i, 8, 8);
		else
			return true;
	}

	private int newRegister(int oldReg)
	{
		if(oldReg < numNonParameterRegisters){
			//no change
			return oldReg;
		} else {
			return oldReg + numNewRegisters;
		}
	}



	public void visit(Instruction i)
	{
		/*
		System.out.println("insn: "+i);
		List<Integer> regs = getUsedRegistersNums(i);
		
		if(regs != null){
			System.out.print("regs: ");
			for(Integer r : regs){
				System.out.print(r+" ");
			}
			System.out.println("");
			}
		*/

		/*
		if(i instanceof RegisterRangeInstruction){
			RegisterRangeInstruction ri = (RegisterRangeInstruction) i;
			int start = ri.getStartRegister();
			int end = start + ri.getRegisterCount() - 1;

			if(start < numNonParameterRegisters && end >= numNonParameterRegisters){
				System.out.println("start: "+start+" end: "+end+" numNonParameterRegisters: "+numNonParameterRegisters);
				numParamRegistersToClone = Math.max(numParamRegistersToClone, end - numNonParameterRegisters + 1);
			}
		}
		*/
	}

}
