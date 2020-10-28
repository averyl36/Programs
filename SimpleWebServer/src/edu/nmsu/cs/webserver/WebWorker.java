/**
 * Web worker: an object of this class executes in its own new thread to receive and respond to a
 * single HTTP request. After the constructor the object executes on its "run" method, and leaves
 * when it is done.
 *
 * One WebWorker object is only responsible for one client connection. This code uses Java threads
 * to parallelize the handling of clients: each WebWorker runs in its own thread. This means that
 * you can essentially just think about what is happening on one client at a time, ignoring the fact
 * that the entirety of the webserver execution might be handling other clients, too.
 *
 * This WebWorker class (i.e., an object of this class) is where all the client interaction is done.
 * The "run()" method is the beginning -- think of it as the "main()" for a client interaction. It
 * does three things in a row, invoking three methods in this class: it reads the incoming HTTP
 * request; it writes out an HTTP header to begin its response, and then it writes out some HTML
 * content for the response content. HTTP requests and responses are just lines of text (in a very
 * particular format).
 *
 * @author Jon Cook, Ph.D.
 *
 **/
 
package edu.nmsu.cs.webserver;

import java.io.*;
import java.util.*;
import java.time.*;
import java.net.Socket;
import java.text.DateFormat;
import java.io.BufferedInputStream;

public class WebWorker implements Runnable {

	private Socket socket;

	// Constructor: must have a valid open socket
	public WebWorker(Socket s) {
		socket = s;
	} // end WebWorker

	/**
	 * Worker thread starting point. Each worker handles just one HTTP request and then returns, which
	 * destroys the thread. This method assumes that whoever created the worker created it with a
	 * valid open socket object.
	 **/
    
	public void run() {
   
		System.err.println("Handling connection...");
      
		try {

         String fileType = "image";
			InputStream is = socket.getInputStream();
			OutputStream os = socket.getOutputStream();
			String fileName = readHTTPRequest(is);
			//writeHTTPHeader(os, "text/html", fileName);
         
         if(fileName.contains(".html")) {
				writeHTTPHeader(os, "text/html", fileName);
            fileType = "html";
			} // end html
         
			else if(fileName.contains(".gif")) {
				writeHTTPHeader(os, "image/gif", fileName);
			} // end gif
         
			else if(fileName.toLowerCase().contains(".png")) {
				writeHTTPHeader(os, "image/png", fileName);
			} // end png
         
			else if(fileName.contains(".jpg")||fileName.toLowerCase().contains(".jpeg")) {
				writeHTTPHeader(os, "image/jpeg", fileName);
			} // end jpg
         
			else if(fileName.equals("")) {
				writeHTTPHeader(os, "text/html", fileName);
				writeContent(os);
			} // end empty
         
         else {
            return; 
         } // end error case
            
         switch(fileType) {
         
            case "html":
            
               try {
               
			         FileReader file = new FileReader(fileName);
			         Scanner scan = new Scanner(file);
			         String fileLine = "";
                  
			         while(scan.hasNextLine()) {
				         fileLine = scan.nextLine();
				         LocalDate date = LocalDate.now();
				         fileLine = fileLine.replaceAll("<cs371date>", date.toString());
				         fileLine = fileLine.replaceAll("<cs371server>", "Avery's Server");
				         os.write(fileLine.getBytes());
			         } // end while
                  
			         scan.close();
                  
			      } // end try
               
              		      catch(Exception e){
				      System.err.println("Output error: " + e);
			      } // end catch
               
               break;
               
            case "image":
            
               try {
	
                 		 BufferedInputStream fis = new BufferedInputStream(new FileInputStream(fileName));
			         int byteRead = fis.read();
			         while(byteRead != -1) {
				         os.write(byteRead);
				         byteRead = fis.read(); 
			         } // end while
			         is.close();
			      } // end try
               
               catch(Exception e){
				      System.err.println("Output error: " + e);
			      } // end catch		

               break;
               
         } // end switch
    
         os.flush();
			socket.close();
         
		} // end try
      
		catch (Exception e) {
			System.err.println("Output error: " + e);
		} // end catch
      
		System.err.println("Done handling connection.");
      
	} // end run

	// Read the HTTP request header.
	private String readHTTPRequest(InputStream is) {
   
		String line;
      String fileName = null;
		BufferedReader r = new BufferedReader(new InputStreamReader(is));
		
      while (true) {
      
			try {
         
				while (!r.ready())
					Thread.sleep(1);
               
				line = r.readLine();
				System.err.println("Request line: (" + line + ")");
   
            if(line.contains("GET")) {
               fileName = line.substring(5, line.length()-9);
               //System.out.println(fileName);
            } // end if
            
				if (line.length() == 0)
					break;
               
			} // end try
         
			catch (Exception e) {
         
				System.err.println("Request error: " + e);
				break;
            
			} // end catch
         
		} // end while
      
		return fileName;
	
   } // end readHTTPRequest

	/**
	 * Write the HTTP header lines to the client network connection.
	 * 
	 * @param os
	 *          is the OutputStream object to write to
	 * @param contentType
	 *          is the string MIME content type (e.g. "text/html")
	 **/
	private void writeHTTPHeader(OutputStream os, String contentType, String fileName) throws Exception {
   
		Date d = new Date();
      boolean err404 = false;
		DateFormat df = DateFormat.getDateTimeInstance();
		df.setTimeZone(TimeZone.getTimeZone("GMT"));
      
      try {
         FileReader file = new FileReader(fileName);
      } // end try
      
      catch (Exception e) {
         os.write("HTTP/1.0 404 Not Found\n".getBytes());
         err404 = true;
      } // end catch
      
      if (!err404)
         os.write("HTTP/1.1 200 OK\n".getBytes());

		os.write("Date: ".getBytes());
		os.write((df.format(d)).getBytes());
		os.write("\n".getBytes());
		os.write("Server: Avery's very own server\n".getBytes());
		os.write("Connection: close\n".getBytes());
		os.write("Content-Type: ".getBytes());
		os.write(contentType.getBytes());
		os.write("\n\n".getBytes());
      return;
      
	} // end writeHTTPHeader

	/**
	 * Write the data content to the client network connection. This MUST be done after the HTTP
	 * header has been written out.
	 * 
	 * @param os
	 *          is the OutputStream object to write to
	 **/
	private void writeContent(OutputStream os) throws Exception {
   
		os.write("<html><head></head><body>\n".getBytes());
		os.write("<h3>My web server works!</h3>\n".getBytes());
		os.write("</body></html>\n".getBytes());
      
	} // end writeContent

} // end class
