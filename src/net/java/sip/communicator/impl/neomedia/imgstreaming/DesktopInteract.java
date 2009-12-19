/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia.imgstreaming;

import java.awt.*;
import java.awt.image.*;

/**
 * Interface to interact with the desktop such as taking
 * screen capture or to generate mouse/keyboard event.
 * 
 * @author Sebastien Vincent
 */
public interface DesktopInteract
{
    /**
     * Capture the full desktop screen.
     *
     * @return <tt>BufferedImage</tt> of the desktop screen
     */
    public BufferedImage captureScreen();

    /**
     * Capture a part of the desktop screen.
     *
     * @return <tt>BufferedImage</tt> of a part of the desktop screen
     * or null if <tt>Robot</tt> problem
     */
    public BufferedImage captureScreen(int x, int y, int width, int height);

    /**
     * Generates keyPress event.
     *
     * @param keycode keycode the user hit
     */
    public void keyPress(int keycode);

    /**
     * Generates keyRelease event.
     *
     * @param keycode keycode the user hit
     */
    public void keyRelease(int keycode);

    /**
     * Generates mouseMove event.
     *
     * @param x position x in the screen
     * @param y position y in the screen
     */
    public void mouseMove(int x, int y);

    /**
     * Generates mousePress event.
     *
     * @param buttons buttons mask (right, middle, left)
     */
    public void mousePress(int buttons);

    /**
     * Generates mouseRelease event.
     *
     * @param buttons buttons mask (right, middle, left)
     */
    public void mouseRelease(int buttons);

    /**
     * Generates mouseWheel event.
     *
     * @param wheelAmt "notches"
     */
    public void mouseWheel(int wheelAmt);
}
