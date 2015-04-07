package com.apposcopy.ella.frontend;

import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;

import static org.apache.commons.lang3.StringEscapeUtils.escapeHtml4;

import java.io.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.logging.*;

@WebServlet(name = "ViewCoverage", urlPatterns = {"/viewcoverage"})
public class ViewCoverage extends HttpServlet
{
	private static Logger logger = Logger.getLogger(ViewCoverage.class.getName());
    private static final int DEFAULT_BUFFER_SIZE = 10240; // 10KB.                                                                     
    public ViewCoverage() {
        super();
    }

	protected void doGet(HttpServletRequest request,
						 HttpServletResponse response)
		throws ServletException, IOException 
	{
		response.setContentType("text/html;charset=UTF-8");

		final PrintWriter writer = response.getWriter();

		// Create path components to save the file
		String appPath = request.getParameter("apppath");
		String ellaOutDir = getServletContext().getInitParameter("ella.outdir");

		File appFile = new File(appPath);
		if(!appFile.exists()){
			writer.println("File "+appPath+" does not exist.");
			return;
		}

		String appId = appPathToAppId(appFile.getCanonicalPath());

		//System.out.println("pkg: "+pkg+" covData: "+covData+" ell.dir: "+ellaDir);

		BufferedReader covReader = null;
		try {
			String path = ellaOutDir + File.separator + appId;
			generateReport(writer, path);
			/*
			File dir = new File(path);
			covReader = new BufferedReader(new FileReader(new File(dir, "coverage.dat")));
			String covData = covReader.readLine();
			writer.println(covData);
			*/
			logger.log(Level.INFO, "ViewCoverage done.");
		} catch (FileNotFoundException fne) {
			writer.println("You either did not specify a file to upload or are "
                + "trying to upload a file to a protected or nonexistent "
						   + "location.");
			writer.println("<br/> ERROR: " + fne.getMessage());

			logger.log(Level.SEVERE, "Problems during file upload. Error: {0}", 
					   new Object[]{fne.getMessage()});
		} finally {
			if (covReader != null) {
				covReader.close();
			}
			if (writer != null) {
				writer.close();
			}
		}
	}

    protected void doPost(HttpServletRequest request,
                          HttpServletResponse response) throws ServletException, IOException {
        doGet(request, response);
    }

	void generateReport(PrintWriter writer, String ellaOutDir) throws IOException
	{
		String idPath = ellaOutDir + File.separator + "covids";
		BufferedReader idReader = new BufferedReader(new FileReader(idPath));
		List<String> meths = new ArrayList();
		String line;
		int totalMeths = 0;
		while((line = idReader.readLine()) != null){
			meths.add(line);
			totalMeths++;
			//System.out.println(line);
		}
		idReader.close();

		String covPath = ellaOutDir + File.separator + "coverage.dat";
		BufferedReader covReader = new BufferedReader(new FileReader(covPath));
		String covData = covReader.readLine();
		covData = covData.substring(1,covData.length()-1); //drop the leading { and trailing }
		String[] ids = covData.split(", ");

		StringBuilder builder = new StringBuilder();
		int coveredMeths = 0;
		builder.append("<ul>");
		for(String id : ids){
			int i = Integer.parseInt(id);
			String m = meths.get(i);
			builder.append("<li>"+escapeHtml4(m)+"</li>");
			//System.out.println(i + " " + m);
			coveredMeths++;
		}
		builder.append("</ul>");

		double coverage = (coveredMeths * 100.0) / totalMeths;
		String s = "Coverage = "+coverage+"%";
		s += "<br>"+builder.toString();

		writer.print(s);

		covReader.close();
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
    
    private static String appPathToAppId(String appPath) {
    	String appId = appPath.replace(File.separatorChar, '_');
    	if(appId.length() > 100) {
    		return sha256(appId);
    	} else {
    		return appId;
    	}
    }

}
