package com.chipsetsv.multipaint;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.util.logging.Level;

import org.ice4j.ice.Agent;
import org.ice4j.ice.IceMediaStream;
import org.ice4j.ice.NominationStrategy;

import com.chipsetsv.multipaint.connection.Connection;
import com.chipsetsv.multipaint.connection.OnReceiveEvent;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.InputFilter.LengthFilter;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class ClientActivity extends Activity {

	private EditText sessionIdEdit;
    private Button connectPhones;
    
    
    
    private String remoteSdp;
    private static Thread ice;
    private static Handler handler;

    
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_client);
        
        sessionIdEdit = (EditText) findViewById(R.id.session_id);
        connectPhones = (Button) findViewById(R.id.connect_phones);
        connectPhones.setOnClickListener(connectListener);
        
        sessionIdEdit.setText(Connection.getClient().getAddress());
        //StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        //StrictMode.setThreadPolicy(policy); 
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_client, menu);
        return true;
    }
    
    private final OnClickListener connectListener = new OnClickListener() {

        public void onClick(View v) {
            if (!Connection.isConnected()) {
            	Connection.getClient().setAddress(sessionIdEdit.getText().toString());
                Connection.getClient().connect();
                
            }
            else {
            	Connection.getClient().sendMessage("message");
            }
        	
//        	Thread thread = new Thread(new Runnable() {
//			    @Override
//				public void run() {
//        	IcePseudoTcp tcp = new IcePseudoTcp();
//        	try {
//				tcp.run();
//			} catch (Throwable e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//			    }
//        	});
//        	thread.start();
        	
        	
//        	if (ice != null && ice.isAlive())
//        		return;
//			ice = new Thread(new Runnable() {
//			    @Override
//				public void run() {
//			    	try {
//			    		remoteSdp = sessionIdEdit.getText().toString();
//						runClient(remoteSdp);
//					} catch (Throwable e) {
//						// TODO Auto-generated catch block
//						e.printStackTrace();
//					}
//			    }
//			});
//			ice.start();
			
        }
    };

    
    
    @Override
    protected void onStop() {
        super.onStop();
        Connection.getClient().getStatusHandler().removeOnReceive();
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        Connection.getClient().getStatusHandler().removeOnReceive();
    }
    
    @Override
    protected void onResume() {
    	super.onResume();
    	Connection.getClient().getStatusHandler().setOnReceive(new OnReceiveEvent() {
			
			@Override
			public void onReceive(String msg) {
				Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
			}
		});
    }
    
    
    
    
    
    
    public void runClient(String id) throws Throwable
    {
        IceConnection.setStartTime(System.currentTimeMillis());

        int localPort = 7999;
        //int remotePort = 6000;

        Agent localAgent = IceConnection.createAgent(localPort);
        //localAgent.setNominationStrategy(
        //    NominationStrategy.NOMINATE_HIGHEST_PRIO);
        
        
        String localSDP = SdpUtils.createSDPDescription(localAgent);
               
        remoteSdp = SessionUtils.getMasterSdpInSession(id, localSDP);
        
        if (remoteSdp == "")
        	return;
        handler.post(new Runnable() {
            public void run() {
            	sessionIdEdit.setText(remoteSdp);
            }
        });
        //serverIp.setText(getMasterSdpInSession("17", localSDP));
        //serverIp.setText(utils.getSlaveSdpInSession("17"));
        
        //Agent remotePeer =
        //		IceConnection.createAgent(remotePort);
        SdpUtils.parseSDP(localAgent, remoteSdp);
        localAgent.addStateChangeListener(new IceConnection.RemoteIceProcessingListener());
        //remotePeer.addStateChangeListener(new IceConnection.RemoteIceProcessingListener());

        //let them fight ... fights forge character.
        localAgent.setControlling(false);
        //remotePeer.setControlling(true);

        long endTime = System.currentTimeMillis();

        
        //Ice.transferRemoteCandidates(localAgent, remotePeer);
        //for (IceMediaStream stream : localAgent.getStreams())
        //{
        //    stream.setRemoteUfrag(remotePeer.getLocalUfrag());
        //    stream.setRemotePassword(remotePeer.getLocalPassword());
        //}

        //Ice.transferRemoteCandidates(remotePeer, localAgent);

        //for (IceMediaStream stream : remotePeer.getStreams())
        //{
        //    stream.setRemoteUfrag(localAgent.getLocalUfrag());
        //    stream.setRemotePassword(localAgent.getLocalPassword());
        //}

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
        synchronized (IceConnection.getRemoteAgentMonitor())
        {
        	IceConnection.getRemoteAgentMonitor().wait(IceConnection.agentJobTimeout);
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

















