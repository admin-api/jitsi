/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package com.onsip.communicator.impl.applet;

import net.java.sip.communicator.util.Logger;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;

import com.onsip.communicator.impl.applet.call.CallManager;
import com.onsip.communicator.impl.applet.call.volume.InputVolumeControl;
import com.onsip.communicator.impl.applet.call.volume.OutputVolumeControl;
import com.onsip.communicator.impl.applet.login.RegistrationManager;
import com.onsip.communicator.impl.applet.utils.LocalNetaddr;
import com.onsip.communicator.impl.applet.utils.NotificationManager;
import com.onsip.communicator.impl.applet.utils.SoundProperties;

import org.jitsi.service.audionotifier.AudioNotifierService;
import org.jitsi.service.configuration.ConfigurationService;
import org.jitsi.service.neomedia.MediaService;
import org.jitsi.service.neomedia.MediaType;
import org.jitsi.service.neomedia.MediaUseCase;
import org.jitsi.service.neomedia.device.MediaDevice;
import net.java.sip.communicator.service.notification.NotificationService;
import net.java.sip.communicator.service.notification.SoundNotificationAction;
import net.java.sip.communicator.service.protocol.ProtocolNames;
import net.java.sip.communicator.service.protocol.ProtocolProviderFactory;
import net.java.sip.communicator.service.protocol.ProtocolProviderService;
import org.jitsi.service.resources.ResourceManagementService;
import net.java.sip.communicator.util.ServiceUtils;

/**
 * Activates the Applet Bundle.
 * Provides an API that serves the needs of the applet.
 *
 * @author Oren Forer
 */
public class AppletActivator
    implements BundleActivator
{
    private final static Logger logger
        = Logger.getLogger(AppletActivator.class);

    private static BundleContext m_context = null;

    private static ResourceManagementService resourcesService = null;
    private static ConfigurationService configurationService = null;
    private static AudioNotifierService audioNotifierService = null;
    private static NotificationService notificationService   = null;
    private static MediaService mediaService = null;

    private static InputVolumeControl inputVolumeControl = null;
    private static OutputVolumeControl outputVolumeControl = null;

    /**
     * Callback objects. These objects call a 'fireEvent' function that
     * exists in the eventing API outside of Felix, and inside of the
     * applet where Felix was launched from.  The listeners, too, are
     * registered inside the eventing API. These objects take a String Array
     * as arguments. Those arguments are then parsed and sent to the browser
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

        try
        {
            /**
             * Effectively, a hash to get private ip address on Mac Lion
             * and Mountain Lion.
             */
            LocalNetaddr.setConfigRoute();
        }
        catch(Exception ex)
        {
            // shouldn't happen, but you never know
            logger.error("Exception :: start : LocalNetaddr.setConfigRoute ");
            logger.error(ex, ex);
        }
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
            configurationService =
                ServiceUtils.getService(m_context,
                    ConfigurationService.class);
        }

        return configurationService;
    }

    /**
     * Returns an instance of the <tt>MediaService</tt> obtained from the
     * bundle context.
     *
     * @return an instance of the <tt>MediaService</tt> obtained from the
     * bundle context
     */
    public static MediaService getMediaService()
    {
        if (mediaService == null)
        {
            mediaService =
                ServiceUtils.getService(m_context,
                    MediaService.class);
        }

        return mediaService;
    }

    /**
     * Returns an instance of the <tt>ResourceManagementService</tt>
     * obtained from the bundle context.
     *
     * @return an instance of the <tt>ResourceManagementService</tt>
     * obtained from the bundle context
     */
    public static ResourceManagementService getResources()
    {
        if (resourcesService == null)
        {
            resourcesService =
                ServiceUtils.getService(m_context,
                    ResourceManagementService.class);
        }

        return resourcesService;
    }

    /**
     * Returns an instance of the <tt>AudioNotifierService</tt>
     * obtained from the bundle context.
     *
     * @return an instance of the <tt>AudioNotifierService</tt>
     * obtained from the bundle context
     */
    public static AudioNotifierService getAudioNotifier()
    {
        if (audioNotifierService == null)
        {
            audioNotifierService =
                ServiceUtils.getService(m_context,
                    AudioNotifierService.class);
        }

        return audioNotifierService;
    }

    /**
     * Returns the <tt>NotificationService</tt> obtained from the bundle context.
     *
     * @return the <tt>NotificationService</tt> obtained from the bundle context
     */
    public static NotificationService getNotificationService()
    {
        if (notificationService == null)
        {
            notificationService =
                ServiceUtils.getService(m_context,
                    NotificationService.class);
        }
        return notificationService;
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

    /**
     * Callback function set by the applet. Asynchronous registration
     * events will be sent through this object
     */
    public void setRegistrationEventSource(Object registrationEventSource)
    {
        this.registrationEventSource = registrationEventSource;
        registrationManager.setRegistrationEventSource(registrationEventSource);
    }

    /**
     * Callback function set by the applet. Asynchronous call events
     * events will be sent through this object
     */
    public void setCallEventSource(Object callEventSource)
    {
        this.callEventSource = callEventSource;
        callManager.setCallEventSource(callEventSource);
    }

    /**
     * Callback function set by the applet. Asynchronous call peer
     * events will be sent through this object
     */
    public void setCallPeerEventSource(Object callPeerSource)
    {
        this.callPeerEventSource = callPeerSource;
        callManager.setCallPeerEventSource(callPeerSource);
    }

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
     * Put a specific call peer on hold
     *
     * @param callId call id
     * @param peerId peer id
     * @param hold on/off
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

    /**
     * Controls the playback volume; enables / disables the ringer.
     *
     * @param level 0 - 100
     * @param enableOnNotify ringer on / off
     */
    public void setOutputVolume(Integer level, Boolean enableOnNotify)
    {
        if (outputVolumeControl == null)
        {
            outputVolumeControl =
                new OutputVolumeControl(getMediaService());
        }

        /**
         * Effectively turns off the ringer
         */
        setNotifyVolumeOnMute(enableOnNotify.booleanValue());

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

    /**
     * Gives the consuming api the ability to turn off
     * the ringer
     *
     * @param enable on / off
     */
    public void setNotifyVolumeOnMute(boolean enable)
    {
        try
        {
            NotificationService notificationService =
                AppletActivator.getNotificationService();

            if (enable)
            {
                SoundNotificationAction inCallSoundHandler =
                    new SoundNotificationAction(SoundProperties.INCOMING_CALL, 2000);

                notificationService.registerDefaultNotificationForEvent(
                    NotificationManager.INCOMING_CALL,
                        inCallSoundHandler);
            }
            else
            {
                notificationService.removeEventNotification(
                    NotificationManager.INCOMING_CALL);
            }
        }
        catch(Exception ex)
        {
            logger.error("AppletActivator :: " +
                "setNotifyVolumeOnMute : Error while enabling / disabling " +
                    "notification volume control");
            logger.error(ex, ex);
        }
    }

    /**
     * If enabled (default), sets the volume of the ringer at a constant
     * volume level (~50) such that changing the playback volume does not
     * affect the ringer.
     *
     * @param enable on / off
     */
    public void setNotifyVolumeControl(boolean enable)
    {
        try
        {
            getConfigurationService().
                setProperty("net.java.sip.communicator.impl.neomedia.notifyvolumecontrol",
                    enable);
        }
        catch(Exception ex)
        {
            logger.error("AppletActivator :: " +
                "setNotifyVolumeControl : Error while enabling " +
                    "notification volume control");
            logger.error(ex, ex);
        }
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
