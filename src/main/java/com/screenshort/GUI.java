package com.screenshort;

import com.screenshort.utils.MenuActionHandler;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import burp.api.montoya.MontoyaApi;
import burp.api.montoya.http.message.HttpRequestResponse;
import burp.api.montoya.ui.contextmenu.ContextMenuEvent;
import burp.api.montoya.ui.contextmenu.ContextMenuItemsProvider;
import burp.api.montoya.ui.contextmenu.MessageEditorHttpRequestResponse;
import java.util.function.Consumer;

public class GUI implements ContextMenuItemsProvider {
    private final MenuActionHandler menuHandler;

    public GUI(MontoyaApi api) {
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

    private JMenuItem createMenuItemWithReqResList(String text, Consumer<List<HttpRequestResponse>> action, List<HttpRequestResponse> reqResList) {
        JMenuItem menuItem = new JMenuItem(text);
        menuItem.addActionListener(e -> action.accept(reqResList));
        return menuItem;
    }

    private JMenu createScreenshotMenu(ContextMenuEvent event) {
        JMenu screenshotMenu = new JMenu("Screenshot");

        // Renamed slightly for clarity, now lead to annotation editor
        screenshotMenu.add(createMenuItem("Annotate Component (Normal) - Ctrl+Shift+S", menuHandler::handleNormalScreenshot));
        screenshotMenu.add(createMenuItem("Annotate Full Req/Res (Full)- Ctrl+Shift+Space", menuHandler::handleFullScreenshot));

        // --- REMOVED Original/Edited items and conditional logic ---

        return screenshotMenu;
    }

    private JMenu createPCopyMenu(List<HttpRequestResponse> requestResponses) {
        JMenu pCopyMenu = new JMenu("PCopy");
        String countLabel = requestResponses.size() > 1 ? " (" + requestResponses.size() + " items)" : "";
        pCopyMenu.add(createMenuItemWithReqResList("PCopy has body" + countLabel + " - Ctrl+Alt+Space", menuHandler::handleCopyToExcel, requestResponses));
        pCopyMenu.add(createMenuItemWithReqResList("PCopy no body" + countLabel + " - Ctrl+Alt+X", menuHandler::handleCopyToExcelNoBody, requestResponses));
        return pCopyMenu;
    }

    private JMenu createGendataMenu(HttpRequestResponse requestResponse) {
        JMenu gendataMenu = new JMenu("Export File");
        gendataMenu.add(createMenuItemWithReqRes("Export File - Ctrl+Alt+D", menuHandler::handleGenDataAction, requestResponse));

        JMenuItem chooseDefaultPathItem = createMenuItem("Choose Default Path", menuHandler::handleChooseDefaultPath);
        gendataMenu.add(chooseDefaultPathItem);

        return gendataMenu;
    }

    @Override
    public List<Component> provideMenuItems(ContextMenuEvent event) {
        List<Component> menuItems = new ArrayList<>();

        // Always add screenshot menu
        menuItems.add(createScreenshotMenu(event));

        // Get all selected request/responses for PCopy (supports multi-selection)
        List<HttpRequestResponse> selectedRequestResponses = new ArrayList<>();

        // First check if we're in a message editor
        Optional<MessageEditorHttpRequestResponse> editorReqRes = event.messageEditorRequestResponse();
        if (editorReqRes.isPresent()) {
            selectedRequestResponses.add(editorReqRes.get().requestResponse());
        } else {
            // Otherwise get all selected items from the table
            selectedRequestResponses.addAll(event.selectedRequestResponses());
        }

        if (!selectedRequestResponses.isEmpty()) {
            // PCopy supports multiple selections
            menuItems.add(createPCopyMenu(selectedRequestResponses));

            // Export File uses first selected item only
            menuItems.add(createGendataMenu(selectedRequestResponses.get(0)));
        }
        return menuItems;
    }
}