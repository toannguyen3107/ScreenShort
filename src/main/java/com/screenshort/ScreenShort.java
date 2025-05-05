package com.screenshort;

import burp.api.montoya.BurpExtension;
import burp.api.montoya.MontoyaApi;

public class ScreenShort implements BurpExtension{
    @Override
    public void initialize(MontoyaApi api) {
        api.extension().setName("ScreenShort");

        api.logging().logToOutput("ScreenShort extension initialized");
        api.logging().logToOutput("Copyright @toancse - 1.5");

        api.userInterface().registerContextMenuItemsProvider(new GUI(api));

    }
}
