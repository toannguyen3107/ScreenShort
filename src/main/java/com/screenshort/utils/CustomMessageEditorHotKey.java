/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.screenshort.utils;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.ui.hotkey.HotKeyContext;
import burp.api.montoya.ui.hotkey.HotKeyEvent;
import burp.api.montoya.ui.hotkey.HotKeyHandler;

/**
 *
 * @author ASUS
 */
public class CustomMessageEditorHotKey {

    public static void registerHotKey(MontoyaApi api) {
        CustomMessageEditorHotKey.registerScreenShortHotKey(api);
    }

    public static void registerScreenShortHotKey(MontoyaApi api) {
        ScreenshotUtils screenshotUtils = new ScreenshotUtils(api);
        api.userInterface().registerHotKeyHandler(HotKeyContext.HTTP_MESSAGE_EDITOR, "Ctrl+Shift+S", new HotKeyHandler() {
            @Override
            public void handle(HotKeyEvent evt) {
                screenshotUtils.handleNormalScreenshot();
            }
        });

        api.userInterface().registerHotKeyHandler(HotKeyContext.HTTP_MESSAGE_EDITOR, "Ctrl+Shift+Space", new HotKeyHandler() {
            @Override
            public void handle(HotKeyEvent evt) {
                screenshotUtils.handleFullScreenshot();
            }
        });
    }
}
