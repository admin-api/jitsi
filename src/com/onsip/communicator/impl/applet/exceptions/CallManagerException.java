package com.onsip.communicator.impl.applet.exceptions;

import net.java.sip.communicator.service.protocol.Call;
import net.java.sip.communicator.service.protocol.CallPeer;

public class CallManagerException
    extends RuntimeException
{                   
    private static final long serialVersionUID = 1L;
    
    private static final String error = 
        "The call could not be found, make sure the call id is correct";
    
    private Call call = null;
    private CallPeer peer = null;
    
    public CallManagerException(Call call)
    {
        super(error);
        this.call = call;                
    }
    
    public CallManagerException(Call call, CallPeer peer)
    {
        super(error);
        this.call = call;                
        this.peer = peer;
    }
    
    public CallManagerException(Call call, String message)
    {
        super(message);
        this.call = call;                
    }
    
    public CallManagerException(Call call, CallPeer peer, String message)
    {
        super(message);
        this.call = call;                
        this.peer = peer;
    }
    
    public Call getCall()
    {
        return this.call;
    }
    
    public CallPeer getCallPeer() 
    {
        return this.peer;
    }
}


