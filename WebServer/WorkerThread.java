
/**
 * Worker Thread is utilized by WebServer class
 * @author 	Quenten Welch
 * @version	2024
 *
 */

import java.io.*;
import java.net.*;
import java.util.*;

class WorkerThread extends Thread {
    private Socket clientSocket;
    private String rootDirectory;
    private int timeout;

    public WorkerThread(Socket clientSocket, String rootDirectory, int timeout) {
        this.clientSocket = clientSocket;
        this.rootDirectory = rootDirectory;
        System.out.println(
                "Client connected: " + clientSocket.getInetAddress().getHostAddress() + ":" + clientSocket.getPort());

    }

    /**
     * Main method in the worker thread.
     * Reads and parses the client's HTTP request, checks if the requested file
     * exists,
     * and sends an appropriate HTTP response back to the client.
     */
    public void run() {
        try {
            // Get input and output streams for the socket
            BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            DataOutputStream out = new DataOutputStream(clientSocket.getOutputStream());

            // Read the request from the client
            String requestLine = in.readLine();
            try {
                if (requestLine != null && !requestLine.isEmpty()) {
                    // Parse the request
                    System.out.println("Request " + requestLine);
                    String[] requestParts = requestLine.split(" ");
                    if (requestParts.length == 3 && requestParts[0].equals("GET")
                            && requestParts[2].equals("HTTP/1.1")) {
                        // Get the requested path
                        String requestedPath = requestParts[1];
                        if (requestedPath.equals("/")) {
                            requestedPath = "/index.html"; // Default to index.html if no path is specified
                        }

                        // Construct the file path
                        String filePath = rootDirectory + requestedPath;

                        // Check if the file exists
                        File file = new File(filePath);
                        if (file.exists() && !file.isDirectory()) {
                            // File exists, send OK response with file content
                            sendResponse(out, 200, "OK", file);
                        } else {
                            // File not found, send 404 response
                            sendResponse(out, 404, "Not Found", null);
                        }
                    } else {
                        // Bad request, send 400 response
                        sendResponse(out, 400, "Bad Request", null);
                    }
                }
            } catch (SocketTimeoutException e) {
                // Request timeout, send 408 response
                sendResponse(out, 408, "Request Timeout", null);
            }

            // Close the streams and socket
            in.close();
            out.close();
            clientSocket.close();
        } catch (IOException e) {
            System.err.println("Error handling client request: " + e.getMessage());
        }
    }

    /**
     * Sends an HTTP response to the client with the specified status code, status
     * message, and file content (if applicable).
     * 
     * @param out           DataOutputStream to send the response to the client.
     * @param statusCode    HTTP status code of the response.
     * @param statusMessage HTTP status message of the response.
     * @param file          File object representing the file to be sent in the
     *                      response, or null if no file content is to be sent.
     * 
     * @throws IOException If an I/O error occurs while sending the response.
     */
    private void sendResponse(DataOutputStream out, int statusCode, String statusMessage, File file)
            throws IOException {
        // Status line
        String statusLine = "HTTP/1.1 " + statusCode + " " + statusMessage;
        System.out.println(statusLine);
        out.writeBytes(statusLine + "\r\n");

        // Header lines
        String[] headers = {
                "Date: " + ServerUtils.getCurrentDate(),
                "Server: MySimpleWebServer",
                "Connection: close"
        };
        for (String header : headers) {
            System.out.println(header);
            out.writeBytes(header + "\r\n");
        }

        if (file != null) {
            // Additional headers for successful response
            String[] fileHeaders = {
                    "Last-Modified: " + ServerUtils.getLastModified(file),
                    "Content-Length: " + ServerUtils.getContentLength(file),
                    "Content-Type: " + ServerUtils.getContentType(file)
            };
            for (String header : fileHeaders) {
                System.out.println(header);
                out.writeBytes(header + "\r\n");
            }
            out.writeBytes("\r\n"); // Empty line to separate headers from content

            // Send the file content
            FileInputStream fileIn = new FileInputStream(file);
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = fileIn.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
            fileIn.close();
        } else {
            // End headers for error response
            out.writeBytes("\r\n");
        }
    }
}
