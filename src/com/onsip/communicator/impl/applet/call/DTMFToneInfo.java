package com.onsip.communicator.impl.applet.call;

import org.jitsi.service.protocol.DTMFTone;

/**
 * DTMF extended information.
 */
public class DTMFToneInfo
{
    /**
     * The tone itself
     */
    DTMFTone tone;

    /**
     * The key code when entered from keyboard.
     */
    private int keyCode;

    /**
     * The char associated with this DTMF tone.
     */
    private char keyChar;

    /**
     * The sound to play during send of this tone.
     */
    private String sound;

    /**
     * Creates DTMF extended info.
     * @param tone the tone.
     * @param keyCode its key code.
     * @param keyChar the char associated with the DTMF
     * @param sound the sound if any.
     */
    public DTMFToneInfo(
        DTMFTone tone, int keyCode, char keyChar,
        String sound)
    {
        this.tone = tone;
        this.keyCode = keyCode;
        this.keyChar = keyChar;            
        this.sound = sound;
    }
    
    public DTMFTone getTone()
    {
        return this.tone;
    }
    
    public String getSound()
    {
        return sound;
    }
    
    public int getKeyCode()
    {
        return keyCode;
    }
    
    public int getKeyChar()
    {
        return keyChar;
    }
}