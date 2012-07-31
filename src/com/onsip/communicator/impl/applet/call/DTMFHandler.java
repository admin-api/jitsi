package com.onsip.communicator.impl.applet.call;

import java.awt.event.KeyEvent;
import java.util.Iterator;

import org.jitsi.service.audionotifier.AudioNotifierService;
import org.jitsi.service.audionotifier.SCAudioClip;
import net.java.sip.communicator.service.protocol.Call;
import net.java.sip.communicator.service.protocol.CallPeer;
import net.java.sip.communicator.service.protocol.CallPeerState;
import org.jitsi.service.protocol.DTMFTone;
import net.java.sip.communicator.service.protocol.OperationSetDTMF;
import org.jitsi.service.resources.ResourceManagementService;
import net.java.sip.communicator.util.Logger;

import com.onsip.communicator.impl.applet.AppletActivator;

public class DTMFHandler
{
    private static final Logger logger
        = Logger.getLogger(DTMFHandler.class);    
    
    /**
     * The zero tone sound id.
     */
    public static String DIAL_ZERO;

    /**
     * The one tone sound id.
     */
    public static String DIAL_ONE;

    /**
     * The two tone sound id.
     */
    public static String DIAL_TWO;

    /**
     * The three tone sound id.
     */
    public static String DIAL_THREE;

    /**
     * The four tone sound id.
     */
    public static String DIAL_FOUR;

    /**
     * The five tone sound id.
     */
    public static String DIAL_FIVE;

    /**
     * The six tone sound id.
     */
    public static String DIAL_SIX;

    /**
     * The seven tone sound id.
     */
    public static String DIAL_SEVEN;

    /**
     * The eight tone sound id.
     */
    public static String DIAL_EIGHT;

    /**
     * The nine tone sound id.
     */
    public static String DIAL_NINE;

    /**
     * The diez tone sound id.
     */
    public static String DIAL_DIEZ;

    /**
     * The star tone sound id.
     */
    public static String DIAL_STAR;

    
    private static DTMFToneInfo[] tones = null; 
    
    /**
     * If we are currently playing an audio for a DTMF tone. Used
     * to play in Loop and stop it if forced to do or new tone has come.
     */
    private static SCAudioClip currentlyPlayingAudio = null;
    
    /**
     * All available tones and its properties like images for buttons, and
     * sounds to be played during send.
     */
    static void initDTMFTones() 
    {      
        ResourceManagementService resources = AppletActivator.getResources();
        
        DIAL_ZERO = resources.getSoundPath("DIAL_ZERO");
        DIAL_ONE = resources.getSoundPath("DIAL_ONE");
        DIAL_TWO = resources.getSoundPath("DIAL_TWO");
        DIAL_THREE = resources.getSoundPath("DIAL_THREE");
        DIAL_FOUR = resources.getSoundPath("DIAL_FOUR");
        DIAL_FIVE = resources.getSoundPath("DIAL_FIVE");
        DIAL_SIX = resources.getSoundPath("DIAL_SIX");
        DIAL_SEVEN = resources.getSoundPath("DIAL_SEVEN");
        DIAL_EIGHT = resources.getSoundPath("DIAL_EIGHT");
        DIAL_NINE = resources.getSoundPath("DIAL_NINE");
        DIAL_DIEZ = resources.getSoundPath("DIAL_DIEZ");
        DIAL_STAR = resources.getSoundPath("DIAL_STAR");
                
                
        DTMFToneInfo[] availableTones = new DTMFToneInfo[] {                                                 
        new DTMFToneInfo(
            DTMFTone.DTMF_1,
            KeyEvent.VK_1,
            '1',            
            DIAL_ONE),
        new DTMFToneInfo(
            DTMFTone.DTMF_2,
            KeyEvent.VK_2,
            '2',            
            DIAL_TWO),
        new DTMFToneInfo(
            DTMFTone.DTMF_3,
            KeyEvent.VK_3,
            '3',            
            DIAL_THREE),
        new DTMFToneInfo(
            DTMFTone.DTMF_4,
            KeyEvent.VK_4,
            '4',            
            DIAL_FOUR),
        new DTMFToneInfo(
            DTMFTone.DTMF_5,
            KeyEvent.VK_5,
            '5',            
            DIAL_FIVE),
        new DTMFToneInfo(
            DTMFTone.DTMF_6,
            KeyEvent.VK_6,
            '6',            
            DIAL_SIX),
        new DTMFToneInfo(
            DTMFTone.DTMF_7,
            KeyEvent.VK_7,
            '7',            
            DIAL_SEVEN),
        new DTMFToneInfo(
            DTMFTone.DTMF_8,
            KeyEvent.VK_8,
            '8',            
            DIAL_EIGHT),
        new DTMFToneInfo(
            DTMFTone.DTMF_9,
            KeyEvent.VK_9,
            '9',            
            DIAL_NINE),
        new DTMFToneInfo(
            DTMFTone.DTMF_A,
            KeyEvent.VK_A,
            'a',            
            null),
        new DTMFToneInfo(
            DTMFTone.DTMF_B,
            KeyEvent.VK_B,
            'b',            
            null),
        new DTMFToneInfo(
            DTMFTone.DTMF_C,
            KeyEvent.VK_C,
            'c',            
            null),
        new DTMFToneInfo(
            DTMFTone.DTMF_D,
            KeyEvent.VK_D,
            'd',            
            null),
        new DTMFToneInfo(
            DTMFTone.DTMF_STAR,
            KeyEvent.VK_ASTERISK,
            '*',            
            DIAL_STAR),
        new DTMFToneInfo(
            DTMFTone.DTMF_0,
            KeyEvent.VK_0,
            '0',            
            DIAL_ZERO),
        new DTMFToneInfo(
            DTMFTone.DTMF_SHARP,
            KeyEvent.VK_NUMBER_SIGN,
            '#',            
            DIAL_DIEZ)          
        };
        tones = availableTones;
    };
    
    public synchronized static DTMFToneInfo[] getDTMFTones()
    {
        if (tones == null)
        {
            initDTMFTones();
        }
        return tones;        
    }
    
    /**
     * Sends a DTMF tone to the current DTMF operation set.
     *
     * @param info The DTMF tone to send.
     */
    public static synchronized void startSendingDtmfTone
        (DTMFToneInfo info, Call call)
    {                
        AudioNotifierService audioNotifier = AppletActivator.getAudioNotifier();
                
        if (info.getSound() != null)
        {            
            if (currentlyPlayingAudio != null)
                currentlyPlayingAudio.stop();

            currentlyPlayingAudio = audioNotifier.createAudio(info.getSound());

            // some little silence, must have a non-zero or it won't loop
            currentlyPlayingAudio.playInLoop(10);
        }

        if (call != null)
        {
            Iterator<? extends CallPeer> callPeers =
                call.getCallPeers();
                    
            try
            {
                while (callPeers.hasNext())
                {                    
                    CallPeer peer = callPeers.next();
                    OperationSetDTMF dtmfOpSet =
                        peer.getProtocolProvider().getOperationSet(
                            OperationSetDTMF.class);
    
                    if (dtmfOpSet != null)
                    {                   
                        if (peer.getState() == CallPeerState.CONNECTED)
                        {
                            dtmfOpSet.startSendingDTMF(peer, info.getTone());
                        }
                    }
                }
            }
            catch (Throwable e1)
            {                
               logger.error("Failed to send a DTMF tone. " + e1.getMessage(), e1);
            }  
        }            
    }

    /**
     * Stop sending DTMF tone.
     */
    public static synchronized void stopSendingDtmfTone(Call call)
    {
        if (currentlyPlayingAudio != null)
            currentlyPlayingAudio.stop();

        currentlyPlayingAudio = null;

        Iterator<? extends CallPeer> callPeers =
            call.getCallPeers();

        try
        {
            while (callPeers.hasNext())
            {
                CallPeer peer = callPeers.next();
                OperationSetDTMF dtmfOpSet =
                    peer.getProtocolProvider().getOperationSet(
                        OperationSetDTMF.class);

                if (dtmfOpSet != null)
                {
                    if (peer.getState() == CallPeerState.CONNECTED)
                    {
                        dtmfOpSet.stopSendingDTMF(peer);
                    }
                }
            }
        }
        catch (Throwable e1)
        {
            logger.error("Failed to send a DTMF tone " + e1.getMessage(), e1);
        }
    }
}
