package com.apposcopy.ella;

import org.jf.dexlib2.iface.Method;
import org.jf.dexlib2.iface.MethodImplementation;
import org.jf.dexlib2.iface.reference.MethodReference;
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

/*
 * @author Saswat Anand
 */
public abstract class MethodInstrumentor
{
	protected MethodReference probeMethRef;

	protected abstract void preinstrument(Method method, MutableMethodImplementation code);

	protected abstract void instrument(Method method, MutableMethodImplementation code, int probeRegister);

	protected abstract int numRegistersToAdd();

	protected abstract String probeMethName();
	
	protected abstract String recorderClassName();
	
	protected void setProbeMethRef(MethodReference probeMethRef)
	{
		this.probeMethRef = probeMethRef;
	}

	/*
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
	*/


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
