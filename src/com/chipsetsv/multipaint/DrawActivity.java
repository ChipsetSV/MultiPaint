package com.chipsetsv.multipaint;

import android.app.Activity;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.Menu;

import com.chipsetsv.multipaint.connection.Connection;
import com.chipsetsv.multipaint.connection.OnSendEvent;
import com.chipsetsv.multipaint.connection.ReceiverHandler;
import com.chipsetsv.multipaint.controls.DrawCanvas;
import com.chipsetsv.multipaint.controls.Toolbar;


public class DrawActivity extends Activity {

	private DrawCanvas drawCanvas;
	private Toolbar toolbar;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_draw);
        
        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        drawCanvas = (DrawCanvas) findViewById(R.id.canvas);
        drawCanvas.setToolbar(toolbar);
        drawCanvas.setMetrics(metrics);
    }

    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_draw, menu);
        return true;
    }
    
    @Override
    protected void onStop() {
        super.onStop();
        ReceiverHandler handler = Connection.getReceiverHandler();
        if (handler != null) {
        	handler.removeOnReceive();
        }
        Connection.getServer().getStatusHandler().removeOnReceive();
        Connection.getClient().getStatusHandler().removeOnReceive();
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        ReceiverHandler handler = Connection.getReceiverHandler();
        if (handler != null) {
        	handler.removeOnReceive();
        }
        Connection.getServer().getStatusHandler().removeOnReceive();
        Connection.getClient().getStatusHandler().removeOnReceive();
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        ReceiverHandler handler = Connection.getReceiverHandler();
        if (handler != null) {
        	handler.setOnReceive(drawCanvas.getReceiveListener());
        }
        drawCanvas.setSendListener(new OnSendEvent() {
			@Override
			public void onSend(String message) {
				Connection.sendMessage(message);
			}
		});
    }
}
