# ScreenClipper - A Java App for Windows
![ScreenClipper Icon](https://user-images.githubusercontent.com/43889196/170592683-87aeac61-e90e-47c9-bec6-8e949ba89a83.png)

Ever watched a nice person on youtube explaining the basics of ```public static void main(String[] args)``` and wanted to just copy-paste all that clean, working code from their video?  Well with ScreenClipper, now you can! This app utilises [Tesseract Open-Source OCR Engine](https://github.com/tesseract-ocr/tesseract), and the [Tess4j](https://github.com/nguyenq/tess4j) JNA Wrapper to enable you to simply copy-paste text directly from any image, or video. 

## Quick Links
- [Installation](https://github.com/JSanders02/ScreenClipper/edit/master/README.md#installation)
- [Usage Instructions](https://github.com/JSanders02/ScreenClipper/edit/master/README.md#usage-instructions)
- [Dependencies](https://github.com/JSanders02/ScreenClipper/edit/master/README.md#dependencies)

## Installation
### Version <1.0.0
Download the .zip [here](https://github.com/JSanders02/ScreenClipper/releases/download/alpha-release/screenclipper__v_1_0_0.zip), and extract to a location of your choosing. Run ScreenClipper.exe to start the program

## Usage Instructions
To use the program, simply run the .exe, and it will start in your system tray (the bottom-right of your screen, next to the volume indicator). Sometimes you may have to click on the little arrow to show the app's icon.
If you want to copy some text:
1) Press ALT+A, or right-click the app's icon and select "Take Clipping". Your screen will darken, indicating that you have entered clipping mode.
2) Drag your mouse to draw a box around the area you want to copy text from (for best results, ensure the area does not contain anything other than desired text, as any artifacts in the selection area could erroneously be read as characters).
3) You will get a notification (if enabled) informing you of whether or not any text was selected, and what that text was.

![GIF instructions on ScreenClipper's usage](https://user-images.githubusercontent.com/43889196/170590234-5034b1a2-6cd3-4e7b-8606-687d5c33289a.gif)

If you want to change the language you are selecting (e.g. English -> Chinese):
1) Right-click on the app's icon and hover your cursor over "Select Language". This will open a submenu of all installed languages.
2) Simply left-click on the language you want to detect.

![GIF instructions on changing language](https://user-images.githubusercontent.com/43889196/170592429-551334f2-57ff-4322-8ffd-e10e3334e090.gif)

If you want to install a new language, or remove language data for one you don't need:
1) Right-click on the app's icon and select "Install/Remove Languages".
2) To install new languages, tick the boxes next to the languages you want to install, and click on "Install Selected Languages"
3) To remove old languages, tick the boxes next to the languages you want to remove, and click on "Remove Selected Languages"

![GIF instructions on adding/removing languages](https://user-images.githubusercontent.com/43889196/170591089-e6bd7913-9ac4-4e8a-a173-77aa2446106a.gif)

## Dependencies
This app directly uses the following third-party libraries. Their licenses can be found in Legal/LICENSE_repository_name.txt
- [JIntellitype](https://github.com/melloware/jintellitype)
- [Tesseract Open-Source OCR Engine](https://github.com/tesseract-ocr/tesseract)
- [Tess4j](https://github.com/nguyenq/tess4j)
- [Log4j2](https://github.com/apache/logging-log4j2)
- [FlatLaf](https://github.com/JFormDesigner/FlatLaf)
