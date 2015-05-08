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

import com.google.gson.Gson;

@WebServlet(name = "UploadCoverage", urlPatterns = {"/uploadcoverage"})
public class UploadCoverage extends HttpServlet
{
	private static Logger logger = Logger.getLogger(UploadCoverage.class.getName());
    private static final int DEFAULT_BUFFER_SIZE = 10240; // 10KB.                                                                                                                                                
	private Map<String,String> appIdToTraceId = new HashMap();

    public UploadCoverage() {
        super();
    }
    
    private class CoverageUpdate {
    	private String id;
    	public String getAppId() { return id; }
    	private String cov;
    	public String getData() { return cov; }
    	private String stop;
    	public boolean requestsStop() { return stop.equals("true"); }
    	private String recorder;
    	public String getRecorderName() { return recorder; }
    }

	protected void doGet(HttpServletRequest request,
						 HttpServletResponse response)
		throws ServletException, IOException 
	{
		response.setContentType("application/json;charset=UTF-8");
		Gson gson = new Gson();
		BufferedWriter out = null;
		PrintWriter writer = response.getWriter();
		try {
            StringBuilder sb = new StringBuilder();
            String s;
            while ((s = request.getReader().readLine()) != null) {
                sb.append(s);
            }
			CoverageUpdate covUpdate = (CoverageUpdate) gson.fromJson(sb.toString(), CoverageUpdate.class);
			
			String ellaOutDir = getServletContext().getInitParameter("ella.outdir");
			String appId = covUpdate.getAppId();
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
				builder.append("recorder:").append(covUpdate.getRecorderName()).append("\n");
				builder.append("version:").append("1").append("\n");
				builder.append("###").append("\n");
				String metaData = builder.toString();
				out.write(metaData, 0, metaData.length());
			}
			out.write(covUpdate.getData());
			
			if(covUpdate.requestsStop()){
				appIdToTraceId.remove(appId);
			}
			logger.log(Level.INFO, "Upload succeeded");
			writer.println("{}");
			response.setStatus(200);
		} catch (FileNotFoundException fne) {
			logger.log(Level.SEVERE, "Problems during file upload. Error: {0}", 
					   new Object[]{fne.getMessage()});
			response.setStatus(400);
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
