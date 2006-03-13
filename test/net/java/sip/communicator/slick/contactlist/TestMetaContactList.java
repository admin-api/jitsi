/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.slick.contactlist;

import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import junit.framework.*;
import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.Logger;
import net.java.sip.communicator.slick.contactlist.mockprovider.*;
import net.java.sip.communicator.service.contactlist.event.*;
import java.util.*;

/**
 * Test meta contact list functionality such as filling in the contact list from
 * existing protocol providers, properly handling events of modified server
 * stored contact lists and modifying server stored contact lists through the
 * meta contact list service. Testing is done against the MockProvider which
 * is directly accessible throughout the tests.
 * <p>
 * What we still need to test here:<br>
 * 1. Test that groups are automatically created when proto contacts are moved.
 * <br>
 * 2. Test that events are generated when creating moving and removing groups
 *    from the metacontact list itself.
 * <br>
 *
 * @author Emil Ivov
 */
public class TestMetaContactList
    extends TestCase
{
    /**
     * A reference to the SLICK fixture.
     */
    private MclSlickFixture fixture = new MclSlickFixture(getClass().getName());

    /**
     * The name of the new subscripiton that we create during testing.
     */
    private String newSubscriptionName = "NewSubscription";

    /**
     * The name to use when renaming the new contat group.
     */
    private String renamedGroupName = "RenamedContactGroup";

    private static final Logger logger =
        Logger.getLogger(TestMetaContactList.class);

    private OperationSetPersistentPresence opSetPersPresence;

    /**
     * Creates a unit test with the specified name.
     * @param name the name of one of the test methods in this class.
     */
    public TestMetaContactList(String name)
    {
        super(name);
    }

    /**
     * Initialize the environment.
     * @throws Exception if anything goes wrong.
     */
    protected void setUp() throws Exception
    {
        super.setUp();
        fixture.setUp();

        Map supportedOperationSets =
            MclSlickFixture.mockProvider.getSupportedOperationSets();

        if ( supportedOperationSets == null
            || supportedOperationSets.size() < 1)
            throw new NullPointerException(
                "No OperationSet implementations are supported by "
                +"this ICQ implementation. ");

        //get the operation set presence here.
        opSetPersPresence =
            (OperationSetPersistentPresence)supportedOperationSets.get(
                OperationSetPersistentPresence.class.getName());

        //if still null then the implementation doesn't offer a presence
        //operation set which is unacceptable for icq.
        if (opSetPersPresence == null)
            throw new NullPointerException(
                "An implementation of the ICQ service must provide an "
                + "implementation of at least the one of the Presence "
                + "Operation Sets");
    }

    /**
     * Finalization
     * @throws Exception in case sth goes wrong.
     */
    protected void tearDown() throws Exception
    {
        fixture.tearDown();
        super.tearDown();
    }

    /**
     * Verifies that the contacts retrieved by the meta contact list service,
     * matches the one that were in the mock provider.
     */
    public void testContactListRetrieving()
    {
        MockContactGroup expectedRoot = (MockContactGroup)opSetPersPresence
                                            .getServerStoredContactListRoot();

        logger.debug("============== Predefined contact List ==============");

        logger.debug("rootGroup="+expectedRoot.getGroupName()
                +" rootGroup.childContacts="+expectedRoot.countContacts()
                + "rootGroup.childGroups="+expectedRoot.countSubgroups()
                + " Printing rootGroupContents=\n"+expectedRoot.toString());

        MetaContactGroup actualRoot = fixture.metaClService.getRoot();

        logger.debug("================ Meta Contact List =================");

        logger.debug("rootGroup="+actualRoot.getGroupName()
                     +" rootGroup.childContacts="+actualRoot.countChildContacts()
                     + " rootGroup.childGroups="+actualRoot.countSubgroups()
                     + " Printing rootGroupContents=\n"+actualRoot.toString());

        assertGroupEquals(expectedRoot, actualRoot);
    }

    /**
     * Makes sure that the specified actualGroup contains the same contacts
     * and subgroups as the expectedGroup. (Method operates recursively).
     * @param expectedGroup a MockContactGroup instance used as a reference.
     * @param actualGroup the MetaContactGroup retrieved from the metacontact
     * list.
     */
    private void assertGroupEquals(MockContactGroup expectedGroup,
                                   MetaContactGroup actualGroup)
    {
        assertNotNull("Group " + expectedGroup.getGroupName() + " was "
                      + "returned by the MetaContactListService implementation "
                      + "but was not in the expected contact list."
                      , actualGroup);

        assertEquals("Group " + expectedGroup.getGroupName()
                     + " did not have the expected number of member contacts"
                     , expectedGroup.countContacts()
                     , actualGroup.countChildContacts());

        assertEquals("Group " + expectedGroup.getGroupName()
                     + " did not have the expected number of member contacts"
                     , expectedGroup.countContacts()
                     , actualGroup.countChildContacts());
        assertEquals("Group " + expectedGroup.getGroupName()
                     + " did not have the expected number of sub groups"
                     , expectedGroup.countSubgroups()
                     , actualGroup.countSubgroups());

        //go over the subgroups and check that they've been all added to the
        //meta contact list.
        Iterator expectedSubgroups = expectedGroup.subGroups();
        while (expectedSubgroups.hasNext() ){
            MockContactGroup expectedSubGroup
                = (MockContactGroup)expectedSubgroups.next();

            MetaContactGroup actualSubGroup
                = actualGroup
                    .getMetaContactSubgroup(expectedSubGroup.getGroupName());

            assertGroupEquals(expectedSubGroup, actualSubGroup);
        }

        Iterator actualContactsIter = actualGroup.getChildContacts();

        //check whether every contact in the meta list exists in the source
        //mock provider contact list.
        while (actualContactsIter.hasNext())
        {
            MetaContact actualMetaContact
                = (MetaContact) actualContactsIter.next();

            assertEquals("Number of protocol specific contacts in a MetaContact"
                          , 1, actualMetaContact.getContactCount());

            assertTrue(
                "No contacts were encapsulated by MetaContact: "
                + actualMetaContact
                , actualMetaContact.getContacts().hasNext());

            Contact actualProtoContact
                = (Contact)actualMetaContact.getContacts().next();

            assertNotNull("getContactForProvider returned null for MockProvider"
                          , actualProtoContact);

            Contact expectedProtoContact
                = expectedGroup.getContact(actualProtoContact.getAddress());

            assertNotNull("Contact " + actualMetaContact.getDisplayName()
                          + " was returned by "
                          + "the MetaContactListService implementation but was "
                          + "not in the expected contact list."
                          , expectedProtoContact);
        }
    }

    /**
     * Performs several tests in order to verify that the findMetaContactByID
     * method of the tested implementation is working properly. We'll first
     * try to locate by using iterators a couple of contacts in different
     * levels. Once e get their references, we'll try to locate them through
     * the findMetaContactByMetaUID method.
     */
    public void testFindMetaContactByMetaUID()
    {
        MetaContactGroup root = fixture.metaClService.getRoot();

        //get a top level contact and then try to find it through the tested
        //findMetaContactByMetaUID method.
        Iterator contactsIter = root.getChildContacts();

        assertTrue(
            "No contacts were found in the meta contact list"
            , contactsIter.hasNext());

        MetaContact expectedContact = (MetaContact)contactsIter.next();

        MetaContact actualResult = fixture.metaClService
            .findMetaContactByMetaUID(expectedContact.getMetaUID());

        assertEquals("find failed for contact "+expectedContact.getDisplayName()
                     , expectedContact, actualResult);

        // get one of the subgroups, extract one of its child contacts and
        // repeat the same test.
        Iterator subgroupsIter = root.getSubgroups();

        assertTrue(
            "No sub groups were found in the meta contact list"
            , subgroupsIter.hasNext());

        MetaContactGroup subgroup = (MetaContactGroup)subgroupsIter.next();

        contactsIter = subgroup.getChildContacts();

        assertTrue(
            "No contacts were found in the meta group: "
            + subgroup.getGroupName()
            , contactsIter.hasNext());

        expectedContact = (MetaContact)contactsIter.next();

        actualResult = fixture.metaClService
            .findMetaContactByMetaUID(expectedContact.getMetaUID());

        assertEquals("find failed for contact "+expectedContact.getDisplayName()
                     , expectedContact, actualResult);

    }

    /**
     * Performs several tests in order to verify that the findMetaContactByContact
     * method of the tested implementation is working properly. We'll first
     * try to locate by using iterators a couple of contacts in different
     * levels. Once we get their references, we'll try to locate them through
     * the findMetaContactByContact method.
     */
    public void testFindMetaContactByContact()
    {
        MetaContactGroup root = fixture.metaClService.getRoot();

        //get a top level contact and then try to find it through the tested
        //findMetaContactByContact method.
        Iterator contactsIter = root.getChildContacts();

        assertTrue(
            "No contacts were found in the meta contact list"
            , contactsIter.hasNext());

        MetaContact expectedMetaContact = (MetaContact)contactsIter.next();

        assertTrue(
            "No contacts are encapsulated by MetaContact: "
            + expectedMetaContact.getDisplayName()
            , expectedMetaContact.getContacts().hasNext());


        Contact mockContact = (Contact)expectedMetaContact.getContacts().next();

        MetaContact actualResult = fixture.metaClService
                                .findMetaContactByContact(mockContact);

        assertEquals("find failed for contact "+expectedMetaContact.getDisplayName()
                     , expectedMetaContact, actualResult);

        // get one of the subgroups, extract one of its child contacts and
        // repeat the same test.
        Iterator subgroupsIter = root.getSubgroups();

        assertTrue(
            "No sub groups were found in the meta contact list"
            , subgroupsIter.hasNext());

        MetaContactGroup subgroup = (MetaContactGroup)subgroupsIter.next();

        contactsIter = subgroup.getChildContacts();

        assertTrue(
            "No contacts were found in MetaContactGroup: "
            + subgroup.getGroupName()
            , contactsIter.hasNext());


        expectedMetaContact = (MetaContact)contactsIter.next();

        assertTrue(
            "No contacts were encapsulated by meta contact: "
            + expectedMetaContact.getDisplayName()
            , expectedMetaContact.getContacts().hasNext());


        mockContact = (Contact)expectedMetaContact.getContacts().next();

        actualResult = fixture.metaClService
            .findMetaContactByContact(mockContact);

        assertEquals("find failed for contact "
                     + expectedMetaContact.getDisplayName()
                     , expectedMetaContact, actualResult);

    }

    /**
     * Performs several tests in order to verify that the
     * <tt>findMetaContactGroupByContactGroup</tt>  method of the tested
     * implementation is working properly. We'll first try to locate by using
     * iterators a couple of protocol specific groups in different levels.
     * Once we get their references, we'll try to locate them through
     * the findMetaContactGroupByContactGroup method.
     */
    public void testFindMetaContactGroupByContactGroup()
    {
        MetaContactGroup root = fixture.metaClService.getRoot();

        //get a group, extract its proto group and then try to obtain a
        //reference through the tested find method.
        Iterator groupsIter = root.getSubgroups();

        assertTrue(
            "No sub groups were found in the meta contact list"
            , groupsIter.hasNext());

        MetaContactGroup expectedMetaContactGroup
                                    = (MetaContactGroup)groupsIter.next();

        assertTrue(
            "There were no contact groups encapsulated in MetaContactGroup: "
            + expectedMetaContactGroup
            , expectedMetaContactGroup.getContactGroups().hasNext());

        assertTrue(
            "No ContactGroups are encapsulated by MetaContactGroup: "
            + expectedMetaContactGroup
            , expectedMetaContactGroup.getContactGroups().hasNext());

        ContactGroup mockContactGroup = (ContactGroup)expectedMetaContactGroup
                                                    .getContactGroups().next();
        MetaContactGroup actualMetaContactGroup = fixture.metaClService
            .findMetaContactGroupByContactGroup(mockContactGroup);

        assertSame("find failed for contact group " + mockContactGroup
                   , expectedMetaContactGroup, actualMetaContactGroup);
    }


    /**
     * In this test we'll add and remove users to the mock provider, and check
     * whether the meta contact list dispatches the corresponding meta contact
     * list event.
     *
     * @throws java.lang.Exception if anything goes wrong.
     */
    public void testSubscriptionHandling() throws Exception
    {
        //add a subscription and check that the corresponding event is generated
        MclEventCollector mclEvtCollector = new MclEventCollector();

        fixture.metaClService.addContactListListener(mclEvtCollector);
        opSetPersPresence.subscribe(newSubscriptionName);

        fixture.metaClService.removeContactListListener(mclEvtCollector);

        //first check that the newly created contact was really added
        MockContact newProtoContact = (MockContact)opSetPersPresence
                                        .findContactByID(newSubscriptionName);
        MetaContact newMetaContact = fixture.metaClService
            .findMetaContactByContact(newProtoContact);

        assertNotNull("The meta contact list was not updated after adding "
                      +"contact "+ newProtoContact +" to the mock provider."
                      , newMetaContact);

        assertEquals("Number of evts dispatched while adding a contact"
                     , 1
                     , mclEvtCollector.collectedEvents.size());
        MetaContactEvent evt = (MetaContactEvent)mclEvtCollector
                                                    .collectedEvents.get(0);

        assertEquals("ID of the generated event",
                     MetaContactEvent.META_CONTACT_ADDED,
                     evt.getEventID());

        assertEquals("Parent group of the source contact"
                    , fixture.metaClService.getRoot()
                    , evt.getParentGroup());

        assertEquals("Source meta contact."
                     , newMetaContact, evt.getSourceContact());

        assertEquals("Source provider"
                     , fixture.mockProvider, evt.getSourceProvider());

        //remove the subscirption and check for the event
        mclEvtCollector.collectedEvents.clear();

        fixture.metaClService.addContactListListener(mclEvtCollector);

        opSetPersPresence.unsubscribe(newProtoContact);

        fixture.metaClService.removeContactListListener(mclEvtCollector);

        //first check that the newly created contact was really added
        assertNull(
            "The impl contact list did not update after a subscr. was removed."
            ,fixture.metaClService.findMetaContactByContact(newProtoContact));

        assertEquals("Number of evts dispatched while adding a contact"
                     , 1
                     , mclEvtCollector.collectedEvents.size());
        evt = (MetaContactEvent)mclEvtCollector.collectedEvents.get(0);

        assertEquals("ID of the generated event",
                     MetaContactEvent.META_CONTACT_REMOVED,
                     evt.getEventID());

        assertEquals("Parent group of the source contact"
                    , fixture.metaClService.getRoot()
                    , evt.getParentGroup());

        assertEquals("Source meta contact."
                     , newMetaContact, evt.getSourceContact());

        assertEquals("Source provider"
                     , fixture.mockProvider, evt.getSourceProvider());

    }


    /**
     * In this test we'll add and remove groups to the mock provider, and check
     * whether the meta contact list dispatches the corresponding meta contact
     * list events and whether it gets properly updated.
     *
     * @throws java.lang.Exception if anything goes wrong.
     */
    public void testGroupChangeEventHandling() throws Exception
    {
        String newGroupName = "testGroupChangeEventHandling.NewContactGroup";
        //add a group and check for the event
        MclEventCollector mclEvtCollector = new MclEventCollector();

        fixture.metaClService.addContactListListener(mclEvtCollector);
        opSetPersPresence.createServerStoredContactGroup(
            opSetPersPresence.getServerStoredContactListRoot(), newGroupName);

        fixture.metaClService.removeContactListListener(mclEvtCollector);

        // first check whether event delivery went ok.
        assertEquals("Number of evts dispatched while adding a contact group"
                     , 1
                     , mclEvtCollector.collectedEvents.size());
        MetaContactGroupEvent evt = (MetaContactGroupEvent)mclEvtCollector
                                                    .collectedEvents.get(0);

        assertEquals("ID of the generated event",
                     MetaContactGroupEvent.META_CONTACT_GROUP_ADDED,
                     evt.getEventID());

        assertEquals("Source group of the AddEvent."
                    , newGroupName
                    , evt.getSourceMetaContactGroup().getGroupName());

        //first check that the newly created group was really added
        MetaContactGroup newMetaGroup = evt.getSourceMetaContactGroup();

        assertEquals("Source provider for the add event."
                     , fixture.mockProvider, evt.getSourceProvider());

        ContactGroup newProtoGroup = newMetaGroup.getContactGroup(
                                            newGroupName, fixture.mockProvider);

        assertNotNull("The new meta contact group did not contain a proto group"
                    , newProtoGroup);

        assertEquals("The new meta contact group did not seem to contain "
                     + "the right protocol contact group."
                     , newProtoGroup.getGroupName()
                     , newGroupName);

        assertEquals("The new meta contact group did not seem to contain "
                     + "the right protocol contact group."
                     , newProtoGroup.getProtocolProvider()
                     , fixture.mockProvider);


        mclEvtCollector.collectedEvents.clear();
        //rename the group and see that the corresponding events are handled
        //properly
        fixture.metaClService.addContactListListener(mclEvtCollector);
        opSetPersPresence.renameServerStoredContactGroup(
                                            newProtoGroup, renamedGroupName);

        fixture.metaClService.removeContactListListener(mclEvtCollector);

        //first check that the group was really renamed
        assertEquals("Number of evts dispatched while renaming a contact group"
                     , 1
                     , mclEvtCollector.collectedEvents.size());

        evt = (MetaContactGroupEvent)mclEvtCollector.collectedEvents.get(0);

        assertEquals("ID of the generated event",
                     MetaContactGroupEvent.CONTACT_GROUP_RENAMED_IN_META_GROUP,
                     evt.getEventID());

        assertEquals("Source group for the RemoveEvent."
                    , newMetaGroup
                    , evt.getSourceMetaContactGroup());

        assertEquals("Source provider for the remove event."
                     , fixture.mockProvider, evt.getSourceProvider());

        //check whether the group was indeed renamed.
        Iterator groupsIter = evt.getSourceMetaContactGroup()
            .getContactGroupsForProvider(fixture.mockProvider);

        assertTrue("A proto group was unexplicably removed after renaming.",
                   groupsIter.hasNext());

        assertEquals("The name of a protocol group after renaming."
                     , renamedGroupName
                     , ((MockContactGroup)groupsIter.next()).getGroupName());


        mclEvtCollector.collectedEvents.clear();

        //remove the group and check for the event.
        fixture.metaClService.addContactListListener(mclEvtCollector);
        opSetPersPresence.removeServerStoredContactGroup(newProtoGroup);

        fixture.metaClService.removeContactListListener(mclEvtCollector);

        //first check that the group was really removed
        assertEquals("Number of evts dispatched while removing a contact group"
                     , 1
                     , mclEvtCollector.collectedEvents.size());
        evt = (MetaContactGroupEvent)mclEvtCollector.collectedEvents.get(0);

        assertEquals("ID of the generated event",
                     MetaContactGroupEvent.CONTACT_GROUP_REMOVED_FROM_META_GROUP,
                     evt.getEventID());

        assertEquals("Source group for the RemoveEvent."
                    , newMetaGroup
                    , evt.getSourceMetaContactGroup());

        assertEquals("Source provider for the remove event."
                     , fixture.mockProvider, evt.getSourceProvider());

        mclEvtCollector.collectedEvents.clear();
    }

    /**
     * Perform manipulations of moving protocol contacts in and outside of a
     * meta contact and verify that they complete properly.
     */
    public void testAddMoveRemoveContactToMetaContact()
    {
        String newContactID = "TestyPesty";
        //get a ref to 2 contacts the we will experiment with.
        MetaContact metaContact = fixture.metaClService.getRoot()
                                                            .getMetaContact(0);
        MetaContact dstMetaContact = fixture.metaClService.getRoot()
                                                            .getMetaContact(1);

        MclEventCollector evtCollector = new MclEventCollector();
        fixture.metaClService.addContactListListener(evtCollector);

        //add a new mock contact to a meta contact
        fixture.metaClService.addNewContactToMetaContact(
            fixture.mockProvider
            , metaContact
            , newContactID);

        fixture.metaClService.removeContactListListener(evtCollector);

        //verify that the contact has been added to the meta contact.
        assertEquals("Dest. meta Contact did not seem to contain an "
                     +"extra proto contact."
                     , 2
                     , metaContact.getContactCount());

        MockContact newContact = (MockContact)metaContact
                                .getContact(newContactID, fixture.mockProvider);

        assertNotNull("newContact", newContact);

        //verify that a mock contact has been created in the mock contact list.
        //and that it is the same as the one added in the MetaContact
        assertSame("Proto specific contact in mock contact list."
                   , newContact
                   , opSetPersPresence.getServerStoredContactListRoot()
                            .getContact(newContactID));

        //verify that events have been properly delivered.
        assertTrue("No events delivered while adding a new contact to a "
                      +"meta contact", evtCollector.collectedEvents.size() == 1);

        MetaContactEvent event = (MetaContactEvent)evtCollector
            .collectedEvents.get(0);
        evtCollector.collectedEvents.clear();

        assertSame ( "Source contact in MetaContactEvent gen. upon add."
                     , metaContact , event.getSourceContact());

        assertSame ( "Source provider in MetaContactEvent gen. upon add."
                     , fixture.mockProvider, event.getSourceProvider());

        assertEquals ( "Event ID in MetaContactEvent gen. upon add."
                     , MetaContactEvent.PROTO_CONTACT_ADDED, event.getEventID());

        //move the mock contact to another meta contact
        fixture.metaClService.addContactListListener(evtCollector);

        fixture.metaClService.moveContact(newContact, dstMetaContact);

        fixture.metaClService.removeContactListListener(evtCollector);

        //verify that the old meta contact does not contain it anymore.
        assertEquals("Orig. Meta Contact did not seem restored after removing "
                     +"the newly added contact."
                     , 1
                     , metaContact.getContactCount());

        //verify that the new meta contact contains it.
        assertEquals("A Meta Contact did not seem updated after moving a "
                     +"contact inside it."
                     , 2
                     , dstMetaContact.getContactCount());

        newContact = (MockContact)dstMetaContact
                                .getContact(newContactID, fixture.mockProvider);

        assertNotNull("newContact", newContact);

        //verify that events have been properly delivered.
        assertTrue("No events delivered while adding a moving a proto contact. "
                   , evtCollector.collectedEvents.size() == 1);

        event = (MetaContactEvent) evtCollector.collectedEvents.get(0);
        evtCollector.collectedEvents.clear();

        assertSame("Source contact in MetaContactEvent gen. upon move."
                   , dstMetaContact, event.getSourceContact());

        assertSame("Source provider in MetaContactEvent gen. upon move."
                   , fixture.mockProvider, event.getSourceProvider());

        assertEquals("Event ID in MetaContactEvent gen. upon add."
                     , MetaContactEvent.PROTO_CONTACT_MOVED, event.getEventID());

        //remove the meta contact
        fixture.metaClService.addContactListListener(evtCollector);

        fixture.metaClService.removeContact(newContact);

        fixture.metaClService.removeContactListListener(evtCollector);

        //verify that it is no more in the meta contact
        assertEquals("Dest. Meta Contact did not seem restored after removing "
                     +"the newly added contact."
                     , 1
                     , dstMetaContact.getContactCount());

        //verify that it is no more in the mock contact list
        assertNull( "The MetaContactList did not remove a contact from the "
                    + "MockList on del."
                    , opSetPersPresence.getServerStoredContactListRoot()
                        .getContact(newContactID));

        //verify that events have been properly delivered.
        assertTrue("No events delivered while adding a new contact to a "
                      +"meta contact", evtCollector.collectedEvents.size() == 1);

        event = (MetaContactEvent)evtCollector
            .collectedEvents.get(0);
        evtCollector.collectedEvents.clear();

        assertSame ( "Source contact in MetaContactEvent gen. upon remove."
                     , dstMetaContact, event.getSourceContact());

        assertSame ( "Source provider in MetaContactEvent gen. upon remove."
                     , fixture.mockProvider, event.getSourceProvider());

        assertEquals ( "Event ID in MetaContactEvent gen. upon remove."
                       , MetaContactEvent.PROTO_CONTACT_REMOVED
                       , event.getEventID());


    }

    /**
     * Tests methods for creating moving and removing meta contacts.
     */
    public void testCreateMoveRemoveMetaContact()
    {
        String newContactID ="testCreateMoveRemoveMetaContact.ContactID";
        MetaContactGroup parentMetaGroup = fixture.metaClService.getRoot()
            .getMetaContactSubgroup(MetaContactListServiceLick.topLevelGroupName);

        //create a new metacontact and, hence mock contact, in the meta
        //"SomePeople" non-toplevel group
        fixture.metaClService.createMetaContact(fixture.mockProvider
            , parentMetaGroup
            , newContactID);

        //check that the contact has been successfully created in the meta cl
        MetaContact newMetaContact  =
            parentMetaGroup.getMetaContact(fixture.mockProvider, newContactID);

        assertNotNull("create failed. couldn't find the new contact."
            , newMetaContact);

        //check that the contact has been successfully created in the mock cl
        assertEquals("create() created a meta contact with the wrong name."
            , newContactID, newMetaContact.getDisplayName());

        //move the meta contact somewhere else
        fixture.metaClService.moveMetaContact(
            newMetaContact, fixture.metaClService.getRoot());

        //check that the meta contact has moved.
        assertNull(newMetaContact.getDisplayName()
               + " was still in its old location after moving it."
               ,parentMetaGroup.getMetaContact( newMetaContact.getMetaUID()));

        assertNotNull(newMetaContact.getDisplayName()
                   + " was not in the new location after moving it."
                   ,fixture.metaClService.getRoot()
                        .getMetaContact(newMetaContact.getMetaUID()));

        //check that the mock contact has moved as well.
        assertNull("The mock contact corresponding to: "
                   + newMetaContact.getDisplayName()
                   + " was still in its old location after its "
                   +"encapsulating meta contact was moved"
                   ,MetaContactListServiceLick.topLevelMockGroup
                        .getContact(newContactID));

        //assert that the mock contact has indeed moved to its new parent.
        assertNotNull("The mock contact corresponding to: "
                   + newMetaContact.getDisplayName()
                   + " was not moved to its new location after its "
                   +"encapsulating meta contact was."
                   ,opSetPersPresence.getServerStoredContactListRoot()
                        .getContact(newContactID));

        //remove the contact
        fixture.metaClService.removeMetaContact(newMetaContact);

        //check that the meta contact has been removed.
        assertNull(newMetaContact.getDisplayName()
               + " was still in its old location after it was removed."
               ,fixture.metaClService.getRoot().getMetaContact(
                   newMetaContact.getMetaUID()));


        //check that the mock contact has been removed.
        assertNull("The mock contact corresponding to: "
                   + newMetaContact.getDisplayName()
                   + " was not removed after its encapsulating meta contact was."
                   ,opSetPersPresence.getServerStoredContactListRoot()
                        .getContact(newContactID));

    }

    /**
     * Tests operations on meta groups.
     */
    public void testCreateRenameRemoveMetaContactGroup()
    {
        String newGroupName = "testCRRMetaContactGroup.NewContactGroup";
        String newContactID = "testCRRMetaContactGroup.NewContactID";

        //create a new meta contact group
        fixture.metaClService.createMetaContactGroup(
            fixture.metaClService.getRoot(), newGroupName);

        //check that the group exists in the meta contact list but not yet in
        //the mock provider
        MetaContactGroup newMetaGroup = fixture.metaClService.getRoot()
                .getMetaContactSubgroup(newGroupName);
        assertNotNull(
            "createMetaContactGroup failed - no group was created."
            , newMetaGroup);

        assertNull(
            "createMetaContactGroup tried to create a proto group too early."
            ,opSetPersPresence.getServerStoredContactListRoot()
                .getGroup(newGroupName));

        //create a mock contcat through the meta contact list.
        fixture.metaClService.createMetaContact(
            fixture.mockProvider, newMetaGroup, newContactID);

        //check that the mock group was created and added to the right meta grp.
        MockContactGroup newMockGroup = (MockContactGroup)opSetPersPresence
            .getServerStoredContactListRoot().getGroup(newGroupName);

        assertNotNull(
            "createMetaContact did not create a parent proto group "
            + "when it had to."
            , newMockGroup);
        assertSame(
            "createMetaContact created a proto group but did not add it to the "
            + "right meta contact group."
            , newMockGroup
            , newMetaGroup.getContactGroup(newGroupName, fixture.mockProvider));

        //check that the contact was added
        MetaContact newMetaContact = newMetaGroup
            .getMetaContact(fixture.mockProvider, newContactID);

        assertNotNull("createMetaContact failed", newMetaContact);

        //rename the meta contact group
        String renamedGroupName = "new" + newGroupName;
        fixture.metaClService.renameMetaContactGroup(newMetaGroup,
                                                     renamedGroupName);

        //check that the meta group changed its name.
        assertEquals ( "renameMetaContactGroup failed"
                       , newMetaGroup.getGroupName(), renamedGroupName);

        //check that the mock group did not change name
        assertEquals ( "renameMetaContactGroup renamed a proto group!"
                       , newMockGroup.getGroupName(), newGroupName);

        //remove the meta contact group
        fixture.metaClService.removeMetaContactGroup(newMetaGroup);

        //check that the meta group is removed
        assertNull(
            "removeMetaContactGroup failed - group not removed."
            , fixture.metaClService.getRoot()
                .getMetaContactSubgroup(newGroupName));


        //check that the mock group is removed
        assertNull(
            "removeMetaContact did not remove the corresp. proto group."
            , opSetPersPresence.getServerStoredContactListRoot()
                                                    .getGroup(newGroupName));
    }

    /**
     * Tests the MetaContactListService.findParentMetaContactGroup(MetaContact)
     * method for two different meta contacts.
     */
    public void testFindParentMetaContactGroup()
    {
        MetaContact metaContact1 = fixture.metaClService
            .findMetaContactByContact(MetaContactListServiceLick
                                      .subLevelContact);
        MetaContact metaContact2 = fixture.metaClService
            .findMetaContactByContact(MetaContactListServiceLick.subsubContact);

        //do testing for the first contact
        MetaContactGroup metaGroup = fixture.metaClService
            .findParentMetaContactGroup(metaContact1);

        assertNotNull("find failed for contact " + metaContact1, metaGroup);
        assertEquals("find failed (wrong group) for contact "
                     + metaContact1.getDisplayName()
                     , MetaContactListServiceLick.topLevelGroupName
                     , metaGroup.getGroupName());

        //do testing for the first contact
        metaGroup = fixture.metaClService.findParentMetaContactGroup(metaContact2);

        assertNotNull("find failed for contact " + metaContact2, metaGroup);
        assertEquals("find failed (wrong group) for contact "
                     + metaContact2.getDisplayName()
                     , MetaContactListServiceLick.subLevelGroup.getGroupName()
                     , metaGroup.getGroupName());
    }

    /**
     * Tests the MetaContactListService
     *             .findParentMetaContactGroup(MetaContactGroup)
     * method for two different meta contact groups.
     */
    public void testFindParentMetaContactGroup2()
    {
        MetaContactGroup metaContactGroup1 = fixture.metaClService
            .findMetaContactGroupByContactGroup(MetaContactListServiceLick
                                      .topLevelMockGroup);
        MetaContactGroup metaContactGroup2 = fixture.metaClService
            .findMetaContactGroupByContactGroup(MetaContactListServiceLick
                                      .subLevelGroup);

        //do testing for the first contact
        MetaContactGroup metaGroup = fixture.metaClService
            .findParentMetaContactGroup(metaContactGroup1);

        assertNotNull("find failed for contact " + metaContactGroup1, metaGroup);
        assertEquals("find failed (wrong group) for group "
                     + metaContactGroup1.getGroupName()
                     , fixture.metaClService.getRoot().getGroupName()
                     , metaGroup.getGroupName());

        //do testing for the first contact
        metaGroup = fixture.metaClService.findParentMetaContactGroup(metaContactGroup2);

        assertNotNull("find failed for contact " + metaContactGroup2, metaGroup);
        assertEquals("find failed (wrong group) for group "
                     + metaContactGroup2.getGroupName()
                     , MetaContactListServiceLick.topLevelGroupName
                     , metaGroup.getGroupName());
    }


    private class MclEventCollector implements MetaContactListListener
    {
        public Vector collectedEvents = new Vector();
        /**
         * Indicates that a MetaContact has been successfully added
         * to the MetaContact list.
         * @param evt the MetaContactListEvent containing the corresponding contact
         */
        public void metaContactAdded(MetaContactEvent evt)
        {
            collectedEvents.add(evt);
        }

        /**
         * Indicates that a MetaContactGroup has been successfully added
         * to the MetaContact list.
         * @param evt the MetaContactListEvent containing the corresponding
         * contact
         */
        public void metaContactGroupAdded(MetaContactGroupEvent evt)
        {
            collectedEvents.add(evt);
        }

        /**
         * Indicates that a MetaContactGroup has been removed from the
         * MetaContact list.
         * @param evt the MetaContactListEvent containing the corresponding
         * contact
         */
        public void metaContactGroupRemoved(MetaContactGroupEvent evt)
        {
            collectedEvents.add(evt);
        }

        /**
         * Indicates that a MetaContact has been removed from the MetaContact
         * list.
         * @param evt the MetaContactListEvent containing the corresponding
         * contact
         */
        public void metaContactRemoved(MetaContactEvent evt)
        {
            collectedEvents.add(evt);
        }

        /**
         * Indicates that a MetaContact has been modified.
         * @param evt the MetaContactListEvent containing the corresponding
         * contact
         */
        public void metaContactModified(MetaContactEvent evt)
        {
            collectedEvents.add(evt);
        }

        /**
         * Indicates that a MetaContactGroup has been modified (e.g. a proto
         * contact group was removed).
         *
         * @param evt the MetaContactListEvent containing the corresponding
         * contact
         */
        public void metaContactGroupModified(MetaContactGroupEvent evt)
        {
            collectedEvents.add(evt);
        }

    }

}
