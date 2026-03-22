package com.badereddine.client;

/**
 * Launcher class to start the JavaFX application.
 * This is needed to run JavaFX applications from a non-modular environment
 * or when running from an IDE without proper module configuration.
 */
public class Launcher {
    public static void main(String[] args) {
        JwtUserManagementApp.main(args);
    }
}
