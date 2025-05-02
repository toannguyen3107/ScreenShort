# Your Extension Name (Example: Data Formatting Extension)

This is a simple extension for Burp Suite Community/Professional developed using the Burp Extension API (Montoya). This extension adds options to the context menu to help you quickly manipulate and extract data from HTTP Request/Response.

## Features

This extension currently provides the following functionalities via the context menu (right-click):

### "Screenshot" Menu

*   **Normal**: Capture the current Burp Suite window.
*   **Full**: Capture the entire Burp Suite work area.
*   **Full - Edited Request (Proxy Tool)**: Capture the entire Editor window for the modified request. (Currently not in use)
*   **Full - Original Request (Proxy Tool)**: Capture the entire Editor window for the original request. (Currently not in use)

### "PCopy" Menu (Only visible when HTTP Request/Response is selected)

These options help copy request/response information to the clipboard in a format suitable for pasting into spreadsheets like Excel or row/column data processing tools.

*   **PCopy has body**: Copy request/response information including both request and response bodies.
*   **PCopy no body**: Copy request/response information but exclude the response body to save space or when the request body contains image data, binary data, etc.

### "GenData" Menu (Only visible when HTTP Request/Response is selected)

*   **Copy as JSON**: Format detailed information of the selected HTTP Request (Headers, Method, URL Query Parameters, Body, etc.) into a specific JSON structure and copy it to the clipboard. This JSON structure can be used for purposes such as generating test cases, formatting data for automated processing, etc.

## Installation

1.  Ensure you have a compatible **Java Development Kit (JDK)** installed (check Burp Suite requirements).
2.  Clone this repository to your machine:
    ```bash
    git clone https://github.com/toannguyen3107/ScreenShort.git
    ```
4. Build into a jar file (`mvn clean package`).
5. Add the built file to Burp Suite (the file is generated in the `target` directory).

## Usage

After the extension is successfully loaded, simply right-click on an HTTP Request or Response in tabs like Proxy History, Repeater, Intruder, etc., to see the new context menu options: "Screenshot", "PCopy", and "GenData".

*   Select options in "Screenshot" to capture Burp Suite screen shots.
*   Select options in "PCopy" to copy request/response data in a row/column format.
*   Select "GenData" -> "Copy as JSON" to get the request information in the structured JSON format.

## Project Structure (Optional)

Consists of 2 packages: `com.screenshort` and `com.screenshort.utils`.
1. `GUI.java`: This file overrides `provideMenuItems` according to the Montoya API to create the menus.
2. `com.screenshort.utils/MenuActionHandler.java`: This file links to the functions in `utils/*`. `GUI` calls functions in this handler, and the handler calls the corresponding functions in the utility classes.

## Author

`@toannguyen3107`

## Contributing (Optional)

Contributions are welcome! If you find bugs or have ideas for improvements, please open an issue or submit a pull request.
