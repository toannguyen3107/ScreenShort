package com.screenshort;
import com.screenshort.utils.MenuActionHandler;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import burp.api.montoya.MontoyaApi;
import burp.api.montoya.core.ToolType;
import burp.api.montoya.http.message.HttpRequestResponse;
import burp.api.montoya.ui.contextmenu.ContextMenuEvent;
import burp.api.montoya.ui.contextmenu.ContextMenuItemsProvider;
import java.util.function.Consumer;
public class GUI implements ContextMenuItemsProvider {
    private final MontoyaApi api;
    private final MenuActionHandler menuHandler;
    public GUI(MontoyaApi api) {
        this.api = api;
        this.menuHandler = new MenuActionHandler(api);
    }
    private JMenuItem createMenuItem(String text, Runnable action) {
        JMenuItem menuItem = new JMenuItem(text);
        menuItem.addActionListener(e -> action.run());
        return menuItem;
    }
    private JMenuItem createMenuItemWithReqRes(String text, Consumer<HttpRequestResponse> action, HttpRequestResponse reqRes) {
        JMenuItem menuItem = new JMenuItem(text);
        menuItem.addActionListener(e -> action.accept(reqRes));
        return menuItem;
    }
    private JMenu createScreenshotMenu(ContextMenuEvent event) {
        JMenu screenshotMenu = new JMenu("Screenshot");
        screenshotMenu.add(createMenuItem("Normal", menuHandler::handleNormalScreenshot));
        screenshotMenu.add(createMenuItem("Full", menuHandler::handleFullScreenshot));   
        if (event.isFromTool(ToolType.PROXY) && event.messageEditorRequestResponse().isPresent()) {
             screenshotMenu.add(createMenuItem("Full - Edited Request", menuHandler::handleEditedScreenshot));
             screenshotMenu.add(createMenuItem("Full - Original Request", menuHandler::handleOriginalScreenshot));
        }
        return screenshotMenu;
    }
    private JMenu createPCopyMenu(HttpRequestResponse requestResponse) {
        JMenu pCopyMenu = new JMenu("PCopy");
        pCopyMenu.add(createMenuItemWithReqRes("PCopy has body", menuHandler::handleCopyToExcel, requestResponse));
        pCopyMenu.add(createMenuItemWithReqRes("PCopy no body", menuHandler::handleCopyToExcelNoBody, requestResponse));
        return pCopyMenu;
    }
    private JMenu createGendataMenu(HttpRequestResponse requestResponse) {
        JMenu gendataMenu = new JMenu("GenData");
        gendataMenu.add(createMenuItemWithReqRes("GenData", menuHandler::handleGenDataAction, requestResponse));
        return gendataMenu;
    }
    @Override
    public List<Component> provideMenuItems(ContextMenuEvent event) {
        List<Component> menuItems = new ArrayList<>();
        menuItems.add(createScreenshotMenu(event));
        Optional<HttpRequestResponse> optionalRequestResponse = event.messageEditorRequestResponse().isPresent()
                ? Optional.of(event.messageEditorRequestResponse().get().requestResponse())
                : event.selectedRequestResponses().isEmpty() ? Optional.empty() : Optional.of(event.selectedRequestResponses().get(0));
        if (optionalRequestResponse.isPresent()) {
             HttpRequestResponse requestResponse = optionalRequestResponse.get();
             menuItems.add(createPCopyMenu(requestResponse));
             menuItems.add(createGendataMenu(requestResponse));
        }
        return menuItems;
    }
}