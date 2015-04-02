package com.chipsetsv.multipaint;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.ice4j.Transport;
import org.ice4j.TransportAddress;
import org.ice4j.ice.Agent;
import org.ice4j.ice.Candidate;
import org.ice4j.ice.CandidatePair;
import org.ice4j.ice.CheckList;
import org.ice4j.ice.Component;
import org.ice4j.ice.IceMediaStream;
import org.ice4j.ice.IceProcessingState;
import org.ice4j.ice.LocalCandidate;
import org.ice4j.ice.harvest.StunCandidateHarvester;
import org.ice4j.ice.harvest.TurnCandidateHarvester;
import org.ice4j.pseudotcp.PseudoTcpSocket;
import org.ice4j.pseudotcp.PseudoTcpSocketFactory;
import org.ice4j.security.LongTermCredential;

public class IceConnection {
	/**
     * The logger.
     */
    private static final Logger logger =
        Logger.getLogger(CheckList.class.getName());
    public static Logger getLogger() {
		return logger;
	}
    private static long startTime;
    public static long getStartTime() {
		return startTime;
	}
	public static void setStartTime(long startTime) {
		IceConnection.startTime = startTime;
	}
    /**
     * Local job thread variable
     */
    private static LocalPseudoTcpJob localJob = null;
    static LocalPseudoTcpJob getLocalJob() {
		return localJob;
	}
    /**
     * Remote job thread variable
     */
    private static RemotePseudoTcpJob remoteJob = null;
    static RemotePseudoTcpJob getRemoteJob() {
		return remoteJob;
	}
    /**
     * Test data size
     */
    private static final int TEST_BYTES_COUNT = 100;
    /**
     * Flag inidcates if STUN should be used
     */
    private static final boolean USE_STUN = true;
    /**
     * Flag inidcates if TURN should be used
     */
    private static final boolean USE_TURN = true;
    /**
     * Monitor object used to wait for remote agent to finish it's job
     */
    private static final Object remoteAgentMonitor = new Object();
    public static Object getRemoteAgentMonitor() {
		return remoteAgentMonitor;
	}
    /**
     * Monitor object used to wait for local agent to finish it's job
     */
    private static final Object localAgentMonitor = new Object();
    public static Object getLocalAgentMonitor() {
		return localAgentMonitor;
	}
    /**
     * Timeout for ICE discovery
     */
    public static long agentJobTimeout = 15000;

    

	protected static Agent createAgent(int pTcpPort)
        throws Throwable
    {
        Agent agent = new Agent();
        // STUN
        if (USE_STUN)
        {
            StunCandidateHarvester stunHarv = new StunCandidateHarvester(
                new TransportAddress("sip-communicator.net",
                                     3478, Transport.UDP));
            StunCandidateHarvester stun6Harv = new StunCandidateHarvester(
                new TransportAddress("ipv6.sip-communicator.net",
                                     3478, Transport.UDP));

            agent.addCandidateHarvester(stunHarv);
            agent.addCandidateHarvester(stun6Harv);
        }
        // TURN 
        if (USE_TURN)
        {
            String[] hostnames = new String[]
            {
                "130.79.90.150",
                "2001:660:4701:1001:230:5ff:fe1a:805f"
            };
            int port = 3478;
            LongTermCredential longTermCredential = new LongTermCredential(
                "guest", "anonymouspower!!");

            for (String hostname : hostnames)
            {
                agent.addCandidateHarvester(new TurnCandidateHarvester(
                    new TransportAddress(hostname, port,
                                         Transport.UDP), longTermCredential));
            }
        }
        //STREAM
        createStream(pTcpPort, "data", agent);

        return agent;
    }

    private static IceMediaStream createStream(int pTcpPort,
                                               String streamName,
                                               Agent agent)
        throws Throwable
    {
        IceMediaStream stream = agent.createMediaStream(streamName);

        long startTime = System.currentTimeMillis();

        //udp component
        agent.createComponent(
            stream, Transport.UDP, pTcpPort, pTcpPort, pTcpPort + 100);

        long endTime = System.currentTimeMillis();
        logger.log(Level.INFO,
                   "UDP Component created in " + (endTime - startTime) + " ms");
        startTime = endTime;

        return stream;
    }

    public static final class LocalIceProcessingListener
    implements PropertyChangeListener
    {
	    /**
	     * System.exit()s as soon as ICE processing enters a final state.
	     *
	     * @param evt the {@link PropertyChangeEvent} containing the old and new
	     * states of ICE processing.
	     */
	    public void propertyChange(PropertyChangeEvent evt)
	    {
	        long processingEndTime = System.currentTimeMillis();
	
	        Object iceProcessingState = evt.getNewValue();
	
	        logger.log(Level.INFO,
	                   "Local agent entered the " + iceProcessingState + " state.");
	        if (iceProcessingState == IceProcessingState.COMPLETED)
	        {
	            logger.log(Level.INFO,
	                       "Local - Total ICE processing time: "
	                + (processingEndTime - startTime) + "ms");
	            Agent agent = (Agent) evt.getSource();
	            logger.log(Level.INFO, "Local: Create pseudo tcp stream");
	            IceMediaStream dataStream = agent.getStream("data");
	            Component udpComponent = dataStream.getComponents().get(0);
	            CandidatePair selectedPair = udpComponent.getSelectedPair();
	            if (selectedPair != null)
	            {
	                LocalCandidate localCandidate = selectedPair.getLocalCandidate();
	                Candidate remoteCandidate = selectedPair.getRemoteCandidate();
	                logger.log(Level.INFO, "Local: " + localCandidate);
	                logger.log(Level.INFO, "Remote: " + remoteCandidate);
	                try
	                {
	                    localJob = new LocalPseudoTcpJob(
	                        localCandidate.getDatagramSocket());                        
	                }
	                catch (UnknownHostException ex)
	                {
	                    logger.log(Level.SEVERE,
	                               "Error while trying to create"
	                        + " local pseudotcp thread " + ex);
	                }
	            }
	            else
	            {
	                logger.log(Level.INFO, "Failed to select any candidate pair");
	            }
	        }
	        else
	        {
	            if (iceProcessingState == IceProcessingState.TERMINATED
	                || iceProcessingState == IceProcessingState.FAILED)
	            {
	                /*
	                 * Though the process will be instructed to die, demonstrate
	                 * that Agent instances are to be explicitly prepared for
	                 * garbage collection.
	                 */
	                if (localJob != null
	                		&& iceProcessingState == IceProcessingState.TERMINATED)
	                {
	                	localJob.start();                        
	                }
	                synchronized (localAgentMonitor)
	                {
	                    localAgentMonitor.notifyAll();
	                }
	            }
	        }
	    }
	}

    public static final class RemoteIceProcessingListener
    implements PropertyChangeListener
	{
	    /**
	     * System.exit()s as soon as ICE processing enters a final state.
	     *
	     * @param evt the {@link PropertyChangeEvent} containing the old and new
	     * states of ICE processing.
	     */
	    public void propertyChange(PropertyChangeEvent evt)
	    {
	        long processingEndTime = System.currentTimeMillis();
	
	        Object iceProcessingState = evt.getNewValue();
	
	        logger.log(Level.INFO,
	                   "Remote agent entered the " + iceProcessingState + " state.");
	        if (iceProcessingState == IceProcessingState.COMPLETED)
	        {
	            logger.log(Level.INFO,
	                       "Remote: Total ICE processing time: "
	                + (processingEndTime - startTime) + " ms ");
	            Agent agent = (Agent) evt.getSource();
	
	            logger.log(Level.INFO, "Remote: Create pseudo tcp stream");
	            IceMediaStream dataStream = agent.getStream("data");
	            Component udpComponent = dataStream.getComponents().get(0);
	            CandidatePair usedPair = udpComponent.getSelectedPair();
	            if (usedPair != null)
	            {
	                LocalCandidate localCandidate = usedPair.getLocalCandidate();
	                Candidate remoteCandidate = usedPair.getRemoteCandidate();
	                logger.log(Level.INFO,
	                           "Remote: Local address " + localCandidate);
	                logger.log(Level.INFO,
	                           "Remote: Peer address " + remoteCandidate);
	                try
	                {
	                    remoteJob = new RemotePseudoTcpJob(
	                        localCandidate.getDatagramSocket(),
	                        remoteCandidate.getTransportAddress());                        
	                }
	                catch (UnknownHostException ex)
	                {
	                    logger.log(Level.SEVERE,
	                               "Error while trying to create"
	                        + " remote pseudotcp thread " + ex);
	                }
	            }
	            else
	            {
	                logger.log(Level.SEVERE,
	                           "Remote: Failed to select any candidate pair");
	            }
	        }
	        else
	        {
	            if (iceProcessingState == IceProcessingState.TERMINATED
	                || iceProcessingState == IceProcessingState.FAILED)
	            {
	                /*
	                 * Though the process will be instructed to die, demonstrate
	                 * that Agent instances are to be explicitly prepared for
	                 * garbage collection.
	                 */
	                if (remoteJob != null
	                		&& iceProcessingState == IceProcessingState.TERMINATED)
	                {
	                	remoteJob.start();                        
	                }
	                synchronized (remoteAgentMonitor)
	                {
	                    remoteAgentMonitor.notifyAll();
	                }
	            }
	        }
	    }
	}

   
    
    public static class LocalPseudoTcpJob extends Thread implements Runnable
    {
    	private DatagramSocket dgramSocket;

        public LocalPseudoTcpJob(DatagramSocket socket)
            throws UnknownHostException
        {
            this.dgramSocket = socket;
        }

        @Override
        public void run()
        {
            logger.log(Level.FINEST, "Local pseudotcp worker started");
            try
            {
                logger.log(Level.INFO,
                           "Local pseudotcp is using: " 
                    + dgramSocket.getLocalSocketAddress()+dgramSocket);
                
                PseudoTcpSocket socket = new PseudoTcpSocketFactory().
                    createSocket(dgramSocket);
                socket.setConversationID(1073741824);
                //socket.setMTU(1500);
                socket.setDebugName("L");
                socket.accept(150000);                
                byte[] buffer = new byte[TEST_BYTES_COUNT];
                int read = 0;
                while (read != TEST_BYTES_COUNT)
                {
                    read += socket.getInputStream().read(buffer);
                    logger.log(Level.FINEST, "Local job read: " + read);
                }
                //TODO: close when all received data is acked
                //socket.close();
            }
            catch (IOException e)
            {
                throw new RuntimeException(e);
            }
            logger.log(Level.FINEST, "Local pseudotcp worker finished");
        }
    }

    public static class RemotePseudoTcpJob extends Thread implements Runnable
    {
        private DatagramSocket dgramSocket;
        private InetSocketAddress peerAddr;

        public RemotePseudoTcpJob(DatagramSocket socket,
                                  InetSocketAddress peerAddr)
            throws UnknownHostException
        {
        	this.dgramSocket = socket;
            this.peerAddr = peerAddr;
        }

        @Override
        public void run()
        {
            logger.log(Level.FINEST, "Remote pseudotcp worker started");
            try
            {
                logger.log(Level.INFO,
                           "Remote pseudotcp is using: " +
                           dgramSocket.getLocalSocketAddress()
                           +" and will comunicate with: " + peerAddr);
                PseudoTcpSocket socket = new PseudoTcpSocketFactory().
                    createSocket(dgramSocket);
                socket.setConversationID(1073741824);
                //socket.setMTU(1500);
                socket.setDebugName("R");
                long start, end;
                start = System.currentTimeMillis();
                socket.connect(peerAddr, 150000);
                byte[] buffer = new byte[TEST_BYTES_COUNT];
                socket.getOutputStream().write(buffer);
                socket.getOutputStream().flush();
                //Socket will be closed by the iceAgent
                //socket.close();
                end = System.currentTimeMillis();
                logger.log(Level.INFO,
                           "Transferred " + TEST_BYTES_COUNT
                    + " bytes in " + ((end - start) / 1000) + " sec");
            }
            catch (IOException e)
            {
                throw new RuntimeException(e);
            }
            logger.log(Level.FINEST, "Remote pseudotcp worker finished");
        }
    }
    
}
