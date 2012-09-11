package com.onsip.communicator.impl.applet.utils;

import java.util.Iterator;
import java.util.Map;

import com.onsip.communicator.impl.applet.AppletActivator;

import net.java.sip.communicator.service.notification.*;
import net.java.sip.communicator.service.protocol.Call;
import net.java.sip.communicator.service.protocol.CallPeer;
import net.java.sip.communicator.util.Logger;

public class NotificationManager
{    
    public static final String INCOMING_MESSAGE = "IncomingMessage";

    public static final String INCOMING_CALL = "IncomingCall";

    public static final String OUTGOING_CALL = "OutgoingCall";

    public static final String BUSY_CALL = "BusyCall";

    public static final String DIALING = "Dialing";

    public static final String HANG_UP = "HangUp";

    public static final String PROACTIVE_NOTIFICATION = "ProactiveNotification";

    public static final String SECURITY_MESSAGE = "SecurityMessage";

    public static final String CALL_SECURITY_ON = "CallSecurityOn";

    public static final String CALL_SECURITY_ERROR = "CallSecurityError";

    public static final String INCOMING_FILE = "IncomingFile";

    public static final String CALL_SAVED = "CallSaved";
        
    private final static Logger logger
        = Logger.getLogger(NotificationManager.class);


    /**
     * Checks if the contained call is a conference call.
     *
     * @param call the call to check
     * @return <code>true</code> if the contained <tt>Call</tt> is a conference
     * call, otherwise - returns <code>false</code>.
     */
    public static boolean isConference(Call call)
    {
        // If we're the focus of the conference.
        if (call.isConferenceFocus())
            return true;

        // If one of our peers is a conference focus, we're in a
        // conference call.
        Iterator<? extends CallPeer> callPeers = call.getCallPeers();

        while (callPeers.hasNext())
        {
            CallPeer callPeer = callPeers.next();

            if (callPeer.isConferenceFocus())
                return true;
        }

        // the call can have two peers at the same time and there is no one
        // is conference focus. This is situation when some one has made an
        // attended transfer and has transfered us. We have one call with two
        // peers the one we are talking to and the one we have been transfered
        // to. And the first one is been hanguped and so the call passes through
        // conference call fo a moment and than go again to one to one call.
        return call.getCallPeerCount() > 1;
    }
    
    public static void registerGuiNotifications()
    {
        NotificationService notificationService
            = AppletActivator.getNotificationService();

        if(notificationService == null)
        {
            logger.error("Notification Service is null");
            return;
        }

        SoundNotificationAction inCallSoundHandler
            = new SoundNotificationAction(SoundProperties.INCOMING_CALL, 2000);

        notificationService.registerDefaultNotificationForEvent(
                NotificationManager.INCOMING_CALL,
                inCallSoundHandler);

        // Register outgoing call notifications.        
        SoundNotificationAction outCallSoundHandler
            = new SoundNotificationAction(SoundProperties.OUTGOING_CALL, 3000);


        notificationService.registerDefaultNotificationForEvent(
                NotificationManager.OUTGOING_CALL,
                outCallSoundHandler);

        // Register busy call notifications.
        SoundNotificationAction busyCallSoundHandler
            = new SoundNotificationAction(SoundProperties.BUSY, 3000);

        notificationService.registerDefaultNotificationForEvent(
                NotificationManager.BUSY_CALL,
                busyCallSoundHandler);

        // Register dial notifications.
        SoundNotificationAction dialSoundHandler
            = new SoundNotificationAction(SoundProperties.DIALING, 0);

        notificationService.registerDefaultNotificationForEvent(
                NotificationManager.DIALING,
                dialSoundHandler);

        // Register the hangup sound notification.
        SoundNotificationAction hangupSoundHandler
            = new SoundNotificationAction(SoundProperties.HANG_UP, -1);


        notificationService.registerDefaultNotificationForEvent(
                NotificationManager.HANG_UP,
                hangupSoundHandler);

        // Register sound notification for security state on during a call.
        notificationService.registerDefaultNotificationForEvent(
                NotificationManager.CALL_SECURITY_ON,
                NotificationAction.ACTION_SOUND,
                SoundProperties.CALL_SECURITY_ON,
                null);

        // Register sound notification for security state off during a call.
        notificationService.registerDefaultNotificationForEvent(
                NotificationManager.CALL_SECURITY_ERROR,
                NotificationAction.ACTION_SOUND,
                SoundProperties.CALL_SECURITY_ERROR,
                null);

        notificationService.registerDefaultNotificationForEvent(
                NotificationManager.INCOMING_FILE,
                NotificationAction.ACTION_SOUND,
                SoundProperties.INCOMING_FILE,
                null);

    }

    /**
     * Fires a notification for the given event type through the
     * <tt>NotificationService</tt>. The event type is one of the static
     * constants defined in this class.
     *
     * @param eventType the event type for which we want to fire a notification
     * @return A reference to the fired notification to stop it.
     */
    public static NotificationData fireNotification(String eventType)
    {
        NotificationService notificationService
            = AppletActivator.getNotificationService();

        if(notificationService == null)
            return null;

        return notificationService.fireNotification(eventType);
    }

    /**
     * Fires a message notification for the given event type through the
     * <tt>NotificationService</tt>.
     *
     * @param eventType the event type for which we fire a notification
     * @param messageTitle the title of the message
     * @param message the content of the message
     * @return A reference to the fired notification to stop it.
     */
    public static NotificationData fireNotification(String eventType,
                                        String messageTitle,
                                        String message)
    {
        NotificationService notificationService
            = AppletActivator.getNotificationService();

        if(notificationService == null)
            return null;

        return notificationService.fireNotification(eventType,
                                                messageTitle,
                                                message,
                                                null,
                                                null);
    }

    /**
     * Fires a message notification for the given event type through the
     * <tt>NotificationService</tt>.
     *
     * @param eventType the event type for which we fire a notification
     * @param messageTitle the title of the message
     * @param message the content of the message
     * @param extra additional event data for external processing
     * @return A reference to the fired notification to stop it.
     */
    public static NotificationData fireNotification(String eventType,
                                        String messageTitle,
                                        String message,
                                        Map<String,String> extra)
    {
        NotificationService notificationService
            = AppletActivator.getNotificationService();

        if(notificationService == null)
            return null;

        return notificationService.fireNotification(eventType,
                                                    messageTitle,
                                                    message,
                                                    extra,
                                                    null,
                                                    null);
    }

    /**
     * Stops all sounds for the given event type.
     * 
     * @param eventType the event type for which we should stop sounds. One of
     * the static event types defined in this class.
     */
    public static void stopSound(NotificationData data)
    {
        NotificationService notificationService
        = AppletActivator.getNotificationService();

        if(notificationService == null)
            return;

        Iterable<NotificationHandler> soundHandlers
            = notificationService.getActionHandlers(
                    NotificationAction.ACTION_SOUND);

        // There could be no sound action handler for this event type
        if (soundHandlers != null)
        {
            for (NotificationHandler handler : soundHandlers)
            {
                if (handler instanceof SoundNotificationHandler)
                    ((SoundNotificationHandler) handler).stop(data);
            }
        }
    }

}
