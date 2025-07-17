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
    public void handleCopyToExcel(HttpRequestResponse requestResponse) {
        if (requestResponse == null) {
             api.logging().logToOutput("Cannot copy to Excel: RequestResponse is null.");
             return;
        }
        // Assume ExcelFormatterUtils exists and works
        String data = ExcelFormatterUtils.formatRequestResponseForExcel(requestResponse);
        ExcelFormatterUtils.copyToClipboard(data);
    }

    public void handleCopyToExcelNoBody(HttpRequestResponse requestResponse) {
         if (requestResponse == null) {
             api.logging().logToOutput("Cannot copy to Excel: RequestResponse is null.");
             return;
        }
         // Assume ExcelFormatterUtils exists and works
         String data = ExcelFormatterUtils.formatRequestResponseForExcelNoBody(requestResponse);
         ExcelFormatterUtils.copyToClipboard(data);
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
