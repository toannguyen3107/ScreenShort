package com.screenshort.utils;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.http.message.HttpRequestResponse;

public class MenuActionHandler {
    private final MontoyaApi api;
    private final ScreenshotUtils screenshotUtils; 
    public MenuActionHandler(MontoyaApi api) {
        this.api = api;
        this.screenshotUtils = new ScreenshotUtils(api);
    }

    public void handleNormalScreenshot() {
        screenshotUtils.handleNormalScreenshot();
    }

    public void handleFullScreenshot() {
        screenshotUtils.handleFullScreenshot();
    }

    public void handleEditedScreenshot() {
        screenshotUtils.handleIndexedScreenshot(1);
    }

    public void handleOriginalScreenshot() {
        screenshotUtils.handleIndexedScreenshot(0);
    }

    public void handleCopyToExcel(HttpRequestResponse requestResponse) {
        if (requestResponse == null) {
             api.logging().logToOutput("Cannot copy to Excel: RequestResponse is null.");
             return;
        }
        String data = ExcelFormatterUtils.formatRequestResponseForExcel(requestResponse); 
        ExcelFormatterUtils.copyToClipboard(data);
    }

    public void handleCopyToExcelNoBody(HttpRequestResponse requestResponse) {
         if (requestResponse == null) {
             api.logging().logToOutput("Cannot copy to Excel: RequestResponse is null.");
             return;
        }
         String data = ExcelFormatterUtils.formatRequestResponseForExcelNoBody(requestResponse); 
        ExcelFormatterUtils.copyToClipboard(data);
    }
    public void handleGenDataAction(HttpRequestResponse requestResponse) {
        if (requestResponse == null) {
             api.logging().logToOutput("Cannot generate GenData: RequestResponse is null.");
             return;
        }
        String jsonData = GenDataToJson.formatRequestResponseToJson(requestResponse);
        GenDataToJson.copyToClipboard(jsonData);
    }
}
