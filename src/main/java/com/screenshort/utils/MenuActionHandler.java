package com.screenshort.utils;

import java.util.List;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.http.message.HttpRequestResponse;

public class MenuActionHandler {
    private final MontoyaApi api;
    private final ScreenshotUtils screenshotUtils;

    public MenuActionHandler(MontoyaApi api) {
        this.api = api;
        this.screenshotUtils = new ScreenshotUtils(api);
    }

    // Calls the modified screenshot method which now opens the annotator
    public void handleNormalScreenshot() {
        screenshotUtils.handleNormalScreenshot();
    }

    // Calls the modified full screenshot method which combines Req/Res and opens the annotator
    public void handleFullScreenshot() {
        screenshotUtils.handleFullScreenshot();
    }


    // --- Other utility methods (unchanged) ---
    public void handleCopyToExcel(List<HttpRequestResponse> requestResponses) {
        if (requestResponses == null || requestResponses.isEmpty()) {
             api.logging().logToOutput("Cannot copy to Excel: No RequestResponse selected.");
             return;
        }
        String data = ExcelFormatterUtils.formatRequestResponseForExcel(requestResponses);
        ExcelFormatterUtils.copyToClipboard(data);
        api.logging().logToOutput("PCopy: Copied " + requestResponses.size() + " row(s) to clipboard.");
    }

    public void handleCopyToExcelNoBody(List<HttpRequestResponse> requestResponses) {
        if (requestResponses == null || requestResponses.isEmpty()) {
             api.logging().logToOutput("Cannot copy to Excel: No RequestResponse selected.");
             return;
        }
        String data = ExcelFormatterUtils.formatRequestResponseForExcelNoBody(requestResponses);
        ExcelFormatterUtils.copyToClipboard(data);
        api.logging().logToOutput("PCopy: Copied " + requestResponses.size() + " row(s) to clipboard (no body).");
    }
    public void handleGenDataAction(HttpRequestResponse requestResponse) {
        if (requestResponse == null) {
             api.logging().logToOutput("Cannot generate GenData: RequestResponse is null.");
             return;
        }
        // Assume GenDataToJson exists and works
        GenDataToJson.exportFile(List.of(requestResponse));
    }
    public void handleChooseDefaultPath() {
        GenDataToJson.chooseDefaultPath();
    }
}
