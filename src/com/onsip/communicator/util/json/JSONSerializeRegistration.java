package com.onsip.communicator.util.json;

import java.util.HashMap;
import java.util.Map;

import com.onsip.communicator.impl.applet.AppletActivator;
import com.onsip.communicator.impl.applet.login.RegistrationManager;

import net.java.sip.communicator.service.protocol.ProtocolProviderService;
import net.java.sip.communicator.service.protocol.event.RegistrationStateChangeEvent;
import net.java.sip.communicator.util.Logger;

public class JSONSerializeRegistration
{
    private final static Logger logger
        = Logger.getLogger(JSONSerializeRegistration.class);

    static Map<String, String> getRegistrationMap
        (RegistrationStateChangeEvent evt,
            RegistrationManager reg)
    {
        Map<String,String> map = new HashMap<String,String>();

        String newState = "";
        if (evt.getNewState() != null)
        {
            newState = evt.getNewState().getStateName();
        }

        ProtocolProviderService provider = evt.getProvider();
        map.put("new_state", newState);
        map.put("aor", reg.getAddressOfRecord(provider));
        map.put("expires", reg.getGrantedExpiration(provider));
        map.put("display_name", reg.getDisplayName(provider));
        map.put("proxy", reg.getProxyAddress(provider));
        map.put("recovery_mode", reg.isTryingToRecover());

        map.put("volume-input",
            String.valueOf(AppletActivator.getInputVolume()));
        map.put("volume-playback",
            String.valueOf(AppletActivator.getOutputVolume()));

        return map;
    }

    static String getType(RegistrationStateChangeEvent evt)
    {
        String type = evt.getNewState().getStateName().toLowerCase();
        if (type == null)
        {
            return "";
        }
        return type;
    }

    public static String getJSONRegObj(RegistrationStateChangeEvent evt,
        RegistrationManager registration)
            throws JSONException
    {
        JSONObject json = new JSONObject();
        JSONObject jsonReg = new JSONObject();
        if (evt != null)
        {
            Map<String, String> regMap = getRegistrationMap(evt, registration);
            jsonReg.put("registration", regMap);
        }

        json.put("package", "registration");
        json.put("type", getType(evt));
        json.put("details", jsonReg);

        return json.toString();
    }

    public static String getJSONRegError(Throwable t, String userId)
        throws JSONException
    {
        JSONObject json = new JSONObject();

        json.put("package", "registration");
        json.put("type", "error");

        Map<String,String> map = new HashMap<String,String>();

        map.put("new_state", "error");
        map.put("message", t.toString());
        map.put("aor", userId);
        map.put("expires", "");
        map.put("display_name", "");
        map.put("proxy", "");
        map.put("volume-input", "");
        map.put("volume-playback", "");

        JSONObject jsonReg = new JSONObject();
        jsonReg.put("registration", map);
        json.put("details", jsonReg);

        return json.toString();
    }
}
