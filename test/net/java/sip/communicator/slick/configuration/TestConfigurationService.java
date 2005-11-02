/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.slick.configuration;

import junit.framework.*;
import net.java.sip.communicator.service.configuration.event.*;
import net.java.sip.communicator.service.configuration.*;
import org.osgi.framework.*;
import net.java.sip.communicator.impl.configuration.xml.*;
import net.java.sip.communicator.util.xml.*;
import net.java.sip.communicator.slick.slickless.*;

/**
 * Tests basic ConfiguratioService behaviour.
 *
 * @author Emil Ivov
 */
public class TestConfigurationService extends TestCase
{
    /**
     * The ConfigurationService that we will be testing.
     */
    private ConfigurationService configurationService = null;

    /**
     * The PropertyChangeEvent that our test listeners will capture for testing.
     * Make sure we null that upon tear down
     */
    private PropertyChangeEvent propertyChangeEvent = null;

    /**
     * The name of a property that we will be using for testing.
     */
    private String propertyName = new String("my.test.property");

    /**
     * The name of a property that we will be using for testing custom event
     * notification.
     */
    private String listenedPropertyName = new String("a.property.i.listen.to");

    /**
     * The value of the property with name propertyName.
     */
    private Integer propertyValue = new Integer(19200);

    /**
     * A new value for the property with name propertyName
     */
    private Integer propertyNewValue = new Integer(19201);

    /**
     * A PropertyChange listener impl that registers the last received event.
     */
    private PropertyChangeListener pListener = new PropertyChangeListener()
    {
        public void propertyChange(PropertyChangeEvent event)
        {
            propertyChangeEvent = event;
        }
    };

    /**
     * A straightforward Vetoable change listener that throws an exception
     * upon any change
     */
    VetoableChangeListener rudeVetoListener = new VetoableChangeListener()
    {
        public void vetoableChange(PropertyChangeEvent event) throws
            PropertyVetoException
        {
            throw new PropertyVetoException("Just for the fun of it", event);
        }
    };

    /**
     * A straightforward implementation of a vetoable change listener that that
     * does not throw a veto exception and only stored the last received event.
     */
    VetoableChangeListener gentleVetoListener = new VetoableChangeListener()
    {
        public void vetoableChange(PropertyChangeEvent event) throws
            PropertyVetoException
        {
            propertyChangeEvent = event;
        }
    };


    /**
     * Generic JUnit Constructor.
     * @param name the name of the test
     */
    public TestConfigurationService(String name)
    {
        super(name);
        BundleContext context = ConfigurationServiceLick.bc;
        ServiceReference ref = context.getServiceReference(
            ConfigurationService.class.getName());
        configurationService = (ConfigurationService)context.getService(ref);
    }

    /**
     * Generic JUnit setUp method.
     * @throws Exception if anything goes wrong.
     */
    protected void setUp() throws Exception
    {
        configurationService.setProperty(propertyName, null);
        configurationService.setProperty(listenedPropertyName, null);
        super.setUp();
    }

    /**
     * Generic JUnit tearDown method.
     * @throws Exception if anything goes wrong.
     */
    protected void tearDown() throws Exception
    {
        //first remove any remaining listeners
        configurationService.removePropertyChangeListener(pListener);
        configurationService.removeVetoableChangeListener(rudeVetoListener);
        configurationService.removeVetoableChangeListener(gentleVetoListener);

        //clear used properties.
        configurationService.setProperty(propertyName, null);
        configurationService.setProperty(listenedPropertyName, null);
        propertyChangeEvent = null;

        super.tearDown();
    }

    /**
     * Tests whether setting and getting properties works correctly.
     * @throws PropertyVetoException in case someone wrongfully vetoes the
     * property change.
     */
    public void testSetGetProperty() throws PropertyVetoException
    {

        String propertyName = "my.test.property";
        Object property = new String("my.test.property's value");
        configurationService.setProperty(propertyName, property);

        Object actualReturn = configurationService.getProperty(propertyName);
        assertEquals("a property was not properly stored",
                     property, actualReturn);
    }

    /**
     * Tests whether setting and getting works alright for properties declared
     * as system and whether resolving and retrieving from the system property
     * set are done right.
     * @throws PropertyVetoException in case someone wrongfully vetoes the
     * property change.
     */
    public void testSystemPoperties() throws PropertyVetoException
    {
        //first simply store and retrieve a system property
        String propertyName = "my.test.system.property";
        Object property = new String("sys.value.1");
        configurationService.setProperty(propertyName, property, true);

        Object actualReturn = configurationService.getProperty(propertyName);
        assertEquals("a sys property was not properly stored",
                     property, actualReturn);

        //now check whether you can also retrieve it from the sys property set.
        actualReturn = System.getProperty(propertyName);
        assertEquals("a property was not properly stored", property, actualReturn);

        //verify that modifying it in the sys property set would affect the
        //value in the configuration service
        property = new String("second.sys.value");
        System.setProperty(propertyName, property.toString());
        actualReturn = configurationService.getProperty(propertyName);
        assertEquals("a property was not properly stored", property, actualReturn);

        //now make sure  that modifying it in the configurationService would
        //result in the corresponding change in the system property set.
        property = new String("third.sys.value");
        configurationService.setProperty(propertyName, property.toString());
        actualReturn = System.getProperty(propertyName);
        assertEquals("a property was not properly stored", property, actualReturn);
    }

    /**
     * Tests whether getString properly returns string values and that it
     * correctly handles corner cases.
     */
    public void testGetString() throws PropertyVetoException
    {
        //test a basic  scenario
        String propertyName = "my.test.property";
        Object property = new String("my.test.property's value");
        configurationService.setProperty(propertyName, property);

        String actualReturn = configurationService.getString(propertyName);
        assertEquals("getString failed to retrieve a property",
                     property.toString(), actualReturn);

        //verify that setting a non string object would not mess things up
        property = new Integer(7121979);
        configurationService.setProperty(propertyName, property);

        actualReturn = configurationService.getString(propertyName);
        assertEquals("getString failed to retrieve a property",
                     property.toString(), actualReturn);

        //verify that setting a whitespace only string would return null
        property = new String("\t\n ");
        configurationService.setProperty(propertyName, property);

        actualReturn = configurationService.getString(propertyName);
        assertNull("getString did not trim a white space only string",
                     actualReturn);
    }

    /**
     * Tests event notification through multicast listeners (those that are
     * registered for the whole configuration and not a single property only).
     */
    public void testMulticastEventNotification()
    {
        propertyChangeEvent = null;

        configurationService.addPropertyChangeListener(pListener);

        // test the initial set of a property.
        try
        {
            configurationService.setProperty(propertyName, propertyValue);
        }
        catch (PropertyVetoException ex)
        {
            fail("A PropertyVetoException came from nowhere. Exc="
                 + ex.getMessage());
        }

        assertNotNull( "No PropertyChangeEvent was delivered upon setProperty",
                       propertyChangeEvent);

        assertNull("oldValue must be null", propertyChangeEvent.getOldValue());
        assertEquals( "newValue is not the value we just set!",
                      propertyValue,
                      propertyChangeEvent.getNewValue()
                      );
        assertEquals( "propertyName is not the value we just set!",
                      propertyName,
                      propertyChangeEvent.getPropertyName()
                      );

        //test setting a new value;
        propertyChangeEvent = null;
        try
        {
            configurationService.setProperty(propertyName, propertyNewValue);
        }
        catch (PropertyVetoException ex)
        {
            fail("A PropertyVetoException came from nowhere. Exc="
                 + ex.getMessage());
        }

        assertNotNull( "No PropertyChangeEvent was delivered upon setProperty",
                       propertyChangeEvent);

        assertEquals("incorrect oldValue",
                     propertyValue,
                     propertyChangeEvent.getOldValue());
        assertEquals( "newValue is not the value we just set!",
                      propertyNewValue,
                      propertyChangeEvent.getNewValue());


        //test remove
        propertyChangeEvent = null;
        configurationService.removePropertyChangeListener(pListener);

        try
        {
            configurationService.setProperty(propertyName, propertyValue);
        }
        catch (PropertyVetoException ex)
        {
            fail("A PropertyVetoException came from nowhere. Exc="
                 + ex.getMessage());
        }

        assertNull( "A PropertyChangeEvent after unregistering a listener.",
                       propertyChangeEvent);
    }

    /**
     * Test event dispatch to vetoable listeners registered for the whole
     * configuration.
     */
    public void testMulticastEventNotificationToVetoableListeners()
    {
        Integer propertyValue = new Integer(19200);
        Integer propertyNewValue = new Integer(19201);
        propertyChangeEvent = null;

        configurationService.addVetoableChangeListener(gentleVetoListener);

        // test the initial set of a property.
        try
        {
            configurationService.setProperty(propertyName, propertyValue);
        }
        catch (PropertyVetoException ex)
        {
            fail("A PropertyVetoException came from nowhere. Exc="
                 + ex.getMessage());
        }

        assertNotNull( "No PropertyChangeEvent was delivered "
                       +"to VetoableListeners upon setProperty",
                       propertyChangeEvent);

        assertNull("oldValue must be null", propertyChangeEvent.getOldValue());
        assertEquals( "newValue is not the value we just set!",
                      propertyValue,
                      propertyChangeEvent.getNewValue());
        assertEquals( "propertyName is not the value we just set!",
                      propertyName,
                      propertyChangeEvent.getPropertyName());

        //test setting a new value;
        propertyChangeEvent = null;
        try
        {
            configurationService.setProperty(propertyName, propertyNewValue);
        }
        catch (PropertyVetoException ex)
        {
            fail("A PropertyVetoException came from nowhere. Exc="
                 + ex.getMessage());
        }

        assertNotNull( "No PropertyChangeEvent was delivered to veto listener "
                       +"upon setProperty",
                       propertyChangeEvent);

        assertEquals("incorrect oldValue",
                     propertyValue,
                     propertyChangeEvent.getOldValue());
        assertEquals( "newValue is not the value we just set!",
                      propertyNewValue,
                      propertyChangeEvent.getNewValue());


        //test remove
        propertyChangeEvent = null;
        configurationService.removeVetoableChangeListener(gentleVetoListener);

        try
        {
            configurationService.setProperty(propertyName, propertyValue);
        }
        catch (PropertyVetoException ex)
        {
            fail("A PropertyVetoException came from nowhere. Exc="
                 + ex.getMessage());
        }

        assertNull( "A PropertyChangeEvent after unregistering a listener.",
                       propertyChangeEvent);
    }

    /**
     * Test whether vetoing changes works as it is supposed to.
     */
    public void testVetos()
    {
        propertyChangeEvent = null;

        configurationService.addVetoableChangeListener(rudeVetoListener);
        configurationService.addPropertyChangeListener(pListener);

        PropertyVetoException exception = null;
        try
        {
            configurationService.setProperty(propertyName, propertyValue);
        }
        catch (PropertyVetoException ex)
        {
            exception = ex;
        }

        //make sure the exception was thrown
        assertNotNull("A vetoable change event was not dispatched or an "
                      +"exception was not let through.",
                      exception);

        //make sure no further event dispatching was done
        assertNull("A property change event was delivered even after "+
                   "the property change was vetoed.",
                   propertyChangeEvent);

        //make sure the property did not get modified after vetoing the change
        assertNull( "A property was changed even avfter vetoing the change."
                    ,configurationService.getProperty(propertyName));

        // now let's make sure that we have the right order of event dispatching.
        propertyChangeEvent = null;
        configurationService.removeVetoableChangeListener(rudeVetoListener);

        VetoableChangeListener vcListener = new VetoableChangeListener(){
            public void vetoableChange(PropertyChangeEvent event)
            {
                assertNull("propertyChangeEvent was not null which means that it has "
                    +"bean delivered to the propertyChangeListener prior to "
                    +"being delivered to the vetoable change listener.",
                    propertyChangeEvent);
            }
        };

        try
        {
            configurationService.setProperty(propertyName, propertyNewValue);
        }
        catch (PropertyVetoException ex1)
        {
            ex1.printStackTrace();
            fail("unexpected veto exception. message:" + ex1.getMessage());
        }
        configurationService.removeVetoableChangeListener(vcListener);
    }

    /**
     * Make sure that adding listeners for a single property name
     * only gets us events for that listeners. Removing a listener for a
     * specific property should also be proved to no obstruct event delivery to
     * the same listener had it been registered for other properties.
     */
    public void testSinglePropertyEventNotification()
        throws PropertyVetoException
    {
        Double listenedPropertyValue = new Double(19.2598);
        Double listenedPropertyNewValue = new Double(19.29581);

        //test basic selective event dispatch
        configurationService.addPropertyChangeListener(
            listenedPropertyName, pListener);

        propertyChangeEvent = null;

        configurationService.setProperty(
            propertyName, propertyValue);

        assertNull("setting prop:"+propertyName + " caused an event notif. to "+
                   "listener registered for prop:" + listenedPropertyName,
                    propertyChangeEvent);

        configurationService.setProperty(
            listenedPropertyName, listenedPropertyValue);

        assertNotNull("No event was dispatched upon modification of prop:"
                      +listenedPropertyName,
                      propertyChangeEvent );

        assertNull("oldValue must be null", propertyChangeEvent.getOldValue());
        assertEquals("wrong newValue",
                     listenedPropertyValue,
                     propertyChangeEvent.getNewValue());

        //test that a generic remove only removes the generic listener
        propertyChangeEvent = null;

        configurationService.removePropertyChangeListener(pListener);

        configurationService.setProperty(
            listenedPropertyName, listenedPropertyNewValue);

        assertNotNull("No event was dispatched upon modification of prop:"
                      +listenedPropertyName
                      + ". The listener was wrongfully removed.",
                      propertyChangeEvent );

        assertEquals("wrong oldValue",
                     listenedPropertyValue,
                     propertyChangeEvent.getOldValue());
        assertEquals("wrong newValue",
                     listenedPropertyNewValue,
                     propertyChangeEvent.getNewValue());

        //make sure that removing the listener properly - really removes it.
        propertyChangeEvent = null;

        configurationService.removePropertyChangeListener(
            listenedPropertyName, pListener);

        configurationService.setProperty(listenedPropertyName, propertyValue);

        assertNull(
            "An event was wrongfully dispatched after removing a listener",
            propertyChangeEvent
        );

    }

    /**
     * Make sure that adding vetoable listeners for a single property name
     * only gets us events for that listeners. Removing a listener for a
     * specific property should also be proved to no obstruct event delivery to
     * the same listener had it been registered for other properties.
     */
    public void testSinglePropertyVetoEventNotification()
        throws PropertyVetoException
    {
        Double listenedPropertyValue = new Double(19.2598);
        Double listenedPropertyNewValue = new Double(19.29581);
        VetoableChangeListener vetoListener = new VetoableChangeListener()
        {
            public void vetoableChange(PropertyChangeEvent event)
            {
                propertyChangeEvent = event;
            }

        };

        //test basic selective event dispatch
        configurationService.addVetoableChangeListener(
            listenedPropertyName, vetoListener);

        propertyChangeEvent = null;

        configurationService.setProperty(
            propertyName, propertyValue);

        assertNull("setting prop:" + propertyName + " caused an event notif. to " +
                   "listener registered for prop:" + listenedPropertyName,
                    propertyChangeEvent);

        configurationService.setProperty(
            listenedPropertyName, listenedPropertyValue);

        assertNotNull("No event was dispatched upon modification of prop:"
                      + listenedPropertyName,
                      propertyChangeEvent);

        assertNull("oldValue must be null", propertyChangeEvent.getOldValue());
        assertEquals("wrong newValue",
                     listenedPropertyValue,
                     propertyChangeEvent.getNewValue());

        //test that a generic remove only removes the generic listener
        propertyChangeEvent = null;

        configurationService.removeVetoableChangeListener(vetoListener);

        configurationService.setProperty(
            listenedPropertyName, listenedPropertyNewValue);

        assertNotNull("No event was dispatched upon modification of prop:"
                      + listenedPropertyName
                      + ". The listener was wrongfully removed.",
                      propertyChangeEvent);

        assertEquals("wrong oldValue",
                     listenedPropertyValue,
                     propertyChangeEvent.getOldValue());
        assertEquals("wrong newValue",
                     listenedPropertyNewValue,
                     propertyChangeEvent.getNewValue());

        //make sure that removing the listener properly - really removes it.
        propertyChangeEvent = null;

        configurationService.removeVetoableChangeListener(
            listenedPropertyName, vetoListener);

        configurationService.setProperty(
            listenedPropertyName, listenedPropertyValue);

        assertNull(
            "An event was wrongfully dispatched after removing a listener",
            propertyChangeEvent
        );

        //make sure that adding a generic listener, then adding a custom prop
        //listener, then removing it - would not remove the generic listener.
        propertyChangeEvent = null;

        configurationService.addVetoableChangeListener(vetoListener);
        configurationService.addVetoableChangeListener(
            listenedPropertyName, vetoListener);
        configurationService.removeVetoableChangeListener(
            listenedPropertyName, vetoListener);

        configurationService.setProperty(
            listenedPropertyName, listenedPropertyNewValue);

        assertNotNull("No event was dispatched upon modification of prop:"
                      + listenedPropertyName
                      + ". The global listener was wrongfully removed.",
                      propertyChangeEvent);

        assertEquals("wrong propertyName",
                     listenedPropertyName,
                     propertyChangeEvent.getPropertyName());
        assertEquals("wrong oldValue",
                     listenedPropertyValue,
                     propertyChangeEvent.getOldValue());
        assertEquals("wrong newValue",
                     listenedPropertyNewValue,
                     propertyChangeEvent.getNewValue());

        //fail("Testing failures! "
        //+"Wanted to know whether cruisecontrol will notice the falure. emil.");
    }
}
