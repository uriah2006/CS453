/*Uriah Sypolt
 * cs453 
 * Lab1 Multi-Threaded Web Server
 * 
 * 
 */
package multithread.web.server;

import java.io.*;
import java.net.*;
import java.util.*;

public final class WebServer {
	public static void main(String argv[]) throws Exception {
		int port = 6897;

		// Establish the listen socket.
		@SuppressWarnings("resource")
		ServerSocket listenSocket = new ServerSocket(port);

		while (true) {
			// Listen for a TCP connection request.
			Socket connectionRequest = listenSocket.accept();

			// Construct an object to process the HTTP request message
			HttpRequest request = new HttpRequest(connectionRequest);

			// Create a new thread to process the request.
			Thread thread = new Thread(request);

			// Start the thread.
			thread.start();
		}

	}
}

final class HttpRequest implements Runnable {
	Socket socket;

	// Constructor
	public HttpRequest(Socket socket) throws Exception {
		this.socket = socket;
	}

	// Implement the run() method of the Runnable interface.
	public void run() {
		try {
			processRequest();
		} catch (Exception e) {
			System.out.println(e);
		}
	}

	private void processRequest() throws Exception {
		// Get a reference to the socket's input and output streams.
		InputStream input = socket.getInputStream();
		DataOutputStream output = new DataOutputStream(socket.getOutputStream());

		// Set up input stream filters.
		BufferedReader br = new BufferedReader(new InputStreamReader(input));

		// Get the request line of the HTTP request message.
		String requestLine = br.readLine();

		// Extract the filename from the request line.
		StringTokenizer tokens = new StringTokenizer(requestLine);

		boolean badRequest = false;
		boolean methodNotImplemented = false;

		String fileName = null;
		String version = null;

		String command = tokens.nextToken();
		if (tokens.hasMoreTokens()) {
			fileName = tokens.nextToken();
			// Prepend a "." so that file request is within the current
			// directory.
			fileName = "." + fileName;
		} else {
			badRequest = true;
		}
		if (tokens.hasMoreTokens()) {
			version = tokens.nextToken();
		} else {
			badRequest = true;
		}

		if (tokens.hasMoreTokens()) {
			badRequest = true;
		}

		boolean fileExists = true;
		boolean fileOpen = true;
		FileOutputStream fos = null;
		FileInputStream fis = null;

		if (!((command.equals("GET")) || (command.equals("HEAD")) || (command.equals("PUT")))) {
			methodNotImplemented = true;
		}

		if (!version.matches("HTTP/.[.].")) {
			System.out.println("Version pattern mismatch");

			badRequest = true;
		}

		// Construct the response message.
		String statusLine = null;
		String contentTypeLine = null;
		String entityBody = null;

		if (badRequest) {
			statusLine = "HTTP/1.0 400 Bad Request\n";
			contentTypeLine = "Content-type: " + "text/html\n";
			entityBody = "<HTML>" + "<HEAD><TITLE>Bad Request</TITLE></HEAD>" + "<BODY>" + requestLine + " is a bad request</BODY></HTML>\n";
		}

		if (methodNotImplemented) {
			statusLine = "HTTP/1.0 501 Method Not Implemented\n";
			contentTypeLine = "Content-type: " + "text/html\n";
			entityBody = "<HTML>" + "<HEAD><TITLE>Method Not Implemented</TITLE></HEAD>" + "<BODY> Method " + command + " Not Implemented</BODY></HTML>\n";
		}

		if (!(badRequest || methodNotImplemented)) {
			if (command.equals("PUT")) {
				try {
					fos = new FileOutputStream(fileName);
				} catch (Exception e) {
					fileOpen = false;
				}
			} else {

				// Open the requested file.
				try {
					fis = new FileInputStream(fileName);
				} catch (FileNotFoundException e) {
					System.out.println("File not found");
					fileExists = false;
				}
			}

			if (fileExists && fileOpen) {
				statusLine = "HTTP/1.0 200 OK\n";
				contentTypeLine = "Content-type: " + contentType(fileName) + "\n";
			} else {
				if (!(fileExists)) {
					statusLine = "404 Not Found\n";
					contentTypeLine = "Content-type: " + "text/html\n";
					entityBody = "<HTML>" + "<HEAD><TITLE>Not Found</TITLE></HEAD>" + "<BODY>File " + fileName + " Not Found</BODY></HTML>\n";
				} else {
					statusLine = "404 Not Found\n";
					contentTypeLine = "Content-type: " + "text/html\n";
					entityBody = "<HTML>" + "<HEAD><TITLE>Not Found</TITLE></HEAD>" + "<BODY>File " + fileName + " could not be written</BODY></HTML>\n";
				}
			}
		}

		// Send the status line.
		output.writeBytes(statusLine);

		// Send the content-type line.
		output.writeBytes(contentTypeLine);

		// Send a blank line to indicate the end of the header lines.
		output.writeBytes("\n");

		// Send the entity body.
		if (!(badRequest || methodNotImplemented) && (fileExists && fileOpen)) {
			if (command.equals("PUT")) {
				try {
					int character;

					// Read the input data one character at a time until end
					// of stream reached
					character = br.read();
					while (character != -1) {
						fos.write(character);
						character = br.read();
					}

					fos.close();
				} catch (Exception e) {
					fos.close();
				}
			} else {
				if (command.equals("GET")) {
					try {
						sendBytes(fis, output);
					} catch (Exception e) {
						System.out.println("Exception raised");
					}
				}
				fis.close();
			}
		} else {
			System.out.println(entityBody);
			output.writeBytes(entityBody);
		}

		// Display the request line.
		System.out.println();
		System.out.println(requestLine);

		output.close();
		br.close();
		socket.close();
	}

	private static void sendBytes(FileInputStream fis, OutputStream os) throws Exception {
		// Construct a 1K buffer to hold bytes on their way to the socket.
		byte[] buffer = new byte[1024];
		int bytes = 0;

		// Copy requested file into the socket's output stream.
		while ((bytes = fis.read(buffer)) != -1) {
			os.write(buffer, 0, bytes);
		}
	}

	private static String contentType(String fileName) {
		if (fileName.endsWith(".htm") || fileName.endsWith(".html")) {
			return "text/html";
		}
		if (fileName.endsWith(".gif") || fileName.endsWith(".GIF")) {
			return "image/gif";
		}
		if (fileName.endsWith(".jpeg")) {
			return "image/jpeg";
		}
		if (fileName.endsWith(".java")) {
			return "java file";
		}
		if (fileName.endsWith(".sh")) {
			return "bourne/awk";
		}
		return "application/octet-stream";
	}
}
