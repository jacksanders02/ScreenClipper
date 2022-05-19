import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;

import com.melloware.jintellitype.*;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

public class ScreenClipper extends JFrame implements IntellitypeListener, HotkeyListener {
    // Create a logger
    private static final Logger LOG = LogManager.getLogger(ScreenClipper.class);

    // Get default screen, and that screen's scale factor
    private static final GraphicsDevice DEFAULT_SCREEN = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
    private static final double SCREEN_SCALE = DEFAULT_SCREEN.getDisplayMode().getWidth() / (double) DEFAULT_SCREEN.getDefaultConfiguration().getBounds().width;

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

    // Instance variables
    private Rectangle screenBounds;

    public ScreenClipper() {
        // Set frame to be undecorated (cannot be moved, no title bar/border), with a translucent background
        setUndecorated(true);
        setBackground(new Color(0, 0, 0, 100));
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
            calculateScreenBounds(); // Update screen bounds

            // Frame will be opened on main screen, so multiply bounds by main screen scale factor to fill all screens
            setSize((int)(screenBounds.getSize().width * SCREEN_SCALE), (int)(screenBounds.getHeight() * SCREEN_SCALE));

            // Divide coordinates by scale factor - fixes frame being placed off screen
            setLocation((int)(screenBounds.getLocation().x / SCREEN_SCALE), (int)(screenBounds.getLocation().y / SCREEN_SCALE));
            setVisible(true);
        }
    }

    @Override
    public void onIntellitype(int i) {

    }

    private void calculateScreenBounds() {
        GraphicsDevice[] devices = GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices();
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
