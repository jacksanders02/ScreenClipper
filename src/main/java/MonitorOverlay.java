import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class MonitorOverlay extends JFrame {
    private Rectangle coveredScreen;
    private ScreenshotPanel screenshotPanel;

    public MonitorOverlay(Rectangle m) {
        updateCoveredMonitor(m); // Cover entire designated screen

        // Set frame to be undecorated (cannot be moved, no title bar/border), with a translucent background
        setUndecorated(true);
        setBackground(new Color(0, 0, 0, 100));
    }

    public void toggle() {
        if (isVisible()) {
            // Hide if shown - allow user to 'back out' without screenshotting
            setVisible(false);
        } else {
            setVisible(true); // Toggle visibility of overlay
            addMouseListeners();
        }
    }

    public void updateCoveredMonitor(Rectangle m) {
        coveredScreen = m;
        setLocation(coveredScreen.getLocation());
        setSize(coveredScreen.getSize());
    }

    private void addMouseListeners() {
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                // Create new instance of ScreenshotPanel, that will draw an outline around the area being captured
                screenshotPanel = new ScreenshotPanel(e.getPoint());
                screenshotPanel.setSize(getSize()); // Fill screen with screenshotPanel
                add(screenshotPanel);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                Rectangle captureRect = screenshotPanel.capture();
                remove(screenshotPanel);
                screenshotPanel = null; // Remove reference - garbage collection
                destroyMouseListeners(); // Remove listeners - no longer needed
                setVisible(false);
            }
        });

        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                screenshotPanel.update(e);
            }
        });
    }

    private void destroyMouseListeners() {
        for (MouseListener ml : getMouseListeners()) {
            removeMouseListener(ml);
        }

        for (MouseMotionListener mml : getMouseMotionListeners()) {
            removeMouseMotionListener(mml);
        }
    }
}
