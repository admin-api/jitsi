/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 * 
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.msnaccregwizz;

import java.awt.*;
import java.util.*;

import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.protocol.*;

import org.osgi.framework.*;

/**
 * The <tt>MsnAccountRegistrationWizard</tt> is an implementation of the
 * <tt>AccountRegistrationWizard</tt> for the Msn protocol. It should allow
 * the user to create and configure a new Msn account.
 * 
 * @author Yana Stamcheva
 */
public class MsnAccountRegistrationWizard
    implements AccountRegistrationWizard
{

    private FirstWizardPage firstWizardPage;

    private MsnAccountRegistration registration = new MsnAccountRegistration();

    private WizardContainer wizardContainer;

    private ProtocolProviderService protocolProvider;

    private String propertiesPackage =
        "net.java.sip.communicator.plugin.msnaccregwizz";

    private boolean isModification;

    /**
     * Creates an instance of <tt>MsnAccountRegistrationWizard</tt>.
     * 
     * @param wizardContainer the wizard container, where this wizard is added
     */
    public MsnAccountRegistrationWizard(WizardContainer wizardContainer)
    {
        this.wizardContainer = wizardContainer;

        this.wizardContainer
            .setFinishButtonText(Resources.getString("service.gui.SIGN_IN"));
    }

    /**
     * Implements the <code>AccountRegistrationWizard.getIcon</code> method.
     * Returns the icon to be used for this wizard.
     * 
     * @return byte[]
     */
    public byte[] getIcon()
    {
        return Resources.getImage(Resources.MSN_LOGO);
    }

    /**
     * Implements the <code>AccountRegistrationWizard.getPageImage</code>
     * method. Returns the image used to decorate the wizard page
     * 
     * @return byte[] the image used to decorate the wizard page
     */
    public byte[] getPageImage()
    {
        return Resources.getImage(Resources.PAGE_IMAGE);
    }

    /**
     * Implements the <code>AccountRegistrationWizard.getProtocolName</code>
     * method. Returns the protocol name for this wizard.
     * 
     * @return String
     */
    public String getProtocolName()
    {
        return Resources.getString("plugin.msnaccregwizz.PROTOCOL_NAME");
    }

    /**
     * Implements the <code>AccountRegistrationWizard.getProtocolDescription
     * </code>
     * method. Returns the description of the protocol for this wizard.
     * 
     * @return String
     */
    public String getProtocolDescription()
    {
        return Resources.getString("plugin.msnaccregwizz.PROTOCOL_DESCRIPTION");
    }

    /**
     * Returns the set of pages contained in this wizard.
     * 
     * @return Iterator
     */
    public Iterator getPages()
    {
        ArrayList pages = new ArrayList();
        firstWizardPage = new FirstWizardPage(this);

        pages.add(firstWizardPage);

        return pages.iterator();
    }

    /**
     * Returns the set of data that user has entered through this wizard.
     * 
     * @return Iterator
     */
    public Iterator getSummary()
    {
        Hashtable summaryTable = new Hashtable();

        summaryTable.put(   Resources.getString("plugin.msnaccregwizz.USERNAME"),
                            registration.getId());
        summaryTable.put(   Resources.getString("service.gui.REMEMBER_PASSWORD"),
                            new Boolean(registration.isRememberPassword()));

        return summaryTable.entrySet().iterator();
    }

    /**
     * Installs the account created through this wizard.
     * 
     * @return ProtocolProviderService corresponding to the newly created
     * account.
     */
    public ProtocolProviderService signin()
    {
        if (!firstWizardPage.isCommitted())
            firstWizardPage.commitPage();

        return signin(  registration.getId(),
                        registration.getPassword());
    }

    /**
     * Installs the account created through this wizard.
     * 
     * @return ProtocolProviderService corresponding to the newly created
     * account.
     */
    public ProtocolProviderService signin(String userName, String password)
    {
        firstWizardPage = null;
        ProtocolProviderFactory factory =
            MsnAccRegWizzActivator.getMsnProtocolProviderFactory();

        return this.installAccount( factory,
                                    userName,
                                    password);
    }

    /**
     * Creates an account for the given user and password.
     * 
     * @param providerFactory the ProtocolProviderFactory which will create the
     *            account
     * @param user the user identifier
     * @param passwd the password
     * @return the <tt>ProtocolProviderService</tt> for the new account.
     */
    public ProtocolProviderService installAccount(
        ProtocolProviderFactory providerFactory, String user, String passwd)
    {

        Hashtable accountProperties = new Hashtable();

        if (registration.isRememberPassword())
        {
            accountProperties.put(ProtocolProviderFactory.PASSWORD, passwd);
        }

        if (isModification)
        {
            providerFactory.modifyAccount(  protocolProvider,
                accountProperties);

            this.isModification  = false;

            return protocolProvider;
        }

        try
        {
            AccountID accountID =
                providerFactory.installAccount(user, accountProperties);

            ServiceReference serRef =
                providerFactory.getProviderForAccount(accountID);

            protocolProvider =
                (ProtocolProviderService) MsnAccRegWizzActivator.bundleContext
                    .getService(serRef);
        }
        catch (IllegalArgumentException e)
        {
            MsnAccRegWizzActivator.getUIService().getPopupDialog()
                .showMessagePopupDialog(e.getMessage(),
                    Resources.getString("service.gui.ERROR"),
                    PopupDialog.ERROR_MESSAGE);
        }
        catch (IllegalStateException e)
        {
            MsnAccRegWizzActivator.getUIService().getPopupDialog()
                .showMessagePopupDialog(e.getMessage(),
                    Resources.getString("service.gui.ERROR"),
                    PopupDialog.ERROR_MESSAGE);
        }

        return protocolProvider;
    }

    /**
     * Fills the UIN and Password fields in this panel with the data coming
     * from the given protocolProvider.
     * 
     * @param protocolProvider The <tt>ProtocolProviderService</tt> to load
     *            the data from.
     */
    public void loadAccount(ProtocolProviderService protocolProvider)
    {
        this.isModification = true;

        this.protocolProvider = protocolProvider;

        this.registration = new MsnAccountRegistration();

        this.firstWizardPage.loadAccount(protocolProvider);
    }

    /**
     * Indicates if this wizard is opened for modification or for creating a
     * new account.
     * 
     * @return <code>true</code> if this wizard is opened for modification and
     * <code>false</code> otherwise.
     */
    public boolean isModification()
    {
        return isModification;
    }

    /**
     * Returns the wizard container, where all pages are added.
     * 
     * @return the wizard container, where all pages are added
     */
    public WizardContainer getWizardContainer()
    {
        return wizardContainer;
    }

    /**
     * Returns the registration object, which will store all the data through
     * the wizard.
     * 
     * @return the registration object, which will store all the data through
     * the wizard
     */
    public MsnAccountRegistration getRegistration()
    {
        return registration;
    }
    
    /**
     * Returns the size of this wizard.
     * @return the size of this wizard
     */
    public Dimension getSize()
    {
        return new Dimension(600, 500);
    }
    
    /**
     * Returns the identifier of the page to show first in the wizard.
     * @return the identifier of the page to show first in the wizard.
     */
    public Object getFirstPageIdentifier()
    {
        return firstWizardPage.getIdentifier();
    }

    /**
     * Returns the identifier of the page to show last in the wizard.
     * @return the identifier of the page to show last in the wizard.
     */
    public Object getLastPageIdentifier()
    {
        return firstWizardPage.getIdentifier();
    }

    /**
     * Sets the modification property to indicate if this wizard is opened for
     * a modification.
     * 
     * @param isModification indicates if this wizard is opened for modification
     * or for creating a new account. 
     */
    public void setModification(boolean isModification)
    {
        this.isModification = isModification;
    }

    /**
     * Returns an example string, which should indicate to the user how the
     * user name should look like.
     * @return an example string, which should indicate to the user how the
     * user name should look like.
     */
    public String getUserNameExample()
    {
        return FirstWizardPage.USER_NAME_EXAMPLE;
    }

    /**
     * Enables the simple "Sign in" form.
     */
    public boolean isSimpleFormEnabled()
    {
        return true;
    }

    /**
     * Opens the browser on the registration page for MSN.
     */
    public void webSignup()
    {
        MsnAccRegWizzActivator.getBrowserLauncher()
            .openURL("https://accountservices.passport.net/reg.srf");
    }

    /**
     * Returns <code>true</code> if the web sign up is supported by the current
     * implementation, <code>false</code> - otherwise.
     * @return <code>true</code> if the web sign up is supported by the current
     * implementation, <code>false</code> - otherwise
     */
    public boolean isWebSignupSupported()
    {
        return true;
    }

    public Object getSimpleForm()
    {
        firstWizardPage = new FirstWizardPage(this);
        return firstWizardPage.getSimpleForm();
    }
}
