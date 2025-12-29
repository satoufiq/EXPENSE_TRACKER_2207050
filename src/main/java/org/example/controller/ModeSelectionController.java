package org.example.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import org.example.MainApp;
import org.example.model.User;
import org.example.util.SessionManager;

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

    private void loadUserInfo() {
        User currentUser = SessionManager.getInstance().getCurrentUser();

        if (currentUser != null) {
            welcomeLabel.setText("Welcome, " + currentUser.getName() + "!");

            if ("parent".equalsIgnoreCase(currentUser.getRole())) {
                parentModeCard.setVisible(true);
                parentModeCard.setManaged(true);
            }
        } else {

            MainApp.loadLogin();
        }
    }

    @FXML
    private void handlePersonalMode() {
        SessionManager.getInstance().setCurrentMode("personal");
        SessionManager.getInstance().setCurrentGroupId(null);
        loadPersonalDashboard();
    }

    @FXML
    private void handleGroupMode() {
        SessionManager.getInstance().setCurrentMode("group");
        loadGroupSelection();
    }

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

    @FXML
    private void handleLogout() {
        SessionManager.clearSession();
        MainApp.loadHome();
    }

    private void loadPersonalDashboard() {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/fxml/personal_dashboard.fxml"));
            javafx.scene.Parent root = loader.load();
            javafx.scene.Scene scene = new javafx.scene.Scene(root);
            scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
            MainApp.getPrimaryStage().setScene(scene);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

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

    private void loadParentDashboard() {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                getClass().getResource("/fxml/parent_dashboard.fxml"));
            javafx.scene.Parent root = loader.load();
            javafx.scene.Scene scene = new javafx.scene.Scene(root);
            scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
            MainApp.getPrimaryStage().setScene(scene);
        } catch (Exception e) {
            System.err.println("Failed to load parent dashboard: " + e.getMessage());
            e.printStackTrace();
            showError("Failed to load Parent Dashboard: " + e.getMessage());
        }
    }

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
            System.err.println("Failed to load alerts: " + e.getMessage());
            e.printStackTrace();
            showError("Failed to load Alerts: " + e.getMessage());
        }
    }

    private void showError(String message) {
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}