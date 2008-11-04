/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package net.java.sip.communicator.impl.gui.main.chat.toolBars;

import java.awt.*;
import java.awt.event.*;
import java.util.*;

import javax.swing.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.customcontrols.*;
import net.java.sip.communicator.impl.gui.event.*;
import net.java.sip.communicator.impl.gui.i18n.*;
import net.java.sip.communicator.impl.gui.main.chat.*;
import net.java.sip.communicator.impl.gui.main.chat.conference.*;
import net.java.sip.communicator.impl.gui.main.chat.history.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.gui.Container;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;

import org.osgi.framework.*;

/**
 * The <tt>MainToolBar</tt> is a <tt>JToolBar</tt> which contains buttons
 * for file operations, like save and print, for copy-paste operations, etc.
 * It's the main toolbar in the <tt>ChatWindow</tt>. It contains only
 * <tt>ChatToolbarButton</tt>s, which have a specific background icon and
 * rollover behaviour to differentiates them from normal buttons.
 * 
 * @author Yana Stamcheva
 * @author Lubomir Marinov
 */
public class MainToolBar
    extends SIPCommToolBar
    implements  ActionListener,
                PluginComponentListener
{
    private Logger logger = Logger.getLogger(MainToolBar.class);

    private ChatToolbarButton saveButton
        = new ChatToolbarButton(ImageLoader.getImage(ImageLoader.SAVE_ICON));

    private ChatToolbarButton printButton
        = new ChatToolbarButton(ImageLoader.getImage(ImageLoader.PRINT_ICON));

    private ChatToolbarButton previousButton
        = new ChatToolbarButton(ImageLoader.getImage(ImageLoader.PREVIOUS_ICON));

    private ChatToolbarButton nextButton
        = new ChatToolbarButton(ImageLoader.getImage(ImageLoader.NEXT_ICON));

    private ChatToolbarButton historyButton
        = new ChatToolbarButton(ImageLoader.getImage(ImageLoader.HISTORY_ICON));

    private ChatToolbarButton sendFileButton
        = new ChatToolbarButton(
                ImageLoader.getImage(ImageLoader.SEND_FILE_ICON));

    private ChatToolbarButton inviteButton
        = new ChatToolbarButton(
                ImageLoader.getImage(ImageLoader.ADD_TO_CHAT_ICON));

    private ChatWindow messageWindow;

    private Contact currentChatContact = null;

    /**
     * Empty constructor to be used from inheritors.
     */
    public MainToolBar()
    {
    }

    /**
     * Creates an instance and constructs the <tt>MainToolBar</tt>.
     * 
     * @param messageWindow The parent <tt>ChatWindow</tt>.
     */
    public MainToolBar(ChatWindow messageWindow)
    {
        this.messageWindow = messageWindow;

        this.setLayout(new FlowLayout(FlowLayout.LEFT, 2, 0));

        this.add(inviteButton);
        this.add(historyButton);

        this.addSeparator();

        this.add(previousButton);
        this.add(nextButton);

        this.saveButton.setName("save");
        this.saveButton.setToolTipText(
            Messages.getI18NString("save").getText() + " Ctrl-S");

        this.printButton.setName("print");
        this.printButton.setToolTipText(
            Messages.getI18NString("print").getText());

        this.previousButton.setName("previous");
        this.previousButton.setToolTipText(
            Messages.getI18NString("previous").getText());

        this.nextButton.setName("next");
        this.nextButton.setToolTipText(
            Messages.getI18NString("next").getText());

        this.sendFileButton.setName("sendFile");
        this.sendFileButton.setToolTipText(
            Messages.getI18NString("sendFile").getText());

        this.historyButton.setName("history");
        this.historyButton.setToolTipText(
            Messages.getI18NString("history").getText() + " Ctrl-H");

        this.inviteButton.setName("invite");
        this.inviteButton.setToolTipText(
            Messages.getI18NString("invite").getText());

        this.saveButton.addActionListener(this);
        this.printButton.addActionListener(this);
        this.previousButton.addActionListener(this);
        this.nextButton.addActionListener(this);
        this.sendFileButton.addActionListener(this);
        this.historyButton.addActionListener(this);
        this.inviteButton.addActionListener(this);

        this.initPluginComponents();
    }

    /**
     * Runs clean-up for associated resources which need explicit disposal (e.g.
     * listeners keeping this instance alive because they were added to the
     * model which operationally outlives this instance).
     */
    public void dispose()
    {
        GuiActivator.getUIService().removePluginComponentListener(this);
    }

    /**
     * Handles the <tt>ActionEvent</tt>, when one of the toolbar buttons is
     * clicked.
     */
    public void actionPerformed(ActionEvent e)
    {
        AbstractButton button = (AbstractButton) e.getSource();
        String buttonText = button.getName();

        ChatPanel chatPanel = messageWindow.getCurrentChatPanel();

        if (buttonText.equalsIgnoreCase("previous"))
        {
            chatPanel.loadPreviousPageFromHistory();
        }
        else if (buttonText.equalsIgnoreCase("next"))
        {
            chatPanel.loadNextPageFromHistory();
        }
        else if (buttonText.equalsIgnoreCase("sendFile")) {

        }
        else if (buttonText.equalsIgnoreCase("history"))
        {
            HistoryWindow history;

            HistoryWindowManager historyWindowManager
                = GuiActivator.getUIService().getHistoryWindowManager();

            ChatSession chatSession = chatPanel.getChatSession();

            if(historyWindowManager
                .containsHistoryWindowForContact(chatSession.getDescriptor()))
            {
                history = historyWindowManager
                    .getHistoryWindowForContact(chatSession.getDescriptor());

                if(history.getState() == JFrame.ICONIFIED)
                    history.setState(JFrame.NORMAL);

                history.toFront();
            }
            else
            {
                history = new HistoryWindow(
                    chatPanel.getChatSession().getDescriptor());

                history.setVisible(true);

                historyWindowManager
                    .addHistoryWindowForContact(chatSession.getDescriptor(),
                                                    history);
            }
        }
        else if (buttonText.equalsIgnoreCase("invite")) 
        {
            ChatInviteDialog inviteDialog
                = new ChatInviteDialog(chatPanel);

            inviteDialog.setVisible(true);
        }
    }

    /**
     * Returns the button used to show the history window.
     * 
     * @return the button used to show the history window.
     */
    public ChatToolbarButton getHistoryButton()
    {
        return historyButton;
    }

    /**
     * Disables/Enables history arrow buttons depending on whether the
     * current page is the first, the last page or a middle page.
     */
    public void changeHistoryButtonsState(ChatPanel chatPanel)
    {
        ChatConversationPanel convPanel = chatPanel.getChatConversationPanel();
        
        Date firstMsgInHistory = chatPanel.getFirstHistoryMsgTimestamp();
        Date lastMsgInHistory = chatPanel.getLastHistoryMsgTimestamp();
        Date firstMsgInPage = convPanel.getPageFirstMsgTimestamp();
        Date lastMsgInPage = convPanel.getPageLastMsgTimestamp();
        
        if(firstMsgInHistory == null || lastMsgInHistory == null)
        {
            previousButton.setEnabled(false);
            nextButton.setEnabled(false);
            return;
        }
        
        if(firstMsgInHistory.compareTo(firstMsgInPage) < 0)
            previousButton.setEnabled(true);
        else
            previousButton.setEnabled(false);
        
        if(lastMsgInPage.getTime() > 0
                && (lastMsgInHistory.compareTo(lastMsgInPage) > 0))
        {
            nextButton.setEnabled(true);
        }
        else
        {
            nextButton.setEnabled(false);
        }
    }
    
    private void initPluginComponents()
    {
        // Search for plugin components registered through the OSGI bundle
        // context.
        ServiceReference[] serRefs = null;

        String osgiFilter = "("
            + Container.CONTAINER_ID
            + "="+Container.CONTAINER_CHAT_TOOL_BAR.getID()+")";

        try
        {
            serRefs = GuiActivator.bundleContext.getServiceReferences(
                PluginComponent.class.getName(),
                osgiFilter);
        }
        catch (InvalidSyntaxException exc)
        {
            logger.error("Could not obtain plugin reference.", exc);
        }

        if (serRefs != null)
        {
            for (int i = 0; i < serRefs.length; i ++)
            {
                PluginComponent component = (PluginComponent) GuiActivator
                    .bundleContext.getService(serRefs[i]);;

                this.add((Component)component.getComponent());

                this.revalidate();
                this.repaint();
            }
        }

        GuiActivator.getUIService().addPluginComponentListener(this);
    }

    /**
     * Implements the <code>PluginComponentListener.pluginComponentAdded</code>
     * method.
     */
    public void pluginComponentAdded(PluginComponentEvent event)
    {
        PluginComponent c = event.getPluginComponent();

        if(c.getContainer().equals(Container.CONTAINER_CHAT_TOOL_BAR))
        {
            this.addSeparator();
            this.add((Component) c.getComponent());

            this.revalidate();
            this.repaint();
        }
    }

    /**
     * Implements the <code>PluginComponentListener.pluginComponentRemoved</code>
     * method.
     */
    public void pluginComponentRemoved(PluginComponentEvent event)
    {
        PluginComponent c = event.getPluginComponent();

        if(c.getContainer().equals(Container.CONTAINER_CHAT_TOOL_BAR))
        {
            this.remove((Component) c.getComponent());
        }
    }

    /**
     * Enables or disables the conference button in this tool bar.
     * 
     * @param isEnabled <code>true</code> if the conference button should be
     * enabled, <code>false</code> - otherwise.
     */
    public void enableInviteButton(boolean isEnabled)
    {
        if (isEnabled)
            this.add(inviteButton, 0);
        else
            this.remove(inviteButton);
    }
}
