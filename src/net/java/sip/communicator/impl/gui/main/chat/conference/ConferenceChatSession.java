/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.chat.conference;

import java.util.*;

import javax.swing.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.main.chat.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.service.msghistory.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;

/**
 * An implementation of <tt>ChatSession</tt> for conference chatting.
 * 
 * @author Yana Stamcheva
 */
public class ConferenceChatSession
    implements  ChatSession,
                ChatRoomMemberPresenceListener,
                ChatRoomPropertyChangeListener
{
    private final ArrayList chatParticipants = new ArrayList();

    private final ArrayList chatTransports = new ArrayList();

    private ChatTransport currentChatTransport;

    private final ChatRoomWrapper chatRoomWrapper;

    private final ChatSessionRenderer sessionRenderer;

    /**
     * Creates an instance of <tt>ConferenceChatSession</tt>, by specifying the
     * sessionRenderer to be used for communication with the UI and the chatRoom
     * corresponding to this conference session.
     * 
     * @param sessionRenderer the renderer to be used for communication with the
     * UI.
     * @param chatRoomWrapper the chat room corresponding to this conference
     * session.
     */
    public ConferenceChatSession(   ChatSessionRenderer sessionRenderer,
                                    ChatRoomWrapper chatRoomWrapper)
    {
        this.sessionRenderer = sessionRenderer;
        this.chatRoomWrapper = chatRoomWrapper;

        currentChatTransport
            = new ConferenceChatTransport(this, chatRoomWrapper.getChatRoom());

        chatTransports.add(currentChatTransport);

        this.initChatParticipants();

        chatRoomWrapper.getChatRoom().addMemberPresenceListener(this);
        chatRoomWrapper.getChatRoom().addPropertyChangeListener(this);
    }

    /**
     * Returns the descriptor of this chat session.
     * 
     * @return the descriptor of this chat session.
     */
    public Object getDescriptor()
    {
        return chatRoomWrapper;
    }

    /**
     * Disposes this chat session.
     */
    public void dispose()
    {
        chatRoomWrapper.getChatRoom().removeMemberPresenceListener(this);
        chatRoomWrapper.getChatRoom().removePropertyChangeListener(this);
    }

    /**
     * Returns the name of the chat room.
     * 
     * @return the name of the chat room.
     */
    public String getChatName()
    {
        return chatRoomWrapper.getChatRoomName();
    }

    /**
     * Returns the subject of the chat room.
     * 
     * @return the subject of the chat room.
     */
    public String getChatSubject()
    {
        return chatRoomWrapper.getChatRoom().getSubject();
    }

    /**
     * Returns the configuration form corresponding to the chat room.
     * 
     * @return the configuration form corresponding to the chat room.
     * @throws OperationFailedException if no configuration form is available
     * for the chat room.
     */
    public ChatRoomConfigurationForm getChatConfigurationForm()
        throws OperationFailedException
    {
        return chatRoomWrapper.getChatRoom().getConfigurationForm();
    }

    /**
     * Returns an iterator to the list of all participants contained in this 
     * chat session.
     * 
     * @return an iterator to the list of all participants contained in this 
     * chat session.
     */
    public Iterator<ChatContact> getParticipants()
    {
        return chatParticipants.iterator();
    }

    /**
     * Returns all available chat transports for this chat session.
     * 
     * @return all available chat transports for this chat session.
     */
    public Iterator<ChatTransport> getChatTransports()
    {
        return chatTransports.iterator();
    }

    /**
     * Returns the currently used transport for all operation within this chat
     * session.
     * 
     * @return the currently used transport for all operation within this chat
     * session.
     */
    public ChatTransport getCurrentChatTransport()
    {
        return currentChatTransport;
    }

    /**
     * Returns the default mobile number used to send sms-es in this session. In
     * the case of conference this is for now null.
     * 
     * @return the default mobile number used to send sms-es in this session.
     */
    public String getDefaultSmsNumber()
    {
        return null;
    }

    /**
     * Returns a collection of the last N number of messages given by count.
     * 
     * @param count The number of messages from history to return.
     * @return a collection of the last N number of messages given by count.
     */
    public Collection getHistory(int count)
    {
        final MessageHistoryService msgHistory
            = GuiActivator.getMsgHistoryService();

        // If the MessageHistoryService is not registered we have nothing to do
        // here. The MessageHistoryService could be "disabled" from the user
        // through one of the configuration forms.
        if (msgHistory == null)
            return null;

        return msgHistory.findLast(
            chatRoomWrapper.getChatRoom(),
            ConfigurationManager.getChatHistorySize());
    }

    /**
     * Returns a collection of the last N number of messages given by count.
     * 
     * @param date The date up to which we're looking for messages.
     * @param count The number of messages from history to return.
     * @return a collection of the last N number of messages given by count.
     */
    public Collection getHistoryBeforeDate(Date date, int count)
    {
        final MessageHistoryService msgHistory
            = GuiActivator.getMsgHistoryService();

        // If the MessageHistoryService is not registered we have nothing to do
        // here. The MessageHistoryService could be "disabled" from the user
        // through one of the configuration forms.
        if (msgHistory == null)
            return null;

        return msgHistory.findLastMessagesBefore(
            chatRoomWrapper.getChatRoom(),
            date,
            ConfigurationManager.getChatHistorySize());
    }

    /**
     * Returns a collection of the last N number of messages given by count.
     * 
     * @param date The date from which we're looking for messages.
     * @param count The number of messages from history to return.
     * @return a collection of the last N number of messages given by count.
     */
    public Collection getHistoryAfterDate(Date date, int count)
    {
        final MessageHistoryService msgHistory
            = GuiActivator.getMsgHistoryService();

        // If the MessageHistoryService is not registered we have nothing to do
        // here. The MessageHistoryService could be "disabled" from the user
        // through one of the configuration forms.
        if (msgHistory == null)
            return null;

        return msgHistory.findFirstMessagesAfter(
            chatRoomWrapper.getChatRoom(),
            date,
            ConfigurationManager.getChatHistorySize());
    }

    /**
     * Returns the start date of the history of this chat session.
     * 
     * @return the start date of the history of this chat session.
     */
    public Date getHistoryStartDate()
    {
        Date startHistoryDate = null;

        MessageHistoryService msgHistory
            = GuiActivator.getMsgHistoryService();

        // If the MessageHistoryService is not registered we have nothing to do
        // here. The MessageHistoryService could be "disabled" from the user
        // through one of the configuration forms.
        if (msgHistory == null)
            return null;

        Collection firstMessage = msgHistory
            .findFirstMessagesAfter(chatRoomWrapper.getChatRoom(),
                                    new Date(0),
                                    1);

        if(firstMessage.size() > 0)
        {
            Iterator i = firstMessage.iterator();

            Object o = i.next();

            if(o instanceof MessageDeliveredEvent)
            {
                MessageDeliveredEvent evt
                    = (MessageDeliveredEvent)o;

                startHistoryDate = evt.getTimestamp();
            }
            else if(o instanceof MessageReceivedEvent)
            {
                MessageReceivedEvent evt = (MessageReceivedEvent)o;

                startHistoryDate = evt.getTimestamp();
            }
        }

        return startHistoryDate;
    }

    /**
     * Returns the end date of the history of this chat session.
     * 
     * @return the end date of the history of this chat session.
     */
    public Date getHistoryEndDate()
    {
        Date endHistoryDate = null;

        MessageHistoryService msgHistory
            = GuiActivator.getMsgHistoryService();

        // If the MessageHistoryService is not registered we have nothing to do
        // here. The MessageHistoryService could be "disabled" from the user
        // through one of the configuration forms.
        if (msgHistory == null)
            return null;

        Collection lastMessage = msgHistory
            .findLastMessagesBefore(chatRoomWrapper.getChatRoom(),
                                    new Date(Long.MAX_VALUE), 1);

        if(lastMessage.size() > 0)
        {
            Iterator i1 = lastMessage.iterator();

            Object o1 = i1.next();

            if(o1 instanceof MessageDeliveredEvent)
            {
                MessageDeliveredEvent evt
                    = (MessageDeliveredEvent)o1;

                endHistoryDate = evt.getTimestamp();
            }
            else if(o1 instanceof MessageReceivedEvent)
            {
                MessageReceivedEvent evt = (MessageReceivedEvent)o1;

                endHistoryDate = evt.getTimestamp();
            }
        }

        return endHistoryDate;
    }

    /**
     * Sets the transport that will be used for all operations within this chat
     * session.
     * 
     * @param chatTransport The transport to set as a default transport for this
     * session.
     */
    public void setCurrentChatTransport(ChatTransport chatTransport)
    {
        this.currentChatTransport = chatTransport;
    }

    /**
     * Sets the default mobile number used to send sms-es in this session.
     * 
     * @param smsPhoneNumber The default mobile number used to send sms-es in
     * this session.
     */
    public void setDefaultSmsNumber(String smsPhoneNumber)
    {}

    /**
     * Returns the <tt>ChatSessionRenderer</tt> that provides the connection
     * between this chat session and its UI.
     * 
     * @return The <tt>ChatSessionRenderer</tt>.
     */
    public ChatSessionRenderer getChatSessionRenderer()
    {
        return sessionRenderer;
    }

    /**
     * Invoked when <tt>ChatRoomMemberPresenceChangeEvent</tt> are received.
     * When a new <tt>ChatRoomMember</tt> has joined the chat adds it to the
     * list of chat participants on the right of the chat window. When a
     * <tt>ChatRoomMember</tt> has left or quit, or has being kicked it's
     * removed from the chat window.
     */
    public void memberPresenceChanged(ChatRoomMemberPresenceChangeEvent evt)
    {
        ChatRoom sourceChatRoom = (ChatRoom) evt.getSource();

        if(!sourceChatRoom.equals(chatRoomWrapper.getChatRoom()))
            return;

        String eventType = evt.getEventType();
        ChatRoomMember chatRoomMember = evt.getChatRoomMember();

        ConferenceChatContact chatContact = null;
        String statusMessage = null;

        if (eventType.equals(ChatRoomMemberPresenceChangeEvent.MEMBER_JOINED))
        {
            chatContact = new ConferenceChatContact(chatRoomMember);

            chatParticipants.add(chatContact);

            sessionRenderer.addChatContact(chatContact);

            statusMessage = GuiActivator.getResources().getI18NString(
                "service.gui.CHAT_ROOM_USER_JOINED",
                new String[] {sourceChatRoom.getName()});

            sessionRenderer.updateChatContactStatus(chatContact, statusMessage);
        }
        else if (eventType.equals(ChatRoomMemberPresenceChangeEvent.MEMBER_LEFT)
            || eventType.equals(ChatRoomMemberPresenceChangeEvent.MEMBER_KICKED)
            || eventType.equals(ChatRoomMemberPresenceChangeEvent.MEMBER_QUIT))
        {
            if(eventType.equals(ChatRoomMemberPresenceChangeEvent.MEMBER_LEFT))
            {
                statusMessage = GuiActivator.getResources().getI18NString(
                    "service.gui.CHAT_ROOM_USER_LEFT",
                    new String[] {sourceChatRoom.getName()});
            }
            else if(eventType.equals(
                ChatRoomMemberPresenceChangeEvent.MEMBER_KICKED))
            {
                statusMessage = GuiActivator.getResources().getI18NString(
                    "service.gui.CHAT_ROOM_USER_KICKED",
                    new String[] {sourceChatRoom.getName()});
            }
            else if(eventType.equals(
                ChatRoomMemberPresenceChangeEvent.MEMBER_QUIT))
            {
                statusMessage = GuiActivator.getResources().getI18NString(
                    "service.gui.CHAT_ROOM_USER_QUIT",
                    new String[] {sourceChatRoom.getName()});
            }

            Iterator<ConferenceChatContact> chatContactsIter
                = chatParticipants.iterator();

            while (chatContactsIter.hasNext())
            {
                chatContact = chatContactsIter.next();

                if(chatContact.getDescriptor().equals(chatRoomMember))
                {
                    sessionRenderer.updateChatContactStatus(
                        chatContact, statusMessage);

                    sessionRenderer.removeChatContact(chatContact);
                    break;
                }
            }
        }
    }

    public void chatRoomPropertyChangeFailed(
        ChatRoomPropertyChangeFailedEvent event)
    {}

    /**
     * Updates the chat panel when a property of the chat room has been modified.
     * 
     * @param evt the event containing information about the property change
     */
    public void chatRoomPropertyChanged(ChatRoomPropertyChangeEvent evt)
    {
        if(evt.getPropertyName().equals(
            ChatRoomPropertyChangeEvent.CHAT_ROOM_SUBJECT))
        {
            sessionRenderer.setChatSubject((String) evt.getNewValue());
        }
    }

    /**
     * Returns <code>true</code> if this contact is persistent, otherwise
     * returns <code>false</code>.
     * @return <code>true</code> if this contact is persistent, otherwise
     * returns <code>false</code>.
     */
    public boolean isDescriptorPersistent()
    {
        return true;
    }

    public ChatTransport findChatTransportForDescriptor(Object descriptor)
    {
        Iterator<ChatTransport> chatTransportsIter = chatTransports.iterator();

        while (chatTransportsIter.hasNext())
        {
            ChatTransport chatTransport = chatTransportsIter.next();

            if (chatTransport.getDescriptor().equals(descriptor))
                return chatTransport;
        }

        return null;
    }

    /**
     * Loads the given chat room in the this chat conference panel. Loads all
     * members and adds all corresponding listeners.
     * 
     * @param chatRoom the <tt>ChatRoom</tt> to load
     */
    public void loadChatRoom(ChatRoom chatRoom)
    {
        List membersList = chatRoom.getMembers();

        for (int i = 0; i < membersList.size(); i ++)
        {
            ChatContact chatContact
                = new ConferenceChatContact((ChatRoomMember)membersList.get(i));

            sessionRenderer.addChatContact(chatContact);
        }

        chatRoom.addPropertyChangeListener(this);
        chatRoom.addMemberPresenceListener(this);

        // Load the subject of the chat room.
        sessionRenderer.setChatSubject(chatRoom.getSubject());
    }

    /**
     * Implements the <tt>ChatPanel.getChatStatusIcon</tt> method.
     *
     * @return the status icon corresponding to this chat room
     */
    public ImageIcon getChatStatusIcon()
    {
        String status = Constants.OFFLINE_STATUS;

        if(chatRoomWrapper.getChatRoom() != null
            && chatRoomWrapper.getChatRoom().isJoined())
            status = Constants.ONLINE_STATUS;

        return new ImageIcon(Constants.getStatusIcon(status));
    }

    /**
     * Returns the avatar icon of this chat session.
     *
     * @return the avatar icon of this chat session.
     */
    public byte[] getChatAvatar()
    {
        return null;
    }

    /**
     * Initializes the list of participants.
     */
    private void initChatParticipants()
    {
        if(chatRoomWrapper.getChatRoom() != null
                && chatRoomWrapper.getChatRoom().isJoined())
        {
            List membersList = chatRoomWrapper.getChatRoom().getMembers();

            for (int i = 0; i < membersList.size(); i ++)
            {
                ChatContact chatContact = new ConferenceChatContact(
                    (ChatRoomMember) membersList.get(i));

                chatParticipants.add(chatContact);
            }
        }
    }
}
