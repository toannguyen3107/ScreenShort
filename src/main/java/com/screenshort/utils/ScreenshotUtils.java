package com.screenshort.utils;

import burp.api.montoya.MontoyaApi;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.*;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter; // Import FileNameExtensionFilter


public class ScreenshotUtils {

    private final MontoyaApi api;
    private final List<BufferedImage> images = new ArrayList<>();

    // Constructor
    public ScreenshotUtils(MontoyaApi api) {
        this.api = api;
    }

    public static Component findComponentUnderMouse(String name, Component parent) {
         try {
             Point location = MouseInfo.getPointerInfo().getLocation();
             if (parent == null) return null; // Handle null parent
             SwingUtilities.convertPointFromScreen(location, parent);
             Component deepest = SwingUtilities.getDeepestComponentAt(parent, location.x, location.y);
             if (deepest != null) {
                  // Search upwards from the deepest component for the named ancestor
                 Component current = deepest;
                 while (current != null) {
                     if (name != null && name.equals(current.getName())) {
                         return current;
                     }
                     if (current == parent) break; // Stop if we reach the initial parent
                     current = current.getParent();
                 }
             }
             return null;
        } catch (Exception e) {
            // Consider logging this via api.logging() if possible, otherwise System.err
            System.err.println("Error in findComponentUnderMouse: " + e.getMessage());
            // If api is accessible statically or via instance:
            // api.logging().logToError("Error in findComponentUnderMouse: " + e.getMessage());
            return null;
        }
    }

    public static Component getComponentByName(Component component, String name) {
        if (component == null) return null;
        if (name != null && name.equals(component.getName())) {
            return component;
        }
        if (component instanceof Container) {
            for (Component child : ((Container) component).getComponents()) {
                Component result = getComponentByName(child, name);
                if (result != null) {
                    return result;
                }
            }
        }
        return null;
    }

     public static List<Component> findAllComponentsByName(Component parent, String name) {
         List<Component> matchingComponents = new ArrayList<>();
         if (parent == null) return matchingComponents;
         if (name != null && name.equals(parent.getName())) {
            matchingComponents.add(parent);
         }
        if (parent instanceof Container) {
            for (Component child : ((Container) parent).getComponents()) {
                matchingComponents.addAll(findAllComponentsByName(child, name));
            }
        }
        return matchingComponents;
    }

    public static JRootPane findJRootPane(Container container) {
         if (container == null) return null;
        for (Component component : container.getComponents()) {
            if (component instanceof JRootPane) {
                return (JRootPane) component;
            } else if (component instanceof Container) {
                JRootPane found = findJRootPane((Container) component);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }


    // --- Image Handling Methods ---
    // ... (clearImages, captureComponentToBuffer, combineBufferedImagesHorizontally, ImageSelection - assumed here) ...
     private void clearImages(){
        api.logging().logToOutput("Clearing image buffer.");
        images.clear();
    }

    private void captureComponentToBuffer(Component component) {
         if (component == null) {
             api.logging().logToOutput("Cannot capture a null component (for buffer).");
             return;
         }
        try {
            api.logging().logToOutput("Capturing component screenshot for buffer...");
            int w = component.getWidth();
            int h = component.getHeight();

            if (w <= 0 || h <= 0) {
                api.logging().logToOutput(String.format("Component size is invalid (width: %d, height: %d). Cannot capture.", w, h));
                return;
            }
            // Use TYPE_INT_ARGB if transparency might be needed, otherwise RGB is fine
            BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = image.createGraphics();
            // Ensure background is painted if component is opaque=false but we want its content
            if (!component.isOpaque() && component.getBackground() != null) {
                g.setColor(component.getBackground());
                g.fillRect(0, 0, w, h);
            }
            component.paint(g); // Use paint for better component rendering than print
            g.dispose();
            images.add(image);
            api.logging().logToOutput(String.format("Captured component screenshot for buffer (size: %dx%d). Buffer size: %d", w, h, images.size()));
        } catch (Exception e) {
            api.logging().logToError("Error capturing component screenshot to buffer: " + e.toString(), e);
        }
    }

     private void copyImageToClipboard(BufferedImage image) {
         if (image == null) {
             api.logging().logToError("Cannot copy a null image to clipboard."); // Changed log level
             return;
         }
          try {
             ImageSelection imageSelection = new ImageSelection(image);
             Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
             clipboard.setContents(imageSelection, null);
             api.logging().logToOutput("Image copied to clipboard.");
          } catch (IllegalStateException e) {
              api.logging().logToError("Error accessing system clipboard (perhaps headless?): " + e.toString(), e);
              // Optionally show a message dialog if appropriate in the context
              // JOptionPane.showMessageDialog(null, "Could not access system clipboard.", "Clipboard Error", JOptionPane.WARNING_MESSAGE);
          } catch (Exception e) {
             api.logging().logToError("Error copying image to clipboard: " + e.toString(), e);
          }
     }

    private BufferedImage combineBufferedImagesHorizontally() {
        api.logging().logToOutput("Attempting to combine images from buffer...");
        try {
            if (images.isEmpty()) { // Check if empty first
                api.logging().logToOutput("Buffer is empty. Cannot combine.");
                return null;
            }
            if (images.size() == 1) { // If only one, return it directly
                 api.logging().logToOutput("Buffer contains only 1 image. Returning it directly.");
                 BufferedImage singleImage = images.get(0);
                 clearImages(); // Clear buffer even if returning single image
                 return singleImage;
            }
            // Proceed with combining 2 images (assuming buffer only holds max 2 for this logic)
            BufferedImage bf1 = images.get(0);
            BufferedImage bf2 = images.get(1);
            if (bf1 == null || bf2 == null) {
                api.logging().logToError("One or both images in buffer are null. Cannot combine."); // Changed level
                clearImages(); // Clear buffer on error
                return null;
            }
            int borderThickness = 5;
            Color borderColor = Color.BLACK;
            int totalWidth = bf1.getWidth() + bf2.getWidth() + borderThickness;
            int maxHeight = Math.max(bf1.getHeight(), bf2.getHeight());
             if (totalWidth <= 0 || maxHeight <= 0) {
                api.logging().logToError(String.format("Combined image size is invalid (width: %d, height: %d). Cannot combine.", totalWidth, maxHeight)); // Changed level
                clearImages(); // Clear buffer on error
                return null;
             }
            // Use TYPE_INT_RGB unless source images have alpha and it needs preserving
            BufferedImage combined = new BufferedImage(totalWidth, maxHeight, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = combined.createGraphics();
            // Fill background explicitly (optional, default is black for RGB)
            g.setColor(Color.WHITE); // Example background
            g.fillRect(0, 0, totalWidth, maxHeight);

            g.drawImage(bf1, 0, 0, null);
            g.setColor(borderColor);
            g.fillRect(bf1.getWidth(), 0, borderThickness, maxHeight);
            g.drawImage(bf2, bf1.getWidth() + borderThickness, 0, null);
            g.dispose();
            api.logging().logToOutput(String.format("Images combined successfully (size: %dx%d).", totalWidth, maxHeight));
            clearImages(); // Clear buffer after successful combination
            return combined;
        } catch (IndexOutOfBoundsException e) {
            api.logging().logToError("Index Out Of Bounds error when accessing images buffer: " + e.getMessage(), e);
            clearImages(); // Clear buffer on error
            return null;
        } catch (Exception e) {
            api.logging().logToError("Error in combineBufferedImagesHorizontally: " + e.toString(), e);
            clearImages(); // Clear buffer on error
            return null;
        }
        // 'finally { clearImages(); }' removed as clearImages() is called on success/error paths now.
    }

    // Inner class for clipboard transfer (keep as is)
    private static class ImageSelection implements Transferable {
        private final Image image;
        public ImageSelection(Image image) { this.image = image; }
        @Override public DataFlavor[] getTransferDataFlavors() { return new DataFlavor[] { DataFlavor.imageFlavor }; }
        @Override public boolean isDataFlavorSupported(DataFlavor flavor) { return DataFlavor.imageFlavor.equals(flavor); }
        @Override public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException {
            if (!isDataFlavorSupported(flavor)) throw new UnsupportedFlavorException(flavor);
            return image;
        }
    }

    // --- Public Methods Called by MenuActionHandler ---
    // ... (handleNormalScreenshot, handleFullScreenshot - assumed here and correct) ...
     public void handleNormalScreenshot() {
        api.logging().logToOutput("Executing Normal Screenshot (to Annotator)...");
        try {
            Frame suiteFrame = api.userInterface().swingUtils().suiteFrame();
            if (suiteFrame == null) {
                api.logging().logToError("Could not get Burp Suite main frame."); return; // Log as error
            }
            // Try finding the specific component first
            Component targetComponent = findComponentUnderMouse("rrvSplitViewerSplitPane", suiteFrame);

            // Fallback: If specific component not under mouse, try the focused component within the suite frame
            if (targetComponent == null) {
                 api.logging().logToOutput("Could not find 'rrvSplitViewerSplitPane' under mouse. Trying focused component.");
                 targetComponent = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
                 // Ensure the focused component is actually part of the Burp window
                 if (targetComponent == null || !SwingUtilities.isDescendingFrom(targetComponent, suiteFrame)) {
                      api.logging().logToError("No suitable component found under mouse or focused."); // Log as error
                      // Maybe inform the user?
                      // JOptionPane.showMessageDialog(suiteFrame, "Could not determine the component to capture.\nPlease ensure the target pane (like Request/Response) has focus or the mouse is over it.", "Capture Error", JOptionPane.WARNING_MESSAGE);
                      return;
                 }
                 api.logging().logToOutput("Using focused component: " + targetComponent.getClass().getName() + (targetComponent.getName() != null ? " Name: " + targetComponent.getName() : ""));
            } else {
                 api.logging().logToOutput("Using component under mouse: " + targetComponent.getName());
            }

            int w = targetComponent.getWidth();
            int h = targetComponent.getHeight();
             if (w <= 0 || h <= 0) {
                api.logging().logToError(String.format("Target component size is invalid (width: %d, height: %d). Cannot capture.", w, h)); // Log as error
                return;
            }
            BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = image.createGraphics();
             // Paint background if needed, then the component
            if (!targetComponent.isOpaque() && targetComponent.getBackground() != null) {
                g.setColor(targetComponent.getBackground());
                g.fillRect(0, 0, w, h);
            }
            targetComponent.paint(g); // Use paint
            g.dispose();
            api.logging().logToOutput("Normal screenshot captured. Launching annotator...");
            launchAnnotationEditor(image); // Launch the editor
        } catch (Exception ex) {
             api.logging().logToError("Error in handleNormalScreenshot: " + ex.toString(), ex);
        }
    }

    public void handleFullScreenshot() {
        api.logging().logToOutput("Executing Full Screenshot (Req/Res to Annotator)...");
        clearImages(); // Start fresh
        try {
            Frame suiteFrame = api.userInterface().swingUtils().suiteFrame();
             if (suiteFrame == null) {
                api.logging().logToError("Full Screenshot: Could not get Burp Suite main frame."); return;
            }
            // Find the split pane regardless of mouse position, assuming it's consistently named/structured
            // This might need adjustment based on Burp's actual UI structure if the name isn't reliable
            // A more robust approach might involve finding the main tabbed pane and navigating down.
            // For now, let's assume finding by name works often enough.
            Component targetSplitPane = findComponentByNameRecursively(suiteFrame, "rrvSplitViewerSplitPane"); // Helper needed

             if (targetSplitPane == null) {
                 api.logging().logToError("Full Screenshot: Could not find 'rrvSplitViewerSplitPane' component in the frame.");
                 // Attempt fallback if needed, e.g., checking active tab
                 return;
             }
             api.logging().logToOutput("Full Screenshot: Found target split pane.");

            // Find Request/Response panes within the located split pane
            Component reqComp = findComponentByNameRecursively(targetSplitPane, "rrvRequestsPane");
            Component resComp = findComponentByNameRecursively(targetSplitPane, "rrvResponsePane");

             if (reqComp == null || resComp == null) {
                 api.logging().logToError("Full Screenshot: Could not find Request or Response panes within the target split pane."); return;
             }
              api.logging().logToOutput("Full Screenshot: Found request and response panes.");

            // Find the text areas within request/response panes
            // Using findAllComponentsByName might be safer if there could be multiple or nested ones
            Component syntaxTextAreaReq = findComponentByNameRecursively(reqComp, "syntaxTextArea");
            Component syntaxTextAreaRes = findComponentByNameRecursively(resComp, "syntaxTextArea");

            if (syntaxTextAreaReq == null || syntaxTextAreaRes == null) {
                api.logging().logToError(String.format("Full Screenshot: Could not find syntaxTextArea in Req (%s found) or Res (%s found). Cannot take screenshot.",
                    syntaxTextAreaReq != null, syntaxTextAreaRes != null));
                return;
            }
            api.logging().logToOutput("Full Screenshot: Found syntaxTextAreas.");

            // Capture both components
            captureComponentToBuffer(syntaxTextAreaReq);
            captureComponentToBuffer(syntaxTextAreaRes);

            // Combine (combineBufferedImagesHorizontally now clears the buffer)
            BufferedImage combinedImage = combineBufferedImagesHorizontally();

            if (combinedImage != null) {
                 api.logging().logToOutput("Full Screenshot: Screenshot combined. Launching annotator...");
                 launchAnnotationEditor(combinedImage); // Launch editor with combined image
            } else {
                 api.logging().logToError("Full Screenshot: Screenshot failed - combined image was null.");
                 // Buffer should be clear already due to combineBufferedImagesHorizontally logic
            }
        } catch (Exception ex) {
            api.logging().logToError("Error in handleFullScreenshot: " + ex.toString(), ex);
             clearImages(); // Ensure buffer is cleared on unexpected errors
        }
    }

    // Helper for recursive search (used in handleFullScreenshot)
    private static Component findComponentByNameRecursively(Component parent, String name) {
        if (parent == null || name == null) return null;
        if (name.equals(parent.getName())) {
            return parent;
        }
        if (parent instanceof Container) {
            for (Component child : ((Container) parent).getComponents()) {
                Component found = findComponentByNameRecursively(child, name);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }


    // --- THE UPDATED ANNOTATION EDITOR LAUNCHER ---
    /**
     * Creates and displays the annotation editor window.
     * Now opens maximized and includes Ctrl+C / Ctrl+S hotkeys.
     * @param snap The initial BufferedImage captured.
     */
    private void launchAnnotationEditor(final BufferedImage snap) {
        if (snap == null) {
            api.logging().logToError("Cannot launch editor: Snapshot image is null.");
            return;
        }
        api.logging().logToOutput("Launching annotation editor...");

        // Ensure editor creation runs on EDT
        SwingUtilities.invokeLater(() -> {
            try {
                final int BAR_W = 140; // Toolbar width
                final JFrame editor = new JFrame("Annotate Screenshot");
                editor.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
                editor.getRootPane().setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY, 2));

                JPanel root = new JPanel(new BorderLayout(5, 5));
                editor.setContentPane(root);

                // --- Image display + layered drawing pane ---
                JLabel imgLabel = new JLabel(new ImageIcon(snap));
                JLayeredPane stack = new JLayeredPane();
                // Set preferred size based on image for initial layout calculation
                stack.setPreferredSize(new Dimension(snap.getWidth(), snap.getHeight()));
                imgLabel.setBounds(0, 0, snap.getWidth(), snap.getHeight());
                stack.add(imgLabel, JLayeredPane.DEFAULT_LAYER);

                // --- Drawing state ---
                final List<Shape> shapes = new ArrayList<>();
                final List<Color> cols = new ArrayList<>();
                final List<String> kinds = new ArrayList<>();
                final Color[] curCol = {Color.RED};
                final String[] mode = {"RECT"};
                final float STROKE_WIDTH = 3f;
                final int HIGHLIGHT_ALPHA = 80;
                final Point[] startDrag = {null};
                final Shape[] previewShape = {null};

                // --- Drawing component (transparent overlay) ---
                final JComponent drawCanvas = new JComponent() {
                    { setOpaque(false); } // Make canvas transparent
                    @Override
                    protected void paintComponent(Graphics g) {
                        super.paintComponent(g);
                        Graphics2D g2 = (Graphics2D) g.create();
                        try {
                            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                            g2.setStroke(new BasicStroke(STROKE_WIDTH));
                            // Draw existing shapes
                            for (int i = 0; i < shapes.size(); i++) {
                                g2.setColor(cols.get(i)); // Color already includes alpha if needed
                                Shape s = shapes.get(i);
                                String k = kinds.get(i);
                                if ("HIGHLIGHT".equals(k)) {
                                    g2.fill(s);
                                } else {
                                    g2.draw(s);
                                }
                            }
                            // Draw preview shape
                            if (previewShape[0] != null && startDrag[0] != null) {
                                g2.setColor(getColorForMode(curCol[0], mode[0], HIGHLIGHT_ALPHA));
                                if ("HIGHLIGHT".equals(mode[0])) {
                                    g2.fill(previewShape[0]);
                                } else {
                                    g2.draw(previewShape[0]);
                                }
                            }
                        } finally {
                            g2.dispose(); // Ensure graphics context is always disposed
                        }
                    }
                     // Set preferred size for the drawing canvas itself
                    @Override
                    public Dimension getPreferredSize() {
                        return new Dimension(snap.getWidth(), snap.getHeight());
                    }
                };
                // Set bounds AFTER stack is added to scrollpane or parent layout manages it
                // We set preferredSize on stack earlier, now set drawCanvas size too
                // drawCanvas.setPreferredSize(new Dimension(snap.getWidth(), snap.getHeight())); // Set via override instead
                drawCanvas.setBounds(0, 0, snap.getWidth(), snap.getHeight()); // Still needed for JLayeredPane layout
                stack.add(drawCanvas, JLayeredPane.PALETTE_LAYER); // Add drawing canvas on top

                // --- Mouse listener for drawing ---
                MouseAdapter drawListener = new MouseAdapter() {
                    @Override public void mousePressed(MouseEvent e) {
                        startDrag[0] = e.getPoint(); previewShape[0] = null; drawCanvas.repaint();
                    }
                    @Override public void mouseDragged(MouseEvent e) {
                        if (startDrag[0] == null) return;
                        Point p = e.getPoint();
                        // Prevent drawing outside image bounds (optional but good practice)
                        p.x = Math.max(0, Math.min(p.x, snap.getWidth() - 1));
                        p.y = Math.max(0, Math.min(p.y, snap.getHeight() - 1));

                        if ("LINE".equals(mode[0])) {
                            previewShape[0] = new Line2D.Double(startDrag[0], p);
                        } else { // RECT or HIGHLIGHT
                            int x = Math.min(startDrag[0].x, p.x);
                            int y = Math.min(startDrag[0].y, p.y);
                            int w = Math.abs(p.x - startDrag[0].x);
                            int h = Math.abs(p.y - startDrag[0].y);
                            // Ensure minimum size 1x1 to avoid issues with zero-dimension shapes
                            previewShape[0] = new Rectangle2D.Double(x, y, Math.max(w, 1), Math.max(h, 1));
                        }
                        drawCanvas.repaint();
                    }
                    @Override public void mouseReleased(MouseEvent e) {
                        if (startDrag[0] != null && previewShape[0] != null) {
                            // Only add if the shape has non-zero dimensions (for rect/highlight)
                            boolean hasSize = true;
                            if (previewShape[0] instanceof Rectangle2D) {
                                Rectangle2D r = (Rectangle2D) previewShape[0];
                                hasSize = r.getWidth() > 1 && r.getHeight() > 1; // Require more than 1 pixel?
                            } else if (previewShape[0] instanceof Line2D) {
                                Line2D l = (Line2D) previewShape[0];
                                hasSize = !l.getP1().equals(l.getP2()); // Check if start/end points differ
                            }

                            if (hasSize) {
                                shapes.add(previewShape[0]);
                                cols.add(getColorForMode(curCol[0], mode[0], HIGHLIGHT_ALPHA));
                                kinds.add(mode[0]);
                            } else {
                                api.logging().logToOutput("Ignoring zero-size shape draw attempt.");
                            }
                        }
                        startDrag[0] = null; previewShape[0] = null; drawCanvas.repaint();
                    }
                };
                drawCanvas.addMouseListener(drawListener);
                drawCanvas.addMouseMotionListener(drawListener);

                // --- Wrap the Layered Pane in a Scroll Pane ---
                JScrollPane scrollPane = new JScrollPane(stack);
                // Let the scrollpane determine its preferred size based on content and screen,
                // pack() will handle initial sizing, maximization handles fullscreen.
                scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
                scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
                // Improve scrolling speed slightly
                scrollPane.getVerticalScrollBar().setUnitIncrement(16);
                scrollPane.getHorizontalScrollBar().setUnitIncrement(16);
                root.add(scrollPane, BorderLayout.CENTER); // Add scrollPane to center

                // --- Toolbar ---
                JPanel bar = new JPanel();
                bar.setLayout(new BoxLayout(bar, BoxLayout.Y_AXIS));
                bar.setBorder(new EmptyBorder(8, 8, 8, 8));
                bar.setPreferredSize(new Dimension(BAR_W, 100)); // Preferred width set
                root.add(bar, BorderLayout.EAST);

                // Helper to add toolbar components
                final Consumer<Component> addToolbarComponent = (comp) -> {
                    if (comp instanceof JComponent) {
                         ((JComponent) comp).setAlignmentX(Component.CENTER_ALIGNMENT);
                         Dimension prefSize = comp.getPreferredSize();
                         // Allow button to use its preferred height, constrain width
                         comp.setMaximumSize(new Dimension(BAR_W - 16, prefSize.height));
                    }
                     bar.add(comp);
                     bar.add(Box.createRigidArea(new Dimension(0, 5))); // Spacing
                 };

                // --- Shape Radio Buttons ---
                ButtonGroup grpShape = new ButtonGroup();
                JToggleButton rBtn = new JToggleButton("Rect", true); // Default selected
                JToggleButton lBtn = new JToggleButton("Line");
                JToggleButton hBtn = new JToggleButton("Highlight");
                grpShape.add(rBtn); grpShape.add(lBtn); grpShape.add(hBtn);
                ActionListener shapeListener = a -> {
                    String oldMode = mode[0];
                    if (a.getSource() == rBtn) mode[0] = "RECT";
                    else if (a.getSource() == lBtn) mode[0] = "LINE";
                    else if (a.getSource() == hBtn) mode[0] = "HIGHLIGHT";

                    // Set cursor based on mode
                     if ("LINE".equals(mode[0])) {
                         drawCanvas.setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR)); // Crosshair might be better for line too
                     } else { // RECT, HIGHLIGHT
                         drawCanvas.setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
                     }
                     if (!mode[0].equals(oldMode)) {
                        api.logging().logToOutput("Annotation mode set to: " + mode[0]);
                     }
                };
                rBtn.addActionListener(shapeListener);
                lBtn.addActionListener(shapeListener);
                hBtn.addActionListener(shapeListener);
                addToolbarComponent.accept(rBtn);
                addToolbarComponent.accept(lBtn);
                addToolbarComponent.accept(hBtn);
                bar.add(Box.createRigidArea(new Dimension(0, 10))); // Spacing
                addToolbarComponent.accept(new JSeparator(SwingConstants.HORIZONTAL));
                bar.add(Box.createRigidArea(new Dimension(0, 10))); // Spacing

                // --- Color Chooser Button ---
                JButton colBtn = new JButton("Colour");
                colBtn.setBackground(curCol[0]);
                colBtn.setForeground(getContrastColor(curCol[0]));
                colBtn.setOpaque(true);
                colBtn.setBorderPainted(false); // Flat look
                colBtn.setFocusPainted(false); // Remove focus border
                colBtn.addActionListener(a -> {
                    Color chosen = JColorChooser.showDialog(editor, "Choose Annotation Colour", curCol[0]);
                    if (chosen != null) {
                        curCol[0] = chosen;
                        colBtn.setBackground(chosen);
                        colBtn.setForeground(getContrastColor(chosen));
                        api.logging().logToOutput("Annotation color changed.");
                    }
                });
                addToolbarComponent.accept(colBtn);

                // --- Undo Button ---
                // Action for Undo (to allow hotkey)
                Action undoAction = new AbstractAction("Undo (Ctrl+Z)") {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                         if (!shapes.isEmpty()) {
                            int lastIndex = shapes.size() - 1;
                            shapes.remove(lastIndex);
                            cols.remove(lastIndex);
                            kinds.remove(lastIndex);
                            drawCanvas.repaint();
                            api.logging().logToOutput("Annotation undone.");
                        } else {
                            api.logging().logToOutput("Nothing to undo.");
                        }
                    }
                };
                JButton undoBtn = new JButton(undoAction); // Create button from action
                addToolbarComponent.accept(undoBtn);

                bar.add(Box.createVerticalGlue()); // Pushes Save/Copy to bottom
                addToolbarComponent.accept(new JSeparator(SwingConstants.HORIZONTAL));
                bar.add(Box.createRigidArea(new Dimension(0, 10))); // Spacing


                // --- Define Actions for Copy and Save (for Buttons and Hotkeys) ---

                // Copy Action
                Action copyAction = new AbstractAction("Copy (Ctrl+C)") {
                    @Override
                    public void actionPerformed(ActionEvent a) {
                        api.logging().logToOutput("[Copy Action] Action started via " + a.getActionCommand()); // Log source (button/key)
                        api.logging().logToOutput("[Copy Action] Shapes count: " + shapes.size());
                        api.logging().logToOutput("[Copy Action] Original image size: " + snap.getWidth() + "x" + snap.getHeight());
                        try {
                            api.logging().logToOutput("[Copy Action] Calling renderAnnotatedImage...");
                            // Pass necessary parameters to render method
                            BufferedImage finalImage = renderAnnotatedImage(snap, shapes, cols, kinds, STROKE_WIDTH);
                            api.logging().logToOutput("[Copy Action] renderAnnotatedImage returned: "
                                    + (finalImage != null ? finalImage.getWidth()+"x"+finalImage.getHeight() : "null"));

                            if (finalImage == null) {
                                api.logging().logToError("[Copy Action] Error: Rendered image is null!");
                                JOptionPane.showMessageDialog(editor, "Error copying image:\nRendered image was null.", "Render Error", JOptionPane.ERROR_MESSAGE);
                                return; // Stop processing
                            }

                            api.logging().logToOutput("[Copy Action] Calling copyImageToClipboard...");
                            copyImageToClipboard(finalImage); // Use existing utility method
                            api.logging().logToOutput("[Copy Action] copyImageToClipboard finished.");

                            // Give feedback and close
                            JOptionPane.showMessageDialog(editor, "Annotated image copied to clipboard.", "Copied", JOptionPane.INFORMATION_MESSAGE);
                            editor.dispose(); // Close editor after copy

                        } catch (Exception ex) {
                            api.logging().logToError("[Copy Action] Error rendering or copying annotated image: " + ex.toString(), ex);
                            JOptionPane.showMessageDialog(editor, "Error copying image:\n" + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                        } finally {
                            api.logging().logToOutput("[Copy Action] Action finished.");
                        }
                    }
                };

                // Save Action
                Action saveAction = new AbstractAction("Save (Ctrl+S)") {
                    @Override
                    public void actionPerformed(ActionEvent a) {
                        api.logging().logToOutput("[Save Action] Action started via " + a.getActionCommand()); // Log source
                        api.logging().logToOutput("[Save Action] Shapes count: " + shapes.size());
                        api.logging().logToOutput("[Save Action] Original image size: " + snap.getWidth() + "x" + snap.getHeight());
                        try {
                            api.logging().logToOutput("[Save Action] Calling renderAnnotatedImage...");
                            BufferedImage finalImage = renderAnnotatedImage(snap, shapes, cols, kinds, STROKE_WIDTH);
                            api.logging().logToOutput("[Save Action] renderAnnotatedImage returned: "
                                    + (finalImage != null ? finalImage.getWidth()+"x"+finalImage.getHeight() : "null"));

                            if (finalImage == null) {
                                api.logging().logToError("[Save Action] Error: Rendered image is null!");
                                JOptionPane.showMessageDialog(editor, "Error saving image:\nRendered image was null.", "Render Error", JOptionPane.ERROR_MESSAGE);
                                return; // Stop processing
                            }

                            JFileChooser fc = new JFileChooser();
                            fc.setDialogTitle("Save Annotated Screenshot");
                            fc.setSelectedFile(new File("annotated_screenshot.png"));
                            // Use FileNameExtensionFilter (more standard)
                            fc.setFileFilter(new FileNameExtensionFilter("PNG Images (*.png)", "png"));
                            fc.setAcceptAllFileFilterUsed(false); // Only allow PNG

                            api.logging().logToOutput("[Save Action] Showing save dialog...");
                            int res = fc.showSaveDialog(editor);
                            api.logging().logToOutput("[Save Action] Save dialog returned: " + (res == JFileChooser.APPROVE_OPTION ? "Approve" : "Cancel/Error"));

                            if (res == JFileChooser.APPROVE_OPTION) {
                                File f = fc.getSelectedFile();
                                // Ensure .png extension
                                if (!f.getName().toLowerCase().endsWith(".png")) {
                                    f = new File(f.getParentFile(), f.getName() + ".png");
                                }

                                // Check for overwrite
                                if (f.exists()) {
                                    int overwriteRes = JOptionPane.showConfirmDialog(editor,
                                        "File already exists:\n" + f.getName() + "\nOverwrite?",
                                        "Confirm Overwrite", JOptionPane.YES_NO_OPTION);
                                    if (overwriteRes != JOptionPane.YES_OPTION) {
                                        api.logging().logToOutput("[Save Action] Save cancelled by user (overwrite denied).");
                                        return; // Don't save or close
                                    }
                                }

                                api.logging().logToOutput("[Save Action] Writing image to " + f.getAbsolutePath());
                                boolean success = ImageIO.write(finalImage, "png", f);
                                api.logging().logToOutput("[Save Action] Image writing finished. Success: " + success);

                                if (success) {
                                    JOptionPane.showMessageDialog(editor,
                                            "Saved to:\n" + f.getAbsolutePath(), "Saved", JOptionPane.INFORMATION_MESSAGE);
                                    editor.dispose(); // Close editor after successful save
                                } else {
                                    api.logging().logToError("[Save Action] ImageIO.write returned false.");
                                    JOptionPane.showMessageDialog(editor, "Error saving image (write operation failed).\nCheck file permissions or disk space.", "Save Error", JOptionPane.ERROR_MESSAGE);
                                }
                            } else {
                                api.logging().logToOutput("[Save Action] Save cancelled by user.");
                            }
                        } catch (IOException ex) {
                            api.logging().logToError("[Save Action] IO Error saving annotated image: " + ex.toString(), ex);
                            JOptionPane.showMessageDialog(editor, "Error saving image (I/O):\n" + ex.getMessage(), "Save Error", JOptionPane.ERROR_MESSAGE);
                        } catch (Exception ex) {
                            api.logging().logToError("[Save Action] General Error rendering or saving annotated image: " + ex.toString(), ex);
                            JOptionPane.showMessageDialog(editor, "Error preparing/saving image:\n" + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                        } finally {
                            api.logging().logToOutput("[Save Action] Action finished.");
                        }
                    }
                };

                // --- Create Buttons using Actions ---
                 JButton copyBtn = new JButton(copyAction);
                 JButton saveBtn = new JButton(saveAction); // Use the Action

                 addToolbarComponent.accept(copyBtn);
                 addToolbarComponent.accept(saveBtn);


                // --- Set up Key Bindings (Hotkeys) ---
                JRootPane rootPane = editor.getRootPane();
                InputMap inputMap = rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
                ActionMap actionMap = rootPane.getActionMap();

                // Ctrl+C (or Cmd+C on Mac)
                // Use getMenuShortcutKeyMaskEx() for modern systems
                KeyStroke copyKeyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_C, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx());
                inputMap.put(copyKeyStroke, "copyActionMapKey"); // Unique key for the map
                actionMap.put("copyActionMapKey", copyAction); // Map key to the Action object

                // Ctrl+S (or Cmd+S on Mac)
                KeyStroke saveKeyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_S, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx());
                inputMap.put(saveKeyStroke, "saveActionMapKey");
                actionMap.put("saveActionMapKey", saveAction);

                // Ctrl+Z (or Cmd+Z on Mac) for Undo
                KeyStroke undoKeyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_Z, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx());
                inputMap.put(undoKeyStroke, "undoActionMapKey");
                actionMap.put("undoActionMapKey", undoAction); // Map to the undo Action


                // --- Finalize Editor ---
                editor.pack(); // Pack first to calculate preferred sizes
                editor.setMinimumSize(new Dimension(500, 400)); // Set a reasonable minimum size AFTER packing

                // <<< NEW: Maximize the window >>>
                editor.setExtendedState(JFrame.MAXIMIZED_BOTH);

                // Center on screen (might not be strictly needed after maximize, but doesn't hurt)
                editor.setLocationRelativeTo(null);

                // Set initial cursor and tool selection
                drawCanvas.setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
                rBtn.setSelected(true); // Ensure Rect button is visually selected initially

                // Make it visible
                editor.setVisible(true);
                api.logging().logToOutput("Annotation editor is now visible (maximized, with scrolling and hotkeys).");

                // Request focus for the drawing canvas so it receives mouse events immediately
                drawCanvas.requestFocusInWindow();

            } catch (Exception ex) {
                 api.logging().logToError("Failed to launch annotation editor: " + ex.toString(), ex);
                 // Show error to user as well
                 JOptionPane.showMessageDialog(null, // Parent might be null if editor failed early
                     "Could not launch the annotation editor:\n" + ex.getMessage(),
                     "Editor Launch Error", JOptionPane.ERROR_MESSAGE);
            }
        }); // End SwingUtilities.invokeLater
    }
    // --- End of the updated function ---


    // --- Helper Methods (unchanged from previous version, ensure they are correct) ---
    private Color getColorForMode(Color baseColor, String mode, int highlightAlpha) {
        if ("HIGHLIGHT".equals(mode)) {
            // Ensure alpha is within valid range 0-255
            int alpha = Math.max(0, Math.min(highlightAlpha, 255));
            return new Color(baseColor.getRed(), baseColor.getGreen(), baseColor.getBlue(), alpha);
        } else {
            return baseColor; // Return opaque color for line/rect
        }
    }

    private Color getContrastColor(Color color) {
        // Simple heuristic based on perceived luminance
        double luminance = (0.299 * color.getRed() + 0.587 * color.getGreen() + 0.114 * color.getBlue()) / 255.0;
        // Adjust threshold slightly? 0.5 is common.
        return luminance > 0.5 ? Color.BLACK : Color.WHITE;
    }

    // Ensure this render method handles the colors (with alpha for highlights) correctly
    private BufferedImage renderAnnotatedImage(BufferedImage original, List<Shape> shapes, List<Color> cols, List<String> kinds, float strokeWidth) {
         if (original == null) {
             api.logging().logToError("renderAnnotatedImage: Original image is null.");
             return null;
         }
         api.logging().logToOutput("Rendering final annotated image...");
         int w = original.getWidth();
         int h = original.getHeight();
         if (w <= 0 || h <= 0) {
              api.logging().logToError("renderAnnotatedImage: Original image has invalid dimensions (" + w + "x" + h + ")");
              return null;
         }

         // Create output image with Alpha channel to support transparent highlights
         BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = out.createGraphics();
        try {
            // 1. Draw the original image as the base layer
            g2.drawImage(original, 0, 0, null);

            // 2. Prepare for drawing annotations
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setStroke(new BasicStroke(strokeWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)); // Use round caps/joins

            // 3. Draw each shape
            for (int i = 0; i < shapes.size(); i++) {
                Shape s = shapes.get(i);
                Color c = cols.get(i); // This color already has alpha set if it's a highlight
                String k = kinds.get(i);

                if (s == null || c == null || k == null) {
                    api.logging().logToOutput("Skipping null shape/color/kind at index " + i);
                    continue;
                }

                g2.setColor(c); // Set the color (includes alpha)

                if ("HIGHLIGHT".equals(k)) {
                    g2.fill(s); // Fill shape for highlights (uses alpha from color)
                } else { // RECT, LINE
                    g2.draw(s); // Draw outline for others
                }
            }
            api.logging().logToOutput("Finished rendering annotations.");
        } catch (Exception e) {
             api.logging().logToError("Error during rendering annotations: " + e.toString(), e);
             // Depending on severity, might return null or the partially rendered image
             // For safety, dispose graphics and return null or the image *before* the error
        } finally {
            g2.dispose(); // VERY IMPORTANT: Release graphics resources
        }
        return out;
    }

} // End of ScreenshotUtils class