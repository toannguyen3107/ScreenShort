package com.screenshort;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.nio.charset.StandardCharsets;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.core.ByteArray;
import burp.api.montoya.http.HttpService;
import burp.api.montoya.http.message.Cookie;
import burp.api.montoya.http.message.HttpHeader;
import burp.api.montoya.http.message.HttpRequestResponse;
import burp.api.montoya.http.message.params.HttpParameterType;
import burp.api.montoya.http.message.params.ParsedHttpParameter;

import java.util.List;

public class MenuActionHandler {
    private final MontoyaApi api;
    private final Action action;

    public MenuActionHandler(MontoyaApi api) {
        this.api = api;
        this.action = new Action(api);
    }

    public void handleNormalScreenshot() {
        Frame frame = api.userInterface().swingUtils().suiteFrame();
        Component targetComponent = ComponentFinder.findComponentUnderMouse("rrvSplitViewerSplitPane", frame);
        action.takeScreenshot(targetComponent);
    }

    public void handleFullScreenshot() {
        try {
            Frame frame = api.userInterface().swingUtils().suiteFrame();
            Component targetComponent = ComponentFinder.findComponentUnderMouse("rrvSplitViewerSplitPane", frame);
            Component reqComp = ComponentFinder.getComponentByName(targetComponent, "rrvRequestsPane");
            Component resComp = ComponentFinder.getComponentByName(targetComponent, "rrvResponsePane");
            Component syntaxTextAreaReq = ComponentFinder.getComponentByName(reqComp, "syntaxTextArea");
            Component syntaxTextAreaRes = ComponentFinder.getComponentByName(resComp, "syntaxTextArea");

            action.takeScreenshotAndGetBufferImage(syntaxTextAreaReq);
            action.takeScreenshotAndGetBufferImage(syntaxTextAreaRes);
            action.takeScreenshot2();
        } catch (Exception ex) {
            api.logging().logToError("Error: " + ex.getMessage());
        }
    }

    public void handleEditedScreenshot() {
        captureScreenshot(1);
    }

    public void handleOriginalScreenshot() {
        captureScreenshot(0);
    }

    private void captureScreenshot(int index) {
        try {
            Frame frame = api.userInterface().swingUtils().suiteFrame();
            Component targetComponent = ComponentFinder.findComponentUnderMouse("rrvSplitViewerSplitPane", frame);
            Component reqComp = ComponentFinder.getComponentByName(targetComponent, "rrvRequestsPane");
            Component resComp = ComponentFinder.getComponentByName(targetComponent, "rrvResponsePane");

            java.util.List<Component> reqComponents = ComponentFinder.findAllComponentsByName(reqComp,
                    "syntaxTextArea");
            java.util.List<Component> resComponents = ComponentFinder.findAllComponentsByName(resComp,
                    "syntaxTextArea");

            if (reqComponents.size() <= index || resComponents.size() <= index) {
                api.logging().logToOutput("No syntaxTextArea found");
                action.clearImages();
                return;
            }

            Component syntaxTextAreaReq = reqComponents.get(index);
            Component syntaxTextAreaRes = resComponents.get(index);

            action.takeScreenshotAndGetBufferImage(syntaxTextAreaReq);
            action.takeScreenshotAndGetBufferImage(syntaxTextAreaRes);
            action.takeScreenshot2();
        } catch (Exception ex) {
            api.logging().logToError("Error: " + ex.getMessage());
        }
    }

    public void handleCopyToExcel(HttpRequestResponse requestResponse) {
        ExcelFormatter excelFormatter = new ExcelFormatter();

        // -----------------------------------------------
        // Method
        String method = requestResponse.request().method();
        // Host
        HttpService httpService = requestResponse.httpService();
        String hostService = httpService.host();
        int portService = httpService.port();
        String host = hostService + ":" + portService;
        // Path
        String path = requestResponse.request().path();
        // Request Header
        List<HttpHeader> headersRequest = requestResponse.request().headers();

        String headersToString = headersRequest.stream()
                .map(HttpHeader::toString)
                .reduce((header1, header2) -> header1 + "\n" + header2)
                .orElse(""); // use reduce to concatenate headers
        String firstLineOfReqHead = method + "   " + path + "   " + requestResponse.request().httpVersion() + "\n";

        headersToString = firstLineOfReqHead + headersToString;
        // Response Header
        List<HttpHeader> headersResponse = requestResponse.response().headers();
        String headersResponseToString = headersResponse.stream()
                .map(HttpHeader::toString)
                .reduce((header1, header2) -> header1 + "\n" + header2)
                .orElse(""); // use reduce to concatenate headers
        String firstLine = requestResponse.response().toString().split("\n")[0];
        headersResponseToString = firstLine + "\n" + headersResponseToString;
        // Request Body
        ByteArray requestBodyByte = requestResponse.request().toByteArray();

        // Strip all control characters (0x00 - 0x1F) and non-ASCII characters (0x80+)
        String requestBody = new String(ExcelFormatter.filterValidASCII(requestBodyByte.getBytes()), StandardCharsets.UTF_8);

        // Response Body
        String responseBody = requestResponse.response().bodyToString();
        // Request response raw
        String tmp = "______ REQUEST ______\n";
        tmp += "GET Params\n";
        List<ParsedHttpParameter> params = requestResponse.request().parameters(HttpParameterType.URL);
        for (ParsedHttpParameter param : params) {
            tmp += param.name() + " | ";
        }
        tmp += "\nPOST Params\n";
        List<ParsedHttpParameter> paramsPost = requestResponse.request().parameters(HttpParameterType.BODY);
        for (ParsedHttpParameter param : paramsPost) {
            tmp += param.name() + " | ";
        }
        tmp += "\nHEADERS\n";
        for (HttpHeader header : headersRequest) {
            tmp += header.name() + " | ";
        }
        tmp += "\nCookie\n";
        List<ParsedHttpParameter> cookies = requestResponse.request().parameters(HttpParameterType.COOKIE);
        for (ParsedHttpParameter cookie : cookies) {
            tmp += cookie.name() + " | ";
        }

        tmp += "\n\n______ RESPONSE ______\n";

        // tmp += "\nPOST Params\n";
        // List<ParsedHttpParameter> paramsPostResponse =
        // requestResponse.response().parameters(HttpParameterType.BODY);
        // for (ParsedHttpParameter param: paramsPostResponse) {
        // tmp += param.name() + " | " ;
        // }
        tmp += "\nHEADERS\n";
        for (HttpHeader header : headersResponse) {
            tmp += header.name() + " | ";
        }
        tmp += "\nCookie\n";
        List<Cookie> cookiesResponse = requestResponse.response().cookies();
        for (Cookie cookie : cookiesResponse) {
            tmp += cookie.name() + " | ";
        }
        tmp += "\n\n______ RAW ______\n";
        // reqeust beautify
        ByteArray  requestBodyByte1 = requestResponse.request().body();

        // Strip all control characters (0x00 - 0x1F) and non-ASCII characters (0x80+)
        String requestBody1 = new String(ExcelFormatter.filterValidASCII(requestBodyByte1.getBytes()), StandardCharsets.UTF_8);
        // -----------------------------------------------
        // method
        excelFormatter.addData(ExcelFormatter.excelFormat(method));
        // host
        excelFormatter.addData(ExcelFormatter.excelFormat(host));
        // path
        excelFormatter.addData(ExcelFormatter.excelFormat(path));
        // request header
        excelFormatter.addData(ExcelFormatter.excelFormat(headersToString));
        // response header
        excelFormatter.addData(ExcelFormatter.excelFormat(headersResponseToString));
        // request body
        excelFormatter.addData(ExcelFormatter.excelFormat(requestBody));
        // response body
        excelFormatter.addData(ExcelFormatter.excelFormat(responseBody));
        // request response raw
        excelFormatter.addData(ExcelFormatter.excelFormat(tmp));
        // request body beautify
        excelFormatter.addData(ExcelFormatter.excelFormat(requestBody1));
        // ------------------------------------------------
        // Copy to clipboard
        String data = excelFormatter.getData();
        StringSelection stringSelection = new StringSelection(data);
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        clipboard.setContents(stringSelection, null);
    }

    public void handleCopyToExcelNoBody(HttpRequestResponse requestResponse) {
        ExcelFormatter excelFormatter = new ExcelFormatter();

        // -----------------------------------------------
        // Method
        String method = requestResponse.request().method();
        // Host
        HttpService httpService = requestResponse.httpService();
        String hostService = httpService.host();
        int portService = httpService.port();
        String host = hostService + ":" + portService;
        // Path
        String path = requestResponse.request().path().split("\\?")[0];
        // Request Header
        List<HttpHeader> headersRequest = requestResponse.request().headers();
        String headersToString = headersRequest.stream()
                .map(HttpHeader::toString)
                .reduce((header1, header2) -> header1 + "\n" + header2)
                .orElse(""); // use reduce to concatenate headers
        // Response Header
        List<HttpHeader> headersResponse = requestResponse.response().headers();
        String headersResponseToString = headersResponse.stream()
                .map(HttpHeader::toString)
                .reduce((header1, header2) -> header1 + "\n" + header2)
                .orElse(""); // use reduce to concatenate headers
        String firstLine = requestResponse.response().toString().split("\n")[0];
        headersResponseToString = firstLine + "\n" + headersResponseToString;
        // Request Body
        String requestBody = requestResponse.request().bodyToString();
        // Response Body
        String responseBody = "<redacted>";
        // Request response raw
        String tmp = "______ REQUEST ______\n";
        tmp += "GET Params\n";
        List<ParsedHttpParameter> params = requestResponse.request().parameters(HttpParameterType.URL);
        for (ParsedHttpParameter param : params) {
            tmp += param.name() + " | ";
        }
        tmp += "\nPOST Params\n";
        List<ParsedHttpParameter> paramsPost = requestResponse.request().parameters(HttpParameterType.BODY);
        for (ParsedHttpParameter param : paramsPost) {
            tmp += param.name() + " | ";
        }
        tmp += "\nHEADERS\n";
        for (HttpHeader header : headersRequest) {
            tmp += header.name() + " | ";
        }
        tmp += "\nCookie\n";
        List<ParsedHttpParameter> cookies = requestResponse.request().parameters(HttpParameterType.COOKIE);
        for (ParsedHttpParameter cookie : cookies) {
            tmp += cookie.name() + " | ";
        }

        tmp += "\n\n______ RESPONSE ______\n";

        // tmp += "\nPOST Params\n";
        // List<ParsedHttpParameter> paramsPostResponse =
        // requestResponse.response().parameters(HttpParameterType.BODY);
        // for (ParsedHttpParameter param: paramsPostResponse) {
        // tmp += param.name() + " | " ;
        // }
        tmp += "\nHEADERS\n";
        for (HttpHeader header : headersResponse) {
            tmp += header.name() + " | ";
        }
        tmp += "\nCookie\n";
        List<Cookie> cookiesResponse = requestResponse.response().cookies();
        for (Cookie cookie : cookiesResponse) {
            tmp += cookie.name() + " | ";
        }
        tmp += "\n\n______ RAW ______\n";
        // ------------------------------------------------
        // method
        excelFormatter.addData(ExcelFormatter.excelFormat(method));
        // host
        excelFormatter.addData(ExcelFormatter.excelFormat(host));
        // path
        excelFormatter.addData(ExcelFormatter.excelFormat(path));
        // request header
        excelFormatter.addData(ExcelFormatter.excelFormat(headersToString));
        // response header
        excelFormatter.addData(ExcelFormatter.excelFormat(headersResponseToString));
        // request body
        excelFormatter.addData(ExcelFormatter.excelFormat(requestBody));
        // response body
        excelFormatter.addData(ExcelFormatter.excelFormat(responseBody));
        // request response raw
        excelFormatter.addData(ExcelFormatter.excelFormat(tmp));
        // ------------------------------------------------
        // Copy to clipboard
        String data = excelFormatter.getData();
        StringSelection stringSelection = new StringSelection(data);
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        clipboard.setContents(stringSelection, null);
    }
}
