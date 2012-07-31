package com.onsip.communicator.impl.applet.call.volume;

import net.java.sip.communicator.util.Logger;

import org.jitsi.service.neomedia.*;

public class OutputVolumeControl
    extends BaseVolumeControl
{
    private final static Logger logger
        = Logger.getLogger(OutputVolumeControl.class);

    public OutputVolumeControl(MediaService ms)
    {
        logger.debug("instantiating OutputVolumeControl");
        volumeControl = ms.getOutputVolumeControl();
    }
}
