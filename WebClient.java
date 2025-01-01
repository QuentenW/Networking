
/**
 * WebClient Class
 * 
 * CPSC 441
 * Assignment 2
 * 
 * @author 	Quenten Welch 30054505
 * @version	2024
 *
 */

import java.io.*;
import java.util.logging.*;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.net.Socket;

public class WebClient {

	private static final Logger logger = Logger.getLogger("WebClient"); // global logger

    /**
     * Default no-arg constructor
     */
	public WebClient() {
		// nothing to do!
	}
	
    /**
     * Downloads the object specified by the parameter url.
	 *
     * @param url	URL of the object to be downloaded. It is a fully qualified URL.
     */
    public void getObject(String url) {
        // Extracting URL components
        String[] urlComponents = parseURL(url);
        String protocol = urlComponents[0];
        String hostname = urlComponents[1];
        int port = Integer.parseInt(urlComponents[2]);
        String pathname = urlComponents[3];

        Socket socket = null;
        try {
            // Establish a TCP connection to the server
            socket = establishConnection(protocol, hostname, port);
            if (socket != null) {
                // Connection established successfully
                if (socket instanceof SSLSocket) {
                    System.out.println("Secure (HTTPS) connection established.");
                } else {
                    System.out.println("Regular (HTTP) connection established.");
                }
                // Proceed with sending a GET request and handling the response
                String response = sendGetRequest(socket, pathname, hostname);
                System.out.println("Server response headers:");
                System.out.println(response);

                // Check if the response status is OK and handle the response
                handleServerResponse(response, socket, pathname);
            } else {
                // Handle the case where the connection could not be established
                System.out.println("Failed to establish a connection.");
            }
        } finally {
            // Close the socket connection
            if (socket != null) {
                try {
                    socket.close();
                   // System.out.println("Connection closed.");
                } catch (IOException e) {
                    System.out.println("Error closing socket: " + e.getMessage());
                }
            }
        }
    }

    /**
     * Parses the URL and returns an array containing the protocol, hostname, port, and pathname.
     *
     * @param url The URL to be parsed.
     * @return An array containing the protocol, hostname, port, and pathname.
     */
    private String[] parseURL(String url) {
        String protocol = url.substring(0, url.indexOf("://"));
        url = url.substring(url.indexOf("://") + 3);

        String hostname;
        int port;
        String pathname;

        // Check if a port is specified
        if (url.contains(":")) {
            hostname = url.substring(0, url.indexOf(":"));
            url = url.substring(url.indexOf(":") + 1);

            if (url.contains("/")) {
                port = Integer.parseInt(url.substring(0, url.indexOf("/")));
                pathname = url.substring(url.indexOf("/"));
            } else {
                port = Integer.parseInt(url);
                pathname = "/";
            }
        } else {
            if (url.contains("/")) {
                hostname = url.substring(0, url.indexOf("/"));
                pathname = url.substring(url.indexOf("/"));
            } else {
                hostname = url;
                pathname = "/";
            }

            // Use default ports based on the protocol
            if (protocol.equalsIgnoreCase("http")) {
                port = 80;
            } else {
                port = 443;
            }
        }

        return new String[]{protocol, hostname, String.valueOf(port), pathname};
    }

    /**
     * Establishes a TCP connection to the server based on the protocol.
     *
     * @param protocol The protocol (HTTP or HTTPS).
     * @param hostname The hostname of the server.
     * @param port     The port number.
     * @return A Socket object representing the connection.
     */
    private Socket establishConnection(String protocol, String hostname, int port) {
        Socket socket = null;

        try {
            if (protocol.equalsIgnoreCase("http")) {
                // Regular TCP connection
                socket = new Socket(hostname, port);
            } else if (protocol.equalsIgnoreCase("https")) {
                // Secure TCP connection
                SSLSocketFactory factory = (SSLSocketFactory) SSLSocketFactory.getDefault();
                SSLSocket sslSocket = (SSLSocket) factory.createSocket(hostname, port);
                socket = sslSocket; // Assign the SSLSocket to the Socket variable
            }
        } catch (Exception e) {
            System.out.println("Error establishing connection: " + e.getMessage());
        }

        return socket;
    }

    /**
     * Sends a GET request for the specified object and reads the server response headers.
     *
     * @param socket   The socket connected to the server.
     * @param pathname The pathname of the object to request.
     * @param hostname the hostname of the server. 
     * @return The server response as a string.
     */
    private String sendGetRequest(Socket socket, String pathname, String hostname) {
    StringBuilder responseHeaders = new StringBuilder();
    try {
        // Send GET request
        OutputStream outputStream = socket.getOutputStream();
        String getRequest = "GET " + pathname + " HTTP/1.1\r\n" +
                             "Host: " + hostname + "\r\n" +
                             "Connection: close\r\n\r\n";
        outputStream.write(getRequest.getBytes("US-ASCII"));

        outputStream.flush();
        System.out.println("REQUEST");
        System.out.println(getRequest);

        // Read server response headers
        InputStream inputStream = socket.getInputStream();
        int b;
        while ((b = inputStream.read()) != -1) {
            responseHeaders.append((char) b);
            if (responseHeaders.toString().endsWith("\r\n\r\n")) {
                break; // End of headers
            }
        }
    } catch (Exception e) {
        System.out.println("Error sending GET request or reading response: " + e.getMessage());
    }
    return responseHeaders.toString();
}

/**
 * Checks if the server response status is OK (200) and handles the response body.
 *
 * @param responseHeaders The server response headers as a string.
 * @param socket          The socket connected to the server.
 * @param pathname        The pathname of the object to request.
 */
private void handleServerResponse(String responseHeaders, Socket socket, String pathname) {
    if (responseHeaders.contains("200 OK")) {
        // Response status is OK, proceed to handle the response body
        try {
            // Create a local file with the object name
            File file = new File(pathname.substring(pathname.lastIndexOf('/') + 1));
            FileOutputStream fileOutputStream = new FileOutputStream(file);

            // Read the response body from the socket and write to the local file
            InputStream inputStream = socket.getInputStream();
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                fileOutputStream.write(buffer, 0, bytesRead);
            }

            fileOutputStream.close();
          //  System.out.println("File downloaded successfully: " + file.getName());
        } catch (IOException e) {
            System.out.println("Error handling server response: " + e.getMessage());
        }
    } else {
        // Server returned a non-OK status
        System.out.println("Server returned a non-OK status: " + responseHeaders.split("\r\n")[0]);
    }
}

}
