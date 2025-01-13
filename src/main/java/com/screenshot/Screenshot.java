package com.screenshot;

import burp.api.montoya.BurpExtension;
import burp.api.montoya.MontoyaApi;

public class Screenshot implements BurpExtension{
    @Override
    public void initialize(MontoyaApi api) {
        api.extension().setName("screenshots");

        api.logging().logToOutput("Screenshot extension initialized");
        api.logging().logToOutput("Copyright @toancse v - 1.1");

        api.userInterface().registerContextMenuItemsProvider(new GUI(api));

    }
}
