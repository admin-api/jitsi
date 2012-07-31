package com.onsip.communicator.util.codec;

import java.util.Iterator;

import org.jitsi.service.neomedia.MediaStream;
import org.jitsi.service.neomedia.MediaType;
import net.java.sip.communicator.service.protocol.Call;
import net.java.sip.communicator.service.protocol.CallPeer;
import net.java.sip.communicator.service.protocol.media.MediaAwareCallPeer;

public class Codec
{
    public static String getCodec(CallPeer peer)
    {
        if (peer instanceof MediaAwareCallPeer)
        {
            @SuppressWarnings("rawtypes")
            MediaAwareCallPeer maware = (MediaAwareCallPeer) peer;
            if (maware != null)
            {
                if (maware.getMediaHandler() != null){                            
                    MediaStream stream = maware.getMediaHandler().getStream(MediaType.AUDIO);
                    if (stream != null){
                        String enc = stream.getFormat().getEncoding();
                        if (enc != null)
                        {
                            return enc;
                        }                                                               
                    }
                }                                          
            }
        }
        return "";
    }
    
    public static String getCodec(Call call, String peerId)
    {        
        Iterator<? extends CallPeer> peers = call.getCallPeers();
        while (peers.hasNext())
        {
            CallPeer peer = (CallPeer) peers.next();
            if (peer.getPeerID().equals(peerId))
            {
                return getCodec(peer);
            }
        }
        return "";
    }
}
