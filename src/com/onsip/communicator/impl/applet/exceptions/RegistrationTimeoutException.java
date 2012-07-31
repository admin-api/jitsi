package com.onsip.communicator.impl.applet.exceptions;

public class RegistrationTimeoutException
    extends RuntimeException
{                   
    private static final long serialVersionUID = 1L;
    
    private static final String error = 
        "Registration timed out";
    
    public RegistrationTimeoutException()
    {
        super(error);
    }
    
    public RegistrationTimeoutException(String s)
    {
        super(s);
    }      
}
