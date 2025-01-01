
/**
 * A driver class for WebClient
 * 
 * 
 * One command line argument is required: 
 * 		- the URL of the object to be downloaded
 * 
 * 
 * @author 	Majid Ghaderi
 * @version 2024
 *
 */

import java.io.*;
import java.util.*;
import java.util.logging.*;

public class ClientDriver {
	private static final Logger logger = Logger.getLogger("WebClient");

	public static void main(String[] args) {
		// example URLs
		// original http://pages.cs.ucalgary.ca:90/~mghaderi/files/large.jpg
		// http://pages.cs.ucalgary.ca/~mghaderi/files/large.jpg
		// http://pages.cpsc.ucalgary.ca/~mghaderi/files/medium.txt
		// https://pages.cpsc.ucalgary.ca/~mghaderi/files/large.pdf
		// https://pages.cpsc.ucalgary.ca/~mghaderi/files/medium.pdf

		// input URL is required
		if (args.length == 0) {
			System.out.println("incorrect usage, input URL is required");
			System.out.println("try again");
			System.exit(0);
		}

		// parse command line args
		HashMap<String, String> params = parseCommandLine(args);

		// set the parameters
		String url = params.getOrDefault("-u", args[0]); // object url
		Level logLevel = Level.parse(params.getOrDefault("-v", "all").toUpperCase()); // log levels: all, info, off

		// set log level
		setLogLevel(logLevel);

		WebClient client = new WebClient();
		System.out.printf("downloading %s...\n", url);
		client.getObject(url);
		System.out.println("download completed.");

		// get rid of any lingering threads/timers
		System.exit(0);
	}

	// parse command line arguments
	private static HashMap<String, String> parseCommandLine(String[] args) {
		HashMap<String, String> params = new HashMap<String, String>();

		int i = 0;
		while ((i + 1) < args.length) {
			params.put(args[i], args[i + 1]);
			i += 2;
		}

		return params;
	}

	// set the global log level and format
	private static void setLogLevel(Level level) {
		System.setProperty("java.util.logging.SimpleFormatter.format", "%5$s %n");

		ConsoleHandler handler = new ConsoleHandler();
		handler.setLevel(level);
		logger.addHandler(handler);
		logger.setLevel(level);
		logger.setUseParentHandlers(false);
	}

}
