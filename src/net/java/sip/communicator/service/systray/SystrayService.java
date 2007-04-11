/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.systray;

import net.java.sip.communicator.service.systray.event.*;

/**
 * The <tt>SystrayService</tt> manages the system tray icon, menu and messages.
 * It is meant to be used by all bundles that want to show a system tray message.
 * 
 * @author Yana Stamcheva
 */
public interface SystrayService
{
    /**
     * Message type corresponding to an error message.
     */
    public static final int ERROR_MESSAGE_TYPE = 0;
    
    /**
     * Message type corresponding to an information message.
     */
    public static final int INFORMATION_MESSAGE_TYPE = 1;
    
    /**
     * Message type corresponding to a warning message.
     */
    public static final int WARNING_MESSAGE_TYPE = 2;
    
    /**
     * Message type is not accessible.
     */
    public static final int NONE_MESSAGE_TYPE = -1;
    
    /**
     * Shows a system tray message with the given title and message content. The
     * message type will affect the icon used to present the message.
     * 
     * @param title the title, which will be shown
     * @param messageContent the content of the message to display
     * @param messageType the message type; one of XXX_MESSAGE_TYPE constants
     * declared in this class
     */
    public void showPopupMessage(String title,
        String messageContent, int messageType);
    
    /**
     * Adds a listener for <tt>SystrayPopupMessageEvent</tt>s posted when user
     * clicks on the system tray popup message.
     *
     * @param l the listener to add
     */
    public void addPopupMessageListener(SystrayPopupMessageListener listener);
    
    /**
     * Removes a listener previously added with <tt>addPopupMessageListener</tt>.
     *
     * @param l the listener to remove
     */
    public void removePopupMessageListener(SystrayPopupMessageListener listener);
}
