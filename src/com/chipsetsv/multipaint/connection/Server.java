package com.chipsetsv.multipaint.connection;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.lang.ref.WeakReference;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.Enumeration;
import org.apache.http.conn.util.InetAddressUtils;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

public class Server {
	
	public Server() {
		ip = getLocalIpAddress();
	}
	
	private volatile boolean connected = false;
	private volatile boolean waiting = false;
    private String ip = "192.168.1.17";
    private int port = 8080;
    private final int ACCEPT_TIMEOUT = 1000;
    private final int READ_TIMEOUT = 500;
  
    private static ReceiverHandler receiverHandler = new ReceiverHandler();
    private static ReceiverHandler statusHandler = new ReceiverHandler();
    private static WeakReference<Handler> senderHandler;
    private volatile ServerReceiverThread receiverThread = new ServerReceiverThread();
    private volatile ServerSenderThread senderThread;
    
    public boolean getConnected() {
		return connected;
	}
    
    public ReceiverHandler getReceiverHandler() {
		return receiverHandler;
	}
    
    public ReceiverHandler getStatusHandler() {
		return statusHandler;
	}
    
    public Handler getSenderHandler() {
		if (senderThread != null)
			return senderThread.getHandler();
		return null;
	}
    
	public int getPort()
	{
		return port;
	}
	
	public String getAddress() {
		return ip;
	}
	
	public void setPort(int port){
		this.port = port;
	}
	
	public void setAddress(String address) {
		if (address.contains(":")) {
			String[] addressData = address.split(":");
			ip = addressData[0];
			int portData = Integer.parseInt(addressData[1]);
			setPort(portData);
		}
		else {
			ip = address;
		}
	}
	
	public void accept() {
		if (!receiverThread.isAlive()) {
			try {
				receiverThread.start();
			} catch (IllegalThreadStateException e)
			{
				receiverThread = new ServerReceiverThread();
				receiverThread.start();
			}
		}
	}
	
	public void close() {	 
		connected = false;	
		waiting = false;
		//receiverHandler.removeOnReceive();
		if (receiverThread.isAlive())
		{
			try {
				receiverThread.join(ACCEPT_TIMEOUT);
			} catch (InterruptedException e) {
				Log.d("Client", "Server Thread join error");
				e.printStackTrace();
			}
		}
	}
	
	public void sendMessage(String message) {
		if (connected) {
			Handler handler = getSenderHandler();
			if (handler == null)
				return;
			Message msg = handler.obtainMessage();
			msg.obj = message;
			handler.sendMessage(msg);
		}
	}
	
	private void SendStatusMessage(String message, Exception e) {
		Message msg = statusHandler.obtainMessage();
        msg.obj = message;
        statusHandler.sendMessage(msg);
        if (e == null) {
        	Log.d("Server", message);
        }
        else {
        	Log.e("Server", message, e);
        	e.printStackTrace();
        }
	}
	
	public class ServerReceiverThread extends Thread {
		private ServerSocket serverSocket;
		
        public void run() {
            try {
                if (ip != null) {
                	SendStatusMessage("Listening on IP: " + ip, null);
                	
                	serverSocket = new ServerSocket(port);
                    serverSocket.setSoTimeout(ACCEPT_TIMEOUT);
                    
                    Socket client = null;
                    waiting = true;
                    while (waiting) {
                    	try {
                    		client = serverSocket.accept();
                    		waiting = !client.isConnected();
                    		connected = client.isConnected();
                    		client.setSoTimeout(READ_TIMEOUT);
                    	} catch (SocketTimeoutException e) {
                        	SendStatusMessage("Waiting on IP: " + ip + "...", null);
                        }
                    }
                    
                    if (connected) {
	                    if (senderThread != null) {
	                    	senderThread.join();
	                	}
	                	if (senderThread == null)
	                	{
	                		senderThread = new ServerSenderThread(client);
	                		senderThread.start();
	                	}
	                    
	                    while (connected) {
	                        // listen for incoming clients
	                        try {
	                        	SendStatusMessage("Connected", null);
	                            BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
	                            String line = null;
	                            while ((line = in.readLine()) != null) {
	                                Log.d("ServerData", line);
	                                Message msg = receiverHandler.obtainMessage();
	                                msg.obj = line;
	                                receiverHandler.sendMessage(msg);
	                            }
	                        } catch (SocketTimeoutException e) {
	                        	Log.d("Server", "ReadLine TimeOut");
	                        }
	                        catch (Exception e) {
	                            SendStatusMessage("Oops. Connection interrupted. Please reconnect your phones", e); 
	                        }
	                    }       		
                    }
                } else {
                	SendStatusMessage("Couldn't detect internet connection", null);
                }
            }
            catch (Exception e) {
        		connected = false;	
        		SendStatusMessage("Error in Server Thread", e);
            }
            finally {
            	try {
            		connected = false;
            		if (senderThread != null && senderThread.isAlive()) {
            			try {
            				Handler handler = senderThread.getHandler();
            				if (handler != null)
            					handler.sendEmptyMessage(0);
            				if (senderThread != null)
            					senderThread.join();
						} catch (InterruptedException e) {
							SendStatusMessage("Error on join sender thread", e);
						}
            		}
            		if (serverSocket != null && !serverSocket.isClosed()) {
            			serverSocket.close();
            			SendStatusMessage("Server Socket closed", null);
            		}
				} catch (IOException e) {
					SendStatusMessage("Error on socket closing", e);
				}
            }
        }
    }
	
	
	private class ServerSenderThread extends Thread {
		private Socket socket;
    	
    	
    	public Handler getHandler() {
    		return senderHandler.get();
    	}
    	
    	public ServerSenderThread(Socket socket)  	{
    		this.socket = socket;
    	}
    	
        public void run() {
        	try {               
                if (connected) {
                	SendStatusMessage("ServerSender Started", null);
                	
                	Looper.prepare();
                	senderHandler = new WeakReference<Handler>(new Handler() {
		                        public void handleMessage(Message msg) {
		                        	if (connected) {
		                        		String message = (String)msg.obj;
		                            	Log.d("Server", "Sending message: " + message);
		                        		PrintWriter out;
										try {
											out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket
											        .getOutputStream())), true);
											out.println(message);
										} catch (IOException e) {
											SendStatusMessage("Error on sending message", e);
										}
		                                Log.d("Server", "Sent");
		                        	}
		                        	else
		                        	{
		                        		this.getLooper().quit();
		                        	}
		                        }
                			}
                		);
                	Looper.loop();
                }
            } catch (Exception e) {
            	connected = false;
            	SendStatusMessage("Error in ServerSender Thread", e);
            }
            finally {
            	try {
            		senderHandler = null;
            		if (socket != null && !socket.isClosed()) {
            			socket.close();
            			SendStatusMessage("Socket closed", null);
            		}
            		senderThread = null;
            		SendStatusMessage("ServerSender Stopped", null);
				} catch (IOException e) {
					SendStatusMessage("Error on socket closing", e);
				}
            }
        }
    }
	
	public String getLocalIpAddress() {
		StringBuilder ips = new StringBuilder();
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
                NetworkInterface intf = en.nextElement();
                for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();) {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    if (!inetAddress.isLoopbackAddress() && InetAddressUtils.isIPv4Address(inetAddress.getHostAddress())) {
                    	ips.append(inetAddress.getHostAddress() + "; "); 
                    }
                }
            }
        } catch (SocketException ex) {
            Log.e("Server", ex.toString());
        }
        return ips.toString();
    }
}
