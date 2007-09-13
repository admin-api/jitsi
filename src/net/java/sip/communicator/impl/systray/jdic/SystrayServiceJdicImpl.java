/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package net.java.sip.communicator.impl.systray.jdic;

import java.awt.event.*;
import java.util.*;
import java.util.Timer;

import javax.swing.*;

import net.java.sip.communicator.impl.systray.*;
import net.java.sip.communicator.service.configuration.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.systray.*;
import net.java.sip.communicator.service.systray.event.*;
import net.java.sip.communicator.util.*;

import org.jdesktop.jdic.tray.*;

/**
 * The <tt>Systray</tt> provides a Icon and the associated <tt>TrayMenu</tt>
 * in the system tray using the Jdic library.
 *
 * @author Nicolas Chamouard
 * @author Yana Stamcheva
 */
public class SystrayServiceJdicImpl
    implements  SystrayService
{
    /**
     * The systray.
     */
    private SystemTray systray;

    /**
     * The icon in the system tray.
     */
    private TrayIcon trayIcon;

    /**
     * The menu that spring with a right click.
     */
    private TrayMenu menu;

    /**
     * The list of all added popup message listeners.
     */
    private Vector popupMessageListeners = new Vector();

    /**
     * List of all messages waiting to be shown.
     */
    private ArrayList messageQueue = new ArrayList();

    private Timer popupTimer = new Timer();

    /**
     * The delay between the message pop ups.
     */
    private int messageDelay = 1000;

    private int maxMessageNumber = 3;

    /**
     * The logger for this class.
     */
    private static Logger logger =
        Logger.getLogger(SystrayServiceJdicImpl.class.getName());

    private ImageIcon logoIcon;

    /**
     * Creates an instance of <tt>Systray</tt>.
     */
    public SystrayServiceJdicImpl()
    {
        try
        {
            systray = SystemTray.getDefaultSystemTray();
        }
        catch (Throwable e)
        {
            logger.error("Failed to create a systray!", e);
        }

        if(systray != null)
        {
            this.initSystray();

            SystrayActivator.getUIService().setExitOnMainWindowClose(false);
        }
    }

    /**
     * Initializes the systray icon and related listeners.
     */
    private void initSystray()
    {
        popupTimer.scheduleAtFixedRate(new ShowPopupTask(), 0, messageDelay);

        menu = new TrayMenu(this);

        String osName = System.getProperty("os.name");
        // If we're running under Windows, we use a special icon without
        // background.
        if (osName.startsWith("Windows"))
        {
           logoIcon = new ImageIcon(
                   Resources.getImage("trayIconWindows"));
        }
        else
        {
            logoIcon = new ImageIcon(
                    Resources.getImage("trayIcon"));
        }

        trayIcon = new TrayIcon(logoIcon, "SIP Communicator", menu);
        trayIcon.setIconAutoSize(true);

        //Show/hide the contact list when user clicks on the systray.
        trayIcon.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                UIService uiService = SystrayActivator.getUIService();

                boolean isVisible;

                isVisible = ! uiService.isVisible();

                uiService.setVisible(isVisible);

                ConfigurationService configService
                    = SystrayActivator.getConfigurationService();

                configService.setProperty(
                        "net.java.sip.communicator.impl.systray.showApplication",
                        new Boolean(isVisible));
            }
        });

        //Notify all interested listener that user has clicked on the systray
        //popup message.
        trayIcon.addBalloonActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                UIService uiService = SystrayActivator.getUIService();

                firePopupMessageEvent(e.getSource());

                ExportedWindow chatWindow
                    = uiService.getExportedWindow(ExportedWindow.CHAT_WINDOW);

                if(chatWindow != null && chatWindow.isVisible())
                {
                    chatWindow.bringToFront();
                }
            }
        });

        systray.addTrayIcon(trayIcon);
    }

    /**
     * Saves the last status for all accounts. This information is used
     * on logging. Each time user logs in he's logged with the same status
     * as he was the last time before closing the application.
     * 
     * @param protocolProvider  the protocol provider for which we save the 
     * last selected status
     * @param statusName the status name to save
     */
    public void saveStatusInformation(
            ProtocolProviderService protocolProvider,
            String statusName)
    {
        ConfigurationService configService
            = SystrayActivator.getConfigurationService();

        if(configService != null)
        {
            String prefix = "net.java.sip.communicator.impl.gui.accounts";

            List accounts = configService
                    .getPropertyNamesByPrefix(prefix, true);

            boolean savedAccount = false;
            Iterator accountsIter = accounts.iterator();

            while(accountsIter.hasNext()) {
                String accountRootPropName
                    = (String) accountsIter.next();

                String accountUID
                    = configService.getString(accountRootPropName);

                if(accountUID.equals(protocolProvider
                        .getAccountID().getAccountUniqueID())) {

                    configService.setProperty(
                            accountRootPropName + ".lastAccountStatus",
                            statusName);

                    savedAccount = true;
                }
            }

            if(!savedAccount) {
                String accNodeName
                    = "acc" + Long.toString(System.currentTimeMillis());

                String accountPackage
                    = "net.java.sip.communicator.impl.gui.accounts."
                            + accNodeName;

                configService.setProperty(accountPackage,
                        protocolProvider.getAccountID().getAccountUniqueID());

                configService.setProperty(
                        accountPackage+".lastAccountStatus",
                        statusName);
            }
        }
    }

    /**
     * Implements the <tt>SystratService.showPopupMessage</tt> method. Shows
     * a pop up message, above the Systray icon, which has the given title,
     * message content and message type.
     * 
     * @param title the title of the message
     * @param messageContent the content text
     * @param messageType the type of the message 
     */
    public void showPopupMessage(   String title,
                                    String messageContent,
                                    int messageType)
    {
        int trayMsgType = TrayIcon.NONE_MESSAGE_TYPE;

        if (messageType == SystrayService.ERROR_MESSAGE_TYPE)
            trayMsgType = TrayIcon.ERROR_MESSAGE_TYPE;
        else if (messageType == SystrayService.INFORMATION_MESSAGE_TYPE)
            trayMsgType = TrayIcon.INFO_MESSAGE_TYPE;
        else if (messageType == SystrayService.WARNING_MESSAGE_TYPE)
            trayMsgType = TrayIcon.WARNING_MESSAGE_TYPE;

        if(messageContent.length() > 100)
            messageContent = messageContent.substring(0, 100).concat("...");

        messageQueue.add(new SystrayMessage(title, messageContent, trayMsgType));
    }

    /**
     * Implements the <tt>SystrayService.addPopupMessageListener</tt> method.
     * 
     * @param listener the listener to add
     */
    public void addPopupMessageListener(SystrayPopupMessageListener listener)
    {
        synchronized (popupMessageListeners)
        {
            this.popupMessageListeners.add(listener);
        }
    }

    /**
     * Implements the <tt>SystrayService.removePopupMessageListener</tt> method.
     * 
     * @param listener the listener to remove
     */
    public void removePopupMessageListener(SystrayPopupMessageListener listener)
    {
        synchronized (popupMessageListeners)
        {
            this.popupMessageListeners.remove(listener);
        }
    }

    /**
     * Notifies all interested listeners that a <tt>SystrayPopupMessageEvent</tt>
     * has occured.
     * 
     * @param sourceObject the source of this event
     */
    private void firePopupMessageEvent(Object sourceObject)
    {
        SystrayPopupMessageEvent evt
            = new SystrayPopupMessageEvent(sourceObject);

        logger.trace("Will dispatch the following systray msg event: " + evt);

        Iterator listeners = null;
        synchronized (popupMessageListeners)
        {
            listeners = new ArrayList(popupMessageListeners).iterator();
        }

        while (listeners.hasNext())
        {
            SystrayPopupMessageListener listener
                = (SystrayPopupMessageListener) listeners.next();

            listener.popupMessageClicked(evt);
        }
    }

    /**
     * Sets a new Systray icon.
     * 
     * @param image the icon to set.
     */
    public void setSystrayIcon(byte[] image)
    {
        this.trayIcon.setIcon(new ImageIcon(image));
    }

    /**
     * Shows the oldest message in the message queue and then removes it from
     * the queue.
     */
    private class ShowPopupTask extends TimerTask
    {
        public void run()
        {
            if(messageQueue.isEmpty())
                return;

            int messageNumber = messageQueue.size();

            SystrayMessage msg = (SystrayMessage) messageQueue.get(0);

            if(messageNumber > maxMessageNumber)
            {
                messageQueue.clear();

                String messageContent = msg.getMessageContent();

                if(!messageContent.endsWith("..."))
                    messageContent.concat("...");

                messageQueue.add(new SystrayMessage(
                    "You have received " + messageNumber
                    + " new messages. For more info check your chat window.",
                    "Messages starts by: " + messageContent,
                    TrayIcon.INFO_MESSAGE_TYPE));
            }
            else
            {
                trayIcon.displayMessage(msg.getTitle(),
                                    msg.getMessageContent(),
                                    msg.getMessageType());

                messageQueue.remove(0);
            }
        }
    }

    private class SystrayMessage
    {
        private String title;
        private String messageContent;
        private int messageType;

        /**
         * Creates an instance of <tt>SystrayMessage</tt> by specifying the
         * message <tt>title</tt>, the content of the message and the type of
         * the message.
         * 
         * @param title the title of the message
         * @param messageContent the content of the message
         * @param messageType the type of the message
         */
        public SystrayMessage(  String title,
                                String messageContent,
                                int messageType)
        {
            this.title = title;
            this.messageContent = messageContent;
            this.messageType = messageType;
        }

        /**
         * Returns the title of the message.
         * 
         * @return the title of the message
         */
        public String getTitle()
        {
            return title;
        }

        /**
         * Returns the message content.
         * 
         * @return the message content
         */
        public String getMessageContent()
        {
            return messageContent;
        }

        /**
         * Returns the message type.
         * 
         * @return the message type
         */
        public int getMessageType()
        {
            return messageType;
        }
    }
}
