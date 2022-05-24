package com.jacksanders.screenclipper;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

/**
 * {@link JFrame} extension that handles downloading of .traineddata files
 */
class LanguageDownloader extends JFrame {
    /** {@link Logger} object used to generate .log files */
    private static final Logger LOG = LogManager.getLogger(LanguageDownloader.class);


    /**
     * @param langs The languages that are already installed.
     */
    protected LanguageDownloader(ArrayList<String> langs, ClipperPopup popup) {
        Container contentPane = getContentPane();

        // Set layout manager
        contentPane.setLayout(new BoxLayout(contentPane, BoxLayout.PAGE_AXIS));

        // Frame config
        setTitle("Language downloader for ScreenClipper");
        setIconImage(Toolkit.getDefaultToolkit().getImage(ScreenClipper.RESOURCE_DIR + "/icon.png"));
        setBackground(new Color(243, 243, 243));

        setSize(300, 450);
        setMaximumSize(new Dimension(300, 450));

        // Initialise language selection panel
        JPanel langSelectPanel = createCheckBoxes(langs);

        // Pack language selection panel into scroll pane, as list is long and scrolling will be needed
        JScrollPane scroll = new JScrollPane(langSelectPanel);
        scroll.setSize(250, 400);
        scroll.setMaximumSize(new Dimension(250, 400));

        JButton dlButton = new JButton("Install Selected Languages");
        dlButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        dlButton.addActionListener(e -> {
            ArrayList<String> toDL = new ArrayList<>();
            for (Component c : langSelectPanel.getComponents()) {
                JCheckBox check = (JCheckBox) c;
                if (check.isSelected()) {
                    // Find key with value matching name of given checkbox, and add to list of languages to download
                    toDL.add(ScreenClipper.LANG_MAP.keySet().stream()
                                                            .filter(s -> ScreenClipper.LANG_MAP.get(s).equals(check.getText()))
                                                            .findFirst().orElse(null));
                }
            }

            getDownloadThread(toDL).start();

            setVisible(false);
        });

        // Add all components + vertical spacing
        add(Box.createVerticalStrut(10));
        add(scroll);
        add(Box.createVerticalStrut(10));
        add(dlButton);
        add(Box.createVerticalStrut(10));

        setVisible(false);
    }

    /**
     * Method to intialise main {@link JPanel} and fill it with checkboxes, to prevent constructor becoming overcrowded
     * @param langs Languages that have already been downloaded
     * @return A Jpanel with a checkbox for each undownloaded language
     */
    private JPanel createCheckBoxes(ArrayList<String> langs) {
        JPanel panel = new JPanel();
        panel.setBackground(Color.WHITE);
        panel.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.setLayout(new BoxLayout(panel, BoxLayout.PAGE_AXIS));

        // Get values (language names) from LANG_MAP as Collection, convert to stream, sort stream, convert back to array
        for (String l : ScreenClipper.LANG_MAP.values().stream().sorted().toArray(String[]::new)) {
            if (!langs.contains(l)) {
                // Fill selection panel with one checkbox per language
                JCheckBox check = new JCheckBox(l);
                check.setBackground(Color.WHITE);
                panel.add(check);
            }
        }

        return panel;
    }

    /*
     * .traineddata files downloaded by threads created by this method are distributed under the Apache-2.0 License.
     * For more information, see Licenses/LICENSE_tesseract_ocr.txt
     */

    /**
     * Takes a list of languages, and starts the download of the first one. When that is done, start the download of the
     * second one.
     * @param langs The languages to download
     * @return The {@link Thread} created containing the download {@link Runnable}
     */
    private Thread getDownloadThread(ArrayList<String> langs) {
        Runnable download = new Runnable() {
            @Override
            public void run() {
                // Remove and return first string (language code)
                String s = langs.remove(0);

                // Get tesseract download link of traineddata files
                URL downLink;
                try {
                    downLink = new URL("https://github.com/tesseract-ocr/tessdata_best/raw/main/" + s + ".traineddata");
                } catch (MalformedURLException malformedURLException) {
                    LOG.error(malformedURLException.toString());
                    return;
                }
                try {
                    // Get fileSize as long
                    int fileSize = downLink.openConnection().getContentLength();

                    // Set up buffers & file stream to write to
                    BufferedInputStream inBuffer = new BufferedInputStream(downLink.openStream());
                    FileOutputStream outFile = new FileOutputStream(ScreenClipper.RESOURCE_DIR + "/tessdata/" + s + ".traineddata");
                    BufferedOutputStream outBuffer = new BufferedOutputStream(outFile, 1024);

                    // Set up disk buffer
                    byte[] buffer = new byte[1024];
                    int downloaded = 0;
                    int bytesRead;

                    // inBuffer.read -> reads the next 1024 bytes (starting at offset 0) to buffer byte array
                    while((bytesRead = inBuffer.read(buffer, 0, 1024)) >= 0) {
                        downloaded += bytesRead;
                        outBuffer.write(buffer, 0, bytesRead);

                        System.out.print((int) (((double) downloaded / (double) fileSize) * 100) +"%\r");
                    }

                    // Close buffers
                    outBuffer.close();
                    inBuffer.close();

                    // Start downloading next language
                    if (langs.size() > 0) {
                        getDownloadThread(langs).start();
                    }

                } catch (IOException x) {
                    LOG.error("Error downloading traineddata files: " + x.toString());
                }
            }
        };

        return new Thread(download);
    }

}
