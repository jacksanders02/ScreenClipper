import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class MonitorOverlay extends JFrame {
    private Rectangle coveredScreen;
    private Rectangle screencapArea = new Rectangle();
    private Point screencapStart = new Point();
    private boolean screenshot;
    private ScreenClipper parentFrame;
    private ScreencapController screencapController;
    private int captureID;

    public MonitorOverlay(Rectangle m, ScreenClipper p, int id) {
        updateCoveredMonitor(m); // Cover entire designated screen

        // Set frame to be undecorated (cannot be moved, no title bar/border), with a translucent background
        setUndecorated(true);
        setBackground(new Color(0, 0, 0, 100));

        setVisible(true);

        parentFrame = p;
        captureID = id;

        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                System.out.println(e);
                screenshot = true;
                screencapController = new ScreencapController(e.getPoint());
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                screenshot = false;
                screencapController = null;
            }
        });

        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                if (screenshot) {
                    screencapController.update(e);
                }
            }
        });
    }

    public void toggle() {
        setVisible(!isVisible());
    }

    public void updateCoveredMonitor(Rectangle m) {
        coveredScreen = m;
        setLocation(coveredScreen.getLocation());
        setSize(coveredScreen.getSize());
    }

    public void reset() {
        screencapArea = new Rectangle();
        screencapStart = new Point();
        screenshot = false;
    }
}
