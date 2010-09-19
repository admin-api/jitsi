/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.protocol;

import java.io.*;
import java.util.*;

import net.java.sip.communicator.util.*;
import static net.java.sip.communicator.service.protocol.ProtocolProviderFactory.*;

/**
 * A <tt>StunServerDescriptor</tt> stores information necessary to create a
 * STUN or TURN candidate harvester that we could use with ICE4J. Descriptors
 * are normally initialized by protocol wizards. They are then used to convert
 * the data into a {@link String} form suitable for storage in an accounts
 * properties Map.
 *
 * @author Yana Stamcheva
 * @author Emil Ivov
 */
public class StunServerDescriptor
{
    /**
     * The maximum number of stun servers that we would allow.
     */
    public static final int MAX_STUN_SERVER_COUNT = 100;

    /**
     * The address (IP or FQDN) of the server.
     */
    private String address;

    /**
     * The port of the server.
     */
    private int port;

    /**
     * Indicates if the server can also act as a TURN server (as opposed to a
     * STUN only server).
     */
    private boolean isTurnSupported;

    /**
     * The username that we need to use with the server or <tt>null</tt> if
     * this server does not require a user name.
     */
    private byte[] username;

    /**
     * The password that we need to use when authenticating with the server
     * or <tt>null</tt> if no password is necessary.
     */
    private byte[] password;

    /**
     * Creates an instance of <tt>StunServer</tt> by specifying all parameters.
     *
     * @param address the IP address or FQDN of the STUN server
     * @param port the port of the server
     * @param supportTurn indicates if this STUN server supports TURN
     * @param username the user name for authenticating
     * @param password the password
     */
    public StunServerDescriptor( String  address,
                                 int     port,
                                 boolean supportTurn,
                                 String  username,
                                 String  password)
    {
        this.address = address;
        this.port = port;
        this.isTurnSupported = supportTurn;
        this.username = StringUtils.getUTF8Bytes( username );
        this.password = StringUtils.getUTF8Bytes( password );
    }

    /**
     * Returns the IP address or FQDN of this server.
     *
     * @return the IP address or FQDN of this server
     */
    public String getAddress()
    {
        return address;
    }

    /**
     * Sets the IP address or FQDN of this server.
     *
     * @param address the IP address or FQDN to set
     */
    public void setAddress(String address)
    {
        this.address = address;
    }

    /**
     * Returns the port of this server.
     *
     * @return the port of this server
     */
    public int getPort()
    {
        return port;
    }

    /**
     * Sets the port corresponding to this server.
     *
     * @param port the port to set
     */
    public void setPort(int port)
    {
        this.port = port;
    }

    /**
     * Indicates if TURN is supported by this server.
     *
     * @return <tt>true</tt> if TURN is supported by this server, otherwise -
     * returns <tt>false</tt>
     */
    public boolean isTurnSupported()
    {
        return isTurnSupported;
    }

    /**
     * Specifies whether this server can also act as a TURN relay.
     *
     * @param turnSupported <tt>true</tt> to indicate that TURN is supported,
     * <tt>false</tt> - otherwise
     */
    public void setTurnSupported(boolean turnSupported)
    {
        this.isTurnSupported = turnSupported;
    }

    /**
     * Returns the username associated to this server.
     *
     * @return the username associated to this server
     */
    public byte[] getUsername()
    {
        return username;
    }

    /**
     * Sets the username associated to this server.
     *
     * @param username the username to set
     */
    public void setUsername(String username)
    {
        this.username = StringUtils.getUTF8Bytes(username);
    }

    /**
     * Returns the password associated to this server username.
     *
     * @return the password associated to this server username
     */
    public byte[] getPassword()
    {
        return password;
    }

    /**
     * Sets the password associated to this server username.
     *
     * @param password the password to set
     */
    public void setPassword(String password)
    {
        this.password = StringUtils.getUTF8Bytes(password);
    }

    /**
     * Stores this descriptor into the specified {@link Map}.The method is meant
     * for use with account property maps. It also allows prepending an account
     * prefix to all property names so that multiple descriptors can be stored
     * in a single {@link Map}.
     *
     * @param props the account properties {@link Map} that we'd like to store
     * this descriptor in.
     * @param namePrefix the prefix that we should prepend to every property
     * name.
     */
    public void storeDescriptor(Map<String, String> props, String namePrefix)
    {
        if(namePrefix == null)
            namePrefix = "";

        props.put(namePrefix + STUN_ADDRESS, getAddress());

        if(getPort() != -1)
            props.put(namePrefix + STUN_PORT, Integer.toString( getPort() ));

        if (getUsername() != null && getUsername().length > 0)
            props.put(namePrefix + STUN_USERNAME,
                      StringUtils.getUTF8String(getUsername()));

        if (getPassword() != null && getPassword().length > 0)
            props.put(namePrefix + STUN_PASSWORD, new String(getPassword()));

        props.put(namePrefix + STUN_IS_TURN_SUPPORTED,
                        Boolean.toString( isTurnSupported() ));
    }

    /**
     * Loads this descriptor from the specified {@link Map}.The method is meant
     * for use with account property maps. It also allows prepending an account
     * prefix to all property names so that multiple descriptors can be read
     * in a single {@link Map}.
     *
     * @param props the account properties {@link Map} that we'd like to load
     * this descriptor from.
     * @param namePrefix the prefix that we should prepend to every property
     * name.
     *
     * @return the newly created descriptor or null if no descriptor was found.
     */
    public static StunServerDescriptor loadDescriptor(
                                        Map<String, String> props,
                                        String              namePrefix)
    {
        if(namePrefix == null)
            namePrefix = "";

        String stunAddress = props.get(namePrefix + STUN_ADDRESS);

        // there doesn't seem to be a stun server with the specified prefix
        if (stunAddress == null)
            return null;

        String stunPortStr = props.get(namePrefix + STUN_PORT);

        int stunPort = -1;

        try
        {
            stunPort = Integer.parseInt(stunPortStr);
        }
        catch(Throwable t)
        {
            //if the port value was wrong we just keep the default: -1
        }

        String stunUsername = props.get(namePrefix + STUN_USERNAME);
        String stunPassword = props.get(namePrefix + STUN_PASSWORD);

        boolean stunIsTurnSupported
            = Boolean.parseBoolean(
                            props.get(namePrefix + STUN_IS_TURN_SUPPORTED));

        StunServerDescriptor stunServer =
                  new StunServerDescriptor( stunAddress,
                                            stunPort,
                                            stunIsTurnSupported,
                                            stunUsername,
                                            stunPassword);

        return stunServer;
    }

    /**
     * Returns a <tt>String</tt> representation of this descriptor
     *
     * @return a <tt>String</tt> representation of this descriptor.
     */
    public String toString()
    {
        return "StunServerDesc: " + getAddress() + "/"
               + getPort()
               + " turnSupp=" + this.isTurnSupported();
    }
}
