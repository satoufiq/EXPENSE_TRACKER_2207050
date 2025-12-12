package org.example.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import org.example.MainApp;
import org.example.model.User;
import org.example.util.SessionManager;

/**
 * Mode Selection Controller
 * Allows users to choose between Personal, Group, or Parent mode
 */
public class ModeSelectionController {

    @FXML
    private Label welcomeLabel;

    @FXML
    private Button personalModeButton;

    @FXML
    private Button groupModeButton;

    @FXML
    private Button parentModeButton;

    @FXML
    private VBox parentModeCard;

    @FXML
    private Button logoutButton;

    @FXML
    private Button alertsButton;

    @FXML
    public void initialize() {
        loadUserInfo();
    }

    /**
     * Load user information and configure UI based on role
     */
    private void loadUserInfo() {
        User currentUser = SessionManager.getInstance().getCurrentUser();

        if (currentUser != null) {
            welcomeLabel.setText("Welcome, " + currentUser.getName() + "!");

            // Show parent mode card only for parent users
            if ("parent".equalsIgnoreCase(currentUser.getRole())) {
                parentModeCard.setVisible(true);
                parentModeCard.setManaged(true);
            }
        } else {
            // If no user session, redirect to login
            MainApp.loadLogin();
        }
    }

    /**
     * Handle Personal Mode selection
     */
    @FXML
    private void handlePersonalMode() {
        SessionManager.getInstance().setCurrentMode("personal");
        SessionManager.getInstance().setCurrentGroupId(null);
        loadPersonalDashboard();
    }

    /**
     * Handle Group Mode selection
     */
    @FXML
    private void handleGroupMode() {
        SessionManager.getInstance().setCurrentMode("group");
        loadGroupSelection();
    }

    /**
     * Handle Parent Mode selection
     */
    @FXML
    private void handleParentMode() {
        User currentUser = SessionManager.getInstance().getCurrentUser();

        if (currentUser != null && "parent".equalsIgnoreCase(currentUser.getRole())) {
            SessionManager.getInstance().setCurrentMode("parent");
            loadParentDashboard();
        } else {
            System.out.println("Parent mode is only available for parent accounts");
        }
    }

    /**
     * Handle logout
     */
    @FXML
    private void handleLogout() {
        SessionManager.clearSession();
        MainApp.loadHome();
    }

    /**
     * Load Personal Dashboard
     */
    private void loadPersonalDashboard() {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                getClass().getResource("/fxml/personal_dashboard.fxml"));
            javafx.scene.Parent root = loader.load();
            javafx.scene.Scene scene = new javafx.scene.Scene(root);
            scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
            MainApp.getPrimaryStage().setScene(scene);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Load Group Selection screen
     */
    private void loadGroupSelection() {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                getClass().getResource("/fxml/group_selection.fxml"));
            javafx.scene.Parent root = loader.load();
            javafx.scene.Scene scene = new javafx.scene.Scene(root);
            scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
            MainApp.getPrimaryStage().setScene(scene);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Load Parent Dashboard
     */
    private void loadParentDashboard() {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                getClass().getResource("/fxml/parent_dashboard.fxml"));
            javafx.scene.Parent root = loader.load();
            javafx.scene.Scene scene = new javafx.scene.Scene(root);
            scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
            MainApp.getPrimaryStage().setScene(scene);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Handle Alerts navigation
     */
    @FXML
    private void handleAlerts() {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                getClass().getResource("/fxml/alerts.fxml"));
            javafx.scene.Parent root = loader.load();
            javafx.scene.Scene scene = new javafx.scene.Scene(root);
            scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
            MainApp.getPrimaryStage().setScene(scene);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
