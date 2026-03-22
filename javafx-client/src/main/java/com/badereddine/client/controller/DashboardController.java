package com.badereddine.client.controller;

import com.badereddine.client.model.*;
import com.badereddine.client.service.ApiService;
import com.badereddine.client.service.SessionManager;
import com.badereddine.client.util.AnimationUtils;
import com.badereddine.client.util.SceneManager;
import com.badereddine.client.util.UIComponents;
import javafx.application.Platform;
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
import javafx.stage.FileChooser;
import org.kordamp.ikonli.javafx.FontIcon;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;

/**
 * Controller for the main Dashboard
 */
public class DashboardController {

    private final BorderPane root;
    private final StackPane contentArea;
    private final VBox sidebar;

    private Button navDashboard;
    private Button navProfile;
    private Button navGenerate;
    private Button navImport;
    private Button navLookup;
    private Button navSettings;
    private Button navUserList;

    private final ApiService apiService = ApiService.getInstance();
    private final SessionManager sessionManager = SessionManager.getInstance();

    private User currentUser;

    public DashboardController() {
        this.contentArea = new StackPane();
        this.sidebar = createSidebar();
        this.root = createView();
        loadUserProfile();
        showDashboardContent();
    }

    public BorderPane getView() {
        return root;
    }

    private BorderPane createView() {
        BorderPane borderPane = new BorderPane();
        borderPane.getStyleClass().add("dashboard-root");
        borderPane.setStyle("-fx-background-color: #0F172A;");

        // Sidebar
        borderPane.setLeft(sidebar);

        // Main content area
        VBox mainContent = new VBox();
        mainContent.setStyle("-fx-background-color: #0F172A;");

        // Header
        HBox header = createHeader();

        // Content
        contentArea.setPadding(new Insets(24));
        contentArea.setStyle("-fx-background-color: #0F172A;");
        VBox.setVgrow(contentArea, Priority.ALWAYS);

        mainContent.getChildren().addAll(header, contentArea);
        borderPane.setCenter(mainContent);

        return borderPane;
    }

    private VBox createSidebar() {
        VBox sidebarPane = new VBox(8);
        sidebarPane.getStyleClass().add("sidebar");
        sidebarPane.setPadding(new Insets(20));
        sidebarPane.setPrefWidth(260);
        sidebarPane.setStyle("-fx-background-color: #1E293B;");

        // Logo section
        HBox logoSection = new HBox(12);
        logoSection.setAlignment(Pos.CENTER_LEFT);
        logoSection.setPadding(new Insets(10, 0, 30, 0));

        StackPane logoContainer = new StackPane();
        Circle logoBg = new Circle(22);
        logoBg.setFill(new LinearGradient(0, 0, 1, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#6366F1")),
                new Stop(1, Color.web("#8B5CF6"))));
        FontIcon logoIcon = new FontIcon("fas-shield-alt");
        logoIcon.setIconSize(20);
        logoIcon.setIconColor(Color.WHITE);
        logoContainer.getChildren().addAll(logoBg, logoIcon);

        Label logoText = new Label("User Manager");
        logoText.setFont(Font.font("System", FontWeight.BOLD, 18));
        logoText.setTextFill(Color.WHITE);

        logoSection.getChildren().addAll(logoContainer, logoText);

        // Navigation section
        Label navLabel = new Label("NAVIGATION");
        navLabel.setFont(Font.font("System", FontWeight.MEDIUM, 11));
        navLabel.setTextFill(Color.web("#64748B"));
        navLabel.setPadding(new Insets(0, 0, 8, 8));

        navDashboard = createNavButton("Dashboard", "fas-th-large", true);
        navProfile = createNavButton("My Profile", "fas-user", false);
        navSettings = createNavButton("Settings", "fas-cog", false);

        navDashboard.setOnAction(e -> { setActiveNav(navDashboard); showDashboardContent(); });
        navProfile.setOnAction(e -> { setActiveNav(navProfile); showProfileContent(); });
        navSettings.setOnAction(e -> { setActiveNav(navSettings); showSettingsContent(); });

        VBox navSection = new VBox(4);
        navSection.getChildren().addAll(navLabel, navDashboard, navProfile, navSettings);

        // Admin section (only visible to admins)
        VBox adminSection = new VBox(4);
        if (sessionManager.isAdmin()) {
            Label adminLabel = new Label("ADMIN");
            adminLabel.setFont(Font.font("System", FontWeight.MEDIUM, 11));
            adminLabel.setTextFill(Color.web("#64748B"));
            adminLabel.setPadding(new Insets(20, 0, 8, 8));

            // Generate and Import are admin-only features
            navGenerate = createNavButton("Generate Users", "fas-users-cog", false);
            navImport = createNavButton("Import Users", "fas-file-import", false);
            navLookup = createNavButton("User Lookup", "fas-search", false);
            navUserList = createNavButton("All Users", "fas-users", false);

            navGenerate.setOnAction(e -> { setActiveNav(navGenerate); showGenerateContent(); });
            navImport.setOnAction(e -> { setActiveNav(navImport); showImportContent(); });
            navLookup.setOnAction(e -> { setActiveNav(navLookup); showLookupContent(); });
            navUserList.setOnAction(e -> { setActiveNav(navUserList); showUserListContent(); });

            adminSection.getChildren().addAll(adminLabel, navGenerate, navImport, navUserList, navLookup);
        }

        // Spacer
        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        // Logout section
        Button logoutButton = createNavButton("Logout", "fas-sign-out-alt", false);
        logoutButton.setStyle("-fx-background-color: transparent;");
        logoutButton.setOnAction(e -> handleLogout());

        sidebarPane.getChildren().addAll(logoSection, navSection, adminSection, spacer, logoutButton);

        return sidebarPane;
    }

    private Button createNavButton(String text, String iconCode, boolean isActive) {
        Button button = new Button(text);
        button.setMaxWidth(Double.MAX_VALUE);
        button.setAlignment(Pos.CENTER_LEFT);
        button.setPadding(new Insets(12, 16, 12, 16));
        button.setFont(Font.font("System", FontWeight.MEDIUM, 14));

        FontIcon icon = new FontIcon(iconCode);
        icon.setIconSize(18);

        if (isActive) {
            button.setStyle("-fx-background-color: #6366F120; -fx-background-radius: 8; -fx-text-fill: #818CF8;");
            icon.setIconColor(Color.web("#818CF8"));
        } else {
            button.setStyle("-fx-background-color: transparent; -fx-background-radius: 8; -fx-text-fill: #94A3B8; -fx-cursor: hand;");
            icon.setIconColor(Color.web("#94A3B8"));
        }

        button.setGraphic(icon);
        button.setGraphicTextGap(12);

        // Hover effect
        button.setOnMouseEntered(e -> {
            if (!button.getStyleClass().contains("nav-active")) {
                button.setStyle("-fx-background-color: #334155; -fx-background-radius: 8; -fx-text-fill: #F1F5F9; -fx-cursor: hand;");
                icon.setIconColor(Color.web("#F1F5F9"));
            }
        });
        button.setOnMouseExited(e -> {
            if (!button.getStyleClass().contains("nav-active")) {
                button.setStyle("-fx-background-color: transparent; -fx-background-radius: 8; -fx-text-fill: #94A3B8; -fx-cursor: hand;");
                icon.setIconColor(Color.web("#94A3B8"));
            }
        });

        return button;
    }

    private void setActiveNav(Button activeButton) {
        Button[] allNavButtons = { navDashboard, navProfile, navGenerate, navImport, navLookup };

        for (Button btn : allNavButtons) {
            if (btn != null) {
                btn.getStyleClass().remove("nav-active");
                FontIcon icon = (FontIcon) btn.getGraphic();
                if (btn == activeButton) {
                    btn.setStyle("-fx-background-color: #6366F120; -fx-background-radius: 8; -fx-text-fill: #818CF8;");
                    btn.getStyleClass().add("nav-active");
                    icon.setIconColor(Color.web("#818CF8"));
                } else {
                    btn.setStyle("-fx-background-color: transparent; -fx-background-radius: 8; -fx-text-fill: #94A3B8; -fx-cursor: hand;");
                    icon.setIconColor(Color.web("#94A3B8"));
                }
            }
        }
    }

    private HBox createHeader() {
        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_RIGHT);
        header.setPadding(new Insets(16, 24, 16, 24));
        header.setStyle("-fx-background-color: #1E293B; -fx-border-color: #334155; -fx-border-width: 0 0 1 0;");

        // User info section
        HBox userSection = new HBox(12);
        userSection.setAlignment(Pos.CENTER_RIGHT);

        VBox userInfo = new VBox(2);
        userInfo.setAlignment(Pos.CENTER_RIGHT);

        Label usernameLabel = new Label(sessionManager.getUsername());
        usernameLabel.setFont(Font.font("System", FontWeight.MEDIUM, 14));
        usernameLabel.setTextFill(Color.WHITE);

        Label roleLabel = new Label(sessionManager.isAdmin() ? "Administrator" : "User");
        roleLabel.setFont(Font.font("System", 12));
        roleLabel.setTextFill(Color.web("#94A3B8"));

        userInfo.getChildren().addAll(usernameLabel, roleLabel);

        // Avatar
        StackPane avatar = new StackPane();
        Circle avatarCircle = new Circle(20);
        avatarCircle.setFill(new LinearGradient(0, 0, 1, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#6366F1")),
                new Stop(1, Color.web("#8B5CF6"))));

        String initials = sessionManager.getUsername() != null && !sessionManager.getUsername().isEmpty()
                ? sessionManager.getUsername().substring(0, 1).toUpperCase()
                : "U";
        Label avatarLabel = new Label(initials);
        avatarLabel.setFont(Font.font("System", FontWeight.BOLD, 14));
        avatarLabel.setTextFill(Color.WHITE);

        avatar.getChildren().addAll(avatarCircle, avatarLabel);

        userSection.getChildren().addAll(userInfo, avatar);

        HBox.setHgrow(userSection, Priority.ALWAYS);
        header.getChildren().add(userSection);

        return header;
    }

    private void loadUserProfile() {
        apiService.getMyProfile(sessionManager.getAuthorizationHeader())
            .thenAccept(result -> Platform.runLater(() -> {
                if (result.isSuccess()) {
                    currentUser = result.getData();
                    sessionManager.setCurrentUser(currentUser);
                }
            }));
    }

    // ========== Dashboard Content ==========
    private void showDashboardContent() {
        contentArea.getChildren().clear();

        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        scrollPane.getStyleClass().add("edge-to-edge");

        VBox dashboardContent = new VBox(24);
        dashboardContent.setPadding(new Insets(0, 0, 24, 0));

        // Welcome section
        VBox welcomeSection = new VBox(8);
        Label welcomeLabel = new Label("Welcome back, " + sessionManager.getUsername() + "!");
        welcomeLabel.setFont(Font.font("System", FontWeight.BOLD, 28));
        welcomeLabel.setTextFill(Color.WHITE);

        Label dateLabel = new Label(new SimpleDateFormat("EEEE, MMMM d, yyyy").format(new java.util.Date()));
        dateLabel.setFont(Font.font("System", 14));
        dateLabel.setTextFill(Color.web("#94A3B8"));

        welcomeSection.getChildren().addAll(welcomeLabel, dateLabel);

        // Quick stats
        HBox statsRow = new HBox(20);
        statsRow.getChildren().addAll(
            createStatCard("Your Role", sessionManager.isAdmin() ? "Administrator" : "User",
                    "fas-user-shield", Color.web("#6366F1")),
            createStatCard("Session Status", "Active", "fas-check-circle", Color.web("#10B981")),
            createStatCard("Token Type", "JWT Bearer", "fas-key", Color.web("#F59E0B")),
            createStatCard("API Server", "localhost:9090", "fas-server", Color.web("#3B82F6"))
        );

        // Admin stats section - Enhanced UI
        VBox adminStatsSection = new VBox(16);
        if (sessionManager.isAdmin()) {
            // Stats header with refresh button
            HBox statsHeader = new HBox();
            statsHeader.setAlignment(Pos.CENTER_LEFT);
            statsHeader.setPadding(new Insets(20, 0, 0, 0));

            Label adminStatsLabel = new Label("📊 User Statistics");
            adminStatsLabel.setFont(Font.font("System", FontWeight.BOLD, 22));
            adminStatsLabel.setTextFill(Color.WHITE);

            Region headerSpacer = new Region();
            HBox.setHgrow(headerSpacer, Priority.ALWAYS);

            Button refreshStatsBtn = new Button("Refresh");
            refreshStatsBtn.setStyle("-fx-background-color: #334155; -fx-text-fill: white; -fx-background-radius: 6; -fx-padding: 6 12;");
            FontIcon refreshIcon = new FontIcon("fas-sync-alt");
            refreshIcon.setIconSize(12);
            refreshIcon.setIconColor(Color.WHITE);
            refreshStatsBtn.setGraphic(refreshIcon);

            statsHeader.getChildren().addAll(adminStatsLabel, headerSpacer, refreshStatsBtn);

            // Stats cards container with gradient background
            HBox adminStatsRow = new HBox(16);
            adminStatsRow.setPadding(new Insets(20));
            adminStatsRow.setStyle("-fx-background-color: linear-gradient(to right, #1E293B, #334155); -fx-background-radius: 16;");

            // Create enhanced stat cards
            VBox totalUsersCard = createEnhancedStatCard("Total Users", "...", "fas-users",
                    Color.web("#6366F1"), Color.web("#818CF8"));
            VBox adminsCard = createEnhancedStatCard("Administrators", "...", "fas-user-shield",
                    Color.web("#8B5CF6"), Color.web("#A78BFA"));
            VBox regularUsersCard = createEnhancedStatCard("Regular Users", "...", "fas-user",
                    Color.web("#10B981"), Color.web("#34D399"));
            VBox newTodayCard = createEnhancedStatCard("New Today", "...", "fas-user-plus",
                    Color.web("#F59E0B"), Color.web("#FBBF24"));

            adminStatsRow.getChildren().addAll(totalUsersCard, adminsCard, regularUsersCard, newTodayCard);

            // Progress indicators for each card
            ProgressIndicator[] spinners = new ProgressIndicator[4];
            VBox[] cards = {totalUsersCard, adminsCard, regularUsersCard, newTodayCard};

            // Load stats function
            Runnable loadStats = () -> {
                // Show loading state
                for (VBox card : cards) {
                    updateEnhancedStatCardValue(card, "...");
                }

                apiService.getUserStats(sessionManager.getAuthorizationHeader())
                    .thenAccept(result -> Platform.runLater(() -> {
                        if (result.isSuccess()) {
                            UserStats stats = result.getData();
                            updateEnhancedStatCardValue(totalUsersCard, String.valueOf(stats.getTotalUsers()));
                            updateEnhancedStatCardValue(adminsCard, String.valueOf(stats.getTotalAdmins()));
                            updateEnhancedStatCardValue(regularUsersCard, String.valueOf(stats.getTotalRegularUsers()));
                            updateEnhancedStatCardValue(newTodayCard, String.valueOf(stats.getNewUsersToday()));

                            // Add percentage labels
                            if (stats.getTotalUsers() > 0) {
                                double adminPercent = (stats.getTotalAdmins() * 100.0) / stats.getTotalUsers();
                                double userPercent = (stats.getTotalRegularUsers() * 100.0) / stats.getTotalUsers();
                                updateEnhancedStatCardSubtext(adminsCard, String.format("%.1f%% of total", adminPercent));
                                updateEnhancedStatCardSubtext(regularUsersCard, String.format("%.1f%% of total", userPercent));
                            }
                        }
                    }));
            };

            refreshStatsBtn.setOnAction(e -> {
                refreshStatsBtn.setDisable(true);
                loadStats.run();
                // Re-enable after a short delay
                new Thread(() -> {
                    try { Thread.sleep(1000); } catch (InterruptedException ignored) {}
                    Platform.runLater(() -> refreshStatsBtn.setDisable(false));
                }).start();
            });

            adminStatsSection.getChildren().addAll(statsHeader, adminStatsRow);

            // Initial load
            loadStats.run();
        }

        // Quick actions
        Label actionsLabel = new Label("Quick Actions");
        actionsLabel.setFont(Font.font("System", FontWeight.BOLD, 20));
        actionsLabel.setTextFill(Color.WHITE);
        actionsLabel.setPadding(new Insets(20, 0, 0, 0));

        HBox actionsRow = new HBox(20);
        actionsRow.getChildren().add(
            createActionCard("View Profile", "See your account details", "fas-user",
                    Color.web("#6366F1"), () -> { setActiveNav(navProfile); showProfileContent(); })
        );

        // Admin-only action cards
        if (sessionManager.isAdmin()) {
            actionsRow.getChildren().addAll(
                createActionCard("Generate Users", "Create fake user data", "fas-users-cog",
                        Color.web("#8B5CF6"), () -> { setActiveNav(navGenerate); showGenerateContent(); }),
                createActionCard("Import Users", "Batch import from JSON", "fas-file-import",
                        Color.web("#10B981"), () -> { setActiveNav(navImport); showImportContent(); }),
                createActionCard("User Lookup", "Search for any user", "fas-search",
                        Color.web("#F59E0B"), () -> { setActiveNav(navLookup); showLookupContent(); })
            );
        }

        // Info card
        VBox infoCard = createInfoCard();

        dashboardContent.getChildren().addAll(welcomeSection, statsRow, adminStatsSection, actionsLabel, actionsRow, infoCard);
        scrollPane.setContent(dashboardContent);
        contentArea.getChildren().add(scrollPane);

        AnimationUtils.fadeIn(dashboardContent, 300);
    }

    private VBox createStatCard(String title, String value, String iconCode, Color accentColor) {
        VBox card = new VBox(12);
        card.setPadding(new Insets(20));
        card.setMinWidth(200);
        card.setStyle("-fx-background-color: #1E293B; -fx-background-radius: 16;");
        HBox.setHgrow(card, Priority.ALWAYS);

        DropShadow shadow = new DropShadow();
        shadow.setColor(Color.rgb(0, 0, 0, 0.2));
        shadow.setRadius(15);
        shadow.setOffsetY(5);
        card.setEffect(shadow);

        HBox iconRow = new HBox();
        StackPane iconContainer = new StackPane();
        Circle iconBg = new Circle(20);
        iconBg.setFill(Color.web(UIComponents.toHexString(accentColor) + "20"));
        FontIcon icon = new FontIcon(iconCode);
        icon.setIconSize(18);
        icon.setIconColor(accentColor);
        iconContainer.getChildren().addAll(iconBg, icon);
        iconRow.getChildren().add(iconContainer);

        Label titleLabel = new Label(title);
        titleLabel.setFont(Font.font("System", 13));
        titleLabel.setTextFill(Color.web("#94A3B8"));

        Label valueLabel = new Label(value);
        valueLabel.setFont(Font.font("System", FontWeight.BOLD, 18));
        valueLabel.setTextFill(Color.WHITE);

        card.getChildren().addAll(iconRow, titleLabel, valueLabel);

        return card;
    }

    private void updateStatCardValue(VBox card, String newValue) {
        for (javafx.scene.Node node : card.getChildren()) {
            if (node instanceof Label) {
                Label label = (Label) node;
                if (label.getFont().getSize() >= 18) { // Value label has larger font
                    label.setText(newValue);
                    break;
                }
            }
        }
    }

    private VBox createEnhancedStatCard(String title, String value, String iconCode, Color primaryColor, Color lightColor) {
        VBox card = new VBox(8);
        card.setPadding(new Insets(20));
        card.setMinWidth(160);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setStyle("-fx-background-color: #0F172A; -fx-background-radius: 12; -fx-border-color: " +
                UIComponents.toHexString(primaryColor) + "40; -fx-border-radius: 12; -fx-border-width: 1;");
        HBox.setHgrow(card, Priority.ALWAYS);

        // Icon with gradient background
        HBox iconRow = new HBox(12);
        iconRow.setAlignment(Pos.CENTER_LEFT);

        StackPane iconContainer = new StackPane();
        Circle iconBg = new Circle(22);
        iconBg.setFill(new LinearGradient(0, 0, 1, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, primaryColor),
                new Stop(1, lightColor)));
        FontIcon icon = new FontIcon(iconCode);
        icon.setIconSize(18);
        icon.setIconColor(Color.WHITE);
        iconContainer.getChildren().addAll(iconBg, icon);

        Label titleLabel = new Label(title);
        titleLabel.setFont(Font.font("System", FontWeight.MEDIUM, 12));
        titleLabel.setTextFill(Color.web("#94A3B8"));

        iconRow.getChildren().addAll(iconContainer, titleLabel);

        // Value with large font
        Label valueLabel = new Label(value);
        valueLabel.setFont(Font.font("System", FontWeight.BOLD, 32));
        valueLabel.setTextFill(lightColor);
        valueLabel.getStyleClass().add("stat-value");

        // Subtext (for percentage, etc.)
        Label subtextLabel = new Label("");
        subtextLabel.setFont(Font.font("System", 11));
        subtextLabel.setTextFill(Color.web("#64748B"));
        subtextLabel.getStyleClass().add("stat-subtext");

        card.getChildren().addAll(iconRow, valueLabel, subtextLabel);

        // Hover effect
        card.setOnMouseEntered(e -> {
            card.setStyle("-fx-background-color: #1E293B; -fx-background-radius: 12; -fx-border-color: " +
                    UIComponents.toHexString(primaryColor) + "; -fx-border-radius: 12; -fx-border-width: 1;");
        });
        card.setOnMouseExited(e -> {
            card.setStyle("-fx-background-color: #0F172A; -fx-background-radius: 12; -fx-border-color: " +
                    UIComponents.toHexString(primaryColor) + "40; -fx-border-radius: 12; -fx-border-width: 1;");
        });

        return card;
    }

    private void updateEnhancedStatCardValue(VBox card, String newValue) {
        for (javafx.scene.Node node : card.getChildren()) {
            if (node instanceof Label && node.getStyleClass().contains("stat-value")) {
                ((Label) node).setText(newValue);
                break;
            }
        }
    }

    private void updateEnhancedStatCardSubtext(VBox card, String subtext) {
        for (javafx.scene.Node node : card.getChildren()) {
            if (node instanceof Label && node.getStyleClass().contains("stat-subtext")) {
                ((Label) node).setText(subtext);
                break;
            }
        }
    }

    private VBox createActionCard(String title, String description, String iconCode, Color accentColor, Runnable action) {
        VBox card = new VBox(12);
        card.setPadding(new Insets(24));
        card.setMinWidth(180);
        card.setAlignment(Pos.CENTER);
        card.setStyle("-fx-background-color: #1E293B; -fx-background-radius: 16; -fx-cursor: hand;");
        HBox.setHgrow(card, Priority.ALWAYS);

        StackPane iconContainer = new StackPane();
        Circle iconBg = new Circle(28);
        iconBg.setFill(accentColor);
        FontIcon icon = new FontIcon(iconCode);
        icon.setIconSize(24);
        icon.setIconColor(Color.WHITE);
        iconContainer.getChildren().addAll(iconBg, icon);

        Label titleLabel = new Label(title);
        titleLabel.setFont(Font.font("System", FontWeight.BOLD, 15));
        titleLabel.setTextFill(Color.WHITE);

        Label descLabel = new Label(description);
        descLabel.setFont(Font.font("System", 12));
        descLabel.setTextFill(Color.web("#94A3B8"));
        descLabel.setWrapText(true);
        descLabel.setAlignment(Pos.CENTER);

        card.getChildren().addAll(iconContainer, titleLabel, descLabel);

        // Hover effect
        card.setOnMouseEntered(e -> {
            card.setStyle("-fx-background-color: #334155; -fx-background-radius: 16; -fx-cursor: hand;");
            AnimationUtils.pulse(iconContainer);
        });
        card.setOnMouseExited(e -> {
            card.setStyle("-fx-background-color: #1E293B; -fx-background-radius: 16; -fx-cursor: hand;");
        });
        card.setOnMouseClicked(e -> action.run());

        return card;
    }

    private VBox createInfoCard() {
        VBox card = new VBox(16);
        card.setPadding(new Insets(24));
        card.setStyle("-fx-background-color: #1E293B; -fx-background-radius: 16;");

        Label titleLabel = new Label("About This Application");
        titleLabel.setFont(Font.font("System", FontWeight.BOLD, 18));
        titleLabel.setTextFill(Color.WHITE);

        Label descLabel = new Label(
            "This JWT User Management application provides secure user authentication and management " +
            "using Spring Security and JWT tokens. Users can be generated with fake data using the Faker " +
            "library or imported from JSON files. Role-based access control ensures that only administrators " +
            "can access certain features."
        );
        descLabel.setFont(Font.font("System", 14));
        descLabel.setTextFill(Color.web("#CBD5E1"));
        descLabel.setWrapText(true);

        HBox badges = new HBox(10);
        badges.getChildren().addAll(
            createBadge("Spring Boot 3.2", Color.web("#6DB33F")),
            createBadge("Spring Security", Color.web("#6DB33F")),
            createBadge("JWT", Color.web("#000000")),
            createBadge("PostgreSQL", Color.web("#336791")),
            createBadge("JavaFX", Color.web("#F0931C"))
        );

        card.getChildren().addAll(titleLabel, descLabel, badges);

        return card;
    }

    private Label createBadge(String text, Color bgColor) {
        Label badge = new Label(text);
        badge.setPadding(new Insets(6, 12, 6, 12));
        badge.setFont(Font.font("System", FontWeight.MEDIUM, 11));
        badge.setTextFill(Color.WHITE);
        badge.setStyle("-fx-background-color: " + UIComponents.toHexString(bgColor) + "; -fx-background-radius: 20;");
        return badge;
    }

    // ========== Profile Content ==========
    private void showProfileContent() {
        contentArea.getChildren().clear();

        if (currentUser == null) {
            showLoadingState("Loading profile...");
            apiService.getMyProfile(sessionManager.getAuthorizationHeader())
                .thenAccept(result -> Platform.runLater(() -> {
                    if (result.isSuccess()) {
                        currentUser = result.getData();
                        displayProfileContent();
                    } else {
                        showErrorState("Failed to load profile: " + result.getError());
                    }
                }));
        } else {
            displayProfileContent();
        }
    }

    private void displayProfileContent() {
        contentArea.getChildren().clear();

        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent;");

        VBox profileContent = new VBox(24);
        profileContent.setPadding(new Insets(0, 0, 24, 0));

        // Header
        Label headerLabel = new Label("My Profile");
        headerLabel.setFont(Font.font("System", FontWeight.BOLD, 28));
        headerLabel.setTextFill(Color.WHITE);

        // Profile card
        HBox profileCard = new HBox(32);
        profileCard.setPadding(new Insets(32));
        profileCard.setAlignment(Pos.CENTER_LEFT);
        profileCard.setStyle("-fx-background-color: #1E293B; -fx-background-radius: 16;");

        // Avatar section
        VBox avatarSection = new VBox(12);
        avatarSection.setAlignment(Pos.CENTER);

        StackPane avatar = new StackPane();
        Circle avatarCircle = new Circle(60);
        avatarCircle.setFill(new LinearGradient(0, 0, 1, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#6366F1")),
                new Stop(1, Color.web("#8B5CF6"))));

        String initials = "";
        if (currentUser.getFirstName() != null && !currentUser.getFirstName().isEmpty()) {
            initials += currentUser.getFirstName().charAt(0);
        }
        if (currentUser.getLastName() != null && !currentUser.getLastName().isEmpty()) {
            initials += currentUser.getLastName().charAt(0);
        }
        if (initials.isEmpty()) {
            initials = currentUser.getUsername().substring(0, 1).toUpperCase();
        }

        Label avatarLabel = new Label(initials.toUpperCase());
        avatarLabel.setFont(Font.font("System", FontWeight.BOLD, 28));
        avatarLabel.setTextFill(Color.WHITE);

        avatar.getChildren().addAll(avatarCircle, avatarLabel);

        Label roleBadge = new Label(currentUser.getRole() != null ? currentUser.getRole().getDisplayName() : "USER");
        roleBadge.setPadding(new Insets(6, 16, 6, 16));
        roleBadge.setFont(Font.font("System", FontWeight.BOLD, 11));
        roleBadge.setTextFill(Color.WHITE);
        roleBadge.setStyle("-fx-background-color: " + (currentUser.isAdmin() ? "#6366F1" : "#10B981") + "; -fx-background-radius: 20;");

        avatarSection.getChildren().addAll(avatar, roleBadge);

        // Info section
        GridPane infoGrid = new GridPane();
        infoGrid.setHgap(40);
        infoGrid.setVgap(20);

        addProfileField(infoGrid, 0, 0, "Full Name", currentUser.getFullName());
        addProfileField(infoGrid, 1, 0, "Username", currentUser.getUsername());
        addProfileField(infoGrid, 0, 1, "Email", currentUser.getEmail());
        addProfileField(infoGrid, 1, 1, "Mobile", currentUser.getMobile());
        addProfileField(infoGrid, 0, 2, "Company", currentUser.getCompany());
        addProfileField(infoGrid, 1, 2, "Job Position", currentUser.getJobPosition());
        addProfileField(infoGrid, 0, 3, "City", currentUser.getCity());
        addProfileField(infoGrid, 1, 3, "Country", currentUser.getCountry());

        if (currentUser.getBirthDate() != null) {
            addProfileField(infoGrid, 0, 4, "Birth Date",
                    new SimpleDateFormat("MMMM d, yyyy").format(currentUser.getBirthDate()));
        }

        profileCard.getChildren().addAll(avatarSection, infoGrid);

        // Edit Profile Section
        VBox editSection = new VBox(20);
        editSection.setPadding(new Insets(32));
        editSection.setStyle("-fx-background-color: #1E293B; -fx-background-radius: 16;");

        HBox editHeader = new HBox();
        editHeader.setAlignment(Pos.CENTER_LEFT);

        Label editLabel = new Label("Edit Profile");
        editLabel.setFont(Font.font("System", FontWeight.BOLD, 20));
        editLabel.setTextFill(Color.WHITE);

        Region editSpacer = new Region();
        HBox.setHgrow(editSpacer, Priority.ALWAYS);

        Button toggleEditButton = new Button("Edit");
        toggleEditButton.setStyle("-fx-background-color: #6366F1; -fx-text-fill: white; -fx-background-radius: 8; -fx-padding: 8 20;");
        FontIcon editIcon = new FontIcon("fas-edit");
        editIcon.setIconSize(14);
        editIcon.setIconColor(Color.WHITE);
        toggleEditButton.setGraphic(editIcon);

        editHeader.getChildren().addAll(editLabel, editSpacer, toggleEditButton);

        // Edit form container (initially hidden)
        VBox editFormContainer = new VBox(16);
        editFormContainer.setVisible(false);
        editFormContainer.setManaged(false);

        // Build the edit form fields
        HBox row1 = new HBox(20);
        VBox firstNameBox = createProfileInputField("First Name", currentUser.getFirstName());
        TextField firstNameField = (TextField) firstNameBox.lookup(".profile-edit-field");
        VBox lastNameBox = createProfileInputField("Last Name", currentUser.getLastName());
        TextField lastNameField = (TextField) lastNameBox.lookup(".profile-edit-field");
        HBox.setHgrow(firstNameBox, Priority.ALWAYS);
        HBox.setHgrow(lastNameBox, Priority.ALWAYS);
        row1.getChildren().addAll(firstNameBox, lastNameBox);

        HBox row2 = new HBox(20);
        VBox emailBox = createProfileInputField("Email", currentUser.getEmail());
        TextField emailField = (TextField) emailBox.lookup(".profile-edit-field");
        VBox mobileBox = createProfileInputField("Mobile", currentUser.getMobile());
        TextField mobileField = (TextField) mobileBox.lookup(".profile-edit-field");
        HBox.setHgrow(emailBox, Priority.ALWAYS);
        HBox.setHgrow(mobileBox, Priority.ALWAYS);
        row2.getChildren().addAll(emailBox, mobileBox);

        HBox row3 = new HBox(20);
        VBox companyBox = createProfileInputField("Company", currentUser.getCompany());
        TextField companyField = (TextField) companyBox.lookup(".profile-edit-field");
        VBox jobBox = createProfileInputField("Job Position", currentUser.getJobPosition());
        TextField jobField = (TextField) jobBox.lookup(".profile-edit-field");
        HBox.setHgrow(companyBox, Priority.ALWAYS);
        HBox.setHgrow(jobBox, Priority.ALWAYS);
        row3.getChildren().addAll(companyBox, jobBox);

        HBox row4 = new HBox(20);
        VBox cityBox = createProfileInputField("City", currentUser.getCity());
        TextField cityField = (TextField) cityBox.lookup(".profile-edit-field");
        VBox countryBox = createProfileInputField("Country", currentUser.getCountry());
        TextField countryField = (TextField) countryBox.lookup(".profile-edit-field");
        HBox.setHgrow(cityBox, Priority.ALWAYS);
        HBox.setHgrow(countryBox, Priority.ALWAYS);
        row4.getChildren().addAll(cityBox, countryBox);

        // Message label
        Label messageLabel = new Label();
        messageLabel.setFont(Font.font("System", 13));
        messageLabel.setVisible(false);
        messageLabel.setManaged(false);

        // Save button
        HBox buttonBox = new HBox(10);
        buttonBox.setAlignment(Pos.CENTER_LEFT);

        Button saveButton = new Button("Save Changes");
        saveButton.setStyle("-fx-background-color: linear-gradient(to right, #10B981, #059669); " +
                "-fx-text-fill: white; -fx-background-radius: 8; -fx-padding: 10 24;");
        saveButton.setFont(Font.font("System", FontWeight.BOLD, 13));
        FontIcon saveIcon = new FontIcon("fas-save");
        saveIcon.setIconSize(14);
        saveIcon.setIconColor(Color.WHITE);
        saveButton.setGraphic(saveIcon);

        Button cancelButton = new Button("Cancel");
        cancelButton.setStyle("-fx-background-color: #334155; -fx-text-fill: white; -fx-background-radius: 8; -fx-padding: 10 24;");

        buttonBox.getChildren().addAll(saveButton, cancelButton);

        editFormContainer.getChildren().addAll(row1, row2, row3, row4, messageLabel, buttonBox);

        // Toggle edit form visibility
        toggleEditButton.setOnAction(e -> {
            boolean isVisible = editFormContainer.isVisible();
            editFormContainer.setVisible(!isVisible);
            editFormContainer.setManaged(!isVisible);
            toggleEditButton.setText(isVisible ? "Edit" : "Cancel");
            toggleEditButton.setStyle(isVisible
                ? "-fx-background-color: #6366F1; -fx-text-fill: white; -fx-background-radius: 8; -fx-padding: 8 20;"
                : "-fx-background-color: #EF4444; -fx-text-fill: white; -fx-background-radius: 8; -fx-padding: 8 20;");
            if (!isVisible) {
                AnimationUtils.fadeIn(editFormContainer, 200);
            }
        });

        cancelButton.setOnAction(e -> {
            editFormContainer.setVisible(false);
            editFormContainer.setManaged(false);
            toggleEditButton.setText("Edit");
            toggleEditButton.setStyle("-fx-background-color: #6366F1; -fx-text-fill: white; -fx-background-radius: 8; -fx-padding: 8 20;");
            messageLabel.setVisible(false);
            messageLabel.setManaged(false);
        });

        ProgressIndicator saveSpinner = new ProgressIndicator();
        saveSpinner.setMaxSize(20, 20);

        saveButton.setOnAction(e -> {
            saveButton.setDisable(true);
            saveButton.setText("Saving...");
            saveButton.setGraphic(saveSpinner);

            User updatedUser = new User();
            updatedUser.setFirstName(firstNameField.getText().trim());
            updatedUser.setLastName(lastNameField.getText().trim());
            updatedUser.setEmail(emailField.getText().trim());
            updatedUser.setMobile(mobileField.getText().trim());
            updatedUser.setCompany(companyField.getText().trim());
            updatedUser.setJobPosition(jobField.getText().trim());
            updatedUser.setCity(cityField.getText().trim());
            updatedUser.setCountry(countryField.getText().trim());

            apiService.updateProfile(sessionManager.getAuthorizationHeader(), updatedUser)
                .thenAccept(result -> Platform.runLater(() -> {
                    saveButton.setDisable(false);
                    saveButton.setText("Save Changes");
                    saveButton.setGraphic(saveIcon);

                    if (result.isSuccess()) {
                        currentUser = result.getData();
                        messageLabel.setText("Profile updated successfully!");
                        messageLabel.setTextFill(Color.web("#10B981"));
                        messageLabel.setVisible(true);
                        messageLabel.setManaged(true);

                        // Refresh the profile view after a short delay
                        new Thread(() -> {
                            try {
                                Thread.sleep(1000);
                                Platform.runLater(() -> showProfileContent());
                            } catch (InterruptedException ignored) {}
                        }).start();
                    } else {
                        messageLabel.setText(result.getError());
                        messageLabel.setTextFill(Color.web("#EF4444"));
                        messageLabel.setVisible(true);
                        messageLabel.setManaged(true);
                    }
                }));
        });

        editSection.getChildren().addAll(editHeader, editFormContainer);

        profileContent.getChildren().addAll(headerLabel, profileCard, editSection);
        scrollPane.setContent(profileContent);
        contentArea.getChildren().add(scrollPane);

        AnimationUtils.fadeIn(profileContent, 300);
    }

    private VBox createProfileInputField(String labelText, String value) {
        VBox container = new VBox(6);

        Label label = new Label(labelText);
        label.setTextFill(Color.web("#94A3B8"));
        label.setFont(Font.font("System", FontWeight.MEDIUM, 12));

        TextField field = new TextField(value != null ? value : "");
        field.getStyleClass().add("profile-edit-field");
        field.setStyle("-fx-background-color: #0F172A; -fx-text-fill: white; " +
                "-fx-background-radius: 8; -fx-padding: 12; -fx-border-color: #334155; -fx-border-radius: 8;");
        field.setPrefHeight(45);

        container.getChildren().addAll(label, field);
        return container;
    }

    private void addProfileField(GridPane grid, int col, int row, String label, String value) {
        VBox field = new VBox(4);

        Label labelNode = new Label(label);
        labelNode.setFont(Font.font("System", 12));
        labelNode.setTextFill(Color.web("#94A3B8"));

        Label valueNode = new Label(value != null ? value : "N/A");
        valueNode.setFont(Font.font("System", FontWeight.MEDIUM, 15));
        valueNode.setTextFill(Color.WHITE);

        field.getChildren().addAll(labelNode, valueNode);
        grid.add(field, col, row);
    }

    // ========== Generate Users Content ==========
    private void showGenerateContent() {
        contentArea.getChildren().clear();

        VBox generateContent = new VBox(24);
        generateContent.setPadding(new Insets(0, 0, 24, 0));

        // Header
        Label headerLabel = new Label("Generate Users");
        headerLabel.setFont(Font.font("System", FontWeight.BOLD, 28));
        headerLabel.setTextFill(Color.WHITE);

        Label subLabel = new Label("Generate fake users with random data using the Faker library");
        subLabel.setFont(Font.font("System", 14));
        subLabel.setTextFill(Color.web("#94A3B8"));

        // Form card
        VBox formCard = new VBox(24);
        formCard.setPadding(new Insets(32));
        formCard.setStyle("-fx-background-color: #1E293B; -fx-background-radius: 16;");
        formCard.setMaxWidth(500);

        // Count input
        VBox countField = new VBox(8);
        Label countLabel = new Label("Total Number of Users");
        countLabel.setFont(Font.font("System", FontWeight.MEDIUM, 13));
        countLabel.setTextFill(Color.web("#94A3B8"));

        Spinner<Integer> countSpinner = new Spinner<>(1, 1000, 10, 5);
        countSpinner.setEditable(true);
        countSpinner.setMaxWidth(Double.MAX_VALUE);
        countSpinner.setStyle("-fx-background-color: #0F172A; -fx-background-radius: 8;");

        countField.getChildren().addAll(countLabel, countSpinner);

        // Admin count input
        VBox adminCountField = new VBox(8);
        Label adminCountLabel = new Label("Number of Admins");
        adminCountLabel.setFont(Font.font("System", FontWeight.MEDIUM, 13));
        adminCountLabel.setTextFill(Color.web("#94A3B8"));

        Spinner<Integer> adminCountSpinner = new Spinner<>(0, 1000, 0, 1);
        adminCountSpinner.setEditable(true);
        adminCountSpinner.setMaxWidth(Double.MAX_VALUE);
        adminCountSpinner.setStyle("-fx-background-color: #0F172A; -fx-background-radius: 8;");

        // Helper text for admin count
        Label adminHelpLabel = new Label("Admins will have ROLE_ADMIN, the rest will have ROLE_USER");
        adminHelpLabel.setFont(Font.font("System", 11));
        adminHelpLabel.setTextFill(Color.web("#64748B"));

        adminCountField.getChildren().addAll(adminCountLabel, adminCountSpinner, adminHelpLabel);

        // Update admin max when total changes
        countSpinner.valueProperty().addListener((obs, oldVal, newVal) -> {
            SpinnerValueFactory.IntegerSpinnerValueFactory factory =
                (SpinnerValueFactory.IntegerSpinnerValueFactory) adminCountSpinner.getValueFactory();
            factory.setMax(newVal);
            if (adminCountSpinner.getValue() > newVal) {
                adminCountSpinner.getValueFactory().setValue(newVal);
            }
        });

        // Description
        Label descLabel = new Label("This will generate a JSON file containing fake user data that can be " +
                "imported using the Batch Import feature. Each user will have randomized information " +
                "including name, email, company, and job position.");
        descLabel.setFont(Font.font("System", 13));
        descLabel.setTextFill(Color.web("#CBD5E1"));
        descLabel.setWrapText(true);

        // Status label
        Label statusLabel = new Label();
        statusLabel.setFont(Font.font("System", 13));
        statusLabel.setVisible(false);

        // Generate button
        Button generateButton = new Button("Generate & Download");
        generateButton.setPrefHeight(48);
        generateButton.setMaxWidth(Double.MAX_VALUE);
        generateButton.setFont(Font.font("System", FontWeight.BOLD, 14));
        generateButton.setStyle("-fx-background-color: linear-gradient(to right, #6366F1, #8B5CF6); " +
                "-fx-text-fill: white; -fx-background-radius: 10; -fx-cursor: hand;");

        FontIcon genIcon = new FontIcon("fas-download");
        genIcon.setIconSize(16);
        genIcon.setIconColor(Color.WHITE);
        generateButton.setGraphic(genIcon);

        ProgressIndicator progress = new ProgressIndicator();
        progress.setMaxSize(20, 20);

        generateButton.setOnAction(e -> {
            int count = countSpinner.getValue();
            int adminCount = adminCountSpinner.getValue();
            generateButton.setDisable(true);
            generateButton.setText("Generating...");
            generateButton.setGraphic(progress);
            statusLabel.setVisible(false);

            apiService.generateUsers(sessionManager.getAuthorizationHeader(), count, adminCount)
                .thenAccept(result -> Platform.runLater(() -> {
                    generateButton.setDisable(false);
                    generateButton.setText("Generate & Download");
                    generateButton.setGraphic(genIcon);

                    if (result.isSuccess()) {
                        // Save file
                        FileChooser fileChooser = new FileChooser();
                        fileChooser.setTitle("Save Users JSON");
                        fileChooser.setInitialFileName("users.json");
                        fileChooser.getExtensionFilters().add(
                            new FileChooser.ExtensionFilter("JSON Files", "*.json"));

                        File file = fileChooser.showSaveDialog(SceneManager.getPrimaryStage());
                        if (file != null) {
                            try (FileOutputStream fos = new FileOutputStream(file)) {
                                fos.write(result.getData());
                                String roleInfo = adminCount > 0
                                    ? " (" + adminCount + " admins, " + (count - adminCount) + " users)"
                                    : " (all regular users)";
                                statusLabel.setText("✓ Successfully saved " + count + " users" + roleInfo + " to " + file.getName());
                                statusLabel.setTextFill(Color.web("#10B981"));
                                statusLabel.setVisible(true);
                            } catch (IOException ex) {
                                statusLabel.setText("✗ Failed to save file: " + ex.getMessage());
                                statusLabel.setTextFill(Color.web("#EF4444"));
                                statusLabel.setVisible(true);
                            }
                        }
                    } else {
                        statusLabel.setText("✗ " + result.getError());
                        statusLabel.setTextFill(Color.web("#EF4444"));
                        statusLabel.setVisible(true);
                    }
                }));
        });

        formCard.getChildren().addAll(countField, adminCountField, descLabel, statusLabel, generateButton);

        generateContent.getChildren().addAll(headerLabel, subLabel, formCard);
        contentArea.getChildren().add(generateContent);

        AnimationUtils.fadeIn(generateContent, 300);
    }

    // ========== Import Users Content ==========
    private void showImportContent() {
        contentArea.getChildren().clear();

        VBox importContent = new VBox(24);
        importContent.setPadding(new Insets(0, 0, 24, 0));

        // Header
        Label headerLabel = new Label("Import Users");
        headerLabel.setFont(Font.font("System", FontWeight.BOLD, 28));
        headerLabel.setTextFill(Color.WHITE);

        Label subLabel = new Label("Import users from a JSON file into the database");
        subLabel.setFont(Font.font("System", 14));
        subLabel.setTextFill(Color.web("#94A3B8"));

        // Upload card
        VBox uploadCard = new VBox(24);
        uploadCard.setPadding(new Insets(48));
        uploadCard.setAlignment(Pos.CENTER);
        uploadCard.setStyle("-fx-background-color: #1E293B; -fx-background-radius: 16; " +
                "-fx-border-color: #334155; -fx-border-width: 2; -fx-border-style: dashed; -fx-border-radius: 16;");
        uploadCard.setMaxWidth(600);

        FontIcon uploadIcon = new FontIcon("fas-cloud-upload-alt");
        uploadIcon.setIconSize(64);
        uploadIcon.setIconColor(Color.web("#6366F1"));

        Label uploadLabel = new Label("Drag & Drop or Click to Upload");
        uploadLabel.setFont(Font.font("System", FontWeight.BOLD, 18));
        uploadLabel.setTextFill(Color.WHITE);

        Label fileTypeLabel = new Label("Supported format: JSON");
        fileTypeLabel.setFont(Font.font("System", 13));
        fileTypeLabel.setTextFill(Color.web("#94A3B8"));

        // Selected file label
        Label selectedFileLabel = new Label();
        selectedFileLabel.setFont(Font.font("System", FontWeight.MEDIUM, 14));
        selectedFileLabel.setTextFill(Color.web("#10B981"));
        selectedFileLabel.setVisible(false);

        // Result card
        VBox resultCard = new VBox(16);
        resultCard.setPadding(new Insets(24));
        resultCard.setStyle("-fx-background-color: #1E293B; -fx-background-radius: 16;");
        resultCard.setMaxWidth(600);
        resultCard.setVisible(false);
        resultCard.setManaged(false);

        Label resultTitle = new Label("Import Results");
        resultTitle.setFont(Font.font("System", FontWeight.BOLD, 16));
        resultTitle.setTextFill(Color.WHITE);

        HBox resultStats = new HBox(20);
        resultStats.setAlignment(Pos.CENTER);

        VBox totalStat = createImportStat("Total Records", "0", Color.web("#3B82F6"));
        VBox successStat = createImportStat("Successful", "0", Color.web("#10B981"));
        VBox failedStat = createImportStat("Failed", "0", Color.web("#EF4444"));

        resultStats.getChildren().addAll(totalStat, successStat, failedStat);
        resultCard.getChildren().addAll(resultTitle, resultStats);

        // Upload button
        Button uploadButton = new Button("Choose File");
        uploadButton.setPrefHeight(48);
        uploadButton.setPrefWidth(200);
        uploadButton.setFont(Font.font("System", FontWeight.BOLD, 14));
        uploadButton.setStyle("-fx-background-color: #6366F1; -fx-text-fill: white; -fx-background-radius: 10; -fx-cursor: hand;");

        FontIcon fileIcon = new FontIcon("fas-file-upload");
        fileIcon.setIconSize(16);
        fileIcon.setIconColor(Color.WHITE);
        uploadButton.setGraphic(fileIcon);

        // Import button
        Button importButton = new Button("Import Users");
        importButton.setPrefHeight(48);
        importButton.setPrefWidth(200);
        importButton.setFont(Font.font("System", FontWeight.BOLD, 14));
        importButton.setStyle("-fx-background-color: #10B981; -fx-text-fill: white; -fx-background-radius: 10; -fx-cursor: hand;");
        importButton.setVisible(false);
        importButton.setManaged(false);

        FontIcon impIcon = new FontIcon("fas-database");
        impIcon.setIconSize(16);
        impIcon.setIconColor(Color.WHITE);
        importButton.setGraphic(impIcon);

        ProgressIndicator progress = new ProgressIndicator();
        progress.setMaxSize(20, 20);

        final File[] selectedFile = {null};

        uploadButton.setOnAction(e -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Select Users JSON File");
            fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("JSON Files", "*.json"));

            File file = fileChooser.showOpenDialog(SceneManager.getPrimaryStage());
            if (file != null) {
                selectedFile[0] = file;
                selectedFileLabel.setText("Selected: " + file.getName());
                selectedFileLabel.setVisible(true);
                importButton.setVisible(true);
                importButton.setManaged(true);
                resultCard.setVisible(false);
                resultCard.setManaged(false);
            }
        });

        importButton.setOnAction(e -> {
            if (selectedFile[0] != null) {
                importButton.setDisable(true);
                importButton.setText("Importing...");
                importButton.setGraphic(progress);

                apiService.batchImportUsers(sessionManager.getAuthorizationHeader(), selectedFile[0])
                    .thenAccept(result -> Platform.runLater(() -> {
                        importButton.setDisable(false);
                        importButton.setText("Import Users");
                        importButton.setGraphic(impIcon);

                        if (result.isSuccess()) {
                            BatchImportResult importResult = result.getData();

                            // Update result stats
                            ((Label) totalStat.getChildren().get(1)).setText(String.valueOf(importResult.getTotalRecords()));
                            ((Label) successStat.getChildren().get(1)).setText(String.valueOf(importResult.getSuccessfulImports()));
                            ((Label) failedStat.getChildren().get(1)).setText(String.valueOf(importResult.getFailedImports()));

                            resultCard.setVisible(true);
                            resultCard.setManaged(true);
                            AnimationUtils.fadeIn(resultCard, 300);
                        } else {
                            resultTitle.setText("Import Failed");
                            resultTitle.setTextFill(Color.web("#EF4444"));
                            resultCard.setVisible(true);
                            resultCard.setManaged(true);
                        }
                    }));
            }
        });

        HBox buttons = new HBox(16);
        buttons.setAlignment(Pos.CENTER);
        buttons.getChildren().addAll(uploadButton, importButton);

        uploadCard.getChildren().addAll(uploadIcon, uploadLabel, fileTypeLabel, selectedFileLabel, buttons);

        importContent.getChildren().addAll(headerLabel, subLabel, uploadCard, resultCard);
        contentArea.getChildren().add(importContent);

        AnimationUtils.fadeIn(importContent, 300);
    }

    private VBox createImportStat(String label, String value, Color accentColor) {
        VBox stat = new VBox(4);
        stat.setAlignment(Pos.CENTER);
        stat.setPadding(new Insets(16));
        stat.setStyle("-fx-background-color: " + UIComponents.toHexString(accentColor) + "15; -fx-background-radius: 12;");
        stat.setMinWidth(120);

        Label labelNode = new Label(label);
        labelNode.setFont(Font.font("System", 12));
        labelNode.setTextFill(Color.web("#94A3B8"));

        Label valueNode = new Label(value);
        valueNode.setFont(Font.font("System", FontWeight.BOLD, 24));
        valueNode.setTextFill(accentColor);

        stat.getChildren().addAll(labelNode, valueNode);
        return stat;
    }

    // ========== User Lookup Content (Admin Only) ==========
    private void showLookupContent() {
        contentArea.getChildren().clear();

        VBox lookupContent = new VBox(24);
        lookupContent.setPadding(new Insets(0, 0, 24, 0));

        // Header
        Label headerLabel = new Label("User Lookup");
        headerLabel.setFont(Font.font("System", FontWeight.BOLD, 28));
        headerLabel.setTextFill(Color.WHITE);

        Label subLabel = new Label("Search and view user profiles by username");
        subLabel.setFont(Font.font("System", 14));
        subLabel.setTextFill(Color.web("#94A3B8"));

        // Search card
        HBox searchCard = new HBox(16);
        searchCard.setPadding(new Insets(24));
        searchCard.setAlignment(Pos.CENTER_LEFT);
        searchCard.setStyle("-fx-background-color: #1E293B; -fx-background-radius: 16;");
        searchCard.setMaxWidth(600);

        TextField searchField = new TextField();
        searchField.setPromptText("Enter username...");
        searchField.setPrefHeight(48);
        searchField.setStyle("-fx-background-color: #0F172A; -fx-background-radius: 10; -fx-text-fill: white; " +
                "-fx-prompt-text-fill: #64748B; -fx-padding: 0 16;");
        HBox.setHgrow(searchField, Priority.ALWAYS);

        Button searchButton = new Button("Search");
        searchButton.setPrefHeight(48);
        searchButton.setPrefWidth(120);
        searchButton.setFont(Font.font("System", FontWeight.BOLD, 14));
        searchButton.setStyle("-fx-background-color: #6366F1; -fx-text-fill: white; -fx-background-radius: 10; -fx-cursor: hand;");

        FontIcon searchIcon = new FontIcon("fas-search");
        searchIcon.setIconSize(14);
        searchIcon.setIconColor(Color.WHITE);
        searchButton.setGraphic(searchIcon);

        searchCard.getChildren().addAll(searchField, searchButton);

        // Result area
        VBox resultArea = new VBox(16);
        resultArea.setVisible(false);
        resultArea.setManaged(false);

        ProgressIndicator progress = new ProgressIndicator();
        progress.setMaxSize(20, 20);

        searchButton.setOnAction(e -> {
            String username = searchField.getText().trim();
            if (!username.isEmpty()) {
                searchButton.setDisable(true);
                searchButton.setText("...");
                searchButton.setGraphic(progress);
                resultArea.setVisible(false);
                resultArea.setManaged(false);

                apiService.getUserProfile(sessionManager.getAuthorizationHeader(), username)
                    .thenAccept(result -> Platform.runLater(() -> {
                        searchButton.setDisable(false);
                        searchButton.setText("Search");
                        searchButton.setGraphic(searchIcon);

                        resultArea.getChildren().clear();

                        if (result.isSuccess()) {
                            User user = result.getData();
                            resultArea.getChildren().add(createUserResultCard(user));
                        } else {
                            Label errorLabel = new Label("✗ " + result.getError());
                            errorLabel.setFont(Font.font("System", 14));
                            errorLabel.setTextFill(Color.web("#EF4444"));
                            resultArea.getChildren().add(errorLabel);
                        }

                        resultArea.setVisible(true);
                        resultArea.setManaged(true);
                        AnimationUtils.fadeIn(resultArea, 300);
                    }));
            }
        });

        searchField.setOnAction(e -> searchButton.fire());

        lookupContent.getChildren().addAll(headerLabel, subLabel, searchCard, resultArea);
        contentArea.getChildren().add(lookupContent);

        AnimationUtils.fadeIn(lookupContent, 300);
    }

    private VBox createUserResultCard(User user) {
        VBox card = new VBox(20);
        card.setPadding(new Insets(24));
        card.setStyle("-fx-background-color: #1E293B; -fx-background-radius: 16;");
        card.setMaxWidth(600);

        // Header with avatar
        HBox header = new HBox(16);
        header.setAlignment(Pos.CENTER_LEFT);

        StackPane avatar = new StackPane();
        Circle avatarCircle = new Circle(30);
        avatarCircle.setFill(new LinearGradient(0, 0, 1, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#6366F1")),
                new Stop(1, Color.web("#8B5CF6"))));

        String initials = "";
        if (user.getFirstName() != null) initials += user.getFirstName().charAt(0);
        if (user.getLastName() != null) initials += user.getLastName().charAt(0);
        if (initials.isEmpty()) initials = user.getUsername().substring(0, 1).toUpperCase();

        Label avatarLabel = new Label(initials.toUpperCase());
        avatarLabel.setFont(Font.font("System", FontWeight.BOLD, 16));
        avatarLabel.setTextFill(Color.WHITE);
        avatar.getChildren().addAll(avatarCircle, avatarLabel);

        VBox nameBox = new VBox(2);
        Label nameLabel = new Label(user.getFullName());
        nameLabel.setFont(Font.font("System", FontWeight.BOLD, 18));
        nameLabel.setTextFill(Color.WHITE);

        Label usernameLabel = new Label("@" + user.getUsername());
        usernameLabel.setFont(Font.font("System", 13));
        usernameLabel.setTextFill(Color.web("#94A3B8"));

        nameBox.getChildren().addAll(nameLabel, usernameLabel);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label roleBadge = new Label(user.getRole() != null ? user.getRole().getDisplayName() : "USER");
        roleBadge.setPadding(new Insets(6, 16, 6, 16));
        roleBadge.setFont(Font.font("System", FontWeight.BOLD, 11));
        roleBadge.setTextFill(Color.WHITE);
        roleBadge.setStyle("-fx-background-color: " + (user.isAdmin() ? "#6366F1" : "#10B981") + "; -fx-background-radius: 20;");

        header.getChildren().addAll(avatar, nameBox, spacer, roleBadge);

        // Details grid
        GridPane grid = new GridPane();
        grid.setHgap(40);
        grid.setVgap(16);

        addProfileField(grid, 0, 0, "Email", user.getEmail());
        addProfileField(grid, 1, 0, "Mobile", user.getMobile());
        addProfileField(grid, 0, 1, "Company", user.getCompany());
        addProfileField(grid, 1, 1, "Position", user.getJobPosition());
        addProfileField(grid, 0, 2, "Location", user.getLocation());

        card.getChildren().addAll(header, new Separator(), grid);

        return card;
    }

    // ========== Helper Methods ==========
    private void showLoadingState(String message) {
        contentArea.getChildren().clear();

        VBox loadingBox = new VBox(16);
        loadingBox.setAlignment(Pos.CENTER);

        ProgressIndicator spinner = new ProgressIndicator();
        spinner.setMaxSize(50, 50);

        Label label = new Label(message);
        label.setFont(Font.font("System", 14));
        label.setTextFill(Color.web("#94A3B8"));

        loadingBox.getChildren().addAll(spinner, label);
        contentArea.getChildren().add(loadingBox);
    }

    private void showErrorState(String message) {
        contentArea.getChildren().clear();

        VBox errorBox = new VBox(16);
        errorBox.setAlignment(Pos.CENTER);

        FontIcon icon = new FontIcon("fas-exclamation-triangle");
        icon.setIconSize(48);
        icon.setIconColor(Color.web("#EF4444"));

        Label label = new Label(message);
        label.setFont(Font.font("System", 14));
        label.setTextFill(Color.web("#EF4444"));
        label.setWrapText(true);

        Button retryButton = new Button("Retry");
        retryButton.setStyle("-fx-background-color: #6366F1; -fx-text-fill: white; -fx-background-radius: 8;");
        retryButton.setOnAction(e -> loadUserProfile());

        errorBox.getChildren().addAll(icon, label, retryButton);
        contentArea.getChildren().add(errorBox);
    }

    private void handleLogout() {
        sessionManager.logout();
        SceneManager.logout();
    }

    // ==================== SETTINGS CONTENT ====================
    private void showSettingsContent() {
        contentArea.getChildren().clear();

        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent;");

        VBox settingsContent = new VBox(30);
        settingsContent.setPadding(new Insets(40));
        settingsContent.setStyle("-fx-background-color: #0F172A;");

        // Header
        Label headerLabel = new Label("Settings");
        headerLabel.setFont(Font.font("System", FontWeight.BOLD, 32));
        headerLabel.setTextFill(Color.WHITE);

        Label subLabel = new Label("Manage your account settings and preferences");
        subLabel.setFont(Font.font("System", 14));
        subLabel.setTextFill(Color.web("#94A3B8"));

        VBox headerBox = new VBox(8);
        headerBox.getChildren().addAll(headerLabel, subLabel);

        // Change Password Card
        VBox passwordCard = new VBox(20);
        passwordCard.setPadding(new Insets(30));
        passwordCard.setStyle("-fx-background-color: #1E293B; -fx-background-radius: 16;");
        passwordCard.setMaxWidth(500);

        Label passwordTitle = new Label("Change Password");
        passwordTitle.setFont(Font.font("System", FontWeight.BOLD, 20));
        passwordTitle.setTextFill(Color.WHITE);

        Label passwordDesc = new Label("Update your password to keep your account secure");
        passwordDesc.setFont(Font.font("System", 13));
        passwordDesc.setTextFill(Color.web("#94A3B8"));

        // Current password
        VBox currentPwBox = createSettingsInputField("Current Password", true);
        PasswordField currentPwField = (PasswordField) currentPwBox.lookup(".settings-field");

        // New password
        VBox newPwBox = createSettingsInputField("New Password", true);
        PasswordField newPwField = (PasswordField) newPwBox.lookup(".settings-field");

        // Confirm password
        VBox confirmPwBox = createSettingsInputField("Confirm New Password", true);
        PasswordField confirmPwField = (PasswordField) confirmPwBox.lookup(".settings-field");

        // Message label
        Label pwMessageLabel = new Label();
        pwMessageLabel.setFont(Font.font("System", 13));
        pwMessageLabel.setVisible(false);
        pwMessageLabel.setManaged(false);

        // Change password button
        Button changePwButton = new Button("Change Password");
        changePwButton.setPrefHeight(45);
        changePwButton.setPrefWidth(200);
        changePwButton.setFont(Font.font("System", FontWeight.BOLD, 14));
        changePwButton.setStyle("-fx-background-color: linear-gradient(to right, #6366F1, #8B5CF6); " +
                "-fx-text-fill: white; -fx-background-radius: 10; -fx-cursor: hand;");

        FontIcon pwIcon = new FontIcon("fas-key");
        pwIcon.setIconSize(14);
        pwIcon.setIconColor(Color.WHITE);
        changePwButton.setGraphic(pwIcon);

        ProgressIndicator pwSpinner = new ProgressIndicator();
        pwSpinner.setMaxSize(20, 20);

        changePwButton.setOnAction(e -> {
            String currentPw = currentPwField.getText();
            String newPw = newPwField.getText();
            String confirmPw = confirmPwField.getText();

            if (currentPw.isEmpty() || newPw.isEmpty() || confirmPw.isEmpty()) {
                pwMessageLabel.setText("Please fill in all fields");
                pwMessageLabel.setTextFill(Color.web("#EF4444"));
                pwMessageLabel.setVisible(true);
                pwMessageLabel.setManaged(true);
                return;
            }

            if (newPw.length() < 6) {
                pwMessageLabel.setText("New password must be at least 6 characters");
                pwMessageLabel.setTextFill(Color.web("#EF4444"));
                pwMessageLabel.setVisible(true);
                pwMessageLabel.setManaged(true);
                return;
            }

            if (!newPw.equals(confirmPw)) {
                pwMessageLabel.setText("New passwords do not match");
                pwMessageLabel.setTextFill(Color.web("#EF4444"));
                pwMessageLabel.setVisible(true);
                pwMessageLabel.setManaged(true);
                return;
            }

            changePwButton.setDisable(true);
            changePwButton.setText("Changing...");
            changePwButton.setGraphic(pwSpinner);

            PasswordChangeRequest request = new PasswordChangeRequest(currentPw, newPw);
            apiService.changePassword(sessionManager.getAuthorizationHeader(), request)
                .thenAccept(result -> Platform.runLater(() -> {
                    changePwButton.setDisable(false);
                    changePwButton.setText("Change Password");
                    changePwButton.setGraphic(pwIcon);

                    if (result.isSuccess()) {
                        pwMessageLabel.setText("Password changed successfully!");
                        pwMessageLabel.setTextFill(Color.web("#10B981"));
                        currentPwField.clear();
                        newPwField.clear();
                        confirmPwField.clear();
                    } else {
                        pwMessageLabel.setText(result.getError());
                        pwMessageLabel.setTextFill(Color.web("#EF4444"));
                    }
                    pwMessageLabel.setVisible(true);
                    pwMessageLabel.setManaged(true);
                }));
        });

        passwordCard.getChildren().addAll(passwordTitle, passwordDesc, currentPwBox, newPwBox, confirmPwBox, pwMessageLabel, changePwButton);

        settingsContent.getChildren().addAll(headerBox, passwordCard);
        scrollPane.setContent(settingsContent);
        contentArea.getChildren().add(scrollPane);

        AnimationUtils.fadeIn(settingsContent, 300);
    }

    private VBox createSettingsInputField(String labelText, boolean isPassword) {
        VBox container = new VBox(6);

        Label label = new Label(labelText);
        label.setTextFill(Color.web("#94A3B8"));
        label.setFont(Font.font("System", FontWeight.MEDIUM, 12));

        Control field;
        if (isPassword) {
            field = new PasswordField();
        } else {
            field = new TextField();
        }
        field.getStyleClass().add("settings-field");
        field.setStyle("-fx-background-color: #0F172A; -fx-text-fill: white; " +
                "-fx-background-radius: 8; -fx-padding: 12; -fx-border-color: #334155; -fx-border-radius: 8;");
        field.setPrefHeight(45);

        container.getChildren().addAll(label, field);
        return container;
    }


    // ==================== USER LIST CONTENT (Admin) ====================
    private void showUserListContent() {
        contentArea.getChildren().clear();

        // Main scrollable container
        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);

        VBox userListContent = new VBox(20);
        userListContent.setPadding(new Insets(40));
        userListContent.setStyle("-fx-background-color: #0F172A;");

        // Header
        HBox headerBox = new HBox(20);
        headerBox.setAlignment(Pos.CENTER_LEFT);

        VBox titleBox = new VBox(8);
        Label headerLabel = new Label("All Users");
        headerLabel.setFont(Font.font("System", FontWeight.BOLD, 32));
        headerLabel.setTextFill(Color.WHITE);

        Label subLabel = new Label("View and manage all registered users");
        subLabel.setFont(Font.font("System", 14));
        subLabel.setTextFill(Color.web("#94A3B8"));
        titleBox.getChildren().addAll(headerLabel, subLabel);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Search box
        HBox searchBox = new HBox(10);
        searchBox.setAlignment(Pos.CENTER_RIGHT);

        TextField searchField = new TextField();
        searchField.setPromptText("Search users...");
        searchField.setPrefWidth(250);
        searchField.setPrefHeight(40);
        searchField.setStyle("-fx-background-color: #1E293B; -fx-text-fill: white; " +
                "-fx-prompt-text-fill: #64748B; -fx-background-radius: 8; -fx-padding: 0 12;");

        Button searchButton = new Button("Search");
        searchButton.setStyle("-fx-background-color: #6366F1; -fx-text-fill: white; -fx-background-radius: 8; -fx-padding: 10 20;");
        FontIcon searchIcon = new FontIcon("fas-search");
        searchIcon.setIconSize(14);
        searchIcon.setIconColor(Color.WHITE);
        searchButton.setGraphic(searchIcon);

        Button clearButton = new Button("Clear");
        clearButton.setStyle("-fx-background-color: #334155; -fx-text-fill: white; -fx-background-radius: 8; -fx-padding: 10 20;");

        // Export CSV button
        Button exportCsvButton = new Button("Export CSV");
        exportCsvButton.setStyle("-fx-background-color: #10B981; -fx-text-fill: white; -fx-background-radius: 8; -fx-padding: 10 20;");
        FontIcon exportIcon = new FontIcon("fas-file-csv");
        exportIcon.setIconSize(14);
        exportIcon.setIconColor(Color.WHITE);
        exportCsvButton.setGraphic(exportIcon);

        searchBox.getChildren().addAll(searchField, searchButton, clearButton, exportCsvButton);
        headerBox.getChildren().addAll(titleBox, spacer, searchBox);

        // Users table container
        VBox tableContainer = new VBox(0);
        tableContainer.setStyle("-fx-background-color: #1E293B; -fx-background-radius: 16;");
        tableContainer.setPadding(new Insets(20));

        // Pagination controls
        HBox paginationBox = new HBox(15);
        paginationBox.setAlignment(Pos.CENTER);
        paginationBox.setPadding(new Insets(10, 0, 0, 0));

        Button prevButton = new Button("← Previous");
        prevButton.setStyle("-fx-background-color: #334155; -fx-text-fill: white; -fx-background-radius: 8; -fx-padding: 10 20;");

        Label pageLabel = new Label("Page 1 of 1");
        pageLabel.setTextFill(Color.web("#94A3B8"));
        pageLabel.setFont(Font.font("System", FontWeight.MEDIUM, 14));

        Button nextButton = new Button("Next →");
        nextButton.setStyle("-fx-background-color: #334155; -fx-text-fill: white; -fx-background-radius: 8; -fx-padding: 10 20;");

        // Page size selector
        Label pageSizeLabel = new Label("Per page:");
        pageSizeLabel.setTextFill(Color.web("#94A3B8"));

        ComboBox<Integer> pageSizeCombo = new ComboBox<>();
        pageSizeCombo.getItems().addAll(5, 10, 20, 50);
        pageSizeCombo.setValue(10);
        pageSizeCombo.setStyle("-fx-background-color: #334155;");

        paginationBox.getChildren().addAll(prevButton, pageLabel, nextButton,
                new Region() {{ HBox.setHgrow(this, Priority.ALWAYS); }},
                pageSizeLabel, pageSizeCombo);

        // State
        final int[] currentPage = {0};
        final String[] currentSearch = {""};
        final Runnable[] loadUsersRef = new Runnable[1];

        // Load users function
        loadUsersRef[0] = () -> {
            tableContainer.getChildren().clear();
            ProgressIndicator spinner = new ProgressIndicator();
            spinner.setMaxSize(40, 40);
            VBox spinnerBox = new VBox(spinner);
            spinnerBox.setAlignment(Pos.CENTER);
            spinnerBox.setPadding(new Insets(40));
            tableContainer.getChildren().add(spinnerBox);

            int pageSize = pageSizeCombo.getValue();

            apiService.getAllUsers(sessionManager.getAuthorizationHeader(), currentPage[0], pageSize, currentSortBy, currentSortDir, currentSearch[0])
                .thenAccept(result -> Platform.runLater(() -> {
                    tableContainer.getChildren().clear();

                    if (result.isSuccess()) {
                        UserListResponse response = result.getData();
                        int totalPages = Math.max(1, response.getTotalPages());
                        pageLabel.setText("Page " + (response.getCurrentPage() + 1) + " of " + totalPages +
                                " (" + response.getTotalItems() + " total)");
                        prevButton.setDisable(response.getCurrentPage() == 0);
                        nextButton.setDisable(response.getCurrentPage() >= response.getTotalPages() - 1);

                        // Table header with sort callback
                        HBox headerRow = createUserTableHeader(loadUsersRef[0]);
                        tableContainer.getChildren().add(headerRow);

                        // User rows in a scrollable VBox
                        VBox rowsContainer = new VBox(0);

                        if (response.getUsers() != null && !response.getUsers().isEmpty()) {
                            for (User user : response.getUsers()) {
                                HBox userRow = createUserTableRow(user, loadUsersRef[0]);
                                rowsContainer.getChildren().add(userRow);
                            }
                        } else {
                            Label noUsersLabel = new Label("No users found");
                            noUsersLabel.setTextFill(Color.web("#94A3B8"));
                            noUsersLabel.setPadding(new Insets(40));
                            noUsersLabel.setFont(Font.font("System", 16));
                            rowsContainer.getChildren().add(noUsersLabel);
                            rowsContainer.setAlignment(Pos.CENTER);
                        }

                        tableContainer.getChildren().add(rowsContainer);

                    } else {
                        Label errorLabel = new Label("Failed to load users: " + result.getError());
                        errorLabel.setTextFill(Color.web("#EF4444"));
                        errorLabel.setPadding(new Insets(20));
                        tableContainer.getChildren().add(errorLabel);
                    }
                }));
        };

        // Event handlers
        searchButton.setOnAction(e -> {
            currentPage[0] = 0;
            currentSearch[0] = searchField.getText().trim();
            loadUsersRef[0].run();
        });

        searchField.setOnAction(e -> {
            currentPage[0] = 0;
            currentSearch[0] = searchField.getText().trim();
            loadUsersRef[0].run();
        });

        clearButton.setOnAction(e -> {
            searchField.clear();
            currentPage[0] = 0;
            currentSearch[0] = "";
            loadUsersRef[0].run();
        });

        // Export CSV handler
        exportCsvButton.setOnAction(e -> {
            exportCsvButton.setDisable(true);
            exportCsvButton.setText("Exporting...");

            apiService.exportUsersToCsv(sessionManager.getAuthorizationHeader(), currentSearch[0])
                .thenAccept(result -> Platform.runLater(() -> {
                    exportCsvButton.setDisable(false);
                    exportCsvButton.setText("Export CSV");
                    exportCsvButton.setGraphic(exportIcon);

                    if (result.isSuccess()) {
                        FileChooser fileChooser = new FileChooser();
                        fileChooser.setTitle("Save CSV File");
                        fileChooser.setInitialFileName("users_export.csv");
                        fileChooser.getExtensionFilters().add(
                            new FileChooser.ExtensionFilter("CSV Files", "*.csv"));

                        File file = fileChooser.showSaveDialog(SceneManager.getPrimaryStage());
                        if (file != null) {
                            try (FileOutputStream fos = new FileOutputStream(file)) {
                                fos.write(result.getData());
                            } catch (IOException ex) {
                                // Show error
                            }
                        }
                    }
                }));
        });

        prevButton.setOnAction(e -> {
            currentPage[0]--;
            loadUsersRef[0].run();
        });

        nextButton.setOnAction(e -> {
            currentPage[0]++;
            loadUsersRef[0].run();
        });

        pageSizeCombo.setOnAction(e -> {
            currentPage[0] = 0;
            loadUsersRef[0].run();
        });

        userListContent.getChildren().addAll(headerBox, tableContainer, paginationBox);
        scrollPane.setContent(userListContent);
        contentArea.getChildren().add(scrollPane);

        // Initial load
        loadUsersRef[0].run();

        AnimationUtils.fadeIn(userListContent, 300);
    }

    // Sort state
    private String currentSortBy = "username";
    private String currentSortDir = "asc";

    private HBox createUserTableHeader(Runnable reloadCallback) {
        HBox header = new HBox();
        header.setStyle("-fx-background-color: #334155; -fx-background-radius: 8;");
        header.setPadding(new Insets(12, 16, 12, 16));
        header.setAlignment(Pos.CENTER_LEFT);

        String[] columns = {"Username", "Email", "Name", "Company", "Role", "Status", "Last Login", "Actions"};
        String[] sortFields = {"username", "email", "firstName", "company", null, "enabled", "lastLogin", null};
        double[] widths = {120, 160, 120, 100, 70, 70, 100, 120};

        for (int i = 0; i < columns.length; i++) {
            final int index = i;
            final String sortField = sortFields[i];

            HBox colBox = new HBox(4);
            colBox.setAlignment(Pos.CENTER_LEFT);
            colBox.setPrefWidth(widths[i]);

            Label colLabel = new Label(columns[i]);
            colLabel.setFont(Font.font("System", FontWeight.BOLD, 12));
            colLabel.setTextFill(Color.web("#94A3B8"));

            colBox.getChildren().add(colLabel);

            // Add sort indicator for sortable columns
            if (sortField != null) {
                colBox.setStyle("-fx-cursor: hand;");

                if (currentSortBy.equals(sortField)) {
                    FontIcon sortIcon = new FontIcon(currentSortDir.equals("asc") ? "fas-sort-up" : "fas-sort-down");
                    sortIcon.setIconSize(10);
                    sortIcon.setIconColor(Color.web("#818CF8"));
                    colBox.getChildren().add(sortIcon);
                }

                colBox.setOnMouseClicked(e -> {
                    if (currentSortBy.equals(sortField)) {
                        currentSortDir = currentSortDir.equals("asc") ? "desc" : "asc";
                    } else {
                        currentSortBy = sortField;
                        currentSortDir = "asc";
                    }
                    reloadCallback.run();
                });

                colBox.setOnMouseEntered(e -> colLabel.setTextFill(Color.web("#818CF8")));
                colBox.setOnMouseExited(e -> colLabel.setTextFill(Color.web("#94A3B8")));
            }

            header.getChildren().add(colBox);
        }

        return header;
    }

    private HBox createUserTableRow(User user, Runnable reloadCallback) {
        HBox row = new HBox();
        row.setStyle("-fx-background-color: transparent; -fx-border-color: #334155; -fx-border-width: 0 0 1 0;");
        row.setPadding(new Insets(10, 16, 10, 16));
        row.setAlignment(Pos.CENTER_LEFT);

        row.setOnMouseEntered(e -> row.setStyle("-fx-background-color: #334155; -fx-border-color: #334155; -fx-border-width: 0 0 1 0;"));
        row.setOnMouseExited(e -> row.setStyle("-fx-background-color: transparent; -fx-border-color: #334155; -fx-border-width: 0 0 1 0;"));

        double[] widths = {120, 160, 120, 100, 70, 70, 100, 120};

        // Username (clickable for details)
        Label usernameLabel = new Label(user.getUsername() != null ? user.getUsername() : "-");
        usernameLabel.setTextFill(Color.web("#818CF8"));
        usernameLabel.setFont(Font.font("System", FontWeight.MEDIUM, 13));
        usernameLabel.setPrefWidth(widths[0]);
        usernameLabel.setStyle("-fx-cursor: hand;");
        usernameLabel.setOnMouseClicked(e -> showUserDetailsModal(user, reloadCallback));
        usernameLabel.setOnMouseEntered(e -> usernameLabel.setUnderline(true));
        usernameLabel.setOnMouseExited(e -> usernameLabel.setUnderline(false));

        Label emailLabel = new Label(user.getEmail() != null ? user.getEmail() : "-");
        emailLabel.setTextFill(Color.web("#94A3B8"));
        emailLabel.setFont(Font.font("System", 12));
        emailLabel.setPrefWidth(widths[1]);

        String fullName = (user.getFirstName() != null ? user.getFirstName() : "") + " " +
                         (user.getLastName() != null ? user.getLastName() : "");
        Label nameLabel = new Label(fullName.trim().isEmpty() ? "-" : fullName.trim());
        nameLabel.setTextFill(Color.web("#94A3B8"));
        nameLabel.setFont(Font.font("System", 12));
        nameLabel.setPrefWidth(widths[2]);

        Label companyLabel = new Label(user.getCompany() != null ? user.getCompany() : "-");
        companyLabel.setTextFill(Color.web("#94A3B8"));
        companyLabel.setFont(Font.font("System", 12));
        companyLabel.setPrefWidth(widths[3]);

        // Role badge
        String roleName = user.getRole() != null && user.getRole().getName() != null
            ? user.getRole().getName().replace("ROLE_", "") : "USER";
        Label roleLabel = new Label(roleName);
        roleLabel.setFont(Font.font("System", FontWeight.BOLD, 10));
        roleLabel.setPadding(new Insets(3, 6, 3, 6));
        if (roleName.equals("ADMIN")) {
            roleLabel.setStyle("-fx-background-color: #6366F130; -fx-background-radius: 4; -fx-text-fill: #818CF8;");
        } else {
            roleLabel.setStyle("-fx-background-color: #10B98130; -fx-background-radius: 4; -fx-text-fill: #34D399;");
        }
        HBox roleBox = new HBox(roleLabel);
        roleBox.setPrefWidth(widths[4]);

        // Status badge
        boolean isEnabled = user.isEnabled();
        Label statusLabel = new Label(isEnabled ? "Active" : "Disabled");
        statusLabel.setFont(Font.font("System", FontWeight.BOLD, 10));
        statusLabel.setPadding(new Insets(3, 6, 3, 6));
        if (isEnabled) {
            statusLabel.setStyle("-fx-background-color: #10B98130; -fx-background-radius: 4; -fx-text-fill: #34D399;");
        } else {
            statusLabel.setStyle("-fx-background-color: #EF444430; -fx-background-radius: 4; -fx-text-fill: #F87171;");
        }
        HBox statusBox = new HBox(statusLabel);
        statusBox.setPrefWidth(widths[5]);

        // Action buttons
        HBox actionsBox = new HBox(6);
        actionsBox.setPrefWidth(widths[6]);
        actionsBox.setAlignment(Pos.CENTER_LEFT);

        // View/Edit button
        Button editBtn = new Button();
        editBtn.setStyle("-fx-background-color: #6366F1; -fx-background-radius: 4; -fx-padding: 4 8;");
        FontIcon editIcon = new FontIcon("fas-edit");
        editIcon.setIconSize(12);
        editIcon.setIconColor(Color.WHITE);
        editBtn.setGraphic(editIcon);
        editBtn.setTooltip(new Tooltip("Edit User"));
        editBtn.setOnAction(e -> showUserDetailsModal(user, reloadCallback));

        // Toggle status button
        Button statusBtn = new Button();
        statusBtn.setStyle("-fx-background-color: " + (isEnabled ? "#F59E0B" : "#10B981") + "; -fx-background-radius: 4; -fx-padding: 4 8;");
        FontIcon statusIcon = new FontIcon(isEnabled ? "fas-ban" : "fas-check");
        statusIcon.setIconSize(12);
        statusIcon.setIconColor(Color.WHITE);
        statusBtn.setGraphic(statusIcon);
        statusBtn.setTooltip(new Tooltip(isEnabled ? "Disable User" : "Enable User"));
        statusBtn.setOnAction(e -> {
            apiService.toggleUserStatus(sessionManager.getAuthorizationHeader(), user.getId(), !isEnabled)
                .thenAccept(result -> Platform.runLater(() -> {
                    if (result.isSuccess()) {
                        reloadCallback.run();
                    }
                }));
        });

        // Delete button
        Button deleteBtn = new Button();
        deleteBtn.setStyle("-fx-background-color: #EF4444; -fx-background-radius: 4; -fx-padding: 4 8;");
        FontIcon deleteIcon = new FontIcon("fas-trash");
        deleteIcon.setIconSize(12);
        deleteIcon.setIconColor(Color.WHITE);
        deleteBtn.setGraphic(deleteIcon);
        deleteBtn.setTooltip(new Tooltip("Delete User"));
        deleteBtn.setOnAction(e -> showDeleteConfirmation(user, reloadCallback));

        actionsBox.getChildren().addAll(editBtn, statusBtn, deleteBtn);

        // Last Login column
        String lastLoginText = "Never";
        if (user.getLastLogin() != null) {
            long diffMs = System.currentTimeMillis() - user.getLastLogin().getTime();
            long diffMins = diffMs / (60 * 1000);
            long diffHours = diffMs / (60 * 60 * 1000);
            long diffDays = diffMs / (24 * 60 * 60 * 1000);

            if (diffMins < 1) {
                lastLoginText = "Just now";
            } else if (diffMins < 60) {
                lastLoginText = diffMins + "m ago";
            } else if (diffHours < 24) {
                lastLoginText = diffHours + "h ago";
            } else if (diffDays < 7) {
                lastLoginText = diffDays + "d ago";
            } else {
                lastLoginText = new SimpleDateFormat("MMM dd").format(user.getLastLogin());
            }
        }
        Label lastLoginLabel = new Label(lastLoginText);
        lastLoginLabel.setTextFill(user.getLastLogin() != null ? Color.web("#94A3B8") : Color.web("#64748B"));
        lastLoginLabel.setFont(Font.font("System", 11));
        lastLoginLabel.setPrefWidth(widths[6]);

        row.getChildren().addAll(usernameLabel, emailLabel, nameLabel, companyLabel, roleBox, statusBox, lastLoginLabel, actionsBox);

        return row;
    }

    private void showDeleteConfirmation(User user, Runnable reloadCallback) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Delete User");
        alert.setHeaderText("Delete user: " + user.getUsername());
        alert.setContentText("Are you sure you want to delete this user? This action cannot be undone.");

        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                apiService.deleteUser(sessionManager.getAuthorizationHeader(), user.getId())
                    .thenAccept(result -> Platform.runLater(() -> {
                        if (result.isSuccess()) {
                            reloadCallback.run();
                        }
                    }));
            }
        });
    }

    private void showUserDetailsModal(User user, Runnable reloadCallback) {
        // Create modal overlay
        StackPane overlay = new StackPane();
        overlay.setStyle("-fx-background-color: rgba(0,0,0,0.7);");

        VBox modal = new VBox(20);
        modal.setMaxWidth(500);
        modal.setMaxHeight(650);
        modal.setPadding(new Insets(30));
        modal.setStyle("-fx-background-color: #1E293B; -fx-background-radius: 16;");
        modal.setAlignment(Pos.TOP_CENTER);

        // Header
        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);

        Label titleLabel = new Label("User Details");
        titleLabel.setFont(Font.font("System", FontWeight.BOLD, 24));
        titleLabel.setTextFill(Color.WHITE);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button closeBtn = new Button("✕");
        closeBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #94A3B8; -fx-font-size: 18;");
        closeBtn.setOnAction(e -> removeOverlay(overlay));

        header.getChildren().addAll(titleLabel, spacer, closeBtn);

        // User info with avatar
        HBox userInfo = new HBox(16);
        userInfo.setAlignment(Pos.CENTER_LEFT);

        StackPane avatar = new StackPane();
        Circle avatarCircle = new Circle(35);
        avatarCircle.setFill(Color.web("#6366F1"));
        String initials = "";
        if (user.getFirstName() != null && !user.getFirstName().isEmpty()) initials += user.getFirstName().charAt(0);
        if (user.getLastName() != null && !user.getLastName().isEmpty()) initials += user.getLastName().charAt(0);
        if (initials.isEmpty() && user.getUsername() != null) initials = user.getUsername().substring(0, 1).toUpperCase();
        Label avatarLabel = new Label(initials.toUpperCase());
        avatarLabel.setFont(Font.font("System", FontWeight.BOLD, 20));
        avatarLabel.setTextFill(Color.WHITE);
        avatar.getChildren().addAll(avatarCircle, avatarLabel);

        VBox userNameBox = new VBox(4);
        Label usernameLabel = new Label(user.getUsername());
        usernameLabel.setFont(Font.font("System", FontWeight.BOLD, 18));
        usernameLabel.setTextFill(Color.WHITE);
        Label emailLabel = new Label(user.getEmail());
        emailLabel.setTextFill(Color.web("#94A3B8"));

        // Last login info
        String lastLoginInfo = "Last login: Never";
        if (user.getLastLogin() != null) {
            lastLoginInfo = "Last login: " + new SimpleDateFormat("MMM dd, yyyy 'at' HH:mm").format(user.getLastLogin());
        }
        Label lastLoginLabel = new Label(lastLoginInfo);
        lastLoginLabel.setFont(Font.font("System", 11));
        lastLoginLabel.setTextFill(Color.web("#64748B"));

        userNameBox.getChildren().addAll(usernameLabel, emailLabel, lastLoginLabel);

        userInfo.getChildren().addAll(avatar, userNameBox);

        // Role selector
        HBox roleRow = new HBox(10);
        roleRow.setAlignment(Pos.CENTER_LEFT);
        Label roleLabel = new Label("Role:");
        roleLabel.setTextFill(Color.web("#94A3B8"));
        roleLabel.setPrefWidth(100);

        ComboBox<String> roleCombo = new ComboBox<>();
        roleCombo.getItems().addAll("ROLE_USER", "ROLE_ADMIN");
        roleCombo.setValue(user.getRole() != null ? user.getRole().getName() : "ROLE_USER");
        roleCombo.setStyle("-fx-background-color: #334155;");

        Button saveRoleBtn = new Button("Update Role");
        saveRoleBtn.setStyle("-fx-background-color: #6366F1; -fx-text-fill: white; -fx-background-radius: 6;");
        saveRoleBtn.setOnAction(e -> {
            apiService.changeUserRole(sessionManager.getAuthorizationHeader(), user.getId(), roleCombo.getValue())
                .thenAccept(result -> Platform.runLater(() -> {
                    if (result.isSuccess()) {
                        reloadCallback.run();
                    }
                }));
        });

        roleRow.getChildren().addAll(roleLabel, roleCombo, saveRoleBtn);

        // Editable fields
        ScrollPane fieldsScroll = new ScrollPane();
        fieldsScroll.setFitToWidth(true);
        fieldsScroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        fieldsScroll.setPrefHeight(300);

        VBox fieldsBox = new VBox(12);

        VBox firstNameBox = createModalField("First Name", user.getFirstName());
        VBox lastNameBox = createModalField("Last Name", user.getLastName());
        VBox emailBox = createModalField("Email", user.getEmail());
        VBox companyBox = createModalField("Company", user.getCompany());
        VBox jobBox = createModalField("Job Position", user.getJobPosition());
        VBox cityBox = createModalField("City", user.getCity());
        VBox countryBox = createModalField("Country", user.getCountry());
        VBox mobileBox = createModalField("Mobile", user.getMobile());

        fieldsBox.getChildren().addAll(firstNameBox, lastNameBox, emailBox, companyBox, jobBox, cityBox, countryBox, mobileBox);
        fieldsScroll.setContent(fieldsBox);

        // Action buttons
        HBox actionButtons = new HBox(12);
        actionButtons.setAlignment(Pos.CENTER_RIGHT);

        Button saveBtn = new Button("Save Changes");
        saveBtn.setStyle("-fx-background-color: #10B981; -fx-text-fill: white; -fx-background-radius: 8; -fx-padding: 10 24;");
        saveBtn.setFont(Font.font("System", FontWeight.BOLD, 13));

        Button deleteBtn = new Button("Delete User");
        deleteBtn.setStyle("-fx-background-color: #EF4444; -fx-text-fill: white; -fx-background-radius: 8; -fx-padding: 10 24;");

        saveBtn.setOnAction(e -> {
            User updatedUser = new User();
            updatedUser.setFirstName(((TextField) firstNameBox.getChildren().get(1)).getText());
            updatedUser.setLastName(((TextField) lastNameBox.getChildren().get(1)).getText());
            updatedUser.setEmail(((TextField) emailBox.getChildren().get(1)).getText());
            updatedUser.setCompany(((TextField) companyBox.getChildren().get(1)).getText());
            updatedUser.setJobPosition(((TextField) jobBox.getChildren().get(1)).getText());
            updatedUser.setCity(((TextField) cityBox.getChildren().get(1)).getText());
            updatedUser.setCountry(((TextField) countryBox.getChildren().get(1)).getText());
            updatedUser.setMobile(((TextField) mobileBox.getChildren().get(1)).getText());

            apiService.updateUserById(sessionManager.getAuthorizationHeader(), user.getId(), updatedUser)
                .thenAccept(result -> Platform.runLater(() -> {
                    if (result.isSuccess()) {
                        removeOverlay(overlay);
                        reloadCallback.run();
                    }
                }));
        });

        deleteBtn.setOnAction(e -> {
            removeOverlay(overlay);
            showDeleteConfirmation(user, reloadCallback);
        });

        actionButtons.getChildren().addAll(deleteBtn, saveBtn);

        modal.getChildren().addAll(header, userInfo, roleRow, fieldsScroll, actionButtons);

        overlay.getChildren().add(modal);
        overlay.setOnMouseClicked(e -> {
            if (e.getTarget() == overlay) {
                contentArea.getChildren().remove(overlay);
            }
        });

        // Add overlay directly to contentArea (which is a StackPane)
        contentArea.getChildren().add(overlay);
        AnimationUtils.fadeIn(overlay, 200);
    }

    private void removeOverlay(StackPane overlay) {
        contentArea.getChildren().remove(overlay);
    }

    private VBox createModalField(String label, String value) {
        VBox container = new VBox(4);
        Label lbl = new Label(label);
        lbl.setTextFill(Color.web("#94A3B8"));
        lbl.setFont(Font.font("System", 12));

        TextField field = new TextField(value != null ? value : "");
        field.setStyle("-fx-background-color: #0F172A; -fx-text-fill: white; -fx-background-radius: 6; -fx-padding: 10;");

        container.getChildren().addAll(lbl, field);
        return container;
    }
}
