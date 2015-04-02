package com.chipsetsv.multipaint.connection.xmpp;

import org.jivesoftware.smack.packet.Packet;

public class Auth2Mechanism extends Packet {
    String stanza;
    public Auth2Mechanism(String txt) {
        stanza = txt;
    }
    public String toXML() {
        return stanza;
    }
}
