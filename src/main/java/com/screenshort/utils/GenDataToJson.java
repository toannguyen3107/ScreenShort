package com.screenshort.utils;

import burp.api.montoya.core.ByteArray;
import burp.api.montoya.http.message.HttpHeader;
import burp.api.montoya.http.message.HttpRequestResponse;
import burp.api.montoya.http.message.params.HttpParameterType;
import burp.api.montoya.http.message.params.ParsedHttpParameter;
import java.awt.datatransfer.StringSelection;
import java.nio.charset.StandardCharsets;
import java.awt.datatransfer.Clipboard;
import java.awt.Toolkit;
import java.util.List;
import java.util.UUID;
public class GenDataToJson {
    public static String formatRequestResponseToJson(HttpRequestResponse requestResponse) {
        if (requestResponse == null) {
            return "{}";
        }
        StringBuilder jsonBuilder = new StringBuilder();
        // ---------------- headers ----------------
        jsonBuilder.append("{");
        jsonBuilder.append("\"headers\": {");
        List<HttpHeader> headers = requestResponse.request().headers();
        for (int i = 0; i < headers.size(); i++) {
            HttpHeader header = headers.get(i);
            String headerValue = header.value();
            headerValue = escapeJsonString(headerValue);
            jsonBuilder.append("\"").append(escapeJsonString(header.name())).append("\": \"").append(headerValue).append("\"");
            if (i < headers.size() - 1) {
                jsonBuilder.append(", ");
            }
        }
        jsonBuilder.append("},");
        // ---------------- method ----------------
        jsonBuilder.append("\"method\": \"").append(escapeJsonString(requestResponse.request().method())).append("\",");
        // ---------------- query ----------------
        jsonBuilder.append("\"query\": [");
        List<ParsedHttpParameter> queryParams = requestResponse.request().parameters(HttpParameterType.URL);
        for (int i = 0; i < queryParams.size(); i++) {
            String paramName = queryParams.get(i).name();
            paramName = escapeJsonString(paramName);
            jsonBuilder.append("\"").append(paramName).append("\"");
            if (i < queryParams.size() - 1) {
                jsonBuilder.append(", ");
            }
        }
        jsonBuilder.append("],");
        jsonBuilder.append("\"modules\": [],");
        jsonBuilder.append("\"extract\": [],");
        jsonBuilder.append("\"replace\": [],");
        String uuid4 = UUID.randomUUID().toString();
        jsonBuilder.append("\"name\": \"").append(uuid4).append("\",");
        jsonBuilder.append("\"index\": \"change_this\",");
        jsonBuilder.append("\"body\": ");
        String contentType = requestResponse.request().headerValue("Content-Type");
        ByteArray requestBodyBytes = requestResponse.request().body();
        if (contentType != null && contentType.contains("application/json") && requestBodyBytes.length() > 0) {
             String bodyString = new String(requestBodyBytes.getBytes(), StandardCharsets.UTF_8);
             if (bodyString.length() > 0 && bodyString.charAt(0) == '\ufeff') {
                 bodyString = bodyString.substring(1);
             }
             jsonBuilder.append(bodyString);
        } else {
             jsonBuilder.append("null");
        }
        jsonBuilder.append(",");
        jsonBuilder.append("\"isProxy\": true,");
        String url = requestResponse.request().url().toString();
        url = escapeJsonString(url);
        
        jsonBuilder.append("\"url\": \"").append(url).append("\"");
        jsonBuilder.append("}");
        return jsonBuilder.toString();
    }
    private static String escapeJsonString(String value) {
        if (value == null) {
            return null;
        }
        StringBuilder escaped = new StringBuilder();
        for (char c : value.toCharArray()) {
            switch (c) {
                case '"':
                    escaped.append("\\\"");
                    break;
                case '\\':
                    escaped.append("\\\\");
                    break;
                case '\b':
                    escaped.append("\\b");
                    break;
                case '\f':
                    escaped.append("\\f");
                    break;
                case '\n':
                    escaped.append("\\n");
                    break;
                case '\r':
                    escaped.append("\\r");
                    break;
                case '\t':
                    escaped.append("\\t");
                    break;
                default:
                     if (c >= 0x00 && c <= 0x1F) { 
                         escaped.append(String.format("\\u%04x", (int) c));
                     } else {
                        escaped.append(c);
                     }
                    break;
            }
        }
        return escaped.toString();
    }
    public static void copyToClipboard(String data) {
        StringSelection stringSelection = new StringSelection(data);
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        clipboard.setContents(stringSelection, null);
    }
}