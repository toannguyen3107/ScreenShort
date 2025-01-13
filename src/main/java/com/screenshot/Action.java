package com.screenshot;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.Container;
import java.awt.Graphics;
import java.awt.Toolkit;
import java.awt.datatransfer.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JRootPane;

import burp.api.montoya.MontoyaApi;

public class Action {
    private final MontoyaApi api;
    private List<BufferedImage> images = new ArrayList<>();
    Action(MontoyaApi api) {
        this.api = api;
    }

    public void clearImages(){
        images.clear();
    }
    public void takeScreenshotAndGetBufferImage(Component component) {
        try {
            int w = component.getWidth();
            int h = component.getHeight();
    
            BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = image.createGraphics();
            component.paint(g);
            images.add(image);
        } catch (Exception e) {
            api.logging().logToOutput(e.toString());
        }
        return ;
    }
    public void takeScreenshot(Component component) {
        try {
            images.clear();
            String componentName = component.getName();
            api.logging().logToOutput("Component name is n: " + componentName);
            int w = component.getWidth();
            int h = component.getHeight();
    
            BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = image.createGraphics();
            component.paint(g);
    
    
            // Copy image to clipboard
            ImageSelection imageSelection = new ImageSelection(image);
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            clipboard.setContents(imageSelection, null);
    
            api.logging().logToOutput("Screenshot of \"" + componentName + "\" saved and copied to clipboard.");
        } catch (Exception e) {
            api.logging().logToOutput(e.toString());
        }
    }
    void takeScreenshot2(){
        try {
            BufferedImage bf1 = images.get(0);
            BufferedImage bf2 = images.get(1);
            BufferedImage combined = combineImagesHorizontally(bf1, bf2);
            ImageSelection imageSelection = new ImageSelection(combined);
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            clipboard.setContents(imageSelection, null);
            images.clear();
        } catch (Exception e) {
            api.logging().logToOutput(e.toString());
        }
    }
    public BufferedImage combineImagesHorizontally(BufferedImage img1, BufferedImage img2) {
        int borderThickness = 5; // Width of the border in pixels
        Color borderColor = Color.BLACK; // Border color
    
        // Calculate dimensions of the combined image
        int width = img1.getWidth() + img2.getWidth() + borderThickness;
        int height = Math.max(img1.getHeight(), img2.getHeight());
        
        // Create a new image with a white background
        BufferedImage combined = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics g = combined.getGraphics();
    
        // Fill the background with white
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, width, height);
    
        // Draw the first image
        g.drawImage(img1, 0, 0, null);
    
        // Draw the border
        g.setColor(borderColor);
        g.fillRect(img1.getWidth(), 0, borderThickness, height);
    
        // Draw the second image
        g.drawImage(img2, img1.getWidth() + borderThickness, 0, null);
    
        // Release resources
        g.dispose();
        return combined;
    }
    
    /**
     * Recursively searches for a JScrollPane within the given container.
     *
     * @param container The container to search within.
     * @return The first JScrollPane found, or null if none is found.
     */
    public static JRootPane findJScrollPane(Container container) {
        for (Component component : container.getComponents()) {
            if (component instanceof JRootPane) {
                return (JRootPane) component;
            } else if (component instanceof Container) {
                JRootPane found = findJScrollPane((Container) component);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

}