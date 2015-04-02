package com.chipsetsv.multipaint.connection.xmpp;

import java.io.IOException;
import java.net.URLEncoder;

import org.jivesoftware.smack.SASLAuthentication;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.sasl.SASLMechanism;

import android.util.Base64;
import android.util.Log;

public class GTalkAuth extends SASLMechanism {
	
    public static final String NAME = "X-GOOGLE-TOKEN";

	public GTalkAuth( SASLAuthentication saslAuthentication )
    {
        super( saslAuthentication );
    }

    @Override
    protected String getName()
    {
        return NAME;
    }

    @Override
    public void authenticate( String username, String host, String password ) throws IOException, XMPPException
    {
        super.authenticate( username, host, password );
    }

    @Override
    protected void authenticate() throws IOException, XMPPException
    {
    	String authCode = password;
        String jidAndToken = "\0" + URLEncoder.encode( authenticationId, "utf-8" ) + "\0" + authCode;

        StringBuilder stanza = new StringBuilder();
        stanza.append( "<auth mechanism=\"" ).append( getName() );
        stanza.append( "\" xmlns=\"urn:ietf:params:xml:ns:xmpp-sasl\">" );
        stanza.append( new String(Base64.encode( jidAndToken.getBytes( "UTF-8" ), Base64.DEFAULT ) ) );
        stanza.append( "</auth>" );

        Log.v("BlueTalk", "Authentication text is " + stanza);
        // Send the authentication to the server
        getSASLAuthentication().send( new Auth2Mechanism(stanza.toString()) );
    } 
}


