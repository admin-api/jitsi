package com.onsip.communicator.impl.applet.utils;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import net.java.sip.communicator.util.Logger;

public class CallToCallSetupIdStore
{
    private static Map<String, String> callToCallSetupIds = 
        Collections.synchronizedMap(new HashMap<String, String>(5));
 
    private final static Logger logger
        = Logger.getLogger(CallToCallSetupIdStore.class);
    
    public static String get(String callId)
    {
        String callSetupId = "";
        try
        {
            if (callId != null)
            {
                callSetupId = callToCallSetupIds.get(callId);
                if (callSetupId != null)
                {
                    logger.info("Found setupId [" + callSetupId + "]" +
                        " in callId [" + callId + "]");
                }
                else
                {
                    logger.info("NO setupId was found for callId [" + callId + "]");
                    return "";
                }                
            }
        }
        catch(Exception e)
        {
            /* 
             * These are somewhat important errors, so
             * we log them and hope that the client side 
             * application will just work 
             */            
            logger.error("Exception :: get :");
            logger.error(e, e);
        }
        return callSetupId;
    }
    
    public static void put(String callId, String setupId)
    {
        try
        {
            if (setupId != null)
            {                
                setupId = setupId.trim();
                if (setupId.length() > 0)
                {
                    logger.info("Adding setupId [" + 
                        setupId + "] to callId [" + callId + "]");
                    callToCallSetupIds.put(callId, setupId);
                }
            }
        }
        catch(Exception e)
        {
            /* 
             * These are somewhat important errors, so
             * we log them and hope that the client side 
             * application will just work 
             */
            logger.error("Exception :: put :");
            logger.error(e,e);
        }
    }
    
    public static void remove(String callId)
    {
        try
        {
            logger.info("Removing setupId from " +
                "callId [" + callId + "]");
            callToCallSetupIds.remove(callId);
        }
        catch(Exception e)
        {
            /* 
             * These are somewhat important errors, so
             * we log them and hope that the client side 
             * application will just work despite 
             */
            logger.error("Exception :: remove :");
            logger.error(e, e);
        }
    }
}
