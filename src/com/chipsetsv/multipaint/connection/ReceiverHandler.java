package com.chipsetsv.multipaint.connection;

import android.os.Handler;
import android.os.Message;

public class ReceiverHandler extends Handler {
	private OnReceiveEvent delegate;
	
	public void handleMessage(Message message)
	{
		if (delegate != null) {
			String msg = (String)message.obj;
			delegate.onReceive(msg);
		}
	}
	
	public void setOnReceive(OnReceiveEvent listener) {
		delegate = listener;
	}
	
	public void removeOnReceive()
	{
		delegate = null;
	}
}


