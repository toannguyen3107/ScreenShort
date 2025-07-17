/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.screenshort.utils;

import java.util.List;
import java.util.Optional;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.http.message.HttpRequestResponse;
import burp.api.montoya.ui.hotkey.HotKeyContext;
import burp.api.montoya.ui.hotkey.HotKeyEvent;
import burp.api.montoya.ui.hotkey.HotKeyHandler;

/**
 *
 * @author ASUS
 */
public class CustomMessageEditorHotKey {
    private final MenuActionHandler menuHandler;

    public CustomMessageEditorHotKey(MontoyaApi api) {
        this.menuHandler = new MenuActionHandler(api);
        this.registerScreenShortHotKey(api);
    }


    public void registerScreenShortHotKey(MontoyaApi api) {
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
        api.userInterface().registerHotKeyHandler(HotKeyContext.HTTP_MESSAGE_EDITOR, "Ctrl+Alt+Space", new HotKeyHandler() {
            @Override
            public void handle(HotKeyEvent evt) {
                Optional<HttpRequestResponse> requestResponse = evt.messageEditorRequestResponse()
                    .map(m -> m.requestResponse());
                if (requestResponse.isPresent()) {
                    menuHandler.handleCopyToExcel(requestResponse.get());
                }
            }
        });
        api.userInterface().registerHotKeyHandler(HotKeyContext.HTTP_MESSAGE_EDITOR, "Ctrl+Alt+C", new HotKeyHandler() {
            @Override
            public void handle(HotKeyEvent evt) {
                Optional<HttpRequestResponse> requestResponse = evt.messageEditorRequestResponse()
                    .map(m -> m.requestResponse());
                if (requestResponse.isPresent()) {
                    menuHandler.handleCopyToExcelNoBody(requestResponse.get());
                }
            }
        });

        api.userInterface().registerHotKeyHandler(HotKeyContext.HTTP_MESSAGE_EDITOR, "Ctrl+Alt+V", new HotKeyHandler() {
            @Override
            public void handle(HotKeyEvent evt) {
                Optional<HttpRequestResponse> requestResponse = evt.messageEditorRequestResponse()
                    .map(m -> m.requestResponse());
                if (requestResponse.isPresent()) {
                    menuHandler.handleGenDataAction(requestResponse.get());
                }
            }
        });
    }
}
