package com.fandomtui;

import java.io.IOException;

/**
 * Main entry point for the Fandom TUI application.
 * Initializes the TUI app and starts the main loop.
 */
public class Main {
    public static void main(String[] args) {
        try {
            TuiApp app = new TuiApp();
            app.start();
        } catch (IOException e) {
            System.err.println("Failed to start application: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}