package com.badereddine.client.util;

import com.badereddine.client.controller.DashboardController;
import com.badereddine.client.controller.LoginController;
import com.badereddine.client.controller.*;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

/**
 * Manages scene navigation and transitions
 */
public class SceneManager {

    private static Stage primaryStage;
    private static Scene mainScene;
    private static BorderPane mainLayout;

    private static LoginController loginController;
    private static DashboardController dashboardController;

    private SceneManager() {
    }

    public static void initialize(Stage stage) {
        primaryStage = stage;
    }

    public static Stage getPrimaryStage() {
        return primaryStage;
    }

    public static void showLoginScene() {
        loginController = new LoginController();
        mainScene = new Scene(loginController.getView(), 1200, 800);
        try {
            var cssResource = SceneManager.class.getResource("/styles/main.css");
            if (cssResource != null) {
                mainScene.getStylesheets().add(cssResource.toExternalForm());
            } else {
                System.err.println("Warning: CSS file not found at /styles/main.css");
            }
        } catch (Exception e) {
            System.err.println("Warning: Could not load CSS: " + e.getMessage());
        }
        primaryStage.setScene(mainScene);
    }

    public static void showDashboard() {
        dashboardController = new DashboardController();
        mainScene = new Scene(dashboardController.getView(), 1200, 800);
        try {
            var cssResource = SceneManager.class.getResource("/styles/main.css");
            if (cssResource != null) {
                mainScene.getStylesheets().add(cssResource.toExternalForm());
            }
        } catch (Exception e) {
            System.err.println("Warning: Could not load CSS: " + e.getMessage());
        }
        primaryStage.setScene(mainScene);
    }

    public static void logout() {
        showLoginScene();
    }
}
