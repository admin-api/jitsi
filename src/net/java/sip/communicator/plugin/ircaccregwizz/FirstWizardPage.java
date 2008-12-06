/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.ircaccregwizz;

import java.util.*;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.*;
import javax.swing.event.*;

import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.swing.*;

/**
 * The <tt>FirstWizardPage</tt> is the page, where user could enter the user ID
 * and the password of the account.
 *
 * @author Lionel Ferreira & Michael Tarantino
 */
public class FirstWizardPage
    extends TransparentPanel
    implements  WizardPage,
                DocumentListener,
                ActionListener
{
    /**
     * The identifier of this wizard page.
     */
    public static final String FIRST_PAGE_IDENTIFIER = "FirstPageIdentifier";

    public static final String USER_NAME_EXAMPLE = "Ex: ircuser";

    public static final String SERVER_EXAMPLE = "Ex: irc.quakenet.org";

    private JPanel userPassPanel = new TransparentPanel(new BorderLayout(10, 10));

    private JPanel serverPanel = new TransparentPanel(new BorderLayout(10, 10));

    private JPanel optionsPanel = new TransparentPanel(new BorderLayout(10, 10));

    private JPanel labelsPanel = new TransparentPanel();

    private JPanel valuesPanel = new TransparentPanel();

    private JPanel labelsServerPanel = new TransparentPanel();

    private JPanel valuesServerPanel = new TransparentPanel();

    private JPanel labelsOptionsPanel = new TransparentPanel();

    private JPanel valuesOptionsPanel = new TransparentPanel();

    private JLabel infoPassword
        = new JLabel(Resources.getString("infoPassword"));

    private JLabel nick = new JLabel(Resources.getString("nick"));

    private JLabel passLabel = new JLabel(Resources.getString("password"));

    private JLabel server = new JLabel(Resources.getString("server"));

    private JLabel port = new JLabel(Resources.getString("port"));

    private JLabel existingAccountLabel
        = new JLabel(Resources.getString("existingAccount"));

    private JPanel emptyPanel = new TransparentPanel();

    private JPanel emptyPanel2 = new TransparentPanel();

    private JLabel nickExampleLabel = new JLabel(USER_NAME_EXAMPLE);

    private JLabel serverExampleLabel = new JLabel(SERVER_EXAMPLE);

    private JTextField nickField = new JTextField();

    private JPasswordField passField = new JPasswordField();

    private JTextField serverField = new JTextField();

    private JTextField portField = new JTextField();

    private JCheckBox rememberPassBox =
        new SIPCommCheckBox(Resources.getString("rememberPassword"));

    private JCheckBox autoNickChange =
        new SIPCommCheckBox(Resources.getString("autoNickChange"));

    private JCheckBox defaultPort =
        new SIPCommCheckBox(Resources.getString("defaultPort"));

    private JCheckBox passwordNotRequired =
        new SIPCommCheckBox(Resources.getString("passwordNotRequired"));

    private JPanel mainPanel = new TransparentPanel();

    private Object nextPageIdentifier = WizardPage.SUMMARY_PAGE_IDENTIFIER;

    private IrcAccountRegistrationWizard wizard;

    private boolean isCommitted = false;

    /**
     * Creates an instance of <tt>FirstWizardPage</tt>.
     * 
     * @param wizard the parent wizard
     */
    public FirstWizardPage(IrcAccountRegistrationWizard wizard)
    {
        super(new BorderLayout());

        this.wizard = wizard;

        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));

        this.init();

        this.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        this.labelsPanel.setLayout(
            new BoxLayout(labelsPanel, BoxLayout.Y_AXIS));

        this.valuesPanel.setLayout(
            new BoxLayout(valuesPanel, BoxLayout.Y_AXIS));

        this.labelsServerPanel.setLayout(
            new BoxLayout(labelsServerPanel, BoxLayout.Y_AXIS));

        this.valuesServerPanel.setLayout(
            new BoxLayout(valuesServerPanel, BoxLayout.Y_AXIS));

        this.labelsOptionsPanel.setLayout(
            new BoxLayout(labelsOptionsPanel, BoxLayout.Y_AXIS));

        this.valuesOptionsPanel.setLayout(
            new BoxLayout(valuesOptionsPanel, BoxLayout.Y_AXIS));

        this.portField.setEnabled(false);
        this.rememberPassBox.setEnabled(false);
    }

    /**
     * Initializes all panels, buttons, etc.
     */
    private void init()
    {
        this.mainPanel.setOpaque(false);
        this.labelsPanel.setOpaque(false);
        this.valuesPanel.setOpaque(false);
        this.emptyPanel.setOpaque(false);

        this.nickField.getDocument().addDocumentListener(this);
        this.serverField.getDocument().addDocumentListener(this);
        this.defaultPort.addActionListener(this);
        this.passwordNotRequired.addActionListener(this);

        this.rememberPassBox.setSelected(true);
        this.autoNickChange.setSelected(true);
        this.defaultPort.setSelected(true);
        this.passwordNotRequired.setSelected(false);

        this.existingAccountLabel.setForeground(Color.RED);

        this.nickExampleLabel.setForeground(Color.GRAY);
        this.nickExampleLabel.setFont(
                nickExampleLabel.getFont().deriveFont(8));
        this.serverExampleLabel.setForeground(Color.GRAY);
        this.serverExampleLabel.setFont(
                serverExampleLabel.getFont().deriveFont(8));
        this.emptyPanel.setMaximumSize(new Dimension(40, 35));
        this.emptyPanel2.setMaximumSize(new Dimension(40, 35));
        this.nickExampleLabel.setBorder(
                BorderFactory.createEmptyBorder(0, 0, 8,0));
        this.serverExampleLabel.setBorder(
                BorderFactory.createEmptyBorder(0, 0, 8,0));

        labelsPanel.add(nick);
        labelsPanel.add(emptyPanel);
        labelsPanel.add(passLabel);

//        labelsPanel.add(server);

        valuesPanel.add(nickField);
        valuesPanel.add(nickExampleLabel);
        valuesPanel.add(passField);
//        valuesPanel.add(serverField);

        userPassPanel.add(infoPassword, BorderLayout.NORTH);
        userPassPanel.add(labelsPanel, BorderLayout.WEST);
        userPassPanel.add(valuesPanel, BorderLayout.CENTER);
        userPassPanel.add(passwordNotRequired, BorderLayout.SOUTH);
//        userPassPanel.add(autoChangeNick, BorderLayout.SOUTH);
        
        userPassPanel.setBorder(BorderFactory
                                .createTitledBorder(Resources.getString(
                                    "userAndPassword")));
        
        labelsServerPanel.add(server);
        labelsServerPanel.add(emptyPanel2);
        labelsServerPanel.add(port);
        
        valuesServerPanel.add(serverField);
        valuesServerPanel.add(serverExampleLabel);
        valuesServerPanel.add(portField);
        
        serverPanel.add(labelsServerPanel, BorderLayout.WEST);
        serverPanel.add(valuesServerPanel, BorderLayout.CENTER);
        serverPanel.add(defaultPort, BorderLayout.SOUTH);
        
        serverPanel.setBorder(BorderFactory.createTitledBorder(
            Resources.getString("serverIRC")));
        
        optionsPanel.add(rememberPassBox, BorderLayout.CENTER);
        optionsPanel.add(autoNickChange, BorderLayout.SOUTH);
        
        optionsPanel.setBorder(BorderFactory.createTitledBorder(
            Resources.getString("options")));
        
        mainPanel.add(userPassPanel);
        mainPanel.add(serverPanel);
        mainPanel.add(optionsPanel);

        this.add(mainPanel, BorderLayout.NORTH);
//        this.add(serverPanel, BorderLayout.SOUTH);
//        this.add(optionsPanel, BorderLayout.AFTER_LAST_LINE);
    }

    /**
     * Implements the <code>WizardPage.getIdentifier</code> to return
     * this page identifier.
     *
     * @return the Identifier of the first page in this wizard.
     */
    public Object getIdentifier()
    {
        return FIRST_PAGE_IDENTIFIER;
    }

    /**
     * Implements the <code>WizardPage.getNextPageIdentifier</code> to return
     * the next page identifier - the summary page.
     *
     * @return the identifier of the page following this one.
     */
    public Object getNextPageIdentifier()
    {
        return nextPageIdentifier;
    }

    /**
     * Implements the <code>WizardPage.getBackPageIdentifier</code> to return
     * the next back identifier - the default page.
     *
     * @return the identifier of the default wizard page.
     */
    public Object getBackPageIdentifier()
    {
        return WizardPage.DEFAULT_PAGE_IDENTIFIER;
    }

    /**
     * Implements the <code>WizardPage.getWizardForm</code> to return
     * this panel.
     *
     * @return the component to be displayed in this wizard page.
     */
    public Object getWizardForm()
    {
        return this;
    }

    /**
     * Before this page is displayed enables or disables the "Next" wizard
     * button according to whether the UserID field is empty.
     */
    public void pageShowing()
    {
        this.setNextButtonAccordingToUserID();
    }

    /**
     * Saves the user input when the "Next" wizard buttons is clicked.
     */
    public void commitPage()
    {
        nextPageIdentifier = SUMMARY_PAGE_IDENTIFIER;
        userPassPanel.remove(existingAccountLabel);

        IrcAccountRegistration registration = wizard.getRegistration();

        registration.setUserID(nickField.getText());

        if (passField.getPassword() != null)
            registration.setPassword(new String(passField.getPassword()));

        registration.setServer(serverField.getText());
        registration.setPort(portField.getText());
        registration.setRememberPassword(rememberPassBox.isSelected());
        registration.setAutoChangeNick(autoNickChange.isSelected());
        registration.setRequiredPassword(!passwordNotRequired.isSelected());

        isCommitted = true;
    }

    /**
     * Enables or disables the "Next" wizard button according to whether the
     * User ID field is empty.
     */
    private void setNextButtonAccordingToUserID()
    {
        if (nickField.getText() == null
                || nickField.getText().equals("")
                || serverField.getText() == null
                || serverField.getText().equals("")
                || (!passwordNotRequired.isSelected()
                        && passField.equals("")))
        {
            wizard.getWizardContainer().setNextFinishButtonEnabled(false);
        }
        else
        {
            wizard.getWizardContainer().setNextFinishButtonEnabled(true);
        }
    }

    /**
     * Handles the <tt>DocumentEvent</tt> triggered when user types in the
     * User ID field. Enables or disables the "Next" wizard button according to
     * whether the User ID field is empty.
     *
     * @param event the event containing the update.
     */
    public void insertUpdate(DocumentEvent event)
    {
        this.setNextButtonAccordingToUserID();
    }

    /**
     * Handles the <tt>DocumentEvent</tt> triggered when user deletes letters
     * from the UserID field. Enables or disables the "Next" wizard button
     * according to whether the UserID field is empty.
     *
     * @param event the event containing the update.
     */
    public void removeUpdate(DocumentEvent event)
    {
        this.setNextButtonAccordingToUserID();
    }

    /**
     * Fills the UserID and Password fields in this panel with the data coming
     * from the given protocolProvider.
     * 
     * @param protocolProvider The <tt>ProtocolProviderService</tt> to load the
     * data from.
     */
    public void loadAccount(ProtocolProviderService protocolProvider)
    {
        AccountID accountID = protocolProvider.getAccountID();

        String password = (String) accountID.getAccountProperties()
            .get(ProtocolProviderFactory.PASSWORD);

        String server = (String) accountID.getAccountProperties()
            .get(ProtocolProviderFactory.SERVER_ADDRESS);

        String port = (String) accountID.getAccountProperties()
            .get(ProtocolProviderFactory.SERVER_PORT);

        String autoNickChange = (String) accountID.getAccountProperties()
            .get(ProtocolProviderFactory.AUTO_CHANGE_USER_NAME);

        String noPasswordRequired = (String) accountID.getAccountProperties()
            .get(ProtocolProviderFactory.NO_PASSWORD_REQUIRED);

        this.nickField.setEnabled(false);
        this.nickField.setText(accountID.getUserID());
        this.serverField.setText(server);

        if (password != null)
        {
            this.passField.setText(password);
            this.rememberPassBox.setSelected(true);
        }

        if (port != null)
        {
            this.portField.setText(port);
            this.portField.setEnabled(true);
            this.defaultPort.setSelected(false);
        }

        if (autoNickChange != null)
        {
            this.autoNickChange.setSelected(
                new Boolean(autoNickChange).booleanValue());
        }

        if (noPasswordRequired != null)
        {
            boolean isPassRequired
                = !(new Boolean(noPasswordRequired).booleanValue());

            this.passwordNotRequired.setSelected(!isPassRequired);

            passField.setEnabled(isPassRequired);
        }
    }

    /**
     * Verifies whether there is already an account installed with the same
     * details as the one that the user has just entered.
     *
     * @param userID the name of the user that the account is registered for
     * @return true if there is already an account for this userID and false
     * otherwise.
     */
    private boolean isExistingAccount(String userID)
    {
        ProtocolProviderFactory factory
            = IrcAccRegWizzActivator.getIrcProtocolProviderFactory();

        ArrayList registeredAccounts = factory.getRegisteredAccounts();

        for (int i = 0; i < registeredAccounts.size(); i++)
        {
            AccountID accountID = (AccountID) registeredAccounts.get(i);

            if (userID.equalsIgnoreCase(accountID.getUserID()))
            {
                return true;
            }
        }
        return false;
    }

    /**
     * Indicates when the default port check box and the passwordNotRequired
     * check box are selected.
     */
    public void actionPerformed(ActionEvent event)
    {
        if (defaultPort.isSelected())
        {
            portField.setText("");
            portField.setEnabled(false);
        }
        else
            portField.setEnabled(true);

        if (passwordNotRequired.isSelected())
        {
            passField.setText("");
            passField.setEnabled(false);
            rememberPassBox.setEnabled(false);
        }
        else
        {
            passField.setEnabled(true);
            rememberPassBox.setEnabled(true);
        }
        
    }

    public void changedUpdate(DocumentEvent event){}

    public void pageHiding(){}

    public void pageShown(){}

    public void pageBack(){}
    
    public Object getSimpleForm()
    {
        JPanel simplePanel = new TransparentPanel(new BorderLayout());
        JPanel labelsPanel = new TransparentPanel(new GridLayout(0, 1));
        JPanel valuesPanel = new TransparentPanel(new GridLayout(0, 1));

        simplePanel.setOpaque(false);
        labelsPanel.setOpaque(false);
        valuesPanel.setOpaque(false);
        emptyPanel2.setOpaque(false);

        simplePanel.add(labelsPanel, BorderLayout.WEST);
        simplePanel.add(valuesPanel, BorderLayout.CENTER);

        labelsPanel.add(nick);
        labelsPanel.add(emptyPanel);
        labelsPanel.add(server);
        labelsPanel.add(emptyPanel2);

        valuesPanel.add(nickField);
        valuesPanel.add(nickExampleLabel);
        valuesPanel.add(serverField);
        valuesPanel.add(serverExampleLabel);

        return simplePanel;
    }
    
    public boolean isCommitted()
    {
        return isCommitted;
    }
}
