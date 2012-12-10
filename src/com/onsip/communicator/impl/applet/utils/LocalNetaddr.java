package com.onsip.communicator.impl.applet.utils;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import net.java.sip.communicator.util.Logger;
import net.java.sip.communicator.util.NetworkUtils;

public class LocalNetaddr
{
    private final static Logger logger
        = Logger.getLogger(LocalNetaddr.class);

    private static void execHostnameCmd()
    {
        BufferedReader buf = null;
        try
        {
            String line = null;
            String cmd = "hostname";
            logger.info(cmd);
            Runtime run = Runtime.getRuntime();
            Process pr = run.exec(cmd);
            pr.waitFor();
            buf = new BufferedReader(new InputStreamReader(pr.getInputStream()));
            while((line = buf.readLine()) != null)
            {
                line = line.trim();
                logger.info("exec hostname returned: " + line);
                System.setProperty("onsip.host_name", line);
            }
        }
        catch(Exception ex)
        {
            logger.error("Failed to execute Hostname cmd", ex);
        }
        finally
        {
            try
            {
                if (buf != null)
                {
                    buf.close();
                }
            }
            catch(Exception ex2){}
        }
    }

    private static String execRouteCmd()
    {
        BufferedReader buf = null;
        try
        {
            String line = null;
            String iface = null;
            String gateway = null;
            String cmd = "route -n get www.google.com" ;
            logger.info(cmd);
            Runtime run = Runtime.getRuntime();
            Process pr = run.exec(cmd);
            pr.waitFor();
            buf = new BufferedReader(new InputStreamReader(pr.getInputStream()));
            while((line = buf.readLine()) != null)
            {
                line = line.trim().toLowerCase();
                String[] tokens = line.split(":");
                if (tokens.length == 2)
                {
                    if (tokens[0].trim().equals("gateway"))
                    {
                        gateway = tokens[1].trim();
                        logger.info("found gateway " + gateway);
                        System.setProperty("onsip.host_gateway", gateway);
                    }
                    else if (tokens[0].trim().equals("interface"))
                    {
                        iface = tokens[1].trim();
                    }
                }
            }
            return iface;
        }
        catch(Exception ex)
        {
            logger.error("Failed to execute Route cmd", ex);
        }
        finally
        {
            try
            {
                if (buf != null)
                {
                    buf.close();
                }
            }
            catch(Exception ex2){}
        }
        return null;
    }

    private static void execConfigCmd(String iface)
    {
        BufferedReader buf = null;
        try
        {
            String cmd = "ifconfig " + iface;
            logger.info("running " + cmd);
            Runtime run = Runtime.getRuntime();
            Process pr = run.exec(cmd) ;
            pr.waitFor();
            buf = new BufferedReader(new InputStreamReader(pr.getInputStream()));
            String line = null;
            while((line = buf.readLine()) != null)
            {
                line = line.trim().toLowerCase();
                String tokens [] = line.split(" ");
                if (tokens.length > 1)
                {
                    if(tokens[0].equals("inet"))
                    {
                        logger.info("Found inet line " + line);
                        String addr = tokens[1];
                        logger.info("Will use IP " + addr);
                        if (NetworkUtils.isIPv4Address(addr))
                        {
                            System.setProperty("onsip.host_address", addr);
                        }
                    }
                    else if(tokens[0].equals("inet6"))
                    {
                        // place holder
                    }
                }
            }
        }
        catch(Exception ex)
        {
            logger.error("Failed to execute config", ex);
        }
        finally
        {
            try
            {
                if (buf != null)
                {
                    buf.close();
                }
            }
            catch(Exception ex2){}
        }
    }

    public static void setConfigRoute()
    {
        try
        {
            System.setProperty("onsip.host_address", "");
            System.setProperty("onsip.host_gateway", "");
            System.setProperty("onsip.host_name", "");
            String os = System.getProperty("os.name");
            os = (os == null) ? "" : os.toLowerCase();
            if (os.indexOf("mac os x") != -1)
            {
                String version = System.getProperty("os.version");
                if (version.startsWith("10.7") || version.startsWith("10.8"))
                {
                    String iface = execRouteCmd();
                    if (iface != null)
                    {
                        execConfigCmd(iface);
                        execHostnameCmd();
                    }
                }
            }
        }
        catch(Exception ex)
        {
            System.setProperty("onsip.host_address", "");
            System.setProperty("onsip.host_gateway", "");
            System.setProperty("onsip.host_name", "");
            logger.debug("Failed to exec runtime config commands", ex);
        }
    }
}
