package com.apposcopy.ella.frontend;

import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;

import java.io.*;
import java.util.logging.*;

@WebServlet(name = "UploadCoverage", urlPatterns = {"/uploadcoverage"})
public class UploadCoverage extends HttpServlet
{
	private static Logger logger = Logger.getLogger(UploadCoverage.class.getName());
    private static final int DEFAULT_BUFFER_SIZE = 10240; // 10KB.                                                                                                                                                
    public UploadCoverage() {
        super();
    }

	protected void doGet(HttpServletRequest request,
								  HttpServletResponse response)
		throws ServletException, IOException 
	{
		response.setContentType("text/html;charset=UTF-8");

		// Create path components to save the file
		String pkg = request.getParameter("pkg");
		String covData = request.getParameter("cov");
		String ellaDir = getServletContext().getInitParameter("ella.dir");

		System.out.println("pkg: "+pkg+" covData: "+covData+" ell.dir: "+ellaDir);

		BufferedWriter out = null;
		final PrintWriter writer = response.getWriter();

		try {
			String path = ellaDir + File.separator + "ellacovdata" + File.separator + pkg;
			File dir = new File(path);
			dir.mkdir();
			out = new BufferedWriter(new FileWriter(new File(dir, "coverage.dat")));
			out.write(covData);
			writer.println("Uploaded coverage data for " + pkg);
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