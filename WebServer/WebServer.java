
/**
 * WebServer Class
 * 
 * Implements a multi-threaded web server
 * supporting non-persistent connections.
 * @author 	Quenten Welch
 * @version	2024
 *
 */

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.*;
import java.util.logging.*;
import java.util.concurrent.*;

public class WebServer extends Thread {
    // global logger object, configures in the driver class
    private static final Logger logger = Logger.getLogger("WebServer");

    private boolean shutdown = false; // shutdown flag
    private int port;
    private String root;
    private int timeout;
    private ServerSocket serverSocket;

    /**
     * Constructor to initialize the web server
     * 
     * @param port    Server port at which the web server listens > 1024
     * @param root    Server's root file directory
     * @param timeout Idle connection timeout in milli-seconds
     * 
     */
    public WebServer(int port, String root, int timeout) {
        this.port = port;
        this.root = root;
        this.timeout = timeout;
    }

    /**
     * Main method in the web server thread.
     * The web server remains in listening mode
     * and accepts connection requests from clients
     * until it receives the shutdown signal.
     * 
     */
    public void run() {
        try {
            serverSocket = new ServerSocket(port);
            serverSocket.setSoTimeout(timeout);

            while (!shutdown) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    // Create a new thread for each connection and start it
                    WorkerThread worker = new WorkerThread(clientSocket, root, timeout);
                    worker.start();
                } catch (SocketTimeoutException e) {
                    // logger.log(Level.SEVERE, "Socket timeout", e);
                } catch (IOException e) {
                    logger.log(Level.SEVERE, "Error accepting client connection", e);
                }
            }
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Could not listen on port: " + port, e);
        } finally {
            try {
                if (serverSocket != null) {
                    serverSocket.close();
                }
            } catch (IOException e) {
                logger.log(Level.SEVERE, "Could not close server socket", e);
            }
        }
    }

    /**
     * Signals the web server to shutdown.
     *
     */
    public void shutdown() {
        shutdown = true;
        try {
            if (serverSocket != null) {
                serverSocket.close();
            }
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Error closing server socket during shutdown", e);
        }
    }
}
