package com.onsip.communicator.impl.applet.exceptions;

public class CallNotFoundException
    extends RuntimeException
{                   
    private static final long serialVersionUID = 1L;
    
    private static final String error = 
        "The call could not be found, make sure the call id is correct";
    
    public CallNotFoundException()
    {
        super(error);
    }
    
    public CallNotFoundException(String s)
    {
        super(s);
    }      
}


