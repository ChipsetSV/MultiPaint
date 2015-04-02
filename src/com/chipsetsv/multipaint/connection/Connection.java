package com.chipsetsv.multipaint.connection;

public class Connection {
	private static Server server = new Server();
	private static Client client = new Client();
	
	public static Server getServer() {
		return server;
	}
	
	public static Client getClient() {
		return client;
	}
	
	public static boolean isConnected() {
		return client.getConnected() || server.getConnected();
	}
	
	public static ReceiverHandler getReceiverHandler() {
		if (server.getConnected())
			return server.getReceiverHandler();
		if (client.getConnected())
			return client.getReceiverHandler();
		return null;
	}
	
	public static void sendMessage(String message) {
		if (server.getConnected())
			server.sendMessage(message);
		if (client.getConnected())
			client.sendMessage(message);
	}
	
	public static void close() {
		server.close();
		client.close();
	}
}
