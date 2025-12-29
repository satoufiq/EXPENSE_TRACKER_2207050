package org.example.controller;

import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import org.example.MainApp;
import org.example.model.ParentChildAlert;
import org.example.service.InviteService;
import org.example.service.ParentChildAlertService;
import org.example.service.UserService;
import org.example.util.SessionManager;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AlertsController {

    @FXML private Button backButton;
    @FXML private VBox parentInvitesPane;
    @FXML private VBox groupInvitesPane;
    @FXML private VBox financialAlertsPane;
    @FXML private VBox emptyParentPane;
    @FXML private VBox emptyGroupPane;
    @FXML private VBox emptyAlertsPane;
    @FXML private Label parentInviteCountLabel;
    @FXML private Label groupInviteCountLabel;
    @FXML private Label alertCountLabel;

    private final Map<String, String> parentInviteMap = new HashMap<>();
    private final Map<String, String> groupInviteMap = new HashMap<>();
    private final Map<String, String> financialAlertsMap = new HashMap<>();

    @FXML
    public void initialize() {
        loadInvitesAndAlerts();
    }

    @FXML
    private void handleBack() {
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

    private void loadInvitesAndAlerts() {
        var currentUser = SessionManager.getInstance().getCurrentUser();
        if (currentUser == null) {
            MainApp.loadLogin();
            return;
        }

        final String oderId = currentUser.getUserId();

        loadParentInvites(oderId);
        loadGroupInvites(oderId);
        loadFinancialAlerts(oderId);
    }

    private void loadParentInvites(String oderId) {
        if (parentInvitesPane == null) return;

        parentInvitesPane.getChildren().clear();
        parentInviteMap.clear();

        List<String[]> pInv = InviteService.getPendingParentInvitesForChild(oderId);

        if (pInv.isEmpty()) {
            if (emptyParentPane != null) {
                emptyParentPane.setVisible(true);
                emptyParentPane.setManaged(true);
            }
            if (parentInviteCountLabel != null) {
                parentInviteCountLabel.setVisible(false);
                parentInviteCountLabel.setManaged(false);
            }
            return;
        }

        if (emptyParentPane != null) {
            emptyParentPane.setVisible(false);
            emptyParentPane.setManaged(false);
        }

        if (parentInviteCountLabel != null) {
            parentInviteCountLabel.setText(String.valueOf(pInv.size()));
            parentInviteCountLabel.setVisible(true);
            parentInviteCountLabel.setManaged(true);
        }

        for (String[] row : pInv) {
            String inviteId = row[0];
            String parentId = row[1];
            var parentUser = UserService.getUserById(parentId);
            String parentName = parentUser != null ? parentUser.getName() : parentId;
            String parentEmail = parentUser != null ? parentUser.getEmail() : "";

            parentInviteMap.put(inviteId, inviteId);
            parentInvitesPane.getChildren().add(createParentInviteCard(inviteId, parentName, parentEmail));
        }
    }

    private HBox createParentInviteCard(String inviteId, String parentName, String parentEmail) {
        HBox card = new HBox(15);
        card.getStyleClass().add("child-card");
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(15, 15, 15, 15));

        // Icon
        Label icon = new Label("👨‍👧");
        icon.setStyle("-fx-font-size: 28px;");

        // Info
        VBox info = new VBox(4);
        info.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(info, Priority.ALWAYS);

        Label nameLabel = new Label(parentName);
        nameLabel.getStyleClass().add("member-name");

        Label descLabel = new Label("Wants to add you as their child");
        descLabel.setStyle("-fx-text-fill: rgba(255,255,255,0.7); -fx-font-size: 12px;");

        if (!parentEmail.isEmpty()) {
            Label emailLabel = new Label("📧 " + parentEmail);
            emailLabel.getStyleClass().add("member-email");
            info.getChildren().addAll(nameLabel, emailLabel, descLabel);
        } else {
            info.getChildren().addAll(nameLabel, descLabel);
        }

        // Buttons
        HBox buttons = new HBox(8);
        buttons.setAlignment(Pos.CENTER_RIGHT);

        Button acceptBtn = new Button("✓ Accept");
        acceptBtn.getStyleClass().addAll("card-action-button");
        acceptBtn.setStyle("-fx-background-color: rgba(76, 175, 80, 0.4); -fx-border-color: rgba(76, 175, 80, 0.6);");
        acceptBtn.setOnAction(e -> handleAcceptParentInvite(inviteId, card));

        Button declineBtn = new Button("✗ Decline");
        declineBtn.getStyleClass().addAll("card-action-button", "card-delete-button");
        declineBtn.setOnAction(e -> handleDeclineParentInvite(inviteId, card));

        buttons.getChildren().addAll(acceptBtn, declineBtn);

        card.getChildren().addAll(icon, info, buttons);
        return card;
    }

    private void handleAcceptParentInvite(String inviteId, HBox card) {
        boolean success = InviteService.acceptParentInvite(inviteId);
        if (success) {
            loadInvitesAndAlerts();
            org.example.util.AlertUtil.showInfo("Success", "Parent invitation accepted!");
        } else {
            showError("Failed to accept invite");
        }
    }

    private void handleDeclineParentInvite(String inviteId, HBox card) {
        boolean success = InviteService.declineParentInvite(inviteId);
        if (success) {
            loadInvitesAndAlerts();
        } else {
            showError("Failed to decline invite");
        }
    }

    private void loadGroupInvites(String oderId) {
        if (groupInvitesPane == null) return;

        groupInvitesPane.getChildren().clear();
        groupInviteMap.clear();

        List<String[]> gInv = InviteService.getPendingGroupInvitesForUser(oderId);

        if (gInv.isEmpty()) {
            if (emptyGroupPane != null) {
                emptyGroupPane.setVisible(true);
                emptyGroupPane.setManaged(true);
            }
            if (groupInviteCountLabel != null) {
                groupInviteCountLabel.setVisible(false);
                groupInviteCountLabel.setManaged(false);
            }
            return;
        }

        if (emptyGroupPane != null) {
            emptyGroupPane.setVisible(false);
            emptyGroupPane.setManaged(false);
        }

        if (groupInviteCountLabel != null) {
            groupInviteCountLabel.setText(String.valueOf(gInv.size()));
            groupInviteCountLabel.setVisible(true);
            groupInviteCountLabel.setManaged(true);
        }

        for (String[] row : gInv) {
            String inviteId = row[0];
            String groupId = row[1];
            var group = org.example.service.GroupService.getGroupById(groupId);
            String groupName = group != null ? group.getGroupName() : groupId;

            groupInviteMap.put(inviteId, inviteId);
            groupInvitesPane.getChildren().add(createGroupInviteCard(inviteId, groupName));
        }
    }

    private HBox createGroupInviteCard(String inviteId, String groupName) {
        HBox card = new HBox(15);
        card.getStyleClass().add("child-card");
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(15, 15, 15, 15));

        // Icon
        Label icon = new Label("👥");
        icon.setStyle("-fx-font-size: 28px;");

        // Info
        VBox info = new VBox(4);
        info.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(info, Priority.ALWAYS);

        Label nameLabel = new Label(groupName);
        nameLabel.getStyleClass().add("member-name");

        Label descLabel = new Label("You've been invited to join this group");
        descLabel.setStyle("-fx-text-fill: rgba(255,255,255,0.7); -fx-font-size: 12px;");

        info.getChildren().addAll(nameLabel, descLabel);

        // Buttons
        HBox buttons = new HBox(8);
        buttons.setAlignment(Pos.CENTER_RIGHT);

        Button acceptBtn = new Button("✓ Join");
        acceptBtn.getStyleClass().addAll("card-action-button");
        acceptBtn.setStyle("-fx-background-color: rgba(76, 175, 80, 0.4); -fx-border-color: rgba(76, 175, 80, 0.6);");
        acceptBtn.setOnAction(e -> handleAcceptGroupInvite(inviteId, card));

        Button declineBtn = new Button("✗ Decline");
        declineBtn.getStyleClass().addAll("card-action-button", "card-delete-button");
        declineBtn.setOnAction(e -> handleDeclineGroupInvite(inviteId, card));

        buttons.getChildren().addAll(acceptBtn, declineBtn);

        card.getChildren().addAll(icon, info, buttons);
        return card;
    }

    private void handleAcceptGroupInvite(String inviteId, HBox card) {
        boolean success = InviteService.acceptGroupInvite(inviteId);
        if (success) {
            loadInvitesAndAlerts();
            org.example.util.AlertUtil.showInfo("Success", "You've joined the group!");
        } else {
            showError("Failed to accept group invite");
        }
    }

    private void handleDeclineGroupInvite(String inviteId, HBox card) {
        boolean success = InviteService.declineGroupInvite(inviteId);
        if (success) {
            loadInvitesAndAlerts();
        } else {
            showError("Failed to decline group invite");
        }
    }

    private void loadFinancialAlerts(String oderId) {
        if (financialAlertsPane == null) return;

        financialAlertsPane.getChildren().clear();
        financialAlertsMap.clear();

        ObservableList<ParentChildAlert> alerts = ParentChildAlertService.getAlertsForUser(oderId);

        if (alerts.isEmpty()) {
            if (emptyAlertsPane != null) {
                emptyAlertsPane.setVisible(true);
                emptyAlertsPane.setManaged(true);
            }
            if (alertCountLabel != null) {
                alertCountLabel.setVisible(false);
                alertCountLabel.setManaged(false);
            }
            return;
        }

        if (emptyAlertsPane != null) {
            emptyAlertsPane.setVisible(false);
            emptyAlertsPane.setManaged(false);
        }

        int unreadCount = (int) alerts.stream().filter(a -> "unread".equals(a.getReadStatus())).count();
        if (alertCountLabel != null) {
            if (unreadCount > 0) {
                alertCountLabel.setText(String.valueOf(unreadCount));
                alertCountLabel.setVisible(true);
                alertCountLabel.setManaged(true);
            } else {
                alertCountLabel.setVisible(false);
                alertCountLabel.setManaged(false);
            }
        }

        for (ParentChildAlert alert : alerts) {
            financialAlertsMap.put(alert.getAlertId(), alert.getAlertId());
            financialAlertsPane.getChildren().add(createAlertCard(alert));
        }
    }

    private VBox createAlertCard(ParentChildAlert alert) {
        VBox card = new VBox(10);
        card.getStyleClass().add("child-card");
        card.setPadding(new Insets(15, 15, 15, 15));

        boolean isUnread = "unread".equals(alert.getReadStatus());
        boolean isSuggestion = "suggestion".equals(alert.getType());

        // Header row
        HBox headerRow = new HBox(10);
        headerRow.setAlignment(Pos.CENTER_LEFT);

        Label typeIcon = new Label(isSuggestion ? "📝" : "⚠️");
        typeIcon.setStyle("-fx-font-size: 22px;");

        Label typeLabel = new Label(isSuggestion ? "Suggestion" : "Alert");
        typeLabel.getStyleClass().add("expense-category-badge");
        if (isSuggestion) {
            typeLabel.setStyle("-fx-background-color: rgba(33, 150, 243, 0.4);");
        } else {
            typeLabel.setStyle("-fx-background-color: rgba(255, 152, 0, 0.4);");
        }

        if (isUnread) {
            Label newBadge = new Label("NEW");
            newBadge.getStyleClass().add("alert-badge");
            newBadge.setStyle("-fx-font-size: 9px; -fx-padding: 2 6;");
            headerRow.getChildren().addAll(typeIcon, typeLabel, newBadge);
        } else {
            headerRow.getChildren().addAll(typeIcon, typeLabel);
        }

        // From row
        String fromName = alert.getFromUserName() != null ? alert.getFromUserName() : alert.getFromUserId();
        Label fromLabel = new Label("From: " + fromName);
        fromLabel.setStyle("-fx-text-fill: #7b8ae4; -fx-font-size: 12px; -fx-font-weight: bold;");

        // Message
        Label messageLabel = new Label(alert.getMessage());
        messageLabel.setWrapText(true);
        messageLabel.setStyle("-fx-text-fill: rgba(255,255,255,0.9); -fx-font-size: 13px;");

        // Time
        String time = alert.getCreatedAt();
        if (time != null && time.length() > 10) {
            time = time.substring(0, 10); // Just the date part
        }
        Label timeLabel = new Label("📅 " + (time != null ? time : ""));
        timeLabel.setStyle("-fx-text-fill: rgba(255,255,255,0.5); -fx-font-size: 11px;");

        // Action button
        HBox actionsRow = new HBox(10);
        actionsRow.setAlignment(Pos.CENTER_RIGHT);

        if (isUnread) {
            Button markReadBtn = new Button("✓ Mark Read");
            markReadBtn.getStyleClass().add("card-action-button");
            markReadBtn.setOnAction(e -> {
                ParentChildAlertService.markAsRead(alert.getAlertId());
                loadInvitesAndAlerts();
            });
            actionsRow.getChildren().add(markReadBtn);
        }

        Button dismissBtn = new Button("🗑️ Dismiss");
        dismissBtn.getStyleClass().addAll("card-action-button", "card-delete-button");
        dismissBtn.setOnAction(e -> {
            ParentChildAlertService.markAsRead(alert.getAlertId());
            ParentChildAlertService.deleteAlert(alert.getAlertId());
            loadInvitesAndAlerts();
        });
        actionsRow.getChildren().add(dismissBtn);

        card.getChildren().addAll(headerRow, fromLabel, messageLabel, timeLabel, actionsRow);

        // Highlight unread
        if (isUnread) {
            card.setStyle("-fx-background-color: rgba(123, 136, 255, 0.15); -fx-border-color: rgba(123, 136, 255, 0.4);");
        }

        return card;
    }

    private void showError(String message) {
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}

