package com.csci846;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
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

			StringBuilder requestBuilder = new StringBuilder();
			String line;

			while (!(line = proxyToClientBufferedReader.readLine()).isBlank()) {
				requestBuilder.append(line).append("\r\n");
			}

			String httpRequest = parseHttpRequest(requestBuilder.toString());
			String url = parseUrl(requestBuilder.toString());

			// (2) If the url of GET request has been cached, respond with cached content
			if(httpRequest.equals("GET")){
				// If cache exists forward it on
				if(server.getCache(url) != null){
					System.out.println("Cached URL: " + url);
					sendCachedInfoToClient(server.getCache(url));
				}
				// (3) Otherwise, call method proxyServertoClient to process the GET request
				proxyServertoClient(requestBuilder.toString());
			}

			clientSocket.close();
		} catch (IOException ex) {
			System.out.println("Server exception: " + ex.getMessage());
			ex.printStackTrace();
		}

	}

	private boolean proxyServertoClient(String clientRequest) {

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

		// (1) Create a socket to connect to the web server (default port 80)
		// No need to close sockets or files as they are in a resource try block.
		StringBuilder requestBuilder = new StringBuilder();
		try (Socket proxySocket = new Socket(parseHost(clientRequest),8080)) {
			System.out.println("New Outbound client connected");
			// (2) Send client's request (clientRequest) to the web server, you may want to use flush() after writing.
			proxySocket.setSoTimeout(2000);
			OutputStream outputStream = proxySocket.getOutputStream();
			outputStream.write(clientRequest.getBytes(StandardCharsets.UTF_8));
			outputStream.flush();

			// TODO: (3) Use a while loop to read all responses from web server and send back to client
			inFromServer = proxySocket.getInputStream();
			BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inFromServer));

//			StringBuilder requestBuilder = new StringBuilder();
			String line;
			System.out.println("GOT HERE");
			System.out.println("Line:" + bufferedReader.readLine());

			//TODO: FIX THIS!!!!
			while (!(line = bufferedReader.readLine()).isBlank()) {
				requestBuilder.append(line).append("\r\n");
			}

			proxyToClientBufferedWriter = new BufferedWriter(new OutputStreamWriter(outToClient));
			proxyToClientBufferedWriter.write(requestBuilder.toString());

		} catch (IOException ex) {
			System.out.println("Server exception: " + ex.getMessage());
			ex.printStackTrace();
		}

		// (4) Write the web server's response to a cache file, put the request URL and cache file name to the cache Map
		try (FileOutputStream stream = new FileOutputStream(fileName)) {
			stream.write(requestBuilder.toString().getBytes(StandardCharsets.UTF_8));
			stream.flush();
		} catch (Exception e) {
			e.printStackTrace();
		}
		 server.putCache(parseUrl(clientRequest),fileName);
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
		String[] requestsLines = textReceived.split("\r\n");
		String[] requestLine = requestsLines[0].split(" ");
		return requestLine[0];
	}

	private String parseUrl(final String textReceived) {
		String[] requestsLines = textReceived.split("\r\n");
		String[] requestLine = requestsLines[0].split(" ");
		return requestLine[1];
	}

	private String parseHost(final String textReceived){
		String[] requestsLines = textReceived.split("\r\n");
		return requestsLines[1].split(" ")[1];
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