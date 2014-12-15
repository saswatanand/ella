package com.apposcopy.app;

import java.io.*;

public class UTF8ToAnsiUtils 
{
    // FEFF because this is the Unicode char represented by the UTF-8 byte order mark (EF BB BF).
    public static final String UTF8_BOM = "\uFEFF";

    public static void main(String args[]) throws Exception
	{
		if (args.length != 2) {
			System.out
				.println("Usage : java UTF8ToAnsiUtils utf8file ansifile");
			System.exit(1);
		}
		
		boolean firstLine = true;
		FileInputStream fis = new FileInputStream(args[0]);
		BufferedReader r = new BufferedReader(new InputStreamReader(fis,
																	"UTF8"));
		FileOutputStream fos = new FileOutputStream(args[1]);
		Writer w = new BufferedWriter(new OutputStreamWriter(fos, "Cp1252"));
		for (String s = ""; (s = r.readLine()) != null;) {
			if (firstLine) {
				s = UTF8ToAnsiUtils.removeUTF8BOM(s);
				firstLine = false;
			}
			s = s + System.getProperty("line.separator");
			//System.out.print(s);
			w.write(s);
			w.flush();
		}
		
		w.close();
		r.close();
    }

    private static String removeUTF8BOM(String s) {
        if (s.startsWith(UTF8_BOM)) {
            s = s.substring(1);
        }
        return s;
    }
}
