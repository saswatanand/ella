package com.apposcopy.ella.runtime;

public class Ella
{
	//instrumentation will set values of the following two fields
	static String id; 
	private static String covRecorderClassName;
	
	static CoverageRecorder covRecorder;

	static {
		try{
			covRecorder = (CoverageRecorder) Class.forName(covRecorderClassName).newInstance();
		} catch(ClassNotFoundException e){
			throw new Error(e);
		} catch(InstantiationException e){
			throw new Error(e);
		} catch(IllegalAccessException e){
			throw new Error(e);
		} 
	}

	public static void m(int mId)
	{
		//System.out.println("Covered "+mId);
		covRecorder.m(mId);
	}
	
}
