/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package net.java.sip.communicator.impl.systray.jdic;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import java.util.*;
import java.util.List;

import javax.swing.*;

import net.java.sip.communicator.impl.systray.*;
import net.java.sip.communicator.service.configuration.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.gui.event.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.service.systray.*;
import net.java.sip.communicator.service.systray.event.*;
import net.java.sip.communicator.util.*;

import org.jdesktop.jdic.tray.*;
import org.osgi.framework.*;

/**
 * The <tt>Systray</tt> provides a Icon and the associated <tt>TrayMenu</tt> 
 * in the system tray using the Jdic library.
 *  
 * @author Nicolas Chamouard
 * @author Yana Stamcheva
 */
public class SystrayServiceJdicImpl
    implements SystrayService,
               ServiceListener,
               MessageListener,
               ChatFocusListener
{   
    /**
     * A reference of the <tt>UIservice</tt>.
     */
    private UIService uiService;
    
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
     * The list of all providers.
     */
    private Map protocolProviderTable = new LinkedHashMap();
    
    /**
     * The list of all added popup message listeners.
     */
    private Vector popupMessageListeners = new Vector();
    
    /**
     * The logger for this class.
     */
    private static Logger logger =
        Logger.getLogger(SystrayServiceJdicImpl.class.getName());
    
    private ImageIcon logoIcon;
    
    /**
     * Creates an instance of <tt>Systray</tt>.
     * @param service a reference of the current <tt>UIservice</tt>
     */
    public SystrayServiceJdicImpl(UIService service)
    {
        this.uiService = service;
        
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
            this.initProvidersTable();
            
            uiService.setExitOnMainWindowClose(false);
            
            SystrayActivator.bundleContext.addServiceListener(this);
        }
    }
    
    /**
     * Initializes the systray icon and related listeners.
     */
    private void initSystray()
    {   
        menu = new TrayMenu(uiService,this);
        
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
                if(uiService.isVisible())
                {
                    uiService.setVisible(false);
                }
                else
                {
                    uiService.setVisible(true);
                }
            }
        });
        
        //Notify all interested listener that user has clicked on the systray
        //popup message.
        trayIcon.addBalloonActionListener(new ActionListener() 
        {
            public void actionPerformed(ActionEvent e) 
            {
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
     * We fill the protocolProviderTable with all
     * running protocol providers at the start of
     * the bundle.
     */
    private void initProvidersTable()
    {
        BundleContext bc = SystrayActivator.bundleContext;
        
        bc.addServiceListener(this);
        ServiceReference[] protocolProviderRefs = null;
        try
        {
            protocolProviderRefs = bc.getServiceReferences(
                ProtocolProviderService.class.getName(),null);
        }
        catch (InvalidSyntaxException ex)
        {
            // this shouldn't happen since we're providing no parameter string
            // but let's log just in case.
            logger.error("Error while retrieving service refs", ex);
            return;
        }

        // in case we found any
        if (protocolProviderRefs != null)
        {
            
            for (int i = 0; i < protocolProviderRefs.length; i++)
            {
                ProtocolProviderService provider = (ProtocolProviderService) bc
                    .getService(protocolProviderRefs[i]);
                
                this.protocolProviderTable.put(
                    provider.getAccountID(),
                    provider);
                
                handleProviderAdded(provider);
            }
        }
    }
    
    /**
     * Returns a set of all protocol providers.
     *
     * @return a set of all protocol providers.
     */
    public Iterator getProtocolProviders()
    {    
        return this.protocolProviderTable.values().iterator();   
    }
    
    /**
     * Currently unused
     * @param evt ignored
     */
    public void messageDelivered(MessageDeliveredEvent evt)
    {
    }
    
    /**
     * Currently unused
     * @param evt ignored
     */
    public void messageDeliveryFailed(MessageDeliveryFailedEvent evt)
    {
    }
    
    /**
     * Display in a balloon the newly received message
     * @param evt the event containing the message
     */
    public void messageReceived(MessageReceivedEvent evt)
    {
        Chat chat = uiService.getChat(evt.getSourceContact());
        
        if(!chat.isChatFocused())
        {                        
            String title = Resources.getString("messageReceived") + " "
                + evt.getSourceContact().getDisplayName();
            
            String message = evt.getSourceMessage().getContent();
            
            if(message.length() > 100)
                message = message.substring(0, 100).concat("...");
            
            GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
            GraphicsDevice gs = ge.getDefaultScreenDevice();
            GraphicsConfiguration gc = gs.getDefaultConfiguration();
            
            // Create an image that does not support transparency
            BufferedImage img = gc.createCompatibleImage(logoIcon.getIconWidth(),
                   logoIcon.getIconHeight(), Transparency.TRANSLUCENT);
            
            Image msgImg = new ImageIcon(
                    Resources.getImage("messageIcon")).getImage();
            
            Graphics2D g = (Graphics2D) img.getGraphics();
            
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            g.drawImage(logoIcon.getImage(), 0, 0, null);
            g.drawImage(msgImg,
                    logoIcon.getIconWidth()/2 - msgImg.getWidth(null)/2,
                    logoIcon.getIconHeight()/2 - msgImg.getHeight(null)/2, null);
            
            this.trayIcon.setIcon(new ImageIcon(img));
            
            this.trayIcon.displayMessage(
                title, message, TrayIcon.NONE_MESSAGE_TYPE);
            
            chat.addChatFocusListener(this);
        }
    }
    
    /**
     * When a service ist registered or unregistered, we update
     * the provider tables and add/remove listeners (if it supports
     * BasicInstantMessenging implementation)
     *
     * @param event ServiceEvent
     */
    public void serviceChanged(ServiceEvent event)
    {        
        ProtocolProviderService provider = (ProtocolProviderService)
            SystrayActivator.bundleContext.getService(event.getServiceReference());
        
        if (event.getType() == ServiceEvent.REGISTERED){
            protocolProviderTable.put(provider.getAccountID(),provider);
            handleProviderAdded(provider);
            
        }
        if (event.getType() == ServiceEvent.UNREGISTERING){
           protocolProviderTable.remove(provider.getAccountID());
           handleProviderRemoved(provider);
        }

    }
    
    /**
     * Checks if the provider has an implementation 
     * of OperationSetBasicInstantMessaging and
     * if so add a listerner to it
     *
     * @param provider ProtocolProviderService
     */
    private void handleProviderAdded(ProtocolProviderService provider)
    {
        OperationSetBasicInstantMessaging opSetIm
        = (OperationSetBasicInstantMessaging) provider
        .getSupportedOperationSets().get(
            OperationSetBasicInstantMessaging.class.getName());
        
        if(opSetIm != null)
            opSetIm.addMessageListener(this);    
        
    }
    
    /**
     * Checks if the provider has an implementation 
     * of OperationSetBasicInstantMessaging and
     * if so remove its listerner
     *
     * @param provider ProtocolProviderService
     */
    private void handleProviderRemoved(ProtocolProviderService provider)
    {
        OperationSetBasicInstantMessaging opSetIm
        = (OperationSetBasicInstantMessaging) provider
        .getSupportedOperationSets().get(
            OperationSetBasicInstantMessaging.class.getName());
        
        if(opSetIm != null)
            opSetIm.removeMessageListener(this);
        
    }

    /**
     * Saves the last status for all accounts. This information is used
     * on loging. Each time user logs in he's logged with the same status
     * as he was the last time before closing the application.
     */
    public void saveStatusInformation(ProtocolProviderService protocolProvider,
            String statusName)
    {
        ServiceReference configReference = SystrayActivator.bundleContext
            .getServiceReference(ConfigurationService.class.getName());
    
        ConfigurationService configService
            = (ConfigurationService) SystrayActivator.bundleContext
                .getService(configReference);
    
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

    public UIService getUiService()
    {
        return uiService;
    }

    /**
     * Implements the <tt>SystratService.showPopupMessage</tt> method. Shows
     * a popup message, above the systray icon, which has the given title,
     * message content and message type.
     */
    public void showPopupMessage(String title, String messageContent,
        int messageType)
    {
        int trayMsgType = TrayIcon.NONE_MESSAGE_TYPE;
        
        if (messageType == SystrayService.ERROR_MESSAGE_TYPE)
            trayMsgType = TrayIcon.ERROR_MESSAGE_TYPE;
        else if (messageType == SystrayService.INFORMATION_MESSAGE_TYPE)
            trayMsgType = TrayIcon.INFO_MESSAGE_TYPE;
        else if (messageType == SystrayService.WARNING_MESSAGE_TYPE)
            trayMsgType = TrayIcon.WARNING_MESSAGE_TYPE;
        
        this.trayIcon.displayMessage(
            title, messageContent, trayMsgType);
    }

    /**
     * Implements the <tt>SystrayService.addPopupMessageListener</tt> method.
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
     */
    public void removePopupMessageListener(SystrayPopupMessageListener listener)
    {
        synchronized (popupMessageListeners)
        {
            this.popupMessageListeners.remove(listener);
        }
    }
    
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

    public void chatFocusGained(ChatFocusEvent event)
    {
        Chat chat = event.getChat();
        
        chat.removeChatFocusListener(this);
        
        this.trayIcon.setIcon(logoIcon);
    }

    public void chatFocusLost(ChatFocusEvent event)
    {   
    }
}
