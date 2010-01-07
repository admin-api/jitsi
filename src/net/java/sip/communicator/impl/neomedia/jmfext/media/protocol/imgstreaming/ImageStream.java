/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia.jmfext.media.protocol.imgstreaming;

import java.awt.*;
import java.awt.image.*;
import java.io.*;

import javax.media.*;
import javax.media.format.*;
import javax.media.protocol.*;

import net.java.sip.communicator.impl.neomedia.imgstreaming.*;
import net.java.sip.communicator.impl.neomedia.jmfext.media.protocol.*;
import net.java.sip.communicator.util.*;

/**
 * The stream used by JMF for our image streaming.
 * 
 * This class launches a thread to handle desktop capture interactions.
 *
 * @author Sebastien Vincent
 * @author Lubomir Marinov
 * @author Damian Minkov
 */
public class ImageStream
    extends AbstractPushBufferStream
    implements Runnable
{
    /**
     * The <tt>Logger</tt>
     */
    private static final Logger logger = Logger.getLogger(ImageStream.class);

    /**
     * Current format used.
     */
    private Format currentFormat = null;

    /**
     * Sequence number.
     */
    private long seqNo = 0;

    /**
     * Capture thread reference.
     */
    private Thread captureThread = null;

    /**
     * If stream is started or not.
     */
    private boolean started = false;

    /**
     * Buffer for the last image.
     */
    private final Buffer buf = new Buffer();

    /**
     * Desktop interaction (screen capture, key press, ...).
     */
    private DesktopInteract desktopInteract = null;

    /**
     * Constructor.
     */
    public ImageStream()
    {
    }

    /**
     * Constructor.
     *
     * @param locator <tt>MediaLocator</tt> to use
     */
    public ImageStream(MediaLocator locator)
    {
    }

    /**
     * Set format to use.
     *
     * @param format new format to use
     */
    public void setFormat(Format format)
    {
        currentFormat = format;
    }

    /**
     * Returns the supported format by this stream.
     *
     * @return supported formats
     */
    public Format getFormat()
    {
        return currentFormat;
    }

    /**
     * Block and read a buffer from the stream.
     *
     * @param buffer the <tt>Buffer</tt> to read captured media into
     * @throws IOException if an error occurs while reading.
     */
    public void read(Buffer buffer)
        throws IOException
    {
        synchronized(buf)
        {
            try
            {
                Object bufData = buf.getData();
                int bufLength = buf.getLength();

                if ((bufData != null) || (bufLength != 0))
                {
                    buffer.setData(bufData);
                    buffer.setOffset(0);
                    buffer.setLength(bufLength);
                    buffer.setFormat(buf.getFormat());
                    buffer.setHeader(null);
                    buffer.setTimeStamp(buf.getTimeStamp());
                    buffer.setSequenceNumber(buf.getSequenceNumber());
                    buffer.setFlags(buf.getFlags());

                    /* clear buf so JMF will not get twice the same image */
                    buf.setData(null);
                    buf.setLength(0);
                }
            }
            catch (Exception e)
            {
            }
        }
    }

    /**
     * Start desktop capture stream.
     */
    public void start()
    {
        if(captureThread == null || !captureThread.isAlive())
        {
            logger.info("Start stream");
            captureThread = new Thread(this);
            captureThread.start();
            started = true;
        }
    }

    /**
     * Stop desktop capture stream.
     */
    public void stop()
    {
        logger.info("Stop stream");
        started = false;
        captureThread = null;
    }

    /**
     * Thread entry point.
     */
    public void run()
    {
        final RGBFormat format = (RGBFormat)currentFormat;
        Dimension formatSize = format.getSize();
        final int width = (int)formatSize.getWidth();
        final int height = (int)formatSize.getHeight();

        if(desktopInteract == null)
        {
            try
            {
                desktopInteract = new DesktopInteractImpl();
            }
            catch(Exception e)
            {
                logger.warn("Cannot create DesktopInteract object!");
                started = false;
                return;
            }
        }

        while(started)
        {
            byte data[] = null;
            BufferedImage scaledScreen = null;
            BufferedImage screen = null;

            /* get desktop screen and resize it */
            screen = desktopInteract.captureScreen();
            scaledScreen = ImageStreamingUtils.getScaledImage(screen, 
                    width, height, BufferedImage.TYPE_INT_ARGB);

            /* get raw bytes */
            data = ImageStreamingUtils.getImageByte(scaledScreen);

            /* notify JMF that new data is available */
            synchronized (buf)
            {
                buf.setData(data);
                buf.setOffset(0);
                buf.setLength(data.length);
                buf.setFormat(currentFormat);
                buf.setHeader(null);
                buf.setTimeStamp(System.nanoTime());
                buf.setSequenceNumber(seqNo++);
                buf.setFlags(Buffer.FLAG_LIVE_DATA | Buffer.FLAG_SYSTEM_TIME);
            }

            /* pass to JMF handler */
            BufferTransferHandler transferHandler = this.transferHandler;

            if(transferHandler != null)
            {
                transferHandler.transferData(this);
                Thread.yield();
            }

            /* cleanup */
            screen = null;
            scaledScreen = null;
            data = null;

            try
            {
                /* 100 ms */
                Thread.sleep(100);
            }
            catch(InterruptedException e)
            {
                /* do nothing */
            }
        }
    }
}
