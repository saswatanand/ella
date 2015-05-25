package com.apposcopy.ella;

/*
 * @author Saswat Anand
 */
public class MethodSequenceInstrumentor extends MethodCoverageInstrumentor
{
	protected String recorderClassName()
	{
		return "com.apposcopy.ella.runtime.MethodSequenceRecorder";
	}
}
