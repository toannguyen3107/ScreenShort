package com.screenshort;

import com.screenshort.utils.MenuActionHandler;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import burp.api.montoya.MontoyaApi;
import burp.api.montoya.core.ToolType; // Keep ToolType import if used elsewhere, but not needed for screenshot menu anymore
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

        // Renamed slightly for clarity, now lead to annotation editor
        screenshotMenu.add(createMenuItem("Annotate Component (Normal)", menuHandler::handleNormalScreenshot));
        screenshotMenu.add(createMenuItem("Annotate Full Req/Res (Full)", menuHandler::handleFullScreenshot));

        // --- REMOVED Original/Edited items and conditional logic ---

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

        // Always add screenshot menu
        menuItems.add(createScreenshotMenu(event));

        // Add other menus if request/response data is available
        Optional<HttpRequestResponse> optionalRequestResponse = event.messageEditorRequestResponse().map(editorReqRes -> editorReqRes.requestResponse())
                .or(() -> event.selectedRequestResponses().stream().findFirst());

        if (optionalRequestResponse.isPresent()) {
             HttpRequestResponse requestResponse = optionalRequestResponse.get();
             menuItems.add(createPCopyMenu(requestResponse));
             menuItems.add(createGendataMenu(requestResponse));
        }
        return menuItems;
    }
}