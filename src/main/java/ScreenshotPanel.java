import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;

public class ScreenshotPanel extends JPanel {
    // Create a logger
    private static final Logger LOG = LogManager.getLogger(ScreenshotPanel.class);

    private final Rectangle originalRect;
    private Rectangle rect;
    public ScreenshotPanel(Point s) {
        // Make a copy of original rect, used to ensure each drawn rect is made up of only two points
        originalRect = new Rectangle(s);
        rect = new Rectangle(originalRect);
        setOpaque(false); // Set opaque - Keep translucent background of JFrame
        LOG.info("ScreenshotPanel initialised successfully.");
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        g.setColor(Color.WHITE);
        g.drawRect(rect.x, rect.y, rect.width, rect.height);
    }

    public Rectangle capture() {
        return rect;
    }

    public void update(MouseEvent e) {
        rect = new Rectangle(originalRect);
        rect.add(e.getPoint()); // Update rectangle to include current mouse point
        repaint();
    }
}
