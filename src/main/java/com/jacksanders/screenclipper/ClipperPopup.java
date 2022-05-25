package com.jacksanders.screenclipper;

import java.awt.*;
import java.io.File;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

        String[] f = new File(ScreenClipper.RESOURCE_DIR + "/tessdata").list();

        if (f == null) {
            ScreenClipper.LOG.error("Language data files not found. Please ensure they haven't been deleted");
            return;
        }

        // Filter to get only language files that aren't osd, and sort these by their language names.
        List<String> langs = Arrays.stream(f).filter(s -> s.endsWith(".traineddata") && !s.startsWith("osd"))
                                                .map(s -> s.replace(".traineddata", ""))
                                                .sorted(new Comparator<String>() {
                                                    @Override
                                                    public int compare(String o1, String o2) {
                                                        Map<String, String> m = ScreenClipper.LANG_MAP;
                                                        return m.get(o1).compareToIgnoreCase(m.get(o2));
                                                    }
                                                })
                                                .collect(Collectors.toList());

        for (String lang : langs) {
            CheckboxMenuItem tempItem = new CheckboxMenuItem(ScreenClipper.LANG_MAP.get(lang));
            tempItem.addItemListener(e -> {
                ScreenClipper.TESS.setLanguage(lang);

                // Ensure that all checkboxes other than current one are disabled.
                for (int i=0; i<langs.size(); i++) {
                    CheckboxMenuItem check = (CheckboxMenuItem) languageSub.getItem(i);
                    check.setState(false);
                }

                tempItem.setState(true);
            });
            languageSub.add(tempItem);
        }

        LanguageManager ld = new LanguageManager(langs, this);
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
