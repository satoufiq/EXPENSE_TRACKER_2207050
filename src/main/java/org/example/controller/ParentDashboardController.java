package org.example.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.example.MainApp;
import org.example.model.User;
import org.example.service.ExpenseService;
import org.example.service.InviteService;
import org.example.service.ParentService;
import org.example.service.UserService;
import org.example.util.SessionManager;

@SuppressWarnings({"unused", "FieldCanBeLocal"})
public class ParentDashboardController {

    @FXML private Label parentNameLabel;
    @FXML private Label childCountLabel;
    @FXML private Label totalChildrenLabel;
    @FXML private VBox childCardsPane;
    @FXML private VBox emptyStatePane;
    @FXML private Button viewChildAnalyticsButton;
    @FXML private Button viewChildExpensesButton;
    @FXML private Button backButton;
    @FXML private Button viewAlertsButton;
    @FXML private Label alertBadge;
    @FXML private HBox alertButtonContainer;
    @FXML private TextField childEmailField;
    @FXML private Button sendChildInviteButton;

    private final ObservableList<User> children = FXCollections.observableArrayList();
    private User selectedChild = null;

    @FXML
    public void initialize() {
        User parent = SessionManager.getInstance().getCurrentUser();
        if (parent == null || !"parent".equalsIgnoreCase(parent.getRole())) {
            loadModeSelection();
            return;
        }
        parentNameLabel.setText("👨‍👧 " + parent.getName());

        refreshChildren();
        updateAlertCount();

        viewChildAnalyticsButton.setDisable(true);
        viewChildExpensesButton.setDisable(true);
    }

    private void updateAlertCount() {
        User parent = SessionManager.getInstance().getCurrentUser();
        if (parent == null) return;

        int count = org.example.service.ParentChildAlertService.getUnreadAlertCount(parent.getUserId());

        if (alertBadge != null && viewAlertsButton != null) {
            if (count > 0) {
                alertBadge.setText(String.valueOf(count));
                alertBadge.setVisible(true);
                alertBadge.setManaged(true);
                viewAlertsButton.getStyleClass().add("alert-button-active");
            } else {
                alertBadge.setVisible(false);
                alertBadge.setManaged(false);
                viewAlertsButton.getStyleClass().remove("alert-button-active");
            }
        }
    }

    @FXML
    private void handleViewAlerts() {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                getClass().getResource("/fxml/parent_alerts.fxml"));
            javafx.scene.Parent root = loader.load();
            javafx.scene.Scene scene = new javafx.scene.Scene(root);
            scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
            MainApp.getPrimaryStage().setScene(scene);
        } catch (Exception ex) {
            ex.printStackTrace();
            showInfo("Failed to load alerts: " + ex.getMessage());
        }
    }

    private void refreshChildren() {
        User parent = SessionManager.getInstance().getCurrentUser();
        var childUsers = ParentService.getChildrenForParent(parent.getUserId());
        children.setAll(childUsers);

        refreshChildCards();
    }

    private void refreshChildCards() {
        if (childCardsPane == null) return;

        childCardsPane.getChildren().clear();

        if (children.isEmpty()) {
            if (emptyStatePane != null) {
                emptyStatePane.setVisible(true);
                emptyStatePane.setManaged(true);
            }
            if (childCountLabel != null) childCountLabel.setText("0 children");
            if (totalChildrenLabel != null) totalChildrenLabel.setText("0");
            return;
        }

        if (emptyStatePane != null) {
            emptyStatePane.setVisible(false);
            emptyStatePane.setManaged(false);
        }

        for (User child : children) {
            childCardsPane.getChildren().add(createChildCard(child));
        }

        if (childCountLabel != null) childCountLabel.setText(children.size() + " children");
        if (totalChildrenLabel != null) totalChildrenLabel.setText(String.valueOf(children.size()));
    }

    private HBox createChildCard(User child) {
        HBox card = new HBox(15);
        card.getStyleClass().add("child-card");
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(15, 20, 15, 20));

        // Avatar
        VBox avatar = new VBox();
        avatar.getStyleClass().add("member-avatar");
        avatar.setAlignment(Pos.CENTER);
        avatar.setMinSize(50, 50);
        avatar.setMaxSize(50, 50);
        String initials = getInitials(child.getName());
        Label initialsLabel = new Label(initials);
        initialsLabel.getStyleClass().add("member-avatar-text");
        avatar.getChildren().add(initialsLabel);

        // Info section
        VBox info = new VBox(4);
        info.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(info, Priority.ALWAYS);

        Label nameLabel = new Label(child.getName());
        nameLabel.getStyleClass().add("member-name");

        Label emailLabel = new Label("📧 " + child.getEmail());
        emailLabel.getStyleClass().add("member-email");

        // Get expense stats
        double totalExpenses = ExpenseService.getTotalExpenses(child.getUserId());
        Label expenseLabel = new Label(String.format("💰 Total: ৳%.2f", totalExpenses));
        expenseLabel.setStyle("-fx-text-fill: #7b8ae4; -fx-font-size: 12px; -fx-font-weight: bold;");

        info.getChildren().addAll(nameLabel, emailLabel, expenseLabel);

        // Status badge
        Label statusBadge = new Label("👶 Child");
        statusBadge.getStyleClass().add("member-role-badge");
        statusBadge.setStyle("-fx-background-color: rgba(92, 107, 192, 0.4); -fx-text-fill: #a0aaff;");

        // Select button
        Button selectBtn = new Button("Select");
        selectBtn.getStyleClass().addAll("card-action-button");
        selectBtn.setOnAction(e -> selectChild(child, card));

        card.getChildren().addAll(avatar, info, statusBadge, selectBtn);

        // Click to select
        card.setOnMouseClicked(e -> selectChild(child, card));

        return card;
    }

    private void selectChild(User child, HBox card) {
        // Deselect all cards
        for (var node : childCardsPane.getChildren()) {
            if (node instanceof HBox) {
                node.setStyle("");
                node.getStyleClass().remove("child-card-selected");
            }
        }

        // Select this card
        card.getStyleClass().add("child-card-selected");
        card.setStyle("-fx-background-color: rgba(92, 107, 192, 0.4); -fx-border-color: rgba(123, 136, 255, 0.8);");

        selectedChild = child;
        SessionManager.getInstance().setSelectedChildId(child.getUserId());

        viewChildAnalyticsButton.setDisable(false);
        viewChildExpensesButton.setDisable(false);
    }

    private String getInitials(String name) {
        if (name == null || name.isEmpty()) return "?";
        String[] parts = name.trim().split("\\s+");
        if (parts.length >= 2) {
            return (parts[0].charAt(0) + "" + parts[1].charAt(0)).toUpperCase();
        }
        return name.substring(0, Math.min(2, name.length())).toUpperCase();
    }

    @FXML
    private void handleViewChildAnalytics() {
        if (selectedChild == null) return;

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

    @FXML
    private void handleViewChildExpenses() {
        if (selectedChild == null) return;

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



    @FXML
    private void handleBack() {
        loadModeSelection();
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

    @FXML
    private void handleSendInvite() {
        String email = childEmailField.getText();
        if (email == null || email.trim().isEmpty()) {
            showInfo("Please enter the child's email.");
            return;
        }

        sendChildInviteButton.setDisable(true);
        sendChildInviteButton.setText("Sending...");

        var parent = SessionManager.getInstance().getCurrentUser();

        var childUser = UserService.getUserByEmail(email.trim());
        if (childUser == null) {
            sendChildInviteButton.setDisable(false);
            sendChildInviteButton.setText("📧 Send Invite");
            showInfo("No user found with this email.");
            return;
        }

        String inviteId = InviteService.sendParentInvite(parent.getUserId(), childUser.getUserId());
        if (inviteId == null) {
            sendChildInviteButton.setDisable(false);
            sendChildInviteButton.setText("📧 Send Invite");
            showInfo("Failed to send invite. Try again.");
            return;
        }

        String message = "Invite sent! The child must accept it from their Alerts.";
        sendChildInviteButton.setDisable(false);
        sendChildInviteButton.setText("📧 Send Invite");
        showInfo(message);
        childEmailField.clear();
    }

    private void showInfo(String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Information");
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}

