package org.example.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import org.example.MainApp;
import org.example.model.User;
import org.example.service.InviteService;
import org.example.service.ParentService;
import org.example.service.UserService;
import org.example.util.SessionManager;

import java.util.List;

/**
 * Parent Dashboard Controller
 * Shows the parent's children and allows navigation to child views
 */
public class ParentDashboardController {

    @FXML
    private Label parentNameLabel;

    @FXML
    private ListView<User> childrenListView;

    @FXML
    private Button viewChildAnalyticsButton;

    @FXML
    private Button viewChildExpensesButton;

    @FXML
    private Button backButton;

    @FXML
    private TextField childEmailField;

    @FXML
    private Button sendChildInviteButton;

    private final ObservableList<User> children = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        // Load parent info
        User parent = SessionManager.getInstance().getCurrentUser();
        if (parent == null || !"parent".equalsIgnoreCase(parent.getRole())) {
            // Not a parent; redirect to mode selection
            loadModeSelection();
            return;
        }
        parentNameLabel.setText("Parent: " + parent.getName());

        // Load children
        refreshChildren();

        // Disable action buttons if no selection
        childrenListView.getSelectionModel().selectedItemProperty().addListener((obs, old, sel) -> {
            boolean hasSelection = sel != null;
            viewChildAnalyticsButton.setDisable(!hasSelection);
            viewChildExpensesButton.setDisable(!hasSelection);
        });
        viewChildAnalyticsButton.setDisable(true);
        viewChildExpensesButton.setDisable(true);

        // Wire buttons
        viewChildAnalyticsButton.setOnAction(e -> openChildAnalytics());
        viewChildExpensesButton.setOnAction(e -> openChildExpenseList());
        backButton.setOnAction(e -> loadModeSelection());
        sendChildInviteButton.setOnAction(e -> sendChildInvite());
    }

    private void refreshChildren() {
        User parent = SessionManager.getInstance().getCurrentUser();

        org.example.util.ThreadPoolManager.getInstance().executeDatabaseWithCallback(
            () -> ParentService.getChildrenForParent(parent.getUserId()),
            childUsers -> {
                children.setAll(childUsers);
                childrenListView.setItems(children);
                childrenListView.setCellFactory(lv -> new javafx.scene.control.ListCell<>() {
                    @Override
                    protected void updateItem(User item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty || item == null) {
                            setText(null);
                        } else {
                            setText(item.getName() + " (" + item.getEmail() + ")");
                        }
                    }
                });
            },
            error -> {
                System.err.println("Error loading children: " + error.getMessage());
                error.printStackTrace();
            }
        );
    }

    private void openChildAnalytics() {
        User selected = childrenListView.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        // Store selected child in session
        SessionManager.getInstance().setSelectedChildId(selected.getUserId());
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                getClass().getResource("/fxml/child_analytics.fxml"));
            javafx.scene.Parent root = loader.load();
            javafx.scene.Scene scene = new javafx.scene.Scene(root);
            scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
            MainApp.getPrimaryStage().setScene(scene);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void openChildExpenseList() {
        User selected = childrenListView.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        SessionManager.getInstance().setSelectedChildId(selected.getUserId());
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                getClass().getResource("/fxml/child_expense_list.fxml"));
            javafx.scene.Parent root = loader.load();
            javafx.scene.Scene scene = new javafx.scene.Scene(root);
            scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
            MainApp.getPrimaryStage().setScene(scene);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void loadModeSelection() {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                getClass().getResource("/fxml/mode_selection.fxml"));
            javafx.scene.Parent root = loader.load();
            javafx.scene.Scene scene = new javafx.scene.Scene(root);
            scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
            MainApp.getPrimaryStage().setScene(scene);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void sendChildInvite() {
        String email = childEmailField.getText();
        if (email == null || email.trim().isEmpty()) {
            showInfo("Please enter the child's email.");
            return;
        }

        sendChildInviteButton.setDisable(true);
        sendChildInviteButton.setText("Sending...");

        var parent = SessionManager.getInstance().getCurrentUser();

        org.example.util.ThreadPoolManager.getInstance().executeDatabaseWithCallback(
            () -> {
                var childUser = UserService.getUserByEmail(email.trim());
                if (childUser == null) {
                    throw new IllegalArgumentException("No user found with this email.");
                }

                String inviteId = InviteService.sendParentInvite(parent.getUserId(), childUser.getUserId());
                if (inviteId == null) {
                    throw new IllegalStateException("Failed to send invite. Try again.");
                }

                return "Invite sent. The child must accept it from Alerts.";
            },
            message -> {
                sendChildInviteButton.setDisable(false);
                sendChildInviteButton.setText("Send Invite");
                showInfo(message);
                childEmailField.clear();
            },
            error -> {
                sendChildInviteButton.setDisable(false);
                sendChildInviteButton.setText("Send Invite");
                showInfo(error.getMessage());
            }
        );
    }

    private void showInfo(String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Information");
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}
