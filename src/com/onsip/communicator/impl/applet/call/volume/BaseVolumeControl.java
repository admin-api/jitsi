package com.onsip.communicator.impl.applet.call.volume;

import org.jitsi.service.neomedia.VolumeControl;

public class BaseVolumeControl
{
    protected static final int MULTIPLIER = 100;

    protected VolumeControl volumeControl = null;

    public int getLevel()
    {
        return (int) (volumeControl.getVolume() * MULTIPLIER);
    }

    public void setVolume(int volume)
    {
        if (volume < 0)
        {
            volume = 0;
        }
        else if (volume > 100)
        {
            volume = 100;
        }
        volumeControl.setVolume((float) volume / MULTIPLIER);
    }
}
