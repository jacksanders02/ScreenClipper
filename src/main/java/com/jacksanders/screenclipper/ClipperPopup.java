package com.jacksanders.screenclipper;

import java.awt.*;
import java.io.File;
import java.util.ArrayList;

/**
 * Class to handle the {@link PopupMenu} that appears when the tray icon is right-clicked.
 */
class ClipperPopup extends PopupMenu {

    /** Language selection submenu */
    Menu languageSub;


    /**
     * Construct a basic popup menu
     */
    protected ClipperPopup() {
        // Language Config
        updateLanguages();

        // Exit Button
        MenuItem exit = new MenuItem("Close ScreenClipper");
        exit.addActionListener(e -> {
            System.exit(0);
        });

        add(languageSub);
        add(exit);
    }

    /**
     * Get details of currently installed languages, and add to set language submenu.
     */
    protected void updateLanguages() {
        languageSub = new Menu("Select Language");

        String[] files = new File(ScreenClipper.RESOURCE_DIR + "/tessdata").list();
        ArrayList<String> langs = new ArrayList<>();
        for (String file : files != null ? files : new String[0]) {
            if (file.endsWith(".traineddata")) {
                String lang = file.replace(".traineddata", "");
                if (!lang.equals("osd")) { // Ignore osd - this is a special datapack used by tesseract for image orientation
                    CheckboxMenuItem tempItem = new CheckboxMenuItem(ScreenClipper.LANG_MAP.get(lang));
                    tempItem.addItemListener(e -> {
                        ScreenClipper.TESS.setLanguage(lang);

                        // Ensure that all checkboxes other than current one are disabled.
                        // -1 to remove osd.traineddata.
                        for (int i=0; i<files.length-1; i++) {
                            CheckboxMenuItem check = (CheckboxMenuItem) languageSub.getItem(i);
                            check.setState(false);
                        }

                        tempItem.setState(true);
                    });
                    // Add language name to langs
                    langs.add(ScreenClipper.LANG_MAP.get(lang));
                    languageSub.add(tempItem);
                }
            }
        }

        LanguageDownloader ld = new LanguageDownloader(langs, this);
        MenuItem addMoreMenuItem = new MenuItem("Add More...");
        addMoreMenuItem.addActionListener(e -> {
            ld.setVisible(true);
        });
        languageSub.add(addMoreMenuItem);
    }

    /**
     * Reset the popup menu, and update languages
     */
    protected void reset() {
        removeAll();

        // Language Config
        updateLanguages();

        // Exit Button
        MenuItem exit = new MenuItem("Close ScreenClipper");
        exit.addActionListener(e -> {
            System.exit(0);
        });

        add(languageSub);
        add(exit);
    }
}
