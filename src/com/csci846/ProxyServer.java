package com.csci846;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ProxyServer {

	//cache is a Map: the key is the URL and the value is the file name of the file that stores the cached content
	Map<String, String> cache;

	String logFileName = "log.txt";

	public static void main(String[] args) {

		// Verify args have been set, if else return message
		if(args.length < 1){
			System.out.println("Please input a port number (0-65353)");
			return;
		}

		new ProxyServer().startServer(Integer.parseInt(args[0]));
	}

	void startServer(int proxyPort) {

		cache = new ConcurrentHashMap<>();

		// create the directory to store cached files.
		File cacheDir = new File("cached");
		if (!cacheDir.exists() || (cacheDir.exists() && !cacheDir.isDirectory())) {
			cacheDir.mkdirs();
		}

		/**
		 * To do:
		 * create a serverSocket to listen on the port (proxyPort)
		 * Create a thread (RequestHandler) for each new client connection
		 * remember to catch Exceptions!
		 *
		 * https://www.codejava.net/java-se/networking/java-socket-server-examples-tcp-ip
		 */
		try (ServerSocket proxySocket = new ServerSocket(proxyPort)) {

			System.out.println("Server is listening on port " + proxyPort);

			while (true) {
				Socket socket = proxySocket.accept();
				System.out.println("New client connected");

				new RequestHandler(socket, this).start();
			}

		} catch (IOException ex) {
			System.out.println("Server exception: " + ex.getMessage());
			ex.printStackTrace();
		}

	}



	public String getCache(String hashcode) {
		return cache.get(hashcode);
	}

	public void putCache(String hashcode, String fileName) {
		cache.put(hashcode, fileName);
	}

	public synchronized void writeLog(String info) {

		/**
		 * To do
		 * write string (info) to the log file, and add the current time stamp
		 * e.g. String timeStamp = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss").format(new Date());
		 *
		 */

		/**
				NOTES: This is how I would write a threadsafe synchronous log
				Use a dedicated thread for logging and use a queue to source it. See the 'Queue Implementations trails in The Java Tutorials for how to use queues and for sample code that works.
				https://docs.oracle.com/javase/tutorial/collections/implementations/queue.html

				Java has several synchronized queue classes that you can use. Here is one:

				https://docs.oracle.com/javase/7/docs/api/java/util/concurrent/ConcurrentLinkedQueue.html
		 */
	}

}