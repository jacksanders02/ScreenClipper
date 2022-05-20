package com.jacksanders.screenclipper;

import com.melloware.jintellitype.HotkeyListener;
import com.melloware.jintellitype.IntellitypeListener;
import com.melloware.jintellitype.JIntellitype;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

/**
 * The main class, that controls the app. It is responsible for handling hotkeys, calculating monitor coordinates, and
 * assigning a {@link MonitorOverlay} to each different screen.
 *
 * @author Jack Sanders
 * @version 1.0.0 20/05/2022
 */
public class ScreenClipper implements IntellitypeListener, HotkeyListener {
    /** {@link Logger} object used to generate .log files */
    private static final Logger LOG = LogManager.getLogger(ScreenClipper.class);

    /** {@link Robot} object used to create screen captures */
    private static Robot robot = null;

    // Handle AWTException in robot creation
    static {
        try {
            robot = new Robot();
        } catch (AWTException e) {
            LOG.error(e.getMessage());
        }
    }

    /** Stores position and size details of each different screen/monitor */
    private ArrayList<Rectangle> screenRects = new ArrayList<>();

    /** Stores a {@link MonitorOverlay} for each screen */
    private ArrayList<MonitorOverlay> overlays = new ArrayList<>();

    public static void main(String[] args) {
        // first check to see if an instance of this application is already
        // running, use the name of the window title of this JFrame for checking
        if (JIntellitype.checkInstanceAlreadyRunning("ScreenClipper")) {
            System.exit(1);
        }

        // next check to make sure JIntellitype DLL can be found and we are on
        // a Windows operating System
        if (!JIntellitype.isJIntellitypeSupported()) {
            LOG.fatal("Non-Windows operating system, or a problem with the JIntellitype library!");
            System.exit(1);
        }

        ScreenClipper app = new ScreenClipper();

        // Initialise JIntellitype
        app.initJIntellitype();
    }

    /**
     * Controls what happens when an Intellitype hotkey is detected.
     * @param i the ID of the hotkey pressed
     */
    @Override
    public void onHotKey(int i) {
        // Hotkey handlers
        if (i == 1001) { // ALT+A
            calculateScreenBounds(); // Update display information
            for (MonitorOverlay o : overlays) {
                o.toggle();
            }
        }
    }

    @Override
    public void onIntellitype(int i) {}


    /**
     * Creates a screen capture from {@link MonitorOverlay}s. This method is called from a MouseReleased event listener,
     * and so there must be a complete screen capture. The method iterates through all overlays to find the one that
     * has a {@link ScreencapController} object drawn onto it, and then gets the area of that screencap. This is passed
     * to robot.createScreenCapture, which creates a {@link BufferedImage}, which is saved,
     * <br><br>
     * Needs to iterate through all overlays rather than just using the one that triggered the MouseReleased event, as
     * user may move mouse from one monitor to another. This means that the overlay the mouse was released on may not
     * be the one they started drawing on, and thus will not have a valid screencap.
     */
    public void createNewScreenCapture() {
        Rectangle r = null;
        int monitorID = -1;
        for(MonitorOverlay o : overlays) {
            if (o.hasCapture()) {
                r = o.getCaptureRect();
                monitorID = o.getCaptureID();
            }
            o.reset(); // Clear monitor overlays
        }

        if (monitorID != -1 && r != null) {
            LOG.info("Attempting new screen capture at " + r.getLocation() + " with size " + r.getSize());
            Rectangle screen = screenRects.get(monitorID);
            r.translate(screen.x, screen.y);
            saveImage(robot.createScreenCapture(r));
            LOG.info("Screen capture taken successfully!");
        }
    }

    /**
     * Saves a given {@link BufferedImage} to a file, temp.png
     * @param b The BufferedImage to save
     */
    private void saveImage(BufferedImage b) {
        File outFile = new File("temp.png");
        try {
            ImageIO.write(b, "png", outFile);
        } catch (IOException e) {
            LOG.fatal(e.getMessage());
        }
    }

    /**
     * Iterates through available {@link GraphicsDevice}s, and adds their dimensions to the screenRects ArrayList. Also
     * creates an overlay for each screen (or updates the corrosponding one if it already exists).
     */
    private void calculateScreenBounds() {
        screenRects.clear();

        // Update on the off-chance the user has updated their graphics devices during running
        GraphicsDevice[] devices = GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices();

        for (int i=0; i<devices.length; i++) {
            Rectangle bounds = devices[i].getDefaultConfiguration().getBounds();
            screenRects.add(bounds);

            // Update overlays so that current screen is covered
            if (overlays.size() > i) {
                overlays.get(i).updateCoveredMonitor(bounds);
            } else {
                overlays.add(new MonitorOverlay(bounds, this, i));
            }
        }
    }

    /**
     * Register all hotkeys used with JIntellitype
     */
    private void registerHotkeys() {
        // Create all required JIntelliType hotkeys with unique identifiers
        JIntellitype.getInstance().registerHotKey(1001, JIntellitype.MOD_ALT, 'A');
        LOG.info("Hotkeys Successfully Registered!");
    }

    /**
     * Initialise JIntellitype, to attach its commands and hotkeys to this app.
     */
    public void initJIntellitype() {
        try {
            JIntellitype.getInstance().addHotKeyListener(this);
            JIntellitype.getInstance().addIntellitypeListener(this);
            LOG.info("JIntellitype initialized");
            registerHotkeys();
        } catch (RuntimeException ex) {
            LOG.fatal("Either you are not on Windows, or there is a problem with the JIntellitype library!");
        }
    }
}
