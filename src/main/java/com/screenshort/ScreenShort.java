package com.screenshort;

import burp.api.montoya.BurpExtension;
import burp.api.montoya.MontoyaApi;
import com.screenshort.utils.CustomMessageEditorHotKey;

public class ScreenShort implements BurpExtension{
    @Override
    public void initialize(MontoyaApi api) {
        api.extension().setName("ScreenShort");

        api.logging().logToOutput("ScreenShort extension initialized");
        api.logging().logToOutput("Copyright @toancse - 1.6");
        api.logging().logToOutput("Update: 2025-06-11 - PCopy added newline in Request!");

        api.userInterface().registerContextMenuItemsProvider(new GUI(api));

        
        CustomMessageEditorHotKey.registerHotKey(api);
    }
}
