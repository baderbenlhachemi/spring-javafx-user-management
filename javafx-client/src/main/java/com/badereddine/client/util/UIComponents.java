package com.badereddine.client.util;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.paint.CycleMethod;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import org.kordamp.ikonli.javafx.FontIcon;

/**
 * Utility class for creating styled UI components
 */
public class UIComponents {

    // Color palette
    public static final Color PRIMARY_COLOR = Color.web("#6366F1");
    public static final Color PRIMARY_DARK = Color.web("#4F46E5");
    public static final Color PRIMARY_LIGHT = Color.web("#818CF8");
    public static final Color SECONDARY_COLOR = Color.web("#8B5CF6");
    public static final Color SUCCESS_COLOR = Color.web("#10B981");
    public static final Color WARNING_COLOR = Color.web("#F59E0B");
    public static final Color DANGER_COLOR = Color.web("#EF4444");
    public static final Color INFO_COLOR = Color.web("#3B82F6");

    public static final Color BACKGROUND_COLOR = Color.web("#0F172A");
    public static final Color SURFACE_COLOR = Color.web("#1E293B");
    public static final Color SURFACE_LIGHT = Color.web("#334155");
    public static final Color TEXT_PRIMARY = Color.web("#F8FAFC");
    public static final Color TEXT_SECONDARY = Color.web("#94A3B8");

    private UIComponents() {
    }

    /**
     * Create a styled primary button
     */
    public static Button createPrimaryButton(String text, String iconCode) {
        Button button = new Button(text);
        button.getStyleClass().add("primary-button");

        if (iconCode != null && !iconCode.isEmpty()) {
            FontIcon icon = new FontIcon(iconCode);
            icon.setIconSize(16);
            icon.setIconColor(Color.WHITE);
            button.setGraphic(icon);
        }

        return button;
    }

    /**
     * Create a styled secondary button
     */
    public static Button createSecondaryButton(String text, String iconCode) {
        Button button = new Button(text);
        button.getStyleClass().add("secondary-button");

        if (iconCode != null && !iconCode.isEmpty()) {
            FontIcon icon = new FontIcon(iconCode);
            icon.setIconSize(16);
            icon.setIconColor(PRIMARY_COLOR);
            button.setGraphic(icon);
        }

        return button;
    }

    /**
     * Create a styled text field
     */
    public static TextField createStyledTextField(String promptText, String iconCode) {
        TextField textField = new TextField();
        textField.setPromptText(promptText);
        textField.getStyleClass().add("styled-text-field");
        return textField;
    }

    /**
     * Create a styled password field
     */
    public static PasswordField createStyledPasswordField(String promptText) {
        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText(promptText);
        passwordField.getStyleClass().add("styled-text-field");
        return passwordField;
    }

    /**
     * Create a card container
     */
    public static VBox createCard(String title, String iconCode) {
        VBox card = new VBox(15);
        card.getStyleClass().add("card");
        card.setPadding(new Insets(24));

        if (title != null && !title.isEmpty()) {
            HBox header = new HBox(10);
            header.setAlignment(Pos.CENTER_LEFT);

            if (iconCode != null && !iconCode.isEmpty()) {
                FontIcon icon = new FontIcon(iconCode);
                icon.setIconSize(20);
                icon.setIconColor(PRIMARY_COLOR);
                header.getChildren().add(icon);
            }

            Label titleLabel = new Label(title);
            titleLabel.getStyleClass().add("card-title");
            header.getChildren().add(titleLabel);

            card.getChildren().add(header);

            Separator separator = new Separator();
            separator.getStyleClass().add("card-separator");
            card.getChildren().add(separator);
        }

        return card;
    }

    /**
     * Create a stat card for dashboard
     */
    public static VBox createStatCard(String title, String value, String iconCode, Color accentColor) {
        VBox card = new VBox(10);
        card.getStyleClass().add("stat-card");
        card.setPadding(new Insets(20));
        card.setAlignment(Pos.CENTER_LEFT);

        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);

        StackPane iconContainer = new StackPane();
        iconContainer.getStyleClass().add("stat-icon-container");
        iconContainer.setStyle("-fx-background-color: " + toHexString(accentColor) + "20; -fx-background-radius: 12;");
        iconContainer.setPadding(new Insets(12));

        FontIcon icon = new FontIcon(iconCode);
        icon.setIconSize(24);
        icon.setIconColor(accentColor);
        iconContainer.getChildren().add(icon);

        VBox textContainer = new VBox(4);
        textContainer.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(textContainer, Priority.ALWAYS);
        HBox.setMargin(textContainer, new Insets(0, 0, 0, 15));

        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("stat-title");

        Label valueLabel = new Label(value);
        valueLabel.getStyleClass().add("stat-value");

        textContainer.getChildren().addAll(titleLabel, valueLabel);
        header.getChildren().addAll(iconContainer, textContainer);
        card.getChildren().add(header);

        return card;
    }

    /**
     * Create a navigation button for sidebar
     */
    public static Button createNavButton(String text, String iconCode, boolean isActive) {
        Button button = new Button(text);
        button.getStyleClass().add("nav-button");
        if (isActive) {
            button.getStyleClass().add("nav-button-active");
        }
        button.setMaxWidth(Double.MAX_VALUE);
        button.setAlignment(Pos.CENTER_LEFT);

        if (iconCode != null && !iconCode.isEmpty()) {
            FontIcon icon = new FontIcon(iconCode);
            icon.setIconSize(18);
            icon.setIconColor(isActive ? PRIMARY_COLOR : TEXT_SECONDARY);
            button.setGraphic(icon);
        }

        return button;
    }

    /**
     * Create an avatar circle
     */
    public static StackPane createAvatar(String initials, double size, Color backgroundColor) {
        Circle circle = new Circle(size / 2);
        circle.setFill(backgroundColor);

        Label label = new Label(initials);
        label.setTextFill(Color.WHITE);
        label.setFont(Font.font("System", FontWeight.BOLD, size / 2.5));

        StackPane avatar = new StackPane(circle, label);
        avatar.setAlignment(Pos.CENTER);

        return avatar;
    }

    /**
     * Create a section title
     */
    public static Label createSectionTitle(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("section-title");
        return label;
    }

    /**
     * Create a loading spinner
     */
    public static StackPane createLoadingSpinner() {
        ProgressIndicator spinner = new ProgressIndicator();
        spinner.getStyleClass().add("loading-spinner");
        spinner.setMaxSize(40, 40);

        StackPane container = new StackPane(spinner);
        container.setAlignment(Pos.CENTER);
        container.getStyleClass().add("loading-container");

        return container;
    }

    /**
     * Create a notification toast
     */
    public static HBox createToast(String message, ToastType type) {
        HBox toast = new HBox(12);
        toast.getStyleClass().addAll("toast", "toast-" + type.name().toLowerCase());
        toast.setAlignment(Pos.CENTER_LEFT);
        toast.setPadding(new Insets(16, 20, 16, 20));

        String iconCode;
        Color iconColor;

        switch (type) {
            case SUCCESS:
                iconCode = "fas-check-circle";
                iconColor = SUCCESS_COLOR;
                break;
            case ERROR:
                iconCode = "fas-exclamation-circle";
                iconColor = DANGER_COLOR;
                break;
            case WARNING:
                iconCode = "fas-exclamation-triangle";
                iconColor = WARNING_COLOR;
                break;
            default:
                iconCode = "fas-info-circle";
                iconColor = INFO_COLOR;
        }

        FontIcon icon = new FontIcon(iconCode);
        icon.setIconSize(20);
        icon.setIconColor(iconColor);

        Label messageLabel = new Label(message);
        messageLabel.getStyleClass().add("toast-message");
        messageLabel.setWrapText(true);

        toast.getChildren().addAll(icon, messageLabel);

        DropShadow shadow = new DropShadow();
        shadow.setColor(Color.rgb(0, 0, 0, 0.3));
        shadow.setRadius(15);
        shadow.setOffsetY(5);
        toast.setEffect(shadow);

        return toast;
    }

    /**
     * Create a form field with label
     */
    public static VBox createFormField(String labelText, Control field) {
        VBox container = new VBox(8);

        Label label = new Label(labelText);
        label.getStyleClass().add("form-label");

        container.getChildren().addAll(label, field);
        return container;
    }

    /**
     * Create a badge
     */
    public static Label createBadge(String text, Color backgroundColor) {
        Label badge = new Label(text);
        badge.getStyleClass().add("badge");
        badge.setStyle("-fx-background-color: " + toHexString(backgroundColor) + ";");
        badge.setPadding(new Insets(4, 12, 4, 12));
        return badge;
    }

    /**
     * Convert Color to hex string
     */
    public static String toHexString(Color color) {
        return String.format("#%02X%02X%02X",
                (int) (color.getRed() * 255),
                (int) (color.getGreen() * 255),
                (int) (color.getBlue() * 255));
    }

    /**
     * Create gradient background
     */
    public static Background createGradientBackground(Color startColor, Color endColor) {
        LinearGradient gradient = new LinearGradient(
                0, 0, 1, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, startColor),
                new Stop(1, endColor)
        );
        return new Background(new BackgroundFill(gradient, CornerRadii.EMPTY, Insets.EMPTY));
    }

    public enum ToastType {
        SUCCESS, ERROR, WARNING, INFO
    }
}
