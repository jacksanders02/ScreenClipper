package com.jacksanders.screenclipper;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;

/**
 * A class used by {@link MonitorOverlay} to control the screen area being captured.
 * @author Jack Sanders
 * @version 1.0.0 20/05/2022
 */
public class ScreencapController extends JPanel {
    /** {@link Logger} object used to generate .log files */
    private static final Logger LOG = LogManager.getLogger(ScreencapController.class);

    /** {@link java.awt.Rectangle} that stores the original point clicked by the user */
    private final Rectangle originalRect;

    /** {@link java.awt.Rectangle} that stores the full area of the screen capture */
    private Rectangle rect;

    /**
     * Constructor for {@link ScreencapController}.
     * @param s The start point of a screen capture
     */
    public ScreencapController(Point s) {
        // Make a copy of original rect, used to ensure each drawn rect is made up of only two points
        originalRect = new Rectangle(s);
        rect = new Rectangle(originalRect);
        setOpaque(false); // Set opaque - Keep translucent background of JFrame
        LOG.info("ScreenshotPanel initialised successfully.");
    }

    /**
     * Paints a hollow rectangle around the area being captured
     * @param g The {@link Graphics} object used to do the drawing
     */
    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        g.setColor(Color.WHITE);
        g.drawRect(rect.x, rect.y, rect.width, rect.height);
    }

    /**
     * @return The area captured by this screenshot
     */
    public Rectangle capture() {
        return rect;
    }

    /**
     * Updates the rect instance variable to contain both the original point, and the mouse cursor's current point.
     * @param e The {@link MouseEvent} that triggered this update
     */
    public void update(MouseEvent e) {
        rect = new Rectangle(originalRect);
        rect.add(e.getPoint()); // Update rectangle to include current mouse point
        repaint();
    }
}
