/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.call;

import java.awt.*;

/**
 * Represents a <code>LayoutManager</code> which centers the first
 * <code>Component</code> within its <code>Container</code> and, if the
 * preferred size of the <code>Component</code> is larger than the size of the
 * <code>Container</code>, scales the former within the bounds of the latter
 * while preserving the aspect ratio. <code>FitLayout</code> is appropriate for
 * <code>Container</code>s which display a single image or video
 * <code>Component</code> in its entirety for which preserving the aspect ratio
 * is important.
 * 
 * @author Lubomir Marinov
 */
public class FitLayout
    implements LayoutManager
{

    /*
     * Does nothing because this LayoutManager lays out only the first Component
     * of the parent Container and thus doesn't need String associations.
     */
    public void addLayoutComponent(String name, Component comp)
    {
    }

    /**
     * Gets the first <code>Component</code> of a specific
     * <code>Container</code> if there is such a <code>Component</code>.
     * 
     * @param parent the <code>Container</code> to retrieve the first
     *            <code>Component</code> of
     * @return the first <code>Component</code> of a specific
     *         <code>Container</code> if there is such a <code>Component</code>;
     *         otherwise, <tt>null</tt>
     */
    protected Component getComponent(Container parent)
    {
        Component[] components = parent.getComponents();

        return (components.length > 0) ? components[0] : null;
    }

    /*
     * Scales the first Component if its preferred size is larger than the size
     * of its parent Container in order to display the Component in its entirety
     * and then centers it within the display area of the parent.
     */
    public void layoutContainer(Container parent)
    {
        Component component = getComponent(parent);

        if (component != null)
        {
            Dimension componentSize = component.getPreferredSize();
            Dimension parentSize = parent.getSize();
            boolean scale = false;
            double ratio = 1;

            if ((componentSize.width > parentSize.width)
                && (componentSize.width > 0))
            {
                scale = true;
                ratio = parentSize.width / (double) componentSize.width;
            }
            if ((componentSize.height > parentSize.height)
                && (componentSize.height > 0))
            {
                scale = true;
                ratio =
                    Math.min(parentSize.height / (double) componentSize.height,
                        ratio);
            }
            if (scale)
            {
                componentSize.width = (int) (componentSize.width * ratio);
                componentSize.height = (int) (componentSize.height * ratio);
            }

            component.setBounds((parentSize.width - componentSize.width) / 2,
                (parentSize.height - componentSize.height) / 2,
                componentSize.width, componentSize.height);
        }
    }

    /*
     * Since this LayoutManager lays out only the first Component of the
     * specified parent Container, the minimum size of the Container is the
     * minimum size of the mentioned Component.
     */
    public Dimension minimumLayoutSize(Container parent)
    {
        Component component = getComponent(parent);

        return (component != null) ? component.getMinimumSize()
            : new Dimension(0, 0);
    }

    /*
     * Since this LayoutManager lays out only the first Component of the
     * specified parent Container, the preferred size of the Container is the
     * preferred size of the mentioned Component.
     */
    public Dimension preferredLayoutSize(Container parent)
    {
        Component component = getComponent(parent);

        return (component != null) ? component.getPreferredSize()
            : new Dimension(0, 0);
    }

    /*
     * Does nothing because this LayoutManager lays out only the first Component
     * of the parent Container and thus doesn't need String associations.
     */
    public void removeLayoutComponent(Component comp)
    {
    }
}
