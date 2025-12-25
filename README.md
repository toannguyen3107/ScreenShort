# ScreenShort

ScreenShort là một Burp Suite Extension được thiết kế để nâng cao hiệu suất làm việc của pentesters và security researchers. Extension cung cấp các tính năng chụp màn hình, định dạng dữ liệu HTTP và xuất file một cách nhanh chóng.

## Phiên bản hiện tại: 1.6.6

### Lịch sử cập nhật:
*   **1.6.6**: Cập nhật và tối ưu hóa code
*   **1.6.4**: Refactor code trong CustomMessageEditorHotKey.java và thêm HotKey cho PCopy
*   **1.6.03**: Cập nhật PCopy với giao diện đẹp hơn cho request/response

## Tính năng chính

### 1. Screenshot với Annotation
Chụp màn hình HTTP request/response với khả năng chú thích (annotation):
*   **Annotate Component (Normal)**: Chụp màn hình vùng được chọn
*   **Annotate Full Req/Res**: Chụp toàn bộ request/response

### 2. PCopy (Pretty Copy)
Định dạng và copy HTTP request/response sang Excel một cách đẹp mắt:
*   **PCopy with body**: Copy cả body của response
*   **PCopy no body**: Copy không kèm body của response

### 3. Export File
Xuất dữ liệu HTTP ra file JSON:
*   Hỗ trợ xuất request/response ra file
*   Có thể chọn đường dẫn mặc định để lưu file

## Phím tắt (Hotkeys)

| Phím tắt | Chức năng |
|----------|-----------|
| `Ctrl+Shift+S` | Chụp màn hình vùng được chọn |
| `Ctrl+Shift+Space` | Chụp toàn bộ request/response |
| `Ctrl+Alt+Space` | PCopy kèm body response |
| `Ctrl+Alt+C` | PCopy không kèm body response |
| `Ctrl+Alt+V` | Export dữ liệu ra file |

## Cấu trúc dự án

```
com.screenshort/
├── ScreenShort.java              # Main extension class
├── GUI.java                      # Context menu provider
└── utils/
    ├── CustomMessageEditorHotKey.java  # Hotkey handler
    ├── ScreenshotUtils.java            # Screenshot logic
    ├── ExcelFormatterUtils.java        # Excel formatting
    ├── GenDataToJson.java              # JSON export
    └── MenuActionHandler.java          # Menu action handler
```

## Yêu cầu hệ thống

*   **Java Development Kit (JDK)**: Version 21 trở lên
*   **Burp Suite**: Professional hoặc Community Edition
*   **Maven**: Để build project

## Dependencies

*   **Burp Montoya API**: 2025.4
*   **JSON**: 20240303
*   **Jackson Databind**: 2.19.0
*   **JUnit**: 3.8.1 (for testing)

## Cài đặt

### 1. Build từ source code

```bash
# Clone repository
git clone https://github.com/toannguyen3107/ScreenShort.git
cd ScreenShort

# Build project với Maven
mvn clean package

# File JAR sẽ được tạo tại: target/Screenshot-1.6.6.jar
```

### 2. Cài đặt vào Burp Suite

1. Mở Burp Suite
2. Đi tới tab **Extensions**
3. Click **Add**
4. Chọn file JAR: `target/Screenshot-1.6.6.jar`
5. Click **Next** để load extension

## Sử dụng

### Screenshot
1. Click chuột phải vào bất kỳ request/response nào trong Burp Suite
2. Chọn **Screenshot** → **Annotate Component (Normal)** hoặc **Annotate Full Req/Res**
3. Hoặc sử dụng phím tắt `Ctrl+Shift+S` hoặc `Ctrl+Shift+Space`

### PCopy (Copy to Excel)
1. Click chuột phải vào request/response
2. Chọn **PCopy** → Chọn loại copy phù hợp
3. Dữ liệu đã được format sẽ được copy vào clipboard
4. Paste vào Excel để xem kết quả

### Export File
1. Click chuột phải vào request/response
2. Chọn **Export File** → **Export File**
3. Chọn vị trí lưu file (hoặc sử dụng đường dẫn mặc định)

## Testing

Chạy unit tests:

```bash
mvn test
```

## Contributing

@quangit, @h4x0rl33tx

## Author

@t0ann9uy3n (Toan Nguyen)

## License

Copyright @toancse

---

**Note**: Extension này được phát triển để hỗ trợ công việc security testing và penetration testing một cách hợp pháp. Vui lòng chỉ sử dụng trên các hệ thống mà bạn có quyền kiểm tra.
