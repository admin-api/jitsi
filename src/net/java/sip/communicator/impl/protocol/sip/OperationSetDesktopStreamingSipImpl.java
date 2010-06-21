/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.sip;

import net.java.sip.communicator.service.neomedia.*;
import net.java.sip.communicator.service.protocol.*;

/**
 * Implements all desktop streaming related functions.
 *
 * @author Sebastien Vincent
 */
public class OperationSetDesktopStreamingSipImpl
    extends OperationSetVideoTelephonySipImpl
    implements OperationSetDesktopStreaming

{
    /**
     * Initializes a new <tt>OperationSetDesktopStreamingSipImpl</tt> instance
     * which builds upon the telephony-related functionality of a specific
     * <tt>OperationSetBasicTelephonySipImpl</tt>.
     *
     * @param parentProvider the SIP <tt>ProtocolProviderService</tt>
     * implementation which has requested the creation of the new instance and
     * for which the new instance is to provide video telephony
     * @param basicTelephony the <tt>OperationSetBasicTelephonySipImpl</tt>
     *            the new extension should build upon
     */
    public OperationSetDesktopStreamingSipImpl(
            ProtocolProviderServiceSipImpl parentProvider,
            OperationSetBasicTelephonySipImpl basicTelephony)
    {
        super(parentProvider, basicTelephony);
    }

    /**
     * Get the <tt>MediaUseCase</tt> of a desktop streaming operation set.
     *
     * @return <tt>MediaUseCase.DESKTOPL</tt>
     */
    @Override
    public MediaUseCase getMediaUseCase()
    {
        return MediaUseCase.DESKTOP;
    }
}
