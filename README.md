# ScreenShort

ScreenShort is a Java application designed to capture screenshots and provide utility functions for data processing, including Excel formatting and JSON generation.

## Features

*   **Screenshot Capture**: Capture screenshots of your screen.
*   **Graphical User Interface (GUI)**: Interact with the application through a user-friendly interface.
*   **Excel Formatting Utilities**: Tools for formatting data into Excel files.
*   **JSON Generation Utilities**: Tools for generating JSON data.

## Project Structure

The project is organized into the following main packages:

*   `com.screenshort`: Contains the main application class (`ScreenShort.java`) and the GUI implementation (`GUI.java`).
*   `com.screenshort.utils`: Contains various utility classes, including:
    *   `ExcelFormatterUtils.java`: Handles Excel formatting logic.
    *   `GenDataToJson.java`: Handles JSON data generation.
    *   `MenuActionHandler.java`: Likely handles actions triggered by GUI menu items.
    *   `ScreenshotUtils.java`: Contains the core screenshot capture logic.
    *   `CustomMessageEditorHotKey.java`: (Purpose inferred from name, likely related to custom key bindings).

## Building and Running

This project uses Maven.

1.  Ensure you have a compatible **Java Development Kit (JDK)** installed.
2.  Navigate to the project root directory in your terminal.
3.  Build the project using Maven:
    ```bash
    mvn clean package
    ```
4.  Run the application. The exact command may vary depending on how the main class is configured in the `pom.xml`, but it will likely be similar to:
    ```bash
    java -jar target/ScreenShort-1.0-SNAPSHOT.jar
    ```
    (Replace `ScreenShort-1.0-SNAPSHOT.jar` with the actual generated jar file name if different).

## Testing

Unit tests are located in `src/test/java/com/screenshot/`.

*   `AppTest.java`: Contains basic unit tests for the application.

To run tests using Maven:

```bash
mvn test
```

## Contributing

@quangit



## Author

@t0ann9uy3n (Toan Nguyen
