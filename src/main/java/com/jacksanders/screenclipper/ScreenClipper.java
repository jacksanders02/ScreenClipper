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

// Distributed by Formdev under the Apache 2.0 License. See Legal/LICENSE_flatlaf.txt
import com.formdev.flatlaf.FlatLightLaf;
////

// Distributed by Melloware under the Apache 2.0 License. See Legal/LICENSE_jintellitype.txt
import com.melloware.jintellitype.HotkeyListener;
import com.melloware.jintellitype.IntellitypeListener;
import com.melloware.jintellitype.JIntellitype;
////

// Distributed by nguyenq under the Apache 2.0 License. See Legal/LICENSE_tess4j.txt
import com.recognition.software.jdeskew.ImageDeskew;
import net.sourceforge.tess4j.util.ImageHelper;
import net.sourceforge.tess4j.Tesseract1;
import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.TesseractException;
////

// Distributed by Apache Software Foundation under the Apache 2.0 License. See Legal/LICENSE_logging_log4j2.txt
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
////

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * The main class, that controls the app. It is responsible for handling hotkeys, calculating monitor coordinates, and
 * assigning a {@link MonitorOverlay} to each different screen.
 *
 * @author Jack Sanders
 * @version 1.0.0 20/05/2022
 */
public class ScreenClipper implements IntellitypeListener, HotkeyListener {
    /** HashMap with language filenames as keys, and their names as values */
    protected static final Map<String, String> LANG_MAP = parseMapFromFile("langs.txt");

    /** {@link Logger} object used to generate .log files */
    protected static final Logger LOG = LogManager.getLogger(ScreenClipper.class);

    /** Directory of non-classpath resources */
    protected static final String RESOURCE_DIR = "./resources";

    /** {@link TrayIcon} object used to control system tray behaviour */
    private static TrayIcon trayIcon;

    /** {@link Robot} object used to create screen captures */
    private static Robot robot = null;

    /** {@link Tesseract1} instance, to perform OCR */
    protected static final ITesseract TESS = new Tesseract1();

    // Static code block to initialise class variables
    static {
        TESS.setDatapath(RESOURCE_DIR + "/tessdata");
        // Handle AWTException in robot creation
        try {
            robot = new Robot();
        } catch (AWTException e) {
            LOG.error(e.toString());
        }
    }

    /** Stores a {@link MonitorOverlay} for each screen */
    private final ArrayList<MonitorOverlay> overlays = new ArrayList<>();

    public static void main(String[] args) {

        // Initialise look and feel. Start with Flat LAF, then try system default, then try swing default.
        try {
            UIManager.setLookAndFeel(new FlatLightLaf());
        } catch( Exception ex ) {
            LOG.error("Failed to initialise L&F. Reverting to system default");
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (ClassNotFoundException |
                     InstantiationException |
                     IllegalAccessException |
                     UnsupportedLookAndFeelException e) {
                LOG.error("Failed to get system default L&F. Reverting to Swing default");
            }
        }

        // first check to see if an instance of this application is already
        // running, use the name of the window title of this JFrame for checking
        if (JIntellitype.checkInstanceAlreadyRunning("ScreenClipper")) {
            System.exit(1);
        }

        // next check to make sure JIntellitype DLL can be found and we are on
        // a Windows operating System
        if (!JIntellitype.isJIntellitypeSupported()) {
            LOG.fatal("Non-Windows operating system, or a problem with the JIntellitype library.");
            System.exit(1);
        }

        ScreenClipper app = new ScreenClipper();

        // Initialise JIntellitype
        app.initJIntellitype();

        // Initialise system tray
        app.initSystemTray();
    }


    /**
     * @param fileName The name of the file (on the classpath) from which to construct a map (a file in the form key: value)
     * @return A map constructed from the file
     */
    protected static Map<String, String> parseMapFromFile(String fileName) {
        HashMap<String, String> returnMap = new HashMap<>();

        // Get InputStream from file on classpath
        InputStream is = ScreenClipper.class.getClassLoader().getResourceAsStream(fileName);

        if (is == null) {
            LOG.error("Error reading language data: Could not find langs.txt on classpath.");
        } else {
            // Create an InputStreamReader to get a char stream, wrap with BufferedReader to read char stream to lines
            BufferedReader lineReader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));

            // Split each line to key and value & pack to returnMap
            lineReader.lines().map(s -> s.split(": ")).forEach(s -> returnMap.put(s[0], s[1]));
        }
        return returnMap;
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
    protected void createNewScreenCapture() {
        Rectangle r = null;
        Rectangle screen = null;
        for(MonitorOverlay o : overlays) {
            if (o.hasCapture()) {
                r = o.getCaptureRect();
                screen = o.getScreenArea();
            }
            o.reset(); // Clear monitor overlays
        }

        if (r != null) {
            LOG.info("Attempting new screen capture at " + r.getLocation() + " with size " + r.getSize());
            r.translate(screen.x, screen.y);
            try {
                File screenCap = new File("read_from.png");
                ImageIO.write(robot.createScreenCapture(r), "png", screenCap);
                LOG.info("Screen capture taken successfully.");
                readText(processForOCR(screenCap));
            } catch (IllegalArgumentException e) {
                LOG.error("Cannot create a capture with no area.");
            } catch (IOException e) {
                LOG.error("Failed to save screen capture.");
            }
        }
    }

    /**
     * Performs some simple image processing before OCR is carried out (deskew, greyscale)
     * @param toProcess The image file to process
     * @return The processed image file
     */
    private File processForOCR(File toProcess) {
        try {
            BufferedImage img = ImageIO.read(toProcess);

            // Greyscaleify image
            int height = img.getHeight();
            int width = img.getWidth();

            for (int y=0; y<height; y++) {
                for (int x=0; x<width; x++) {
                    int col = img.getRGB(x, y);

                    // Mask and right-shift
                    int b = col & 0xff;
                    int g = (col & 0xff00) >> 8;
                    int r = (col & 0xff0000) >> 16;

                    // Calculate which grey to use
                    int grey = (int)(0.299 * r + 0.587 * g + 0.114 * b);

                    // Left shift and OR together
                    int newCol = (grey << 16) | (grey <<8) | grey;

                    img.setRGB(x, y, newCol);
                }
            }

            // Get skew angle
            double skew = new ImageDeskew(img).getSkewAngle();

            // Rotate by skew angle
            img = ImageHelper.rotateImage(img, -skew);

            ImageIO.write(img, "png", toProcess);
        } catch (IOException e) {
            LOG.error(e.toString());
        }

        return toProcess;
    }

    /**
     * Utilises Tesseract OCR to read text from an image, and output it.
     * @param readFile The {@link File} from which text will be read.
     */
    private void readText(File readFile) {
        try {
            String outString = TESS.doOCR(readFile);
            readFile.delete();
            // If nothing found in selection
            if (outString == null || outString.trim().isEmpty()) {
                trayIcon.displayMessage("No text found", "Unfortunately, no text could be found in your selection",
                        TrayIcon.MessageType.ERROR);
                LOG.info("No text found in selection");
            } else {
                sendToClipboard(outString);
                trayIcon.displayMessage("Text copied to clipboard!", outString, TrayIcon.MessageType.INFO);
                LOG.info("Read text from screen capture successfully.");
            }
        } catch (TesseractException e) {
            LOG.error(e.toString());
        }
    }

    /**
     * Saves text to the system's clipboard
     * @param text The text to copy
     */
    private void sendToClipboard(String text) {
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(text), null);
    }

    /**
     * Iterates through available {@link GraphicsDevice}s, and adds their dimensions to the screenRects ArrayList. Also
     * creates an overlay for each screen (or updates the corrosponding one if it already exists).
     */
    private void calculateScreenBounds() {
        // Update on the off-chance the user has updated their graphics devices during running
        GraphicsDevice[] devices = GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices();

        for (int i=0; i<devices.length; i++) {
            Rectangle bounds = devices[i].getDefaultConfiguration().getBounds();

            // Update overlays so that current screen is covered
            if (overlays.size() > i) {
                overlays.get(i).updateCoveredMonitor(bounds);
            } else {
                overlays.add(new MonitorOverlay(bounds, this));
            }
        }
    }

    /**
     * Register all hotkeys used with JIntellitype
     */
    private void registerHotkeys() {
        // Create all required JIntelliType hotkeys with unique identifiers
        JIntellitype.getInstance().registerHotKey(1001, JIntellitype.MOD_ALT, 'A');
        LOG.info("Hotkeys Successfully Registered.");
    }

    /**
     * Initialise JIntellitype, to attach its commands and hotkeys to this app.
     */
    protected void initJIntellitype() {
        try {
            JIntellitype.getInstance().addHotKeyListener(this);
            JIntellitype.getInstance().addIntellitypeListener(this);
            LOG.info("JIntellitype initialized");
            registerHotkeys();
        } catch (RuntimeException ex) {
            LOG.fatal("Either you are not on Windows, or there is a problem with the JIntellitype library.");
        }
    }

    /**
     * Initialises {@link SystemTray}-related instance variables, and adds the {@link TrayIcon} to the system tray.
     */
    protected void initSystemTray() {
        if (SystemTray.isSupported()) {
            // Get device's system tray
            SystemTray systemTray = SystemTray.getSystemTray();

            // Use small (less cluttered) version of system icon to avoid scaling issues
            Image appIcon = Toolkit.getDefaultToolkit().getImage(RESOURCE_DIR + "/icon_small.png");

            // Manually scale tray icon, to avoid rough scaling from using trayIcon.setImageAutoSize(true)
            Dimension trayIconSize = systemTray.getTrayIconSize();
            trayIcon = new TrayIcon(appIcon.getScaledInstance(trayIconSize.width, trayIconSize.height, Image.SCALE_SMOOTH), "ScreenClipper");

            trayIcon.setPopupMenu(new ClipperPopup());
            try {
                systemTray.add(trayIcon);
            } catch (AWTException e) {
                LOG.error("Error adding tray icon to system tray: " + e.toString());
            }
        }
    }
}
