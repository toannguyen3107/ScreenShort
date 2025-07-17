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
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.prefs.Preferences;
public class GenDataToJson {
    private static final String PREFS_KEY_DEFAULT_PATH = "default_export_path";
    private static final Preferences prefs = Preferences.userNodeForPackage(GenDataToJson.class);
    
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
    
    /**
     * Create a copy of the HttpRequestResponse in temporary file.
     * This method is used to save the HttpRequestResponse object to a temporary file, 
     * so that it is no longer held in memory. Extensions can use this method to 
     * convert HttpRequest objects into a form suitable for long-term usage.
     * 
     * @param requestResponse The HttpRequestResponse to save
     * @return A new ByteArray instance stored in temporary file, or null if operation failed
     */
    public static ByteArray copyToTempFile(HttpRequestResponse requestResponse) {
        if (requestResponse == null) {
            JOptionPane.showMessageDialog(null, "No request/response data to save.", "Error", JOptionPane.ERROR_MESSAGE);
            return null;
        }
        
        // Show file chooser dialog
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Save HttpRequestResponse to file");
        fileChooser.setSelectedFile(new File("request_response_" + System.currentTimeMillis() + ".dat"));
        
        // Set default path if exists
        String defaultPath = prefs.get(PREFS_KEY_DEFAULT_PATH, null);
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
                // Get raw bytes from request only
                byte[] requestBytes = requestResponse.request().toByteArray().getBytes();
                
                // Write to file
                try (FileOutputStream fos = new FileOutputStream(fileToSave)) {
                    fos.write(requestBytes);
                    fos.flush();
                    
                    JOptionPane.showMessageDialog(null, 
                        "File saved successfully to: " + fileToSave.getAbsolutePath(), 
                        "Success", 
                        JOptionPane.INFORMATION_MESSAGE);
                    
                    return ByteArray.byteArray(requestBytes);
                }
                
            } catch (IOException e) {
                JOptionPane.showMessageDialog(null, 
                    "Error saving file: " + e.getMessage(), 
                    "Error", 
                    JOptionPane.ERROR_MESSAGE);
                return null;
            }
        }
        
        return null; // User cancelled or operation failed
    }
    
    public static void exportFile(List<HttpRequestResponse> listReqRes){
        for (HttpRequestResponse reqRes: listReqRes) {
            copyToTempFile(reqRes);
        }
    }
    public static void chooseDefaultPath(){
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Choose Default Export Path");
        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        
        // Set current default path if exists
        String currentDefaultPath = prefs.get(PREFS_KEY_DEFAULT_PATH, null);
        if (currentDefaultPath != null) {
            File currentDir = new File(currentDefaultPath);
            if (currentDir.exists() && currentDir.isDirectory()) {
                fileChooser.setCurrentDirectory(currentDir);
            }
        }
        
        int userSelection = fileChooser.showOpenDialog(null);
        
        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File selectedDir = fileChooser.getSelectedFile();
            prefs.put(PREFS_KEY_DEFAULT_PATH, selectedDir.getAbsolutePath());
            
            JOptionPane.showMessageDialog(null, 
                "Default export path set to: " + selectedDir.getAbsolutePath(), 
                "Default Path Set", 
                JOptionPane.INFORMATION_MESSAGE);
        }
    }
    
    /**
     * Get the current default export path
     * @return Default path or null if not set
     */
    public static String getDefaultPath() {
        return prefs.get(PREFS_KEY_DEFAULT_PATH, null);
    }
    
    /**
     * Set the default export path
     * @param path The path to set as default
     */
    public static void setDefaultPath(String path) {
        if (path != null) {
            prefs.put(PREFS_KEY_DEFAULT_PATH, path);
        } else {
            prefs.remove(PREFS_KEY_DEFAULT_PATH);
        }
    } 
}