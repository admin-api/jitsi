/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.call;

import javax.swing.*;

import net.java.sip.communicator.util.swing.*;

/**
 * The dialog created for a given call.
 *
 * @author Yana Stamcheva
 * @author Adam Netocny
 */
public class CallDialog
    extends SIPCommFrame
    implements  CallContainer,
                CallTitleListener
{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

    /**
     * The panel, where all call components are added.
     */
    private CallPanel callPanel;

    /**
     * Creates a <tt>CallDialog</tt> by specifying the underlying call panel.
     */
    public CallDialog()
    {
        super(false);
    }

    /**
     * Adds a call panel.
     *
     * @param callPanel the call panel to add to this dialog
     */
    public void addCallPanel(CallPanel callPanel)
    {
        this.callPanel = callPanel;

        getContentPane().add(callPanel);

        this.setTitle(callPanel.getCallTitle());
        callPanel.addCallTitleListener(this);

        if (!isVisible())
            setVisible(true);
    }

    /**
     * Closes the given call panel.
     *
     * @param callPanel the <tt>CallPanel</tt> to close
     */
    public void close(CallPanel callPanel)
    {
        if (this.callPanel.equals(callPanel))
            dispose();
    }

    /**
     * Indicates if the given <tt>callPanel</tt> is currently visible.
     *
     * @param callPanel the <tt>CallPanel</tt>, for which we verity
     * @return <tt>true</tt> if the given call container is visible in this
     * call window, otherwise - <tt>false</tt>
     */
    public boolean isCallVisible(CallPanel callPanel)
    {
        if (this.callPanel.equals(callPanel))
            return isVisible();

        return false;
    }

    /**
     * Returns the frame of the call window.
     *
     * @return the frame of the call window
     */
    public JFrame getFrame()
    {
        return this;
    }

    /**
     * Called when the title of the given <tt>CallPanel</tt> changes.
     *
     * @param callPanel the <tt>CallPanel</tt>, which title has changed
     */
    public void callTitleChanged(CallPanel callPanel)
    {
        if (this.callPanel.equals(callPanel))
            this.setTitle(callPanel.getCallTitle());
    }
}
