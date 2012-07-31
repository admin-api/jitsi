package com.onsip.communicator.impl.applet.exceptions;

public class MissingCallIdException
    extends RuntimeException
{               
    private static final long serialVersionUID = 1L;
    
    private static final String error = "The call id is required, but missing";
    
    public MissingCallIdException()
    {
        super(error);
    }
    
    public MissingCallIdException(String s)
    {
        super(s);
    }   
}