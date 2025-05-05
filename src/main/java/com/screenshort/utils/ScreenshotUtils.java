package com.screenshort.utils;

import burp.api.montoya.MontoyaApi;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.*; // Import event classes
import java.awt.geom.Area; // Import geom classes
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File; // For JFileChooser
import java.io.IOException; // Import cho IOException trong Transferable
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import javax.imageio.ImageIO; // For ImageIO
import javax.swing.*; // Import swing classes
import javax.swing.border.EmptyBorder; // For borders
import javax.swing.JScrollPane; // <-- Import JScrollPane


public class ScreenshotUtils {

    private final MontoyaApi api;
    private final List<BufferedImage> images = new ArrayList<>();

    // Constructor
    public ScreenshotUtils(MontoyaApi api) {
        this.api = api;
    }

    // --- Static Helper Methods (Component Finding) ---
    // ... (findComponentUnderMouse, getComponentByName, etc. - assumed to be here) ...
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
            System.err.println("Error in findComponentUnderMouse: " + e.getMessage());
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
            BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = image.createGraphics();
            component.paint(g);
            g.dispose();
            images.add(image);
            api.logging().logToOutput(String.format("Captured component screenshot for buffer (size: %dx%d). Buffer size: %d", w, h, images.size()));
        } catch (Exception e) {
            api.logging().logToError("Error capturing component screenshot to buffer: " + e.toString(), e);
        }
    }

     private void copyImageToClipboard(BufferedImage image) {
         if (image == null) {
             api.logging().logToOutput("Cannot copy a null image to clipboard.");
             return;
         }
          try {
             ImageSelection imageSelection = new ImageSelection(image);
             Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
             clipboard.setContents(imageSelection, null);
             api.logging().logToOutput("Image copied to clipboard.");
          } catch (IllegalStateException e) {
              api.logging().logToError("Error accessing system clipboard (perhaps headless?): " + e.toString(), e);
          } catch (Exception e) {
             api.logging().logToError("Error copying image to clipboard: " + e.toString(), e);
          }
     }

    private BufferedImage combineBufferedImagesHorizontally() {
        api.logging().logToOutput("Attempting to combine images from buffer...");
        try {
            if (images.size() < 2) {
                api.logging().logToOutput("Buffer contains less than 2 images. Cannot combine.");
                return null;
            }
            BufferedImage bf1 = images.get(0);
            BufferedImage bf2 = images.get(1);
            if (bf1 == null || bf2 == null) {
                api.logging().logToOutput("One or both images in buffer are null. Cannot combine.");
                return null;
            }
            int borderThickness = 5;
            Color borderColor = Color.BLACK;
            int totalWidth = bf1.getWidth() + bf2.getWidth() + borderThickness;
            int maxHeight = Math.max(bf1.getHeight(), bf2.getHeight());
             if (totalWidth <= 0 || maxHeight <= 0) {
                api.logging().logToOutput(String.format("Combined image size is invalid (width: %d, height: %d). Cannot combine.", totalWidth, maxHeight));
                return null;
             }
            BufferedImage combined = new BufferedImage(totalWidth, maxHeight, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = combined.createGraphics();
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, totalWidth, maxHeight);
            g.drawImage(bf1, 0, 0, null);
            g.setColor(borderColor);
            g.fillRect(bf1.getWidth(), 0, borderThickness, maxHeight);
            g.drawImage(bf2, bf1.getWidth() + borderThickness, 0, null);
            g.dispose();
            api.logging().logToOutput(String.format("Images combined successfully (size: %dx%d).", totalWidth, maxHeight));
            return combined;
        } catch (IndexOutOfBoundsException e) {
            api.logging().logToError("Index Out Of Bounds error when accessing images buffer: " + e.getMessage(), e);
            return null;
        } catch (Exception e) {
            api.logging().logToError("Error in combineBufferedImagesHorizontally: " + e.toString(), e);
            return null;
        } finally {
             clearImages();
        }
    }

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
    // ... (handleNormalScreenshot, handleFullScreenshot, handleAnnotateScreenshot - assumed here) ...
     public void handleNormalScreenshot() {
        api.logging().logToOutput("Executing Normal Screenshot (to Annotator)...");
        try {
            Frame suiteFrame = api.userInterface().swingUtils().suiteFrame();
            if (suiteFrame == null) {
                api.logging().logToOutput("Could not get Burp Suite main frame."); return;
            }
            Component targetComponent = findComponentUnderMouse("rrvSplitViewerSplitPane", suiteFrame);
            if (targetComponent == null) {
                 api.logging().logToOutput("Could not find 'rrvSplitViewerSplitPane' under mouse. Trying focused component.");
                 targetComponent = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
                 if (targetComponent == null || !SwingUtilities.isDescendingFrom(targetComponent, suiteFrame)) {
                      api.logging().logToOutput("No suitable component found under mouse or focused."); return;
                 }
                 api.logging().logToOutput("Found focused component: " + targetComponent.getClass().getName());
            }
            api.logging().logToOutput("Taking screenshot of component: " + targetComponent.getName());
            int w = targetComponent.getWidth();
            int h = targetComponent.getHeight();
             if (w <= 0 || h <= 0) {
                api.logging().logToOutput(String.format("Target component size is invalid (width: %d, height: %d). Cannot capture.", w, h));
                return;
            }
            BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = image.createGraphics();
            targetComponent.paint(g);
            g.dispose();
            api.logging().logToOutput("Normal screenshot captured. Launching annotator...");
            launchAnnotationEditor(image);
        } catch (Exception ex) {
             api.logging().logToError("Error in handleNormalScreenshot: " + ex.toString(), ex);
        }
    }

    public void handleFullScreenshot() {
        api.logging().logToOutput("Executing Full Screenshot (Req/Res to Annotator)...");
        clearImages();
        try {
            Frame suiteFrame = api.userInterface().swingUtils().suiteFrame();
             if (suiteFrame == null) {
                api.logging().logToOutput("Full Screenshot: Could not get Burp Suite main frame."); return;
            }
            Component targetSplitPane = findComponentUnderMouse("rrvSplitViewerSplitPane", suiteFrame);
             if (targetSplitPane == null) {
                 api.logging().logToOutput("Full Screenshot: Could not find 'rrvSplitViewerSplitPane' component under mouse."); return;
             }
             api.logging().logToOutput("Full Screenshot: Found target split pane.");
            Component reqComp = getComponentByName(targetSplitPane, "rrvRequestsPane");
            Component resComp = getComponentByName(targetSplitPane, "rrvResponsePane");
             if (reqComp == null || resComp == null) {
                 api.logging().logToOutput("Full Screenshot: Could not find Request or Response panes within the target split pane."); return;
             }
              api.logging().logToOutput("Full Screenshot: Found request and response panes.");
            java.util.List<Component> reqTextAreas = findAllComponentsByName(reqComp, "syntaxTextArea");
            java.util.List<Component> resTextAreas = findAllComponentsByName(resComp, "syntaxTextArea");
            if (reqTextAreas.isEmpty() || resTextAreas.isEmpty()) {
                api.logging().logToOutput(String.format("Full Screenshot: Could not find syntaxTextArea in Req (%d found) or Res (%d found). Cannot take screenshot.", reqTextAreas.size(), resTextAreas.size()));
                return;
            }
            api.logging().logToOutput("Full Screenshot: Found first syntaxTextAreas.");
            Component syntaxTextAreaReq = reqTextAreas.get(0);
            Component syntaxTextAreaRes = resTextAreas.get(0);
            captureComponentToBuffer(syntaxTextAreaReq);
            captureComponentToBuffer(syntaxTextAreaRes);
            BufferedImage combinedImage = combineBufferedImagesHorizontally();
            if (combinedImage != null) {
                 api.logging().logToOutput("Full Screenshot: Screenshot combined. Launching annotator...");
                 launchAnnotationEditor(combinedImage);
            } else {
                 api.logging().logToOutput("Full Screenshot: Screenshot failed due to image combining issues.");
            }
        } catch (Exception ex) {
            api.logging().logToError("Error in handleFullScreenshot: " + ex.toString(), ex);
             clearImages();
        }
    }

     public void handleAnnotateScreenshot() {
        api.logging().logToOutput("Executing Annotate Region...");
        SwingUtilities.invokeLater(() -> {
            try {
                final int BORDER = 3, GRIP = 6, BTN_W = 90, BTN_H = 30;
                final JWindow overlay = new JWindow();
                overlay.setBounds(200, 200, 600, 350);
                overlay.setAlwaysOnTop(true);
                overlay.setFocusableWindowState(true);
                Color CLEAR = new Color(0, 0, 0, 0);
                overlay.setBackground(CLEAR);
                if (overlay.getContentPane() instanceof JComponent) ((JComponent) overlay.getContentPane()).setOpaque(false);
                 overlay.getRootPane().setOpaque(false);
                overlay.getContentPane().setLayout(null);
                final Runnable updateShape = () -> { /* ... shape update logic ... */ }; // Assume shape logic is correct
                overlay.addComponentListener(new ComponentAdapter() { /* ... listener for shape update ... */ });
                JComponent framePainter = new JComponent() { /* ... painter logic ... */ };
                framePainter.setBounds(0, 0, overlay.getWidth(), overlay.getHeight());
                overlay.setContentPane(framePainter);
                overlay.getContentPane().setLayout(null);
                updateShape.run();
                final JButton capture = new JButton("Capture");
                capture.setBounds(BORDER + 4, BORDER + 4, BTN_W, BTN_H);
                capture.setFocusable(false);
                overlay.getContentPane().add(capture);
                MouseAdapter mover = new MouseAdapter() { /* ... move/resize logic ... */ };
                framePainter.addMouseListener(mover);
                framePainter.addMouseMotionListener(mover);
                capture.addActionListener(ev -> {
                    try {
                        Rectangle reg = overlay.getBounds();
                        reg.x += BORDER; reg.y += BORDER;
                        reg.width -= BORDER * 2; reg.height -= BORDER * 2;
                        if (reg.width <= 0 || reg.height <= 0) {
                            api.logging().logToOutput("Annotation region is too small to capture.");
                            overlay.dispose(); return;
                        }
                        overlay.setVisible(false);
                        try { Thread.sleep(100); } catch (InterruptedException ignored) {}
                        BufferedImage snap = new Robot().createScreenCapture(reg);
                        overlay.dispose();
                        launchAnnotationEditor(snap);
                    } catch (AWTException | SecurityException ex) {
                        api.logging().logToError("Error during region capture setup/Robot: " + ex.toString(), ex);
                        overlay.dispose();
                    } catch (Exception ex) {
                         api.logging().logToError("Error during region capture or editor launch: " + ex.toString(), ex);
                         overlay.dispose();
                    }
                });
                overlay.setVisible(true);
                 api.logging().logToOutput("Annotation overlay displayed.");
            } catch (Exception ex) {
                api.logging().logToError("Error setting up annotation overlay: " + ex.toString(), ex);
            }
        });
    }

    // --- THE FUNCTION IN QUESTION ---
    /**
     * Creates and displays the annotation editor window.
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
                stack.setPreferredSize(new Dimension(snap.getWidth(), snap.getHeight())); // Set preferred size for scrollpane
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
                    { setOpaque(false); }
                    @Override
                    protected void paintComponent(Graphics g) {
                        super.paintComponent(g);
                        Graphics2D g2 = (Graphics2D) g.create();
                        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                        g2.setStroke(new BasicStroke(STROKE_WIDTH));
                        // Draw existing shapes
                        for (int i = 0; i < shapes.size(); i++) {
                            g2.setColor(cols.get(i));
                            Shape s = shapes.get(i); String k = kinds.get(i);
                            if ("HIGHLIGHT".equals(k)) g2.fill(s); else g2.draw(s);
                        }
                        // Draw preview shape
                        if (previewShape[0] != null && startDrag[0] != null) {
                             g2.setColor(getColorForMode(curCol[0], mode[0], HIGHLIGHT_ALPHA));
                             if ("HIGHLIGHT".equals(mode[0])) g2.fill(previewShape[0]); else g2.draw(previewShape[0]);
                        }
                        g2.dispose();
                    }
                };
                drawCanvas.setBounds(0, 0, snap.getWidth(), snap.getHeight());
                stack.add(drawCanvas, JLayeredPane.PALETTE_LAYER);

                // --- Mouse listener for drawing ---
                MouseAdapter drawListener = new MouseAdapter() {
                    @Override public void mousePressed(MouseEvent e) {
                        startDrag[0] = e.getPoint(); previewShape[0] = null; drawCanvas.repaint();
                    }
                    @Override public void mouseDragged(MouseEvent e) {
                        if (startDrag[0] == null) return;
                        Point p = e.getPoint();
                        if ("LINE".equals(mode[0])) {
                            previewShape[0] = new Line2D.Double(startDrag[0], p);
                        } else {
                            int x = Math.min(startDrag[0].x, p.x), y = Math.min(startDrag[0].y, p.y);
                            int w = Math.abs(p.x - startDrag[0].x), h = Math.abs(p.y - startDrag[0].y);
                            previewShape[0] = new Rectangle2D.Double(x, y, Math.max(w, 1), Math.max(h, 1));
                        }
                        drawCanvas.repaint();
                    }
                    @Override public void mouseReleased(MouseEvent e) {
                        if (startDrag[0] != null && previewShape[0] != null) {
                            shapes.add(previewShape[0]);
                            cols.add(getColorForMode(curCol[0], mode[0], HIGHLIGHT_ALPHA));
                            kinds.add(mode[0]);
                        }
                        startDrag[0] = null; previewShape[0] = null; drawCanvas.repaint();
                    }
                };
                drawCanvas.addMouseListener(drawListener);
                drawCanvas.addMouseMotionListener(drawListener);

                // --- Wrap the Layered Pane in a Scroll Pane ---
                JScrollPane scrollPane = new JScrollPane(stack);
                scrollPane.setPreferredSize(new Dimension(Math.min(snap.getWidth(), 800), Math.min(snap.getHeight(), 600)));
                scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
                scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
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
                         comp.setMaximumSize(new Dimension(BAR_W - 16, prefSize.height));
                    }
                     bar.add(comp);
                     bar.add(Box.createRigidArea(new Dimension(0, 5)));
                 };

                // --- Shape Radio Buttons ---
                ButtonGroup grpShape = new ButtonGroup();
                JToggleButton rBtn = new JToggleButton("Rect", true); JToggleButton lBtn = new JToggleButton("Line"); JToggleButton hBtn = new JToggleButton("Highlight");
                grpShape.add(rBtn); grpShape.add(lBtn); grpShape.add(hBtn);
                ActionListener shapeListener = a -> {
                    if (a.getSource() == rBtn) mode[0] = "RECT"; else if (a.getSource() == lBtn) mode[0] = "LINE"; else if (a.getSource() == hBtn) mode[0] = "HIGHLIGHT";
                     if ("LINE".equals(mode[0])) drawCanvas.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                     else drawCanvas.setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
                     api.logging().logToOutput("Annotation mode set to: " + mode[0]);
                };
                rBtn.addActionListener(shapeListener); lBtn.addActionListener(shapeListener); hBtn.addActionListener(shapeListener);
                addToolbarComponent.accept(rBtn); addToolbarComponent.accept(lBtn); addToolbarComponent.accept(hBtn);
                bar.add(Box.createRigidArea(new Dimension(0, 10)));
                addToolbarComponent.accept(new JSeparator(SwingConstants.HORIZONTAL));
                bar.add(Box.createRigidArea(new Dimension(0, 10)));

                // --- Color Chooser Button ---
                JButton colBtn = new JButton("Colour");
                colBtn.setBackground(curCol[0]); colBtn.setForeground(getContrastColor(curCol[0]));
                colBtn.setOpaque(true); colBtn.setBorderPainted(false);
                colBtn.addActionListener(a -> {
                    Color chosen = JColorChooser.showDialog(editor, "Choose colour", curCol[0]);
                    if (chosen != null) {
                        curCol[0] = chosen; colBtn.setBackground(chosen); colBtn.setForeground(getContrastColor(chosen));
                        api.logging().logToOutput("Annotation color changed.");
                    }
                });
                addToolbarComponent.accept(colBtn);

                // --- Undo Button ---
                JButton undo = new JButton("Undo");
                undo.addActionListener(a -> {
                    if (!shapes.isEmpty()) {
                        int lastIndex = shapes.size() - 1;
                        shapes.remove(lastIndex); cols.remove(lastIndex); kinds.remove(lastIndex);
                        drawCanvas.repaint(); api.logging().logToOutput("Annotation undone.");
                    } else { api.logging().logToOutput("Nothing to undo."); }
                });
                addToolbarComponent.accept(undo);

                bar.add(Box.createVerticalGlue());
                addToolbarComponent.accept(new JSeparator(SwingConstants.HORIZONTAL));
                bar.add(Box.createRigidArea(new Dimension(0, 10)));

                // --- Copy Button (with added logging) ---
                 JButton copyBtn = new JButton("Copy");
                 copyBtn.addActionListener(a -> {
                     api.logging().logToOutput("[Copy Button] Action started."); // <-- Log start
                     api.logging().logToOutput("[Copy Button] Shapes count: " + shapes.size()); // <-- Log data size
                     api.logging().logToOutput("[Copy Button] Original image size: " + snap.getWidth() + "x" + snap.getHeight()); // <-- Log original size
                      try {
                         api.logging().logToOutput("[Copy Button] Calling renderAnnotatedImage..."); // <-- Log before render
                         BufferedImage finalImage = renderAnnotatedImage(snap, shapes, cols, kinds, STROKE_WIDTH);
                         api.logging().logToOutput("[Copy Button] renderAnnotatedImage returned: "
                                 + (finalImage != null ? finalImage.getWidth()+"x"+finalImage.getHeight() : "null")); // <-- Log result

                         // --- Explicit Null Check ---
                         if (finalImage == null) {
                            api.logging().logToError("[Copy Button] Error: Rendered image is null!");
                            JOptionPane.showMessageDialog(editor, "Error copying image:\nRendered image was null.", "Render Error", JOptionPane.ERROR_MESSAGE);
                            return; // Stop processing
                         }
                         // --- End Null Check ---

                         api.logging().logToOutput("[Copy Button] Calling copyImageToClipboard..."); // <-- Log before copy
                         copyImageToClipboard(finalImage); // Use existing utility method
                         api.logging().logToOutput("[Copy Button] copyImageToClipboard finished."); // <-- Log after copy

                         JOptionPane.showMessageDialog(editor, "Annotated image copied to clipboard.", "Copied", JOptionPane.INFORMATION_MESSAGE);
                         editor.dispose(); // Close editor after copy
                     } catch (Exception ex) {
                          // Log the full exception details
                          api.logging().logToError("[Copy Button] Error rendering or copying annotated image: " + ex.toString(), ex);
                         JOptionPane.showMessageDialog(editor, "Error copying image:\n" + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                     } finally {
                         api.logging().logToOutput("[Copy Button] Action finished."); // <-- Log end
                     }
                 });
                 addToolbarComponent.accept(copyBtn);

                // --- Save Button (with added logging) ---
                JButton save = new JButton("Save");
                save.addActionListener(a -> {
                    api.logging().logToOutput("[Save Button] Action started."); // <-- Log start
                    api.logging().logToOutput("[Save Button] Shapes count: " + shapes.size()); // <-- Log data size
                    api.logging().logToOutput("[Save Button] Original image size: " + snap.getWidth() + "x" + snap.getHeight()); // <-- Log original size
                    try {
                        api.logging().logToOutput("[Save Button] Calling renderAnnotatedImage..."); // <-- Log before render
                        BufferedImage finalImage = renderAnnotatedImage(snap, shapes, cols, kinds, STROKE_WIDTH);
                        api.logging().logToOutput("[Save Button] renderAnnotatedImage returned: "
                                + (finalImage != null ? finalImage.getWidth()+"x"+finalImage.getHeight() : "null")); // <-- Log result

                        // --- Explicit Null Check ---
                         if (finalImage == null) {
                            api.logging().logToError("[Save Button] Error: Rendered image is null!");
                            JOptionPane.showMessageDialog(editor, "Error saving image:\nRendered image was null.", "Render Error", JOptionPane.ERROR_MESSAGE);
                            return; // Stop processing
                         }
                         // --- End Null Check ---

                        JFileChooser fc = new JFileChooser();
                        fc.setSelectedFile(new File("annotated_screenshot.png"));
                        fc.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("PNG Images", "png"));

                        api.logging().logToOutput("[Save Button] Showing save dialog..."); // <-- Log before dialog
                        int res = fc.showSaveDialog(editor);
                        api.logging().logToOutput("[Save Button] Save dialog returned: " + (res == JFileChooser.APPROVE_OPTION ? "Approve" : "Cancel/Error")); // <-- Log dialog result

                        if (res == JFileChooser.APPROVE_OPTION) {
                            File f = fc.getSelectedFile();
                             if (!f.getName().toLowerCase().endsWith(".png")) {
                                 f = new File(f.getParentFile(), f.getName() + ".png");
                             }
                            api.logging().logToOutput("[Save Button] Writing image to " + f.getAbsolutePath()); // <-- Log before write
                            ImageIO.write(finalImage, "png", f);
                            api.logging().logToOutput("[Save Button] Image writing finished."); // <-- Log after write

                            JOptionPane.showMessageDialog(editor,
                                    "Saved to:\n" + f.getAbsolutePath(), "Saved", JOptionPane.INFORMATION_MESSAGE);
                            editor.dispose(); // Close editor after saving
                        } else {
                             api.logging().logToOutput("[Save Button] Save cancelled by user.");
                        }
                    } catch (IOException ex) {
                         api.logging().logToError("[Save Button] IO Error saving annotated image: " + ex.toString(), ex);
                         JOptionPane.showMessageDialog(editor, "Error saving image (I/O):\n" + ex.getMessage(), "Save Error", JOptionPane.ERROR_MESSAGE);
                    } catch (Exception ex) {
                         api.logging().logToError("[Save Button] General Error rendering or saving annotated image: " + ex.toString(), ex);
                         JOptionPane.showMessageDialog(editor, "Error preparing/saving image:\n" + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                    } finally {
                         api.logging().logToOutput("[Save Button] Action finished."); // <-- Log end
                    }
                });
                 addToolbarComponent.accept(save);


                // --- Finalize Editor ---
                editor.pack();
                editor.setLocationRelativeTo(null);

                 // Limit initial size and re-center
                 Dimension currentSize = editor.getSize();
                 int maxInitialWidth = Toolkit.getDefaultToolkit().getScreenSize().width - 100;
                 int maxInitialHeight = Toolkit.getDefaultToolkit().getScreenSize().height - 100;
                 editor.setSize(Math.min(currentSize.width, maxInitialWidth),
                                Math.min(currentSize.height, maxInitialHeight));
                 editor.setLocationRelativeTo(null);

                editor.setMinimumSize(new Dimension(350, 300)); // Set minimum size
                drawCanvas.setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
                editor.setVisible(true);
                api.logging().logToOutput("Annotation editor is now visible (with scrolling).");

            } catch (Exception ex) {
                 api.logging().logToError("Failed to launch annotation editor: " + ex.toString(), ex);
            }
        }); // End SwingUtilities.invokeLater
    }
    // --- End of the function ---


    // --- Helper Methods (unchanged) ---
    private Color getColorForMode(Color baseColor, String mode, int highlightAlpha) {
        if ("HIGHLIGHT".equals(mode)) {
            return new Color(baseColor.getRed(), baseColor.getGreen(), baseColor.getBlue(), highlightAlpha);
        } else {
            return baseColor;
        }
    }

    private Color getContrastColor(Color color) {
        double luminance = (0.299 * color.getRed() + 0.587 * color.getGreen() + 0.114 * color.getBlue()) / 255;
        return luminance > 0.5 ? Color.BLACK : Color.WHITE;
    }

    private BufferedImage renderAnnotatedImage(BufferedImage original, List<Shape> shapes, List<Color> cols, List<String> kinds, float strokeWidth) {
         api.logging().logToOutput("Rendering final annotated image...");
         // Use ARGB for transparency support (important for highlights)
         BufferedImage out = new BufferedImage(original.getWidth(), original.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = out.createGraphics();
        g2.drawImage(original, 0, 0, null); // Draw original image first
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setStroke(new BasicStroke(strokeWidth));
        for (int i = 0; i < shapes.size(); i++) {
            g2.setColor(cols.get(i)); // Color already includes alpha for highlights
            Shape s = shapes.get(i); String k = kinds.get(i);
            if ("HIGHLIGHT".equals(k)) {
                g2.fill(s); // Fill for highlight
            } else {
                g2.draw(s); // Draw outline for rect/line
            }
        }
        g2.dispose();
        api.logging().logToOutput("Finished rendering final annotated image.");
        return out;
    }

} // End of ScreenshotUtils class