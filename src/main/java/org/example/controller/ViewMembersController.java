package org.example.controller;

import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.example.model.GroupMember;
import org.example.service.GroupService;

import java.io.IOException;

/**
 * View Members Controller
 * Displays all members in a group with options to manage them
 */
public class ViewMembersController {

    @FXML
    private Label groupNameLabel;

    @FXML
    private ListView<GroupMember> membersList;

    private String currentGroupId;
    private String currentUserId;
    private String currentGroupName;

    @FXML
    public void initialize() {
        // Setup custom cell factory for member list items
        membersList.setCellFactory(param -> new MemberListCell());
    }

    /**
     * Initialize with group information
     */
    public void initWithGroup(String groupId, String userId, String groupName) {
        System.out.println("=== ViewMembersController Initialization ===");
        this.currentGroupId = groupId;
        this.currentUserId = userId;
        this.currentGroupName = groupName;

        System.out.println("Group ID: " + groupId);
        System.out.println("User ID: " + userId);
        System.out.println("Group Name: " + groupName);

        groupNameLabel.setText(groupName);

        loadMembers();

        System.out.println("=== Initialization Complete ===");
    }

    /**
     * Load members for this group
     */
    private void loadMembers() {
        if (currentGroupId == null || currentGroupId.isEmpty()) {
            membersList.setItems(null);
            return;
        }

        ObservableList<GroupMember> members = GroupService.getGroupMembersObservable(currentGroupId);
        membersList.setItems(members);

        System.out.println("Loaded " + members.size() + " members for group: " + currentGroupName);
    }

    /**
     * Handle Add Member button
     */
    @FXML
    private void handleAddMember() {
        if (currentGroupId == null) {
            showError("No group selected!");
            return;
        }

        try {
            System.out.println("Opening Add Member dialog from View Members");

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/add_member.fxml"));
            Stage dialog = new Stage();
            dialog.setTitle("Add Member to " + currentGroupName);
            Scene scene = new Scene(loader.load());
            dialog.setScene(scene);
            dialog.initModality(Modality.WINDOW_MODAL);
            dialog.setResizable(false);
            dialog.setWidth(700);
            dialog.setHeight(500);

            // Initialize controller using reflection
            Object controllerObj = loader.getController();
            if (controllerObj != null) {
                try {
                    var method = controllerObj.getClass().getMethod("setGroupContext", String.class, String.class);
                    method.invoke(controllerObj, currentGroupId, currentGroupName);
                    System.out.println("Dialog controller initialized");
                } catch (Exception ex) {
                    System.err.println("Error initializing dialog: " + ex.getMessage());
                }
            }

            dialog.showAndWait();

            // Refresh member list
            loadMembers();

        } catch (IOException e) {
            e.printStackTrace();
            showError("Failed to open add member dialog: " + e.getMessage());
        }
    }

    /**
     * Handle Back button
     */
    @FXML
    private void handleBack() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/group_dashboard.fxml"));
            Scene scene = new Scene(loader.load());

            // Initialize controller using reflection
            Object controllerObj = loader.getController();
            if (controllerObj != null) {
                try {
                    var method = controllerObj.getClass().getMethod("initWithGroup", String.class, String.class, String.class);
                    method.invoke(controllerObj, currentGroupId, currentUserId, currentGroupName);
                    System.out.println("Group Dashboard controller initialized");
                } catch (Exception ex) {
                    System.err.println("Error initializing Group Dashboard: " + ex.getMessage());
                }
            }

            Stage stage = (Stage) groupNameLabel.getScene().getWindow();
            stage.setScene(scene);

        } catch (IOException e) {
            e.printStackTrace();
            showError("Failed to go back: " + e.getMessage());
        }
    }

    /**
     * Show error dialog
     */
    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setContentText(message);
        alert.showAndWait();

        System.err.println("ERROR: " + message);
    }

    /**
     * Custom ListCell for displaying members with action buttons
     */
    private class MemberListCell extends ListCell<GroupMember> {
        private final Label nameLabel = new Label();
        private final Label emailLabel = new Label();
        private final Region spacer = new Region();
        private final Button removeBtn = new Button("Remove");
        private final HBox hbox = new HBox(10);

        public MemberListCell() {
            nameLabel.setStyle("-fx-font-size: 14; -fx-font-weight: bold;");
            emailLabel.setStyle("-fx-font-size: 12; -fx-text-fill: #666666;");
            removeBtn.setStyle("-fx-padding: 5 15;");

            HBox.setHgrow(spacer, Priority.ALWAYS);
            hbox.setStyle("-fx-padding: 10; -fx-border-color: #e0e0e0; -fx-border-width: 0 0 1 0;");
            hbox.getChildren().addAll(nameLabel, emailLabel, spacer, removeBtn);
        }

        @Override
        protected void updateItem(GroupMember member, boolean empty) {
            super.updateItem(member, empty);

            if (empty || member == null) {
                setGraphic(null);
            } else {
                nameLabel.setText(member.getName());
                emailLabel.setText(member.getEmail());

                removeBtn.setOnAction(event -> {
                    // Confirm before removing
                    Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
                    confirm.setTitle("Remove Member");
                    confirm.setHeaderText("Remove " + member.getName() + "?");
                    confirm.setContentText("Are you sure you want to remove this member from the group?");

                    confirm.showAndWait().ifPresent(response -> {
                        if (response == ButtonType.OK) {
                            System.out.println("Removing member: " + member.getName());
                            if (GroupService.removeMemberFromGroup(currentGroupId, member.getUserId())) {
                                System.out.println("Member removed successfully");
                                loadMembers();
                            } else {
                                showError("Failed to remove member");
                            }
                        }
                    });
                });

                setGraphic(hbox);
            }
        }
    }
}

