/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.call;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

import javax.swing.*;
import javax.swing.text.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.service.protocol.media.*;
import net.java.sip.communicator.service.resources.*;
import net.java.sip.communicator.util.*;
import net.java.sip.communicator.util.skin.*;
import net.java.sip.communicator.util.swing.*;
import net.java.sip.communicator.util.swing.TransparentPanel;

import org.jitsi.service.neomedia.*;
import org.jitsi.service.protocol.event.*;
import org.jitsi.util.swing.*;

/**
 * The <tt>OneToOneCallPeerPanel</tt> is the panel containing data for a call
 * peer in a given call. It contains information like call peer
 * name, photo, call duration, etc.
 *
 * @author Yana Stamcheva
 * @author Lyubomir Marinov
 * @author Sebastien Vincent
 * @author Adam Netocny
 */
public class OneToOneCallPeerPanel
    extends TransparentPanel
    implements  CallPeerRenderer,
                Skinnable
{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

    /**
     * The <tt>CallPeerAdapter</tt> that implements all common tt>CallPeer</tt>
     * related listeners.
     */
    private CallPeerAdapter callPeerAdapter;

    /**
     * The component showing the status of the underlying call peer.
     */
    private final JLabel callStatusLabel = new JLabel();

    /**
     * The security status of the peer
     */
    private SecurityStatusLabel securityStatusLabel = new SecurityStatusLabel();

    /**
     * The label showing whether the voice has been set to mute.
     */
    private final JLabel muteStatusLabel = new JLabel();

    /**
     * The label showing whether the call is on or off hold.
     */
    private final JLabel holdStatusLabel = new JLabel();

    /**
     * The DTMF label.
     */
    private final JLabel dtmfLabel = new JLabel();

    /**
     * Current id for security image.
     */
    private ImageID securityImageID = ImageLoader.SECURE_BUTTON_OFF;

    /**
     * The component responsible for displaying an error message.
     */
    private JTextComponent errorMessageComponent;

    /**
     * The <tt>Component</tt>s showing the avatar of the underlying call peer.
     * <p>
     * Because the <tt>Component</tt>s showing the avatar of the underlying call
     * peer are managed by their respective <tt>VideoContainer</tt>s and are
     * automatically displayed whenever there is no associated remote video,
     * each <tt>VideoContainer</tt> must have its own. Otherwise, the various
     * <tt>VideoContainer</tt>s might start fighting over which one is to
     * contain the one and only photoLabel.
     * </p>
     */
    private final List<JLabel> photoLabels = new LinkedList<JLabel>();

    /**
     * The panel containing security related components.
     */
    private SecurityPanel<?> securityPanel;

    /**
     * The <tt>Icon</tt> which represents the avatar of the associated call
     * peer.
     */
    private ImageIcon peerImage;

    /**
     * The name of the peer.
     */
    private String peerName;

    /**
     * The list containing all video containers.
     */
    private final List<Container> videoContainers;

    /**
     * The renderer of the call.
     */
    private final CallRenderer callRenderer;

    /**
     * The <tt>CallPeer</tt>, which is rendered in this panel.
     */
    private CallPeer callPeer;

    /**
     * Sound local level label.
     */
    private InputVolumeControlButton localLevel;

    /**
     * Sound remote level label.
     */
    private OutputVolumeControlButton remoteLevel;

    /**
     * The center component.
     */
    private final Component center;

    /**
     * The status bar component.
     */
    private final Component statusBar;

    /**
     * The label containing the user photo.
     */
    private final JLabel photoLabel;

    /**
     * Creates a <tt>CallPeerPanel</tt> for the given call peer.
     *
     * @param callRenderer the renderer of the call
     * @param callPeer the <tt>CallPeer</tt> represented in this panel
     * @param videoContainers the video <tt>Container</tt> list
     */
    public OneToOneCallPeerPanel(   CallRenderer callRenderer,
                                    CallPeer callPeer,
                                    List<Container> videoContainers)
    {
        this.callRenderer = callRenderer;
        this.callPeer = callPeer;
        this.peerName = callPeer.getDisplayName();
        this.videoContainers = videoContainers;
        this.securityPanel = SecurityPanel.create(this, callPeer, null);

        photoLabel = new JLabel(getPhotoLabelIcon());
        center = createCenter(videoContainers);
        statusBar = createStatusBar();

        this.setPeerImage(CallManager.getPeerImage(callPeer));

        /* Lay out the main Components of the UI. */
        setLayout(new GridBagLayout());

        GridBagConstraints constraints = new GridBagConstraints();

        if (center != null)
        {
            /*
             * Don't let the center dictate the preferred size because it may
             * display large videos. Otherwise, the large video will make this
             * panel expand and then the panel's container will show scroll
             * bars.
             */
            center.setPreferredSize(new Dimension(1, 1));

            constraints.fill = GridBagConstraints.BOTH;
            constraints.gridx = 0;
            constraints.gridy = 1;
            constraints.weightx = 1;
            constraints.weighty = 1;

            add(center, constraints);
        }
        if (statusBar != null)
        {
            constraints.fill = GridBagConstraints.NONE;
            constraints.gridx = 0;
            constraints.gridy = 3;
            constraints.weightx = 0;
            constraints.weighty = 0;
            constraints.insets = new Insets(5, 0, 0, 0);

            add(statusBar, constraints);
        }

        createSoundLevelIndicators();
        initSecuritySettings();
    }

    /**
     * Creates the <code>Component</code> hierarchy of the central area of this
     * <code>CallPeerPanel</code> which displays the photo of the
     * <code>CallPeer</code> or the video if any.
     *
     * @return the root of the <code>Component</code> hierarchy of the central
     *         area of this <code>CallPeerPanel</code> which displays the
     *         photo of the <code>CallPeer</code> or the video if any
     */
    Component createCenter(final List<Container> videoContainers)
    {
        photoLabel.setPreferredSize(new Dimension(90, 90));

        final Container videoContainer = createVideoContainer(photoLabel);

        videoContainer.addHierarchyListener(new HierarchyListener()
        {
            public void hierarchyChanged(HierarchyEvent event)
            {
                int changeFlags = HierarchyEvent.DISPLAYABILITY_CHANGED;

                if ((event.getChangeFlags() & changeFlags) == changeFlags)
                {
                    synchronized (videoContainers)
                    {
                        boolean changed = false;

                        if (videoContainer.isDisplayable())
                        {
                            if (!videoContainers.contains(videoContainer))
                                changed = videoContainers.add(videoContainer);
                            if (!photoLabels.contains(photoLabel))
                                photoLabels.add(photoLabel);
                        }
                        else
                        {
                            changed = videoContainers.remove(videoContainer);
                            photoLabels.remove(photoLabel);
                        }
                        if (changed)
                            callRenderer.getVideoHandler()
                                .handleVideoEvent(callPeer.getCall(), null);
                    }
                }
            }
        });
        return videoContainer;
    }

    /**
     * Creates a new AWT <code>Container</code> which can display a single
     * <code>Component</code> at a time (supposedly, one which represents video)
     * and, in the absence of such a <code>Component</code>, displays a
     * predefined default <code>Component</code> (in accord with the previous
     * supposition, one which is the default when there is no video). The
     * returned <code>Container</code> will track the <code>Components</code>s
     * added to and removed from it in order to make sure that
     * <code>noVideoContainer</code> is displayed as described.
     *
     * @param noVideoComponent the predefined default <code>Component</code> to
     *            be displayed in the returned <code>Container</code> when there
     *            is no other <code>Component</code> in it
     * @return a new <code>Container</code> which can display a single
     *         <code>Component</code> at a time and, in the absence of such a
     *         <code>Component</code>, displays <code>noVideoComponent</code>
     */
    private Container createVideoContainer(Component noVideoComponent)
    {
        VideoContainer oldParent
            = (VideoContainer) noVideoComponent.getParent();
        if (oldParent != null)
            oldParent.remove(noVideoComponent);

        return new VideoContainer(noVideoComponent, false);
    }

    /**
     * Creates the <code>Component</code> hierarchy of the area of
     * status-related information such as <code>CallPeer</code> display
     * name, call duration, security status.
     *
     * @return the root of the <code>Component</code> hierarchy of the area of
     *         status-related information such as <code>CallPeer</code>
     *         display name, call duration, security status
     */
    private Component createStatusBar()
    {
        // stateLabel
        callStatusLabel.setForeground(Color.WHITE);
        dtmfLabel.setForeground(Color.WHITE);
        callStatusLabel.setText(callPeer.getState().getLocalizedStateString());

        PeerStatusPanel statusPanel = new PeerStatusPanel(new GridBagLayout());

        GridBagConstraints constraints = new GridBagConstraints();

        constraints.gridx = 0;
        constraints.gridy = 0;
        statusPanel.add(securityStatusLabel, constraints);
        initSecurityStatusLabel();

        constraints.gridx++;
        statusPanel.add(holdStatusLabel, constraints);

        constraints.gridx++;
        statusPanel.add(muteStatusLabel, constraints);

        constraints.gridx++;
        callStatusLabel.setBorder(
            BorderFactory.createEmptyBorder(2, 3, 2, 12));
        statusPanel.add(callStatusLabel, constraints);

        constraints.gridx++;
        constraints.weightx = 1f;
        statusPanel.add(dtmfLabel, constraints);

        return statusPanel;
    }

    /**
     * Creates sound level related components.
     */
    private void createSoundLevelIndicators()
    {
        TransparentPanel localLevelPanel
            = new TransparentPanel(new BorderLayout(5, 0));
        TransparentPanel remoteLevelPanel
            = new TransparentPanel(new BorderLayout(5, 0));

        localLevel = new InputVolumeControlButton(
                callPeer.getCall(),
                ImageLoader.MICROPHONE,
                ImageLoader.MUTE_BUTTON,
                false, false, false);
        remoteLevel = new OutputVolumeControlButton(
                ImageLoader.HEADPHONE, false, false);

        final SoundLevelIndicator localLevelIndicator
            = new SoundLevelIndicator(
                    SoundLevelChangeEvent.MIN_LEVEL,
                    SoundLevelChangeEvent.MAX_LEVEL);
        final SoundLevelIndicator remoteLevelIndicator
            = new SoundLevelIndicator(
                    SoundLevelChangeEvent.MIN_LEVEL,
                    SoundLevelChangeEvent.MAX_LEVEL);

        localLevelPanel.add(localLevel, BorderLayout.WEST);
        localLevelPanel.add(localLevelIndicator, BorderLayout.CENTER);
        remoteLevelPanel.add(remoteLevel, BorderLayout.WEST);
        remoteLevelPanel.add(remoteLevelIndicator, BorderLayout.CENTER);

        GridBagConstraints constraints = new GridBagConstraints();
        constraints.fill = GridBagConstraints.NONE;
        constraints.gridx = 0;
        constraints.gridy = 5;
        constraints.weightx = 0;
        constraints.weighty = 0;
        constraints.insets = new Insets(10, 0, 0, 0);

        add(localLevelPanel, constraints);

        constraints.fill = GridBagConstraints.NONE;
        constraints.gridx = 0;
        constraints.gridy = 6;
        constraints.weightx = 0;
        constraints.weighty = 0;
        constraints.insets = new Insets(5, 0, 10, 0);

        add(remoteLevelPanel, constraints);

        this.callPeer.addStreamSoundLevelListener(
                new SoundLevelListener()
                {
                    public void soundLevelChanged(Object source, int level)
                    {
                        remoteLevelIndicator.updateSoundLevel(level);
                    }
                });

        this.callPeer.getCall().addLocalUserSoundLevelListener(
                new SoundLevelListener()
                {
                    public void soundLevelChanged(Object source, int level)
                    {
                        localLevelIndicator.updateSoundLevel(level);
                    }
                });
    }

    /**
     * Initializes the security settings for this call peer.
     */
    private void initSecuritySettings()
    {
        CallPeerSecurityStatusEvent securityEvent
            = callPeer.getCurrentSecuritySettings();

        if (securityEvent != null
            && securityEvent instanceof CallPeerSecurityOnEvent)
        {
            securityOn((CallPeerSecurityOnEvent) securityEvent);
        }
    }

    /**
     * Returns the name of the peer, contained in this panel.
     *
     * @return the name of the peer, contained in this panel
     */
    public String getPeerName()
    {
        return peerName;
    }

    /**
     * The <tt>TransparentPanel</tt> that will display the peer status.
     */
    private static class PeerStatusPanel
        extends TransparentPanel
    {
        /**
         * Silence the serial warning. Though there isn't a plan to serialize
         * the instances of the class, there're no fields so the default
         * serialization routine will work.
         */
        private static final long serialVersionUID = 0L;

        /**
         * Constructs a new <tt>PeerStatusPanel</tt>.
         *
         * @param layout the <tt>LayoutManager</tt> to use
         */
        public PeerStatusPanel(LayoutManager layout)
        {
            super(layout);
        }

        /**
         * @{inheritDoc}
         */
        @Override
        public void paintComponent(Graphics g)
        {
            super.paintComponent(g);

            g = g.create();

            try
            {
                AntialiasingManager.activateAntialiasing(g);

                g.setColor(Color.DARK_GRAY);
                g.fillRoundRect(0, 0, this.getWidth(), this.getHeight(), 20, 20);
            }
            finally
            {
                g.dispose();
            }
        }
    }

    /**
     * Sets the name of the peer.
     * @param name the name of the peer
     */
    public void setPeerName(String name)
    {
        peerName = name;

        ((OneToOneCallPanel) callRenderer).setPeerName(name);
    }

    /**
     * Set the image of the peer
     *
     * @param image new image
     */
    public void setPeerImage(byte[] image)
    {
        if (image == null || image.length <= 0)
        {
            GuiActivator.getContactList().setSourceContactImage(
                peerName, photoLabel, 100, 100);
        }
        else
        {
            this.peerImage = ImageUtils.getScaledRoundedIcon(image, 100, 100);

            this.peerImage = getPhotoLabelIcon();

            synchronized (videoContainers)
            {
                if (photoLabels.size() > 0)
                    for (JLabel photoLabel : photoLabels)
                    {
                        photoLabel.setIcon(this.peerImage);
                        photoLabel.repaint();
                    }
                else
                {
                    photoLabel.setIcon(peerImage);
                    photoLabel.repaint();
                }
            }
        }
    }

    /**
     * Gets the <tt>Icon</tt> to be displayed in {@link #photoLabels}.
     *
     * @return the <tt>Icon</tt> to be displayed in {@link #photoLabels}
     */
    private ImageIcon getPhotoLabelIcon()
    {
        return
            (peerImage == null)
                ? new ImageIcon(
                        ImageLoader.getImage(ImageLoader.DEFAULT_USER_PHOTO))
                : peerImage;
    }

    /**
     * Sets the state of the contained call peer by specifying the
     * state name.
     *
     * @param oldState the previous state of the peer
     * @param newState the new state of the peer
     * @param stateString the state of the contained call peer
     */
    public void setPeerState(   CallPeerState oldState,
                                CallPeerState newState,
                                String stateString)
    {
        this.callStatusLabel.setText(stateString);

        if (newState == CallPeerState.CONNECTED
            && !CallPeerState.isOnHold(oldState)
            && !securityStatusLabel.isSecurityStatusSet())
        {
            securityStatusLabel.setSecurityOff();
        }
    }

    /**
     * Sets the mute status icon to the status panel.
     *
     * @param isMute indicates if the call with this peer is
     * muted
     */
    public void setMute(boolean isMute)
    {
        if(isMute)
        {
            muteStatusLabel.setIcon(new ImageIcon(
                ImageLoader.getImage(ImageLoader.MUTE_STATUS_ICON)));
            muteStatusLabel.setBorder(
                BorderFactory.createEmptyBorder(2, 3, 2, 3));
        }
        else
        {
            muteStatusLabel.setIcon(null);
            muteStatusLabel.setBorder(
                BorderFactory.createEmptyBorder(0, 0, 0, 0));
        }

        this.revalidate();
        this.repaint();
    }

    /**
     * Sets the "on hold" property value.
     * @param isOnHold indicates if the call with this peer is put on hold
     */
    public void setOnHold(boolean isOnHold)
    {
        if(isOnHold)
        {
            holdStatusLabel.setIcon(new ImageIcon(
                ImageLoader.getImage(ImageLoader.HOLD_STATUS_ICON)));
            holdStatusLabel.setBorder(
                BorderFactory.createEmptyBorder(2, 3, 2, 3));
        }
        else
        {
            holdStatusLabel.setIcon(null);
            holdStatusLabel.setBorder(
                BorderFactory.createEmptyBorder(0, 0, 0, 0));
        }

        this.revalidate();
        this.repaint();
    }

    /**
     * Indicates that the security is turned on.
     * <p>
     * Sets the secured status icon to the status panel and initializes/updates
     * the corresponding security details.
     * @param evt Details about the event that caused this message.
     */
    public void securityOn(CallPeerSecurityOnEvent evt)
    {
        // If the securityOn is called without a specific event, we'll just set
        // the security label status to on.
        if (evt == null)
        {
            securityStatusLabel.setSecurityOn();
            return;
        }

        SrtpControl srtpControl = evt.getSecurityController();

        if ((srtpControl.requiresSecureSignalingTransport()
            && callPeer.getProtocolProvider().isSignalingTransportSecure())
            || !srtpControl.requiresSecureSignalingTransport())
        {
            if (srtpControl instanceof ZrtpControl)
            {
                securityStatusLabel.setText("zrtp");

                if (!((ZrtpControl) srtpControl).isSecurityVerified())
                    securityStatusLabel.setSecurityPending();
                else
                    securityStatusLabel.setSecurityOn();
            }
            else
                securityStatusLabel.setSecurityOn();
        }

        // if we have some other panel, using other control
        if(!srtpControl.getClass().isInstance(
                securityPanel.getSecurityControl())
            || (securityPanel instanceof ParanoiaTimerSecurityPanel))
        {
            setSecurityPanelVisible(false);

            securityPanel
                = SecurityPanel.create(this, callPeer, srtpControl);

            if (srtpControl instanceof ZrtpControl)
                ((ZrtpSecurityPanel) securityPanel)
                    .setSecurityStatusLabel(securityStatusLabel);
        }

        securityPanel.securityOn(evt);

        boolean isSecurityLowPriority = Boolean.parseBoolean(
            GuiActivator.getResources().getSettingsString(
                "impl.gui.I_DONT_CARE_THAT_MUCH_ABOUT_SECURITY"));

        // Display ZRTP panel in case SAS was not verified or a AOR mismtach
        // was detected during creation of ZrtpSecurityPanel.
	    // Don't show panel if user does not care about security at all.
        if (srtpControl instanceof ZrtpControl
            && !isSecurityLowPriority
            && (!((ZrtpControl) srtpControl).isSecurityVerified() 
                || ((ZrtpSecurityPanel) securityPanel).isZidAorMismatch()))
        {
            setSecurityPanelVisible(true);
        }

        this.revalidate();
    }

    /**
     * Indicates that the security has gone off.
     */
    public void securityOff(CallPeerSecurityOffEvent evt)
    {
        securityStatusLabel.setText("");
        securityStatusLabel.setSecurityOff();
        if (securityStatusLabel.getBorder() == null)
            securityStatusLabel.setBorder(
                BorderFactory.createEmptyBorder(2, 5, 2, 3));

        securityPanel.securityOff(evt);
    }

    /**
     * Indicates that the security status is pending confirmation.
     */
    public void securityPending()
    {
        securityStatusLabel.setSecurityPending();
    }

    /**
     * Indicates that the security is timeouted, is not supported by the
     * other end.
     * @param evt Details about the event that caused this message.
     */
    public void securityTimeout(CallPeerSecurityTimeoutEvent evt)
    {
        if (securityPanel != null)
            securityPanel.securityTimeout(evt);
    }

    /**
     * The handler for the security event received. The security event
     * for starting establish a secure connection.
     *
     * @param evt the security started event received
     */
    public void securityNegotiationStarted(
        CallPeerSecurityNegotiationStartedEvent evt)
    {
        if(Boolean.parseBoolean(GuiActivator.getResources()
                .getSettingsString("impl.gui.PARANOIA_UI")))
        {
            SrtpControl srtpControl = null;
            if (callPeer instanceof MediaAwareCallPeer)
                srtpControl = evt.getSecurityController();

            securityPanel
                = new ParanoiaTimerSecurityPanel<SrtpControl>(srtpControl);

            setSecurityPanelVisible(true);
        }
    }

    /**
     * Sets the call peer adapter managing all related listeners.
     * @param adapter the adapter to set
     */
    public void setCallPeerAdapter(CallPeerAdapter adapter)
    {
        this.callPeerAdapter = adapter;
    }

    /**
     * Returns the call peer adapter managing all related listeners.
     * @return the call peer adapter
     */
    public CallPeerAdapter getCallPeerAdapter()
    {
        return callPeerAdapter;
    }

    /**
     * Returns the parent <tt>CallPanel</tt> containing this renderer.
     * @return the parent <tt>CallPanel</tt> containing this renderer
     */
    public CallPanel getCallPanel()
    {
        return callRenderer.getCallContainer();
    }

    /**
     * Prints the given DTMG character through this <tt>CallPeerRenderer</tt>.
     * @param dtmfChar the DTMF char to print
     */
    public void printDTMFTone(char dtmfChar)
    {
        dtmfLabel.setText(dtmfLabel.getText() + dtmfChar);
        if (dtmfLabel.getBorder() == null)
            dtmfLabel.setBorder(
                BorderFactory.createEmptyBorder(2, 1, 2, 5));
    }

    /**
     * Sets the reason of a call failure if one occurs. The renderer should
     * display this reason to the user.
     * @param reason the reason to display
     */
    public void setErrorReason(String reason)
    {
        if (errorMessageComponent == null)
        {
            errorMessageComponent = new JTextPane();

            JTextPane textPane = (JTextPane) errorMessageComponent;
            textPane.setEditable(false);
            textPane.setOpaque(false);

            StyledDocument doc = textPane.getStyledDocument();

            MutableAttributeSet standard = new SimpleAttributeSet();
            StyleConstants.setAlignment(standard, StyleConstants.ALIGN_CENTER);
            StyleConstants.setFontFamily(standard,
                                        textPane.getFont().getFamily());
            StyleConstants.setFontSize(standard, 12);
            doc.setParagraphAttributes(0, 0, standard, true);

            GridBagConstraints constraints = new GridBagConstraints();
            constraints.fill = GridBagConstraints.HORIZONTAL;
            constraints.gridx = 0;
            constraints.gridy = 4;
            constraints.weightx = 1;
            constraints.weighty = 0;
            constraints.insets = new Insets(5, 0, 0, 0);

            add(errorMessageComponent, constraints);
            this.revalidate();
        }

        errorMessageComponent.setText(reason);

        if (isVisible())
            errorMessageComponent.repaint();
    }

    /**
     * Reloads all icons.
     */
    public void loadSkin()
    {
        if(localLevel != null)
            localLevel.setIcon(new ImageIcon(
                ImageLoader.getImage(ImageLoader.MICROPHONE)));

        if(remoteLevel != null)
            remoteLevel.setIcon(new ImageIcon(
                ImageLoader.getImage(ImageLoader.HEADPHONE)));

        if(muteStatusLabel.getIcon() != null)
            muteStatusLabel.setIcon(new ImageIcon(
                ImageLoader.getImage(ImageLoader.MUTE_STATUS_ICON)));

        if(holdStatusLabel.getIcon() != null)
            holdStatusLabel.setIcon(new ImageIcon(
                ImageLoader.getImage(ImageLoader.HOLD_STATUS_ICON)));

        securityStatusLabel.setIcon(new ImageIcon(
                ImageLoader.getImage(securityImageID)));

        if(this.peerImage == null)
        {
            synchronized (videoContainers)
            {
                for (JLabel photoLabel : photoLabels)
                    photoLabel.setIcon(new ImageIcon(
                        ImageLoader.getImage(ImageLoader.DEFAULT_USER_PHOTO)));
            }
        }
    }

    /**
     * Shows/hides the local video component.
     *
     * @param isVisible <tt>true</tt> to show the local video, <tt>false</tt> -
     * otherwise
     */
    public void setLocalVideoVisible(boolean isVisible)
    {
        callRenderer.getVideoHandler().setLocalVideoVisible(isVisible);
    }

    /**
     * Indicates if the local video component is currently visible.
     *
     * @return <tt>true</tt> if the local video component is currently visible,
     * <tt>false</tt> - otherwise
     */
    public boolean isLocalVideoVisible()
    {
        return callRenderer.getVideoHandler().isLocalVideoVisible();
    }

    /**
     * Returns the parent call renderer.
     *
     * @return the parent call renderer
     */
    public CallRenderer getCallRenderer()
    {
        return callRenderer;
    }

    /**
     * Returns the component associated with this renderer.
     *
     * @return the component associated with this renderer
     */
    public Component getComponent()
    {
        return this;
    }

    /**
     * Initializes the security status label, shown in the call status bar.
     */
    private void initSecurityStatusLabel()
    {
        securityStatusLabel.setBorder(
            BorderFactory.createEmptyBorder(2, 5, 2, 5));

        securityStatusLabel.addMouseListener(new MouseAdapter()
        {
            /**
             * Invoked when a mouse button has been pressed on a component.
             */
            public void mousePressed(MouseEvent e)
            {
                CallPeerSecurityStatusEvent securityEvt
                    = callPeer.getCurrentSecuritySettings();

                // Only show the security details if the security is on.
                if (securityEvt instanceof CallPeerSecurityOnEvent
                    && ((CallPeerSecurityOnEvent) securityEvt)
                        .getSecurityController() instanceof ZrtpControl)
                {
                    setSecurityPanelVisible(!callRenderer.getCallContainer()
                        .getCallWindow().getFrame().getGlassPane().isVisible());
                }
            }
        });
    }

    /**
     * Shows/hides the security panel.
     *
     * @param isVisible <tt>true</tt> to show the security panel, <tt>false</tt>
     * to hide it
     */
    public void setSecurityPanelVisible(boolean isVisible)
    {
        final JFrame callFrame = callRenderer.getCallContainer()
            .getCallWindow().getFrame();

        final JPanel glassPane = (JPanel) callFrame.getGlassPane();

        if (!isVisible)
        {
            // Need to hide the security panel explicitly in order to keep the
            // fade effect.
            securityPanel.setVisible(false);
            glassPane.setVisible(false);
            glassPane.removeAll();
        }
        else
        {
            glassPane.setLayout(null);
            glassPane.addMouseListener(new MouseListener()
            {
                public void mouseReleased(MouseEvent e)
                {
                    redispatchMouseEvent(glassPane, e);
                }

                public void mousePressed(MouseEvent e)
                {
                    redispatchMouseEvent(glassPane, e);
                }

                public void mouseExited(MouseEvent e)
                {
                    redispatchMouseEvent(glassPane, e);
                }

                public void mouseEntered(MouseEvent e)
                {
                    redispatchMouseEvent(glassPane, e);
                }

                public void mouseClicked(MouseEvent e)
                {
                    redispatchMouseEvent(glassPane, e);
                }
            });

            Point securityLabelPoint = securityStatusLabel.getLocation();

            Point newPoint
                = SwingUtilities.convertPoint(securityStatusLabel.getParent(),
                securityLabelPoint.x, securityLabelPoint.y,
                callFrame);

            securityPanel.setBeginPoint(
                new Point((int) newPoint.getX() + 15, 0));
            securityPanel.setBounds(
                0, (int) newPoint.getY() - 5, this.getWidth(), 110);

            glassPane.add(securityPanel);
            // Need to show the security panel explicitly in order to keep the
            // fade effect.
            securityPanel.setVisible(true);
            glassPane.setVisible(true);

            glassPane.addComponentListener(new ComponentAdapter()
            {
                /**
                 * Invoked when the component's size changes.
                 */
                public void componentResized(ComponentEvent e)
                {
                    if (glassPane.isVisible())
                    {
                        glassPane.setVisible(false);
                        callFrame.removeComponentListener(this);
                    }
                }
            });
        }
    }

    /**
     * Re-dispatches glass pane mouse events only in case they occur on the
     * security panel.
     *
     * @param glassPane the glass pane
     * @param e the mouse event in question
     */
    private void redispatchMouseEvent(Component glassPane, MouseEvent e)
    {
        Point glassPanePoint = e.getPoint();

        Point securityPanelPoint = SwingUtilities.convertPoint(
                                        glassPane,
                                        glassPanePoint,
                                        securityPanel);

        Component component;
        Point componentPoint;

        if (securityPanelPoint.y > 0)
        {
            component = securityPanel;
            componentPoint = securityPanelPoint;
        }
        else
        {
            Container contentPane
                = callRenderer.getCallContainer().getCallWindow()
                                    .getFrame().getContentPane();

            Point containerPoint = SwingUtilities.convertPoint(
                                                    glassPane,
                                                    glassPanePoint,
                                                    contentPane);

            component = SwingUtilities.getDeepestComponentAt(contentPane,
                    containerPoint.x, containerPoint.y);

            componentPoint = SwingUtilities.convertPoint(contentPane,
                glassPanePoint, component);
        }

        if (component != null)
            component.dispatchEvent(new MouseEvent( component,
                                                    e.getID(),
                                                    e.getWhen(),
                                                    e.getModifiers(),
                                                    componentPoint.x,
                                                    componentPoint.y,
                                                    e.getClickCount(),
                                                    e.isPopupTrigger()));

        e.consume();
    }
}
