/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.call;

import java.awt.*;
import java.beans.*;
import java.util.*;

import javax.swing.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.media.*;
import net.java.sip.communicator.util.*;
import net.java.sip.communicator.util.swing.*;

import org.ice4j.ice.*;
import org.jitsi.service.neomedia.*;
import org.jitsi.service.resources.*;
import org.jitsi.util.*;

import com.explodingpixels.macwidgets.*;

/**
 * The frame displaying the statistical information for a call.
 *
 * @author Vincent Lucas
 * @author Yana Stamcheva
 */
public class CallInfoFrame
    implements CallTitleListener,
               PropertyChangeListener
{
    /**
     * The Call from which are computed the statisics displayed.
     */
    private Call call;

    /**
     * The call info window.
     */
    private final JDialog callInfoWindow;

    /**
     * The information text pane.
     */
    private final JEditorPane infoTextPane;

    /**
     * The font color.
     */
    private String fontColor;

    /**
     * The resource management service.
     */
    private final ResourceManagementService resources
        = GuiActivator.getResources();

    /**
     * Creates a new frame containing the statistical information for a call.
     *
     * @param call The call from which are computed the statistics displayed.
     */
    public CallInfoFrame(Call call)
    {
        this.call = call;

        JScrollPane scrollPane = new JScrollPane();
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);

        infoTextPane = createGeneralInfoPane();
        scrollPane.getViewport().add(infoTextPane);

        callInfoWindow
            = createCallInfoWindow(
                GuiActivator.getResources().getI18NString(
                    "service.gui.callinfo.TECHNICAL_CALL_INFO"));

        callInfoWindow.getContentPane().add(scrollPane);

        this.constructCallInfo();
    }

    /**
     * Creates different types of windows depending on the operating system.
     *
     * @param title the title of the created window
     */
    private JDialog createCallInfoWindow( String title)
    {
        JDialog callInfoWindow = null;

        if (OSUtils.IS_MAC)
        {
            HudWindow window = new HudWindow();

            JDialog dialog = window.getJDialog();
            dialog.setTitle(title);

            callInfoWindow = window.getJDialog();
            callInfoWindow.setResizable(true);

            fontColor = "FFFFFF";
        }
        else
        {
            SIPCommDialog dialog = new SIPCommDialog(false);

            callInfoWindow = dialog;
            callInfoWindow.setTitle(title);

            fontColor = "000000";
        }

        return callInfoWindow;
    }

    /**
     * Create call info text pane.
     *
     * @return the created call info text pane
     */
    private JEditorPane createGeneralInfoPane()
    {
        JEditorPane infoTextPane = new JEditorPane();

        /*
         * Make JEditorPane respect our default font because we will be using it
         * to just display text.
         */
        infoTextPane.putClientProperty(
                JEditorPane.HONOR_DISPLAY_PROPERTIES,
                true);

        infoTextPane.setOpaque(false);
        infoTextPane.setEditable(false);
        infoTextPane.setContentType("text/html");

        return infoTextPane;
    }

    /**
     * Returns an HTML string corresponding to the given labelText and infoText,
     * that could be easily added to the information text pane.
     *
     * @param labelText the label text that would be shown in bold
     * @param infoText the info text that would be shown in plain text
     * @return the newly constructed HTML string
     */
    private String getLineString(String labelText, String infoText)
    {
        return "<b>" + labelText + "</b> : " + infoText + "<br/>";
    }

    /**
     * Constructs the call info text.
     */
    private void constructCallInfo()
    {
        StringBuffer stringBuffer = new StringBuffer();

        stringBuffer.append(
            "<html><body><p align=\"left\">"
                + "<font color=\"" + fontColor + "\" size=\"3\">");

        stringBuffer.append(getLineString(resources.getI18NString(
            "service.gui.callinfo.CALL_INFORMATION"), ""));

        stringBuffer.append(getLineString(resources.getI18NString(
            "service.gui.callinfo.CALL_IDENTITY"),
            call.getProtocolProvider().getAccountID().getDisplayName()));

        int callPeerCount = call.getCallPeerCount();
        if (call.getCallPeerCount() > 1)
            stringBuffer.append(
                getLineString(resources.getI18NString(
                    "service.gui.callinfo.PEER_COUNT"),
                    String.valueOf(callPeerCount)));

        boolean isConfFocus = call.isConferenceFocus();

        if (isConfFocus)
            stringBuffer.append(getLineString(
                resources.getI18NString(
                    "service.gui.callinfo.IS_CONFERENCE_FOCUS"),
                String.valueOf(isConfFocus)));

        TransportProtocol preferredTransport =
            call.getProtocolProvider().getTransportProtocol();

        if (preferredTransport != TransportProtocol.UNKNOWN)
            stringBuffer.append(getLineString(
                resources.getI18NString("service.gui.callinfo.CALL_TRANSPORT"),
                preferredTransport.toString()));

        constructCallPeersInfo(stringBuffer);

        stringBuffer.append("</font></p></body></html>");

        infoTextPane.setText(stringBuffer.toString());
        infoTextPane.revalidate();
        infoTextPane.repaint();
    }

    /**
     * Constructs call peers' info.
     *
     * @param stringBuffer the <tt>StringBuffer</tt>, where call peer info will
     * be added
     */
    private void constructCallPeersInfo(StringBuffer stringBuffer)
    {
        Iterator<? extends CallPeer> callPeers = this.call.getCallPeers();

        while(callPeers.hasNext())
        {
            CallPeer callPeer = callPeers.next();
            if(callPeer instanceof MediaAwareCallPeer)
            {
                ((MediaAwareCallPeer)callPeer).getMediaHandler()
                    .addPropertyChangeListener(this);
            }
            stringBuffer.append("<br/>");
            constructPeerInfo(callPeer, stringBuffer);
        }
    }

    /**
     * Constructs peer info.
     *
     * @param callPeer the <tt>CallPeer</tt>, for which we'll construct the info
     * @param stringBuffer the <tt>StringBuffer</tt>, where call peer info will
     * be added
     */
    private void constructPeerInfo(CallPeer callPeer, StringBuffer stringBuffer)
    {
        stringBuffer.append(getLineString(callPeer.getAddress(), ""));

        if(callPeer.getCallDurationStartTime() !=
                CallPeer.CALL_DURATION_START_TIME_UNKNOWN)
        {
            Date startTime = new Date(callPeer.getCallDurationStartTime());
            stringBuffer.append(getLineString(
                resources.getI18NString("service.gui.callinfo.CALL_DURATION"),
                GuiUtils.formatTime(startTime.getTime(),
                                    System.currentTimeMillis())));
        }

        if(callPeer instanceof MediaAwareCallPeer)
        {
            CallPeerMediaHandler callPeerMediaHandler
                = ((MediaAwareCallPeer) callPeer).getMediaHandler();

            if(callPeerMediaHandler != null)
            {
                String iceCandidateExtendedType =
                    callPeerMediaHandler.getICECandidateExtendedType();
                if(iceCandidateExtendedType != null)
                {
                    stringBuffer.append(getLineString(resources.getI18NString(
                            "service.gui.callinfo.ICE_CANDIDATE_EXTENDED_TYPE"),
                                iceCandidateExtendedType));
                }

                String iceState = callPeerMediaHandler.getICEState();
                if(iceState != null && !iceState.equals("Terminated"))
                {
                    stringBuffer.append(getLineString(
                        resources.getI18NString(
                            "service.gui.callinfo.ICE_STATE"),
                        resources.getI18NString(
                            "service.gui.callinfo.ICE_STATE." + iceState)));
                }

                MediaStream mediaStream =
                    callPeerMediaHandler.getStream(MediaType.AUDIO);

                if (mediaStream != null && mediaStream.isStarted())
                {
                    stringBuffer.append("<br/>");
                    stringBuffer.append(getLineString(resources.getI18NString(
                        "service.gui.callinfo.AUDIO_INFO"), ""));

                    this.appendStreamEncryptionMethod(
                            stringBuffer,
                            callPeerMediaHandler,
                            mediaStream,
                            MediaType.AUDIO);

                    constructAudioVideoInfo(
                            mediaStream,
                            stringBuffer,
                            MediaType.AUDIO);
                }

                mediaStream = callPeerMediaHandler.getStream(MediaType.VIDEO);

                if (mediaStream != null && mediaStream.isStarted())
                {
                    stringBuffer.append("<br/>");
                    stringBuffer.append(getLineString(resources.getI18NString(
                        "service.gui.callinfo.VIDEO_INFO"), ""));

                    this.appendStreamEncryptionMethod(
                            stringBuffer,
                            callPeerMediaHandler,
                            mediaStream,
                            MediaType.VIDEO);

                    constructAudioVideoInfo(
                            mediaStream,
                            stringBuffer,
                            MediaType.VIDEO);
                }
            }
        }
    }

    /**
     * Constructs audio video peer info.
     *
     * @param mediaStream the <tt>MediaStream</tt> that gives us access to
     * audio video info
     * @param stringBuffer the <tt>StringBuffer</tt>, where call peer info will
     * be added
     * @param mediaType The media type used to determine which stream of the
     * media handler must returns it encryption method.
     */
    private void constructAudioVideoInfo(
            MediaStream mediaStream,
            StringBuffer stringBuffer,
            MediaType mediaType)
    {
        MediaStreamStats mediaStreamStats
            = mediaStream.getMediaStreamStats();

        if(mediaStreamStats == null)
            return;

        mediaStreamStats.updateStats();

        if(mediaType == MediaType.VIDEO)
        {
            Dimension downloadVideoSize =
                mediaStreamStats.getDownloadVideoSize();
            Dimension uploadVideoSize = mediaStreamStats.getUploadVideoSize();
            // Checks that at least one video stream is active.
            if(downloadVideoSize != null || uploadVideoSize != null)
            {
                stringBuffer.append(
                        getLineString(resources.getI18NString(
                                "service.gui.callinfo.VIDEO_SIZE"),
                            "&darr; "
                            + this.videoSizeToString(downloadVideoSize)
                            + " &uarr; "
                            + this.videoSizeToString(uploadVideoSize)));
            }
            // Otherwise, quit the stats for this video stream.
            else
            {
                return;
            }
        }

        stringBuffer.append(
            getLineString(
                resources.getI18NString("service.gui.callinfo.CODEC"),
                mediaStreamStats.getEncoding()
                + " / " + mediaStreamStats.getEncodingClockRate() + " Hz"));

        stringBuffer.append(
            getLineString(
                resources.getI18NString("service.gui.callinfo.LOCAL_IP"),
                mediaStreamStats.getLocalIPAddress()
                + " / " + String.valueOf(mediaStreamStats.getLocalPort())));

        stringBuffer.append(
            getLineString(
                resources.getI18NString("service.gui.callinfo.REMOTE_IP"),
                mediaStreamStats.getRemoteIPAddress()
                + " / " + String.valueOf(mediaStreamStats.getRemotePort())));

        stringBuffer.append(
            getLineString(
                resources.getI18NString(
                    "service.gui.callinfo.BANDWITH"),
                    "&darr; "
                    + (int) mediaStreamStats.getDownloadRateKiloBitPerSec()
                        + " Kbps "
                    + " &uarr; "
                    + (int) mediaStreamStats.getUploadRateKiloBitPerSec()
                        + " Kbps"));

        stringBuffer.append(
            getLineString(
                resources.getI18NString("service.gui.callinfo.LOSS_RATE"),
                    "&darr; " + (int) mediaStreamStats.getDownloadPercentLoss()
                    + "% &uarr; "
                    + (int) mediaStreamStats.getUploadPercentLoss()
                    + "%"));

        long rttMs = mediaStreamStats.getRttMs();
        if(rttMs != -1)
        {
            stringBuffer.append(
                getLineString(resources.getI18NString(
                        "service.gui.callinfo.RTT"),
                    rttMs + " ms"));
        }

        stringBuffer.append(
            getLineString(resources.getI18NString(
                    "service.gui.callinfo.JITTER"),
                "&darr; " + (int) mediaStreamStats.getDownloadJitterMs()
                + " ms &uarr; "
                + (int) mediaStreamStats.getUploadJitterMs() + " ms"));
    }

    /**
     * Called when the title of the given CallPanel changes.
     *
     * @param callContainer the <tt>CallContainer</tt>, which title has changed
     */
    public void callTitleChanged(CallPanel callContainer)
    {
        String selectedText = infoTextPane.getSelectedText();

        // If there's a selection do not update call info, otherwise the user
        // would not be able to copy the selected text.
        if (selectedText != null && selectedText.length() > 0)
            return;

        constructCallInfo();
        callInfoWindow.pack();
    }

    /**
     * Shows/hides the corresponding window.
     *
     * @param isVisible <tt>true</tt> to show the window, <tt>false</tt> to
     * hide it
     */
    public void setVisible(boolean isVisible)
    {
        if (isVisible)
        {
            callInfoWindow.pack();
            callInfoWindow.setPreferredSize(new Dimension(300, 450));
            callInfoWindow.setSize(300, 450);
            callInfoWindow.setLocationRelativeTo(null);
        }

        callInfoWindow.setVisible(isVisible);
    }

    /**
     * Indicates if the corresponding window is visible.
     *
     * @return <tt>true</tt> if the window is visible, <tt>false</tt> -
     * otherwise
     */
    public boolean isVisible()
    {
        return callInfoWindow.isVisible();
    }

    /**
     * Disposes the corresponding window.
     */
    public void dispose()
    {
        callInfoWindow.dispose();
    }

    /**
     * Appends to the string buffer the stream encryption method (null, MIKEY,
     * SDES, ZRTP) used for a given media stream (type AUDIO or VIDEO).
     *
     * @param stringBuffer The string buffer containing the call information
     * statistics.
     * @param callPeerMediaHandler The media handler containing the different
     * media streams.
     * @param mediaStream the <tt>MediaStream</tt> that gives us access to
     * audio/video info.
     * @param mediaType The media type used to determine which stream of the
     * media handler must returns it encryption method.
     */
    private void appendStreamEncryptionMethod(
            StringBuffer stringBuffer,
            CallPeerMediaHandler callPeerMediaHandler,
            MediaStream mediaStream,
            MediaType mediaType)
    {
        String transportProtocolString = "";
        StreamConnector.Protocol transportProtocol =
            mediaStream.getTransportProtocol();
        if(transportProtocol != null)
        {
            transportProtocolString = transportProtocol.toString();
        }

        String rtpType;
        SrtpControl srtpControl
            = callPeerMediaHandler.getEncryptionMethod(mediaType);
        // If the stream is secured.
        if(srtpControl != null)
        {
            String info;
            if (srtpControl instanceof ZrtpControl) 
            {
                info = "ZRTP " + ((ZrtpControl)srtpControl).getCipherString();
            }
            else
            {
                info = "SDES";
            }

            rtpType = resources.getI18NString(
                "service.gui.callinfo.MEDIA_STREAM_SRTP")
                + " (" 
                + resources.getI18NString(
                    "service.gui.callinfo.KEY_EXCHANGE_PROTOCOL")
                + ": "
                + info
                + ")";
        }
        // If the stream is not secured.
        else
        {
            rtpType = resources.getI18NString(
                            "service.gui.callinfo.MEDIA_STREAM_RTP");
        }
        // Appends the encryption status String.
        stringBuffer.append(getLineString(
                    resources.getI18NString(
                        "service.gui.callinfo.MEDIA_STREAM_TRANSPORT_PROTOCOL"),
                    transportProtocolString + " / " + rtpType));

    }

    /**
     * Listen for ice property change to trigger call info update.
     * @param evt the event for state change.
     */
    public void propertyChange(PropertyChangeEvent evt)
    {
        if(evt.getPropertyName().equals(Agent.PROPERTY_ICE_PROCESSING_STATE))
        {
            callTitleChanged(null);
        }
    }

    /**
     * Converts a video size Dimension into its String representation.
     *
     * @param videoSize The video size Dimension, containing the width and the
     * hieght of the video.
     *
     * @return The String representation of the video width and height, or a
     * String with "Not Available (N.A.)" if the videoSize is null.
     */
    private String videoSizeToString(Dimension videoSize)
    {
        if(videoSize == null)
        {
            return resources.getI18NString("service.gui.callinfo.NA");
        }
        return ((int) videoSize.getWidth()) + " x " + ((int) videoSize.getHeight());
    }
}
