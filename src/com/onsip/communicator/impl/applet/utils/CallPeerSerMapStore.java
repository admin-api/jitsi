package com.onsip.communicator.impl.applet.utils;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import net.java.sip.communicator.util.Logger;

public class CallPeerSerMapStore
{
    private final static Logger logger
        = Logger.getLogger(CallPeerSerMapStore.class);
    
    private static
        Map<String, CallPeerSerMapStore[]> callIdPeerSerialStore = 
            Collections.synchronizedMap
                (new HashMap<String, CallPeerSerMapStore[]>(5));
    
    private String peerId = null;
    private String peerSerialized = null;
    
    private CallPeerSerMapStore
        (String peerId, String peerSerialized)
    {        
        this.setPeerId(peerId);
        this.setPeerSerialized(peerSerialized);        
    }
           
    public static void put(String callId, String peerId, String peerSerialized)
    {
        CallPeerSerMapStore [] serPeers = get(callId);
        CallPeerSerMapStore [] newPeers = null;
        boolean replace = false;
        int idx = 0;
        if (serPeers == null)
        {
            newPeers = new CallPeerSerMapStore[1];            
        }
        else
        {
            newPeers = new CallPeerSerMapStore[serPeers.length + 1];
            for (int i=0; i < serPeers.length; i++)
            {                                
                if (serPeers[i].getPeerId().equals(peerId))
                {
                    newPeers[i] = new CallPeerSerMapStore(peerId, peerSerialized);;
                    replace = true;                    
                }
                else
                {
                    newPeers[i] = serPeers[i];
                }
                idx = i + 1;
            }
        }        
        if (!replace)
        {
            logger.debug("adding to peer serialization store " 
                + callId + " -> " + peerId);
            newPeers[idx] = new CallPeerSerMapStore(peerId, peerSerialized);
            callIdPeerSerialStore.put(callId, newPeers);
        }
        else
        {
            logger.debug("replacing peer serialization store " 
                + callId + " -> " + peerId);
            serPeers = new CallPeerSerMapStore[serPeers.length];
            System.arraycopy(newPeers, 0, serPeers, 0, serPeers.length);
            callIdPeerSerialStore.put(callId, serPeers);
        }                
    }
    
    public static void remove(String callId)
    {
        callIdPeerSerialStore.remove(callId);
    }
    
    public static void remove(String callId, String peerId)
    {        
        CallPeerSerMapStore [] serPeers = get(callId);
        CallPeerSerMapStore [] tmpPeers = 
            new CallPeerSerMapStore[serPeers.length];
        CallPeerSerMapStore [] newPeers = null; 
        int idx = 0;
        if (serPeers != null)
        {
            for (int i=0; i < serPeers.length; i++)
            {
                CallPeerSerMapStore serPeer = serPeers[i];
                if (!serPeer.getPeerId().equals(peerId))
                {
                    tmpPeers[idx] = serPeers[i];
                    idx++;
                }
                
            }
            newPeers = new CallPeerSerMapStore[idx];
            for (int i=0; i < idx; i++)
            {
                newPeers[i] = tmpPeers[i];
            }
            callIdPeerSerialStore.put(callId, newPeers);
        }        
        
    }
    
    public static String get(String callId, String peerId)
    {
        CallPeerSerMapStore [] serPeers = get(callId);
               
        for (int i=0; i < serPeers.length; i++)
        {                                        
            if (serPeers[i].equals(peerId))
            {
                return serPeers[i].peerSerialized;
            }           
        }               
        return "";
    }
            
    public static CallPeerSerMapStore[] get(String callId)
    {        
        CallPeerSerMapStore[] peersSer = callIdPeerSerialStore.get(callId);
        if (peersSer != null)
        {
            return peersSer;
        }
        else
        {
            return new CallPeerSerMapStore[0];
        }
    }

    public static int size()
    {
        if (callIdPeerSerialStore != null)
        {
            return callIdPeerSerialStore.size();
        }
        return 0;
    }
    
    public void setPeerId(String peerId)
    {
        this.peerId = peerId;
    }

    public String getPeerId()
    {
        return peerId;
    }

    public void setPeerSerialized(String peerSerialized)
    {
        this.peerSerialized = peerSerialized;
    }

    public String getPeerSerialized()
    {
        return peerSerialized;
    }
    
    
    
}
