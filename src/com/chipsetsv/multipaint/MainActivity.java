package com.chipsetsv.multipaint;

import org.ice4j.ice.Agent;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.chipsetsv.multipaint.connection.Connection;
import com.chipsetsv.multipaint.connection.OnReceiveEvent;

public class MainActivity extends Activity implements OnClickListener {

    protected Intent intent;
	
	private TextView tvStatusCaption;
	private TextView tvServerIp;
	private EditText etServerIp;
	
	private TextView tvSdp;
	private Button buttonSDP;
	
	private Button buttonClientConnect;
	private Button buttonStartServer;
	private Button buttonDraw;
	
	
	private final Handler handler = new Handler();
	private String localSDP;
    private static Thread iceInitial;
    private static Agent localAgent;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);
    	setContentView(R.layout.activity_main);
    	
    	buttonClientConnect = (Button)findViewById(R.id.buttonClient);
    	buttonStartServer = (Button)findViewById(R.id.buttonServer);
    	buttonDraw = (Button)findViewById(R.id.buttonDraw);
    	
    	tvStatusCaption = (TextView)findViewById(R.id.textViewStatus);
    	tvServerIp = (TextView)findViewById(R.id.textViewServerIp);
    	etServerIp = (EditText)findViewById(R.id.editTextServerIp);
    	
    	buttonClientConnect.setOnClickListener(this);
    	buttonStartServer.setOnClickListener(this);
    	buttonDraw.setOnClickListener(this);
    	
    	tvSdp = (TextView)findViewById(R.id.textViewSDP);
    	buttonSDP = (Button)findViewById(R.id.buttonCheckSDP);
    	buttonSDP.setOnClickListener(this);
    	
    	tvServerIp.setText(Connection.getServer().getAddress());
    	etServerIp.setText("192.168.1.20:8080");
    	
    	
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return true;
    }
    
   
    public void onClick(View v)
    {
    	Intent intent = new Intent();
    
    	switch (v.getId()) 
    	{
    	    case R.id.buttonClient:
		    	intent.setClass(this, AccountListActivity.class);
		        startActivity(intent);
    	    	//Connection.close();
    	    	//Connection.getClient().setAddress(etServerIp.getText().toString());
                //Connection.getClient().connect();
	        break;
    	    case R.id.buttonServer:
		    	//intent.setClass(this, ServerActivity.class);
		        //startActivity(intent);
    	    	Connection.close();
    	    	Connection.getServer().setAddress(etServerIp.getText().toString());
    	    	Connection.getServer().accept();
    	    break;
    	    case R.id.buttonDraw:
		    	intent.setClass(this, DrawActivity.class);
		        startActivity(intent);
    	    break;
    	    case R.id.buttonCheckSDP:
    	    	if (iceInitial != null && iceInitial.isAlive())
    	    		return;
    			iceInitial = new Thread(new Runnable() {
    			    @Override
    				public void run() {
    			    	try {
    						runServer();
    					} catch (Throwable e) {
    						// TODO Auto-generated catch block
    						e.printStackTrace();
    					}
    			    }
    			});
    			iceInitial.start();
    	    break;
    	}
    }
     
    
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event)  {
        if (keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0) {
        	Connection.close();
            this.finish();
            return true;
        }

        return super.onKeyDown(keyCode, event);
    }
    
    
    
    @Override
    protected void onStop() {
        super.onStop();
		Connection.getServer().getStatusHandler().removeOnReceive();
		Connection.getClient().getStatusHandler().removeOnReceive();
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        Connection.getServer().getStatusHandler().removeOnReceive();
        Connection.getClient().getStatusHandler().removeOnReceive();
    }
    
    @Override
    protected void onResume() {
    	super.onResume();
    	OnReceiveEvent statusEvent = new OnReceiveEvent() {
			
			@Override
			public void onReceive(String msg) {
				tvStatusCaption.setText(msg);
			}
		};
    	Connection.getServer().getStatusHandler().setOnReceive(statusEvent);
    	Connection.getClient().getStatusHandler().setOnReceive(statusEvent);
    }
    
    
    
    
    
    
    
    public void runServer() throws Throwable
    {
        int localPort = Connection.getServer().getPort();
        //int remotePort = Connection.getClient().getPort();

        localAgent = IceConnection.createAgent(localPort);
        //localAgent.setNominationStrategy(
        //    NominationStrategy.NOMINATE_HIGHEST_PRIO);
        
        localSDP = SdpUtils.createSDPDescription(localAgent);
               
        handler.post(new Runnable() {
            public void run() {
            	tvSdp.setText(localSDP);
            }
        });
        
        localAgent = null;      
    }
    
    
    
    
    
    
    
    
}
