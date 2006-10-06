/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.msghistory;

import java.io.*;
import java.util.*;

import org.osgi.framework.*;
import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.service.history.*;
import net.java.sip.communicator.service.history.event.*;
import net.java.sip.communicator.service.history.event.ProgressEvent;
import net.java.sip.communicator.service.history.records.*;
import net.java.sip.communicator.service.msghistory.*;
import net.java.sip.communicator.service.msghistory.event.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.*;


/**
 * The Message History Service stores messages exchanged through the various protocols
 * Logs messages for all protocol providers that support basic instant messaging
 * (i.e. those that implement OperationSetBasicInstantMessaging).
 *
 * @author Alexander Pelov
 * @author Damian Minkov
 */
public class MessageHistoryServiceImpl
    implements  MessageHistoryService,
                MessageListener,
                ServiceListener
{
    /**
     * The logger for this class.
     */
    private static Logger logger = Logger
            .getLogger(MessageHistoryServiceImpl.class);

    private static String[] STRUCTURE_NAMES =
        new String[] { "dir", "msg_CDATA", "msgTyp", "enc", "uid", "sub", "receivedTimestamp" };

    private static HistoryRecordStructure recordStructure =
        new HistoryRecordStructure(STRUCTURE_NAMES);

    // the field used to search by keywords
    private static final String SEARCH_FIELD = "msg";

    /**
     * The BundleContext that we got from the OSGI bus.
     */
    private BundleContext bundleContext = null;

    private HistoryService historyService = null;

    private Object syncRoot_HistoryService = new Object();

    private Hashtable progressListeners = new Hashtable();

    public HistoryService getHistoryService()
    {
        return historyService;
    }

    /**
     * Returns all the messages exchanged by all the contacts
     * in the supplied metacontact after the given date
     *
     * @param contact MetaContact
     * @param startDate Date the start date of the conversations
     * @return Collection of MessageReceivedEvents or MessageDeliveredEvents
     * @throws RuntimeException
     */
    public Collection findByStartDate(MetaContact contact, Date startDate)
        throws RuntimeException
    {
        TreeSet result = new TreeSet(new MessageEventComparator());
        // get the readers for this contact
        Hashtable readers = getHistoryReaders(contact);

        Iterator iter = readers.keySet().iterator();
        while (iter.hasNext())
        {
            Contact item = (Contact)iter.next();
            HistoryReader reader = (HistoryReader)readers.get(item);

            // add the progress listeners
            addHistorySearchProgressListeners(reader, readers.size());
            Iterator recs = reader.findByStartDate(startDate);
            while (recs.hasNext())
            {
                result.add(convertHistoryRecordToMessageEvent((HistoryRecord)recs.next(), item));
            }
        }

        // now remove this listeners
        iter = readers.values().iterator();
        while (iter.hasNext())
        {
            HistoryReader item = (HistoryReader) iter.next();
            removeHistorySearchProgressListeners(item);
        }

        return result;
    }

    /**
     * Returns all the messages exchanged by all the contacts
     * in the supplied metacontact before the given date
     *
     * @param contact MetaContact
     * @param endDate Date the end date of the conversations
     * @return Collection of MessageReceivedEvents or MessageDeliveredEvents
     * @throws RuntimeException
     */
    public Collection findByEndDate(MetaContact contact, Date endDate)
        throws RuntimeException
    {
        TreeSet result = new TreeSet(new MessageEventComparator());

        // get the readers for this contact
        Hashtable readers = getHistoryReaders(contact);

        Iterator iter = readers.keySet().iterator();
        while (iter.hasNext())
        {
            Contact item = (Contact)iter.next();
            HistoryReader reader = (HistoryReader)readers.get(item);

            // add the progress listeners
            addHistorySearchProgressListeners(reader, readers.size());
            Iterator recs = reader.findByEndDate(endDate);
            while (recs.hasNext())
            {
                result.add(convertHistoryRecordToMessageEvent((HistoryRecord)recs.next(), item));
            }
        }

        // now remove this listeners
        iter = readers.values().iterator();
        while (iter.hasNext())
        {
            HistoryReader item = (HistoryReader) iter.next();
            removeHistorySearchProgressListeners(item);
        }

        return result;
    }

    /**
     * Returns all the messages exchanged by all the contacts
     * in the supplied metacontact between the given dates
     *
     * @param contact MetaContact
     * @param startDate Date the start date of the conversations
     * @param endDate Date the end date of the conversations
     * @return Collection of MessageReceivedEvents or MessageDeliveredEvents
     * @throws RuntimeException
     */
    public Collection findByPeriod(MetaContact contact, Date startDate, Date endDate)
        throws RuntimeException
    {
        TreeSet result = new TreeSet(new MessageEventComparator());
        // get the readers for this contact
        Hashtable readers = getHistoryReaders(contact);

        Iterator iter = readers.keySet().iterator();
        while (iter.hasNext())
        {
            Contact item = (Contact)iter.next();
            HistoryReader reader = (HistoryReader)readers.get(item);

            // add the progress listeners
            addHistorySearchProgressListeners(reader, readers.size());

            Iterator recs = reader.findByPeriod(startDate, endDate);
            while (recs.hasNext())
            {
                result.add(convertHistoryRecordToMessageEvent((HistoryRecord)recs.next(), item));
            }
        }

        // now remove this listeners
        iter = readers.values().iterator();
        while (iter.hasNext())
        {
            HistoryReader item = (HistoryReader) iter.next();
            removeHistorySearchProgressListeners(item);
        }

        return result;
    }

    /**
     * Returns all the messages exchanged by all the contacts
     * in the supplied metacontact between the given dates and having the given
     * keywords
     *
     * @param contact MetaContact
     * @param startDate Date the start date of the conversations
     * @param endDate Date the end date of the conversations
     * @param keywords array of keywords
     * @return Collection of MessageReceivedEvents or MessageDeliveredEvents
     * @throws RuntimeException
     */
    public Collection findByPeriod(MetaContact contact,
                                       Date startDate, Date endDate,
                                       String[] keywords)
        throws RuntimeException
    {
        return findByPeriod(contact, startDate, endDate, keywords, false);

    }

    /**
     * Returns all the messages exchanged by all the contacts
     * in the supplied metacontact having the given keyword
     *
     * @param contact MetaContact
     * @param keyword keyword
     * @return Collection of MessageReceivedEvents or MessageDeliveredEvents
     * @throws RuntimeException
     */
    public Collection findByKeyword(MetaContact contact, String keyword)
        throws RuntimeException
    {
        return findByKeyword(contact, keyword, false);
    }

    /**
     * Returns all the messages exchanged by all the contacts
     * in the supplied metacontact having the given keywords
     *
     * @param contact MetaContact
     * @param keywords keyword
     * @return Collection of MessageReceivedEvents or MessageDeliveredEvents
     * @throws RuntimeException
     */
    public Collection findByKeywords(MetaContact contact, String[] keywords)
        throws RuntimeException
    {
        return findByKeywords(contact, keywords, false);
    }

    /**
     * Returns the supplied number of recent messages exchanged by all the contacts
     * in the supplied metacontact
     *
     * @param contact MetaContact
     * @param count messages count
     * @return Collection of MessageReceivedEvents or MessageDeliveredEvents
     * @throws RuntimeException
     */
    public Collection findLast(MetaContact contact, int count)
        throws RuntimeException
    {
        TreeSet result = new TreeSet(new MessageEventComparator());

        Iterator iter = contact.getContacts();
        while (iter.hasNext())
        {
            Contact item = (Contact) iter.next();

            try
            {
                History history = this.getHistory(null, item);

                HistoryReader reader = history.getReader();
                Iterator recs = reader.findLast(count);
                while (recs.hasNext())
                {
                    result.add(
                        convertHistoryRecordToMessageEvent(
                            (HistoryRecord)recs.next(),
                            item));

                }
            } catch (IOException e)
            {
                logger.error("Could not read history", e);
            }
        }

        LinkedList resultAsList = new LinkedList(result);
        int startIndex = resultAsList.size() - count;

        if(startIndex < 0)
            startIndex = 0;

        return resultAsList.subList(startIndex, resultAsList.size());
    }

    /**
     * Returns the history by specified local and remote contact
     * if one of them is null the default is used
     *
     * @param localContact Contact
     * @param remoteContact Contact
     * @return History
     * @throws IOException
     */
    private History getHistory(Contact localContact, Contact remoteContact)
            throws IOException {
        History retVal = null;

        String localId = localContact == null ? "default" : localContact
                .getAddress();
        String remoteId = remoteContact == null ? "default" : remoteContact
                .getAddress();

        HistoryID historyId = HistoryID.createFromID(new String[] { "messages",
                localId, remoteId });

        if (this.historyService.isHistoryExisting(historyId))
        {
            retVal = this.historyService.getHistory(historyId);
        } else {
            retVal = this.historyService.createHistory(historyId,
                    recordStructure);
        }

        return retVal;
    }

    /**
     * Used to convert HistoryRecord in MessageDeliveredEvent or MessageReceivedEvent
     * which are returned by the finder methods
     *
     * @param hr HistoryRecord
     * @param contact Contact
     * @return Object
     */
    private Object convertHistoryRecordToMessageEvent(HistoryRecord hr, Contact contact)
    {
        MessageImpl msg = new MessageImpl(hr);
        Date timestamp = null;

        // if there is value for date of receiving the message
        // this is the event timestamp (this is the date that had came from protocol)
        // the HistoryRecord timestamp is the timestamp when the record was written
        if(msg.getMessageReceivedDate() != null)
        {
            if(msg.getMessageReceivedDate().after(hr.getTimestamp()) &&
                (msg.getMessageReceivedDate().getTime() -
                 hr.getTimestamp().getTime()) > 86400000) // 24*60*60*1000
                timestamp = hr.getTimestamp();
            else
                timestamp = msg.getMessageReceivedDate();
        }
        else
            timestamp = hr.getTimestamp();

        if(msg.isOutgoing)
        {
            return new MessageDeliveredEvent(
                    msg,
                    contact,
                    timestamp);
        }
        else
            return new MessageReceivedEvent(
                        msg,
                        contact,
                        timestamp);
    }

    /**
     * starts the service. Check the current registerd protocol providers
     * which supports BasicIM and adds message listener to them
     *
     * @param bc BundleContext
     */
    public void start(BundleContext bc)
    {
        logger.debug("Starting the meta contact list implementation.");
        this.bundleContext = bc;

        // start listening for newly register or removed protocol providers
        bc.addServiceListener(this);

        ServiceReference[] protocolProviderRefs = null;
        try
        {
            protocolProviderRefs = bc.getServiceReferences(
                ProtocolProviderService.class.getName(),
                null);
        }
        catch (InvalidSyntaxException ex)
        {
            // this shouldn't happen since we're providing no parameter string
            // but let's log just in case.
            logger.error(
                "Error while retrieving service refs", ex);
            return;
        }

        // in case we found any
        if (protocolProviderRefs != null)
        {
            logger.debug("Found "
                         + protocolProviderRefs.length
                         + " already installed providers.");
            for (int i = 0; i < protocolProviderRefs.length; i++)
            {
                ProtocolProviderService provider = (ProtocolProviderService) bc
                    .getService(protocolProviderRefs[i]);

                this.handleProviderAdded(provider);
            }
        }
    }


    // //////////////////////////////////////////////////////////////////////////
    // MessageListener implementation methods

    public void messageReceived(MessageReceivedEvent evt)
    {
        this.writeMessage("in", null, evt.getSourceContact(), evt
                .getSourceMessage(), evt.getTimestamp());
    }

    public void messageDelivered(MessageDeliveredEvent evt)
    {
        this.writeMessage("out", null, evt.getDestinationContact(), evt
                .getSourceMessage(), evt.getTimestamp());
    }

    public void messageDeliveryFailed(MessageDeliveryFailedEvent evt)
    {
    }

    /**
     *
     * @param direction String
     * @param source Contact
     * @param destination Contact
     * @param message Message
     * @param messageTimestamp Date this is the timestamp when was message received
     *                          that came from the protocol provider
     */
    private void writeMessage(String direction, Contact source,
            Contact destination, Message message, Date messageTimestamp)
    {
        try {
            History history = this.getHistory(source, destination);
            HistoryWriter historyWriter = history.getWriter();
            historyWriter.addRecord(new String[] { direction,
                    message.getContent(), message.getContentType(),
                    message.getEncoding(), message.getMessageUID(),
                    message.getSubject(), String.valueOf(messageTimestamp.getTime()) },
                    new Date()); // this date is when the history record is written
        } catch (IOException e)
        {
            logger.error("Could not add message to history", e);
        }
    }
    // //////////////////////////////////////////////////////////////////////////

    /**
     * Set the configuration service.
     *
     * @param historyService HistoryService
     * @throws IOException
     * @throws IllegalArgumentException
     */
    public void setHistoryService(HistoryService historyService)
            throws IllegalArgumentException, IOException {
        synchronized (this.syncRoot_HistoryService)
        {
            this.historyService = historyService;

            logger.debug("New history service registered.");
        }
    }

    /**
     * Remove a configuration service.
     *
     * @param historyService HistoryService
     */
    public void unsetHistoryService(HistoryService historyService)
    {
        synchronized (this.syncRoot_HistoryService)
        {
            if (this.historyService == historyService)
            {
                this.historyService = null;

                logger.debug("History service unregistered.");
            }
        }
    }

    /**
     * When new protocol provider is registered we check
     * does it supports BasicIM and if so add a listener to it
     *
     * @param serviceEvent ServiceEvent
     */
    public void serviceChanged(ServiceEvent serviceEvent)
    {
        Object sService = bundleContext.getService(serviceEvent.getServiceReference());

        logger.trace("Received a service event for: " + sService.getClass().getName());

        // we don't care if the source service is not a protocol provider
        if (! (sService instanceof ProtocolProviderService))
        {
            return;
        }

        logger.debug("Service is a protocol provider.");
        if (serviceEvent.getType() == ServiceEvent.REGISTERED)
        {
            logger.debug("Handling registration of a new Protocol Provider.");

            this.handleProviderAdded((ProtocolProviderService)sService);
        }
        else if (serviceEvent.getType() == ServiceEvent.UNREGISTERING)
        {
            this.handleProviderRemoved( (ProtocolProviderService) sService);
        }

    }

    /**
     * Used to attach the Message History Service to existing or
     * just registered protocol provider. Checks if the provider has implementation
     * of OperationSetBasicInstantMessaging
     *
     * @param provider ProtocolProviderService
     */
    private void handleProviderAdded(ProtocolProviderService provider)
    {
        logger.debug("Adding protocol provider " + provider.getProtocolName());

        // check whether the provider has a basic im operation set
        OperationSetBasicInstantMessaging opSetIm
            = (OperationSetBasicInstantMessaging) provider
            .getSupportedOperationSets().get(
                OperationSetBasicInstantMessaging.class.getName());

        if (opSetIm != null)
        {
            opSetIm.addMessageListener(this);
        }
        else
        {
            logger.trace("Service did not have a im op. set.");
        }
    }

    /**
     * Removes the specified provider from the list of currently known providers
     * and ignores all the messages exchanged by it
     *
     * @param provider the ProtocolProviderService that has been unregistered.
     */
    private void handleProviderRemoved(ProtocolProviderService provider)
    {
        OperationSetBasicInstantMessaging opSetIm
            = (OperationSetBasicInstantMessaging) provider
            .getSupportedOperationSets().get(
                OperationSetBasicInstantMessaging.class.getName());

        if (opSetIm != null)
        {
            opSetIm.removeMessageListener(this);
        }
    }

    /**
     * Adding progress listener for monitoring progress of search process
     *
     * @param listener HistorySearchProgressListener
     */
    public void addSearchProgressListener(MessageHistorySearchProgressListener
                                          listener)
    {
        synchronized(progressListeners){
            HistorySearchProgressListener wrapperListener =
                new SearchProgressWrapper(listener);
            progressListeners.put(listener, wrapperListener);
        }
    }

    /**
     * Removing progress listener
     *
     * @param listener HistorySearchProgressListener
     */
    public void removeSearchProgressListener(
        MessageHistorySearchProgressListener listener)
    {
        synchronized(progressListeners){
            progressListeners.remove(listener);
        }
    }

    /**
     * Add the registered MessageHistorySearchProgressListeners
     * to the given HistoryReader
     *
     * @param reader HistoryReader
     * @param countContacts number of contacts will search
     */
    private void addHistorySearchProgressListeners(
        HistoryReader reader, int countContacts)
    {
        synchronized(progressListeners)
        {
            Iterator iter = progressListeners.values().iterator();
            while (iter.hasNext())
            {
                SearchProgressWrapper l =
                    (SearchProgressWrapper) iter.next();
                l.contactCount = countContacts;
                reader.addSearchProgressListener(l);
            }
        }
    }

    /**
     * Removes the registered MessageHistorySearchProgressListeners
     * from the given HistoryReader
     *
     * @param reader HistoryReader
     */
    private void removeHistorySearchProgressListeners(HistoryReader reader)
    {
        synchronized(progressListeners)
        {
            Iterator iter = progressListeners.values().iterator();
            while (iter.hasNext())
            {
                SearchProgressWrapper l =
                    (SearchProgressWrapper) iter.next();
                l.clear();
                reader.removeSearchProgressListener(l);
            }
        }
    }

    /**
     * Returns all the messages exchanged by all the contacts
     * in the supplied metacontact between the given dates and having the given
     * keywords
     *
     * @param contact MetaContact
     * @param startDate Date the start date of the conversations
     * @param endDate Date the end date of the conversations
     * @param keywords array of keywords
     * @param caseSensitive is keywords search case sensitive
     * @return Collection of MessageReceivedEvents or MessageDeliveredEvents
     * @throws RuntimeException
     */
    public Collection findByPeriod(MetaContact contact, Date startDate,
                                   Date endDate, String[] keywords,
                                   boolean caseSensitive)
        throws RuntimeException
    {
        TreeSet result = new TreeSet(new MessageEventComparator());
        // get the readers for this contact
        Hashtable readers = getHistoryReaders(contact);

        Iterator iter = readers.keySet().iterator();
        while (iter.hasNext())
        {
            Contact item = (Contact) iter.next();
            HistoryReader reader = (HistoryReader) readers.get(item);

            // add the progress listeners
            addHistorySearchProgressListeners(reader, readers.size());
            Iterator recs = reader.findByPeriod(startDate, endDate, keywords,
                                                SEARCH_FIELD, caseSensitive);
            while (recs.hasNext())
            {
                result.add(convertHistoryRecordToMessageEvent( (HistoryRecord) recs.
                    next(), item));
            }
        }

        // now remove this listeners
        iter = readers.values().iterator();
        while (iter.hasNext())
        {
            HistoryReader item = (HistoryReader) iter.next();
            removeHistorySearchProgressListeners(item);
        }

        return result;
    }

    /**
     * Returns all the messages exchanged by all the contacts
     * in the supplied metacontact having the given keyword
     *
     * @param contact MetaContact
     * @param keyword keyword
     * @param caseSensitive is keywords search case sensitive
     * @return Collection of MessageReceivedEvents or MessageDeliveredEvents
     * @throws RuntimeException
     */
    public Collection findByKeyword(MetaContact contact, String keyword,
                                    boolean caseSensitive)
        throws RuntimeException
    {
        TreeSet result = new TreeSet(new MessageEventComparator());
        // get the readers for this contact
        Hashtable readers = getHistoryReaders(contact);

        Iterator iter = readers.keySet().iterator();
        while (iter.hasNext())
        {
            Contact item = (Contact) iter.next();
            HistoryReader reader = (HistoryReader) readers.get(item);

            // add the progress listeners
            addHistorySearchProgressListeners(reader, readers.size());
            Iterator recs = reader.
                findByKeyword(keyword, SEARCH_FIELD, caseSensitive);
            while (recs.hasNext())
            {
                result.add(convertHistoryRecordToMessageEvent( (HistoryRecord) recs.
                    next(), item));
            }
        }

        // now remove this listeners
        iter = readers.values().iterator();
        while (iter.hasNext())
        {
            HistoryReader item = (HistoryReader) iter.next();
            removeHistorySearchProgressListeners(item);
        }

        return result;
    }

    /**
     * Returns all the messages exchanged by all the contacts
     * in the supplied metacontact having the given keywords
     *
     * @param contact MetaContact
     * @param keywords keyword
     * @param caseSensitive is keywords search case sensitive
     * @return Collection of MessageReceivedEvents or MessageDeliveredEvents
     * @throws RuntimeException
     */
    public Collection findByKeywords(MetaContact contact, String[] keywords,
                                     boolean caseSensitive)
        throws RuntimeException
    {
        TreeSet result = new TreeSet(new MessageEventComparator());
        // get the readers for this contact
        Hashtable readers = getHistoryReaders(contact);

        Iterator iter = readers.keySet().iterator();
        while (iter.hasNext())
        {
            Contact item = (Contact) iter.next();
            HistoryReader reader = (HistoryReader) readers.get(item);

            // add the progress listeners
            addHistorySearchProgressListeners(reader, readers.size());
            Iterator recs = reader.
                findByKeywords(keywords, SEARCH_FIELD, caseSensitive);
            while (recs.hasNext())
            {
                result.add(convertHistoryRecordToMessageEvent( (HistoryRecord) recs.
                    next(), item));
            }
        }

        // now remove this listeners
        iter = readers.values().iterator();
        while (iter.hasNext())
        {
            HistoryReader item = (HistoryReader) iter.next();
            removeHistorySearchProgressListeners(item);
        }

        return result;
    }

    /**
     * Gets all the history readers for the contacts in the given MetaContact
     * @param contact MetaContact
     * @return Hashtable
     */
    private Hashtable getHistoryReaders(MetaContact contact)
    {
        Hashtable readers = new Hashtable();
        Iterator iter = contact.getContacts();
        while (iter.hasNext())
        {
            Contact item = (Contact) iter.next();

            try
            {
                History history = this.getHistory(null, item);
                readers.put(item, history.getReader());
            }
            catch (IOException e)
            {
                logger.error("Could not read history", e);
            }
        }
        return readers;
    }

    /**
     * A wrapper around HistorySearchProgressListener
     * that fires events for MessageHistorySearchProgressListener
     */
    private class SearchProgressWrapper
        implements HistorySearchProgressListener
    {
        private MessageHistorySearchProgressListener listener = null;
        int contactCount = 0;
        int currentContactCount = 0;
        int currentProgress = 0;
        int lastHistoryProgress = 0;

        SearchProgressWrapper(MessageHistorySearchProgressListener listener)
        {
            this.listener = listener;
        }

        public void progressChanged(ProgressEvent evt)
        {
            int progress = getProgressMapping(evt.getProgress());

            listener.progressChanged(
                new net.java.sip.communicator.service.msghistory.event.
                    ProgressEvent(MessageHistoryServiceImpl.this,
                    evt, progress));
        }

        /**
         * Calculates the progress according the count of the contacts
         * we will search
         * @param historyProgress int
         * @return int
         */
        private int getProgressMapping(int historyProgress)
        {
            currentProgress += (historyProgress - lastHistoryProgress)/contactCount;

            if(historyProgress == HistorySearchProgressListener.PROGRESS_MAXIMUM_VALUE)
            {
                currentContactCount++;
                lastHistoryProgress = 0;

                // this is the last one and the last event fire the max
                // there will be looses in currentProgress due to the devision
                if(currentContactCount == contactCount)
                    currentProgress =
                        MessageHistorySearchProgressListener.PROGRESS_MAXIMUM_VALUE;
            }
            else
                lastHistoryProgress = historyProgress;

            return currentProgress;
        }

        /**
         * clear the values
         */
        void clear()
        {
            contactCount = 0;
            currentProgress = 0;
            lastHistoryProgress = 0;
            currentContactCount = 0;
        }
    }

    /**
     * Simple message implementation.
     */
    private class MessageImpl
        implements Message
    {
        private String textContent = null;
        private String contentType = null;
        private String contentEncoding = null;
        private String messageUID = null;
        private String subject = null;

        private boolean isOutgoing = false;

        private Date messageReceivedDate = null;

        MessageImpl(HistoryRecord hr)
        {
            // History structure
            // 0 - dir
            // 1 - msg_CDATA
            // 2 - msgTyp
            // 3 - enc
            // 4- uid
            // 5 - sub
            // 6 - receivedTimestamp

            for (int i = 0; i < hr.getPropertyNames().length; i++)
            {
                String propName = hr.getPropertyNames()[i];

                if(propName.equals("msg") || propName.equals(STRUCTURE_NAMES[1]))
                    textContent = hr.getPropertyValues()[i];
                else if(propName.equals(STRUCTURE_NAMES[2]))
                    contentType = hr.getPropertyValues()[i];
                else if(propName.equals(STRUCTURE_NAMES[3]))
                    contentEncoding = hr.getPropertyValues()[i];
                else if(propName.equals(STRUCTURE_NAMES[4]))
                    messageUID = hr.getPropertyValues()[i];
                else if(propName.equals(STRUCTURE_NAMES[5]))
                    subject = hr.getPropertyValues()[i];
                else if(propName.equals(STRUCTURE_NAMES[0]))
                {
                    if (hr.getPropertyValues()[i].equals("in"))
                        isOutgoing = false;
                    else if (hr.getPropertyValues()[i].equals("out"))
                        isOutgoing = true;
                }
                else if(propName.equals(STRUCTURE_NAMES[6]))
                {
                    messageReceivedDate = new Date(
                        Long.parseLong(hr.getPropertyValues()[i]));
                }
            }
        }

        public String getContent()
        {
            return textContent;
        }

        public String getContentType()
        {
            return contentType;
        }

        public String getEncoding()
        {
            return contentEncoding;
        }

        public String getMessageUID()
        {
            return messageUID;
        }

        public byte[] getRawData()
        {
            return getContent().getBytes();
        }

        public int getSize()
        {
            return getContent().length();
        }

        public String getSubject()
        {
            return subject;
        }

        public Date getMessageReceivedDate()
        {
            return messageReceivedDate;
        }
    }

    /**
     * Used to compare MessageDeliveredEvent or MessageReceivedEvent
     * and to be ordered in TreeSet according their timestamp
     */
    private class MessageEventComparator
        implements Comparator
    {
        public int compare(Object o1, Object o2)
        {
            Date date1 = null;
            Date date2 = null;

            if(o1 instanceof MessageDeliveredEvent)
                date1 = ((MessageDeliveredEvent)o1).getTimestamp();
            else if(o1 instanceof MessageReceivedEvent)
                date1 = ((MessageReceivedEvent)o1).getTimestamp();
            else
                return 0;

            if(o2 instanceof MessageDeliveredEvent)
                date2 = ((MessageDeliveredEvent)o2).getTimestamp();
            else if(o2 instanceof MessageReceivedEvent)
                date2 = ((MessageReceivedEvent)o2).getTimestamp();
            else
                return 0;

            return date1.compareTo(date2);
        }
    }
}
