package com.screenshort.utils;

import burp.api.montoya.MontoyaApi;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import javax.swing.SwingUtilities;

/**
 * Utilities for capturing screenshots of Burp Suite components.
 */
public class ScreenshotUtils {

    private final MontoyaApi api;
    private final List<BufferedImage> imageBuffer = new ArrayList<>();

    public ScreenshotUtils(MontoyaApi api) {
        this.api = api;
    }

    /**
     * Captures a normal screenshot of the component under the mouse cursor.
     * Opens the annotation editor with the captured image.
     */
    public void handleNormalScreenshot() {
        clearImageBuffer();
        try {
            Frame suiteFrame = api.userInterface().swingUtils().suiteFrame();
            if (suiteFrame == null) {
                api.logging().logToError("Could not get Burp Suite main frame.");
                return;
            }

            Component targetComponent = ComponentFinder.findComponentUnderMouse(Constants.SPLIT_VIEWER_PANE, suiteFrame);
            if (targetComponent == null) {
                api.logging().logToOutput("Could not find '" + Constants.SPLIT_VIEWER_PANE + "' under mouse. Trying focused component.");
                targetComponent = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
                if (targetComponent == null || !SwingUtilities.isDescendingFrom(targetComponent, suiteFrame)) {
                    api.logging().logToError("No suitable component found under mouse or focused.");
                    return;
                }
                api.logging().logToOutput("Using focused component: " + targetComponent.getClass().getName()
                        + (targetComponent.getName() != null ? " Name: " + targetComponent.getName() : ""));
            }

            BufferedImage image = captureComponent(targetComponent);
            if (image != null) {
                launchAnnotationEditor(image);
            }
        } catch (Exception ex) {
            api.logging().logToError("Error in handleNormalScreenshot: " + ex.toString(), ex);
        }
    }

    /**
     * Captures a full screenshot combining request and response panes.
     * Opens the annotation editor with the combined image.
     */
    public void handleFullScreenshot() {
        clearImageBuffer();
        try {
            Frame suiteFrame = api.userInterface().swingUtils().suiteFrame();
            if (suiteFrame == null) {
                api.logging().logToError("Full Screenshot: Could not get Burp Suite main frame.");
                return;
            }

            Component targetComponent = ComponentFinder.findComponentUnderMouse(Constants.SPLIT_VIEWER_PANE, suiteFrame);
            if (targetComponent == null) {
                api.logging().logToError("Full Screenshot: Could not find '" + Constants.SPLIT_VIEWER_PANE + "' component in the frame.");
                return;
            }

            Component reqComp = ComponentFinder.findByNameRecursively(targetComponent, Constants.REQUESTS_PANE);
            Component resComp = ComponentFinder.findByNameRecursively(targetComponent, Constants.RESPONSE_PANE);
            if (reqComp == null || resComp == null) {
                api.logging().logToError("Full Screenshot: Could not find Request or Response panes within the target split pane.");
                return;
            }

            Component syntaxTextAreaReq = ComponentFinder.findByNameRecursively(reqComp, Constants.SYNTAX_TEXT_AREA);
            Component syntaxTextAreaRes = ComponentFinder.findByNameRecursively(resComp, Constants.SYNTAX_TEXT_AREA);
            if (syntaxTextAreaReq == null || syntaxTextAreaRes == null) {
                api.logging().logToError(String.format(
                        "Full Screenshot: Could not find syntaxTextArea in Req (%s found) or Res (%s found). Cannot take screenshot.",
                        syntaxTextAreaReq != null, syntaxTextAreaRes != null));
                return;
            }

            captureComponentToBuffer(syntaxTextAreaReq);
            captureComponentToBuffer(syntaxTextAreaRes);

            BufferedImage combinedImage = combineImagesHorizontally();
            if (combinedImage != null) {
                launchAnnotationEditor(combinedImage);
            } else {
                api.logging().logToError("Full Screenshot: Screenshot failed - combined image was null.");
            }
        } catch (Exception ex) {
            api.logging().logToError("Error in handleFullScreenshot: " + ex.toString(), ex);
            clearImageBuffer();
        }
    }

    /**
     * Captures a component to a BufferedImage.
     *
     * @param component The component to capture
     * @return The captured image, or null if capture failed
     */
    private BufferedImage captureComponent(Component component) {
        if (component == null) {
            return null;
        }

        int w = component.getWidth();
        int h = component.getHeight();
        if (w <= 0 || h <= 0) {
            api.logging().logToError(String.format("Target component size is invalid (width: %d, height: %d). Cannot capture.", w, h));
            return null;
        }

        BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        try {
            if (!component.isOpaque() && component.getBackground() != null) {
                g.setColor(component.getBackground());
                g.fillRect(0, 0, w, h);
            }
            component.paint(g);
        } finally {
            g.dispose();
        }
        return image;
    }

    /**
     * Captures a component and adds it to the internal buffer.
     */
    private void captureComponentToBuffer(Component component) {
        BufferedImage image = captureComponent(component);
        if (image != null) {
            imageBuffer.add(image);
            api.logging().logToOutput(String.format(
                    "Captured component screenshot for buffer (size: %dx%d). Buffer size: %d",
                    image.getWidth(), image.getHeight(), imageBuffer.size()));
        }
    }

    /**
     * Clears the internal image buffer.
     */
    private void clearImageBuffer() {
        imageBuffer.clear();
    }

    /**
     * Combines images in the buffer horizontally with a border between them.
     *
     * @return The combined image, or null if buffer is empty or contains invalid images
     */
    private BufferedImage combineImagesHorizontally() {
        try {
            if (imageBuffer.isEmpty()) {
                return null;
            }
            if (imageBuffer.size() == 1) {
                BufferedImage singleImage = imageBuffer.get(0);
                clearImageBuffer();
                return singleImage;
            }

            BufferedImage bf1 = imageBuffer.get(0);
            BufferedImage bf2 = imageBuffer.get(1);
            if (bf1 == null || bf2 == null) {
                api.logging().logToError("One or both images in buffer are null. Cannot combine.");
                clearImageBuffer();
                return null;
            }

            int totalWidth = bf1.getWidth() + bf2.getWidth() + Constants.BORDER_THICKNESS;
            int maxHeight = Math.max(bf1.getHeight(), bf2.getHeight());
            if (totalWidth <= 0 || maxHeight <= 0) {
                api.logging().logToError(String.format(
                        "Combined image size is invalid (width: %d, height: %d). Cannot combine.",
                        totalWidth, maxHeight));
                clearImageBuffer();
                return null;
            }

            BufferedImage combined = new BufferedImage(totalWidth, maxHeight, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = combined.createGraphics();
            try {
                g.setColor(Constants.BACKGROUND_COLOR);
                g.fillRect(0, 0, totalWidth, maxHeight);

                g.drawImage(bf1, 0, 0, null);

                g.setColor(Constants.BORDER_COLOR);
                g.fillRect(bf1.getWidth(), 0, Constants.BORDER_THICKNESS, maxHeight);

                g.drawImage(bf2, bf1.getWidth() + Constants.BORDER_THICKNESS, 0, null);
            } finally {
                g.dispose();
            }

            api.logging().logToOutput(String.format("Images combined successfully (size: %dx%d).", totalWidth, maxHeight));
            clearImageBuffer();
            return combined;

        } catch (IndexOutOfBoundsException e) {
            api.logging().logToError("Index Out Of Bounds error when accessing images buffer: " + e.getMessage(), e);
            clearImageBuffer();
            return null;
        } catch (Exception e) {
            api.logging().logToError("Error in combineImagesHorizontally: " + e.toString(), e);
            clearImageBuffer();
            return null;
        }
    }

    /**
     * Launches the annotation editor with the given image.
     */
    private void launchAnnotationEditor(BufferedImage image) {
        new AnnotationEditor(api, image).launch();
    }
}
