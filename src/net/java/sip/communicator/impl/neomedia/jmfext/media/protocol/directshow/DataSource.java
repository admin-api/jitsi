/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia.jmfext.media.protocol.directshow;

import java.io.*;
import java.util.*;
import java.awt.Dimension;

import javax.media.*;
import javax.media.control.*;
import javax.media.format.*;

import net.java.sip.communicator.impl.neomedia.codec.video.*;
import net.java.sip.communicator.impl.neomedia.device.*;
import net.java.sip.communicator.impl.neomedia.directshow.*;
import net.java.sip.communicator.impl.neomedia.jmfext.media.protocol.*;
import net.java.sip.communicator.util.*;

/**
 * <tt>DataSource</tt> for DirectShow capture devices.
 *
 * @author Lubomir Marinov
 * @author Sebastien Vincent
 */
public class DataSource extends AbstractPushBufferCaptureDevice
{
    /**
     * The <tt>Logger</tt> used by the <tt>DataSource</tt> class and its
     * instances for logging output.
     */
    private static final Logger logger = Logger.getLogger(DataSource.class);

    /**
     * DirectShow capture device.
     */
    private DSCaptureDevice device = null;

    /**
     * DirectShow manager.
     */
    private DSManager manager = null;

    /**
     * The default width of <tt>DataSource</tt>.
     */
    static final int DEFAULT_WIDTH = 640;

    /**
     * The default height of <tt>DataSource</tt>.
     */
    static final int DEFAULT_HEIGHT = 480;

    /**
     * Constructor.
     */
    public DataSource()
    {
        manager = DSManager.getInstance();
    }

    /**
     * Initializes a new <tt>DataSource</tt> instance from a specific
     * <tt>MediaLocator</tt>.
     *
     * @param locator the <tt>MediaLocator</tt> to create the new instance from
     */
    public DataSource(MediaLocator locator)
    {
        super(locator);
        manager = DSManager.getInstance();
    }

    /**
     * Sets the <tt>MediaLocator</tt> which specifies the media source of this
     * <tt>DataSource</tt>.
     *
     * @param locator the <tt>MediaLocator</tt> which specifies the media source
     * of this <tt>DataSource</tt>
     * @see DataSource#setLocator(MediaLocator)
     */
    @Override
    public void setLocator(MediaLocator locator)
    {
        DSCaptureDevice device = null;
        super.setLocator(locator);

        locator = getLocator();

        if((locator != null) &&
                DirectShowAuto.LOCATOR_PROTOCOL.equalsIgnoreCase(
                        locator.getProtocol()))
        {
            DSCaptureDevice[] devices = manager.getCaptureDevices();

            /* find device */
            for(int i = 0 ; i < devices.length ; i++)
            {
                if(devices[i].getName().equals(locator.getRemainder()))
                {
                    device = devices[i];
                    break;
                }
            }
        }
        else
        {
            device = null;
        }
        setDevice(device);
    }

    /**
     * Sets the <tt>DSCaptureDevice</tt> which represents the media source of
     * this <tt>DataSource</tt>.
     *
     * @param device the <tt>DSCaptureDevice</tt> which represents the media
     * source of this <tt>DataSource</tt>
     */
    private void setDevice(DSCaptureDevice device)
    {
        if(this.device != device)
        {
            this.device = device;
        }
    }

    /**
     * Create a new <tt>PushBufferStream</tt> which is to be at a specific
     * zero-based index in the list of streams of this
     * <tt>PushBufferDataSource</tt>. The <tt>Format</tt>-related information of
     * the new instance is to be abstracted by a specific
     * <tt>FormatControl</tt>.
     *
     * @param streamIndex the zero-based index of the <tt>PushBufferStream</tt>
     * in the list of streams of this <tt>PushBufferDataSource</tt>
     * @param formatControl the <tt>FormatControl</tt> which is to abstract the
     * <tt>Format</tt>-related information of the new instance
     * @return a new <tt>PushBufferStream</tt> which is to be at the specified
     * <tt>streamIndex</tt> in the list of streams of this
     * <tt>PushBufferDataSource</tt> and which has its <tt>Format</tt>-related
     * information abstracted by the specified <tt>formatControl</tt>
     * @see AbstractPushBufferCaptureDevice#createStream(int, FormatControl)
     */
    protected AbstractPushBufferStream createStream(
            int streamIndex,
            FormatControl formatControl)
    {
        DirectShowStream stream = new DirectShowStream(formatControl);

        /*
        DSFormat fmts[] = device.getSupportedFormats();
        for(int i = 0 ; i < fmts.length ; i++)
        {
            System.out.println(fmts[i].getWidth() + " " + fmts[i].getHeight()
                    + fmts[i].getColorSpace());
        }
        */

        device.setDelegate(stream.grabber);
        return stream;
    }

    /**
     * Gets the <tt>Format</tt>s which are to be reported by a
     * <tt>FormatControl</tt> as supported formats for a
     * <tt>PushBufferStream</tt> at a specific zero-based index in the list of
     * streams of this <tt>PushBufferDataSource</tt>.
     *
     * @param streamIndex the zero-based index of the <tt>PushBufferStream</tt>
     * for which the specified <tt>FormatControl</tt> is to report the list of
     * supported <tt>Format</tt>s
     * @return an array of <tt>Format</tt>s to be reported by a
     * <tt>FormatControl</tt> as the supported formats for the
     * <tt>PushBufferStream</tt> at the specified <tt>streamIndex</tt> in the
     * list of streams of this <tt>PushBufferDataSource</tt>
     * @see AbstractPushBufferCaptureDevice#getSupportedFormats(int)
     */
    @Override
    protected Format[] getSupportedFormats(int streamIndex)
    {
        DSFormat fmts[] = device.getSupportedFormats();
        List<Format> formats = new ArrayList<Format>();

        for(DSFormat fmt : fmts)
        {
            Dimension size = new Dimension(fmt.getWidth(), fmt.getHeight());
            int colorSpace = 0;

            switch(fmt.getColorSpace())
            {
            case DSFormat.RGB32:
                colorSpace = FFmpeg.PIX_FMT_RGB32;
                break;
            case DSFormat.ARGB32:
                colorSpace = FFmpeg.PIX_FMT_ARGB;
                break;
            case DSFormat.RGB24:
                colorSpace = FFmpeg.PIX_FMT_RGB24;
                break;
            default:
                /* does not support other for the moment */
                continue;
            }

            formats.add(new AVFrameFormat(size, Format.NOT_SPECIFIED,
                    colorSpace));
        }

        return formats.toArray(new Format[formats.size()]);
    }

    /**
     * Attempts to set the <tt>Format</tt> to be reported by the
     * <tt>FormatControl</tt> of a <tt>PushBufferStream</tt> at a specific
     * zero-based index in the list of streams of this
     * <tt>PushBufferDataSource</tt>. The <tt>PushBufferStream</tt> does not
     * exist at the time of the attempt to set its <tt>Format</tt>.
     *
     * @param streamIndex the zero-based index of the <tt>PushBufferStream</tt>
     * the <tt>Format</tt> of which is to be set
     * @param oldValue the last-known <tt>Format</tt> for the
     * <tt>PushBufferStream</tt> at the specified <tt>streamIndex</tt>
     * @param newValue the <tt>Format</tt> which is to be set
     * @return the <tt>Format</tt> to be reported by the <tt>FormatControl</tt>
     * of the <tt>PushBufferStream</tt> at the specified <tt>streamIndex</tt>
     * in the list of streams of this <tt>PushBufferStream</tt> or <tt>null</tt>
     * if the attempt to set the <tt>Format</tt> did not success and any
     * last-known <tt>Format</tt> is to be left in effect
     * @see AbstractPushBufferCaptureDevice#setFormat(int, Format, Format)
     */
    @Override
    protected Format setFormat(
            int streamIndex,
            Format oldValue, Format newValue)
    {
        /*
         * A resolution that is too small will yield bad image quality. We've
         * decided that DEFAULT_WIDTH and DEFAULT_HEIGHT make sense as the
         * minimum resolution to request from the capture device.
         */
        if(newValue instanceof VideoFormat)
        {
            VideoFormat newVideoFormatValue = (VideoFormat) newValue;
            Dimension newSize = newVideoFormatValue.getSize();

            if((newSize != null)
                    && (newSize.width < DEFAULT_WIDTH)
                    && (newSize.height < DEFAULT_HEIGHT))
            {
                String encoding = newVideoFormatValue.getEncoding();
                Class<?> dataType = newVideoFormatValue.getDataType();
                float frameRate = newVideoFormatValue.getFrameRate();

                newSize = new Dimension(DEFAULT_WIDTH, DEFAULT_HEIGHT);
                newValue
                    = ((Format) newVideoFormatValue.clone())
                        .relax()
                            .intersects(
                                new VideoFormat(
                                        encoding,
                                        newSize,
                                        Format.NOT_SPECIFIED,
                                        dataType,
                                        frameRate));
            }

            if(newValue instanceof AVFrameFormat)
            {

                AVFrameFormat f = (AVFrameFormat)newValue;
                int colorSpace = DSFormat.UNKNOWN;
                int pixFmt = f.getPixFmt();

                if(pixFmt == FFmpeg.PIX_FMT_ARGB)
                {
                    colorSpace = DSFormat.ARGB32;
                }
                else if(pixFmt == FFmpeg.PIX_FMT_RGB32)
                {
                    colorSpace = DSFormat.RGB32;
                }
                else if(pixFmt == FFmpeg.PIX_FMT_RGB24)
                {
                    colorSpace = DSFormat.RGB24;
                }
                else
                {
                    /* unsupported */
                }

                DSFormat fmt = new DSFormat(newSize.width, newSize.height,
                        colorSpace);
                device.setFormat(fmt);
            }

            return newValue;
        }
        else
        {
            return super.setFormat(streamIndex, oldValue, newValue);
        }
    }
    /**
     * Opens a connection to the media source specified by the
     * <tt>MediaLocator</tt> of this <tt>DataSource</tt>.
     *
     * @throws IOException if anything goes wrong while opening the connection
     * to the media source specified by the <tt>MediaLocator</tt> of this
     * <tt>DataSource</tt>
     * @see AbstractPushBufferCaptureDevice#doConnect()
     */
    @Override
    protected void doConnect() throws IOException
    {
        logger.info("doConnect");

        if(manager == null)
        {
            manager = DSManager.getInstance();
        }
        super.doConnect();
    }

    /**
     * Closes the connection to the media source specified by the
     * <tt>MediaLocator</tt> of this <tt>DataSource</tt>.
     *
     * @see AbstractPushBufferCaptureDevice#doDisconnect()
     */
    @Override
    protected void doDisconnect()
    {
        logger.info("doDisconnect");
        super.doDisconnect();

        if(manager != null)
        {
            device.setDelegate(null);
            device = null;

            DSManager.dispose();
            manager = null;
        }
    }

    /**
     * Starts the transfer of media data from this <tt>DataSource</tt>.
     *
     * @throws IOException if anything goes wrong while starting the transfer of
     * media data from this <tt>DataSource</tt>
     * @see AbstractPushBufferCaptureDevice#doStart()
     */
    @Override
    protected void doStart() throws IOException
    {
        logger.info("start");
        /* open and start capture */
        device.open();
        super.doStart();
    }

    /**
     * Stops the transfer of media data from this <tt>DataSource</tt>.
     *
     * @throws IOException if anything goes wrong while stopping the transfer of
     * media data from this <tt>DataSource</tt>
     * @see AbstractPushBufferCaptureDevice#doStop()
     */
    @Override
    protected void doStop() throws IOException
    {
        logger.info("stop");
        /* close capture */
        super.doStop();
        device.close();
    }
}

