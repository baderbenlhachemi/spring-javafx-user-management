package com.badereddine.client;

import com.badereddine.client.util.SceneManager;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.image.Image;
import javafx.stage.Stage;

/**
 * Main JavaFX Application for JWT User Management Client
 */
public class JwtUserManagementApp extends Application {

    private static final String APP_TITLE = "JWT User Management";
    private static final int MIN_WIDTH = 1200;
    private static final int MIN_HEIGHT = 800;

    @Override
    public void init() {
        // Prevent implicit exit when all windows are hidden
        Platform.setImplicitExit(true);
    }

    @Override
    public void start(Stage primaryStage) {
        Thread.setDefaultUncaughtExceptionHandler((t, e) -> {
            System.err.println("Uncaught exception in thread " + t.getName());
            e.printStackTrace();
        });

        try {
            System.out.println("Starting JWT User Management Client...");

            // Initialize the scene manager
            SceneManager.initialize(primaryStage);

            // Set up the primary stage
            primaryStage.setTitle(APP_TITLE);
            primaryStage.setMinWidth(MIN_WIDTH);
            primaryStage.setMinHeight(MIN_HEIGHT);
            primaryStage.setWidth(MIN_WIDTH);
            primaryStage.setHeight(MIN_HEIGHT);

            // Try to load application icon
            try {
                var iconStream = getClass().getResourceAsStream("/images/icon.png");
                if (iconStream != null) {
                    primaryStage.getIcons().add(new Image(iconStream));
                }
            } catch (Exception e) {
                System.out.println("Icon not found, continuing without it.");
            }

            // Handle window close request
            primaryStage.setOnCloseRequest(event -> {
                System.out.println("Window close requested by user.");
                Platform.exit();
            });

            // Show the login scene
            System.out.println("Loading login scene...");
            SceneManager.showLoginScene();

            System.out.println("Showing primary stage...");
            primaryStage.show();
            primaryStage.toFront();
            primaryStage.requestFocus();
            primaryStage.centerOnScreen();

            System.out.println("Application started successfully! Window should be visible now.");

        } catch (Exception e) {
            System.err.println("Error starting application: " + e.getMessage());
            e.printStackTrace();

            // Show a simple error dialog
            javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                javafx.scene.control.Alert.AlertType.ERROR,
                "Failed to start application: " + e.getMessage()
            );
            alert.showAndWait();
            System.exit(1);
        }
    }

    @Override
    public void stop() {
        // Cleanup resources when application closes
        System.out.println("Application closing...");
    }

    public static void main(String[] args) {
        launch(args);
    }
}
