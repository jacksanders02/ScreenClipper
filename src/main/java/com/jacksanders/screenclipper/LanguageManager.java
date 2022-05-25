package com.jacksanders.screenclipper;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import javax.swing.text.BoxView;
import java.awt.*;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * {@link JFrame} extension that handles downloading of .traineddata files
 */
class LanguageManager extends JFrame {
    /** {@link Logger} object used to generate .log files */
    private static final Logger LOG = LogManager.getLogger(LanguageManager.class);

    /** Handles total download progress (i.e. downloading 1 of 3 files would be 33% */
    private JProgressBar overallDownloadProgress;

    /** Handles progress of current file being downloaded */
    private JProgressBar currentFileDownloadProgress;

    /** The {@link PopupMenu} of the application */
    private ClipperPopup pop;


    /**
     * @param langs The languages that are already installed.
     */
    protected LanguageManager(List<String> langs, ClipperPopup popup) {
        pop = popup;

        Container contentPane = getContentPane();

        // Set layout manager and constraints
        contentPane.setLayout(new BoxLayout(contentPane, BoxLayout.LINE_AXIS));

        // Frame config
        setTitle("Language Manager for ScreenClipper");
        setIconImage(Toolkit.getDefaultToolkit().getImage(ScreenClipper.RESOURCE_DIR + "/icon.png"));
        setBackground(new Color(243, 243, 243));

        setSize(600, 450);
        setMaximumSize(new Dimension(600, 450));

        // Initialise language selection panels
        JPanel langDownloadSelectPanel = createCheckBoxes(langs, false);
        JPanel langDeleteSelectPanel = createCheckBoxes(langs, true);

        // Pack language selection panel into scroll pane, as list is long and scrolling will be needed
        JScrollPane addScroll = new JScrollPane(langDownloadSelectPanel);
        addScroll.setSize(250, 400);
        addScroll.setMaximumSize(new Dimension(250, 400));

        // Create scroll pane for deleting languages
        JScrollPane deleteScroll = new JScrollPane(langDeleteSelectPanel);
        deleteScroll.setSize(250, 400);
        deleteScroll.setMaximumSize(new Dimension(250, 400));

        // Setup download button and add action listener
        JButton dlButton = new JButton("Install Selected Languages");
        dlButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        dlButton.addActionListener(e -> {
            ArrayList<String> toDL = new ArrayList<>();
            for (Component c : langDownloadSelectPanel.getComponents()) {
                JCheckBox check = (JCheckBox) c;
                if (check.isSelected()) {
                    // Add checkbox's name (language key) to download list
                    toDL.add(check.getName());
                }
            }

            if (toDL.size() == 0) {
                return; // No point trying to download 0 languages
            }

            contentPane.removeAll();
            contentPane.repaint();

            // Create generic progress bars
            overallDownloadProgress = genProgressBar(0, toDL.size() * 100);
            currentFileDownloadProgress = genProgressBar(0, 100);

            // Change axis of layoutm, to stack progress bars atop one another
            contentPane.setLayout(new BoxLayout(contentPane, BoxLayout.PAGE_AXIS));

            // Set size to only fit download bars + padding
            setSize(270, 130);
            setResizable(false);

            add(Box.createVerticalStrut(10));
            add(overallDownloadProgress);
            add(Box.createVerticalStrut(10));
            add(currentFileDownloadProgress);
            add(Box.createVerticalStrut(10));

            getDownloadThread(toDL).start();
        });

        JButton deleteButton = new JButton("Remove Selected Languages");
        deleteButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        deleteButton.addActionListener(e -> {
            for (Component c : langDeleteSelectPanel.getComponents()) {
                JCheckBox check = (JCheckBox) c;
                if (check.isSelected()) {
                    deleteLang(c.getName());
                }
            }
            setVisible(false);
            pop.reset();
        });

        // Create dummy panels to hold each side
        // Add all components + vertical spacing
        JPanel left = new JPanel();
        left.setLayout(new BoxLayout(left, BoxLayout.PAGE_AXIS));

        left.add(Box.createVerticalStrut(10));
        left.add(titleBar("Add Language(s)"));
        left.add(Box.createVerticalStrut(10));
        left.add(addScroll);
        left.add(Box.createVerticalStrut(10));
        left.add(dlButton);
        left.add(Box.createVerticalStrut(10));

        JPanel right = new JPanel();
        right.setLayout(new BoxLayout(right, BoxLayout.PAGE_AXIS));

        right.add(Box.createVerticalStrut(10));
        right.add(titleBar("Delete Language(s)"));
        right.add(Box.createVerticalStrut(10));
        right.add(deleteScroll);
        right.add(Box.createVerticalStrut(10));
        right.add(deleteButton);
        right.add(Box.createVerticalStrut(10));

        add(left);
        add(Box.createHorizontalStrut(25));
        add(right);

        setVisible(false);
    }

    /**
     * @param text The text to put on the title bar
     * @return The title bar containing the text, in semibold font (FlatLaf)
     */
    private JLabel titleBar(String text) {
        JLabel bar = new JLabel(text);
        bar.putClientProperty("FlatLaf.style", "font: 200% $semibold.font");
        bar.setFont(new Font(bar.getFont().getName(), Font.BOLD, 18));
        bar.setAlignmentX(Component.CENTER_ALIGNMENT);
        return bar;
    }

    /**
     * Method to intialise main {@link JPanel} and fill it with checkboxes, to prevent constructor becoming overcrowded
     * @param langs Languages that have already been downloaded
     * @return A Jpanel with a checkbox for each undownloaded language
     */
    private JPanel createCheckBoxes(List<String> langs, boolean addIfIn) {
        JPanel panel = new JPanel();
        panel.setBackground(Color.WHITE);
        panel.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.setLayout(new BoxLayout(panel, BoxLayout.PAGE_AXIS));

        // Sort all language keys by their respective values
        String[] sortedLangs = ScreenClipper.LANG_MAP.keySet().stream()
                                                            .sorted(new Comparator<String>() {
                                                                @Override
                                                                public int compare(String o1, String o2) {
                                                                    Map<String, String> m = ScreenClipper.LANG_MAP;
                                                                    return m.get(o1).compareToIgnoreCase(m.get(o2));
                                                                }
                                                            }).toArray(String[]::new);
        for (String l : sortedLangs) {
            if ((langs.contains(l) && addIfIn) || (!langs.contains(l) && !addIfIn)) {
                // Fill selection panel with one checkbox per language
                JCheckBox check = new JCheckBox(ScreenClipper.LANG_MAP.get(l));
                check.setName(l); // Set name to key, to be grabbed in download action listener
                check.setBackground(Color.WHITE);
                panel.add(check);
            }
        }

        return panel;
    }

    /**
     * Creates a generic {@link JProgressBar}
     * @param min The minimum value of the progress bar
     * @param max The maximum value of the progress bar
     * @return The progress bar
     */
    private JProgressBar genProgressBar(int min, int max) {
        JProgressBar bar = new JProgressBar(min, max);
        bar.setAlignmentX(Component.CENTER_ALIGNMENT);
        bar.setPreferredSize(new Dimension(250, 50));
        bar.setStringPainted(true);
        return bar;
    }

    /*
     * .traineddata files downloaded by threads created by this method are distributed by tesseract-ocr under the
     * Apache-2.0 License.
     * For more information, see Licenses/LICENSE_tesseract_ocr.txt
     */

    /**
     * Takes a list of languages, and creates a thread that when run, will download all languages in that list in order.
     * @param langs The languages to download
     * @return The {@link Thread} created containing the download {@link Runnable}
     */
    private Thread getDownloadThread(List<String> langs) {
        Runnable download = new Runnable() {
            @Override
            public void run() {
                // Remove and return first string (language code)
                String s = langs.remove(0);

                // Scrape data number of files downloaded from download progress
                int startProgress = overallDownloadProgress.getValue();

                // Add one to negate zero-indexing
                int fileNum = startProgress / 100 + 1;
                int maxNum = overallDownloadProgress.getMaximum() / 100;

                overallDownloadProgress.setString("Downloading " + ScreenClipper.LANG_MAP.get(s) +
                        "... (" + fileNum + "/" + maxNum + ")");
                currentFileDownloadProgress.setString("0%");
                currentFileDownloadProgress.setValue(0);

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

                        int downloadPercent = (int) (((double) downloaded / (double) fileSize) * 100);

                        // Update download progress
                        overallDownloadProgress.setValue(startProgress + downloadPercent);

                        currentFileDownloadProgress.setString(downloadPercent + "%");
                        currentFileDownloadProgress.setValue(downloadPercent);
                    }

                    // Close buffers
                    outBuffer.close();
                    inBuffer.close();

                    LOG.info("Successfully downloaded file " + s + ".traineddata");

                    // Start downloading next language
                    if (langs.size() > 0) {
                        getDownloadThread(langs).start();
                    } else {
                        setVisible(false);
                        pop.reset();
                    }

                } catch (IOException x) {
                    LOG.error("Error downloading traineddata files: " + x.toString());
                }
            }
        };

        return new Thread(download);
    }

    /**
     * Deletes a given language from tessdata directory
     * @param lang The language code to delete
     */
    private void deleteLang(String lang) {
        File langFile = new File(ScreenClipper.RESOURCE_DIR + "/tessdata/" + lang + ".traineddata");

        if (langFile.delete()) {
            LOG.info("Deleted file " + langFile);
        } else {
            LOG.error("Failed to delete file " + langFile);
        }
    }
}
