package net.java.sip.communicator.slick.contactlist.mockprovider;

import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.icqconstants.*;

/**
 * A simple, straightforward mock implementation of the Contact interface
 * that  can be manually created and used in testing a
 * MetaContactList service
 *
 * @author Emil Ivov
 */
public class MockContact
    implements Contact
{
    private String contactID = null;
    private MockProvider parentProvider = null;

    /**
     * Creates an instance of a meta contact with the specified string used
     * as a name and identifier.
     *
     * @param id the identifier of this contact (also used as a name).
     * @param parentProvider
     */
    public MockContact(String id, MockProvider parentProvider)
    {
        this.contactID = id;
        this.parentProvider = parentProvider;
    }

    /**
     * Returns a String that can be used for identifying the contact.
     *
     * @return a String id representing and uniquely identifying the contact.
     */
    public String getAddress()
    {
        return contactID;
    }

    /**
     * Returns a String that could be used by any user interacting modules
     * for referring to this contact.
     *
     * @return a String that can be used for referring to this contact when
     *   interacting with the user.
     */
    public String getAlias()
    {
        return contactID;
    }

    /**
     * Returns a byte array containing an image (most often a photo or an
     * avatar) that the contact uses as a representation.
     *
     * @return byte[] an image representing the contact.
     */
    public byte[] getImage()
    {
        return null;
    }

    /**
     * Returns the status of the contact.
     *
     * @return always IcqStatusEnum.ONLINE.
     */
    public PresenceStatus getPresenceStatus()
    {
        return IcqStatusEnum.ONLINE;
    }

    /**
     * Returns a reference to the protocol provider that created the contact.
     *
     * @return a refererence to an instance of the ProtocolProviderService
     */
    public ProtocolProviderService getProtocolProvider()
    {
        return parentProvider;
    }

    /**
     * Determines whether or not this contact represents our own identity.
     *
     * @return true in case this is a contact that represents ourselves and
     *   false otherwise.
     */
    public boolean isLocal()
    {
        return false;
    }
}
