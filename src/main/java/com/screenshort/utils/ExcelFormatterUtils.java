package com.screenshort.utils;

import burp.api.montoya.http.HttpService;
import burp.api.montoya.http.message.Cookie;
import burp.api.montoya.http.message.HttpHeader;
import burp.api.montoya.http.message.HttpRequestResponse;
import burp.api.montoya.http.message.params.HttpParameterType;
import burp.api.montoya.http.message.params.ParsedHttpParameter;

import org.json.JSONException;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Utilities for formatting HTTP request/response data for Excel.
 */
public final class ExcelFormatterUtils {

    private ExcelFormatterUtils() {
        // Utility class - prevent instantiation
    }

    /**
     * Checks if the given byte array contains binary data.
     * Binary data is detected by checking the ratio of non-printable characters.
     *
     * @param data The byte array to check
     * @return true if the data appears to be binary, false otherwise
     */
    public static boolean isBinaryContent(byte[] data) {
        if (data == null || data.length == 0) {
            return false;
        }

        int nonPrintableCount = 0;
        int checkLength = Math.min(data.length, 8192); // Check first 8KB for performance

        for (int i = 0; i < checkLength; i++) {
            byte b = data[i];
            // Check for null bytes or control characters (except tab, newline, carriage return)
            if (b == 0 || (b < 32 && b != 9 && b != 10 && b != 13) || b == 127) {
                nonPrintableCount++;
            }
        }

        double ratio = (double) nonPrintableCount / checkLength;
        return ratio > Constants.BINARY_THRESHOLD;
    }

    /**
     * Checks if the given string contains binary data.
     *
     * @param data The string to check
     * @return true if the data appears to be binary, false otherwise
     */
    public static boolean isBinaryContent(String data) {
        if (data == null || data.isEmpty()) {
            return false;
        }
        return isBinaryContent(data.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Formats data for Excel, handling special characters and length limits.
     * This method filters out problematic control characters while preserving
     * newlines for readability.
     *
     * @param data The data to format
     * @return Formatted string suitable for Excel
     */
    public static String excelFormat(String data) {
        if (data == null) {
            return "";
        }
        data = data.stripLeading();
        if (data.length() > Constants.MAX_EXCEL_CELL_LENGTH) {
            data = data.substring(0, Constants.MAX_EXCEL_CELL_LENGTH);
        }

        StringBuilder formattedData = new StringBuilder();
        for (char c : data.toCharArray()) {
            switch (c) {
                case '\t':
                    formattedData.append("\\t");
                    break;
                case '\n':
                case '\r':
                    // Keep newlines for readability - Excel handles them in quoted cells
                    formattedData.append(c);
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
                    // Skip problematic control characters (ASCII 0-31 except tab/newline, and 127)
                    if (c >= 32 && c != 127) {
                        formattedData.append(c);
                    }
                    break;
            }
        }
        return "\"" + formattedData.toString() + "\"";
    }

    /**
     * Pretty-prints a JSON string with indentation.
     *
     * @param jsonString The JSON string to format
     * @return Formatted JSON string, or original string if not valid JSON
     */
    private static String prettyPrintJson(String jsonString) {
        if (jsonString == null || jsonString.trim().isEmpty()) {
            return "";
        }
        try {
            JSONObject jsonObject = new JSONObject(jsonString);
            return jsonObject.toString(2);
        } catch (JSONException e) {
            // Not a valid JSON object, return original
            return jsonString;
        }
    }

    /**
     * Formats HttpRequestResponse for Excel with full body content.
     *
     * @param requestResponse The request/response data
     * @return Tab-separated string ready for Excel paste
     */
    public static String formatRequestResponseForExcel(HttpRequestResponse requestResponse) {
        return formatRequestResponse(requestResponse, true);
    }

    /**
     * Formats HttpRequestResponse for Excel with body content redacted.
     *
     * @param requestResponse The request/response data
     * @return Tab-separated string ready for Excel paste
     */
    public static String formatRequestResponseForExcelNoBody(HttpRequestResponse requestResponse) {
        return formatRequestResponse(requestResponse, false);
    }

    /**
     * Common method for formatting request/response data.
     *
     * @param requestResponse The request/response data
     * @param includeBody     Whether to include body content
     * @return Tab-separated string ready for Excel paste
     */
    private static String formatRequestResponse(HttpRequestResponse requestResponse, boolean includeBody) {
        StringBuilder data = new StringBuilder();

        // Method
        String method = requestResponse.request().method();
        data.append(excelFormat(method)).append(Constants.EXCEL_SEPARATOR);

        // Host
        HttpService httpService = requestResponse.httpService();
        String host = httpService.host() + ":" + httpService.port();
        data.append(excelFormat(host)).append(Constants.EXCEL_SEPARATOR);

        // Path (without query string)
        String path = requestResponse.request().path();
        int questionMarkIndex = path.indexOf('?');
        if (questionMarkIndex != -1) {
            path = path.substring(0, questionMarkIndex);
        }
        data.append(excelFormat(path)).append(Constants.EXCEL_SEPARATOR);

        // Full request
        String fullPath = requestResponse.request().path();
        String requestSection = formatRequestSection(requestResponse, method, fullPath, includeBody);
        data.append(excelFormat(requestSection)).append(Constants.EXCEL_SEPARATOR);

        // Full response
        String responseSection = formatResponseSection(requestResponse, includeBody);
        data.append(excelFormat(responseSection)).append(Constants.EXCEL_SEPARATOR);

        // Request body
        String requestBody;
        if (!includeBody) {
            requestBody = Constants.REDACTED_TEXT;
        } else {
            byte[] requestBodyBytes = requestResponse.request().body().getBytes();
            if (isBinaryContent(requestBodyBytes)) {
                requestBody = Constants.BINARY_DATA_TEXT;
            } else {
                requestBody = prettyPrintJson(new String(requestBodyBytes, StandardCharsets.UTF_8));
            }
        }
        data.append(excelFormat(requestBody)).append(Constants.EXCEL_SEPARATOR);

        // Response body
        String responseBody;
        if (!includeBody) {
            responseBody = Constants.REDACTED_TEXT;
        } else {
            byte[] responseBodyBytes = requestResponse.response().body().getBytes();
            if (isBinaryContent(responseBodyBytes)) {
                responseBody = Constants.BINARY_DATA_TEXT;
            } else {
                responseBody = prettyPrintJson(new String(responseBodyBytes, StandardCharsets.UTF_8));
            }
        }
        data.append(excelFormat(responseBody)).append(Constants.EXCEL_SEPARATOR);

        // Raw summary
        String rawSummary = buildRawSummary(requestResponse);
        data.append(excelFormat(rawSummary));

        return data.toString();
    }

    /**
     * Formats the request section with headers and optionally body.
     */
    private static String formatRequestSection(HttpRequestResponse requestResponse, String method, String path, boolean includeBody) {
        List<HttpHeader> headers = requestResponse.request().headers();
        String headersString = headers.stream()
                .map(HttpHeader::toString)
                .collect(Collectors.joining("\n"));

        String firstLine = method + " " + path + " " + requestResponse.request().httpVersion() + "\n";

        if (includeBody) {
            byte[] bodyBytes = requestResponse.request().body().getBytes();
            String prettyBody;
            if (isBinaryContent(bodyBytes)) {
                prettyBody = "\n" + Constants.BINARY_DATA_TEXT;
            } else {
                String bodyString = new String(bodyBytes, StandardCharsets.UTF_8);
                prettyBody = "\n" + prettyPrintJson(bodyString);
            }
            return firstLine + headersString + "\n" + prettyBody;
        } else {
            return firstLine + headersString + "\n\n" + Constants.REDACTED_TEXT;
        }
    }

    /**
     * Formats the response section with headers and optionally body.
     */
    private static String formatResponseSection(HttpRequestResponse requestResponse, boolean includeBody) {
        List<HttpHeader> headers = requestResponse.response().headers();
        String headersString = headers.stream()
                .map(HttpHeader::toString)
                .collect(Collectors.joining("\n"));

        String firstLine = requestResponse.response().toString().split("\n", 2)[0];

        if (includeBody) {
            byte[] bodyBytes = requestResponse.response().body().getBytes();
            String prettyBody;
            if (isBinaryContent(bodyBytes)) {
                prettyBody = "\n" + Constants.BINARY_DATA_TEXT;
            } else {
                String bodyString = new String(bodyBytes, StandardCharsets.UTF_8);
                prettyBody = "\n" + prettyPrintJson(bodyString);
            }
            return firstLine + "\n" + headersString + "\n" + prettyBody;
        } else {
            return firstLine + "\n" + headersString + "\n\n" + Constants.REDACTED_TEXT;
        }
    }

    /**
     * Builds the raw summary section with parameter and header names.
     */
    private static String buildRawSummary(HttpRequestResponse requestResponse) {
        StringBuilder summary = new StringBuilder();

        summary.append("______ REQUEST ______\n");

        // GET Params
        summary.append("GET Params\n");
        appendParameterNames(summary, requestResponse.request().parameters(HttpParameterType.URL));

        // POST Params
        summary.append("POST Params\n");
        appendParameterNames(summary, requestResponse.request().parameters(HttpParameterType.BODY));

        // Request Headers
        summary.append("HEADERS\n");
        appendHeaderNames(summary, requestResponse.request().headers());

        // Request Cookies
        summary.append("Cookie\n");
        appendParameterNames(summary, requestResponse.request().parameters(HttpParameterType.COOKIE));

        summary.append("\n______ RESPONSE ______\n");

        // Response Headers
        summary.append("HEADERS\n");
        appendHeaderNames(summary, requestResponse.response().headers());

        // Response Cookies
        summary.append("COOKIES\n");
        appendCookieNames(summary, requestResponse.response().cookies());

        summary.append("\n______ RAW ______\n");

        return summary.toString();
    }

    /**
     * Appends numbered parameter names to the summary.
     */
    private static void appendParameterNames(StringBuilder summary, List<ParsedHttpParameter> params) {
        for (int i = 0; i < params.size(); i++) {
            summary.append(i + 1).append(". ").append(params.get(i).name()).append(" | ");
        }
        if (!params.isEmpty()) {
            summary.setLength(summary.length() - 3); // Remove trailing " | "
        }
        summary.append("\n");
    }

    /**
     * Appends numbered header names to the summary.
     */
    private static void appendHeaderNames(StringBuilder summary, List<HttpHeader> headers) {
        for (int i = 0; i < headers.size(); i++) {
            summary.append(i + 1).append(". ").append(headers.get(i).name()).append(" | ");
        }
        if (!headers.isEmpty()) {
            summary.setLength(summary.length() - 3); // Remove trailing " | "
        }
        summary.append("\n");
    }

    /**
     * Appends numbered cookie names to the summary.
     */
    private static void appendCookieNames(StringBuilder summary, List<Cookie> cookies) {
        for (int i = 0; i < cookies.size(); i++) {
            summary.append(i + 1).append(". ").append(cookies.get(i).name()).append(" | ");
        }
        if (!cookies.isEmpty()) {
            summary.setLength(summary.length() - 3); // Remove trailing " | "
        }
        summary.append("\n");
    }

    /**
     * Copies text to the system clipboard.
     * Delegates to ClipboardUtils for centralized clipboard handling.
     *
     * @param data The text to copy
     */
    public static void copyToClipboard(String data) {
        ClipboardUtils.copyToClipboard(data);
    }
}
