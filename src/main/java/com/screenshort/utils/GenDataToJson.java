package com.screenshort.utils;

import burp.api.montoya.core.ByteArray;
import burp.api.montoya.http.message.HttpHeader;
import burp.api.montoya.http.message.HttpRequestResponse;
import burp.api.montoya.http.message.params.HttpParameterType;
import burp.api.montoya.http.message.params.ParsedHttpParameter;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import java.util.prefs.Preferences;

/**
 * Utilities for generating JSON data and exporting HTTP requests to files.
 */
public final class GenDataToJson {

    private static final Preferences prefs = Preferences.userNodeForPackage(GenDataToJson.class);

    private GenDataToJson() {
        // Utility class - prevent instantiation
    }

    /**
     * Formats an HttpRequestResponse to JSON format.
     *
     * @param requestResponse The request/response to format
     * @return JSON string representation
     */
    public static String formatRequestResponseToJson(HttpRequestResponse requestResponse) {
        if (requestResponse == null) {
            return "{}";
        }

        StringBuilder jsonBuilder = new StringBuilder();
        jsonBuilder.append("{");

        // Headers
        appendHeaders(jsonBuilder, requestResponse);

        // Method
        jsonBuilder.append("\"method\": \"")
                .append(escapeJsonString(requestResponse.request().method()))
                .append("\",");

        // Query parameters
        appendQueryParams(jsonBuilder, requestResponse);

        // Standard fields
        jsonBuilder.append("\"modules\": [],");
        jsonBuilder.append("\"extract\": [],");
        jsonBuilder.append("\"replace\": [],");
        jsonBuilder.append("\"name\": \"").append(UUID.randomUUID().toString()).append("\",");
        jsonBuilder.append("\"index\": \"change_this\",");

        // Body
        appendBody(jsonBuilder, requestResponse);

        // Additional fields
        jsonBuilder.append("\"isProxy\": true,");

        // URL
        String url = escapeJsonString(requestResponse.request().url().toString());
        jsonBuilder.append("\"url\": \"").append(url).append("\"");

        jsonBuilder.append("}");
        return jsonBuilder.toString();
    }

    private static void appendHeaders(StringBuilder jsonBuilder, HttpRequestResponse requestResponse) {
        jsonBuilder.append("\"headers\": {");
        List<HttpHeader> headers = requestResponse.request().headers();
        for (int i = 0; i < headers.size(); i++) {
            HttpHeader header = headers.get(i);
            String headerValue = escapeJsonString(header.value());
            jsonBuilder.append("\"")
                    .append(escapeJsonString(header.name()))
                    .append("\": \"")
                    .append(headerValue)
                    .append("\"");
            if (i < headers.size() - 1) {
                jsonBuilder.append(", ");
            }
        }
        jsonBuilder.append("},");
    }

    private static void appendQueryParams(StringBuilder jsonBuilder, HttpRequestResponse requestResponse) {
        jsonBuilder.append("\"query\": [");
        List<ParsedHttpParameter> queryParams = requestResponse.request().parameters(HttpParameterType.URL);
        for (int i = 0; i < queryParams.size(); i++) {
            String paramName = escapeJsonString(queryParams.get(i).name());
            jsonBuilder.append("\"").append(paramName).append("\"");
            if (i < queryParams.size() - 1) {
                jsonBuilder.append(", ");
            }
        }
        jsonBuilder.append("],");
    }

    private static void appendBody(StringBuilder jsonBuilder, HttpRequestResponse requestResponse) {
        jsonBuilder.append("\"body\": ");
        String contentType = requestResponse.request().headerValue("Content-Type");
        ByteArray requestBodyBytes = requestResponse.request().body();

        if (contentType != null && contentType.contains("application/json") && requestBodyBytes.length() > 0) {
            String bodyString = new String(requestBodyBytes.getBytes(), StandardCharsets.UTF_8);
            // Remove BOM if present
            if (bodyString.length() > 0 && bodyString.charAt(0) == '\ufeff') {
                bodyString = bodyString.substring(1);
            }
            jsonBuilder.append(bodyString);
        } else {
            jsonBuilder.append("null");
        }
        jsonBuilder.append(",");
    }

    /**
     * Escapes a string for safe inclusion in JSON.
     *
     * @param value The string to escape
     * @return Escaped string
     */
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

    /**
     * Copies text to the system clipboard.
     * Delegates to ClipboardUtils for centralized clipboard handling.
     *
     * @param data The text to copy
     */
    public static void copyToClipboard(String data) {
        ClipboardUtils.copyToClipboard(data);
    }

    /**
     * Exports HTTP request data to a file.
     *
     * @param requestResponse The request/response to export
     * @return ByteArray of the saved data, or null if cancelled/failed
     */
    public static ByteArray copyToTempFile(HttpRequestResponse requestResponse) {
        if (requestResponse == null) {
            JOptionPane.showMessageDialog(null,
                    "No request/response data to save.",
                    "Error", JOptionPane.ERROR_MESSAGE);
            return null;
        }

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Save HttpRequestResponse to file");
        fileChooser.setSelectedFile(new File("request_response_" + System.currentTimeMillis() + ".req"));

        // Set default path if exists
        String defaultPath = prefs.get(Constants.PREFS_KEY_DEFAULT_PATH, null);
        if (defaultPath != null) {
            File defaultDir = new File(defaultPath);
            if (defaultDir.exists() && defaultDir.isDirectory()) {
                fileChooser.setCurrentDirectory(defaultDir);
            }
        }

        int userSelection = fileChooser.showSaveDialog(null);

        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File fileToSave = fileChooser.getSelectedFile();

            try {
                byte[] requestBytes = requestResponse.request().toByteArray().getBytes();

                try (FileOutputStream fos = new FileOutputStream(fileToSave)) {
                    fos.write(requestBytes);
                    fos.flush();

                    JOptionPane.showMessageDialog(null,
                            "File saved successfully to: " + fileToSave.getAbsolutePath(),
                            "Success", JOptionPane.INFORMATION_MESSAGE);

                    return ByteArray.byteArray(requestBytes);
                }
            } catch (IOException e) {
                JOptionPane.showMessageDialog(null,
                        "Error saving file: " + e.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
                return null;
            }
        }

        return null;
    }

    /**
     * Exports multiple HTTP requests to files.
     *
     * @param listReqRes List of request/responses to export
     */
    public static void exportFile(List<HttpRequestResponse> listReqRes) {
        for (HttpRequestResponse reqRes : listReqRes) {
            copyToTempFile(reqRes);
        }
    }

    /**
     * Opens a dialog to choose the default export path.
     */
    public static void chooseDefaultPath() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Choose Default Export Path");
        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

        String currentDefaultPath = prefs.get(Constants.PREFS_KEY_DEFAULT_PATH, null);
        if (currentDefaultPath != null) {
            File currentDir = new File(currentDefaultPath);
            if (currentDir.exists() && currentDir.isDirectory()) {
                fileChooser.setCurrentDirectory(currentDir);
            }
        }

        int userSelection = fileChooser.showOpenDialog(null);

        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File selectedDir = fileChooser.getSelectedFile();
            prefs.put(Constants.PREFS_KEY_DEFAULT_PATH, selectedDir.getAbsolutePath());

            JOptionPane.showMessageDialog(null,
                    "Default export path set to: " + selectedDir.getAbsolutePath(),
                    "Default Path Set", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    /**
     * Gets the current default export path.
     *
     * @return Default path or null if not set
     */
    public static String getDefaultPath() {
        return prefs.get(Constants.PREFS_KEY_DEFAULT_PATH, null);
    }

    /**
     * Sets the default export path.
     *
     * @param path The path to set as default
     */
    public static void setDefaultPath(String path) {
        if (path != null) {
            prefs.put(Constants.PREFS_KEY_DEFAULT_PATH, path);
        } else {
            prefs.remove(Constants.PREFS_KEY_DEFAULT_PATH);
        }
    }
}
