package com.apposcopy.ella.frontend;

import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;

import java.io.*;
import java.util.*;
import java.util.logging.*;

import java.util.Date;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

@WebServlet(name = "UploadCoverage", urlPatterns = {"/uploadcoverage"})
public class UploadCoverage extends HttpServlet
{
	private static Logger logger = Logger.getLogger(UploadCoverage.class.getName());
    private static final int DEFAULT_BUFFER_SIZE = 10240; // 10KB.                                                                                                                                                
	private Map<String,String> appIdToTraceId = new HashMap();

    public UploadCoverage() {
        super();
    }

	protected void doGet(HttpServletRequest request,
						 HttpServletResponse response)
		throws ServletException, IOException 
	{
		response.setContentType("text/html;charset=UTF-8");

		// Create path components to save the file
		String appId = request.getParameter("id");
		String covData = request.getParameter("cov");
		String stop = request.getParameter("stop");

		String ellaOutDir = getServletContext().getInitParameter("ella.outdir");

		//System.out.println("pkg: "+pkg+" covData: "+covData+" ell.dir: "+ellaDir);

		BufferedWriter out = null;
		final PrintWriter writer = response.getWriter();

		try {
			String path = ellaOutDir + File.separator + appId;
			File dir = new File(path);

			String traceId = appIdToTraceId.get(appId);
			if(traceId == null){
				dir.mkdir();

				DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
				traceId = dateFormat.format(new Date());
				appIdToTraceId.put(appId, traceId);
			} 

			File datFile = new File(dir, "coverage.dat."+traceId);
			boolean append = datFile.exists();
			out = new BufferedWriter(new FileWriter(datFile, append));
			if(!append){
				String recorderName = request.getParameter("recorder");
				StringBuilder builder = new StringBuilder();
				builder.append("recorder:").append(recorderName).append("\n");
				builder.append("version:").append("1").append("\n");
				builder.append("###").append("\n");
				String metaData = builder.toString();
				out.write(metaData, 0, metaData.length());
			}
			out.write(covData);

			if(stop.equals("true")){
				appIdToTraceId.remove(appId);
			}
			//writer.println("Uploaded coverage data.");
			logger.log(Level.INFO, "Upload succeeded");
		} catch (FileNotFoundException fne) {
			writer.println("You either did not specify a file to upload or are "
                + "trying to upload a file to a protected or nonexistent "
						   + "location.");
			writer.println("<br/> ERROR: " + fne.getMessage());

			logger.log(Level.SEVERE, "Problems during file upload. Error: {0}", 
					   new Object[]{fne.getMessage()});
		} finally {
			if (out != null) {
				out.close();
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

}
