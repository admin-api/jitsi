/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.generalconfig.autoaway;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;
import javax.swing.event.*;

import net.java.sip.communicator.plugin.generalconfig.*;
import net.java.sip.communicator.util.swing.*;

import org.jitsi.service.configuration.*;
import org.jitsi.service.resources.*;

/**
 * The <tt>ConfigurationForm</tt> that would be added in the user interface
 * configuration window.
 *
 * @author Damien Roth
 */
public class AutoAwayConfigurationPanel
    extends TransparentPanel
{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

    private JCheckBox enable;
    private JSpinner timer;

    /**
     * Create an instance of <tt>StatusConfigForm</tt>
     */
    public AutoAwayConfigurationPanel()
    {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        JPanel pnlSection = GeneralConfigPluginActivator.
            createConfigSectionComponent(
                Resources.getString("service.gui.STATUS"));
        pnlSection.add(createMainPanel());
        add(pnlSection);

        initValues();
    }

    /**
     * Init the main panel.
     * @return the created component
     */
    private Component createMainPanel()
    {
        JPanel mainPanel = new TransparentPanel(new BorderLayout(5, 5));

        enable = new SIPCommCheckBox(GeneralConfigPluginActivator.getResources()
                .getI18NString("plugin.autoaway.ENABLE_CHANGE_STATUS"));

        mainPanel.add(enable, BorderLayout.NORTH);

        enable.addActionListener(new ActionListener()
        {

            public void actionPerformed(ActionEvent e)
            {
                timer.setEnabled(enable.isSelected());
                saveData();
            }
        });

        JPanel timerPanel =
            new TransparentPanel(new FlowLayout(FlowLayout.LEFT));
        // Text
        timerPanel.add(new JLabel(
                GeneralConfigPluginActivator.getResources()
                    .getI18NString("plugin.autoaway.AWAY_MINUTES")));
        // Spinner
        timer = new JSpinner(new SpinnerNumberModel(
                                    Preferences.DEFAULT_TIMER, 1, 180, 1));
        timerPanel.add(timer);
        timer.addChangeListener(new ChangeListener()
        {

            public void stateChanged(ChangeEvent e)
            {
                saveData();
            }
        });

        mainPanel.add(timerPanel, BorderLayout.WEST);

        return mainPanel;
    }

    /**
     * Init the values of the widgets
     */
    private void initValues()
    {
        boolean enabled = Preferences.isEnabled();

        this.enable.setSelected(enabled);
        this.timer.setEnabled(enabled);

        this.timer.setValue(Preferences.getTimer());
    }

    /**
     * Save data in the configuration file
     */
    private void saveData()
    {
        Preferences.saveData(enable.isSelected(), timer.getValue().toString());
    }
}
