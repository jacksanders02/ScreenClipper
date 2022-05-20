import com.melloware.jintellitype.HotkeyListener;
import com.melloware.jintellitype.IntellitypeListener;
import com.melloware.jintellitype.JIntellitype;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

public class ScreenClipper implements IntellitypeListener, HotkeyListener {
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


    private ArrayList<Rectangle> screenRects = new ArrayList<>();
    private ArrayList<MonitorOverlay> overlays = new ArrayList<>();

    private ScreencapController screencapController;

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

        // Initialise JIntellitype
        app.initJIntellitype();
    }

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
    public void onIntellitype(int i) {

    }

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
            Rectangle screen = screenRects.get(monitorID);
            r.translate(screen.x, screen.y);
            saveImage(robot.createScreenCapture(r));
        }
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
