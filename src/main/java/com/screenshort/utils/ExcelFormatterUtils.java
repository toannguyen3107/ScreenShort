package com.screenshort.utils;

import burp.api.montoya.http.HttpService;
import burp.api.montoya.http.message.Cookie;
import burp.api.montoya.http.message.HttpHeader;
import burp.api.montoya.http.message.HttpRequestResponse;
import burp.api.montoya.http.message.params.HttpParameterType;
import burp.api.montoya.http.message.params.ParsedHttpParameter;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;
import org.json.JSONObject;
import org.json.JSONException;

public class ExcelFormatterUtils {
    private static final String SEPARATOR = "\t";

    /**
     * Định dạng dữ liệu cho Excel, xử lý ký tự đặc biệt và giới hạn độ dài.
     * (Lấy từ phương thức static excelFormat trong ExcelFormatter cũ)
     */
    public static String excelFormat(String data) {
        if (data == null) {
            return "";
        }
        data = data.stripLeading();
        if (data.length() > 29000) {
            data = data.substring(0, 29000);
        }
        StringBuilder formattedData = new StringBuilder();
        for (char c : data.toCharArray()) {
            switch (c) {
                case '\t':
                    formattedData.append("\\t");
                    break;
                case '"':
                    formattedData.append("\"\"");
                    break;
                case '<':
                    formattedData.append("<");
                    break;
                case '>':
                    formattedData.append(">");
                    break;
                case '&':
                    formattedData.append("&");
                    break;
                case '\'':
                    formattedData.append("&#39;");
                    break;
                default:
                    formattedData.append(c);
                    break;
            }
        }
        return "\"" + formattedData.toString() + "\"";
    }

    /**
     * Pretty-prints a JSON string. If the input is not valid JSON, it returns the original string.
     */
    private static String prettyPrintJson(String jsonString) {
        if (jsonString == null || jsonString.trim().isEmpty()) {
            return "";
        }
        try {
            // Attempt to parse as JSONObject
            JSONObject jsonObject = new JSONObject(jsonString);
            return jsonObject.toString(2); // Indent with 2 spaces
        } catch (JSONException e) {
            // Not a valid JSON object, try as array if needed, or return original
            // For simplicity, we'll just return the original string if it's not a JSON object
            return jsonString;
        }
    }

    /**
     * Tạo chuỗi dữ liệu theo định dạng Excel từ HttpRequestResponse, bao gồm body.
     * 
     * @param requestResponse Dữ liệu HttpRequestResponse.
     * @return Chuỗi định dạng sẵn sàng copy.
     */
    public static String formatRequestResponseForExcel(HttpRequestResponse requestResponse) {
        StringBuilder data = new StringBuilder();
        // ------------- method ------------------
        String method = requestResponse.request().method();
        data.append(excelFormat(method)).append(SEPARATOR);
        // ------------- host ------------------
        HttpService httpService = requestResponse.httpService();
        String host = httpService.host() + ":" + httpService.port();
        data.append(excelFormat(host)).append(SEPARATOR);
        // ------------- path ------------------
        String path = requestResponse.request().path();
        int questionMarkIndex = path.indexOf('?');
        if (questionMarkIndex != -1) {
            path = path.substring(0, questionMarkIndex);
        }
        data.append(excelFormat(path)).append(SEPARATOR);
        // ------------- request ------------------
        List<HttpHeader> headersRequest = requestResponse.request().headers();
        String headersToString = headersRequest.stream()
                .map(HttpHeader::toString)
                .collect(Collectors.joining("\n"));
        String firstLineOfReqHead = method + " " + path + " " + requestResponse.request().httpVersion() + "\n";
        String requestBodyString = new String(requestResponse.request().body().getBytes(), StandardCharsets.UTF_8);
        String endString = "\n" + prettyPrintJson(requestBodyString);
        headersToString = firstLineOfReqHead + headersToString +"\n"+ endString;
        data.append(excelFormat(headersToString)).append(SEPARATOR);
        // ------------- response ------------------
        List<HttpHeader> headersResponse = requestResponse.response().headers();
        String headersResponseToString = headersResponse.stream()
                .map(HttpHeader::toString)
                .collect(Collectors.joining("\n"));
        String firstLineRes = requestResponse.response().toString().split("\n", 2)[0];
        String responseBodyString = new String(requestResponse.response().body().getBytes(), StandardCharsets.UTF_8);
        String endString2 = "\n" + prettyPrintJson(responseBodyString);
        headersResponseToString = firstLineRes + "\n" + headersResponseToString + "\n" + endString2;
        data.append(excelFormat(headersResponseToString)).append(SEPARATOR);
        // ------------- body ------------------
        String requestBody = prettyPrintJson(new String(requestResponse.request().body().getBytes(), StandardCharsets.UTF_8));
        data.append(excelFormat(requestBody)).append(SEPARATOR);
        String responseBody = prettyPrintJson(new String(requestResponse.response().body().getBytes(), StandardCharsets.UTF_8));
        data.append(excelFormat(responseBody)).append(SEPARATOR);
        // ------------- raw summary ------------------
        StringBuilder rawSummary = new StringBuilder();
        rawSummary.append("______ REQUEST ______\n");
        rawSummary.append("GET Params\n");
        // Đánh số và nối tên GET Params
        List<ParsedHttpParameter> getParams = requestResponse.request().parameters(HttpParameterType.URL);
        for (int i = 0; i < getParams.size(); i++) {
            rawSummary.append(i + 1).append(". ").append(getParams.get(i).name()).append(" | ");
        }
        if (!getParams.isEmpty()) {
            rawSummary.setLength(rawSummary.length() - 3); // Xóa " | " cuối cùng nếu có params
        }
        rawSummary.append("\n");
        rawSummary.append("POST Params\n");
        // Đánh số và nối tên POST Params
        List<ParsedHttpParameter> postParams = requestResponse.request().parameters(HttpParameterType.BODY);
        for (int i = 0; i < postParams.size(); i++) {
            rawSummary.append(i + 1).append(". ").append(postParams.get(i).name()).append(" | ");
        }
        if (!postParams.isEmpty()) {
            rawSummary.setLength(rawSummary.length() - 3); // Xóa " | " cuối cùng nếu có params
        }
        rawSummary.append("\n");
        rawSummary.append("HEADERS\n");
        // Đánh số và nối tên Request Headers
        List<HttpHeader> requestHeaders = requestResponse.request().headers();
        for (int i = 0; i < requestHeaders.size(); i++) {
            rawSummary.append(i + 1).append(". ").append(requestHeaders.get(i).name()).append(" | ");
        }
        if (!requestHeaders.isEmpty()) {
            rawSummary.setLength(rawSummary.length() - 3); // Xóa " | " cuối cùng nếu có headers
        }
        rawSummary.append("\n");
        rawSummary.append("Cookie\n");
        // Đánh số và nối tên Request Cookies
        List<ParsedHttpParameter> requestCookies = requestResponse.request().parameters(HttpParameterType.COOKIE);
        for (int i = 0; i < requestCookies.size(); i++) {
            rawSummary.append(i + 1).append(". ").append(requestCookies.get(i).name()).append(" | ");
        }
        if (!requestCookies.isEmpty()) {
            rawSummary.setLength(rawSummary.length() - 3); // Xóa " | " cuối cùng nếu có cookies
        }
        rawSummary.append("\n\n");
        rawSummary.append("______ RESPONSE ______\n");
        rawSummary.append("HEADERS\n");
        // Đánh số và nối tên Response Headers
        List<HttpHeader> responseHeaders = requestResponse.response().headers();
        for (int i = 0; i < responseHeaders.size(); i++) {
            rawSummary.append(i + 1).append(". ").append(responseHeaders.get(i).name()).append(" | ");
        }
        if (!responseHeaders.isEmpty()) {
            rawSummary.setLength(rawSummary.length() - 3); // Xóa " | " cuối cùng nếu có headers
        }
        rawSummary.append("\n");
        rawSummary.append("COOKIES\n");
        // Đánh số và nối tên Response Cookies
        List<Cookie> responseCookies = requestResponse.response().cookies();
        for (int i = 0; i < responseCookies.size(); i++) {
            rawSummary.append(i + 1).append(". ").append(responseCookies.get(i).name()).append(" | ");
        }
        if (!responseCookies.isEmpty()) {
            rawSummary.setLength(rawSummary.length() - 3); // Xóa " | " cuối cùng nếu có cookies
        }
        rawSummary.append("\n\n");
        rawSummary.append("______ RAW ______\n");
        data.append(excelFormat(rawSummary.toString()));
        return data.toString();
    }

    public static String formatRequestResponseForExcelNoBody(HttpRequestResponse requestResponse) {
        StringBuilder data = new StringBuilder();
        // ------------- method ------------------
        String method = requestResponse.request().method();
        data.append(excelFormat(method)).append(SEPARATOR);
        // ------------- host ------------------
        HttpService httpService = requestResponse.httpService();
        String host = httpService.host() + ":" + httpService.port();
        data.append(excelFormat(host)).append(SEPARATOR);
        // ------------- path ------------------
        String path = requestResponse.request().path();
        int questionMarkIndex = path.indexOf('?');
        if (questionMarkIndex != -1) {
            path = path.substring(0, questionMarkIndex);
        }
        data.append(excelFormat(path)).append(SEPARATOR);
        // ------------- request ------------------
        List<HttpHeader> headersRequest = requestResponse.request().headers();
        String headersToString = headersRequest.stream()
                .map(HttpHeader::toString)
                .collect(Collectors.joining("\n"));
        String firstLineOfReqHead = method + " " + path + " " + requestResponse.request().httpVersion() + "\n";
        headersToString = firstLineOfReqHead + headersToString + "\n\nREDACTED";
        data.append(excelFormat(headersToString)).append(SEPARATOR);
        List<HttpHeader> headersResponse = requestResponse.response().headers();
        String headersResponseToString = headersResponse.stream()
                .map(HttpHeader::toString)
                .collect(Collectors.joining("\n"));
        String firstLineRes = requestResponse.response().toString().split("\n", 2)[0];
        headersResponseToString = firstLineRes + "\n" + headersResponseToString;
        data.append(excelFormat(headersResponseToString + "\n\nREDACTED")).append(SEPARATOR);
        String requestBody = "REDACTED";
        data.append(excelFormat(requestBody)).append(SEPARATOR);
        String responseBody = "REDACTED"; // Giữ nguyên REDACTED vì đây là hàm NoBody
        data.append(excelFormat(responseBody)).append(SEPARATOR);
        // ------------- raw summary ------------------
        StringBuilder rawSummary = new StringBuilder();
        rawSummary.append("______ REQUEST ______\n");
        rawSummary.append("GET Params\n");
        // Đánh số và nối tên GET Params
        List<ParsedHttpParameter> getParams = requestResponse.request().parameters(HttpParameterType.URL);
        for (int i = 0; i < getParams.size(); i++) {
            rawSummary.append(i + 1).append(". ").append(getParams.get(i).name()).append(" | ");
        }
        if (!getParams.isEmpty()) {
            rawSummary.setLength(rawSummary.length() - 3); // Xóa " | " cuối cùng nếu có params
        }
        rawSummary.append("\n");
        rawSummary.append("POST Params\n");
        // Đánh số và nối tên POST Params
        List<ParsedHttpParameter> postParams = requestResponse.request().parameters(HttpParameterType.BODY);
        for (int i = 0; i < postParams.size(); i++) {
            rawSummary.append(i + 1).append(". ").append(postParams.get(i).name()).append(" | ");
        }
        if (!postParams.isEmpty()) {
            rawSummary.setLength(rawSummary.length() - 3); // Xóa " | " cuối cùng nếu có params
        }
        rawSummary.append("\n");
        rawSummary.append("HEADERS\n");
        // Đánh số và nối tên Request Headers
        List<HttpHeader> requestCookies = requestResponse.request().headers();
        for (int i = 0; i < requestCookies.size(); i++) {
            rawSummary.append(i + 1).append(". ").append(requestCookies.get(i).name()).append(" | ");
        }
        if (!requestCookies.isEmpty()) {
            rawSummary.setLength(rawSummary.length() - 3); // Xóa " | " cuối cùng nếu có cookies
        }
        rawSummary.append("\n");
        rawSummary.append("Cookie\n");
        // Đánh số và nối tên Request Cookies
        List<ParsedHttpParameter> requestCookies1 = requestResponse.request().parameters(HttpParameterType.COOKIE);
        for (int i = 0; i < requestCookies1.size(); i++) {
            rawSummary.append(i + 1).append(". ").append(requestCookies1.get(i).name()).append(" | ");
        }
        if (!requestCookies1.isEmpty()) {
            rawSummary.setLength(rawSummary.length() - 3); // Xóa " | " cuối cùng nếu có cookies
        }
        rawSummary.append("\n\n");
        rawSummary.append("______ RESPONSE ______\n");
        rawSummary.append("HEADERS\n");
        // Đánh số và nối tên Response Headers
        List<HttpHeader> responseHeaders = requestResponse.response().headers();
        for (int i = 0; i < responseHeaders.size(); i++) {
            rawSummary.append(i + 1).append(". ").append(responseHeaders.get(i).name()).append(" | ");
        }
        if (!responseHeaders.isEmpty()) {
            rawSummary.setLength(rawSummary.length() - 3); // Xóa " | " cuối cùng nếu có headers
        }
        rawSummary.append("\n");
        rawSummary.append("COOKIES\n");
        // Đánh số và nối tên Response Cookies
        List<Cookie> responseCookies1 = requestResponse.response().cookies();
        for (int i = 0; i < responseCookies1.size(); i++) {
            rawSummary.append(i + 1).append(". ").append(responseCookies1.get(i).name()).append(" | ");
        }
        if (!responseCookies1.isEmpty()) {
            rawSummary.setLength(rawSummary.length() - 3); // Xóa " | " cuối cùng nếu có cookies
        }
        rawSummary.append("\n\n");
        rawSummary.append("______ RAW ______\n");
        data.append(excelFormat(rawSummary.toString()));
        return data.toString();
    }
    public static void copyToClipboard(String data) {
        StringSelection stringSelection = new StringSelection(data);
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        clipboard.setContents(stringSelection, null);
    }
}
