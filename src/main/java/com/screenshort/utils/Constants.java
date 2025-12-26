package com.screenshort.utils;

import java.awt.Color;

/**
 * Centralized constants for the ScreenShort extension.
 * Contains magic numbers, component names, and configuration values.
 */
public final class Constants {

    private Constants() {
        // Utility class - prevent instantiation
    }

    // ==================== Burp Suite Component Names ====================
    public static final String SPLIT_VIEWER_PANE = "rrvSplitViewerSplitPane";
    public static final String REQUESTS_PANE = "rrvRequestsPane";
    public static final String RESPONSE_PANE = "rrvResponsePane";
    public static final String SYNTAX_TEXT_AREA = "syntaxTextArea";

    // ==================== Annotation Editor ====================
    public static final int TOOLBAR_WIDTH = 140;
    public static final float STROKE_WIDTH = 3f;
    public static final int HIGHLIGHT_ALPHA = 200;
    public static final int EDITOR_MIN_WIDTH = 500;
    public static final int EDITOR_MIN_HEIGHT = 400;

    // ==================== Image Combining ====================
    public static final int BORDER_THICKNESS = 5;
    public static final Color BORDER_COLOR = Color.BLACK;
    public static final Color BACKGROUND_COLOR = Color.WHITE;

    // ==================== Excel Formatting ====================
    public static final int MAX_EXCEL_CELL_LENGTH = 29000;
    public static final String EXCEL_SEPARATOR = "\t";
    public static final String REDACTED_TEXT = "REDACTED";

    // ==================== Annotation Modes ====================
    public static final String MODE_RECT = "RECT";
    public static final String MODE_LINE = "LINE";
    public static final String MODE_HIGHLIGHT = "HIGHLIGHT";

    // ==================== Default Colors ====================
    public static final Color DEFAULT_ANNOTATION_COLOR = new Color(255, 0, 0, HIGHLIGHT_ALPHA);

    // ==================== Preferences Keys ====================
    public static final String PREFS_KEY_DEFAULT_PATH = "default_export_path";
}
