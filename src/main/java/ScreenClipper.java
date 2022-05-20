import com.melloware.jintellitype.*;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class ScreenClipper extends JFrame implements IntellitypeListener, HotkeyListener {
    // Create a logger
    private static final Logger LOG = LogManager.getLogger(ScreenClipper.class);

    // Create a robot, for taking screenshots
    private static Robot robot = null;
    // Handle AWTException in robot creation
    static {
        try {
            robot = new Robot();
        } catch (AWTException e) {
            LOG.error(e.getMessage());
        }
    }

    // Array of graphics devices (monitors)
    private static GraphicsDevice[] devices = GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices();

    // Get default screen, and that screen's scale factor
    private static GraphicsDevice defaultScreenDevice = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
    private static double screenScale = defaultScreenDevice.getDisplayMode().getWidth() / (double) defaultScreenDevice.getDefaultConfiguration().getBounds().width;

    // Instance variables
    private Rectangle screenBounds;
    private ScreenshotPanel screenshotPanel;

    public ScreenClipper() {
        // Set frame to be undecorated (cannot be moved, no title bar/border), with a translucent background
        setUndecorated(true);
        setBackground(new Color(0, 0, 0, 100));

        saveImage(robot.createScreenCapture(new Rectangle(-1920, -645, 1920, 1080)));
    }

    public static void main(String[] args) {
        // first check to see if an instance of this application is already
        // running, use the name of the window title of this JFrame for checking
        if (JIntellitype.checkInstanceAlreadyRunning("ScreenClipper")) {
            System.exit(1);
        }

        // next check to make sure JIntellitype DLL can be found and we are on
        // a Windows operating System
        if (!JIntellitype.isJIntellitypeSupported()) {
            LOG.error("Non-Windows operating system, or a problem with the JIntellitype library!");
            System.exit(1);
        }

        ScreenClipper app = new ScreenClipper();
        app.setTitle("ScreenClipper");

        // Initialise JIntellitype
        app.initJIntellitype();
    }

    @Override
    public void onHotKey(int i) {
        // Hotkey handlers
        if (i == 1001) { // ALT+A
            if (isVisible()) {
                // Hide if shown - allow user to 'back out' without screenshotting
                setVisible(false);
            } else {
                calculateScreenBounds(); // Update screen bounds

                setSize(screenBounds.getSize());
                // Divide coordinates by scale factor - fixes frame being placed off screen if primary screen is scaled
                // (e.g. laptops)
                setLocation((int) (screenBounds.getLocation().x / screenScale), (int) (screenBounds.getLocation().y / screenScale));
                setVisible(true); // Toggle visibility of overlay

                addMouseListeners();
            }
        }
    }

    @Override
    public void onIntellitype(int i) {

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
                captureImage(captureRect, e.getLocationOnScreen());
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

    private void adjustRect(Rectangle r, Point mouseEnd) {
        System.out.println(r);
        System.out.println(mouseEnd);
        for(GraphicsDevice d : devices) {
            Rectangle bounds = d.getDefaultConfiguration().getBounds();
            if (bounds.contains(mouseEnd)) {
                System.out.println(bounds);
                double scaleFactor = d.getDisplayMode().getWidth() / (double) d.getDefaultConfiguration().getBounds().width;
                double multFactor = screenScale / scaleFactor;

                r.x *= multFactor;
                r.y *= multFactor;

                r.x += (screenBounds.x / scaleFactor);
                r.y += (screenBounds.y / scaleFactor);

                r.width *= multFactor;
                r.height *= multFactor;
            }
        }
        System.out.println(r);
        System.out.println("------------");
    }

    private void captureImage(Rectangle r, Point mouseEnd) {
        adjustRect(r, mouseEnd);
        BufferedImage capture = robot.createScreenCapture(r);
        saveImage(capture);
    }

    private void saveImage(BufferedImage b) {
        File outFile = new File("temp.png");
        try {
            ImageIO.write(b, "png", outFile);
        } catch (IOException e) {
            LOG.error(e.getMessage());
        }
    }

    private void calculateScreenBounds() {
        // Update on the off-chance the user has updated their graphics devices during running
        devices = GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices();
        defaultScreenDevice = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
        screenScale = defaultScreenDevice.getDisplayMode().getWidth() / (double) defaultScreenDevice.getDefaultConfiguration().getBounds().width;

        screenBounds = devices[0].getDefaultConfiguration().getBounds(); // Initially default screen bounds

        // Add bounds of other screens, so clipper covers all available space
        for (int i=1; i<devices.length; i++) {
            Rectangle b = devices[i].getDefaultConfiguration().getBounds();
            screenBounds.add(b);
        }
    }

    private void registerHotkeys() {
        // Create all required JIntelliType hotkeys with unique identifiers
        JIntellitype.getInstance().registerHotKey(1001, JIntellitype.MOD_ALT, 'A');
        LOG.info("Hotkeys Successfully Registered!");
    }

    public void initJIntellitype() {
        try {
            // initialize JIntellitype with the frame so all windows commands can
            // be attached to this window
            JIntellitype.getInstance().addHotKeyListener(this);
            JIntellitype.getInstance().addIntellitypeListener(this);
            LOG.info("JIntellitype initialized");
            registerHotkeys();
        } catch (RuntimeException ex) {
            LOG.error("Either you are not on Windows, or there is a problem with the JIntellitype library!");
        }
    }
}
