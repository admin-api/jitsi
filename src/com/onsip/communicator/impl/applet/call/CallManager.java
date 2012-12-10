package com.onsip.communicator.impl.applet.call;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.WeakHashMap;

import javax.swing.Timer;

import net.java.sip.communicator.service.notification.NotificationData;
import net.java.sip.communicator.service.protocol.Call;
import net.java.sip.communicator.service.protocol.CallPeer;
import net.java.sip.communicator.service.protocol.CallPeerState;
import net.java.sip.communicator.service.protocol.OperationFailedException;
import net.java.sip.communicator.service.protocol.OperationSet;
import net.java.sip.communicator.service.protocol.OperationSetAdvancedTelephony;
import net.java.sip.communicator.service.protocol.OperationSetBasicTelephony;
import net.java.sip.communicator.service.protocol.OperationSetTelephonyConferencing;
import net.java.sip.communicator.service.protocol.ProtocolProviderService;
import net.java.sip.communicator.service.protocol.event.CallChangeEvent;
import net.java.sip.communicator.service.protocol.event.CallChangeListener;
import net.java.sip.communicator.service.protocol.event.CallEvent;
import net.java.sip.communicator.service.protocol.event.CallListener;
import net.java.sip.communicator.service.protocol.event.CallPeerChangeEvent;
import net.java.sip.communicator.service.protocol.event.CallPeerEvent;
import net.java.sip.communicator.util.Logger;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

import com.onsip.communicator.impl.applet.AppletActivator;
import com.onsip.communicator.impl.applet.exceptions.CallManagerException;
import com.onsip.communicator.impl.applet.exceptions.CallNotFoundException;
import com.onsip.communicator.impl.applet.exceptions.MissingCallIdException;
import com.onsip.communicator.impl.applet.utils.CallPeerSerMapStore;
import com.onsip.communicator.impl.applet.utils.CallToCallSetupIdStore;
import com.onsip.communicator.impl.applet.utils.NotificationManager;
import com.onsip.communicator.util.json.JSONException;
import com.onsip.communicator.util.json.JSONSerializeCall;

public class CallManager extends CallPeerAdapter
    implements CallListener, CallChangeListener
{
    private final static Logger logger
        = Logger.getLogger(CallManager.class);

    /**
     * We'll use timers to temper requests / responses
     * into and out of the api. The problem we're specifically
     * solving here is a failure to answer a call immediately after
     * being notified of an incoming call.
     */
    private static Map<String, Long> callEventTimers =
        Collections.synchronizedMap(new HashMap<String, Long>(5));

    /**
     * Blind transfer, Attended transfer, or adhoc it;
     * If the two calls are up, and the transferee's uri
     * is one of the peers, then do attended
     * else do blind
     */
    public static enum TransferType {BLIND, ATTENDED, SEAMLESS};

    /**
     * Stores notification references to stop them if a notification has expired
     * (e.g. to stop the dialing sound).
     */
    private Map<Call, NotificationData> callNotifications =
        new WeakHashMap<Call, NotificationData>();

    private Object callEventSource = null;
    private Object callPeerEventSource = null;

    private static DTMFToneInfo[] tones = null;

    private String callSetupId = null;

    public CallManager()
    {
        /* cache DTMF tones */
        tones = DTMFHandler.getDTMFTones();
    }

    public void removeListeners()
    {
        try
        {
            BundleContext context = AppletActivator.getBundleContext();

            ServiceReference[] sipProviderRefs =
                AppletActivator.getServiceReferences();

            if (sipProviderRefs != null && sipProviderRefs.length > 0)
            {
                for (int i=0; i < sipProviderRefs.length; i++)
                {
                    ProtocolProviderService provider = (ProtocolProviderService)
                        context.getService(sipProviderRefs[i]);

                    if (provider != null)
                    {
                        OperationSetBasicTelephony<?> basicTelephony =
                            provider.getOperationSet(OperationSetBasicTelephony.class);

                        if (basicTelephony != null)
                        {
                            basicTelephony.removeCallListener(this);
                        }
                    }
                }
            }
        }
        catch (Exception e)
        {
            /* if an error occurs here, we can effectively ignore it */
            logWarn(e, Thread.currentThread().getStackTrace());
        }
    }

    public void setCallEventSource(Object callEventSource)
    {
        this.callEventSource = callEventSource;
    }

    public void setCallPeerEventSource(Object callPeerEventSource)
    {
        this.callPeerEventSource = callPeerEventSource;
    }

    public void mute(String callId, boolean m)
    {
        Call call = null;
        CallPeer peer = null;

        try
        {
            if (callId == null)
            {
                throw new MissingCallIdException();
            }

            call = getActiveCall(callId);
            if (call == null)
            {
                throw new CallNotFoundException();
            }

            ProtocolProviderService provider = call.getProtocolProvider();
            if (provider == null)
            {
                throw new Exception(ProtocolProviderService.class.toString() +
                    " is a missing detail, can't set call on mute");
            }

            OperationSetBasicTelephony<?> basicTelephony =
                provider.getOperationSet(OperationSetBasicTelephony.class);

            if (basicTelephony == null)
            {
                throw new Exception("Oh oh, OperationSet " +
                    OperationSetBasicTelephony.class.toString() + " is not supported");
            }

            boolean isLocallyOnHold = false;
            Iterator<? extends CallPeer> peers = call.getCallPeers();
            while(peers.hasNext())
            {
                peer = peers.next();
                if (CallManager.isLocallyOnHold(peer.getState()))
                {
                    isLocallyOnHold = true;
                    break;
                }
            }

            /**
             * If we have our call peers on hold we disable muting.
             * This functionality mimics Polycoms.
             * As an aside, setting a peer on hold, automatically
             * mutes the call as well.
             */
            if (!isLocallyOnHold)
            {
                basicTelephony.setMute(call, m);
            }

            String callSetupId = CallToCallSetupIdStore.get(callId);
            String json = JSONSerializeCall.JSONSerialize(call,
                call.getCallPeers(), callSetupId);

            logger.debug("FIRE SERIALIZED : mute " + json);

            String[] args = new String[] { json };

            Method method = this.callPeerEventSource.getClass().getMethod("fireEvent",
                Array.newInstance(String.class, 1).getClass());

            method.invoke(callPeerEventSource, (Object) args);
        }
        catch (Exception e)
        {
            logErr(new CallManagerException(
                call, peer, "Error in mute, details: " + e.getMessage()),
                Thread.currentThread().getStackTrace(), true);
            try
            {
              e.printStackTrace();
            } catch(Exception e2){}
        }

    }

    /**
     * hold
     *
     * @param hold set (true | false)
     */
    public void hold(String callId, String peerId, boolean hold)
    {
        Call call = null;
        CallPeer peer = null;

        try
        {
            if (callId == null)
            {
                throw new MissingCallIdException();
            }
            if (peerId != null)
            {
                logger.info("We are going to set hold with peer id " + peerId);
            }

            call = getActiveCall(callId);
            if (call == null)
            {
                throw new CallNotFoundException();
            }

            ProtocolProviderService provider = call.getProtocolProvider();
            if (provider == null)
            {
                throw new Exception(ProtocolProviderService.class.toString() +
                    " is a missing detail, can't set call on hold");
            }

            logger.debug("Call " + callId +
                " in progress, set hold = " + hold);

            OperationSetBasicTelephony<?> basicTelephony =
                provider.getOperationSet(OperationSetBasicTelephony.class);

            if (basicTelephony == null)
            {
                throw new Exception("Oh oh, OperationSet " +
                    OperationSetBasicTelephony.class.toString() + " is not supported");
            }
            Iterator<? extends CallPeer> peers = call.getCallPeers();
            while(peers.hasNext())
            {
                peer = peers.next();
                if (peerId == null ||
                    (peerId.equals(peer.getPeerID())))
                {
                    if (hold)
                    {
                        /**
                         * Oren F.
                         * Rework the functionality of hold so that its
                         * behavior mimics a Polycom. That is:
                         *
                         * If on local hold:
                         * - set call off mute (the underlying jitsi core will
                         *   set individual peers on mute)
                         * - do not let end users control mute functionality.
                         *
                         * If on remote hold:
                         * - end users can control mute as they'd like
                         */
                        boolean callOnMute = basicTelephony.isMute(call);
                        if (callOnMute)
                        {
                            basicTelephony.setMute(call, false);
                        }
                        basicTelephony.putOnHold(peer);
                    }
                    else
                    {
                        basicTelephony.putOffHold(peer);
                    }
                }
            }
        }
        catch (Exception e)
        {
            logErr(new CallManagerException(
                call, peer, "Error while trying to set user hold, details: " +
                    e.getMessage()), Thread.currentThread().getStackTrace(), true);
        }
    }

    private static boolean isLocallyOnHold(CallPeerState state)
    {
        return CallPeerState.ON_HOLD_LOCALLY.equals(state)
            || CallPeerState.ON_HOLD_MUTUALLY.equals(state);
    }

    /**
     * This transfer method can accommodate a number of use cases.
     * it can do attended, blind, or it can be setup up to attempt an
     * attended transfer and default to blind.
     *
     * @param callId
     * @param peerId
     * @param targetUri
     */
    public void transfer(String callId, String peerId,
        String targetUri, TransferType transferType)
    {
        Call call = null;
        CallPeer sourcePeer = null;

        try
        {
            if (callId == null)
            {
                throw new MissingCallIdException();
            }

            call = getActiveCall(callId);
            if (call == null)
            {
                throw new CallNotFoundException();
            }

            ProtocolProviderService provider = call.getProtocolProvider();
            if (provider == null)
            {
                throw new Exception(ProtocolProviderService.class.toString() +
                    " is a missing detail, can't transfer call");
            }

            logger.info("In call transfer, callId " + callId +
                ", peerId " + peerId + ", targetUri " + targetUri);

            OperationSetAdvancedTelephony<?> advancedTelephony =
                provider.getOperationSet(OperationSetAdvancedTelephony.class);

            if (advancedTelephony == null)
            {
                throw new Exception("Oh oh, Operation Set " +
                    OperationSetAdvancedTelephony.class.toString() +
                        " is not supported");
            }

            CallPeer targetPeer = null;

            OperationSetBasicTelephony<?> basicTelephony =
                provider.getOperationSet(OperationSetBasicTelephony.class);

            /* get all active calls */
            Iterator<? extends Call> calls = basicTelephony.getActiveCalls();
            while (calls.hasNext())
            {
                call = calls.next();
                Iterator<? extends CallPeer> peers = call.getCallPeers();
                while(peers.hasNext())
                {
                    CallPeer peer = peers.next();
                    if ((peerId.equals(peer.getPeerID())))
                    {
                        sourcePeer = peer;
                    }
                    logger.debug("in transfer, comparing, " +
                        peer.getAddress() + " AND " + targetUri);
                    if (targetUri.equalsIgnoreCase(peer.getAddress()))
                    {
                        targetPeer = peer;
                    }
                }
            }

            if (transferType == TransferType.SEAMLESS ||
                transferType == TransferType.ATTENDED)
            {
                if (targetPeer != null)
                {
                    logger.debug("Execute Attended Transfer uri = " + targetUri);

                    boolean threadSleep = false;
                    if (!CallManager.isLocallyOnHold(targetPeer.getState()))
                    {
                        threadSleep = true;
                        this.hold(targetPeer.getCall().getCallID(),
                            targetPeer.getPeerID(), true);
                    }

                    if (!CallManager.isLocallyOnHold(sourcePeer.getState()))
                    {
                        threadSleep = true;
                        this.hold(sourcePeer.getCall().getCallID(),
                            sourcePeer.getPeerID(), true);
                    }

                    /*
                     * Turns out that if we overwhelm the devices with
                     * requests (i.e hold, then instantly transfer), the device
                     * will start responding with a 491 Request Pending,
                     * or a 500 Internal Server error.  Creating a short
                     * buffer appears to sort the issue out.
                     */
                    if (threadSleep)
                    {
                        Thread.sleep(500);
                    }

                    advancedTelephony.transfer(sourcePeer, targetPeer);
                }
                else if(transferType == TransferType.ATTENDED)
                {
                    logger.error("We need to do an attended transfer here, " +
                        "but we can't seem to match up the uri to an existing peer");

                    throw new Exception("Could not match targetUri to existing call peer");

                }
                else
                {
                    // SEAMLESS, so just blind transfer it
                    logger.debug("Execute Blind Transfer uri = " + targetUri);
                    advancedTelephony.transfer(sourcePeer, targetUri);
                    this.hangUp(callId, sourcePeer.getPeerID());
                }
            }
            else
            {
                logger.debug("Execute Blind Transfer uri = " + targetUri);
                advancedTelephony.transfer(sourcePeer, targetUri);
                this.hangUp(callId, sourcePeer.getPeerID());
            }
        }
        catch(Exception e)
        {
            logErr(new CallManagerException(call, sourcePeer,
                "Failed to transfer peer, details: " + e.getMessage()),
                Thread.currentThread().getStackTrace(), true);
        }
    }

    /**
     * Conference in a new call. Calls out this sipUril
     */
    public void inviteCalleeToCall(String callId, String sipUri)
    {
        Call call = null;

        try
        {
            if (callId == null)
            {
                throw new MissingCallIdException();
            }

            call = getActiveCall(callId);
            if (call == null)
            {
                throw new CallNotFoundException();
            }

            ProtocolProviderService provider = call.getProtocolProvider();
            if (provider == null)
            {
                throw new Exception(ProtocolProviderService.class.toString() +
                    " is a missing detail, can't do invite");
            }

            OperationSetTelephonyConferencing telephonyConferencing =
                provider.getOperationSet(OperationSetTelephonyConferencing.class);

            if (telephonyConferencing == null)
            {
                throw new Exception("Oh oh, Operation Set " +
                    OperationSetTelephonyConferencing.class.toString() +
                        " is not supported");
            }

            telephonyConferencing.inviteCalleeToCall(sipUri, call);
        }
        catch (Exception e)
        {
            logErr(new CallManagerException(call, null,
                "Failed, details: " + e.getMessage()),
                    Thread.currentThread().getStackTrace(), true);
        }
    }

    /**
     * Create a call
     */
    public void call(String userId, String sip, String callSetupId)
    {
        Call call = null;
        try
        {
            ProtocolProviderService provider =
                AppletActivator.getPrototocolProviderService(userId);

            if (provider == null)
            {
                throw new Exception(ProtocolProviderService.class.toString() +
                    " is a missing detail, can't create call");
            }

            OperationSetBasicTelephony<?> basicTelephony =
                provider.getOperationSet(OperationSetBasicTelephony.class);
            if (basicTelephony == null)
            {
                throw new Exception("Oh oh, Operation Set " +
                    OperationSetBasicTelephony.class.toString() +
                        " is not supported");
            }

            if (callSetupId != null)
            {
                logger.info("We need to add callSetupId [" + callSetupId + "]");
                this.callSetupId = callSetupId;
            }

            call = basicTelephony.createCall(sip);

            if (call == null)
            {
                throw new Exception("Failed to create call to " + sip);
            }

            call.addCallChangeListener(this);
        }
        catch (Exception e)
        {
            logErr(new CallManagerException(call, null,
                "Failed to create call, details: " + e.getMessage()),
                    Thread.currentThread().getStackTrace(), true);
        }
        finally
        {
            logger.info("Setting callSetupId to NULL");
            this.callSetupId = null;
        }
    }

    /**
     * Let's pick up the incoming phone call
     */
    public void pickupCall(String callId)
    {
        Call call = null;
        CallPeer peer = null;
        try
        {
            if (callId == null)
            {
                throw new MissingCallIdException();
            }

            call = getActiveCall(callId);
            if (call == null)
            {
                throw new CallNotFoundException();
            }

            ProtocolProviderService provider = call.getProtocolProvider();
            if (provider == null)
            {
                throw new Exception(ProtocolProviderService.class.toString() +
                    " is a missing detail, can't answer call");
            }

            logger.info("We found call " + callId);

            OperationSetBasicTelephony<?> basicTelephony =
                provider.getOperationSet(OperationSetBasicTelephony.class);

            if (basicTelephony == null)
            {
                throw new Exception("Oh oh, Operation Set " +
                    OperationSetBasicTelephony.class.toString() +
                        " is not supported");
            }

            logger.info("Picking up call with id " + callId +
                " in call state " + call.getCallState().getStateString());

            Iterator<? extends CallPeer> peers = call.getCallPeers();
            while(peers.hasNext())
            {
                peer = peers.next();
                logger.info("Adding call peer listener " + peer.getAddress());
                peer.addCallPeerListener(this);
            }

            logger.info("CallManager.answer " + call.getCallID());
            this.answer(call, this);
            logger.info("call.addCallChangeListener");
            call.addCallChangeListener(this);
        }
        catch (Exception e)
        {
            logErr(new CallManagerException(call, peer,
                "Failed to answer call, details: " + e.getMessage()),
                    Thread.currentThread().getStackTrace(), true);
        }
    }

    /**
     * answer call thread
     *
     * @param incoming the incoming call
     */
    public void answer(final Call incoming, final CallManager callManager)
    {
        logger.info("in call answer");

        ActionListener answerTask = new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                callEventTimers.remove(incoming.getCallID());
                logger.info("launch answer call thread");
                new AnswerCallThread(incoming, callManager).start();
            }
        };

        logger.info("callEventTimer get incoming call Id");

        Long tIncoming = callEventTimers.get(incoming.getCallID());

        int delay = 0;
        if ((System.currentTimeMillis() - tIncoming.longValue()) < 500)
        {
            delay = 500;
        }
        logger.info("start answer timer");
        Timer tAnswer = new Timer(delay, answerTask);
        tAnswer.setRepeats(false);
        tAnswer.start();
    }

    private static class AnswerCallThread
        extends Thread
    {
        private final Call call;
        private final CallManager callManager;

        public AnswerCallThread(Call call, CallManager callManager)
        {
            this.call = call;
            this.callManager = callManager;
        }

        @Override
        public void run()
        {
            try
            {
                ProtocolProviderService provider = call.getProtocolProvider();

                OperationSetBasicTelephony<?> basicTelephony =
                    provider.getOperationSet(OperationSetBasicTelephony.class);

                Iterator<? extends CallPeer> peers = call.getCallPeers();
                while (peers.hasNext())
                {
                    CallPeer peer = peers.next();
                    try
                    {
                        if (peer.getState() == CallPeerState.INCOMING_CALL)
                        {
                            basicTelephony.answerCallPeer(peer);
                        }
                    }
                    catch (OperationFailedException e)
                    {
                        logger.error("AnswerCallThread :: run :");
                        logger.error("Could not answer : " + peer
                            + " caused by the following exception: " + e, e);
                        this.callManager.sendError(
                            new CallManagerException(this.call, peer,
                                "Failed to answer call for peer " + peer.getAddress()));
                    }
                }
            }
            catch (Exception e)
            {
                logger.error("Exception :: AnswerCallThread : ");
                logger.error(e, e);
            }
        }
    }

    /**
     * Hang up the call peer
     * Hang up by call id or a peer id
     *
     * @param peerId
     */
    public void hangUp(String callId, String peerId)
    {
        Call call = null;
        CallPeer peer = null;
        try
        {
            if (callId == null)
            {
                throw new MissingCallIdException();
            }
            logger.info("We are going to hang up a call with call id " + callId);

            if (peerId != null)
            {
                logger.info("We are going to hang up a call with peer id " + peerId);
            }
            else
            {
                logger.info("Peer id was not provided on hangup");
            }

            call = getActiveCall(callId);
            if (call == null)
            {
                throw new CallNotFoundException();
            }

            ProtocolProviderService provider = call.getProtocolProvider();
            if (provider == null)
            {
                throw new Exception(ProtocolProviderService.class.toString() +
                    " is a missing detail, can't hang up call");
            }

            logger.info("We found call " + callId);

            OperationSetBasicTelephony<?> basicTelephony =
                provider.getOperationSet(OperationSetBasicTelephony.class);
            if (basicTelephony == null)
            {
                throw new Exception("Oh oh, Operation Set " +
                    OperationSetBasicTelephony.class.toString() +
                        " is not supported");
            }

            boolean bHungUp = false;

            Iterator<? extends CallPeer> peers = call.getCallPeers();
            while (peers.hasNext())
            {
                peer = peers.next();
                try
                {
                    if (peerId == null || peerId.equals(peer.getPeerID()))
                    {
                        logger.debug("Attempt to hangup call " + callId);
                        basicTelephony.hangupCallPeer(peer);
                        /* remove the listener before we hang up */
                        peer.removeCallPeerListener(this);
                        bHungUp = true;
                    }
                }
                catch (OperationFailedException e)
                {
                    logger.error("OperationFailedException :: hangup : ");
                    logger.error(e, e);
                    sendError(new CallManagerException(call, peer,
                        "Failed to hangup call, details: " + e.getMessage()));
                }
            }

            if (bHungUp)
            {
                NotificationManager.fireNotification(
                    NotificationManager.HANG_UP);
            }
        }
        catch (Exception e)
        {
            logErr(new CallManagerException(call, peer,
                "Failed to hangup on peer, details: " + e.getMessage()),
                    Thread.currentThread().getStackTrace(), true);
        }
    }

    public void dispatchKeyEvent(final String callId, char key, boolean start)
    {
        Call call = null;
        try
        {
            logger.debug("Dispatching DTMF tone for key " + key);
            if (callId == null)
            {
                throw new MissingCallIdException();
            }

            call = getActiveCall(callId);
            if (call == null)
            {
                throw new CallNotFoundException();
            }

            for (int i = 0; i < tones.length; i++)
            {
                DTMFToneInfo info = tones[i];
                if (info.getKeyChar() == key)
                {
                    logger.debug("Found tone for key pressed " + key);
                    if (start)
                    {
                        DTMFHandler.startSendingDtmfTone(info, call);
                    }
                    else
                    {
                        DTMFHandler.stopSendingDtmfTone(call);
                    }
                }
            }
        }
        catch(Exception e)
        {
            logErr(new CallManagerException(call, null,
                "Failed while sending DTMF, details: " + e.getMessage()),
                    Thread.currentThread().getStackTrace(), true);
        }
    }

    public void dispatchKeyEvent(final String callId, char key)
    {
        Call call = null;

        try
        {
            logger.debug("Dispatching DTMF tone for key " + key);
            if (callId == null)
            {
                throw new MissingCallIdException();
            }

            call = getActiveCall(callId);
            if (call == null)
            {
                throw new CallNotFoundException();
            }

            for (int i = 0; i < tones.length; i++)
            {
                DTMFToneInfo info = tones[i];
                if (info.getKeyChar() == key)
                {
                    logger.debug("Found tone for key pressed " + key);
                    DTMFHandler.startSendingDtmfTone(info, call);
                    int delay = 240;
                    ActionListener taskPerformer = new ActionListener() {
                        public void actionPerformed(ActionEvent evt) {
                            try
                            {
                                Call call = getActiveCall(callId);
                                DTMFHandler.stopSendingDtmfTone(call);
                            }
                            catch (Exception e)
                            {
                                logger.error("Exception :: stopSendingDtmfTone : ");
                                logger.error(e, e);
                            }
                        }
                    };

                    Timer t = new Timer(delay, taskPerformer);
                    t.setRepeats(false);
                    t.start();
                }
            }
        }
        catch(Exception e)
        {
            logErr(new CallManagerException(call, null,
                "Failed while sending DTMF, details: " + e.getMessage()),
                    Thread.currentThread().getStackTrace(), true);
        }
    }

    private static Call getActiveCall(String callId)
        throws Exception
    {
        Call call = null;

        if (callId == null)
        {
            throw new MissingCallIdException
                ("Invalid call id parameter in getActiveCall");
        }

        BundleContext context = AppletActivator.getBundleContext();

        ServiceReference[] sipProviderRefs =
            AppletActivator.getServiceReferences();

        if (sipProviderRefs != null && sipProviderRefs.length > 0)
        {
            for (int i=0; i < sipProviderRefs.length; i++)
            {
                ProtocolProviderService provider = (ProtocolProviderService)
                    context.getService(sipProviderRefs[i]);
                if (provider == null)
                {
                    logger.info(ProtocolProviderService.class.toString() +
                        " is NULL, this call doesn't exist for call id " +
                            callId);
                }
                else if (provider.isRegistered())
                {
                    String userId = provider.getAccountID().getUserID();

                    OperationSetBasicTelephony<?> basicTelephony =
                        provider.getOperationSet(OperationSetBasicTelephony.class);

                    if(basicTelephony == null)
                    {
                        throw new Exception("Oh oh, Operation Set " +
                            OperationSetBasicTelephony.class.toString() +
                                " is not supported for user " + userId);
                    }

                    /* get all active calls */
                    Iterator<? extends Call> calls = basicTelephony.getActiveCalls();
                    while (calls.hasNext())
                    {
                        call = calls.next();
                        if (callId.equals(call.getCallID()))
                        {
                            logger.info("Found call details in " + userId);
                            return call;
                        }
                    }
                }
            }

        }
        if (call == null)
        {
            logger.info("Oh dang, could not find the call " +
                "associated with this call id [" + callId + "], return null");
        }
        return null;
    }

    public void outgoingCallCreated(CallEvent event)
    {
        Call call = null;
        CallPeer callPeer = null;
        try
        {
            call = event.getSourceCall();
            String callId = call.getCallID();
            call.addCallChangeListener(this);

            if (callId == null)
            {
                throw new MissingCallIdException();
            }
            if (this.callSetupId != null)
            {
                logger.info("Adding callSetupId [" + this.callSetupId +
                    "] to callId [" + callId + "]");

                CallToCallSetupIdStore.put(callId, this.callSetupId);
            }
            else
            {
                logger.info("callSetupId is NULL on this OUTGOING call " +
                    ", callId [" + callId + "]");
            }

            Iterator<? extends CallPeer> callPeers = call.getCallPeers();

            while(callPeers.hasNext())
            {
                callPeer = callPeers.next();
                callPeer.addCallPeerListener(this);
                String peerId = callPeer.getPeerID();
                if (peerId != null)
                {
                    String jsonPeer = JSONSerializeCall.JSONSerialize(callPeer);
                    if (jsonPeer != null && jsonPeer.length() > 0)
                    {
                        logger.debug("outgoingCallCreated: put into store" +
                            " CallId : " + callId + ", -> " + jsonPeer);
                        CallPeerSerMapStore.put(callId, peerId, jsonPeer);
                    }
                }
            }

            logger.info("Outgoing call created - Call ID " +  callId);

            if (callEventSource == null)
            {
                throw new Exception("Looks like callEventSource does not exist, " +
                    "can't send events back to the client applet");
            }

            String json = null;
            if (this.callSetupId != null)
            {
                // outgoing calls
                logger.info("Serializing with call setupId " + this.callSetupId);
                json = JSONSerializeCall.JSONSerialize
                    (call, call.getCallPeers(), this.callSetupId);
            }
            else
            {
                logger.info("Serializing with NO setupId ");
                // on transfer, no setup id
                json = JSONSerializeCall.JSONSerialize
                    (call, call.getCallPeers());
            }

            logger.debug("FIRE SERIALIZED : outgoingCallCreated " + json);

            String[] args = new String[] { json };
            Method m = this.callEventSource.getClass().getMethod("fireEvent",
                Array.newInstance(String.class, 1).getClass());

            m.invoke(callEventSource, (Object) args);
        }
        catch (Exception e)
        {
            logErr(new CallManagerException(call, callPeer,
                "Error creating the call, details: " +  e.getMessage()),
                    Thread.currentThread().getStackTrace(), true);
        }
        finally
        {
            this.callSetupId = null;
        }
    }

    public void incomingCallReceived(CallEvent event)
    {
        Call call = null;
        CallPeer peer = null;
        try
        {
            call = event.getSourceCall();

            if (call == null)
            {
                throw new CallNotFoundException();
            }

            callNotifications.put(call,
                NotificationManager.fireNotification(
                    NotificationManager.INCOMING_CALL));

            String callId = call.getCallID();

            callEventTimers.put(callId, new Long(System.currentTimeMillis()));

            logger.info("Incoming call - Call ID " +  callId);

            if (callEventSource == null)
            {
                throw new Exception("Looks like callEventSource does not exist, " +
                    "can't send events back to the client applet");
            }

            Iterator<? extends CallPeer> peers = call.getCallPeers();
            while (peers.hasNext())
            {
                peer = peers.next();
                String peerId = peer.getPeerID();
                if (peerId != null && peerId.length() > 0)
                {
                    String jsonPeer = JSONSerializeCall.JSONSerialize(peer);
                    if (jsonPeer != null && jsonPeer.length() > 0)
                    {
                        logger.debug("incomingCallReceived: put into store -> " + jsonPeer);
                        CallPeerSerMapStore.put(callId, peerId, jsonPeer);
                    }
                }
            }

            String json = JSONSerializeCall.JSONSerialize
                (call, call.getCallPeers());

            logger.debug("FIRE SERIALIZED : incomingCallReceived " + json);

            String[] args = new String[] { json };
            Method m = this.callEventSource.getClass().getMethod("fireEvent",
                Array.newInstance(String.class, 1).getClass());

            m.invoke(callEventSource, (Object) args);

        }
        catch (Exception e)
        {
            logErr(new CallManagerException(call, peer,
                "Error on incoming call, details: " +  e.getMessage()),
                    Thread.currentThread().getStackTrace(), true);
        }
    }

    public void callEnded(CallEvent event)
    {
        Call call = null;
        try
        {

            /*
             * retrieve the call that ended
             */
            call = event.getSourceCall();
            String callId = call.getCallID();

            logger.info("Call Ended - Call ID " +  callId +
                " - Found " + call.getCallPeerCount() + " call peers");

            // Stop all telephony related sounds.
            NotificationManager.stopSound(callNotifications.get(call));

            CallPeerSerMapStore[] peerSer =
                CallPeerSerMapStore.get(callId);

            String json = "";
            String callSetupId = CallToCallSetupIdStore.get(callId);
            if (peerSer.length == 0)
            {
                json = JSONSerializeCall.JSONSerialize(call,
                    call.getCallPeers(), callSetupId);
            }
            else
            {
                json = JSONSerializeCall.JSONSerialize(call,
                    peerSer, callSetupId);
            }

            if (callEventSource == null)
            {
                throw new Exception("Looks like callEventSource does not exist, " +
                    "can't send events back to the client applet");
            }

            // fire event back to our front end API
            String[] args = new String[] { json };
            Method m = this.callEventSource.getClass().getMethod("fireEvent",
                Array.newInstance(String.class, 1).getClass());

            m.invoke(callEventSource, (Object) args);

            logger.debug("FIRE SERIALIZED : callEnded " + json);

            CallToCallSetupIdStore.remove(callId);
            CallPeerSerMapStore.remove(callId);
            callEventTimers.remove(callId);

            logger.debug("Current Calls in MAP Store " +
                CallPeerSerMapStore.size());


            /* remove listener */
            call.removeCallChangeListener(this);

            /*
             *  loop over any existing calls, automatically
             *  take off hold one of the existing calls
             */

            /**
             ----------------------
             | Removed 10/19/2011 |
             ------------------------------------------------------------
             | I'm not sure, that this functionality makes sense.
             | Let the end user manually take a call off hold,
             | rather than doing it programmatically
             ------------------------------------------------------------
            ProtocolProviderService provider = call.getProtocolProvider();

            if (provider != null)
            {
                OperationSetBasicTelephony<?> basicTelephony =
                    provider.getOperationSet(OperationSetBasicTelephony.class);

                if (basicTelephony == null)
                {
                    return;
                }

                Iterator<? extends Call> calls =
                    basicTelephony.getActiveCalls();

                while (calls.hasNext())
                {
                    call = calls.next();
                    if (!call.getCallID().equals(callId))
                    {
                        Iterator<? extends CallPeer> peers =
                            call.getCallPeers();

                        logger.debug("Call " + call.getCallID() +
                            " is still live and in state " +
                                call.getCallState().getStateString());

                        while (peers.hasNext())
                        {
                            CallPeer peer = peers.next();
                            logger.debug("Current peer still live " +
                                peer.getState().getStateString() + " " +
                                    peer.getAddress());

                            if (peer.getState() == CallPeerState.ON_HOLD_LOCALLY ||
                                peer.getState() == CallPeerState.ON_HOLD_MUTUALLY)
                            {
                                basicTelephony.putOffHold(peer);
                            }
                        }

                        // peers on this call are off hold
                        return;
                    }
                }
            }
            **/
        }
        catch (Exception e)
        {
            logErr(new CallManagerException(call, null,
                "Error when call ended, details: " +  e.getMessage()),
                    Thread.currentThread().getStackTrace(), true);
        }
    }

    /**
     * Fired when peer's state is changed
     *
     * @param evt fired CallPeerEvent
     */
    @Override
    public void peerStateChanged(CallPeerChangeEvent evt)
    {
        Call call = null;
        CallPeer sourcePeer = null;
        try
        {
            sourcePeer = evt.getSourceCallPeer();

            //if (!sourcePeer.equals(callPeer))
            //{
            //    return;
            //}

            logger.debug("peerStateChanged: " + sourcePeer.getAddress());

            CallPeerState newState = (CallPeerState) evt.getNewValue();
            CallPeerState oldState = (CallPeerState) evt.getOldValue();

            String jsonSer = "";
            boolean bFireEvent = false;

            String callId = "";
            try
            {
                if (sourcePeer != null)
                {
                    call = sourcePeer.getCall();
                    logger.debug("JSON serialize Peer ID " + sourcePeer.getPeerID());
                    if (call != null)
                    {
                        callId = call.getCallID();
                    }
                    String callSetupId = CallToCallSetupIdStore.get(callId);
                    if (callSetupId != null)
                    {
                        jsonSer = JSONSerializeCall.JSONSerialize
                            (call, sourcePeer, callSetupId);
                    }
                    else
                    {
                        jsonSer = JSONSerializeCall.JSONSerialize
                            (call, sourcePeer);
                    }
                }
            }
            catch (JSONException e)
            {
                logErr(new CallManagerException(null, null,
                    "Error retrieving call, details: " +  e.getMessage()),
                        Thread.currentThread().getStackTrace(), false);
            }

            // Play the dialing audio when in connecting and initiating call state.
            // Stop the dialing audio when we enter any other state.
            if (newState == CallPeerState.INITIATING_CALL
                || newState == CallPeerState.CONNECTING)
            {
                logger.debug("Initiating call... ");
            }
            else
            {
                NotificationManager.stopSound(
                    callNotifications.get(call));
            }

            if (newState == CallPeerState.ALERTING_REMOTE_SIDE
                //if we were already in state CONNECTING_WITH_EARLY_MEDIA the server
                //is already taking care of playing the notifications so we don't
                //need to fire a notification here.
                && oldState != CallPeerState.CONNECTING_WITH_EARLY_MEDIA)
            {
                logger.debug("Alerting remote side, should play SOUND");
                callNotifications.put(call,
                    NotificationManager.fireNotification(
                        NotificationManager.OUTGOING_CALL));
            }
            else if (newState == CallPeerState.BUSY)
            {
                // We start the busy sound only if we're in a simple call.
                if (!NotificationManager.isConference(call))
                {
                    bFireEvent = true;

                    callNotifications.put(call,
                        NotificationManager.fireNotification(
                            NotificationManager.BUSY_CALL));
                }
            }
            else if (newState == CallPeerState.CONNECTING_INCOMING_CALL ||
                newState == CallPeerState.CONNECTING_INCOMING_CALL_WITH_MEDIA)
            {
                // place holder
            }
            else if (newState == CallPeerState.CONNECTING_WITH_EARLY_MEDIA)
            {
                /*
                 * This means a call with early media. make sure that we are not
                 * playing local notifications any more.
                 */
            }
            else if (newState == CallPeerState.CONNECTED)
            {
                // fire a client side event on CONNECTED
                bFireEvent = true;
            }
            else if (newState == CallPeerState.DISCONNECTED)
            {
                // the DISCONNECTED event appears to be called after call ended
            }
            else if (newState == CallPeerState.FAILED)
            {
                // The call peer should be already removed from the call
                // see CallPeerRemoved
                bFireEvent = false;
            }
            else if (newState == CallPeerState.REFERRED)
            {
                // fire a client side event on transfer
                bFireEvent = true;
            }
            else if (CallPeerState.isOnHold(newState))
            {
                bFireEvent = true;
            }

            /*
             *  Check the flag to see if we need to fire an event
             *  back to the front end
             */
            if (bFireEvent)
            {
                String[] args = new String[] { jsonSer };

                logger.debug("FIRE SERIALIZED : peerStateChanged " + jsonSer);

                Method m = this.callPeerEventSource.getClass().getMethod("fireEvent",
                    Array.newInstance(String.class, 1).getClass());

                m.invoke(callPeerEventSource, (Object) args);
            }
        }
        catch (Exception e)
        {
            logErr(new CallManagerException(sourcePeer.getCall(), sourcePeer,
                "Error peer changed state details: " +  e.getMessage()),
                    Thread.currentThread().getStackTrace(), true);
        }
    }

    public void callPeerRemoved(CallPeerEvent evt)
    {
        Call call = null;
        CallPeer peer = null;
        try
        {
            call = evt.getSourceCall();
            String callId = call.getCallID();

            peer = evt.getSourceCallPeer();
            String peerId = peer.getPeerID();

            logger.debug("CallPeerRemoved: Peer id " + peerId +
                " for call id " + callId);

            logger.debug("CallPeerRemoved: Number of peers on the call " +
                call.getCallPeerCount());

            String jsonPeer = JSONSerializeCall.JSONSerialize(peer);
            logger.debug("CallPeerRemoved: put into store -> " + jsonPeer);

            CallPeerSerMapStore.put(callId, peerId, jsonPeer);

            /* Send to client
             * TODO: Needs some explanation
             */
            if (call.getCallPeerCount() > 0)
            {
                String callSetupId = CallToCallSetupIdStore.get(callId);
                String json = JSONSerializeCall.JSONSerialize(call,
                    peer, callSetupId);

                logger.debug("FIRE SERIALIZED : callPeerRemoved " + json);

                if (callEventSource == null)
                {
                    throw new Exception("Looks like callEventSource does not exist, " +
                        "can't send events back to the client applet");
                }

                String[] args = new String[] { json };
                Method m = this.callEventSource.getClass().getMethod("fireEvent",
                    Array.newInstance(String.class, 1).getClass());

                m.invoke(callEventSource, (Object) args);
                try
                {
                    CallPeerSerMapStore.remove(callId, peerId);
                }
                catch(Exception e)
                {
                    // Non-Critical
                    logger.warn("Non-critical Error while trying to remove " +
                        " callId " + callId + ", peerId " + peerId +
                            " from store.", e);
                }
            }
        }
        catch (Exception e)
        {
            logErr(new CallManagerException(call, peer,
                "Error peer changed state details: " +  e.getMessage()),
                    Thread.currentThread().getStackTrace(), true);
        }
    }

    public void callPeerAdded(CallPeerEvent evt)
    {
        Call call = null;
        try
        {
            call = evt.getSourceCall();
            String callId = call.getCallID();
            CallPeer peer = evt.getSourceCallPeer();

            String callSetupId = CallToCallSetupIdStore.get(callId);
            String json = JSONSerializeCall.JSONSerialize(call,
                peer, callSetupId);

            logger.debug("callPeerAdded: Call Peer added with Call ID " +
                callId + ", and peer id " + peer.getPeerID());

            logger.debug("FIRE SERIALIZED : callPeerAdded " + json);

            if (callEventSource == null)
            {
                throw new Exception("Looks like callEventSource does not exist, " +
                    "can't send events back to the client applet");
            }

            String[] args = new String[] { json };
            Method m = this.callEventSource.getClass().getMethod("fireEvent",
                Array.newInstance(String.class, 1).getClass());

            m.invoke(callEventSource, (Object) args);
        }
        catch (Exception e)
        {
            logErr(new CallManagerException(call, null,
                "Error details: " +  e.getMessage()),
                    Thread.currentThread().getStackTrace(), false);
        }
    }

    public void callStateChanged(CallChangeEvent evt)
    {
        try
        {
            logger.debug("CallChangeEvent: with Call ID " +
                evt.getSourceCall().getCallID() +
                    " with value " + evt.getNewValue() +
                        " has " + evt.getSourceCall().getCallPeerCount() +
                            " peer count");
        }
        catch(Exception e)
        {
            logErr(new CallManagerException(evt.getSourceCall(), null,
                "Error peer changed state details: " +  e.getMessage()),
                    Thread.currentThread().getStackTrace(), false);
        }
    }

    private void sendError(CallManagerException cme)
    {
        try
        {
            String json = JSONSerializeCall.getJSONCallError(cme);

            if (callEventSource == null)
            {
                throw new Exception("Looks like callEventSource does not exist, " +
                    "can't send events back to the client applet");
            }

            logger.debug("FIRE SERIALIZED : sendError " + json);

            String[] args = new String[] { json };
            Method m = this.callEventSource.getClass().getMethod("fireEvent",
                Array.newInstance(String.class, 1).getClass());

            m.invoke(callEventSource, (Object) args);
        }
        catch(Exception e)
        {
            logErr(new CallManagerException(null, null,
                "Error peer changed state details: " +  e.getMessage()),
                    Thread.currentThread().getStackTrace(), false);
        }
    }

    private void logWarn(Throwable t, StackTraceElement [] st)
    {
        try
        {
            if (st != null && st.length > 1)
            {
                String methodName = st[1].getMethodName();
                String exName = t.getClass().getName();
                logger.warn(exName + " :: " + methodName + " : ");
                logger.warn(t, t);
            }
        }
        catch(Exception e)
        {
            System.out.println("<<<< COULD NOT LOG WARNING");
        }
    }

    private void logErr(CallManagerException cme,
        StackTraceElement [] st, boolean passToJsClient)
    {
        try
        {
            if (st != null && st.length > 1)
            {
                String methodName = st[1].getMethodName();
                String exName = cme.getClass().getName();
                logger.error(exName + " :: " + methodName + " : ");
                logger.error(cme, cme);
                if (passToJsClient)
                {
                    sendError(
                        new CallManagerException(
                            cme.getCall(), cme.getCallPeer(), cme.getMessage()));
                }
            }
        }
        catch(Exception e)
        {
            System.out.println("<<<< THIS IS REALLY BAD, CAN'T REPORT ERRORS");
        }
    }

    @SuppressWarnings("unused")
    private void printSupportedOperationSet(ProtocolProviderService provider)
    {
        try
        {
            Map<String, OperationSet> m = provider.getSupportedOperationSets();
            Iterator<String> i = m.keySet().iterator();
            while (i.hasNext())
            {
                String key = i.next();
                OperationSet o = m.get(key);
                logger.info("Supported OperationSet :: " + o.toString());
            }
        }
        catch(Exception e)
        {
            logErr(new CallManagerException(null, null,
                "Error peer changed state details: " +  e.getMessage()),
                    Thread.currentThread().getStackTrace(), false);
        }
    }

    public void peerDisplayNameChanged(CallPeerChangeEvent evt)
    {
        // TODO Auto-generated method stub
    }

    public void peerImageChanged(CallPeerChangeEvent evt)
    {
        // TODO Auto-generated method stub
    }
}
