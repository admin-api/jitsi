/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package net.java.sip.communicator.impl.gui.main.contactlist;

import java.awt.*;
import java.awt.event.*;
import java.util.*;

import javax.swing.*;
import javax.swing.Timer;

import net.java.sip.communicator.impl.gui.i18n.*;
import net.java.sip.communicator.impl.gui.main.*;
import net.java.sip.communicator.impl.gui.main.chat.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.*;

/**
 * The contactlist panel not only contains the contact list but it has the role
 * of a message dispatcher. It process all sent and received messages as well as
 * all typing notifications. Here are managed all contact list mouse events.
 * 
 * @author Yana Stamcheva
 */
public class ContactListPanel
    extends JScrollPane
    implements  MessageListener,
                TypingNotificationsListener,
                ContactListListener
{

    private MainFrame mainFrame;

    private ContactList contactList;

    private JPanel treePanel = new JPanel(new BorderLayout());

    private TypingTimer typingTimer = new TypingTimer();

    private CommonRightButtonMenu commonRightButtonMenu;
    
    private Logger logger = Logger.getLogger(ContactListPanel.class);
    
    private ChatWindowManager chatWindowManager;

    /**
     * Creates the contactlist scroll panel defining the parent frame.
     * 
     * @param mainFrame The parent frame.
     */
    public ContactListPanel(MainFrame mainFrame) {

        this.mainFrame = mainFrame;

        this.chatWindowManager = mainFrame.getChatWindowManager();
        
        this.getViewport().add(treePanel);

        this.setHorizontalScrollBarPolicy(
            JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        this.getVerticalScrollBar().setUnitIncrement(30);
    }

    /**
     * Initializes the contact list.
     * 
     * @param contactListService The MetaContactListService which will be used
     *            for a contact list data model.
     */
    public void initList(MetaContactListService contactListService)
    {
        this.contactList = new ContactList(mainFrame);

        this.contactList.addContactListListener(this);
        this.treePanel.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e)
            {

                if ((e.getModifiers() & InputEvent.BUTTON3_MASK) != 0) {
                    commonRightButtonMenu = new CommonRightButtonMenu(mainFrame);

                    commonRightButtonMenu.setInvoker(treePanel);

                    commonRightButtonMenu.setLocation(e.getX()
                            + mainFrame.getX() + 5, e.getY() + mainFrame.getY()
                            + 105);

                    commonRightButtonMenu.setVisible(true);
                }
            }
        });

        this.treePanel.add(contactList, BorderLayout.NORTH);

        this.treePanel.setBackground(Color.WHITE);
        this.contactList.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));        
    }

    /**
     * Returns the contact list.
     * 
     * @return the contact list
     */
    public ContactList getContactList()
    {
        return this.contactList;
    }

    /**
     * Implements the ContactListListener.contactSelected method.
     */
    public void contactClicked(ContactListEvent evt)
    {
        if (evt.getClickCount() > 1)
        {
            SwingUtilities
                .invokeLater(new RunMessageWindow(evt.getSourceContact()));
        }
    }
    
    /**
     * Implements the ContactListListener.groupSelected method.
     */
    public void groupSelected(ContactListEvent evt)
    {}

    /**
     * Implements the ContactListListener.protocolContactSelected method.
     */
    public void protocolContactClicked(ContactListEvent evt)
    {
        SwingUtilities.invokeLater(new RunMessageWindow(evt.getSourceContact(),
                evt.getSourceProtoContact()));
    }

    /**
     * Runs the chat window for the specified contact. We examine different
     * cases here, depending on the chat window mode.
     * 
     * In mode "Open messages in new window" a new window is opened for the
     * given <tt>MetaContact</tt> if there's no opened window for it,
     * otherwise the existing chat window is made visible and focused.
     * 
     * In mode "Group messages in one chat window" a JTabbedPane is used to show
     * chats for different contacts in ona window. A new tab is opened for the
     * given <tt>MetaContact</tt> if there's no opened tab for it, otherwise
     * the existing chat tab is selected and focused.
     * 
     * @author Yana Stamcheva
     */
    public class RunMessageWindow implements Runnable
    {
        private MetaContact metaContact;

        private Contact protocolContact;

        public RunMessageWindow(MetaContact metaContact)
        {
            this.metaContact = metaContact;
        }

        public RunMessageWindow(MetaContact metaContact, 
            Contact protocolContact)
        {
            this.metaContact = metaContact;
            this.protocolContact = protocolContact;
        }

        public void run()
        {
            ChatPanel chatPanel;
            
            if(protocolContact != null)
                chatPanel = chatWindowManager
                    .getContactChat(metaContact, protocolContact);
            else
                chatPanel = chatWindowManager.getContactChat(metaContact);
            
            chatWindowManager.openChat(chatPanel, true);
        }
    }
    

    /**
     * When a message is received determines whether to open a new chat window
     * or chat window tab, or to indicate that a message is received from a
     * contact which already has an open chat. When the chat is found checks if
     * in mode "Auto popup enabled" and if this is the case shows the message in
     * the appropriate chat panel.
     * 
     * @param evt the event containing details on the received message
     */
    public void messageReceived(MessageReceivedEvent evt)
    {
        logger.trace("MESSAGE RECEIVED from contact: "
            + evt.getSourceContact().getAddress());
        
        Contact protocolContact = evt.getSourceContact();
        Date date = evt.getTimestamp();
        Message message = evt.getSourceMessage();
        int eventType = evt.getEventType();

        MetaContact metaContact = mainFrame.getContactList()
                .findMetaContactByContact(protocolContact);
        
        if(metaContact != null)
        {            
            // Show an envelope on the sender contact in the contact list.
            ContactListModel clistModel
                = (ContactListModel) contactList.getModel();
            
            clistModel.addActiveContact(metaContact);
            contactList.refreshContact(metaContact);
            
            // Obtain the corresponding chat panel.
            ChatPanel chatPanel = chatWindowManager.getContactChat(
                metaContact, protocolContact, message.getMessageUID());
            
            // Distinguish the message type, depending on the type of event that
            // we have received.
            String messageType = null;

            if(eventType == MessageReceivedEvent.CONVERSATION_MESSAGE_RECEIVED)
            {
                messageType = Constants.INCOMING_MESSAGE;                
            }
            else if(eventType == MessageReceivedEvent.SYSTEM_MESSAGE_RECEIVED)
            {
                messageType = Constants.SYSTEM_MESSAGE;
            }

            chatPanel.processMessage(protocolContact.getDisplayName(), date,
                messageType, message.getContent(),
                message.getContentType());

            // Opens the chat panel with the new message.
            chatWindowManager.openChat(chatPanel, false);
         
            // Fire notification
            String title = Messages.getI18NString("messageReceived").getText()
                + " " + evt.getSourceContact().getDisplayName();
            
            NotificationManager.fireNotification(
                                            NotificationManager.INCOMING_MESSAGE,
                                            title,
                                            message.getContent());
            
            chatPanel.treatReceivedMessage(protocolContact);
        }
        else
        {
            logger.trace("MetaContact not found for protocol contact: "
                    + protocolContact + ".");
        }
    }

    /**
     * When a sent message is delivered shows it in the chat conversation panel.
     * 
     * @param evt the event containing details on the message delivery
     */
    public void messageDelivered(MessageDeliveredEvent evt)
    {
        Contact contact = evt.getDestinationContact();
        
        MetaContact metaContact = mainFrame.getContactList()
            .findMetaContactByContact(contact);
                
        logger.trace("MESSAGE DELIVERED to contact: "
            + evt.getDestinationContact().getAddress());
        
        Message msg = evt.getSourceMessage();        
        
        ChatWindowManager chatWindowManager = mainFrame.getChatWindowManager();
        ChatPanel chatPanel = null;
        
        if(chatWindowManager.isChatOpenedForContact(metaContact))
            chatPanel = chatWindowManager.getContactChat(metaContact);
            
        if (chatPanel != null)
        {   
            ProtocolProviderService protocolProvider = evt
                    .getDestinationContact().getProtocolProvider();

            logger.trace("MESSAGE DELIVERED: process message to chat for contact: "
                    + evt.getDestinationContact().getAddress());
            
            chatPanel.processMessage(this.mainFrame
                    .getAccount(protocolProvider), evt.getTimestamp(),
                    Constants.OUTGOING_MESSAGE, msg.getContent(),
                    msg.getContentType());
        }
    }

    /**
     * Shows a warning message to the user when message delivery has failed.
     * 
     * @param evt the event containing details on the message delivery failure
     */
    public void messageDeliveryFailed(MessageDeliveryFailedEvent evt)
    {
        String errorMsg = null;
     
        Message sourceMessage = (Message) evt.getSource();
        
        Contact sourceContact = evt.getDestinationContact();
        
        MetaContact metaContact = mainFrame.getContactList()
            .findMetaContactByContact(sourceContact);
        
        if (evt.getErrorCode() 
                == MessageDeliveryFailedEvent.OFFLINE_MESSAGES_NOT_SUPPORTED) {

            errorMsg = Messages.getI18NString(
                    "msgDeliveryOfflineNotSupported").getText();
        }
        else if (evt.getErrorCode()
                == MessageDeliveryFailedEvent.NETWORK_FAILURE) {
            
            errorMsg = Messages.getI18NString("msgNotDelivered").getText();
        }
        else if (evt.getErrorCode()
                == MessageDeliveryFailedEvent.PROVIDER_NOT_REGISTERED) {

            errorMsg = Messages.getI18NString(
                    "msgSendConnectionProblem").getText();
        }
        else if (evt.getErrorCode()
                == MessageDeliveryFailedEvent.INTERNAL_ERROR) {
            
            errorMsg = Messages.getI18NString(
                    "msgDeliveryInternalError").getText();
        }
        else {
            errorMsg = Messages.getI18NString(
                    "msgDeliveryFailedUnknownError").getText();
        }
                       
        ChatPanel chatPanel = chatWindowManager
            .getContactChat(metaContact, sourceContact);
        
        chatPanel.processMessage(
                metaContact.getDisplayName(),
                new Date(System.currentTimeMillis()),
                Constants.OUTGOING_MESSAGE,
                sourceMessage.getContent(),
                sourceMessage.getContentType());
        
        chatPanel.processMessage(
                metaContact.getDisplayName(),
                new Date(System.currentTimeMillis()),
                Constants.ERROR_MESSAGE,
                errorMsg,
                "text");
        
        chatWindowManager.openChat(chatPanel, false);
    }

    /**
     * Informs the user what is the typing state of his chat contacts.
     * 
     * @param evt the event containing details on the typing notification
     */
    public void typingNotificationReceifed(TypingNotificationEvent evt)
    {
        if (typingTimer.isRunning())
            typingTimer.stop();

        String notificationMsg = "";

        String contactName = this.mainFrame.getContactList()
                .findMetaContactByContact(evt.getSourceContact())
                .getDisplayName()
                + " ";

        if (contactName.equals("")) {
            contactName = Messages.getI18NString("unknown").getText() + " ";
        }

        int typingState = evt.getTypingState();
        MetaContact metaContact = mainFrame.getContactList()
                .findMetaContactByContact(evt.getSourceContact());

        if (typingState == OperationSetTypingNotifications.STATE_TYPING)
        {
            notificationMsg
                = Messages.getI18NString("contactTyping",
                    new String[]{contactName}).getText();
            
            typingTimer.setMetaContact(metaContact);
            typingTimer.start();
        }
        else if (typingState == OperationSetTypingNotifications.STATE_PAUSED)
        {
            notificationMsg = Messages.getI18NString("contactPausedTyping",
                new String[]{contactName}).getText();
            typingTimer.setMetaContact(metaContact);
            typingTimer.start();
        }
        else if (typingState == OperationSetTypingNotifications.STATE_STOPPED)
        {
            notificationMsg = "";
        }
        else if (typingState == OperationSetTypingNotifications.STATE_STALE)
        {
            notificationMsg
                = Messages.getI18NString("contactTypingStateStale").getText();
        }
        else if (typingState == OperationSetTypingNotifications.STATE_UNKNOWN)
        {
            // TODO: Implement state unknown
        }
        this.setChatNotificationMsg(metaContact, notificationMsg);
    }

    /**
     * Sets the typing notification message at the appropriate chat.
     * 
     * @param metaContact The meta contact.
     * @param notificationMsg The typing notification message.
     */
    public void setChatNotificationMsg(MetaContact metaContact,
            String notificationMsg)
    { 
        if(chatWindowManager.isChatOpenedForContact(metaContact))
            chatWindowManager.getContactChat(metaContact)
                .setStatusMessage(notificationMsg);                
    }
    
    /**
     * Returns the right button menu of the contact list.
     * @return the right button menu of the contact list
     */
    public CommonRightButtonMenu getCommonRightButtonMenu()
    {
        return commonRightButtonMenu;
    }
    
    /**
     * The TypingTimer is started after a PAUSED typing notification is
     * received. It waits 5 seconds and if no other typing event occurs removes
     * the PAUSED message from the chat status panel.
     */
    private class TypingTimer extends Timer
    {

        private MetaContact metaContact;

        public TypingTimer() {
            // Set delay
            super(5 * 1000, null);

            this.addActionListener(new TimerActionListener());
        }

        private class TimerActionListener implements ActionListener
        {
            public void actionPerformed(ActionEvent e)
            {
                setChatNotificationMsg(metaContact, "");
            }
        }

        private void setMetaContact(MetaContact metaContact)
        {
            this.metaContact = metaContact;
        }
    }   
}
