package com.chipsetsv.multipaint;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

// Функционал для создания сессии подключения
public class SessionUtils {
	
	private static String connectToSessionServer(StringBuilder request) {
    	String citateRequestString = "http://192.168.1.2/test";
		String resultString = new String("");
		try {
			URLConnection connection = null;
			URL url = new URL(citateRequestString);
			connection = url.openConnection();
 
			HttpURLConnection httpConnection = (HttpURLConnection)connection;
			httpConnection.setRequestMethod("POST");
 
			httpConnection.setRequestProperty("User-Agent", "MyAndroid/1.6");
			httpConnection.setRequestProperty("Content-Language", "ru-RU");
			httpConnection.setRequestProperty("Content-Type", "text/plain");

 
			httpConnection.setDoOutput(true);
			httpConnection.setDoInput(true);
 
			httpConnection.connect();

			// здесь можем писать в поток данные запроса
			OutputStream os = httpConnection.getOutputStream();

			os.write(request.toString().getBytes()); 
 
			os.flush();
			os.close();

			int responseCode = httpConnection.getResponseCode();
			if (responseCode == HttpURLConnection.HTTP_OK) {
				InputStream in = httpConnection.getInputStream();
         
				InputStreamReader isr = new InputStreamReader(in, "UTF-8");

                StringBuffer data = new StringBuffer();
                int c;
                while ((c = isr.read()) != -1){
                	data.append((char) c);
                }
                resultString = new String (data.toString());
			}
            else {
            	resultString = "Server does not respond";
            }
		}
		catch (MalformedURLException e) { 
			resultString = "MalformedURLException:" + e.getMessage();
		}
		catch (IOException e) { 
			resultString = "IOException:" + e.getMessage();
		}
                
		return resultString; 
    }
    
	public static String createSession(String sdp) {
		StringBuilder str = new StringBuilder("CreateSession");
		str.append(";");
		str.append(sdp);
		
		return connectToSessionServer(str);
    }
    
	public static String getMasterSdpInSession(String id, String sdp) {
		StringBuilder str = new StringBuilder("GetMasterSdpInSession");
		str.append(";");
		str.append(id);
		str.append(";");
		str.append(sdp);
		
		return connectToSessionServer(str);
    }
    
	public static String getSlaveSdpInSession(String id) {
		StringBuilder str = new StringBuilder("GetSlaveSdpInSession");
		str.append(";");
		str.append(id);
		
		return connectToSessionServer(str);
    }
}
