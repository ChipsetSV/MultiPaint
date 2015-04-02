package com.chipsetsv.multipaint.connection.xmpp;

import java.io.IOException;
import java.lang.ref.WeakReference;

import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.SASLAuthentication;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.filter.AndFilter;
import org.jivesoftware.smack.filter.PacketTypeFilter;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Packet;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.chipsetsv.multipaint.connection.ReceiverHandler;

public class XMPPConnectionHelper {
	
	//private static String CHAT_NAME = "MultiPaint";
	private static String TO;
	private static Account ACCOUNT;
	private static XMPPConnectionType TYPE = XMPPConnectionType.GTalk;
	
	private volatile GTalkConnectionThread GTalkThread = new GTalkConnectionThread();
	
	private volatile boolean connected = false;
	
	private static ReceiverHandler statusHandler = new ReceiverHandler();
	private static ReceiverHandler receiverHandler = new ReceiverHandler();
	private static WeakReference<Handler> senderHandler;
	
	public Handler getSenderHandler() {
		if (GTalkThread != null)
			return GTalkThread.getHandler();
		return null;
	}
	
	public ReceiverHandler getStatusHandler() {
		return statusHandler;
	}
	
	public ReceiverHandler getReceiverHandler()
	{
		return receiverHandler;
	}
	
	public boolean getConnected() {
		return connected;
	}
	
	public void sendMessage(String message) {
		if (connected) {
			Handler handler = getSenderHandler();
			if (handler == null)
				return;
			android.os.Message msg = handler.obtainMessage();
			msg.obj = message;
			getSenderHandler().sendMessage(msg);
		}
	}
	
	private void SendStatusMessage(String message, Exception e) {
		android.os.Message msg = statusHandler.obtainMessage();
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
	
	public void InitializeGTalk(AccountManager accountManager, Account account, String to)
	{
		ACCOUNT = account;
		TO = to;
		TYPE = XMPPConnectionType.GTalk;
		
		SASLAuthentication.registerSASLMechanism(GTalkAuth.NAME, GTalkAuth.class);
        SASLAuthentication.supportSASLMechanism(GTalkAuth.NAME, 0);
        
		accountManager.getAuthToken(ACCOUNT, "mail", null, null, new GetAuthTokenCallback(), null);
	}
	
	public void InitializeXMPPStandart(String user, String password, String to)
	{
		ACCOUNT = null;
		TO = to;
		TYPE = XMPPConnectionType.XMPPStandart;
		
		SASLAuthentication.unregisterSASLMechanism(GTalkAuth.NAME);
        SASLAuthentication.unsupportSASLMechanism(GTalkAuth.NAME);
        SASLAuthentication.supportSASLMechanism("PLAIN");
        
        if (!connected) {
      	   if (!GTalkThread.isAlive()) {
      		   try {
      			   	GTalkThread.setAuth(password, user);
      			  	GTalkThread.start();
       			} catch (IllegalThreadStateException e) {
  	       			GTalkThread = new GTalkConnectionThread();
      	       		GTalkThread.setAuth(password, user);
      	       		GTalkThread.start();
       			}
      	    }
        }
	}
	
	public class GTalkConnectionThread extends Thread {
		
		private String authToken;
		private String authName;
		
		public void setAuth(String authToken, String authName) {
			this.authToken = authToken;
			this.authName = authName;
		}
		
		public Handler getHandler() {
    		return senderHandler.get();
    	}
		
		public void run() {
			ConnectionConfiguration config = null;
			switch (TYPE) {
			case XMPPStandart:
				config = new ConnectionConfiguration("jabber.ru", 5222);
				break;
			case GTalk:
				config = new ConnectionConfiguration("talk.google.com", 5222, "googlemail.com");
				break;
			} 
			config.setSASLAuthenticationEnabled(true);
		    
			final XMPPConnection connection = new XMPPConnection(config);
			try {
			    connection.connect();
			    connection.login(authName, authToken);
			    connected = connection.isConnected();
			    if (connection.isConnected()) {
			    	Log.d("XMPP", "Connected");
			    	
			    	connection.addPacketListener(new PacketListener() {
						
						@Override
						public void processPacket(Packet arg0) {
							String data = ((Message)arg0).getBody();
							Log.d("XMPPReceivedData", data);
                            android.os.Message msg = receiverHandler.obtainMessage();
                            msg.obj = data;
                            receiverHandler.sendMessage(msg);
							
						}
					}, new AndFilter(new PacketTypeFilter(Message.class)));
			    	
//			    	final Chat chat = connection.getChatManager().createChat(TO, new MessageListener() {
//						@Override
//						public void processMessage(Chat arg0, Message arg1) {
//							String data = arg1.getBody();
//							Log.d("XMPPReceivedData", data);
//                            android.os.Message msg = receiverHandler.obtainMessage();
//                            msg.obj = data;
//                            receiverHandler.sendMessage(msg);
//						}
//					});
			    	
				    try {
	                	Looper.prepare();
	                	
	                	senderHandler = new WeakReference<Handler>(new Handler() {
	                        public void handleMessage(android.os.Message msg) {
	                        	String message = (String)msg.obj;
	                        	
	                        	//chat.sendMessage(message);
								Message msgToSent = new Message(TO);
								msgToSent.setBody(message);
								connection.sendPacket(msgToSent);
	                        	
	                        	if (msg.equals("exit"))
	                        		this.getLooper().quit();
	                        }
	                	});
	                	Looper.loop();
	                } catch (Exception e) {
	                	connected = false;
	                	e.printStackTrace();
	                }
			    	
			    }
			} catch (XMPPException e) {
				connected = false;
			    // Most likely an expired token
			    // Invalidate the token and start over. There are example of this available
				Log.d("XMPP", "Connection error");
				e.printStackTrace();
			}
        }
	}

	private class GetAuthTokenCallback implements AccountManagerCallback<Bundle> {
        public void run(AccountManagerFuture<Bundle> result) {
            Bundle bundle;
            try {
                    bundle = (Bundle) result.getResult();
                    Intent intent = (Intent)bundle.get(AccountManager.KEY_INTENT);
                    if(intent != null) {
                        // User input required
                        //startActivity(intent);
                    } else {
                    	String retVal = bundle.get(AccountManager.KEY_AUTHTOKEN).toString();
                    	
                    	if (!connected) {
                     	   if (!GTalkThread.isAlive()) {
                     		   try {
                     			   	GTalkThread.setAuth(retVal, ACCOUNT.name);
                     			  	GTalkThread.start();
             	       			} catch (IllegalThreadStateException e) {
                 	       			GTalkThread = new GTalkConnectionThread();
                     	       		GTalkThread.setAuth(retVal, ACCOUNT.name);
                     	       		GTalkThread.start();
             	       			}
                     	   	}
                        }
                    	
                    }
            } catch (OperationCanceledException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
            } catch (AuthenticatorException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
            } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
            }
        } 
	};
}
