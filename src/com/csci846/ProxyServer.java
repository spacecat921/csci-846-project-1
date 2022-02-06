package com.csci846;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ProxyServer {

	//cache is a Map: the key is the URL and the value is the file name of the file that stores the cached content
	Map<String, String> cache;

	private String temp;
	private String logFileName = "proxy.log"; //required name
	private Queue<String> log_q; 		
	private FileWriter fw = null;


	public static void main(String[] args) {
		//added
		log_q = new ConcurrentLinkedQueue<>(); 
		if (!pro_log.exists() || (pro_log.exists() && !pro_log.isDirectory()))
		{
			File pro_log = new File(logFileName);			
		} 
		// end added
		// Verify args have been set, if else return message
		if(args.length < 1){
			System.out.println("Please input a port number (0-65353)");
			return;
		}

		new ProxyServer().startServer(Integer.parseInt(args[0]));

		//added
		// do i need to close the file? can't use empty q to know to close
		fw = new FileWriter(logFileName);
		// idk how to make this run forever
		try 
		{			
			while (log_q.peek().notEqual(null)) //check if empty
			{
				fw.write(log_q.poll());
			}
		} finally 
		{

		}
		//end added

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


	public synchronized void writeLog(String info) 
	{	 /*
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

		String timeStamp = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss").format(new Date());
		synchronized(this) //google suggests that this is how to sync stuff
		//time stamp happens when the req comes in, not when synced
		{
			temp = info;
		}
		log_q.add(timeStamp + " " + info);


	}
}
