package com.screenshort.utils;

import java.util.List;
import java.util.function.Consumer;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.http.message.HttpRequestResponse;
import burp.api.montoya.ui.hotkey.HotKeyContext;

/**
 * Registers keyboard shortcuts for the HTTP message editor context.
 */
public class CustomMessageEditorHotKey {

    private final MenuActionHandler menuHandler;
    private final ScreenshotUtils screenshotUtils;

    public CustomMessageEditorHotKey(MontoyaApi api) {
        this.menuHandler = new MenuActionHandler(api);
        this.screenshotUtils = new ScreenshotUtils(api);
        registerHotKeys(api);
    }

    private void registerHotKeys(MontoyaApi api) {
        // Screenshot hotkeys
        api.userInterface().registerHotKeyHandler(
                HotKeyContext.HTTP_MESSAGE_EDITOR,
                "Ctrl+Shift+S",
                evt -> screenshotUtils.handleNormalScreenshot()
        );

        api.userInterface().registerHotKeyHandler(
                HotKeyContext.HTTP_MESSAGE_EDITOR,
                "Ctrl+Shift+Space",
                evt -> screenshotUtils.handleFullScreenshot()
        );

        // PCopy hotkeys - using helper method to reduce duplication (supports list)
        registerRequestResponseListHotKey(api, "Ctrl+Alt+Space", menuHandler::handleCopyToExcel);
        registerRequestResponseListHotKey(api, "Ctrl+Alt+X", menuHandler::handleCopyToExcelNoBody);

        // GenData hotkey - single item
        registerRequestResponseHotKey(api, "Ctrl+Alt+V", menuHandler::handleGenDataAction);
    }

    /**
     * Helper method to register a hotkey that requires List of HttpRequestResponse.
     * Used for PCopy which supports multiple selections.
     *
     * @param api     The Montoya API
     * @param hotkey  The hotkey combination string
     * @param handler The handler to invoke with the List of HttpRequestResponse
     */
    private void registerRequestResponseListHotKey(
            MontoyaApi api,
            String hotkey,
            Consumer<List<HttpRequestResponse>> handler
    ) {
        api.userInterface().registerHotKeyHandler(
                HotKeyContext.HTTP_MESSAGE_EDITOR,
                hotkey,
                evt -> evt.messageEditorRequestResponse()
                        .map(m -> m.requestResponse())
                        .ifPresent(reqRes -> handler.accept(List.of(reqRes)))
        );
    }

    /**
     * Helper method to register a hotkey that requires HttpRequestResponse.
     * Extracts the common pattern of getting the request/response and invoking the handler.
     *
     * @param api     The Montoya API
     * @param hotkey  The hotkey combination string
     * @param handler The handler to invoke with the HttpRequestResponse
     */
    private void registerRequestResponseHotKey(
            MontoyaApi api,
            String hotkey,
            Consumer<HttpRequestResponse> handler
    ) {
        api.userInterface().registerHotKeyHandler(
                HotKeyContext.HTTP_MESSAGE_EDITOR,
                hotkey,
                evt -> evt.messageEditorRequestResponse()
                        .map(m -> m.requestResponse())
                        .ifPresent(handler)
        );
    }
}
