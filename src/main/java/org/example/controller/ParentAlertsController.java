package org.example.controller;

import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.example.MainApp;
import org.example.model.ParentChildAlert;
import org.example.service.ParentChildAlertService;
import org.example.util.SessionManager;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class ParentAlertsController {

    @FXML private VBox alertsCardsPane;
    @FXML private VBox emptyAlertsPane;
    @FXML private Button backButton;
    @FXML private Label unreadCountLabel;
    @FXML private Label alertCountBadge;

    private String parentUserId;

    @FXML
    public void initialize() {
        parentUserId = SessionManager.getInstance().getCurrentUser().getUserId();
        loadAlerts();
    }

    @FXML
    private void handleBack() {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                getClass().getResource("/fxml/parent_dashboard.fxml"));
            javafx.scene.Parent root = loader.load();
            javafx.scene.Scene scene = new javafx.scene.Scene(root);
            scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
            MainApp.getPrimaryStage().setScene(scene);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void loadAlerts() {
        if (alertsCardsPane == null) return;

        alertsCardsPane.getChildren().clear();

        var alerts = ParentChildAlertService.getAlertsForUser(parentUserId);

        if (alerts.isEmpty()) {
            if (emptyAlertsPane != null) {
                emptyAlertsPane.setVisible(true);
                emptyAlertsPane.setManaged(true);
            }
            if (alertCountBadge != null) {
                alertCountBadge.setVisible(false);
                alertCountBadge.setManaged(false);
            }
            if (unreadCountLabel != null) {
                unreadCountLabel.setText("No messages");
            }
            return;
        }

        if (emptyAlertsPane != null) {
            emptyAlertsPane.setVisible(false);
            emptyAlertsPane.setManaged(false);
        }

        long unreadCount = alerts.stream().filter(a -> "unread".equals(a.getReadStatus())).count();

        if (unreadCountLabel != null) {
            unreadCountLabel.setText(unreadCount > 0 ? unreadCount + " unread message(s)" : "All messages read");
        }

        if (alertCountBadge != null) {
            if (unreadCount > 0) {
                alertCountBadge.setText(String.valueOf(unreadCount));
                alertCountBadge.setVisible(true);
                alertCountBadge.setManaged(true);
            } else {
                alertCountBadge.setVisible(false);
                alertCountBadge.setManaged(false);
            }
        }

        for (ParentChildAlert alert : alerts) {
            alertsCardsPane.getChildren().add(createAlertCard(alert));
        }
    }

    private VBox createAlertCard(ParentChildAlert alert) {
        VBox card = new VBox(12);
        card.getStyleClass().add("child-card");
        card.setPadding(new Insets(18, 18, 18, 18));

        boolean isUnread = "unread".equals(alert.getReadStatus());
        boolean isAlert = "alert".equals(alert.getType());

        // Header row with type icon and badge
        HBox headerRow = new HBox(12);
        headerRow.setAlignment(Pos.CENTER_LEFT);

        Label typeIcon = new Label(isAlert ? "⚠️" : "📝");
        typeIcon.setStyle("-fx-font-size: 26px;");

        VBox typeInfo = new VBox(2);
        Label typeLabel = new Label(isAlert ? "Alert" : "Suggestion");
        typeLabel.getStyleClass().add("expense-category-badge");
        if (isAlert) {
            typeLabel.setStyle("-fx-background-color: rgba(255, 152, 0, 0.4); -fx-text-fill: #ffca28;");
        } else {
            typeLabel.setStyle("-fx-background-color: rgba(33, 150, 243, 0.4); -fx-text-fill: #64b5f6;");
        }
        typeInfo.getChildren().add(typeLabel);

        HBox.setHgrow(typeInfo, Priority.ALWAYS);

        if (isUnread) {
            Label newBadge = new Label("● NEW");
            newBadge.setStyle("-fx-text-fill: #ff5252; -fx-font-weight: bold; -fx-font-size: 11px;");
            headerRow.getChildren().addAll(typeIcon, typeInfo, newBadge);
        } else {
            headerRow.getChildren().addAll(typeIcon, typeInfo);
        }

        // Child info
        String childName = alert.getFromUserName() != null ? alert.getFromUserName() : "Unknown";
        Label fromLabel = new Label("👶 From: " + childName);
        fromLabel.setStyle("-fx-text-fill: #7b8ae4; -fx-font-size: 14px; -fx-font-weight: bold;");

        // Message
        Label messageLabel = new Label(alert.getMessage());
        messageLabel.setWrapText(true);
        messageLabel.setStyle("-fx-text-fill: rgba(255,255,255,0.9); -fx-font-size: 14px; -fx-padding: 10 0;");

        // Time
        String timeStr = formatDate(alert.getCreatedAt());
        Label timeLabel = new Label("📅 " + timeStr);
        timeLabel.setStyle("-fx-text-fill: rgba(255,255,255,0.5); -fx-font-size: 11px;");

        // Action buttons
        HBox actionsRow = new HBox(12);
        actionsRow.setAlignment(Pos.CENTER_RIGHT);
        actionsRow.setPadding(new Insets(8, 0, 0, 0));

        if (isUnread) {
            Button markReadBtn = new Button("✓ Mark as Read");
            markReadBtn.getStyleClass().add("card-action-button");
            markReadBtn.setStyle("-fx-background-color: rgba(76, 175, 80, 0.3); -fx-border-color: rgba(76, 175, 80, 0.5);");
            markReadBtn.setOnAction(e -> {
                ParentChildAlertService.markAsRead(alert.getAlertId());
                loadAlerts();
            });
            actionsRow.getChildren().add(markReadBtn);
        }

        Button deleteBtn = new Button("🗑️ Dismiss");
        deleteBtn.getStyleClass().addAll("card-action-button", "card-delete-button");
        deleteBtn.setOnAction(e -> {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Dismiss Message");
            confirm.setHeaderText("Dismiss this message?");
            confirm.setContentText("This action cannot be undone.");
            confirm.showAndWait().ifPresent(response -> {
                if (response == ButtonType.OK) {
                    ParentChildAlertService.deleteAlert(alert.getAlertId());
                    loadAlerts();
                }
            });
        });
        actionsRow.getChildren().add(deleteBtn);

        card.getChildren().addAll(headerRow, fromLabel, messageLabel, timeLabel, actionsRow);

        // Highlight unread cards
        if (isUnread) {
            card.setStyle("-fx-background-color: rgba(123, 136, 255, 0.18); -fx-border-color: rgba(123, 136, 255, 0.5); -fx-border-width: 2;");
        }

        return card;
    }

    private String formatDate(String dateStr) {
        try {
            LocalDateTime dateTime = LocalDateTime.parse(dateStr);
            return dateTime.format(DateTimeFormatter.ofPattern("MMM dd, yyyy 'at' HH:mm"));
        } catch (Exception e) {
            if (dateStr != null && dateStr.length() > 10) {
                return dateStr.substring(0, 10);
            }
            return dateStr != null ? dateStr : "Unknown";
        }
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}

