package com.screenshort;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JMenuItem;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.core.ToolType;
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
        
        JMenuItem normalMenuItem = new JMenuItem("Normal");
        JMenuItem fullMenuItem = new JMenuItem("Full");
        JMenuItem fullEditedMenuItem = new JMenuItem("Full - Edited Request");
        JMenuItem fullOriginalMenuItem = new JMenuItem("Full - Original Request");
        
        
        normalMenuItem.addActionListener(e -> menuHandler.handleNormalScreenshot());
        fullMenuItem.addActionListener(e -> menuHandler.handleFullScreenshot());
        fullEditedMenuItem.addActionListener(e -> menuHandler.handleEditedScreenshot());
        fullOriginalMenuItem.addActionListener(e -> menuHandler.handleOriginalScreenshot());
        
        menuItems.add(normalMenuItem);
        menuItems.add(fullMenuItem);
        
        if (event.isFromTool(ToolType.PROXY)) {
            menuItems.add(fullEditedMenuItem);
            menuItems.add(fullOriginalMenuItem);
        }
        
        return menuItems;
    }
}
