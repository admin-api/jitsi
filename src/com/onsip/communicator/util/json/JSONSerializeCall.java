package com.onsip.communicator.util.json;

import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Vector;

import net.java.sip.communicator.service.protocol.AccountID;
import net.java.sip.communicator.service.protocol.Call;
import net.java.sip.communicator.service.protocol.CallPeer;
import net.java.sip.communicator.service.protocol.CallPeerState;
import net.java.sip.communicator.service.protocol.CallState;
import net.java.sip.communicator.service.protocol.OperationSetBasicTelephony;
import net.java.sip.communicator.service.protocol.ProtocolProviderFactory;
import net.java.sip.communicator.service.protocol.ProtocolProviderService;
import net.java.sip.communicator.util.Logger;

import com.onsip.communicator.impl.applet.AppletActivator;
import com.onsip.communicator.impl.applet.exceptions.CallManagerException;
import com.onsip.communicator.impl.applet.utils.CallPeerSerMapStore;
import com.onsip.communicator.util.codec.Codec;


public class JSONSerializeCall
{
    private final static Logger logger
        = Logger.getLogger(JSONSerializeCall.class);

    static Map<String, String> getCallMap(Call call)
        throws JSONException
    {
        Map<String,String> callMap = new HashMap<String,String>();

        /**
         * Returns the id of the specified Call.
         * @return a String uniquely identifying the call.
         */
        String callUniqueId = call.getCallID();
        callMap.put("id", callUniqueId);

        /**
         * Returns the number of peers currently associated with this call.
         *
         * @return an <tt>int</tt> indicating the number of peers currently
         *         associated with this call.
         */
        int callPeerCount = call.getCallPeerCount();
        callMap.put("count", "" + callPeerCount);

        /**
         * Returns the state that this call is currently in.
         *
         * @return a reference to the <tt>CallState</tt> instance that the call is
         *         currently in.
         */
        CallState callState = call.getCallState();
        callMap.put("state", callState.getStateString());

        callMap.put("volume-input",
            String.valueOf(AppletActivator.getInputVolume()));
        callMap.put("volume-playback",
            String.valueOf(AppletActivator.getOutputVolume()));

        /**
         * In this asynchronous architecture where we can
         * register multiple lines / user agents, we need to
         * return to our clients the aor / userId associated
         * with this userAgent. This would allow consumers
         * of the applet the ability to tie the call
         * (especially incoming calls)
         * to a specific line / userAgent.
         */
        if (call.getProtocolProvider() != null)
        {
            AccountID account = call.getProtocolProvider().getAccountID();
            if (account != null)
            {
                Map<String, String> properties =
                    account.getAccountProperties();
                String userId =
                    properties.get(ProtocolProviderFactory.USER_ID);
                callMap.put("aor", userId);
            }
        }

        return callMap;
    }

    static Map<String, Object> getCallPeerMap(CallPeer peer)
    {
        Map<String,Object> peerMap = new HashMap<String,Object>();

        /**
        * Returns a unique identifier representing this peer. Identifiers
        * returned by this method should remain unique across calls. In other
        * words, if it returned the value of "A" for a given peer it should
        * not return that same value for any other peer and return a
        * different value even if the same person (address) is participating in
        * another call. Values need not remain unique after restarting the program.
        **/
        String peerID = peer.getPeerID();
        if (peerID != null)
        {
            peerMap.put("id", peerID);
        }

        /**
         * Returns a String locator for that peer. A locator might be a SIP
         * URI, an IP address or a telephone number.
         * @return the peer's address or phone number.
         */
        String peerAddress = peer.getAddress();
        if (peerAddress != null)
        {
            peerMap.put("address", peerAddress);
        }

        String localUri = peer.getLocalURI();
        boolean isServer = peer.isServer();

        String fromOrToLocal = isServer ? "to-uri" : "from-uri";
        String fromOrToRemote = isServer ? "from-uri" : "to-uri";
        if (localUri != null)
        {
            peerMap.put(fromOrToLocal, localUri);
        }

        String remoteUri = peer.getRemoteURI();
        if (remoteUri != null)
        {
            peerMap.put(fromOrToRemote, remoteUri);
        }

        String callId = peer.getId();
        if (callId != null)
        {
            peerMap.put("sip-cid", callId);
        }

        /**
         * Returns an object representing the current state of that peer.
         * CallPeerState may vary among CONNECTING, RINGING, CALLING, BUSY,
         * CONNECTED, and others, and it reflects the state of the connection between
         * us and that peer.
         * @return a CallPeerState instance representing the peer's
         * state.
         */
        CallPeerState callPeerState = peer.getState();
        if (callPeerState != null)
        {   String peerState = callPeerState.getStateString();
            peerMap.put("state", peerState);
        }

        String displayName = peer.getDisplayName();
        if (displayName != null)
        {
            int idx = displayName.indexOf(";");
            if (idx != -1)
            {
                displayName = displayName.substring(0, idx);
            }
            peerMap.put("display_name", displayName);
        }

        /**
         * Gets the time at which this <tt>CallPeer</tt> transitioned
         * into a state (likely {@link CallPeerState#CONNECTED}) marking the
         * start of the duration of the participation in a <tt>Call</tt>.
         *
         * @return the time at which this <tt>CallPeer</tt> transitioned
         *         into a state marking the start of the duration of the
         *         participation in a <tt>Call</tt> or
         *         {@link #CALL_DURATION_START_TIME_UNKNOWN} if such a transition
         *         has not been performed
         */
        long peerCallDuration = peer.getCallDurationStartTime();
        peerMap.put("duration", "" + peerCallDuration);

        /**
         * Determines whether the audio stream (if any) being sent to this
         * peer is mute.
         *
         * @return <tt>true</tt> if an audio stream is being sent to this
         *         peer and it is currently mute; <tt>false</tt>, otherwise
         */

        ProtocolProviderService provider = peer.getProtocolProvider();
        boolean onCallHold = false;
        if (provider != null && peer.getCall() != null)
        {
            OperationSetBasicTelephony<?> basicTelephony =
                provider.getOperationSet(OperationSetBasicTelephony.class);
            onCallHold = basicTelephony.isMute(peer.getCall());
        }

        peerMap.put("mute", "" + onCallHold);

        /**
         * Gets the number of <tt>ConferenceMember</tt>s currently known to this
         * peer if it is acting as a conference focus.
         *
         * @return the number of <tt>ConferenceMember</tt>s currently known to
         *         this peer if it is acting as a conference focus. If this
         *         peer is not acting as a conference focus or it does but
         *         there are currently no members in the conference it manages, a
         *         value of zero is returned.
         */
        int peerConferenceCount = peer.getConferenceMemberCount();
        peerMap.put("conference_count", "" + peerConferenceCount);

        JSONArray hold = new JSONArray();

        CallPeerState cpState = peer.getState();

        if (cpState == CallPeerState.ON_HOLD_LOCALLY ||
            cpState == CallPeerState.ON_HOLD_MUTUALLY)
        {
            logger.debug("set locally on hold");
            hold.put("local");
        }
        if(cpState == CallPeerState.ON_HOLD_REMOTELY ||
           cpState == CallPeerState.ON_HOLD_MUTUALLY)
        {
            logger.debug("set remotely on hold");
            hold.put("remote");
        }

        logger.debug("hold has " + hold.length() + " values ");
        peerMap.put("hold", hold);

        /**
         * Returns a URL pointing to a location with call control information or
         * null if such an URL is not available for the current call peer.
         *
         * @return a URL link to a location with call information or a call control
         * web interface related to this peer or <tt>null</tt> if no such URL
         * is available.
         */
        URL callPeerUrl = peer.getCallInfoURL();
        if (callPeerUrl != null)
        {
            String peerUrl = callPeerUrl.toString();
            peerMap.put("call_peer_url", peerUrl);
        }

        return peerMap;
    }

    static JSONObject getJSONCallObj(Call call)
        throws JSONException
    {
        // The JSON call
        JSONObject jsonCall = new JSONObject();
        if (call != null)
        {
            Map<String, String> callMap = getCallMap(call);
            jsonCall.put("call", callMap);
        }
        return jsonCall;
    }

    static String getType(Call call, Iterator<? extends CallPeer> peers)
    {
        CallState callState = call.getCallState();
        logger.debug("Before getting call peers");
        logger.debug("Call state = " + callState.getStateString());
        if (callState == CallState.CALL_INITIALIZATION)
        {
            while (peers.hasNext())
            {
                CallPeer peer = peers.next();
                CallPeerState cpState = peer.getState();
                logger.debug("Peer call state = " + cpState.getStateString());

                if (cpState.getStateString() == CallPeerState._CONNECTING ||
                    cpState.getStateString() == CallPeerState._ALERTING_REMOTE_SIDE)
                {
                    return "created";
                }
                else if(cpState.getStateString() == CallPeerState._INCOMING_CALL ||
                    cpState.getStateString() == CallPeerState._CONNECTING_WITH_EARLY_MEDIA)
                {
                    return "requested";
                }
                else if (cpState == CallPeerState.INITIATING_CALL)
                {
                    return "created";
                }
                else if (cpState == CallPeerState.DISCONNECTED)
                {
                    return "terminated";
                }
            }
        }
        else if (callState == CallState.CALL_ENDED)
        {
            return "terminated";
        }
        else if (callState == CallState.CALL_REFERRED)
        {
            return "terminated";
        }
        else if (callState == CallState.CALL_IN_PROGRESS)
        {
            while (peers.hasNext())
            {
                CallPeer peer = peers.next();
                CallPeerState cpState = peer.getState();
                if (cpState == CallPeerState.ON_HOLD_LOCALLY)
                {
                    //return "on-hold-locally";
                    return "confirmed";
                }
                else if(cpState == CallPeerState.ON_HOLD_MUTUALLY)
                {
                    //return "on-hold-mutually";
                    return "confirmed";
                }
                else if(cpState == CallPeerState.ON_HOLD_REMOTELY)
                {
                    //return "on-hold-remotely";
                    return "confirmed";
                }
                else if(cpState == CallPeerState.CONNECTED)
                {
                    return "confirmed";
                }
                else if (cpState == CallPeerState.INITIATING_CALL)
                {
                    return peer.getState().getStateString().toLowerCase();
                }
                else if(cpState == CallPeerState.BUSY)
                {
                    return peer.getState().getStateString().toLowerCase();
                }
                else if(cpState == CallPeerState.FAILED)
                {
                    return peer.getState().getStateString().toLowerCase();
                }
                else if(cpState == CallPeerState.DISCONNECTED)
                {
                    if (call.getCallPeerCount() == 0)
                    {
                        return "terminated";
                    }
                    // call.getCallPeerCount() > 0
                    return "confirmed";
                }
                else if(cpState == CallPeerState.UNKNOWN)
                {
                    if (call.getCallPeerCount() > 0)
                    {
                        return "confirmed";
                    }
                }
            }
        }
        return "";
    }

    static String getTypeForPeer(CallPeer peer)
    {
        CallPeerState cpState = peer.getState();
        if(cpState == CallPeerState.BUSY)
        {
            return peer.getState().getStateString().toLowerCase();
        }
        else if(cpState == CallPeerState.FAILED)
        {
            return peer.getState().getStateString().toLowerCase();
        }
        return "";
    }

    public static String JSONSerialize(CallPeer peer)
        throws JSONException
    {
        Map<String,Object> peerMap = getCallPeerMap(peer);
        JSONObject json = new JSONObject(peerMap);
        return json.toString();
    }

    public static String JSONSerialize(Call call,
        Iterator<? extends CallPeer> iterator)
            throws JSONException
    {
        return JSONSerialize(call, iterator, null);
    }

    public static String JSONSerialize
        (Call call, Iterator<? extends CallPeer> iterator, String callSetupId)
            throws JSONException
    {
        JSONObject jsonCallDetails = null;
        // add call to details
        if (call != null)
        {
            jsonCallDetails = getJSONCallObj(call);
        }
        else
        {
            jsonCallDetails = new JSONObject();
            jsonCallDetails.put("call", "");
        }

        while(iterator.hasNext())
        {
            CallPeer peer = iterator.next();
            Map<String,Object> peerMap = getCallPeerMap(peer);
            String codec = Codec.getCodec(call, peer.getPeerID());
            peerMap.put("codec", codec);
            // add peers to details
            jsonCallDetails.append("peers", peerMap);
        }

        JSONObject jsonCall = new JSONObject();
        jsonCall.accumulate("package", "call");
        if (call != null)
        {
            jsonCall.accumulate("type", getType(call, call.getCallPeers()));
            jsonCall.accumulate("callId", call.getCallID());
            if (callSetupId != null)
            {
                jsonCall.accumulate("callSetupId", callSetupId);
            }
            else
            {
                jsonCall.accumulate("callSetupId", "");
            }
        } else {
            jsonCall.accumulate("type", "terminated");
        }

        jsonCall.accumulate("details", jsonCallDetails);

        // log
        logger.info("JSON : " + jsonCall.toString());
        return jsonCall.toString();
    }

    public static String JSONSerialize(Call call, CallPeer peer)
        throws JSONException
    {
        return JSONSerialize(call, peer, null);
    }

    public static String JSONSerialize
        (Call call, CallPeer peer, String callSetupId)
            throws JSONException
    {
        JSONObject jsonCallDetails = new JSONObject();
        if (call != null)
        {
            jsonCallDetails = getJSONCallObj(call);
        }
        else
        {
            jsonCallDetails = new JSONObject();
            jsonCallDetails.put("call", "");
        }

        Map<String,Object> peerMap = getCallPeerMap(peer);
        jsonCallDetails.append("peers", peerMap);
        if (call != null)
        {
            String codec = Codec.getCodec(call, peer.getPeerID());
            peerMap.put("codec", codec);
        }

        JSONObject jsonCall = new JSONObject();
        jsonCall.accumulate("package", "call");
        if (call != null)
        {
            Vector<CallPeer> callPeers = new Vector<CallPeer>(1);
            callPeers.add(peer);
            jsonCall.accumulate("type", getType(call, callPeers.iterator()));
            jsonCall.accumulate("callId", call.getCallID());
            if (callSetupId != null)
            {
                jsonCall.accumulate("callSetupId", callSetupId);
            }
            else
            {
                jsonCall.accumulate("callSetupId", "");
            }
        }
        else
        {
            jsonCall.accumulate("type", "terminated");
        }

        jsonCall.accumulate("details", jsonCallDetails);

        // log
        logger.info("JSON : " + jsonCall.toString());
        return jsonCall.toString();
    }

    public static String JSONSerialize
        (Call call, CallPeerSerMapStore[] peerSer, String callSetupId)
            throws JSONException
    {
        JSONObject jsonCallDetails = null;
        if (call != null)
        {
            jsonCallDetails = getJSONCallObj(call);
        }
        else
        {
            jsonCallDetails = new JSONObject();
            jsonCallDetails.put("call", "");
        }

        JSONArray serA = new JSONArray();
        for (int i=0; i < peerSer.length; i++)
        {
            String ser = peerSer[i].getPeerSerialized();
            JSONObject j = new JSONObject(ser);
            serA.put(j);
        }
        jsonCallDetails.put("peers", serA);

        JSONObject jsonCall = new JSONObject();
        jsonCall.accumulate("package", "call");
        if (call != null)
        {
            Vector<CallPeer> callPeers = new Vector<CallPeer>(0);
            jsonCall.accumulate("type", getType(call, callPeers.iterator()));
            jsonCall.accumulate("callId", call.getCallID());
        }
        else
        {
            jsonCall.accumulate("type", "terminated");
        }

        if (callSetupId != null)
        {
            jsonCall.accumulate("callSetupId", callSetupId);
        }
        else
        {
            jsonCall.accumulate("callSetupId", "");
        }

        jsonCall.accumulate("details", jsonCallDetails);

        // log
        logger.info("JSON : " + jsonCall.toString());
        return jsonCall.toString();
    }

    public static String getJSONCallError(CallManagerException cme)
        throws JSONException
    {
        JSONObject json = new JSONObject();

        json.accumulate("package", "call");
        json.accumulate("type", "error");

        JSONObject jsonCallDetails = new JSONObject();
        Call call = cme.getCall();
        CallPeer peer = cme.getCallPeer();

        if (call != null)
        {
            jsonCallDetails = getJSONCallObj(call);
        }
        else
        {
            jsonCallDetails = new JSONObject();
            jsonCallDetails.put("call", "");
        }

        if (peer != null)
        {
            Map<String,Object> peerMap = getCallPeerMap(peer);
            jsonCallDetails.append("peers", peerMap);
            if (call != null)
            {
                String codec = Codec.getCodec(call, peer.getPeerID());
                peerMap.put("codec", codec);
            }
        }
        else
        {
            jsonCallDetails.append("peers", "");
        }

        jsonCallDetails.put("message", cme.getMessage());
        json.accumulate("details", jsonCallDetails);

        return json.toString();
    }
}
