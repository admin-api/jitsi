/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia.device;

import java.io.*;
import java.util.*;

import javax.media.*;
import javax.media.control.*;
import javax.media.protocol.*;

import net.java.sip.communicator.impl.neomedia.*;
import net.java.sip.communicator.impl.neomedia.codec.*;
import net.java.sip.communicator.impl.neomedia.format.*;
import net.java.sip.communicator.service.neomedia.*;
import net.java.sip.communicator.service.neomedia.device.*;
import net.java.sip.communicator.service.neomedia.format.*;
import net.java.sip.communicator.util.*;

/**
 * Implements <tt>MediaDevice</tt> for the JMF <tt>CaptureDevice</tt>.
 *
 * @author Lubomir Marinov
 */
public class MediaDeviceImpl
    extends AbstractMediaDevice
{

    /**
     * The <tt>Logger</tt> used by <tt>MediaDeviceImpl</tt> and its instances
     * for logging output.
     */
    private static final Logger logger
        = Logger.getLogger(MediaDeviceImpl.class);

    /**
     * The JMF <tt>CaptureDevice</tt> this instance wraps and provides an
     * implementation of <tt>MediaDevice</tt> for.
     */
    private CaptureDevice captureDevice;

    /**
     * The <tt>CaptureDeviceInfo</tt> of {@link #captureDevice}.
     */
    private CaptureDeviceInfo captureDeviceInfo;

    /**
     * The indicator which determines whether {@link DataSource#connect()} has
     * been successfully executed on {@link #captureDevice}.
     */
    private boolean captureDeviceIsConnected;

    /**
     * The <tt>MediaType</tt> of this instance and the <tt>CaptureDevice</tt>
     * that it wraps.
     */
    private final MediaType mediaType;

    /**
     * Initializes a new <tt>MediaDeviceImpl</tt> instance with a specific
     * <tt>MediaType</tt> and with <tt>MediaDirection</tt> which does not allow
     * sending.
     *
     * @param mediaType the <tt>MediaType</tt> of the new instance
     */
    public MediaDeviceImpl(MediaType mediaType)
    {
        this.captureDevice = null;
        this.captureDeviceInfo = null;
        this.mediaType = mediaType;
    }

    /**
     * Initializes a new <tt>MediaDeviceImpl</tt> instance which is to provide
     * an implementation of <tt>MediaDevice</tt> for a specific
     * <tt>CaptureDevice</tt> with a specific <tt>MediaType</tt>.
     *
     * @param captureDevice the JMF <tt>CaptureDevice</tt> the new instance is
     * to provide an implementation of <tt>MediaDevice</tt> for
     * @param mediaType the <tt>MediaType</tt> of the new instance
     */
    public MediaDeviceImpl(CaptureDevice captureDevice, MediaType mediaType)
    {
        if (captureDevice == null)
            throw new NullPointerException("captureDevice");
        if (mediaType == null)
            throw new NullPointerException("mediaType");

        this.mediaType = mediaType;

        setCaptureDevice(captureDevice);
    }

    /**
     * Initializes a new <tt>MediaDeviceImpl</tt> instance which is to provide
     * an implementation of <tt>MediaDevice</tt> for a <tt>CaptureDevice</tt>
     * with a specific <tt>CaptureDeviceInfo</tt> and which is of a specific
     * <tt>MediaType</tt>.
     *
     * @param captureDeviceInfo the <tt>CaptureDeviceInfo</tt> of the JMF
     * <tt>CaptureDevice</tt> the new instance is to provide an implementation
     * of <tt>MediaDevice</tt> for
     * @param mediaType the <tt>MediaType</tt> of the new instance
     */
    public MediaDeviceImpl(
            CaptureDeviceInfo captureDeviceInfo,
            MediaType mediaType)
    {
        if (captureDeviceInfo == null)
            throw new NullPointerException("captureDeviceInfo");
        if (mediaType == null)
            throw new NullPointerException("mediaType");

        this.captureDevice = null;
        this.captureDeviceInfo = captureDeviceInfo;
        this.mediaType = mediaType;
    }

    /**
     * Notifies this instance that its <tt>captureDevice</tt> (the JMF
     * <tt>CaptureDevice</tt> this instance wraps and provides an implementation
     * of <tt>MediaDevice</tt> for) property has changed its value from
     * <tt>oldValue</tt> to <tt>newValue</tt>. Allows extenders to override in
     * order to perform additional processing of the new <tt>captureDevice</tt>
     * once it is clear that it is set into this instance.
     *
     * @param oldValue the JMF <tt>CaptureDevice</tt> which was the value of the
     * <tt>captureDevice</tt> property of this instance before <tt>newValue</tt>
     * was set
     * @param newValue the JMF <tt>CaptureDevice</tt> which is the value of the
     * <tt>captureDevice</tt> property of this instance and which replaced
     * <tt>oldValue</tt>
     */
    protected void captureDeviceChanged(
            CaptureDevice oldValue,
            CaptureDevice newValue)
    {
    }

    /**
     * Creates a <tt>DataSource</tt> instance for this <tt>MediaDevice</tt>
     * which gives access to the captured media.
     *
     * @return a <tt>DataSource</tt> instance which gives access to the media
     * captured by this <tt>MediaDevice</tt>
     * @see AbstractMediaDevice#createOutputDataSource()
     */
    DataSource createOutputDataSource()
    {
        return
            getDirection().allowsSending()
                ? (DataSource) getConnectedCaptureDevice()
                : null;
    }

    /**
     * Gets the JMF <tt>CaptureDevice</tt> this instance wraps and provides an
     * implementation of <tt>MediaDevice</tt> for.
     *
     * @return the JMF <tt>CaptureDevice</tt> this instance wraps and provides
     * an implementation of <tt>MediaDevice</tt> for
     */
    public CaptureDevice getCaptureDevice()
    {
        return getCaptureDevice(true);
    }

    /**
     * Gets the JMF <tt>CaptureDevice</tt> this instance wraps and provides an
     * implementation of <tt>MediaDevice</tt> for and, optionally, creates it if
     * it does not exist yet.
     *
     * @param create <tt>true</tt> to create the <tt>CaptureDevice</tt> this
     * instance provides an implementation of <tt>MediaDevice</tt> for it it
     * does not exist yet; <tt>false</tt> to not create it and return
     * <tt>null</tt> if it does not exist yet
     * @return the JMF <tt>CaptureDevice</tt> this instance wraps and provides
     * an implementation of <tt>MediaDevice</tt> for if it exists or
     * <tt>create</tt> is <tt>true</tt> and its creation succeeds; <tt>null</tt>
     * if it does not exist yet and <tt>create</tt> is <tt>false</tt> or its
     * creation fails
     */
    protected CaptureDevice getCaptureDevice(boolean create)
    {
        if (getDirection().allowsSending() && (captureDevice == null) && create)
        {
            CaptureDevice captureDevice = null;
            Throwable exception = null;

            try
            {
                captureDevice
                    = (CaptureDevice)
                        Manager
                            .createDataSource(captureDeviceInfo.getLocator());
            }
            catch (IOException ioe)
            {
                // TODO
                exception = ioe;
            }
            catch (NoDataSourceException ndse)
            {
                // TODO
                exception = ndse;
            }

            if (exception != null)
                logger
                    .error(
                        "Failed to create CaptureDevice DataSource "
                            + "from CaptureDeviceInfo "
                            + captureDeviceInfo,
                        exception);
            else
            {

                // Try to enable tracing on captureDevice.
                if (logger.isTraceEnabled()
                        && (captureDevice instanceof PushBufferDataSource))
                    captureDevice
                        = new CaptureDeviceDelegatePushBufferDataSource(
                                captureDevice)
                        {
                            @Override
                            public void connect()
                                throws IOException
                            {
                                super.connect();

                                if (logger.isTraceEnabled())
                                    logger
                                        .trace(
                                            "Connected "
                                                + MediaDeviceImpl.toString(this.captureDevice));
                            }

                            @Override
                            public void disconnect()
                            {
                                super.disconnect();

                                if (logger.isTraceEnabled())
                                    logger
                                        .trace(
                                            "Disconnected "
                                                + MediaDeviceImpl.toString(this.captureDevice));
                            }

                            @Override
                            public void start()
                                throws IOException
                            {
                                super.start();

                                if (logger.isTraceEnabled())
                                    logger
                                        .trace(
                                            "Started "
                                                + MediaDeviceImpl.toString(this.captureDevice));
                            }

                            @Override
                            public void stop()
                                throws IOException
                            {
                                super.stop();

                                if (logger.isTraceEnabled())
                                    logger
                                        .trace(
                                            "Stopped "
                                                + MediaDeviceImpl.toString(this.captureDevice));
                            }
                        };

                setCaptureDevice(captureDevice);
            }
        }
        return captureDevice;
    }

    /**
     * Gets the <tt>CaptureDeviceInfo</tt> of the JMF <tt>CaptureDevice</tt>
     * represented by this instance.
     *
     * @return the <tt>CaptureDeviceInfo</tt> of the <tt>CaptureDevice</tt>
     * represented by this instance
     */
    public CaptureDeviceInfo getCaptureDeviceInfo()
    {
        return captureDeviceInfo;
    }

    /**
     * Gets the JMF <tt>CaptureDevice</tt> this instance wraps and provides an
     * implementation of <tt>MediaDevice</tt> for in a connected state. If the
     * <tt>CaptureDevice</tt> is not connected to yet, first tries to connect to
     * it. Returns <tt>null</tt> if this instance has failed to create a
     * <tt>CaptureDevice</tt> instance or to connect to it.
     *
     * @return the JMF <tt>CaptureDevice</tt> this instance wraps and provides
     * an implementation of <tt>MediaDevice</tt> for in a connected state;
     * <tt>null</tt> if this instance has failed to create a
     * <tt>CaptureDevice</tt> instance or to connect to it
     */
    private CaptureDevice getConnectedCaptureDevice()
    {
        CaptureDevice captureDevice = getCaptureDevice();

        if ((captureDevice != null) && !captureDeviceIsConnected)
        {
            Throwable exception = null;

            try
            {
                captureDevice.connect();
            }
            catch (IOException ioe)
            {
                // TODO
                exception = ioe;
            }
            catch (NullPointerException npe)
            {
                /*
                 * TODO The old media says it happens when the operating system
                 * does not support the operation.
                 */
                exception = npe;
            }

            if (exception == null)
            {
                captureDeviceIsConnected = true;

                /*
                 * 1. Changing buffer size. The default buffer size (for
                 * javasound) is 125 milliseconds - 1/8 sec. On MacOS this leads
                 * to an exception and no audio capture. A value of 30 for the
                 * buffer fixes the problem and is OK when using some pstn
                 * gateways.
                 * 
                 * 2. Changing to 60. When it is 30 there are some issues with
                 * asterisk and nat (we don't start to send stream and so
                 * asterisk rtp part doesn't notice that we are behind nat)
                 * 
                 * 3. Do not set buffer length on linux as it completely breaks
                 * audio capture.
                 */
                String osName = System.getProperty("os.name");

                if ((osName == null) || !osName.toLowerCase().contains("linux"))
                {
                    Control bufferControl
                        = (Control)
                            ((DataSource) captureDevice)
                                .getControl(
                                    "javax.media.control.BufferControl");

                    if (bufferControl != null)
                        ((BufferControl) bufferControl)
                            .setBufferLength(60); // in milliseconds
                }
            }
            else
                captureDevice = null;
        }
        return captureDevice;
    }

    /**
     * Returns the <tt>MediaDirection</tt> supported by this device.
     *
     * @return {@link MediaDirection#SENDONLY} if this is a read-only device,
     * {@link MediaDirection#RECVONLY} if this is a write-only device or
     * {@link MediaDirection#SENDRECV} if this <tt>MediaDevice</tt> can both
     * capture and render media
     * @see MediaDevice#getDirection()
     */
    public MediaDirection getDirection()
    {
        if ((captureDeviceInfo != null) || (captureDevice != null))
            return MediaDirection.SENDRECV;
        else
        {
            /*
             * If there is no audio CaptureDevice, then even play back is not
             * possible.
             */
            return
                MediaType.AUDIO.equals(getMediaType())
                    ? MediaDirection.INACTIVE
                    : MediaDirection.RECVONLY;
        }
    }

    /**
     * Gets the <tt>MediaFormat</tt> in which this <t>MediaDevice</tt> captures
     * media.
     *
     * @return the <tt>MediaFormat</tt> in which this <tt>MediaDevice</tt>
     * captures media
     * @see MediaDevice#getFormat()
     */
    public MediaFormat getFormat()
    {
        CaptureDevice captureDevice = getCaptureDevice();

        if (captureDevice != null)
        {
            MediaType mediaType = getMediaType();

            for (FormatControl formatControl
                    : captureDevice.getFormatControls())
            {
                MediaFormat format
                    = MediaFormatImpl.createInstance(formatControl.getFormat());

                if ((format != null) && format.getMediaType().equals(mediaType))
                    return format;
            }
        }
        return null;
    }

    /**
     * Gets the <tt>MediaType</tt> that this device supports.
     *
     * @return {@link MediaType#AUDIO} if this is an audio device or
     * {@link MediaType#VIDEO} if this is a video device
     * @see MediaDevice#getMediaType()
     */
    public MediaType getMediaType()
    {
        return mediaType;
    }

    /**
     * Gets a list of <tt>MediaFormat</tt>s supported by this
     * <tt>MediaDevice</tt>.
     *
     * @return the list of <tt>MediaFormat</tt>s supported by this device
     * @see MediaDevice#getSupportedFormats()
     */
    public List<MediaFormat> getSupportedFormats()
    {
        EncodingConfiguration encodingConfiguration
            = NeomediaActivator
                .getMediaServiceImpl().getEncodingConfiguration();
        MediaFormat[] supportedEncodings
            = encodingConfiguration.getSupportedEncodings(getMediaType());
        List<MediaFormat> supportedFormats = new ArrayList<MediaFormat>();

        if (supportedEncodings != null)
            for (MediaFormat supportedEncoding : supportedEncodings)
                supportedFormats.add(supportedEncoding);

        return supportedFormats;
    }

    /**
     * Determines whether this <tt>MediaDevice</tt> will provide silence instead
     * of actual captured data next time it is read.
     *
     * @return <tt>true</tt> if this <tt>MediaDevice</tt> will provide silence
     * instead of actual captured data next time it is read; <tt>false</tt>,
     * otherwise
     */
    public boolean isMute()
    {
        CaptureDevice captureDevice = getCaptureDevice(false);

        if (captureDevice instanceof MutePushBufferDataSource)
            return ((MutePushBufferDataSource) captureDevice).isMute();

        /*
         * If there is no underlying CaptureDevice, this instance is mute
         * because it cannot capture any media.
         */
        return !getDirection().allowsSending();
    }

    /**
     * Sets the JMF <tt>CaptureDevice</tt> this instance wraps and provides a
     * <tt>MediaDevice</tt> implementation for. Allows extenders to override in
     * order to customize <tt>captureDevice</tt> including to replace it before
     * it is set into this instance.
     *
     * @param captureDevice the JMF <tt>CaptureDevice</tt> this instance is to
     * wrap and provide a <tt>MediaDevice</tt> implementation for
     */
    protected void setCaptureDevice(CaptureDevice captureDevice)
    {
        // Try to enable mute support on the specified CaptureDevice.
        if (captureDevice instanceof PushBufferDataSource)
            captureDevice
                = new MutePushBufferDataSource(
                        (PushBufferDataSource) captureDevice);

        if (this.captureDevice != captureDevice)
        {
            CaptureDevice oldValue = this.captureDevice;

            this.captureDevice = captureDevice;
            this.captureDeviceInfo = captureDevice.getCaptureDeviceInfo();

            CaptureDevice newValue = captureDevice;

            captureDeviceChanged(oldValue, newValue);
        }
    }

    /**
     * Sets the indicator which determines whether this <tt>MediaDevice</tt>
     * will start providing silence instead of actual captured data next time it
     * is read.
     *
     * @param mute <tt>true</tt> to have this <tt>MediaDevice</tt> start
     * providing silence instead of actual captured data next time it is read;
     * otherwise, <tt>false</tt>
     */
    public void setMute(boolean mute)
    {
        CaptureDevice captureDevice = getCaptureDevice();

        if (captureDevice instanceof MutePushBufferDataSource)
            ((MutePushBufferDataSource) captureDevice).setMute(mute);
    }

    /**
     * Gets a human-readable <tt>String</tt> representation of this instance.
     *
     * @return a <tt>String</tt> providing a human-readable representation of
     * this instance
     */
    @Override
    public String toString()
    {
        CaptureDeviceInfo captureDeviceInfo = getCaptureDeviceInfo();

        return
            (captureDeviceInfo == null)
                ? super.toString()
                : captureDeviceInfo.toString();
    }

    /**
     * Returns a human-readable representation of a specific
     * <tt>CaptureDevice</tt> in the form of a <tt>String</tt> value.
     *
     * @param captureDevice the <tt>CaptureDevice</tt> to get a human-readable
     * representation of
     * @return a <tt>String</tt> value which gives a human-readable
     * representation of the specified <tt>captureDevice</tt>
     */
    private static String toString(CaptureDevice captureDevice)
    {
        return
            "CaptureDevice with hashCode "
                + captureDevice.hashCode()
                + " and captureDeviceInfo "
                + captureDevice.getCaptureDeviceInfo();
    }
}