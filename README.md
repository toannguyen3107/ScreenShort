# ScreenShort

ScreenShort is a Burp Suite Extension designed to enhance the productivity of pentesters and security researchers. The extension provides features for capturing screenshots, formatting HTTP data, and exporting files quickly.

## Current Version: 1.6.6

### Version History:
*   **1.6.6**: Code refactoring and optimization - added new utility classes
*   **1.6.4**: Refactored code in CustomMessageEditorHotKey.java and added HotKey for PCopy
*   **1.6.03**: Updated PCopy with improved UI for request/response

## Main Features

### 1. Screenshot with Annotation
Capture screenshots of HTTP request/response with annotation capabilities:
*   **Annotate Component (Normal)**: Capture selected area screenshot
*   **Annotate Full Req/Res**: Capture full request/response screenshot

> **Important Note:** If you are using screenshot in **Request Edited** mode and the screenshot is incorrect or showing wrong content, press **`Ctrl+O`** (Send to Organizer in Burp Suite) first, then capture the screenshot again to get the correct result.

### 2. PCopy (Pretty Copy)
Format and copy HTTP request/response to Excel in a beautiful format:
*   **PCopy with body**: Copy including response body
*   **PCopy no body**: Copy without response body

### 3. Export File
Export HTTP data to file:
*   Support exporting request/response to file
*   Configurable default save path

## Hotkeys

| Shortcut | Function |
|----------|-----------|
| `Ctrl+Shift+S` | Capture selected area screenshot |
| `Ctrl+Shift+Space` | Capture full request/response screenshot |
| `Ctrl+Alt+Space` | PCopy with response body |
| `Ctrl+Alt+X` | PCopy without response body |
| `Ctrl+Alt+V` | Export data to file |

## Project Structure

```
com.screenshort/
├── ScreenShort.java              # Main extension class
├── GUI.java                      # Context menu provider
└── utils/
    ├── Constants.java            # Centralized constants
    ├── ClipboardUtils.java       # Clipboard operations
    ├── ComponentFinder.java      # Component discovery utilities
    ├── AnnotationEditor.java     # Screenshot annotation editor
    ├── ScreenshotUtils.java      # Screenshot capture logic
    ├── ExcelFormatterUtils.java  # Excel formatting
    ├── GenDataToJson.java        # JSON/File export
    ├── MenuActionHandler.java    # Menu action handler
    └── CustomMessageEditorHotKey.java  # Hotkey handler
```

## System Requirements

*   **Java Development Kit (JDK)**: Version 21 or higher
*   **Burp Suite**: Professional or Community Edition
*   **Maven**: For building the project

## Dependencies

*   **Burp Montoya API**: 2025.4
*   **JSON**: 20240303
*   **Jackson Databind**: 2.19.0
*   **JUnit**: 3.8.1 (for testing)

## Installation

### 1. Build from source code

```bash
# Clone repository
git clone https://github.com/toannguyen3107/ScreenShort.git
cd ScreenShort

# Build project with Maven
mvn clean package

# JAR file will be generated at: target/Screenshot-1.6.6.jar
```

### 2. Install into Burp Suite

1. Open Burp Suite
2. Navigate to **Extensions** tab
3. Click **Add**
4. Select JAR file: `target/Screenshot-1.6.6.jar`
5. Click **Next** to load the extension

## Usage

### Screenshot
1. Right-click on any request/response in Burp Suite
2. Select **Screenshot** → **Annotate Component (Normal)** or **Annotate Full Req/Res**
3. Or use hotkeys `Ctrl+Shift+S` or `Ctrl+Shift+Space`

### PCopy (Copy to Excel)
1. Right-click on request/response
2. Select **PCopy** → Choose appropriate copy type
3. Formatted data will be copied to clipboard
4. Paste into Excel to view results

### Export File
1. Right-click on request/response
2. Select **Export File** → **Export File**
3. Choose save location (or use default path)

## CI/CD

This project uses GitHub Actions for automated build and release:
- On every push to `main` branch, the project is automatically built
- A new release is created with filename format: `screenshort_DD_MM_YYYY.jar`

## Testing

Run unit tests:

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

**Note**: This extension is developed to support legitimate security testing and penetration testing activities. Please only use it on systems you have permission to test.
