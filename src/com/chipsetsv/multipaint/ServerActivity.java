package com.chipsetsv.multipaint;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.logging.Level;

import org.apache.http.conn.util.InetAddressUtils;
import org.ice4j.ice.Agent;
import org.ice4j.ice.IceMediaStream;
import org.ice4j.ice.NominationStrategy;

import com.chipsetsv.multipaint.IceConnection.LocalPseudoTcpJob;
import com.chipsetsv.multipaint.connection.Connection;
import com.chipsetsv.multipaint.connection.OnReceiveEvent;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

public class ServerActivity extends Activity {

	private TextView serverStatus;
	private Button initiateConnection;

    // designate a port
    public static final int SERVERPORT = 8080;

    private final Handler handler = new Handler();
    private String remoteSdp;
    private String sessionId;
    private static Thread iceInitial;
    private static Thread iceConnect;
    private static Agent localAgent;

	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_server);
        
        serverStatus = (TextView) findViewById(R.id.server_status);
        initiateConnection = (Button) findViewById(R.id.button_connect);
        initiateConnection.setOnClickListener(connectListener);
        
        
        
//        Core.getServer().getHandler().setOnReceive(new OnReceiveListener() {
//			
//			@Override
//			public void onReceive(Message msg) {
//				serverStatus.setText((String)msg.obj);
//				
//			}
//		});
        Connection.getServer().accept();
        
//        if (iceInitial != null && iceInitial.isAlive())
//    		return;
//		iceInitial = new Thread(new Runnable() {
//		    @Override
//			public void run() {
//		    	try {
//					runServer();
//				} catch (Throwable e) {
//					// TODO Auto-generated catch block
//					e.printStackTrace();
//				}
//		    }
//		});
//		iceInitial.start();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_server, menu);
        return true;
    }
    
    private final OnClickListener connectListener = new OnClickListener() {

        public void onClick(View v) {
        	if (Connection.getServer().getConnected())
        		serverStatus.setText("Connected");
        	else
        		serverStatus.setText("Non connected");
        		
//            if (!connected) {
//                serverIpAddress = serverStatus.getText().toString();
//               if (!serverIpAddress.equals("")) {
//                    Thread cThread = new Thread(new ClientThread());
//                    cThread.start();
//                }
//            }
        	
//        	if (iceConnect != null && iceConnect.isAlive())
//        		return;
//        	iceConnect = new Thread(new Runnable() {
//			    @Override
//				public void run() {
//			    	try {
//						runConnect(sessionId);
//					} catch (Throwable e) {
//						// TODO Auto-generated catch block
//						e.printStackTrace();
//					}
//			    }
//			});
//        	iceConnect.start();
        }
    };
    
    
    
    
    

    @Override
    protected void onStop() {
        super.onStop();
		Connection.getServer().getStatusHandler().removeOnReceive();
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        Connection.getServer().getStatusHandler().removeOnReceive();
    }
    
    @Override
    protected void onResume() {
    	super.onResume();
    	Connection.getServer().getStatusHandler().setOnReceive(new OnReceiveEvent() {
			
			@Override
			public void onReceive(String msg) {
				serverStatus.setText(msg);
			}
		});
    }
    
    
    
    
    
    
    
    public void runServer() throws Throwable
    {
        IceConnection.setStartTime(System.currentTimeMillis());

        int localPort = 7999;
        int remotePort = 6000;

        localAgent = IceConnection.createAgent(localPort);
        localAgent.setNominationStrategy(
            NominationStrategy.NOMINATE_HIGHEST_PRIO);
        
        
        String localSDP = SdpUtils.createSDPDescription(localAgent);
               
        sessionId = SessionUtils.createSession(localSDP);
        handler.post(new Runnable() {
            public void run() {
            	serverStatus.setText(sessionId);
            }
        });
        //serverIp.setText(getMasterSdpInSession("17", localSDP));
        //serverIp.setText(utils.getSlaveSdpInSession("17"));
      
        //Agent remotePeer =
        //    createAgent(remotePort);

        
    }
    
    
    
    
    public void runConnect(String id) throws Throwable
    {
    	remoteSdp = SessionUtils.getSlaveSdpInSession(id);
    	if (remoteSdp == "")
    		return;
    	SdpUtils.parseSDP(localAgent, remoteSdp);
    	
    	localAgent.addStateChangeListener(new IceConnection.LocalIceProcessingListener());
        //remotePeer.addStateChangeListener(new ClientActivity.RemoteIceProcessingListener());

        //let them fight ... fights forge character.
        localAgent.setControlling(true);
        //remotePeer.setControlling(false);

        long endTime = System.currentTimeMillis();

        
        
        
     
        IceConnection.getLogger().log(Level.INFO, "Total candidate gathering time: {0} ms",
                   (endTime - IceConnection.getStartTime()));
        IceConnection.getLogger().log(Level.INFO, "LocalAgent: {0}",
                   localAgent);

        localAgent.startConnectivityEstablishment();

        //if (START_CONNECTIVITY_ESTABLISHMENT_OF_REMOTE_PEER)
        //remotePeer.startConnectivityEstablishment();


        IceMediaStream dataStream = localAgent.getStream("data");

        if (dataStream != null)
        {
        	IceConnection.getLogger().log(Level.INFO,
                       "Local data clist:" + dataStream.getCheckList());
        }
        //wait for one of the agents to complete it's job 
        
        synchronized (IceConnection.getLocalAgentMonitor())
        {
        	IceConnection.getLocalAgentMonitor().wait(IceConnection.agentJobTimeout);
        }
        if (IceConnection.getRemoteJob() != null)
        {
        	IceConnection.getLogger().log(Level.FINEST, "Remote thread join started");
        	IceConnection.getRemoteJob().join();
            IceConnection.getLogger().log(Level.FINEST, "Remote thread joined");
        }
        if (IceConnection.getLocalJob() != null)
        {
        	IceConnection.getLogger().log(Level.FINEST, "Local thread join started");
            IceConnection.getLocalJob().join();
            IceConnection.getLogger().log(Level.FINEST, "Local thread joined");
        }
       
       
    }

}
