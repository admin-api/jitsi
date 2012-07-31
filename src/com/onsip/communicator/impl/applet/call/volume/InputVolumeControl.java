package com.onsip.communicator.impl.applet.call.volume;

import net.java.sip.communicator.util.Logger;

import org.jitsi.service.neomedia.*;

public class InputVolumeControl
    extends BaseVolumeControl
{
    private final static Logger logger
        = Logger.getLogger(InputVolumeControl.class);

    public InputVolumeControl(MediaService ms)
    {
        logger.debug("instantiating InputVolumeControl");
        volumeControl = ms.getInputVolumeControl();
    }
}
