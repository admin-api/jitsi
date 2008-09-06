/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.argdelegation;

import java.util.*;

import org.osgi.framework.*;

import net.java.sip.communicator.service.argdelegation.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.version.*;
import net.java.sip.communicator.util.*;
import net.java.sip.communicator.util.launchutils.*;


/**
 * Implements the <tt>UriDelegationPeer</tt> interface from our argument handler
 * utility. We use this handler to relay arguments to URI handlers that have
 * been registered from other services such as the SIP provider for example.
 *
 * @author Emil Ivov
 */
public class UriDelegationPeerImpl
    implements UriDelegationPeer, ServiceListener
{
    private static final Logger logger =
        Logger.getLogger(UriDelegationPeerImpl.class);

    /**
     * The list of uriHandlers that we are currently aware of.
     */
    private Map<String, UriHandler> uriHandlers
        = new Hashtable<String, UriHandler>();

    /**
     * Creates an instance of this peer and scans <tt>bundleContext</tt> for all
     * existing <tt>UriHandler</tt>s
     *
     * @param bundleContext a reference to a currently valid instance of a
     * bundle context.
     */
    public UriDelegationPeerImpl(BundleContext bundleContext)
    {
        ServiceReference[] uriHandlerRefs = null;

        synchronized (uriHandlers)
        {
            try
            {
                uriHandlerRefs = bundleContext.getServiceReferences(
                                UriHandler.class.getName(), null);
            }
            catch (InvalidSyntaxException exc)
            {
                // this shouldn't happen because we aren't using a filter
                // but let's log just the same.
                logger.info("An error occurred while retrieving UriHandlers",
                                exc);
                return;
            }

            if(uriHandlerRefs == null)
            {
                //none URI handlers are registered at this point. Some might
                //come later.
                return;
            }

            for (ServiceReference uriHandlerRef : uriHandlerRefs)
            {
                UriHandler uriHandler = (UriHandler) bundleContext
                                .getService(uriHandlerRef);
                uriHandlers.put(uriHandler.getProtocol(), uriHandler);
            }
        }
    }

    /**
     * Listens for <tt>UriHandlers</tt> that are registered in the bundle
     * context after we had started so that we could add them to the list
     * of currently known handlers.
     *
     * @param event the event containing the newly (un)registered service.
     */
    public void serviceChanged(ServiceEvent event)
    {
        synchronized (uriHandlers)
        {
            BundleContext bc = event.getServiceReference().getBundle()
                            .getBundleContext();

            Object service = bc.getService(event.getServiceReference());

            //we are only interested in UriHandler-s
            if(!(service instanceof UriHandler) )
            {
                return;
            }

            if (event.getType() == ServiceEvent.MODIFIED
                            || event.getType() == ServiceEvent.REGISTERED)
            {
                UriHandler uriHandler = (UriHandler) bc.getService(event
                                .getServiceReference());

                uriHandlers.put(uriHandler.getProtocol(), uriHandler);
            }
            else if (event.getType() == ServiceEvent.UNREGISTERING)
            {
                UriHandler uriHandler = (UriHandler) bc.getService(event
                                .getServiceReference());

                if(uriHandlers.get(uriHandler.getProtocol()) == uriHandler)
                    uriHandlers.remove(uriHandler.getProtocol());
            }
        }
    }

    /**
     * Relays <tt>uirArg</tt> to the corresponding handler or shows an error
     * message in case no handler has been registered for the corresponding
     * protocol.
     *
     * @param uriArg the uri that we've been passed and that we'd like to
     * delegate to the corresponding provider.
     */
    public void handleUri(String uriArg)
    {
        logger.trace("Handling URI: " + uriArg);
        //first parse the uri and determine the scheme/protocol
        //the parsing is currently a bit oversimplified so we'd probably need
        //to revisit it at some point.
        int colonIndex = uriArg.indexOf(":");

        if( colonIndex == -1)
        {
            //no scheme, we don't know how to handle the URI
            ArgDelegationActivator.getUIService().getPopupDialog()
                .showMessagePopupDialog(
                        "Could not determine how to handle: " + uriArg
                        + ".\nNo protocol scheme found.",
                        "Error handling URI",
                        PopupDialog.ERROR_MESSAGE);
            return;
        }

        String scheme = uriArg.substring(0, colonIndex);

        UriHandler handler = uriHandlers.get(scheme);

        //if handler is null we need to tell the user.
        if(handler == null)
        {
            logger.trace("Couldn't open " + uriArg
                         + "No handler found for protocol"+ scheme);
            ArgDelegationActivator.getUIService().getPopupDialog()
                .showMessagePopupDialog(
                     "\"" + scheme + "\" URIs are currently not supported.",
                     "Error handling URI",
                     PopupDialog.ERROR_MESSAGE);
            return;
        }

        //we're all set. let's do the handling now.
        try
        {
            handler.handleUri(uriArg);
        }
        //catch every possible exception
        catch(Throwable thr)
        {
            ArgDelegationActivator.getUIService().getPopupDialog()
                .showMessagePopupDialog(
                    "Error handling " + uriArg,
                 "Error handling URI",
                 PopupDialog.ERROR_MESSAGE);
            logger.error("Failed to handle \""+ uriArg +"\"", thr);
        }
    }


}
