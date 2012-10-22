package com.onsip.communicator.impl.applet;

import java.io.IOException;

import net.java.sip.communicator.util.Logger;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;

import com.onsip.communicator.impl.applet.call.CallManager;
import com.onsip.communicator.impl.applet.call.volume.InputVolumeControl;
import com.onsip.communicator.impl.applet.call.volume.OutputVolumeControl;
import com.onsip.communicator.impl.applet.login.RegistrationManager;
import com.onsip.communicator.impl.applet.utils.NotificationManager;

import org.jitsi.service.audionotifier.AudioNotifierService;
import org.jitsi.service.configuration.ConfigurationService;
import org.jitsi.service.libjitsi.LibJitsi;
import org.jitsi.service.neomedia.MediaService;
import org.jitsi.service.neomedia.MediaType;
import org.jitsi.service.neomedia.MediaUseCase;
import org.jitsi.service.neomedia.device.MediaDevice;
import net.java.sip.communicator.service.notification.NotificationService;
import net.java.sip.communicator.service.protocol.ProtocolNames;
import net.java.sip.communicator.service.protocol.ProtocolProviderFactory;
import net.java.sip.communicator.service.protocol.ProtocolProviderService;
import org.jitsi.service.resources.ResourceManagementService;
import net.java.sip.communicator.util.ServiceUtils;

/**
 * This class implements a simple bundle that utilizes the OSGi framework's
 * event mechanism to listen for service events. Upon receiving a service event,
 * it prints out the event's details.
 **/
public class AppletActivator
    implements BundleActivator
{
    private final static Logger logger
        = Logger.getLogger(AppletActivator.class);


    private static BundleContext m_context = null;

    private static NotificationService m_notificationService = null;

    private static ResourceManagementService m_resourcesService = null;

    private static AudioNotifierService m_audioNotifierService = null;

    private static MediaService mediaService = null;

    private static ConfigurationService configurationService = null;

    private static InputVolumeControl inputVolumeControl = null;
    private static OutputVolumeControl outputVolumeControl = null;


    /**
     * Callback objects. These objects call a 'fireEvent'
     * function that exists in the eventing API outside of
     * Felix, and inside of the applet where Felix was launched
     * from.  The listeners, too, are registered
     * inside the eventing API. These objects
     * take a String Array as arguments. Those arguments
     * are then parsed and sent to the browser
     */
    protected Object callEventSource = null;
    protected Object callPeerEventSource = null;
    protected Object registrationEventSource = null;

    protected CallManager callManager = null;
    protected RegistrationManager registrationManager = null;

    public void start(BundleContext context)
    {
        m_context = context;
        m_context.registerService(AppletActivator.class.getName(), this, null);

        NotificationManager.registerGuiNotifications();

        callManager = new CallManager();
        registrationManager = new RegistrationManager(callManager);
    }

    /**
     * Implements BundleActivator.stop(). Prints a message and removes itself
     * from the bundle context as a service listener.
     *
     * @param context the framework context for the bundle.
     **/
    public void stop(BundleContext context)
    {
        if (registrationManager != null)
        {
            registrationManager.removeListeners();
        }
        if (callManager != null)
        {
            callManager.removeListeners();
        }
    }

    public static BundleContext getBundleContext()
    {
        return m_context;
    }

    public static ConfigurationService getConfigurationService()
    {
        if (configurationService == null)
        {
            configurationService
                = ServiceUtils.getService(
                        m_context,
                        ConfigurationService.class);
        }
        return configurationService;
    }

    /**
     * Returns an instance of the <tt>MediaService</tt> obtained from the
     * bundle context.
     * @return an instance of the <tt>MediaService</tt> obtained from the
     * bundle context
     */
    public static MediaService getMediaService()
    {
        if (mediaService == null)
        {
            mediaService
                = ServiceUtils.getService(m_context, MediaService.class);

        }
        return mediaService;
    }

    public synchronized static ResourceManagementService getResources()
    {
        if (m_resourcesService == null)
        {
            ServiceReference serviceReference =
                m_context.getServiceReference(ResourceManagementService.class
                    .getName());
            m_resourcesService =
                (ResourceManagementService) m_context.getService(serviceReference);
        }
        return m_resourcesService;
    }

    public static synchronized AudioNotifierService getAudioNotifier()
    {
        if (m_audioNotifierService == null)
        {
            m_audioNotifierService
                = ServiceUtils.getService(
                    m_context,
                        AudioNotifierService.class);
        }
        return m_audioNotifierService;
    }

    /**
     * Returns the <tt>NotificationService</tt> obtained from the bundle context.
     *
     * @return the <tt>NotificationService</tt> obtained from the bundle context
     */
    public synchronized static NotificationService getNotificationService()
    {
        if (m_notificationService == null)
        {
            m_notificationService
                = ServiceUtils.getService(
                    m_context,
                        NotificationService.class);
        }
        return m_notificationService;
    }

    public static ServiceReference[] getServiceReferences()
    {
        ServiceReference[] sipProviderRefs = null;
        BundleContext context = AppletActivator.getBundleContext();

        try
        {
            if (context != null)
            {
                sipProviderRefs =
                    context.getServiceReferences(
                        ProtocolProviderService.class.getName(), "(&" + "("
                            + ProtocolProviderFactory.PROTOCOL + "="
                                + ProtocolNames.SIP + "))");

                if (sipProviderRefs != null && sipProviderRefs.length > 0)
                {
                    logger.info("Number of AORs found = " +
                        sipProviderRefs.length);
                    return sipProviderRefs;
                }
            }
        }
        catch (InvalidSyntaxException e)
        {
            logger.error("InvalidSyntaxException :: getServiceReferences : ");
            logger.error(e.getMessage(), e);
        }
        return new ServiceReference[0];
    }

    public static ProtocolProviderService
        getPrototocolProviderService(String userId)
    {
        ProtocolProviderService provider = null;
        ServiceReference[] sipProviderRefs = null;
        try
        {
            logger.debug("Get Protocol Service Provider SIP for user " +
                userId);
            if (m_context != null && userId != null)
            {
                sipProviderRefs =
                    m_context.getServiceReferences(
                        ProtocolProviderService.class.getName(), "(&" + "(" +
                            ProtocolProviderFactory.PROTOCOL + "=" +
                                ProtocolNames.SIP + ")" + "(" +
                                    ProtocolProviderFactory.USER_ID + "=" +
                                        userId + "))");

                if (sipProviderRefs != null && sipProviderRefs.length > 0)
                {
                    return (ProtocolProviderService)
                        m_context.getService(sipProviderRefs[0]);
                }
            }
        }
        catch (InvalidSyntaxException e)
        {
            logger.error("InvalidSyntaxException :: " +
                "getPrototocolProviderService : ");
            logger.error(e, e);
        }

        return provider;
    }

    // callback objects
    public void setRegistrationEventSource(Object registrationEventSource)
    {
        this.registrationEventSource = registrationEventSource;
        registrationManager.setRegistrationEventSource(registrationEventSource);
    }

    public void setCallEventSource(Object callEventSource)
    {
        this.callEventSource = callEventSource;
        callManager.setCallEventSource(callEventSource);
    }

    public void setCallPeerEventSource(Object callPeerSource)
    {
        this.callPeerEventSource = callPeerSource;
        callManager.setCallPeerEventSource(callPeerSource);
    }
    // end callback objects

    // exported events
    public void call(String userId, String sip)
    {
        callManager.call(userId, sip, null);
    }

    public void call(String userId, String sip, String setupId)
    {
        callManager.call(userId, sip, setupId);
    }

    public void inviteCalleeToCall(String callId, String sipUri)
    {
        callManager.inviteCalleeToCall(callId, sipUri);
    }

    public void transfer(String callId, String peerId, String targetUri)
    {
        callManager.transfer(callId, peerId, targetUri,
            CallManager.TransferType.SEAMLESS);
    }

    public void transfer(String callId, String peerId,
        String targetUri, int typeOfTransfer)
    {
        CallManager.TransferType transferType = CallManager.TransferType.SEAMLESS;
        if (typeOfTransfer == 1)
        {
            transferType = CallManager.TransferType.BLIND;
        }
        else if (typeOfTransfer == 2)
        {
            transferType = CallManager.TransferType.ATTENDED;
        }

        callManager.transfer(callId, peerId, targetUri, transferType);
    }

    public void mute(String callId, Boolean m)
    {
        callManager.mute(callId, m.booleanValue());
    }

    public void mute(Boolean m)
    {
        callManager.mute(null, m.booleanValue());
    }

    public void unregister(String userId)
    {
        registrationManager.unregister(userId);
    }

    public void register(String userId, String displayName,
            String authUsername, String password)
    {
        registrationManager.register(userId, displayName, authUsername,
            password, null, null, null);
    }

    public void register(String userId, String displayName,
            String authUsername, String password,
            String serverAddress, String proxyAddress, String proxyPort)
    {
        registrationManager.register(userId, displayName, authUsername,
            password, serverAddress, proxyAddress, proxyPort);
    }

    public void hangUp(String callId)
    {
        callManager.hangUp(callId, null);
    }

    public void hangUp(String callId, String peerId)
    {
        callManager.hangUp(callId, peerId);
    }

    public void pickupCall()
    {
        callManager.pickupCall(null);
    }

    public void pickupCall(String callId)
    {
        callManager.pickupCall(callId);
    }

    /**
     * Put all active calls on hold
     *
     * @param hold on/off hold
     */
    public void hold(Boolean hold)
    {
        callManager.hold(null, null, hold.booleanValue());
    }

    /**
     * Put a specific call on hold
     *
     * @param callId call id
     * @param hold on/off hold
     */
    public void hold(String callId, Boolean hold)
    {
        callManager.hold(callId, null, hold.booleanValue());
    }

    /**
     * Put a specific all peer on hold
     *
     * @param callId
     * @param peerId
     * @param hold
     */
    public void hold(String callId, String peerId, Boolean hold)
    {
        callManager.hold(callId, peerId, hold.booleanValue());
    }

    /** exported : DTMF support **/
    public void dispatchKeyEvent(char key)
    {
        callManager.dispatchKeyEvent(null, key);
    }

    public void dispatchKeyEvent(String callId, char key, Boolean start)
    {
        callManager.dispatchKeyEvent(callId, key, start.booleanValue());
    }

    public void dispatchKeyEvent(String callId, char key)
    {
        callManager.dispatchKeyEvent(callId, key);
    }

    public void setInputVolume(Integer level)
    {
        if (inputVolumeControl == null)
        {
            inputVolumeControl =
                new InputVolumeControl(getMediaService());
        }
        inputVolumeControl.setVolume(level);
    }

    public void setOutputVolume(Integer level)
    {
        if (outputVolumeControl == null)
        {
            outputVolumeControl =
                new OutputVolumeControl(getMediaService());
        }
        outputVolumeControl.setVolume(level);
    }

    public static int getInputVolume()
    {
        if (inputVolumeControl == null)
        {
            inputVolumeControl =
                new InputVolumeControl(getMediaService());
        }
        return inputVolumeControl.getLevel();
    }

    public static int getOutputVolume()
    {
        if (outputVolumeControl == null)
        {
            outputVolumeControl =
                new OutputVolumeControl(getMediaService());
        }
        return outputVolumeControl.getLevel();
    }

    public String getDefaultAudioDevice()
    {
        ServiceReference ref =
            m_context.getServiceReference(MediaService.class.getName());
        MediaService ms = (MediaService) m_context.getService(ref);

        MediaDevice m = ms.getDefaultDevice
            (MediaType.AUDIO, MediaUseCase.CALL);

        if (m != null)
        {
            logger.info("Media device AUDIO supports "
                + m.getSupportedFormats().size() + " formats");
            return m.toString();
        }
        return "";
    }
}
