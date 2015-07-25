import org.apache.tools.ant.*;
import org.apache.tools.ant.taskdefs.*;

import java.util.*;
import java.io.*;

public class GuessBuildToolsPathTask extends Task
{
	public void execute() throws BuildException
	{
		String btPath = getProject().getProperty("android.buildtools.dir");
		if(btPath == null){
			String sdkPath = findAndroidSDKPath();
			btPath = findAndroidBuildToolsPath(sdkPath);
			System.out.println("path to build-tools directory: "+btPath);			
		}
		File dxPath = new File(btPath, "dx");
		if(!dxPath.isFile())
			throw new Error("The configuration variable android.buildtools.dir is probably not set correctly. Current value is "+btPath);
		getProject().setProperty("android.buildtools.dir", btPath);
	}
	
	String findAndroidSDKPath() 
	{
		String[] sCmdExts = {""}; //TODO: support windows?
		StringTokenizer st = new StringTokenizer(System.getenv("PATH"), File.pathSeparator);
		String path, sFile;
		while (st.hasMoreTokens()) {
			path = st.nextToken();
			for (String sExt : sCmdExts) {
				sFile = path + File.separator + "android" + sExt;
				File file = new File(sFile);
				if (file.isFile()) {
					return file.getAbsoluteFile().getParentFile().getParent();
				}
			}
		}
		throw new RuntimeException("The executable 'android' is not in the path");
	}

	String findAndroidBuildToolsPath(String sdkPath)
	{
		File buildToolsDir = new File(sdkPath, "build-tools");
		//find the latest version
		String latestVersion = null;
		for(String v : buildToolsDir.list()){
			if(latestVersion == null || v.compareTo(latestVersion) > 0){
				latestVersion = v;
			}
		}
		assert latestVersion != null : "Build tools not found";
		return buildToolsDir.getPath()+File.separator+latestVersion;
	}


}