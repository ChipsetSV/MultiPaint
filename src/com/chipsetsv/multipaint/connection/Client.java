package com.chipsetsv.multipaint.connection;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.lang.ref.WeakReference;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketTimeoutException;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

public class Client {

	private volatile boolean connected = false;
	private boolean reconnect = true;
	private final int CONNECT_TIMEOUT = 7000;
	private final int READ_TIMEOUT = 500;
	private String serverIpAddress = "192.168.1.17";
	private int port = 8080;
	
	private volatile ClientSenderThread senderThread = new ClientSenderThread();
	private volatile ClientReceiverThread receiverThread;
	private static ReceiverHandler statusHandler = new ReceiverHandler();
	private static ReceiverHandler receiverHandler = new ReceiverHandler();
	private static WeakReference<Handler> senderHandler;
	
	public boolean getReconnect() {
		return reconnect;
	}
	
	public void setReconnect(boolean reconnect) {
		this.reconnect = reconnect;
	}
	
	public int getPort() {
		return port;
	}
	
	public void setPort(int port){
		this.port = port;
	}
	
	public void setAddress(String address) {
		if (address.contains(":")) {
			String[] addressData = address.split(":");
			serverIpAddress = addressData[0];
			int portData = Integer.parseInt(addressData[1]);
			setPort(portData);
		}
		else {
			serverIpAddress = address;
		}
	}
	
	public String getAddress() {
		return serverIpAddress;
	}
	
	public boolean getConnected() {
		return connected;
	}
	
	public Handler getSenderHandler() {
		if (senderThread != null)
			return senderThread.getHandler();
		return null;
	}
	
	public ReceiverHandler getStatusHandler() {
		return statusHandler;
	}
	
	public ReceiverHandler getReceiverHandler()
	{
		return receiverHandler;
	}
	
	public void connect() {
		if (!connected) {
           if (!serverIpAddress.equals("")) {
        	   if (!senderThread.isAlive()) {
        		   try {
       					senderThread.start();
	       			} catch (IllegalThreadStateException e)
	       			{
	       				senderThread = new ClientSenderThread();
	       				senderThread.start();
	       			}
        	   }
            }
        }
	}
	
	public void close() {
		connected = false;
		//receiverHandler.removeOnReceive();
		if (senderThread.isAlive())
		{
			Handler handler = senderThread.getHandler();
			if (handler != null)
				handler.sendEmptyMessage(0);
			try {
				senderThread.join();
			} catch (InterruptedException e) {
				Log.d("Client", "C: Client Thread join error");
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
			getSenderHandler().sendMessage(msg);
		}
	}
	
	private void SendStatusMessage(String message, Exception e) {
		Message msg = statusHandler.obtainMessage();
        msg.obj = message;
        statusHandler.sendMessage(msg);
        if (e == null) {
        	Log.d("Client", message);
        }
        else {
        	Log.e("Client", message, e);
        	e.printStackTrace();
        }
	}
	
    public class ClientSenderThread extends Thread {
    	
    	private Socket socket;
    	
    	public Handler getHandler() {
    		return senderHandler.get();
    	}

        public void run() {
            try {
                InetAddress serverAddr = InetAddress.getByName(serverIpAddress);
                SendStatusMessage("Connecting...", null);
                
                socket = new Socket();
                SocketAddress remoteAddr = new InetSocketAddress(serverAddr, port);
                socket.connect(remoteAddr, CONNECT_TIMEOUT);
                
        		connected = true;	
        		SendStatusMessage("Connected", null);
                if (connected) {
                	
                	if (receiverThread != null) {
                		receiverThread.join();
                	}
                	if (receiverThread == null)
                	{
                		receiverThread = new ClientReceiverThread(socket);
                		receiverThread.start();
                	}
                	
                    try {
                    	Looper.prepare();
                    	senderHandler = new WeakReference<Handler>(new Handler() {
                            public void handleMessage(Message msg) {
                            	if (connected) {
                            		
                            		String message = (String)msg.obj;
	                            	Log.d("Client", "Sending message: " + message);
	                        		PrintWriter out;
									try {
										out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket
										        .getOutputStream())), true);
										out.println(message);
									} catch (IOException e) {
										SendStatusMessage("Error on sending message", e);
									}
	                                Log.d("Client", "Sent");
                            	}
                            	else
                            	{
                            		this.getLooper().quit();
                            	}
                            }
                    	});
                    	Looper.loop();
                    } catch (Exception e) {
                		connected = false;	
                		SendStatusMessage("Error in ClientSender Thread", e);
                    }
                }
            } catch (SocketTimeoutException e) {
	        	SendStatusMessage("Connection timeout", null);
	        } 
            catch (Exception e) {
            	SendStatusMessage("Error in ClientSender Thread", e);
            }
            finally {
            	try {
            		connected = false;
            		if (receiverThread != null && receiverThread.isAlive()) {
            			try {
							receiverThread.join();
						} catch (InterruptedException e) {
							SendStatusMessage("Error on join receiver thread", e);
						}
            		}
            		if (socket != null && !socket.isClosed()) {
            			socket.close();
            			SendStatusMessage("Socket closed", null);
            		}
				} catch (IOException e) {
					SendStatusMessage("Error on socket closing", e);
				}
            }
        }
    }
    
	private class ClientReceiverThread extends Thread {
    	private Socket socket;
    	
    	public ClientReceiverThread(Socket socket)  	{
    		this.socket = socket;
    	}
    	
    	public void run() {
            try {
            	socket.setSoTimeout(READ_TIMEOUT);
            	BufferedReader in = new BufferedReader(new InputStreamReader(this.socket.getInputStream()));
    		
            	SendStatusMessage("ClientReceiver Started", null);
                while (connected) {
                    // listen for incoming data
                    try {
                    	SendStatusMessage("Connected", null);
                        String line = null;
                        while ((line = in.readLine()) != null) {
                            Log.d("ClientReceivedData", line);
                            Message msg = receiverHandler.obtainMessage();
                            msg.obj = line;
                            receiverHandler.sendMessage(msg);
                        }
                    } catch (SocketTimeoutException e) {
                    	Log.d("Client", "ReadLine TimeOut");
                    }
                    catch (Exception e) {
                    	SendStatusMessage("Oops. Connection interrupted. Please reconnect your phones", e); 
                    }
                }       		    
          
            } catch (Exception e) {
        		connected = false;	
        		SendStatusMessage("Error in ClientReceiver Thread", e);
            }
            finally {
            	try {
            		connected = false;
            		if (socket != null && !socket.isClosed()) {
                		socket.close();
                		SendStatusMessage("Socket closed", null);
            		}
            		receiverThread = null;
            		SendStatusMessage("ClientReceiver Stopped", null);
				} catch (IOException e) {
					SendStatusMessage("Error on socket closing", e);
				}
            }
    	}
    }
    
}
