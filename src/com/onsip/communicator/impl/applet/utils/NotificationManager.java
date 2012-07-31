package com.onsip.communicator.impl.applet.utils;

import com.onsip.communicator.impl.applet.AppletActivator;

import net.java.sip.communicator.service.notification.*;
import net.java.sip.communicator.service.notification.SoundNotificationHandler;
import org.jitsi.service.resources.ResourceManagementService;
import net.java.sip.communicator.plugin.notificationwiring.*;
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
     * The incoming message sound id.
     */
    public static String INCOMING_MESSAGE_PATH;

    /**
     * The incoming file sound id.
     */
    public static String INCOMING_FILE_PATH;

    /**
     * The outgoing call sound id.
     */
    public static String OUTGOING_CALL_PATH;

    /**
     * The incoming call sound id.
     */
    public static String INCOMING_CALL_PATH;

    /**
     * The busy sound id.
     */
    public static String BUSY_PATH;

    /**
     * The dialing sound id.
     */
    public static String DIALING_PATH;

    /**
     * The sound id of the sound played when call security is turned on.
     */
    public static String CALL_SECURITY_ON_PATH;

    /**
     * The sound id of the sound played when a call security error occurs.
     */
    public static String CALL_SECURITY_ERROR_PATH;

    /**
     * The hang up sound id.
     */
    public static String HANG_UP_PATH;
    
    static void initRingTones() 
    {      
        ResourceManagementService resources = AppletActivator.getResources();
        
        INCOMING_MESSAGE_PATH = resources.getSoundPath("INCOMING_MESSAGE");
        INCOMING_FILE_PATH = resources.getSoundPath("INCOMING_FILE");
        OUTGOING_CALL_PATH = resources.getSoundPath("OUTGOING_CALL");        
        INCOMING_CALL_PATH = resources.getSoundPath("INCOMING_CALL");
        BUSY_PATH = resources.getSoundPath("BUSY");
        DIALING_PATH = resources.getSoundPath("DIAL");
        CALL_SECURITY_ON_PATH = resources.getSoundPath("CALL_SECURITY_ON");
        CALL_SECURITY_ERROR_PATH= resources.getSoundPath("CALL_SECURITY_ERROR");
        HANG_UP_PATH = resources.getSoundPath("HANG_UP");        
    }
    
    public static void registerGuiNotifications()
    {
        initRingTones();
        
        NotificationService notificationService
            = AppletActivator.getNotificationService();

        if(notificationService == null)
        {
            logger.error("Notification Service is null");
            return;
        }
        
        // Register incoming message notifications.
        /**
        notificationService.registerDefaultNotificationForEvent(
                INCOMING_MESSAGE,
                NotificationService.ACTION_POPUP_MESSAGE,
                null,
                null);
        **/
        /**
        notificationService.registerDefaultNotificationForEvent(
                INCOMING_MESSAGE,
                NotificationService.ACTION_SOUND,
                SoundProperties.INCOMING_MESSAGE,
                null);
         **/
        // Register incoming call notifications.
        /**
        notificationService.registerDefaultNotificationForEvent(
                INCOMING_CALL,
                NotificationService.ACTION_POPUP_MESSAGE,
                null,
                null);
        **/
        
        SoundNotificationAction inCallSoundHandler
            = new SoundNotificationAction(SoundProperties.INCOMING_CALL, 2000);

        /**
        SoundNotificationHandler inCallSoundHandler
            = notificationService
                .createSoundNotificationHandler(INCOMING_CALL_PATH,
                                                2000);
        **/

        notificationService.registerDefaultNotificationForEvent(
                NotificationManager.INCOMING_CALL,
                inCallSoundHandler);

        // Register outgoing call notifications.        
        SoundNotificationAction outCallSoundHandler
            = new SoundNotificationAction(SoundProperties.OUTGOING_CALL, 3000);

        //notificationService
          //      .createSoundNotificationHandler(NotificationManager.OUTGOING_CALL,
           //                                     3000);

        notificationService.registerDefaultNotificationForEvent(
                NotificationManager.OUTGOING_CALL,
                outCallSoundHandler);

        // Register busy call notifications.
        SoundNotificationAction busyCallSoundHandler
            = new SoundNotificationAction(SoundProperties.BUSY, 3000);

        /**
        SoundNotificationHandler busyCallSoundHandler
            = notificationService
                .createSoundNotificationHandler(BUSY_PATH, 1);
        **/

        notificationService.registerDefaultNotificationForEvent(
                NotificationManager.BUSY_CALL,
                busyCallSoundHandler);

        // Register dial notifications.
        SoundNotificationAction dialSoundHandler
            = new SoundNotificationAction(SoundProperties.DIALING, 0);

        /**
        SoundNotificationHandler dialSoundHandler
            = notificationService
                .createSoundNotificationHandler(DIALING_PATH, 0);
        **/

        notificationService.registerDefaultNotificationForEvent(
                NotificationManager.DIALING,
                dialSoundHandler);

        // Register the hangup sound notification.
        SoundNotificationAction hangupSoundHandler
            = new SoundNotificationAction(SoundProperties.HANG_UP, 0);

        /**
        SoundNotificationHandler hangupSoundHandler
            = notificationService
                .createSoundNotificationHandler(HANG_UP_PATH, -1);
        **/

        notificationService.registerDefaultNotificationForEvent(
                NotificationManager.HANG_UP,
                hangupSoundHandler);

        // Register proactive notifications.
        /**
        notificationService.registerDefaultNotificationForEvent(
                PROACTIVE_NOTIFICATION,
                NotificationService.ACTION_POPUP_MESSAGE,
                null,
                null);
        **/
        // Register warning message notifications.
        /**
        notificationService.registerDefaultNotificationForEvent(
                SECURITY_MESSAGE,
                NotificationService.ACTION_POPUP_MESSAGE,
                null,
                null);
        **/
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

        // Register sound notification for incoming files.
        /** 
        notificationService.registerDefaultNotificationForEvent(
                INCOMING_FILE,
                NotificationService.ACTION_POPUP_MESSAGE,
                null,
                null);
        **/
        notificationService.registerDefaultNotificationForEvent(
                NotificationManager.INCOMING_FILE,
                NotificationAction.ACTION_SOUND,
                SoundProperties.INCOMING_FILE,
                null);

        // Register notification for saved calls.
        /**
        notificationService.registerDefaultNotificationForEvent(
            CALL_SAVED,
            NotificationService.ACTION_POPUP_MESSAGE,
            null,
            null);
        **/
    }

    /**
     * Fires a message notification for the given event type through the
     * <tt>NotificationService</tt>.
     * 
     * @param eventType the event type for which we fire a notification
     * @param messageTitle the title of the message
     * @param message the content of the message
     */
    public static void fireNotification(String eventType,
                                        String messageTitle,
                                        String message)
    {        
        NotificationService notificationService
            = AppletActivator.getNotificationService();

        if(notificationService == null)
            return;

        notificationService.fireNotification(   eventType,
                                                messageTitle,
                                                message,
                                                null,
                                                null);
    }


    /**
     * Fires a notification for the given event type through the
     * <tt>NotificationService</tt>. The event type is one of the static
     * constants defined in this class.
     * 
     * @param eventType the event type for which we want to fire a notification
     */
    public static void fireNotification(String eventType)
    {
        NotificationService notificationService
            = AppletActivator.getNotificationService();

        if(notificationService == null)
            return;

        notificationService.fireNotification(eventType);
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
        = NotificationWiringActivator.getNotificationService();

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
