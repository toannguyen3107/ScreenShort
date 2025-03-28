package com.screenshort;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JMenu;
import javax.swing.JMenuItem;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.core.ToolType;
import burp.api.montoya.http.message.HttpRequestResponse;
import burp.api.montoya.ui.contextmenu.ContextMenuEvent;
import burp.api.montoya.ui.contextmenu.ContextMenuItemsProvider;

public class GUI implements ContextMenuItemsProvider {
    private final MontoyaApi api;
    private final MenuActionHandler menuHandler;

    public GUI(MontoyaApi api) {
        this.api = api;
        this.menuHandler = new MenuActionHandler(api);
    }

    @Override
    public List<Component> provideMenuItems(ContextMenuEvent event) {
        List<Component> menuItems = new ArrayList<>();

        // Tạo JMenu thay vì JMenuItem để có thể chứa các mục con
        JMenu screenshotMenu = new JMenu("Screenshot");

        JMenuItem normalMenuItem = new JMenuItem("Normal");
        JMenuItem fullMenuItem = new JMenuItem("Full");
        JMenuItem fullEditedMenuItem = new JMenuItem("Full - Edited Request");
        JMenuItem fullOriginalMenuItem = new JMenuItem("Full - Original Request");

        normalMenuItem.addActionListener(e -> menuHandler.handleNormalScreenshot());
        fullMenuItem.addActionListener(e -> menuHandler.handleFullScreenshot());
        fullEditedMenuItem.addActionListener(e -> menuHandler.handleEditedScreenshot());
        fullOriginalMenuItem.addActionListener(e -> menuHandler.handleOriginalScreenshot());

        screenshotMenu.add(normalMenuItem);
        screenshotMenu.add(fullMenuItem);

        if (event.isFromTool(ToolType.PROXY)) {
            screenshotMenu.add(fullEditedMenuItem);
            screenshotMenu.add(fullOriginalMenuItem);
        }

        menuItems.add(screenshotMenu);

        // PCopy
        JMenu pCopy = new JMenu("PCopy");
        JMenuItem pCopyhasBody = new JMenuItem("PCopy has body");
        JMenuItem pCopynoBody = new JMenuItem("PCopy no body");
        // Lấy HttpRequestResponse từ sự kiện
        HttpRequestResponse requestResponse = event.messageEditorRequestResponse().isPresent()
                ? event.messageEditorRequestResponse().get().requestResponse()
                : event.selectedRequestResponses().get(0);

        pCopyhasBody.addActionListener(e -> menuHandler.handleCopyToExcel(requestResponse));
        pCopynoBody.addActionListener(e -> menuHandler.handleCopyToExcelNoBody(requestResponse));

        pCopy.add(pCopyhasBody);
        pCopy.add(pCopynoBody);
        
        menuItems.add(pCopy);

        return menuItems;
    }

}
