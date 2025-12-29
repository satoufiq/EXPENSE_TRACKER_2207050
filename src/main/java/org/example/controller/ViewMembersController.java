package org.example.controller;

import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.example.model.GroupMember;
import org.example.service.ExpenseService;
import org.example.service.GroupService;

import java.io.IOException;

public class ViewMembersController {

    @FXML private Label groupNameLabel;
    @FXML private Label memberCountLabel;
    @FXML private Label adminCountLabel;
    @FXML private VBox memberCardsPane;
    @FXML private VBox emptyStatePane;

    private String currentGroupId;
    private String currentUserId;
    private String currentGroupName;
    private boolean isCurrentUserAdmin;
    private ObservableList<GroupMember> membersList;

    @FXML
    public void initialize() {
        // Will be called before initWithGroup
    }

    public void initWithGroup(String groupId, String oderId, String groupName) {
        this.currentGroupId = groupId;
        this.currentUserId = oderId;
        this.currentGroupName = groupName;
        this.isCurrentUserAdmin = GroupService.isAdmin(groupId, oderId);

        groupNameLabel.setText(groupName + (isCurrentUserAdmin ? " (Admin)" : ""));

        loadMembers();
    }

    private void loadMembers() {
        if (currentGroupId == null || currentGroupId.isEmpty()) {
            return;
        }

        membersList = GroupService.getGroupMembersObservable(currentGroupId);
        refreshMemberCards();
    }

    private void refreshMemberCards() {
        if (memberCardsPane == null) return;

        memberCardsPane.getChildren().clear();

        if (membersList == null || membersList.isEmpty()) {
            if (emptyStatePane != null) {
                emptyStatePane.setVisible(true);
                emptyStatePane.setManaged(true);
            }
            if (memberCountLabel != null) memberCountLabel.setText("0");
            if (adminCountLabel != null) adminCountLabel.setText("0");
            return;
        }

        if (emptyStatePane != null) {
            emptyStatePane.setVisible(false);
            emptyStatePane.setManaged(false);
        }

        int adminCount = 0;
        for (GroupMember member : membersList) {
            memberCardsPane.getChildren().add(createHorizontalMemberCard(member));
            if (member.isAdmin()) {
                adminCount++;
            }
        }

        if (memberCountLabel != null) memberCountLabel.setText(String.valueOf(membersList.size()));
        if (adminCountLabel != null) adminCountLabel.setText(String.valueOf(adminCount));
    }

    private HBox createHorizontalMemberCard(GroupMember member) {
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
        String initials = getInitials(member.getName());
        Label initialsLabel = new Label(initials);
        initialsLabel.getStyleClass().add("member-avatar-text");
        avatar.getChildren().add(initialsLabel);

        // Info section
        VBox info = new VBox(4);
        info.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(info, Priority.ALWAYS);

        Label nameLabel = new Label(member.getName());
        nameLabel.getStyleClass().add("member-name");

        Label emailLabel = new Label("📧 " + member.getEmail());
        emailLabel.getStyleClass().add("member-email");

        // Get expense count for this member
        var expenses = ExpenseService.getGroupExpensesObservable(currentGroupId);
        long expenseCount = expenses.stream()
            .filter(e -> member.getUserId().equals(e.getUserId()))
            .count();
        double totalSpent = expenses.stream()
            .filter(e -> member.getUserId().equals(e.getUserId()))
            .mapToDouble(e -> e.getAmount())
            .sum();

        Label statsLabel = new Label(String.format("💰 %d expenses • ৳%.2f", expenseCount, totalSpent));
        statsLabel.setStyle("-fx-text-fill: #7b8ae4; -fx-font-size: 11px;");

        info.getChildren().addAll(nameLabel, emailLabel, statsLabel);

        // Role badge
        Label roleLabel = new Label(member.isAdmin() ? "👑 Admin" : "👤 Member");
        roleLabel.getStyleClass().add("member-role-badge");
        if (member.isAdmin()) {
            roleLabel.setStyle("-fx-background-color: rgba(255, 193, 7, 0.4); -fx-text-fill: #ffd54f; -fx-padding: 4 10; -fx-background-radius: 10;");
        } else {
            roleLabel.setStyle("-fx-background-color: rgba(76, 175, 80, 0.3); -fx-text-fill: #81c784; -fx-padding: 4 10; -fx-background-radius: 10;");
        }

        // Spacer
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.SOMETIMES);

        // Action buttons (only for admins)
        HBox actions = new HBox(8);
        actions.setAlignment(Pos.CENTER_RIGHT);

        if (isCurrentUserAdmin) {
            // Don't show actions for self
            boolean isSelf = member.getUserId().equals(currentUserId);

            if (!isSelf) {
                // Promote/Demote button
                if (member.isAdmin()) {
                    Button demoteBtn = new Button("⬇️ Demote");
                    demoteBtn.getStyleClass().add("card-action-button");
                    demoteBtn.setStyle("-fx-background-color: rgba(255, 152, 0, 0.3); -fx-border-color: rgba(255, 152, 0, 0.5);");
                    demoteBtn.setOnAction(e -> handleDemoteMember(member));
                    actions.getChildren().add(demoteBtn);
                } else {
                    Button promoteBtn = new Button("⬆️ Promote");
                    promoteBtn.getStyleClass().add("card-action-button");
                    promoteBtn.setStyle("-fx-background-color: rgba(255, 193, 7, 0.3); -fx-border-color: rgba(255, 193, 7, 0.5);");
                    promoteBtn.setOnAction(e -> handlePromoteMember(member));
                    actions.getChildren().add(promoteBtn);
                }

                // Remove button
                Button removeBtn = new Button("🗑️ Remove");
                removeBtn.getStyleClass().addAll("card-action-button", "card-delete-button");
                removeBtn.setOnAction(e -> handleRemoveMember(member));
                actions.getChildren().add(removeBtn);
            } else {
                // Show "You" indicator for self
                Label youLabel = new Label("(You)");
                youLabel.setStyle("-fx-text-fill: rgba(255,255,255,0.5); -fx-font-style: italic;");
                actions.getChildren().add(youLabel);
            }
        }

        card.getChildren().addAll(avatar, info, roleLabel, spacer, actions);

        return card;
    }

    private String getInitials(String name) {
        if (name == null || name.isEmpty()) return "?";
        String[] parts = name.trim().split("\\s+");
        if (parts.length >= 2) {
            return (parts[0].charAt(0) + "" + parts[1].charAt(0)).toUpperCase();
        }
        return name.substring(0, Math.min(2, name.length())).toUpperCase();
    }

    private void handlePromoteMember(GroupMember member) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Promote to Admin");
        confirm.setHeaderText("Promote " + member.getName() + " to Admin?");
        confirm.setContentText("This user will be able to:\n• Add and remove members\n• Promote/demote other members\n• Manage group settings");

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                if (GroupService.promoteToAdmin(currentGroupId, member.getUserId(), currentUserId)) {
                    showSuccess("Successfully promoted " + member.getName() + " to Admin!");
                    loadMembers();
                } else {
                    showError("Failed to promote member");
                }
            }
        });
    }

    private void handleDemoteMember(GroupMember member) {
        int adminCount = GroupService.getAdminCount(currentGroupId);

        if (adminCount <= 1) {
            showError("Cannot demote the last admin. The group must have at least one admin.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Demote to Member");
        confirm.setHeaderText("Demote " + member.getName() + " to regular member?");
        confirm.setContentText("This user will no longer be able to manage the group.");

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                if (GroupService.demoteToMember(currentGroupId, member.getUserId(), currentUserId)) {
                    showSuccess("Successfully demoted " + member.getName() + " to Member");
                    loadMembers();
                } else {
                    showError("Failed to demote member");
                }
            }
        });
    }

    private void handleRemoveMember(GroupMember member) {
        // Check if trying to remove an admin when there's only one
        if (member.isAdmin() && GroupService.getAdminCount(currentGroupId) <= 1) {
            showError("Cannot remove the last admin. Promote another member first.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Remove Member");
        confirm.setHeaderText("Remove " + member.getName() + "?");
        confirm.setContentText("Are you sure you want to remove this member from the group?\nThis action cannot be undone.");

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                if (GroupService.removeMemberFromGroup(currentGroupId, member.getUserId(), currentUserId)) {
                    showSuccess("Successfully removed " + member.getName());
                    loadMembers();
                } else {
                    showError("Failed to remove member. You must be an admin to remove members.");
                }
            }
        });
    }

    @FXML
    private void handleAddMember() {
        if (currentGroupId == null) {
            showError("No group selected!");
            return;
        }

        // Check if user is admin
        if (!isCurrentUserAdmin) {
            showError("Only admins can add members to the group.");
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/add_member.fxml"));
            Stage dialog = new Stage();
            dialog.setTitle("Add Member to " + currentGroupName);
            Scene scene = new Scene(loader.load());
            dialog.setScene(scene);
            dialog.initModality(Modality.WINDOW_MODAL);
            dialog.setResizable(false);
            dialog.setWidth(700);
            dialog.setHeight(500);

            Object controllerObj = loader.getController();
            if (controllerObj != null) {
                try {
                    var method = controllerObj.getClass().getMethod("setGroupContext", String.class, String.class, String.class);
                    method.invoke(controllerObj, currentGroupId, currentGroupName, currentUserId);
                } catch (Exception e) {
                    try {
                        var method2 = controllerObj.getClass().getMethod("setGroupContext", String.class, String.class);
                        method2.invoke(controllerObj, currentGroupId, currentGroupName);
                    } catch (Exception ignored) {}
                }
            }

            dialog.showAndWait();
            loadMembers();

        } catch (IOException e) {
            e.printStackTrace();
            showError("Failed to open add member dialog: " + e.getMessage());
        }
    }

    @FXML
    private void handleBack() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/group_dashboard.fxml"));
            Scene scene = new Scene(loader.load());

            Object controllerObj = loader.getController();
            if (controllerObj != null) {
                try {
                    var method = controllerObj.getClass().getMethod("initWithGroup", String.class, String.class, String.class);
                    method.invoke(controllerObj, currentGroupId, currentUserId, currentGroupName);
                } catch (Exception ignored) {}
            }

            Stage stage = (Stage) groupNameLabel.getScene().getWindow();
            stage.setScene(scene);

        } catch (IOException e) {
            e.printStackTrace();
            showError("Failed to go back: " + e.getMessage());
        }
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showSuccess(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Success");
        alert.setContentText(message);
        alert.showAndWait();
    }
}

