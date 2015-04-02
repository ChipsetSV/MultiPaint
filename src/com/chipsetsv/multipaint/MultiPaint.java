package com.chipsetsv.multipaint;

import android.app.Application;

import com.chipsetsv.multipaint.connection.Connection;

public class MultiPaint extends Application {
	
	private static Connection connection = new Connection();
	
	public static Connection getConnection() {
		return connection;
	}
	
}
