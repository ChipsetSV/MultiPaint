package com.chipsetsv.multipaint.connection.xmpp;

import org.jivesoftware.smack.packet.Packet;

public class AuthMechanism extends Packet {
    final private String name;
    final private String authenticationText;

    public AuthMechanism(String name, String authenticationText) {
        if (name == null) {
            throw new NullPointerException("SASL mechanism name shouldn't be null.");
        }
        this.name = name;
        this.authenticationText = authenticationText;
    }

    public String toXML() {
        StringBuilder stanza = new StringBuilder();
        stanza.append("<auth mechanism=\"").append(name);
        stanza.append("\" xmlns=\"urn:ietf:params:xml:ns:xmpp-sasl\">");
        if (authenticationText != null &&
                authenticationText.trim().length() > 0) {
            stanza.append(authenticationText);
        }
        stanza.append("</auth>");
        return stanza.toString();
    }
}
