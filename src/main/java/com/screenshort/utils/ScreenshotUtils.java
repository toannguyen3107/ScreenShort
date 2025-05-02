package com.screenshort.utils;

import burp.api.montoya.MontoyaApi;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JRootPane;
import javax.swing.SwingUtilities; // SwingUtilities cho tìm component
import java.io.IOException; // Import cho IOException trong Transferable

// Gộp Component Finder và logic chụp/xử lý ảnh từ Action
public class ScreenshotUtils {

    private final MontoyaApi api;
    private final List<BufferedImage> images = new ArrayList<>(); // Buffer ảnh ngay trong ScreenshotUtils

    // Constructor chỉ nhận MontoyaApi
    public ScreenshotUtils(MontoyaApi api) {
        this.api = api;
    }

    // --- Static Helper Methods (Component Finding) ---
    // Giữ các phương thức tìm component là static
    public static Component findComponentUnderMouse(String name, Component parent) {
         try {
             Point location = MouseInfo.getPointerInfo().getLocation();
             if (parent == null) return null; // Handle null parent
             SwingUtilities.convertPointFromScreen(location, parent);
             Component deepest = SwingUtilities.getDeepestComponentAt(parent, location.x, location.y);
             if (deepest != null) {
                  return SwingUtilities.getAncestorNamed(name, deepest);
             }
             return null; // Handle case where deepest component is null
        } catch (Exception e) {
            // Catch exceptions like HeadlessException if run in a non-graphics environment
            System.err.println("Error in findComponentUnderMouse: " + e.getMessage());
            return null;
        }
    }

    public static Component getComponentByName(Component component, String name) {
        if (component == null) return null; // Add null check

        if (name != null && name.equals(component.getName())) { // Add null check for name
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
         if (parent == null) return matchingComponents; // Add null check

         // Check parent itself
         if (name != null && name.equals(parent.getName())) { // Add null check for name
            matchingComponents.add(parent);
         }

        // Recursively check children
        if (parent instanceof Container) {
            for (Component child : ((Container) parent).getComponents()) {
                matchingComponents.addAll(findAllComponentsByName(child, name));
            }
        }
        return matchingComponents;
    }

    // Hàm tìm JRootPane (nếu còn cần) - Vẫn static
    public static JRootPane findJRootPane(Container container) { // Đổi tên hàm cho đúng
         if (container == null) return null; // Add null check
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

    // --- Image Handling Methods (Formerly in Action) ---

    /**
     * Xóa sạch buffer ảnh đã lưu.
     */
    private void clearImages(){ // private vì chỉ dùng nội bộ lớp này
        api.logging().logToOutput("Clearing image buffer.");
        images.clear();
    }

    /**
     * Chụp ảnh một Component và lưu vào buffer ảnh.
     * @param component Component cần chụp.
     */
    private void captureComponentToBuffer(Component component) { // private vì chỉ dùng nội bộ
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

            // Paint component onto the buffered image
            component.paint(g);
            g.dispose(); // Release graphics resources

            images.add(image);
            api.logging().logToOutput(String.format("Captured component screenshot for buffer (size: %dx%d). Buffer size: %d", w, h, images.size()));

        } catch (Exception e) {
            api.logging().logToError("Error capturing component screenshot to buffer: " + e.toString());
        }
    }

     /**
     * Copy BufferedImage vào clipboard của hệ thống.
     * @param image Ảnh cần copy.
     */
     private void copyImageToClipboard(BufferedImage image) { // private vì chỉ dùng nội bộ
         if (image == null) {
             api.logging().logToOutput("Cannot copy a null image to clipboard.");
             return;
         }
          try {
             ImageSelection imageSelection = new ImageSelection(image); // Sử dụng helper class
             Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
             clipboard.setContents(imageSelection, null);
             api.logging().logToOutput("Image copied to clipboard.");
          } catch (Exception e) {
             api.logging().logToError("Error copying image to clipboard: " + e.toString());
          }
     }


    /**
     * Kết hợp các ảnh đã lưu trong buffer (hiện tại giả định 2 ảnh: req và res)
     * theo chiều ngang.
     * @return Ảnh đã kết hợp hoặc null nếu buffer không đủ ảnh.
     */
    private BufferedImage combineBufferedImagesHorizontally() { // private vì chỉ dùng nội bộ
        api.logging().logToOutput("Attempting to combine images from buffer...");
        try {
            if (images.size() < 2) {
                api.logging().logToOutput("Buffer contains less than 2 images. Cannot combine.");
                return null; // Không làm gì nếu không có đủ ảnh
            }

            BufferedImage bf1 = images.get(0); // Request part (giả định)
            BufferedImage bf2 = images.get(1); // Response part (giả định)

            if (bf1 == null || bf2 == null) {
                api.logging().logToOutput("One or both images in buffer are null. Cannot combine.");
                return null;
            }

            int borderThickness = 5; // Width of the border in pixels
            Color borderColor = Color.BLACK; // Border color

            // Calculate dimensions of the combined image
            int width = bf1.getWidth() + bf2.getWidth() + borderThickness;
            int height = Math.max(bf1.getHeight(), bf2.getHeight());

             if (width <= 0 || height <= 0) {
                api.logging().logToOutput(String.format("Combined image size is invalid (width: %d, height: %d). Cannot combine.", width, height));
                return null;
             }

            // Create a new image with a white background
            BufferedImage combined = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            Graphics g = combined.getGraphics();

            // Fill the background with white
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, width, height);

            // Draw the first image
            g.drawImage(bf1, 0, 0, null);

            // Draw the border
            g.setColor(borderColor);
            g.fillRect(bf1.getWidth(), 0, borderThickness, height);

            // Draw the second image
            g.drawImage(bf2, bf1.getWidth() + borderThickness, 0, null);

            // Release resources
            g.dispose();
            api.logging().logToOutput(String.format("Images combined successfully (size: %dx%d).", width, height));
            return combined;
        } catch (IndexOutOfBoundsException e) {
            // Should ideally be caught by images.size() check, but keep just in case
            api.logging().logToError("Index Out Of Bounds error when accessing images buffer: " + e.getMessage());
            return null; // Trả về null nếu có lỗi
        }
        catch (Exception e) {
            api.logging().logToError("Error in combineBufferedImagesHorizontally: " + e.toString());
            return null; // Trả về null nếu có lỗi khác
        } finally {
            // Luôn xóa buffer sau khi cố gắng combine, dù thành công hay thất bại
             clearImages(); // Hoặc cân nhắc không clear ở đây nếu logic gọi khác. Nhưng cho trường hợp hiện tại, clear là hợp lý.
        }
    }

    // Helper method để copy BufferedImage vào Clipboard (Cần class này)
    private static class ImageSelection implements Transferable {
        private Image image;

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
            if (!DataFlavor.imageFlavor.equals(flavor)) {
                throw new UnsupportedFlavorException(flavor);
            }
             // Return a copy to protect against modification outside the transfer process? Often not necessary.
            return image;
        }
    }


    // --- Public Methods Called by MenuActionHandler ---

    /**
     * Xử lý chức năng "Normal Screenshot": tìm component dưới chuột và copy ảnh
     * của component đó ra clipboard ngay lập tức.
     */
    public void handleNormalScreenshot() {
        api.logging().logToOutput("Executing Normal Screenshot...");
        try {
            Frame suiteFrame = api.userInterface().swingUtils().suiteFrame();
            Component targetComponent = findComponentUnderMouse("rrvSplitViewerSplitPane", suiteFrame); // Sử dụng static helper

            if (targetComponent != null) {
                api.logging().logToOutput("Found target component 'rrvSplitViewerSplitPane'. Taking screenshot.");
                // Trực tiếp chụp và copy component này, không cần buffer.
                 BufferedImage image = new BufferedImage(targetComponent.getWidth(), targetComponent.getHeight(), BufferedImage.TYPE_INT_RGB);
                 Graphics2D g = image.createGraphics();
                 targetComponent.paint(g);
                 g.dispose();
                 copyImageToClipboard(image); // Copy trực tiếp

                 api.logging().logToOutput("Normal screenshot captured and copied to clipboard.");
            } else {
                 api.logging().logToOutput("Could not find 'rrvSplitViewerSplitPane' component for normal screenshot.");
            }
        } catch (Exception ex) {
             api.logging().logToError("Error in handleNormalScreenshot: " + ex.getMessage());
        } finally {
             // Dù có lỗi hay không, luôn đảm bảo buffer trống sau thao tác độc lập này
             clearImages();
        }
    }

    /**
     * Xử lý chức năng "Full Screenshot": Chụp ảnh phần Request và Response SyntaxTextArea
     * mặc định, kết hợp chúng và copy ra clipboard.
     */
    public void handleFullScreenshot() {
        api.logging().logToOutput("Executing Full Screenshot (default views)...");
        clearImages(); // Bắt đầu bằng cách xóa buffer

        try {
            Frame suiteFrame = api.userInterface().swingUtils().suiteFrame();
             Component targetComponent = findComponentUnderMouse("rrvSplitViewerSplitPane", suiteFrame);

             if (targetComponent == null) {
                 api.logging().logToOutput("Could not find 'rrvSplitViewerSplitPane' component for full screenshot.");
                 return; // Exit if the main split pane isn't found
             }

            Component reqComp = getComponentByName(targetComponent, "rrvRequestsPane");
            Component resComp = getComponentByName(targetComponent, "rrvResponsePane");

             if (reqComp == null || resComp == null) {
                 api.logging().logToOutput("Could not find Request/Response panes for full screenshot.");
                 return; // Exit if request/response panes aren't found
             }

            // Tìm syntaxTextArea đầu tiên trong mỗi pane (index 0)
            java.util.List<Component> reqComponents = findAllComponentsByName(reqComp, "syntaxTextArea");
            java.util.List<Component> resComponents = findAllComponentsByName(resComp, "syntaxTextArea");

            if (reqComponents.isEmpty() || resComponents.isEmpty()) {
                 api.logging().logToOutput("No syntaxTextArea found in Request/Response panes for full screenshot.");
                 return; // Exit if syntaxTextArea aren't found
             }

            Component syntaxTextAreaReq = reqComponents.get(0); // Index 0 for default view
            Component syntaxTextAreaRes = resComponents.get(0); // Index 0 for default view

            // Chụp và lưu vào buffer (sử dụng phương thức nội bộ)
            captureComponentToBuffer(syntaxTextAreaReq);
            captureComponentToBuffer(syntaxTextAreaRes);

            // Kết hợp các ảnh trong buffer và copy ra clipboard
            BufferedImage combinedImage = combineBufferedImagesHorizontally();
            if (combinedImage != null) {
                 copyImageToClipboard(combinedImage);
                 api.logging().logToOutput("Full screenshot combined and copied.");
            } else {
                 api.logging().logToOutput("Full screenshot failed due to image combining issues.");
            }


        } catch (Exception ex) {
            api.logging().logToError("Error in handleFullScreenshot: " + ex.getMessage());
        } finally {
             // Luôn xóa buffer sau khi thao tác hoàn tất (thành công hoặc lỗi)
             clearImages();
        }
    }


    /**
     * Xử lý chức năng "Indexed Screenshot": Chụp ảnh Request/Response SyntaxTextArea
     * tại index được chỉ định, kết hợp chúng và copy ra clipboard.
     * @param index Index của syntaxTextArea cần chụp (0 cho original, 1 cho edited).
     */
    public void handleIndexedScreenshot(int index) {
        api.logging().logToOutput(String.format("Executing Indexed Screenshot (index %d)...", index));
         clearImages(); // Bắt đầu bằng cách xóa buffer

        try {
             Frame suiteFrame = api.userInterface().swingUtils().suiteFrame();
             Component targetComponent = findComponentUnderMouse("rrvSplitViewerSplitPane", suiteFrame);

              if (targetComponent == null) {
                 api.logging().logToOutput("Could not find 'rrvSplitViewerSplitPane' component for indexed screenshot.");
                 return; // Exit
             }

            Component reqComp = getComponentByName(targetComponent, "rrvRequestsPane");
            Component resComp = getComponentByName(targetComponent, "rrvResponsePane");

             if (reqComp == null || resComp == null) {
                 api.logging().logToOutput("Could not find Request/Response panes for indexed screenshot.");
                 return; // Exit
             }

            java.util.List<Component> reqComponents = findAllComponentsByName(reqComp,
                    "syntaxTextArea");
            java.util.List<Component> resComponents = findAllComponentsByName(resComp,
                    "syntaxTextArea");

            if (reqComponents.size() <= index || resComponents.size() <= index) {
                api.logging().logToOutput(String.format("Not enough syntaxTextArea found for index %d. Req count: %d, Res count: %d. Cannot take screenshot.", index, reqComponents.size(), resComponents.size()));
                return; // Exit if index is out of bounds
            }

            Component syntaxTextAreaReq = reqComponents.get(index);
            Component syntaxTextAreaRes = resComponents.get(index);

            // Chụp và lưu vào buffer (sử dụng phương thức nội bộ)
            captureComponentToBuffer(syntaxTextAreaReq);
            captureComponentToBuffer(syntaxTextAreaRes);

             // Kết hợp các ảnh trong buffer và copy ra clipboard
            BufferedImage combinedImage = combineBufferedImagesHorizontally();
             if (combinedImage != null) {
                 copyImageToClipboard(combinedImage);
                 api.logging().logToOutput(String.format("Indexed screenshot (index %d) combined and copied.", index));
            } else {
                 api.logging().logToOutput(String.format("Indexed screenshot (index %d) failed due to image combining issues.", index));
            }


        } catch (Exception ex) {
            api.logging().logToError("Error in handleIndexedScreenshot: " + ex.getMessage());
        } finally {
            // Luôn xóa buffer sau khi thao tác hoàn tất (thành công hoặc lỗi)
             clearImages();
        }
    }
}
