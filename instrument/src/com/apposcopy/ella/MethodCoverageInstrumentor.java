package com.apposcopy.ella;

import org.jf.dexlib2.Opcode;
import org.jf.dexlib2.iface.Method;
import org.jf.dexlib2.iface.reference.MethodReference;
import com.apposcopy.ella.dexlib2builder.MutableMethodImplementation;
import com.apposcopy.ella.dexlib2builder.BuilderInstruction;
import com.apposcopy.ella.dexlib2builder.instruction.*;

import java.util.*;

/*
 * @author Saswat Anand
 */
public class MethodCoverageInstrumentor extends MethodInstrumentor
{
	protected int numRegistersToAdd()
	{
		return 1;
	}

	protected void preinstrument(Method method, MutableMethodImplementation code)
	{
	}

	protected String probeMethName()
	{
		return "m";
	}
	
	protected String recorderClassName()
	{
		return "com.apposcopy.ella.runtime.MethodCoverageRecorder";
	}

	protected void instrument(Method method, MutableMethodImplementation code, int probeRegister)
	{
		int id = CoverageId.g().idFor(Util.signatureOf0(method));
		int probeIdNumBits = Instrument.numBits(probeRegister);
		if(probeIdNumBits == 4){
			code.addInstruction(0, new BuilderInstruction35c(Opcode.INVOKE_STATIC, 1, probeRegister, 0, 0, 0, 0, probeMethRef));
		} else {
			code.addInstruction(0, new BuilderInstruction3rc(Opcode.INVOKE_STATIC_RANGE, probeRegister, 1, probeMethRef));
		}
		if(probeIdNumBits <= 8)
			code.addInstruction(0, new BuilderInstruction31i(Opcode.CONST, probeRegister, id));
		else
			throw new RuntimeException("TODO: Did not find a 8-bit reg to store the probe id");
	}
}
