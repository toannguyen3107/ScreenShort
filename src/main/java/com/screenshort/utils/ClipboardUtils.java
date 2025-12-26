package com.screenshort.utils;

import java.awt.Image;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.image.BufferedImage;
import java.io.IOException;

/**
 * Centralized clipboard utilities for copying text and images.
 */
public final class ClipboardUtils {

    private ClipboardUtils() {
        // Utility class - prevent instantiation
    }

    /**
     * Copies a string to the system clipboard.
     *
     * @param data The string to copy
     */
    public static void copyToClipboard(String data) {
        if (data == null) {
            return;
        }
        StringSelection stringSelection = new StringSelection(data);
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        clipboard.setContents(stringSelection, null);
    }

    /**
     * Copies an image to the system clipboard.
     *
     * @param image The image to copy
     * @return true if successful, false otherwise
     */
    public static boolean copyImageToClipboard(BufferedImage image) {
        if (image == null) {
            return false;
        }
        try {
            ImageSelection imageSelection = new ImageSelection(image);
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            clipboard.setContents(imageSelection, null);
            return true;
        } catch (IllegalStateException e) {
            // Clipboard not available (perhaps headless environment)
            return false;
        }
    }

    /**
     * Transferable wrapper for images to enable clipboard operations.
     */
    private static class ImageSelection implements Transferable {
        private final Image image;

        public ImageSelection(Image image) {
            this.image = image;
        }

        @Override
        public DataFlavor[] getTransferDataFlavors() {
            return new DataFlavor[]{DataFlavor.imageFlavor};
        }

        @Override
        public boolean isDataFlavorSupported(DataFlavor flavor) {
            return DataFlavor.imageFlavor.equals(flavor);
        }

        @Override
        public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException {
            if (!isDataFlavorSupported(flavor)) {
                throw new UnsupportedFlavorException(flavor);
            }
            return image;
        }
    }
}
