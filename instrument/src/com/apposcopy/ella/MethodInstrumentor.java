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
}
