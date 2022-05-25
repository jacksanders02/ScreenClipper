/*
 * Copyright (c) 2022 Jack Sanders
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.jacksanders.screenclipper;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;

/**
 * A class used by {@link MonitorOverlay} to control the screen area being captured.
 * @author Jack Sanders
 * @version 1.0.0 20/05/2022
 */
class ScreencapController extends JPanel {

    /** {@link java.awt.Rectangle} that stores the original point clicked by the user */
    private final Rectangle originalRect;

    /** {@link java.awt.Rectangle} that stores the full area of the screen capture */
    private Rectangle rect;

    /**
     * Constructor for {@link ScreencapController}.
     * @param s The start point of a screen capture
     */
    protected ScreencapController(Point s) {
        // Make a copy of original rect, used to ensure each drawn rect is made up of only two points
        originalRect = new Rectangle(s);
        rect = new Rectangle(originalRect);
        setOpaque(false); // Set opaque - Keep translucent background of JFrame
    }

    /**
     * Paints a hollow rectangle around the area being captured
     * @param g The {@link Graphics} object used to do the drawing
     */
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        g.setColor(Color.WHITE);
        g.drawRect(rect.x, rect.y, rect.width, rect.height);
    }

    /**
     * @return The area captured by this screenshot
     */
    protected Rectangle capture() {  return rect; }

    /**
     * Method that coerces a given value to a range.
     * @param min The minimum value of the range
     * @param max The maximum value of the range
     * @param val The value to coerce
     * @return min, if val < min. max, if val > max. val, if val is between min and max
     */
    private int coerceToRange(int min, int max, int val) { return Math.min(Math.max(min, val), max); }

    /**
     * Updates the rect instance variable to contain both the original point, and the mouse cursor's current point.
     * @param e The {@link MouseEvent} that triggered this update
     */
    protected void update(MouseEvent e) {
        rect = new Rectangle(originalRect);
        Point newPoint = e.getPoint();

        newPoint.x = coerceToRange(0, getWidth() - 1, newPoint.x);
        newPoint.y = coerceToRange(0, getHeight() - 1, newPoint.y);

        rect.add(newPoint);
        repaint();
    }
}
