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
import javax.swing.filechooser.FileNameExtensionFilter; 
public class ScreenshotUtils {
    private final MontoyaApi api;
    private final List<BufferedImage> images = new ArrayList<>();
    public ScreenshotUtils(MontoyaApi api) {
        this.api = api;
    }
    public static Component findComponentUnderMouse(String name, Component parent) {
        try {
            Point location = MouseInfo.getPointerInfo().getLocation();
            if (parent == null)
                return null; 
            SwingUtilities.convertPointFromScreen(location, parent);
            Component deepest = SwingUtilities.getDeepestComponentAt(parent, location.x, location.y);
            if (deepest != null) {
                Component current = deepest;
                while (current != null) {
                    if (name != null && name.equals(current.getName())) {
                        return current;
                    }
                    if (current == parent)
                        break; 
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
        if (component == null)
            return null;
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
        if (parent == null)
            return matchingComponents;
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
        if (container == null)
            return null;
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
    private void clearImages() {
        // api.logging().logToOutput("Clearing image buffer.");
        images.clear();
    }
    private void captureComponentToBuffer(Component component) {
        if (component == null) {
            // api.logging().logToOutput("Cannot capture a null component (for buffer).");
            return;
        }
        try {
            // api.logging().logToOutput("Capturing component screenshot for buffer...");
            int w = component.getWidth();
            int h = component.getHeight();
            if (w <= 0 || h <= 0) {
                // api.logging().logToOutput(String.format("Component size is invalid (width: %d, height: %d). Cannot capture.", w, h));
                return;
            }
            BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = image.createGraphics();
            if (!component.isOpaque() && component.getBackground() != null) {
                g.setColor(component.getBackground());
                g.fillRect(0, 0, w, h);
            }
            component.paint(g); 
            g.dispose();
            images.add(image);
            api.logging().logToOutput(String.format(
                    "Captured component screenshot for buffer (size: %dx%d). Buffer size: %d", w, h, images.size()));
        } catch (Exception e) {
            api.logging().logToError("Error capturing component screenshot to buffer: " + e.toString(), e);
        }
    }
    private void copyImageToClipboard(BufferedImage image) {
        if (image == null) {
            api.logging().logToError("Cannot copy a null image to clipboard."); 
            return;
        }
        try {
            ImageSelection imageSelection = new ImageSelection(image);
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            clipboard.setContents(imageSelection, null);
            // api.logging().logToOutput("Image copied to clipboard.");
        } catch (IllegalStateException e) {
            api.logging().logToError("Error accessing system clipboard (perhaps headless?): " + e.toString(), e);
        } catch (Exception e) {
            api.logging().logToError("Error copying image to clipboard: " + e.toString(), e);
        }
    }
    private BufferedImage combineBufferedImagesHorizontally() {
        // api.logging().logToOutput("Attempting to combine images from buffer...");
        try {
            if (images.isEmpty()) { 
                // api.logging().logToOutput("Buffer is empty. Cannot combine.");
                return null;
            }
            if (images.size() == 1) { 
                // api.logging().logToOutput("Buffer contains only 1 image. Returning it directly.");
                BufferedImage singleImage = images.get(0);
                clearImages(); 
                return singleImage;
            }
            BufferedImage bf1 = images.get(0);
            BufferedImage bf2 = images.get(1);
            if (bf1 == null || bf2 == null) {
                api.logging().logToError("One or both images in buffer are null. Cannot combine."); 
                clearImages(); 
                return null;
            }
            int borderThickness = 5;
            Color borderColor = Color.BLACK;
            int totalWidth = bf1.getWidth() + bf2.getWidth() + borderThickness;
            int maxHeight = Math.max(bf1.getHeight(), bf2.getHeight());
            if (totalWidth <= 0 || maxHeight <= 0) {
                api.logging().logToError(
                        String.format("Combined image size is invalid (width: %d, height: %d). Cannot combine.",
                                totalWidth, maxHeight)); 
                clearImages(); 
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
            api.logging()
                    .logToOutput(String.format("Images combined successfully (size: %dx%d).", totalWidth, maxHeight));
            clearImages(); 
            return combined;
        } catch (IndexOutOfBoundsException e) {
            api.logging().logToError("Index Out Of Bounds error when accessing images buffer: " + e.getMessage(), e);
            clearImages(); 
            return null;
        } catch (Exception e) {
            api.logging().logToError("Error in combineBufferedImagesHorizontally: " + e.toString(), e);
            clearImages(); 
            return null;
        }
    }
    private static class ImageSelection implements Transferable {
        private final Image image;
        public ImageSelection(Image image) {
            this.image = image;
        }
        @Override
        public DataFlavor[] getTransferDataFlavors() {
            return new DataFlavor[] { DataFlavor.imageFlavor };
        }
        @Override
        public boolean isDataFlavorSupported(DataFlavor flavor) {
            return DataFlavor.imageFlavor.equals(flavor);
        }
        @Override
        public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException {
            if (!isDataFlavorSupported(flavor))
                throw new UnsupportedFlavorException(flavor);
            return image;
        }
    }
    public void handleNormalScreenshot() {
        // api.logging().logToOutput("Executing Normal Screenshot (to Annotator)...");
        clearImages();
        try {
            Frame suiteFrame = api.userInterface().swingUtils().suiteFrame();
            if (suiteFrame == null) {
                api.logging().logToError("Could not get Burp Suite main frame.");
                return; 
            }
            Component targetComponent = findComponentUnderMouse("rrvSplitViewerSplitPane", suiteFrame);
            if (targetComponent == null) {
                api.logging()
                        .logToOutput("Could not find 'rrvSplitViewerSplitPane' under mouse. Trying focused component.");
                targetComponent = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
                if (targetComponent == null || !SwingUtilities.isDescendingFrom(targetComponent, suiteFrame)) {
                    api.logging().logToError("No suitable component found under mouse or focused."); 
                    return;
                }
                api.logging().logToOutput("Using focused component: " + targetComponent.getClass().getName()
                        + (targetComponent.getName() != null ? " Name: " + targetComponent.getName() : ""));
            } else {
                // api.logging().logToOutput("Using component under mouse: " + targetComponent.getName());
            }
            int w = targetComponent.getWidth();
            int h = targetComponent.getHeight();
            if (w <= 0 || h <= 0) {
                api.logging().logToError(String
                        .format("Target component size is invalid (width: %d, height: %d). Cannot capture.", w, h)); 
                return;
            }
            BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = image.createGraphics();
            if (!targetComponent.isOpaque() && targetComponent.getBackground() != null) {
                g.setColor(targetComponent.getBackground());
                g.fillRect(0, 0, w, h);
            }
            targetComponent.paint(g); 
            g.dispose();
            // api.logging().logToOutput("Normal screenshot captured. Launching annotator...");
            launchAnnotationEditor(image); 
        } catch (Exception ex) {
            api.logging().logToError("Error in handleNormalScreenshot: " + ex.toString(), ex);
        }
    }
    public void handleFullScreenshot() {
        // api.logging().logToOutput("Executing Full Screenshot (Req/Res to Annotator)...");
        clearImages(); 
        try {
            Frame suiteFrame = api.userInterface().swingUtils().suiteFrame();
            if (suiteFrame == null) {
                api.logging().logToError("Full Screenshot: Could not get Burp Suite main frame.");
                return;
            }
            Component targetComponent = findComponentUnderMouse("rrvSplitViewerSplitPane", suiteFrame);
            if (targetComponent == null) {
                api.logging().logToError(
                        "Full Screenshot: Could not find 'rrvSplitViewerSplitPane' component in the frame.");
                return;
            }
            // api.logging().logToOutput("Full Screenshot: Found target split pane.");
            Component reqComp = findComponentByNameRecursively(targetComponent, "rrvRequestsPane");
            Component resComp = findComponentByNameRecursively(targetComponent, "rrvResponsePane");
            if (reqComp == null || resComp == null) {
                api.logging().logToError(
                        "Full Screenshot: Could not find Request or Response panes within the target split pane.");
                return;
            }
            // api.logging().logToOutput("Full Screenshot: Found request and response panes.");
            Component syntaxTextAreaReq = findComponentByNameRecursively(reqComp, "syntaxTextArea");
            Component syntaxTextAreaRes = findComponentByNameRecursively(resComp, "syntaxTextArea");
            if (syntaxTextAreaReq == null || syntaxTextAreaRes == null) {
                api.logging().logToError(String.format(
                        "Full Screenshot: Could not find syntaxTextArea in Req (%s found) or Res (%s found). Cannot take screenshot.",
                        syntaxTextAreaReq != null, syntaxTextAreaRes != null));
                return;
            }
            // api.logging().logToOutput("Full Screenshot: Found syntaxTextAreas.");
            captureComponentToBuffer(syntaxTextAreaReq);
            captureComponentToBuffer(syntaxTextAreaRes);
            BufferedImage combinedImage = combineBufferedImagesHorizontally();
            if (combinedImage != null) {
                // api.logging().logToOutput("Full Screenshot: Screenshot combined. Launching annotator...");
                launchAnnotationEditor(combinedImage); 
            } else {
                api.logging().logToError("Full Screenshot: Screenshot failed - combined image was null.");
            }
        } catch (Exception ex) {
            api.logging().logToError("Error in handleFullScreenshot: " + ex.toString(), ex);
            clearImages(); 
        }
    }
    private static Component findComponentByNameRecursively(Component parent, String name) {
        if (parent == null || name == null)
            return null;
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
    /**
     * Creates and displays the annotation editor window.
     * Now opens maximized and includes Ctrl+C / Ctrl+S hotkeys.
     * 
     * @param snap The initial BufferedImage captured.
     */
    private void launchAnnotationEditor(final BufferedImage snap) {
        if (snap == null) {
            api.logging().logToError("Cannot launch editor: Snapshot image is null.");
            return;
        }
        // api.logging().logToOutput("Launching annotation editor...");
        SwingUtilities.invokeLater(() -> {
            try {
                final int BAR_W = 140; 
                final JFrame editor = new JFrame("Annotate Screenshot");
                editor.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
                editor.getRootPane().setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY, 2));
                JPanel root = new JPanel(new BorderLayout(5, 5));
                editor.setContentPane(root);
                JLabel imgLabel = new JLabel(new ImageIcon(snap));
                JLayeredPane stack = new JLayeredPane();
                stack.setPreferredSize(new Dimension(snap.getWidth(), snap.getHeight()));
                imgLabel.setBounds(0, 0, snap.getWidth(), snap.getHeight());
                stack.add(imgLabel, JLayeredPane.DEFAULT_LAYER);
                final List<Shape> shapes = new ArrayList<>();
                final List<Color> cols = new ArrayList<>();
                final List<String> kinds = new ArrayList<>();
                final String[] mode = { "RECT" };
                final float STROKE_WIDTH = 3f;
                final int HIGHLIGHT_ALPHA = 200;
                final Color[] curCol = { new Color(255, 0, 0, HIGHLIGHT_ALPHA) }; 
                final Point[] startDrag = { null };
                final Shape[] previewShape = { null };

                final JComponent drawCanvas = new JComponent() {
                    {
                        setOpaque(false);
                    } 
                    @Override
                    protected void paintComponent(Graphics g) {
                        super.paintComponent(g);
                        Graphics2D g2 = (Graphics2D) g.create();
                        try {
                            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                            g2.setStroke(new BasicStroke(STROKE_WIDTH));
                            for (int i = 0; i < shapes.size(); i++) {
                                g2.setColor(cols.get(i)); 
                                Shape s = shapes.get(i);
                                String k = kinds.get(i);
                                if ("HIGHLIGHT".equals(k)) {
                                    g2.setColor(getColorForMode(curCol[0], k, curCol[0].getAlpha()));
                                    g2.fill(s);
                                } else {
                                    g2.draw(s);
                                }
                            }
                            if (previewShape[0] != null && startDrag[0] != null) {
                                if ("HIGHLIGHT".equals(mode[0])) {
                                    g2.setColor(getColorForMode(curCol[0], mode[0], curCol[0].getAlpha()));
                                    g2.fill(previewShape[0]);
                                } else {
                                    g2.setColor(getColorForMode(curCol[0], mode[0], HIGHLIGHT_ALPHA));
                                    g2.draw(previewShape[0]);
                                }
                            }
                        } finally {
                            g2.dispose(); 
                        }
                    }
                    @Override
                    public Dimension getPreferredSize() {
                        return new Dimension(snap.getWidth(), snap.getHeight());
                    }
                };
                drawCanvas.setBounds(0, 0, snap.getWidth(), snap.getHeight()); 
                stack.add(drawCanvas, JLayeredPane.PALETTE_LAYER); 
                MouseAdapter drawListener = new MouseAdapter() {
                    @Override
                    public void mousePressed(MouseEvent e) {
                        startDrag[0] = e.getPoint();
                        previewShape[0] = null;
                        drawCanvas.repaint();
                    }
                    @Override
                    public void mouseDragged(MouseEvent e) {
                        if (startDrag[0] == null)
                            return;
                        Point p = e.getPoint();
                        p.x = Math.max(0, Math.min(p.x, snap.getWidth() - 1));
                        p.y = Math.max(0, Math.min(p.y, snap.getHeight() - 1));
                        if ("LINE".equals(mode[0])) {
                            previewShape[0] = new Line2D.Double(startDrag[0], p);
                        } else { 
                            int x = Math.min(startDrag[0].x, p.x);
                            int y = Math.min(startDrag[0].y, p.y);
                            int w = Math.abs(p.x - startDrag[0].x);
                            int h = Math.abs(p.y - startDrag[0].y);
                            previewShape[0] = new Rectangle2D.Double(x, y, Math.max(w, 1), Math.max(h, 1));
                        }
                        drawCanvas.repaint();
                    }
                    @Override
                    public void mouseReleased(MouseEvent e) {
                        if (startDrag[0] != null && previewShape[0] != null) {
                            boolean hasSize = true;
                            if (previewShape[0] instanceof Rectangle2D) {
                                Rectangle2D r = (Rectangle2D) previewShape[0];
                                hasSize = r.getWidth() > 1 && r.getHeight() > 1; 
                            } else if (previewShape[0] instanceof Line2D) {
                                Line2D l = (Line2D) previewShape[0];
                                hasSize = !l.getP1().equals(l.getP2()); 
                            }
                            if (hasSize) {
                                shapes.add(previewShape[0]);
                                cols.add(getColorForMode(curCol[0], mode[0], curCol[0].getAlpha()));
                                kinds.add(mode[0]);
                            } else {
                                // api.logging().logToOutput("Ignoring zero-size shape draw attempt.");
                            }
                        }
                        startDrag[0] = null;
                        previewShape[0] = null;
                        drawCanvas.repaint();
                    }
                };
                drawCanvas.addMouseListener(drawListener);
                drawCanvas.addMouseMotionListener(drawListener);
                JScrollPane scrollPane = new JScrollPane(stack);
                scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
                scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
                scrollPane.getVerticalScrollBar().setUnitIncrement(16);
                scrollPane.getHorizontalScrollBar().setUnitIncrement(16);
                root.add(scrollPane, BorderLayout.CENTER); 
                JPanel bar = new JPanel();
                bar.setLayout(new BoxLayout(bar, BoxLayout.Y_AXIS));
                bar.setBorder(new EmptyBorder(8, 8, 8, 8));
                bar.setPreferredSize(new Dimension(BAR_W, 100)); 
                root.add(bar, BorderLayout.EAST);
                final Consumer<Component> addToolbarComponent = (comp) -> {
                    if (comp instanceof JComponent) {
                        ((JComponent) comp).setAlignmentX(Component.CENTER_ALIGNMENT);
                        Dimension prefSize = comp.getPreferredSize();
                        comp.setMaximumSize(new Dimension(BAR_W - 16, prefSize.height));
                    }
                    bar.add(comp);
                    bar.add(Box.createRigidArea(new Dimension(0, 5))); 
                };
                ButtonGroup grpShape = new ButtonGroup();
                JToggleButton rBtn = new JToggleButton("Rect (R)", true); 
                JToggleButton lBtn = new JToggleButton("Line (L)");
                JToggleButton hBtn = new JToggleButton("Highlight (H)");
                grpShape.add(rBtn);
                grpShape.add(lBtn);
                grpShape.add(hBtn);
                ActionListener shapeListener = a -> {
                    String oldMode = mode[0];
                    if (a.getSource() == rBtn)
                        mode[0] = "RECT";
                    else if (a.getSource() == lBtn)
                        mode[0] = "LINE";
                    else if (a.getSource() == hBtn)
                        mode[0] = "HIGHLIGHT";
                    if ("LINE".equals(mode[0])) {
                        drawCanvas.setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR)); 
                    } else { 
                        drawCanvas.setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
                    }
                    if (!mode[0].equals(oldMode)) {
                        // api.logging().logToOutput("Annotation mode set to: " + mode[0]);
                    }
                };
                rBtn.addActionListener(shapeListener);
                lBtn.addActionListener(shapeListener);
                hBtn.addActionListener(shapeListener);
                addToolbarComponent.accept(rBtn);
                addToolbarComponent.accept(lBtn);
                addToolbarComponent.accept(hBtn);
                bar.add(Box.createRigidArea(new Dimension(0, 10))); 
                addToolbarComponent.accept(new JSeparator(SwingConstants.HORIZONTAL));
                bar.add(Box.createRigidArea(new Dimension(0, 10))); 
                Action colorAction = new AbstractAction("Colour (X)") {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        Color chosen = JColorChooser.showDialog(editor, "Choose Annotation Colour", curCol[0]);
                        if (chosen != null) {
                            curCol[0] = chosen;
                            // api.logging().logToOutput("Annotation color changed.");
                        }
                    }
                };
                JButton colBtn = new JButton(colorAction);
                colBtn.setBackground(curCol[0]);
                colBtn.setForeground(getContrastColor(curCol[0]));
                colBtn.setOpaque(true);
                colBtn.setBorderPainted(false);
                colBtn.setFocusPainted(false); 
                addToolbarComponent.accept(colBtn);
                Action undoAction = new AbstractAction("Undo (Ctrl+Z)") {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        if (!shapes.isEmpty()) {
                            int lastIndex = shapes.size() - 1;
                            shapes.remove(lastIndex);
                            cols.remove(lastIndex);
                            kinds.remove(lastIndex);
                            drawCanvas.repaint();
                            // api.logging().logToOutput("Annotation undone.");
                        } else {
                            // api.logging().logToOutput("Nothing to undo.");
                        }
                    }
                };
                JButton undoBtn = new JButton(undoAction); 
                addToolbarComponent.accept(undoBtn);
                bar.add(Box.createVerticalGlue()); 
                addToolbarComponent.accept(new JSeparator(SwingConstants.HORIZONTAL));
                bar.add(Box.createRigidArea(new Dimension(0, 10))); 
                Action copyAction = new AbstractAction("Copy (Ctrl+C)") {
                    @Override
                    public void actionPerformed(ActionEvent a) {
                        // api.logging().logToOutput("[Copy Action] Action started via " + a.getActionCommand()); 
                        // api.logging().logToOutput("[Copy Action] Shapes count: " + shapes.size());
                        api.logging().logToOutput(
                                "[Copy Action] Original image size: " + snap.getWidth() + "x" + snap.getHeight());
                        try {
                            // api.logging().logToOutput("[Copy Action] Calling renderAnnotatedImage...");
                            BufferedImage finalImage = renderAnnotatedImage(snap, shapes, cols, kinds, STROKE_WIDTH);
                            api.logging().logToOutput("[Copy Action] renderAnnotatedImage returned: "
                                    + (finalImage != null ? finalImage.getWidth() + "x" + finalImage.getHeight()
                                            : "null"));
                            if (finalImage == null) {
                                api.logging().logToError("[Copy Action] Error: Rendered image is null!");
                                JOptionPane.showMessageDialog(editor, "Error copying image:\nRendered image was null.",
                                        "Render Error", JOptionPane.ERROR_MESSAGE);
                                return; 
                            }
                            // api.logging().logToOutput("[Copy Action] Calling copyImageToClipboard...");
                            copyImageToClipboard(finalImage); 
                            // api.logging().logToOutput("[Copy Action] copyImageToClipboard finished.");
                            editor.dispose(); 
                        } catch (Exception ex) {
                            api.logging().logToError(
                                    "[Copy Action] Error rendering or copying annotated image: " + ex.toString(), ex);
                            JOptionPane.showMessageDialog(editor, "Error copying image:\n" + ex.getMessage(), "Error",
                                    JOptionPane.ERROR_MESSAGE);
                        } finally {
                            // api.logging().logToOutput("[Copy Action] Action finished.");
                        }
                    }
                };
                Action saveAction = new AbstractAction("Save (Ctrl+S)") {
                    @Override
                    public void actionPerformed(ActionEvent a) {
                        // api.logging().logToOutput("[Save Action] Action started via " + a.getActionCommand()); 
                        // api.logging().logToOutput("[Save Action] Shapes count: " + shapes.size());
                        api.logging().logToOutput(
                                "[Save Action] Original image size: " + snap.getWidth() + "x" + snap.getHeight());
                        try {
                            // api.logging().logToOutput("[Save Action] Calling renderAnnotatedImage...");
                            BufferedImage finalImage = renderAnnotatedImage(snap, shapes, cols, kinds, STROKE_WIDTH);
                            api.logging().logToOutput("[Save Action] renderAnnotatedImage returned: "
                                    + (finalImage != null ? finalImage.getWidth() + "x" + finalImage.getHeight()
                                            : "null"));
                            if (finalImage == null) {
                                api.logging().logToError("[Save Action] Error: Rendered image is null!");
                                JOptionPane.showMessageDialog(editor, "Error saving image:\nRendered image was null.",
                                        "Render Error", JOptionPane.ERROR_MESSAGE);
                                return; 
                            }
                            JFileChooser fc = new JFileChooser();
                            fc.setDialogTitle("Save Annotated Screenshot");
                            fc.setSelectedFile(new File("annotated_screenshot.png"));
                            fc.setFileFilter(new FileNameExtensionFilter("PNG Images (*.png)", "png"));
                            fc.setAcceptAllFileFilterUsed(false); 
                            // api.logging().logToOutput("[Save Action] Showing save dialog...");
                            int res = fc.showSaveDialog(editor);
                            api.logging().logToOutput("[Save Action] Save dialog returned: "
                                    + (res == JFileChooser.APPROVE_OPTION ? "Approve" : "Cancel/Error"));
                            if (res == JFileChooser.APPROVE_OPTION) {
                                File f = fc.getSelectedFile();
                                if (!f.getName().toLowerCase().endsWith(".png")) {
                                    f = new File(f.getParentFile(), f.getName() + ".png");
                                }
                                if (f.exists()) {
                                    int overwriteRes = JOptionPane.showConfirmDialog(editor,
                                            "File already exists:\n" + f.getName() + "\nOverwrite?",
                                            "Confirm Overwrite", JOptionPane.YES_NO_OPTION);
                                    if (overwriteRes != JOptionPane.YES_OPTION) {
                                        api.logging().logToOutput(
                                                "[Save Action] Save cancelled by user (overwrite denied).");
                                        return; 
                                    }
                                }
                                // api.logging().logToOutput("[Save Action] Writing image to " + f.getAbsolutePath());
                                boolean success = ImageIO.write(finalImage, "png", f);
                                // api.logging().logToOutput("[Save Action] Image writing finished. Success: " + success);
                                if (success) {
                                    JOptionPane.showMessageDialog(editor,
                                            "Saved to:\n" + f.getAbsolutePath(), "Saved",
                                            JOptionPane.INFORMATION_MESSAGE);
                                    editor.dispose(); 
                                } else {
                                    api.logging().logToError("[Save Action] ImageIO.write returned false.");
                                    JOptionPane.showMessageDialog(editor,
                                            "Error saving image (write operation failed).\nCheck file permissions or disk space.",
                                            "Save Error", JOptionPane.ERROR_MESSAGE);
                                }
                            } else {
                                // api.logging().logToOutput("[Save Action] Save cancelled by user.");
                            }
                        } catch (IOException ex) {
                            api.logging().logToError("[Save Action] IO Error saving annotated image: " + ex.toString(),
                                    ex);
                            JOptionPane.showMessageDialog(editor, "Error saving image (I/O):\n" + ex.getMessage(),
                                    "Save Error", JOptionPane.ERROR_MESSAGE);
                        } catch (Exception ex) {
                            api.logging().logToError(
                                    "[Save Action] General Error rendering or saving annotated image: " + ex.toString(),
                                    ex);
                            JOptionPane.showMessageDialog(editor, "Error preparing/saving image:\n" + ex.getMessage(),
                                    "Error", JOptionPane.ERROR_MESSAGE);
                        } finally {
                            // api.logging().logToOutput("[Save Action] Action finished.");
                        }
                    }
                };
                JButton copyBtn = new JButton(copyAction);
                JButton saveBtn = new JButton(saveAction); 
                addToolbarComponent.accept(copyBtn);
                addToolbarComponent.accept(saveBtn);
                JRootPane rootPane = editor.getRootPane();
                InputMap inputMap = rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
                ActionMap actionMap = rootPane.getActionMap();
                KeyStroke copyKeyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_C,
                        Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx());
                inputMap.put(copyKeyStroke, "copyActionMapKey"); 
                actionMap.put("copyActionMapKey", copyAction); 
                KeyStroke saveKeyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_S,
                        Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx());
                inputMap.put(saveKeyStroke, "saveActionMapKey");
                actionMap.put("saveActionMapKey", saveAction);
                KeyStroke undoKeyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_Z,
                        Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx());
                inputMap.put(undoKeyStroke, "undoActionMapKey");
                actionMap.put("undoActionMapKey", undoAction); 
                inputMap.put(KeyStroke.getKeyStroke("X"), "openActionMapKey");
                actionMap.put("openActionMapKey", colorAction); 
                // Rect, Line, Highlight hotkeys
                inputMap.put(KeyStroke.getKeyStroke("R"), "rectActionMapKey");
                actionMap.put("rectActionMapKey", new AbstractAction() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        rBtn.doClick(); 
                    }
                });
                inputMap.put(KeyStroke.getKeyStroke("L"), "lineActionMapKey");
                actionMap.put("lineActionMapKey", new AbstractAction() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        lBtn.doClick(); 
                    }
                });
                inputMap.put(KeyStroke.getKeyStroke("H"), "highlightActionMapKey");
                actionMap.put("highlightActionMapKey", new AbstractAction() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        hBtn.doClick(); 
                    }
                });
                editor.pack(); 
                editor.setMinimumSize(new Dimension(500, 400)); 
                editor.setExtendedState(JFrame.MAXIMIZED_BOTH);
                editor.setLocationRelativeTo(null);
                drawCanvas.setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
                rBtn.setSelected(true); 
                editor.setVisible(true);
                // api.logging().logToOutput("Annotation editor is now visible (maximized, with scrolling and hotkeys).");
                drawCanvas.requestFocusInWindow();
            } catch (Exception ex) {
                api.logging().logToError("Failed to launch annotation editor: " + ex.toString(), ex);
                JOptionPane.showMessageDialog(null, 
                        "Could not launch the annotation editor:\n" + ex.getMessage(),
                        "Editor Launch Error", JOptionPane.ERROR_MESSAGE);
            }
        }); 
    }
    private Color getColorForMode(Color baseColor, String mode, int highlightAlpha) {
        if ("HIGHLIGHT".equals(mode)) {
            int alpha = Math.min(255, highlightAlpha);
            return new Color(baseColor.getRed(), baseColor.getGreen(), baseColor.getBlue(), alpha);
        } else {
            return baseColor;
        }
    }
    private Color getContrastColor(Color color) {
        double luminance = (0.299 * color.getRed() + 0.587 * color.getGreen() + 0.114 * color.getBlue()) / 255.0;
        return luminance > 0.5 ? Color.BLACK : Color.WHITE;
    }
    private BufferedImage renderAnnotatedImage(BufferedImage original, List<Shape> shapes, List<Color> cols,
            List<String> kinds, float strokeWidth) {
        if (original == null) {
            api.logging().logToError("renderAnnotatedImage: Original image is null.");
            return null;
        }
        // api.logging().logToOutput("Rendering final annotated image...");
        int w = original.getWidth();
        int h = original.getHeight();
        if (w <= 0 || h <= 0) {
            api.logging()
                    .logToError("renderAnnotatedImage: Original image has invalid dimensions (" + w + "x" + h + ")");
            return null;
        }
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = out.createGraphics();
        try {
            g2.drawImage(original, 0, 0, null);
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setStroke(new BasicStroke(strokeWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)); 
            for (int i = 0; i < shapes.size(); i++) {
                Shape s = shapes.get(i);
                Color c = cols.get(i); 
                String k = kinds.get(i);
                if (s == null || c == null || k == null) {
                    // api.logging().logToOutput("Skipping null shape/color/kind at index " + i);
                    continue;
                }
                g2.setColor(c); 
                if ("HIGHLIGHT".equals(k)) {
                    g2.setColor(getColorForMode(c, k, c.getAlpha())); 
                    g2.fill(s); 
                } else { 
                    g2.draw(s); 
                }
            }
            // api.logging().logToOutput("Finished rendering annotations.");
        } catch (Exception e) {
            api.logging().logToError("Error during rendering annotations: " + e.toString(), e);
        } finally {
            g2.dispose(); 
        }
        return out;
    }
}