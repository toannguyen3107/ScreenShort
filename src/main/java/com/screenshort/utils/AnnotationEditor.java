package com.screenshort.utils;

import burp.api.montoya.MontoyaApi;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Annotation editor window for marking up screenshots.
 * Supports drawing rectangles, lines, and highlights with customizable colors.
 */
public class AnnotationEditor {

    private final MontoyaApi api;
    private final BufferedImage originalImage;
    private final AnnotationState state;

    public AnnotationEditor(MontoyaApi api, BufferedImage image) {
        this.api = api;
        this.originalImage = image;
        this.state = new AnnotationState();
    }

    /**
     * Launches the annotation editor window.
     */
    public void launch() {
        if (originalImage == null) {
            api.logging().logToError("Cannot launch editor: Snapshot image is null.");
            return;
        }

        SwingUtilities.invokeLater(this::createAndShowEditor);
    }

    private void createAndShowEditor() {
        try {
            JFrame editor = new JFrame("Annotate Screenshot");
            editor.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            editor.getRootPane().setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY, 2));

            JPanel root = new JPanel(new BorderLayout(5, 5));
            editor.setContentPane(root);

            // Create image display with layered drawing canvas
            JLayeredPane stack = createLayeredPane();
            JScrollPane scrollPane = createScrollPane(stack);
            root.add(scrollPane, BorderLayout.CENTER);

            // Create toolbar
            JPanel toolbar = createToolbar(editor);
            root.add(toolbar, BorderLayout.EAST);

            // Setup keyboard shortcuts
            setupKeyboardShortcuts(editor);

            // Configure and show window
            editor.pack();
            editor.setMinimumSize(new Dimension(Constants.EDITOR_MIN_WIDTH, Constants.EDITOR_MIN_HEIGHT));
            editor.setExtendedState(JFrame.MAXIMIZED_BOTH);
            editor.setLocationRelativeTo(null);
            editor.setVisible(true);

            state.drawCanvas.requestFocusInWindow();
        } catch (Exception ex) {
            api.logging().logToError("Failed to launch annotation editor: " + ex.toString(), ex);
            JOptionPane.showMessageDialog(null,
                    "Could not launch the annotation editor:\n" + ex.getMessage(),
                    "Editor Launch Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private JLayeredPane createLayeredPane() {
        JLabel imgLabel = new JLabel(new ImageIcon(originalImage));
        JLayeredPane stack = new JLayeredPane();
        stack.setPreferredSize(new Dimension(originalImage.getWidth(), originalImage.getHeight()));

        imgLabel.setBounds(0, 0, originalImage.getWidth(), originalImage.getHeight());
        stack.add(imgLabel, JLayeredPane.DEFAULT_LAYER);

        JComponent drawCanvas = createDrawingCanvas();
        drawCanvas.setBounds(0, 0, originalImage.getWidth(), originalImage.getHeight());
        stack.add(drawCanvas, JLayeredPane.PALETTE_LAYER);

        state.drawCanvas = drawCanvas;
        return stack;
    }

    private JScrollPane createScrollPane(JLayeredPane stack) {
        JScrollPane scrollPane = new JScrollPane(stack);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.getHorizontalScrollBar().setUnitIncrement(16);
        return scrollPane;
    }

    private JComponent createDrawingCanvas() {
        JComponent canvas = new JComponent() {
            {
                setOpaque(false);
            }

            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                try {
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setStroke(new BasicStroke(Constants.STROKE_WIDTH));

                    // Draw completed shapes
                    drawShapes(g2, state.shapes, state.colors, state.kinds);

                    // Draw preview shape
                    if (state.previewShape != null && state.startDrag != null) {
                        drawPreviewShape(g2);
                    }
                } finally {
                    g2.dispose();
                }
            }

            @Override
            public Dimension getPreferredSize() {
                return new Dimension(originalImage.getWidth(), originalImage.getHeight());
            }
        };

        MouseAdapter drawListener = createDrawListener(canvas);
        canvas.addMouseListener(drawListener);
        canvas.addMouseMotionListener(drawListener);
        canvas.setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));

        return canvas;
    }

    private void drawShapes(Graphics2D g2, List<Shape> shapes, List<Color> colors, List<String> kinds) {
        for (int i = 0; i < shapes.size(); i++) {
            Shape s = shapes.get(i);
            Color c = colors.get(i);
            String k = kinds.get(i);

            if (Constants.MODE_HIGHLIGHT.equals(k)) {
                g2.setColor(applyAlpha(c, c.getAlpha()));
                g2.fill(s);
            } else {
                g2.setColor(c);
                g2.draw(s);
            }
        }
    }

    private void drawPreviewShape(Graphics2D g2) {
        if (Constants.MODE_HIGHLIGHT.equals(state.mode)) {
            g2.setColor(applyAlpha(state.currentColor, state.currentColor.getAlpha()));
            g2.fill(state.previewShape);
        } else {
            g2.setColor(applyAlpha(state.currentColor, Constants.HIGHLIGHT_ALPHA));
            g2.draw(state.previewShape);
        }
    }

    private MouseAdapter createDrawListener(JComponent canvas) {
        return new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                state.startDrag = e.getPoint();
                state.previewShape = null;
                canvas.repaint();
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                if (state.startDrag == null) return;

                Point p = constrainPoint(e.getPoint());
                state.previewShape = createShape(state.startDrag, p);
                canvas.repaint();
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (state.startDrag != null && state.previewShape != null && hasSize(state.previewShape)) {
                    state.shapes.add(state.previewShape);
                    state.colors.add(applyAlpha(state.currentColor, state.currentColor.getAlpha()));
                    state.kinds.add(state.mode);
                }
                state.startDrag = null;
                state.previewShape = null;
                canvas.repaint();
            }
        };
    }

    private Point constrainPoint(Point p) {
        p.x = Math.max(0, Math.min(p.x, originalImage.getWidth() - 1));
        p.y = Math.max(0, Math.min(p.y, originalImage.getHeight() - 1));
        return p;
    }

    private Shape createShape(Point start, Point end) {
        if (Constants.MODE_LINE.equals(state.mode)) {
            return new Line2D.Double(start, end);
        } else {
            int x = Math.min(start.x, end.x);
            int y = Math.min(start.y, end.y);
            int w = Math.abs(end.x - start.x);
            int h = Math.abs(end.y - start.y);
            return new Rectangle2D.Double(x, y, Math.max(w, 1), Math.max(h, 1));
        }
    }

    private boolean hasSize(Shape shape) {
        if (shape instanceof Rectangle2D) {
            Rectangle2D r = (Rectangle2D) shape;
            return r.getWidth() > 1 && r.getHeight() > 1;
        } else if (shape instanceof Line2D) {
            Line2D l = (Line2D) shape;
            return !l.getP1().equals(l.getP2());
        }
        return true;
    }

    private JPanel createToolbar(JFrame editor) {
        JPanel bar = new JPanel();
        bar.setLayout(new BoxLayout(bar, BoxLayout.Y_AXIS));
        bar.setBorder(new EmptyBorder(8, 8, 8, 8));
        bar.setPreferredSize(new Dimension(Constants.TOOLBAR_WIDTH, 100));

        // Shape buttons
        ButtonGroup shapeGroup = new ButtonGroup();
        JToggleButton rectBtn = new JToggleButton("Rect (R)", true);
        JToggleButton lineBtn = new JToggleButton("Line (L)");
        JToggleButton highlightBtn = new JToggleButton("Highlight (H)");

        shapeGroup.add(rectBtn);
        shapeGroup.add(lineBtn);
        shapeGroup.add(highlightBtn);

        rectBtn.addActionListener(e -> state.mode = Constants.MODE_RECT);
        lineBtn.addActionListener(e -> state.mode = Constants.MODE_LINE);
        highlightBtn.addActionListener(e -> state.mode = Constants.MODE_HIGHLIGHT);

        addToolbarComponent(bar, rectBtn);
        addToolbarComponent(bar, lineBtn);
        addToolbarComponent(bar, highlightBtn);

        bar.add(Box.createRigidArea(new Dimension(0, 10)));
        addToolbarComponent(bar, new JSeparator(SwingConstants.HORIZONTAL));
        bar.add(Box.createRigidArea(new Dimension(0, 10)));

        // Color button
        JButton colorBtn = new JButton("Colour (X)");
        colorBtn.setBackground(state.currentColor);
        colorBtn.setForeground(getContrastColor(state.currentColor));
        colorBtn.setOpaque(true);
        colorBtn.setBorderPainted(false);
        colorBtn.addActionListener(e -> {
            Color chosen = JColorChooser.showDialog(editor, "Choose Annotation Colour", state.currentColor);
            if (chosen != null) {
                state.currentColor = chosen;
                colorBtn.setBackground(chosen);
                colorBtn.setForeground(getContrastColor(chosen));
            }
        });
        addToolbarComponent(bar, colorBtn);

        // Undo button
        JButton undoBtn = new JButton("Undo (Ctrl+Z)");
        undoBtn.addActionListener(e -> {
            if (!state.shapes.isEmpty()) {
                int lastIndex = state.shapes.size() - 1;
                state.shapes.remove(lastIndex);
                state.colors.remove(lastIndex);
                state.kinds.remove(lastIndex);
                state.drawCanvas.repaint();
            }
        });
        addToolbarComponent(bar, undoBtn);

        bar.add(Box.createVerticalGlue());
        addToolbarComponent(bar, new JSeparator(SwingConstants.HORIZONTAL));
        bar.add(Box.createRigidArea(new Dimension(0, 10)));

        // Copy button
        JButton copyBtn = new JButton("Copy (Ctrl+C)");
        copyBtn.addActionListener(e -> copyAnnotatedImage(editor));
        addToolbarComponent(bar, copyBtn);

        // Save button
        JButton saveBtn = new JButton("Save (Ctrl+S)");
        saveBtn.addActionListener(e -> saveAnnotatedImage(editor));
        addToolbarComponent(bar, saveBtn);

        // Store references for keyboard shortcuts
        state.rectButton = rectBtn;
        state.lineButton = lineBtn;
        state.highlightButton = highlightBtn;
        state.colorButton = colorBtn;
        state.undoButton = undoBtn;

        return bar;
    }

    private void addToolbarComponent(JPanel bar, Component comp) {
        if (comp instanceof JComponent) {
            ((JComponent) comp).setAlignmentX(Component.CENTER_ALIGNMENT);
            Dimension prefSize = comp.getPreferredSize();
            comp.setMaximumSize(new Dimension(Constants.TOOLBAR_WIDTH - 16, prefSize.height));
        }
        bar.add(comp);
        bar.add(Box.createRigidArea(new Dimension(0, 5)));
    }

    private void setupKeyboardShortcuts(JFrame editor) {
        JRootPane rootPane = editor.getRootPane();
        InputMap inputMap = rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap actionMap = rootPane.getActionMap();

        int shortcutKey = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();

        // Copy: Ctrl+C
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_C, shortcutKey), "copy");
        actionMap.put("copy", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                copyAnnotatedImage(editor);
            }
        });

        // Save: Ctrl+S
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_S, shortcutKey), "save");
        actionMap.put("save", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                saveAnnotatedImage(editor);
            }
        });

        // Undo: Ctrl+Z
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_Z, shortcutKey), "undo");
        actionMap.put("undo", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                state.undoButton.doClick();
            }
        });

        // Color: X
        inputMap.put(KeyStroke.getKeyStroke("X"), "color");
        actionMap.put("color", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                state.colorButton.doClick();
            }
        });

        // Shape shortcuts
        inputMap.put(KeyStroke.getKeyStroke("R"), "rect");
        actionMap.put("rect", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                state.rectButton.doClick();
            }
        });

        inputMap.put(KeyStroke.getKeyStroke("L"), "line");
        actionMap.put("line", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                state.lineButton.doClick();
            }
        });

        inputMap.put(KeyStroke.getKeyStroke("H"), "highlight");
        actionMap.put("highlight", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                state.highlightButton.doClick();
            }
        });
    }

    private void copyAnnotatedImage(JFrame editor) {
        try {
            BufferedImage finalImage = renderAnnotatedImage();
            if (finalImage == null) {
                api.logging().logToError("[Copy Action] Error: Rendered image is null!");
                JOptionPane.showMessageDialog(editor, "Error copying image:\nRendered image was null.",
                        "Render Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            api.logging().logToOutput("[Copy Action] Original image size: " + originalImage.getWidth() + "x" + originalImage.getHeight());

            if (ClipboardUtils.copyImageToClipboard(finalImage)) {
                editor.dispose();
            } else {
                api.logging().logToError("[Copy Action] Failed to copy image to clipboard");
            }
        } catch (Exception ex) {
            api.logging().logToError("[Copy Action] Error rendering or copying annotated image: " + ex.toString(), ex);
            JOptionPane.showMessageDialog(editor, "Error copying image:\n" + ex.getMessage(), "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void saveAnnotatedImage(JFrame editor) {
        try {
            BufferedImage finalImage = renderAnnotatedImage();
            if (finalImage == null) {
                api.logging().logToError("[Save Action] Error: Rendered image is null!");
                JOptionPane.showMessageDialog(editor, "Error saving image:\nRendered image was null.",
                        "Render Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            api.logging().logToOutput("[Save Action] Original image size: " + originalImage.getWidth() + "x" + originalImage.getHeight());

            JFileChooser fc = new JFileChooser();
            fc.setDialogTitle("Save Annotated Screenshot");
            fc.setSelectedFile(new File("annotated_screenshot.png"));
            fc.setFileFilter(new FileNameExtensionFilter("PNG Images (*.png)", "png"));
            fc.setAcceptAllFileFilterUsed(false);

            int res = fc.showSaveDialog(editor);
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
                        return;
                    }
                }

                boolean success = ImageIO.write(finalImage, "png", f);
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
            }
        } catch (IOException ex) {
            api.logging().logToError("[Save Action] IO Error saving annotated image: " + ex.toString(), ex);
            JOptionPane.showMessageDialog(editor, "Error saving image (I/O):\n" + ex.getMessage(),
                    "Save Error", JOptionPane.ERROR_MESSAGE);
        } catch (Exception ex) {
            api.logging().logToError("[Save Action] General Error rendering or saving annotated image: " + ex.toString(), ex);
            JOptionPane.showMessageDialog(editor, "Error preparing/saving image:\n" + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Renders the original image with all annotations applied.
     */
    public BufferedImage renderAnnotatedImage() {
        if (originalImage == null) {
            api.logging().logToError("renderAnnotatedImage: Original image is null.");
            return null;
        }

        int w = originalImage.getWidth();
        int h = originalImage.getHeight();
        if (w <= 0 || h <= 0) {
            api.logging().logToError("renderAnnotatedImage: Original image has invalid dimensions (" + w + "x" + h + ")");
            return null;
        }

        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = out.createGraphics();
        try {
            g2.drawImage(originalImage, 0, 0, null);
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setStroke(new BasicStroke(Constants.STROKE_WIDTH, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

            for (int i = 0; i < state.shapes.size(); i++) {
                Shape s = state.shapes.get(i);
                Color c = state.colors.get(i);
                String k = state.kinds.get(i);

                if (s == null || c == null || k == null) {
                    continue;
                }

                g2.setColor(c);
                if (Constants.MODE_HIGHLIGHT.equals(k)) {
                    g2.setColor(applyAlpha(c, c.getAlpha()));
                    g2.fill(s);
                } else {
                    g2.draw(s);
                }
            }
        } catch (Exception e) {
            api.logging().logToError("Error during rendering annotations: " + e.toString(), e);
        } finally {
            g2.dispose();
        }
        return out;
    }

    private Color applyAlpha(Color baseColor, int alpha) {
        return new Color(baseColor.getRed(), baseColor.getGreen(), baseColor.getBlue(), Math.min(255, alpha));
    }

    private Color getContrastColor(Color color) {
        double luminance = (0.299 * color.getRed() + 0.587 * color.getGreen() + 0.114 * color.getBlue()) / 255.0;
        return luminance > 0.5 ? Color.BLACK : Color.WHITE;
    }

    /**
     * Encapsulates the mutable state of the annotation editor.
     */
    private static class AnnotationState {
        String mode = Constants.MODE_RECT;
        Color currentColor = Constants.DEFAULT_ANNOTATION_COLOR;
        Point startDrag = null;
        Shape previewShape = null;

        final List<Shape> shapes = new ArrayList<>();
        final List<Color> colors = new ArrayList<>();
        final List<String> kinds = new ArrayList<>();

        JComponent drawCanvas;
        JToggleButton rectButton;
        JToggleButton lineButton;
        JToggleButton highlightButton;
        JButton colorButton;
        JButton undoButton;
    }
}
