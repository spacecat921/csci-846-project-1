package com.csci846;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.SecureRandom;


// RequestHandler is thread that process requests of one client connection
public class RequestHandler extends Thread {


	Socket clientSocket;

	InputStream inFromClient;

	OutputStream outToClient;

	byte[] request = new byte[1024];

	BufferedReader proxyToClientBufferedReader;

	BufferedWriter proxyToClientBufferedWriter;


	private ProxyServer server;


	public RequestHandler(Socket clientSocket, ProxyServer proxyServer) {
		this.clientSocket = clientSocket;
		this.server = proxyServer;

		try {
			clientSocket.setSoTimeout(2000);
			inFromClient = clientSocket.getInputStream();
			outToClient = clientSocket.getOutputStream();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}


	@Override

	public void run() {
		try {
			// (1) Check the request type, only process GET request and ignore others
			proxyToClientBufferedReader = new BufferedReader(new InputStreamReader(inFromClient));

			// Build the request byte array
			StringBuilder textReceived = new StringBuilder();
			for(String line = proxyToClientBufferedReader.readLine(); line != null; line = proxyToClientBufferedReader.readLine()){
				textReceived.append(line);
			}

			// TODO: Is this needed to be set here????
			request = textReceived.toString().getBytes();

			String httpRequest = parseHttpRequest(textReceived.toString());
			String url = parseUrl(textReceived.toString());

			// (2) If the url of GET request has been cached, respond with cached content
			if(httpRequest.equals("GET")){
				// If cache exists forward it on
				if(server.getCache(url) != null){
					sendCachedInfoToClient(server.getCache(url));
				}
				// (3) Otherwise, call method proxyServertoClient to process the GET request
				proxyServertoClient(textReceived.toString().getBytes());
			}

			clientSocket.close();
		} catch (IOException ex) {
			System.out.println("Server exception: " + ex.getMessage());
			ex.printStackTrace();
		}

	}

	private boolean proxyServertoClient(byte[] clientRequest) {

		FileOutputStream fileWriter = null;
		Socket serverSocket = null;
		InputStream inFromServer;
		OutputStream outToServer;

		// Create Buffered output stream to write to cached copy of file
		String fileName = "cached/" + generateRandomFileName() + ".dat";

		// to handle binary content, byte is used
		byte[] serverReply = new byte[4096];


		/**
		 * To do
		 * (1) Create a socket to connect to the web server (default port 80)
		 * (2) Send client's request (clientRequest) to the web server, you may want to use flush() after writing.
		 * (3) Use a while loop to read all responses from web server and send back to client
		 * (4) Write the web server's response to a cache file, put the request URL and cache file name to the cache Map
		 * (5) close file, and sockets.
		 */

		String url = parseUrlFromByteArray(clientRequest);
		// (1) Create a socket to connect to the web server (default port 80)
		try (ServerSocket proxySocket = new ServerSocket(80)) {
			serverSocket = proxySocket.accept();
			System.out.println("New Outbound client connected");

			serverSocket.setSoTimeout(2000);
			outToServer =  serverSocket.getOutputStream();

			// (2) Send client's request (clientRequest) to the web server, you may want to use flush() after writing.
			outToServer.write(clientRequest);
			outToServer.flush();

			// TODO: (3) Use a while loop to read all responses from web server and send back to client
			inFromServer = serverSocket.getInputStream();
//			while(inFromServer){
//
//			}

		} catch (IOException ex) {
			System.out.println("Server exception: " + ex.getMessage());
			ex.printStackTrace();
		}

		// (4) Write the web server's response to a cache file, put the request URL and cache file name to the cache Map
		try (FileOutputStream stream = new FileOutputStream(fileName)) {
			stream.write(serverReply);
		} catch (Exception e) {
			e.printStackTrace();
		}

		server.putCache(url,fileName);

		// Close sockets , No need to close file as it is in a try block.
		try {

			if (serverSocket != null) {
				serverSocket.close();
			}

		} catch (Exception e) {
			e.printStackTrace();

		}

		return true;
	}

	// Sends the cached content stored in the cache file to the client

	private void sendCachedInfoToClient(String fileName) {

		try {

			byte[] bytes = Files.readAllBytes(Paths.get(fileName));

			outToClient.write(bytes);
			outToClient.flush();

		} catch (Exception e) {
			e.printStackTrace();
		}

		try {

			if (clientSocket != null) {
				clientSocket.close();
			}

		} catch (Exception e) {
			e.printStackTrace();

		}
	}


	private String parseHttpRequest(final String textReceived) {
		int firstSpacePosition = textReceived.indexOf(" ");
		return textReceived.substring(0, firstSpacePosition);
	}

	private String parseUrl(final String textReceived) {
		int firstSpacePosition = textReceived.indexOf(" ");
		int secondWordStartPosition = textReceived.indexOf(" ", firstSpacePosition + 1);
		return textReceived.substring(firstSpacePosition + 1, secondWordStartPosition);
	}

	private String parseUrlFromByteArray(final byte[] clientRequest) {
		String byteString = new String(clientRequest, StandardCharsets.UTF_8);
		return parseUrl(byteString);
	}

	// Generates a random file name
	public String generateRandomFileName() {

		String ALPHABET = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ-_";
		SecureRandom RANDOM = new SecureRandom();
		StringBuilder sb = new StringBuilder();

		for (int i = 0; i < 10; ++i) {
			sb.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
		}
		return sb.toString();
	}

}