package com.onsip.communicator.impl.applet.login;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.Map;

import javax.swing.Timer;

import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;

import com.onsip.communicator.impl.applet.AppletActivator;
import com.onsip.communicator.impl.applet.call.CallManager;
import com.onsip.communicator.impl.applet.exceptions.RegistrationTimeoutException;
import com.onsip.communicator.util.json.JSONSerializeRegistration;

import net.java.sip.communicator.service.protocol.AccountID;
import net.java.sip.communicator.service.protocol.OperationFailedException;
import net.java.sip.communicator.service.protocol.OperationSetBasicTelephony;
import net.java.sip.communicator.service.protocol.ProtocolNames;
import net.java.sip.communicator.service.protocol.ProtocolProviderFactory;
import net.java.sip.communicator.service.protocol.ProtocolProviderService;
import net.java.sip.communicator.service.protocol.RegistrationState;
import net.java.sip.communicator.service.protocol.SecurityAuthority;
import net.java.sip.communicator.service.protocol.event.RegistrationStateChangeEvent;
import net.java.sip.communicator.service.protocol.event.RegistrationStateChangeListener;
import net.java.sip.communicator.util.Logger;

public class RegistrationManager
    implements RegistrationStateChangeListener, ActionListener
{
    private final static Logger logger
        = Logger.getLogger(RegistrationManager.class);

    public static final String ACCOUNT_PREFIX = "com.onsip.accounts";

    /**
     *  Throw exception if registration times out. The interface should
     *  pick this up and notify the end user
     */
    private static int REGISTER_TIMEOUT = 15000;
    private static int REGISTRATION_COUNTER = 0;
    private Timer registrationTimer = null;

    /**
     * This object will be set by the applet. When events in registration
     * have to be passed through the applet to the javascript front end,
     * this object is the mechanism by which those messages pass thru.
     */
    private Object registrationEventSource = null;

    /**
     * @see CallManager is responsible for handling all the call events.
     * This object will be registered as a listener upon a successful
     * registration
     */
    private CallManager callManager = null;

    public RegistrationManager(CallManager callManager)
    {
        init(callManager);
    }

    private void init(CallManager callManager)
    {
        this.callManager = callManager;
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
                ProtocolProviderService provider = null;
                for (int i = 0; i < sipProviderRefs.length; i++)
                {
                    provider =
                        (ProtocolProviderService) context
                            .getService(sipProviderRefs[i]);
                    if (provider != null)
                    {
                        provider.removeRegistrationStateChangeListener(this);
                    }
                }
            }
        }
        catch (Exception e)
        {
            /* if an error occurs here, we can effectively ignore it */
            logger.warn("Exception :: removeListeners :");
            logger.warn(e, e);
        }
    }

    /**
     * This is set by the applet, and is used to notify the applet of events
     * pertaining to registration
     *
     * @param handler Passes the event over to the applet
     */
    public void setRegistrationEventSource(Object handler)
    {
        this.registrationEventSource = handler;
    }

    public void registrationStateChanged(RegistrationStateChangeEvent evt)
    {
        try
        {
            if (evt.getNewState().equals(RegistrationState.CONNECTION_FAILED) ||
                evt.getNewState().equals(RegistrationState.AUTHENTICATION_FAILED))
            {
                String s = (evt.getReason() == null) ? "" : evt.getReason();
                if (s == null)
                {
                    throw new Exception("Error while registering");
                }
                else
                {
                    throw new Exception(s);
                }
            }

            String json = JSONSerializeRegistration.getJSONRegObj(evt, this);

            logger.debug("FIRE SERIALIZED : registrationStateChanged " + json);

            String[] args = new String[] { json };

            if (registrationEventSource == null)
            {
                throw new Exception(
                    "Looks like registrationEventSource does not exist");
            }

            if (evt.getNewState().equals(RegistrationState.REGISTERED))
            {
                ProtocolProviderService provider = evt.getProvider();
                OperationSetBasicTelephony<?> basicTelephony =
                    provider.getOperationSet(OperationSetBasicTelephony.class);
                basicTelephony.addCallListener(callManager);
            }

            Method m =
                registrationEventSource.getClass().getMethod("fireEvent",
                    Array.newInstance(String.class, 1).getClass());

            m.invoke(registrationEventSource, (Object) args);
        }
        catch (Exception e)
        {
            logger.error("Exception :: registrationStateChanged : ");
            logger.error(e, e);
            if (evt.getProvider() != null)
            {
                sendError(e, this.getAddressOfRecord(evt.getProvider()));
                try
                {
                    // reset registration states
                    this.unregister(evt.getProvider());
                }
                catch(Exception ex2)
                {
                    // do nothing
                }
            }
        }
    }

    protected void unregister(ProtocolProviderService provider)
    {
        try
        {
            if (provider != null)
                provider.unregister();
        }
        catch (OperationFailedException e)
        {
            logger.error("OperationFailedException :: unregister : ");
            logger.error(e, e);
            if (provider != null)
            {
                sendError(e, this.getAddressOfRecord(provider));
            }
        }
        finally
        {
            this.registrationTimer = null;
            REGISTRATION_COUNTER = 0;
        }
    }

    public void unregister(String userId)
    {
        try
        {
            this.unregister(
                AppletActivator.getPrototocolProviderService(userId));
        }
        catch(Exception ex)
        {
            this.registrationTimer = null;
            REGISTRATION_COUNTER = 0;
        }
    }

    private ProtocolProviderFactory getProviderFactory()
    {
        try
        {
            ProtocolProviderFactory sipProviderFactory = null;

            String osgiFilter = "(" +
                ProtocolProviderFactory.PROTOCOL + "=" +
                    ProtocolNames.SIP + ")";

            ServiceReference[] serviceRefs =
                AppletActivator.getBundleContext().getServiceReferences(
                    ProtocolProviderFactory.class.getName(), osgiFilter);

            if (serviceRefs != null && serviceRefs.length > 0)
            {
                sipProviderFactory = (ProtocolProviderFactory)
                    AppletActivator.getBundleContext().getService(serviceRefs[0]);
            }
            return sipProviderFactory;
        }
        catch (InvalidSyntaxException e)
        {
            logger.error("InvalidSyntaxException :: getProviderFactory : ");
            logger.error(e, e);
        }
        catch (Exception e)
        {
            logger.error("Exception :: getProviderFactory : ");
            logger.error(e,e);
        }
        return null;
    }

    private synchronized void unRegisterAll()
    {
        try
        {
            logger
                .info("******************   unRegisterAll *******************");
            ProtocolProviderFactory sipProviderFactory = getProviderFactory();
            ArrayList<AccountID> accounts =
                sipProviderFactory.getRegisteredAccounts();
            Iterator<AccountID> i = accounts.iterator();
            while (i.hasNext())
            {
                AccountID account = i.next();

                String userId =
                    account
                        .getAccountPropertyString(ProtocolProviderFactory.USER_ID);

                ProtocolProviderService provider =
                    AppletActivator.getPrototocolProviderService(userId);

                if (provider != null)
                {
                    provider.unregister();
                    sipProviderFactory.uninstallAccount(account);
                    logger.info("unregistering " + userId);
                }
            }
        }
        catch (Exception e)
        {
            logger.error("Exception :: unRegisterAll : ");
            logger.error(e, e);
        }
    }

    private synchronized AccountID getAccount(String userId)
    {
        try
        {
            ProtocolProviderFactory sipProviderFactory = getProviderFactory();
            ArrayList<AccountID> accounts =
                sipProviderFactory.getRegisteredAccounts();
            Iterator<AccountID> i = accounts.iterator();
            while (i.hasNext())
            {
                AccountID account = i.next();

                String currentUserId =
                    (String) account
                        .getAccountProperty(ProtocolProviderFactory.USER_ID);
                if (currentUserId.equals(userId))
                {
                    return account;
                }
            }
        }
        catch (Exception e)
        {
            logger.error("Exception :: getAccount : ");
            logger.error(e, e);
        }
        return null;
    }

    public synchronized void register(String userId, String displayName,
        String authUsername, String password, String serverAddress,
        String proxyAddress, String proxyPort)
    {
        try
        {
            if (REGISTRATION_COUNTER == 0)
            {
                REGISTRATION_COUNTER++;
                this.unRegisterAll();
            }

            if (registrationTimer == null)
            {
                this.registrationTimer = new Timer(REGISTER_TIMEOUT, this);
                this.registrationTimer.setActionCommand(userId);
                this.registrationTimer.setRepeats(false);
                this.registrationTimer.start();
            }

            ProtocolProviderFactory sipProviderFactory = getProviderFactory();

            /* does the account exist */
            logger.debug("Has an account id been registered with " +
                "provider before one is installed [" + userId + "] : " +
                    (sipProviderFactory.getRegisteredAccounts().size() == 0));

            /* get the details of the account in the form of a hash */
            logger.debug("Fill hash details of the account " +  "[" + userId + "]");
            Hashtable<String, String> sipAccountProperties =
                getAccountProperties(userId, displayName,
                    authUsername, password, serverAddress,
                        proxyAddress, proxyPort);

            AccountID account = null;

            /**
             * Patched 11/28/2012, Oren F.
             * This patch is meant to deal with a bizarre edge case
             * discovered on a single test box running Windows 7.
             * installAccount is called, persisting some of the
             * account details, but in the process references
             * a Jitsi Bundle which the Java cache deems to be
             * out of date. So Java cache goes out and retrieves
             * the bundle, but temporarily disables the Service
             * associated with the Bundle. That causes a null reference
             * exception and an error suggesting that our user agent
             * is now in some weird / illegal state.
             *
             */
            for (int i=0; i < 3; i++)
            {
                account = this.getAccount(userId);
                if (account != null)
                {
                    logger.debug("Since the account exists, we'll just load it");
                    account.setAccountProperties(sipAccountProperties);
                    sipProviderFactory.loadAccount(account);
                    logger.debug("Done loading it");
                    break;
                }
                else
                {
                    try
                    {
                        logger.debug("The account does not exists, so we'll install anew " + userId);
                        sipProviderFactory.installAccount(userId, sipAccountProperties);
                        logger.debug("The account installed");
                        break;
                    }
                    catch(Exception ex)
                    {
                        /**
                         * An error here puts us into that illegal state where the
                         * account is partially filled. When we loop over,
                         * the getAccount function will return a valid account. At least
                         * that's what seems to be happening. We put the thread to sleep
                         * briefly so that Java cache will go out and retrieve
                         * or refresh the Service Bundle (credentials.jar)
                         */
                        logger.debug("install account failed, try set account again");
                        Thread.sleep(1000);
                    }
                }
            }

            ProtocolProviderService provider =
                AppletActivator.getPrototocolProviderService(userId);

            if (provider == null)
            {
                logger.info(ProtocolProviderService.class.toString() +
                    " is NULL, can't register");
                throw new Exception("Unable to register, no account found");
            }

            if (provider.isRegistered())
            {
                logger.debug(userId + " IS REGISTERED :::");
                return;
            }

            /* add listener */
            provider.addRegistrationStateChangeListener(this);

            logger.debug("Init SecurityAuthority");
            SecurityAuthority auth =
                new SecurityAuthorityImpl(password.toCharArray());

            logger.debug("Try to register");
            provider.register(auth);
        }
        catch (OperationFailedException e)
        {
            logger.error("OperationFailedException :: register : ");
            logger.error(e, e);
            sendError(e, userId);
        }
        catch (Exception e)
        {
            logger.error("Exception :: register : ");
            logger.error(e, e);
            sendError(e, userId);
        }
    }

    private Hashtable<String, String> getAccountProperties(String userId,
        String displayName, String authUsername, String password,
            String serverAddress, String proxyAddress, String proxyPort) throws Exception
    {
        BundleContext context = AppletActivator.getBundleContext();

        Hashtable<String, String> table = new Hashtable<String, String>();

        if (displayName == null)
        {
            displayName = "";
        }

        userId = userId.trim();
        displayName = displayName.trim();
        authUsername = authUsername.trim();
        password = password.trim();

        if (userId.length() == 0)
        {
            throw new Exception("Missing required field userId / username");
        }

        if (authUsername.length() == 0)
        {
            throw new Exception("Missing required field authUsername");
        }

        if (displayName.length() == 0)
        {
            displayName = userId;
            displayName = displayName.split("@")[0];
            logger.debug("Display name not provided, we'll take from userId");
        }

        table.put(ProtocolProviderFactory.USER_ID, userId);
        table.put(ProtocolProviderFactory.DISPLAY_NAME, displayName);

        table.put(ProtocolProviderFactory.AUTHORIZATION_NAME, authUsername);
        table.put(ProtocolProviderFactory.PASSWORD, password);

        if (proxyAddress != null && proxyAddress.length() > 0)
        {
            proxyAddress = proxyAddress.trim();
            table.put(ProtocolProviderFactory.PROXY_ADDRESS, proxyAddress);
            logger.debug(proxyAddress + " details came in from the JS side");
        }
        else
        {
            String defaultProxyAddress =
                context.getProperty("com.onsip.communicator.proxy_address");

            /* The global property was set by the applet */
            defaultProxyAddress = System.getProperty(ACCOUNT_PREFIX
                + ProtocolProviderFactory.PROXY_ADDRESS, defaultProxyAddress);

            if (defaultProxyAddress != null && defaultProxyAddress.length() > 0)
            {
                defaultProxyAddress = defaultProxyAddress.trim();
                table.put(ProtocolProviderFactory.PROXY_ADDRESS, defaultProxyAddress);
                logger.debug("Found proxy address in config " + defaultProxyAddress);
            }
        }

        System.setProperty(ProtocolProviderFactory.PROTOCOL,
            ProtocolNames.SIP);

        System.setProperty(ProtocolProviderFactory.FORCE_P2P_MODE,
            Boolean.FALSE.toString());

        System.setProperty(ProtocolProviderFactory.IS_USE_ICE,
                Boolean.FALSE.toString());

        System.setProperty(ProtocolProviderFactory.IS_USE_UPNP,
                Boolean.FALSE.toString());

        System.setProperty(
            ProtocolProviderFactory.PREFERRED_CLEAR_PORT_PROPERTY_NAME,
                "8978");

        System.setProperty(
            ProtocolProviderFactory.PREFERRED_SECURE_PORT_PROPERTY_NAME,
                "8979");

        /**
         * These settings which enable ZRTP caused Polycoms to start emitting
         * really odd noise at the start of the call
         */
        table.put(ProtocolProviderFactory.DEFAULT_ENCRYPTION, "false");
        table.put(ProtocolProviderFactory.DEFAULT_SIPZRTP_ATTRIBUTE, "false");

        /**
         * Setting PROXY_AUTO_CONFIG to "true" forces DNS look ups to
         * follow the path of NAPTR to SRV to A or AAAA
         */
        table.put(ProtocolProviderFactory.PROXY_AUTO_CONFIG, "true");

        /**
         * Disable video
         */
        table.put("net.java.sip.communicator.impl.neomedia.VIDEO_CONFIG_DISABLED", "true");
        table.put("net.java.sip.communicator.impl.neomedia.h264config.DISABLED", "true");
        table.put("net.java.sip.communicator.impl.neomedia.zrtpconfig.DISABLED", "true");
        table.put("net.java.sip.communicator.impl.neomedia.callrecordingconfig.DISABLED", "true");

        return table;
    }

    public String getDisplayName(ProtocolProviderService provider)
    {
        try
        {
            AccountID account = provider.getAccountID();

            Map<String, String> properties = account.getAccountProperties();

            String displayName =
                properties.get(ProtocolProviderFactory.DISPLAY_NAME);
            if (displayName != null)
            {
                return displayName;
            }
        }
        catch (Exception e)
        {
            logger.error("Exception :: getDisplayName :");
            logger.error(e, e);
        }
        return "";
    }

    public String getServerAddress(ProtocolProviderService provider)
    {
        try
        {
            AccountID account = provider.getAccountID();

            Map<String, String> properties = account.getAccountProperties();

            String value =
                properties.get(ProtocolProviderFactory.SERVER_ADDRESS);
            if (value != null)
            {
                return value;
            }
        }
        catch (Exception e)
        {
            logger.error("Exception :: getServerAddress :");
            logger.error(e, e);
        }
        return "";
    }

    public String getProxyAddress(ProtocolProviderService provider)
    {
        try
        {
            AccountID account = provider.getAccountID();

            Map<String, String> properties = account.getAccountProperties();

            String value =
                properties.get(ProtocolProviderFactory.PROXY_ADDRESS);
            if (value != null)
            {
                return value;
            }
        }
        catch (Exception e)
        {
            logger.error("Exception :: getProxyAddress :");
            logger.error(e, e);
        }
        return "";
    }

    public String getUniqueId(ProtocolProviderService provider)
    {
        try
        {
            AccountID account = provider.getAccountID();

            Map<String, String> properties = account.getAccountProperties();

            String value =
                properties.get(ProtocolProviderFactory.ACCOUNT_UID);
            if (value != null)
            {
                return value;
            }
        }
        catch (Exception e)
        {
            logger.error("Exception :: getUniqueId :");
            logger.error(e, e);
        }
        return "";
    }

    public String getProxyPort(ProtocolProviderService provider)
    {
        try
        {
            AccountID account = provider.getAccountID();

            Map<String, String> properties = account.getAccountProperties();

            String value =
                properties.get(ProtocolProviderFactory.PROXY_PORT);
            if (value != null)
            {
                return value;
            }
        }
        catch (Exception e)
        {
            logger.error("Exception :: getProxyPort :");
            logger.error(e, e);
        }
        return "";
    }

    public String getGrantedExpiration(ProtocolProviderService provider)
    {
        if (provider == null)
        {
            return "";
        }
        try
        {
            Method m =
                provider.getClass().getMethod(
                    "getGrantedRegistrationExpiration", new Class[0]);
            Object expires = m.invoke(provider, new Object[0]);
            return expires.toString();
        }
        catch (SecurityException e)
        {
            logger.error("SecurityException :: getGrantedExpiration : ");
            logger.error(e, e);
        }
        catch (NoSuchMethodException e)
        {
            logger.error("NoSuchMethodException :: getGrantedExpiration : ");
            logger.error(e, e);
        }
        catch (IllegalArgumentException e)
        {
            logger.error("IllegalArgumentException :: getGrantedExpiration : ");
            logger.error(e, e);
        }
        catch (IllegalAccessException e)
        {
            logger.error("IllegalAccessException :: getGrantedExpiration : ");
            logger.error(e, e);
        }
        catch (InvocationTargetException e)
        {
            logger.error("InvocationTargetException :: getGrantedExpiration :");
            logger.error(e, e);
        }

        return "";
    }

    public String getAddressOfRecord(ProtocolProviderService provider)
    {
        if (provider == null)
        {
            return "";
        }
        try
        {
            Method m =
                provider.getClass().getMethod("getAddressOfRecord",
                    new Class[0]);
            return (String) m.invoke(provider, new Object[0]);
        }
        catch (SecurityException e)
        {
            logger.error("SecurityException :: getAddressOfRecord : ");
            logger.error(e, e);
        }
        catch (NoSuchMethodException e)
        {
            logger.error("NoSuchMethodException :: getAddressOfRecord : ");
            logger.error(e, e);
        }
        catch (IllegalArgumentException e)
        {
            logger.error("IllegalArgumentException :: getAddressOfRecord : ");
            logger.error(e, e);
        }
        catch (IllegalAccessException e)
        {
            logger.error("IllegalAccessException :: getAddressOfRecord : ");
            logger.error(e, e);
        }
        catch (InvocationTargetException e)
        {
            logger.error("InvocationTargetException :: getAddressOfRecord : ");
            logger.error(e, e);
        }
        return "";
    }

    public int getRegisteredAccountsCount()
    {
        try
        {
            ProtocolProviderFactory sipProviderFactory = getProviderFactory();
            if (sipProviderFactory != null)
            {
                return sipProviderFactory.getRegisteredAccounts().size();
            }
        }
        catch(Exception e)
        {
            logger.error("Exception :: getRegisteredAccountsCount : ");
            logger.error(e, e);
        }
        return 0;
    }

    private void sendError(Throwable t, String userId)
    {
        try
        {
            String json = JSONSerializeRegistration.getJSONRegError(t, userId);

            logger.debug("FIRE SERIALIZED : sendError " + json);

            if (registrationEventSource == null)
            {
                throw new Exception
                    ("Looks like registrationEventSource does not exist");
            }

            String[] args = new String[] { json };

            Method m = registrationEventSource.getClass().getMethod("fireEvent",
                Array.newInstance(String.class, 1).getClass());

            m.invoke(registrationEventSource, (Object) args);
        }
        catch(Exception e)
        {
            logger.error("Exception :: sendError :");
            logger.error(e, e);
        }
    }

    public void actionPerformed(ActionEvent e)
    {
        try
        {
            String userId = e.getActionCommand();
            if (userId != null && userId.length() > 0)
            {
                ProtocolProviderService provider =
                    AppletActivator.getPrototocolProviderService(userId);

                if (provider != null)
                {
                    if (provider.getRegistrationState().equals(RegistrationState.REGISTERING))
                    {
                        this.sendError(new RegistrationTimeoutException(),
                            e.getActionCommand() != null ? e.getActionCommand() : "");
                    }
                    else if (provider.getRegistrationState().equals(RegistrationState.REGISTERED))
                    {
                        // print details of all registered accounts
                        printRegisteredAccounts();
                    }
                }
            }
        }
        catch(Exception ex)
        {
            logger.error("Exception :: actionPerformed :");
            logger.error(ex, ex);
        }
    }

    private void printRegisteredAccounts()
    {
        ProtocolProviderFactory sipProviderFactory = getProviderFactory();

        if (sipProviderFactory != null)
        {
            ArrayList<AccountID> accounts =
                sipProviderFactory.getRegisteredAccounts();
            ListIterator<AccountID> i = accounts.listIterator();
            logger.info("===================================================");
            while (i.hasNext())
            {
                AccountID account = i.next();
                logger.info("Account Unique ID :: " + account.getAccountUniqueID());
                logger.info("User ID :: " + account.getUserID());
                logger.info("Display Name :: " + account.getDisplayName());
                logger.info("Protocol Display Name :: " + account.getProtocolDisplayName());
                logger.info("Protocol Name :: " + account.getProtocolName());
                logger.info("Service :: " + account.getService());
                Map<String,String> m =
                    account.getAccountProperties();
                Iterator<String> keys = m.keySet().iterator();
                logger.info("| Account Properties");
                logger.info("-----------------------------------------------");
                while(keys.hasNext())
                {
                    String k = keys.next();
                    if (k.contains(ProtocolProviderFactory.PASSWORD))
                    {
                        logger.info("| " + k + " --> **************** ");
                    }
                    else
                    {
                        logger.info("| " + k + " --> " + m.get(k));
                    }
                }
                logger.info("================================================");
            }
        }
    }

}
