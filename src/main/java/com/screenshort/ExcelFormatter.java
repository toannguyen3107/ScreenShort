package com.screenshort;

import java.util.ArrayList;

public class ExcelFormatter {
    private StringBuilder data;

    public ExcelFormatter() {
        this.data = new StringBuilder();
    }

    public void addData(String data) {
        this.data.append(data).append("\t"); // Đảm bảo mỗi lần thêm sẽ là một ô mới
    }

    public String getData() {
        return this.data.toString().stripTrailing(); // Loại bỏ tab cuối cùng dư thừa
    }

    public static String excelFormat(String data) {
        // Loại bỏ khoảng trắng đầu
        data = data.stripLeading();
    
        // Giới hạn độ dài tối đa 29000 ký tự
        if (data.length() > 29000) {
            data = data.substring(0, 29000);
        }
    
        StringBuilder formattedData = new StringBuilder();
    
        for (char c : data.toCharArray()) {
            if (c < 0x20 && c != '\t' && c != '\n' && c != '\r') {
                continue; // Bỏ qua ký tự điều khiển
            }
            switch (c) {
                case '\t': 
                    formattedData.append("\\t"); // Encode tab
                    break;
                case '"': 
                    formattedData.append("\"\""); // Nhân đôi dấu "
                    break;
                case '<':
                    formattedData.append("&lt;"); // Encode <
                    break;
                case '>':
                    formattedData.append("&gt;"); // Encode >
                    break;
                case '&':
                    formattedData.append("&amp;"); // Encode &
                    break;
                case '\'':
                    formattedData.append("&#39;"); // Encode '
                    break;
                default:
                    formattedData.append(c);
                    break;
            }
        }
    
        // Giới hạn tổng độ dài là 500000 ký tự
        if (formattedData.length() > 35000) {
            formattedData.setLength(35000);
        }
    
        return "\"" + formattedData.toString() + "\"";
    }    
    public static byte[] filterValidASCII(byte[] data) {
        ArrayList<Byte> validBytes = new ArrayList<>();
        
        for (byte b : data) {
            // Giữ lại ký tự ASCII hợp lệ (từ 0x20 đến 0x7E) và xuống dòng (\n, \r)
            if ((b >= 0x20 && b <= 0x7E) || b == '\n' || b == '\r') {
                validBytes.add(b);
            }
        }

        // Convert ArrayList<Byte> thành byte[]
        byte[] result = new byte[validBytes.size()];
        for (int i = 0; i < validBytes.size(); i++) {
            result[i] = validBytes.get(i);
        }
        return result;
    }
}
