package org.example.controller;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.example.model.User;
import org.example.service.GroupService;
import org.example.service.UserService;

public class AddMemberDialogController {

    @FXML
    private RadioButton byEmailRadio;

    @FXML
    private RadioButton byUserIdRadio;

    @FXML
    private VBox emailSection;

    @FXML
    private VBox userIdSection;

    @FXML
    private TextField emailField;

    @FXML
    private TextField userIdField;

    @FXML
    private Label errorLabel;

    @FXML
    private Label successLabel;

    @FXML
    private Label userIdHint;

    private String currentGroupId;
    private String currentGroupName;

    @FXML
    public void initialize() {

        byEmailRadio.selectedProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal) {

                emailSection.setVisible(true);
                emailSection.setManaged(true);
                userIdSection.setVisible(false);
                userIdSection.setManaged(false);
            } else {

                emailSection.setVisible(false);
                emailSection.setManaged(false);
                userIdSection.setVisible(true);
                userIdSection.setManaged(true);
            }
        });

        byUserIdRadio.selectedProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal) {

                emailSection.setVisible(false);
                emailSection.setManaged(false);
                userIdSection.setVisible(true);
                userIdSection.setManaged(true);
            } else {

                emailSection.setVisible(true);
                emailSection.setManaged(true);
                userIdSection.setVisible(false);
                userIdSection.setManaged(false);
            }
        });

        emailSection.setVisible(true);
        emailSection.setManaged(true);
        userIdSection.setVisible(false);
        userIdSection.setManaged(false);
    }

    public void setGroupContext(String groupId, String groupName) {
        this.currentGroupId = groupId;
        this.currentGroupName = groupName;
        System.out.println("AddMemberDialog initialized for group: " + groupName + " (ID: " + groupId + ")");
    }

    @FXML
    private void handleAddMember() {
        System.out.println("=== ADD MEMBER REQUEST ===");
        clearMessages();

        if (currentGroupId == null || currentGroupId.isEmpty()) {
            showError("Error: No group selected!");
            System.err.println("CRITICAL: groupId is null or empty!");
            return;
        }

        System.out.println("Group ID: " + currentGroupId);

        User userToAdd = null;

        if (byEmailRadio.isSelected()) {

            String email = emailField.getText().trim().toLowerCase();

            if (email.isEmpty()) {
                showError("Please enter an email address");
                return;
            }

            System.out.println("Searching for user by email: " + email);
            userToAdd = UserService.getUserByEmail(email);

            if (userToAdd == null) {
                showError("No user found with email: " + email);
                System.err.println("User not found with email: " + email);
                return;
            }

            System.out.println("Found user: " + userToAdd.getName() + " (ID: " + userToAdd.getUserId() + ")");

        } else {

            String userId = userIdField.getText().trim();

            if (userId.isEmpty()) {
                showError("Please enter a user ID");
                return;
            }

            System.out.println("Searching for user by ID: " + userId);
            userToAdd = UserService.getUserById(userId);

            if (userToAdd == null) {
                showError("No user found with ID: " + userId);
                System.err.println("User not found with ID: " + userId);
                return;
            }

            System.out.println("Found user: " + userToAdd.getName() + " (ID: " + userToAdd.getUserId() + ")");
        }

        final User finalUserToAdd = userToAdd;

        if (GroupService.isMemberOfGroup(currentGroupId, finalUserToAdd.getUserId())) {
            showError(finalUserToAdd.getName() + " is already a member of this group");
            return;
        }

        String inviterId = org.example.util.SessionManager.getInstance().getCurrentUser() != null
                ? org.example.util.SessionManager.getInstance().getCurrentUser().getUserId() : null;

        if (inviterId == null) {
            showError("You must be logged in to send invites.");
            return;
        }

        System.out.println("Sending group invite...");
        String inviteId = org.example.service.InviteService.sendGroupInvite(currentGroupId, inviterId, finalUserToAdd.getUserId());

        if (inviteId == null) {
            showError("Failed to create invite in database");
            return;
        }

        showSuccess("Invite sent to " + finalUserToAdd.getName() + "!\nThey must accept it from Alerts to join the group.");
        System.out.println("✓ Group invite sent successfully! Invite ID: " + inviteId);
        clearInputs();

        javafx.animation.PauseTransition pause = new javafx.animation.PauseTransition(javafx.util.Duration.seconds(2));
        pause.setOnFinished(event -> {
            Stage stage = (Stage) emailField.getScene().getWindow();
            stage.close();
        });
        pause.play();

        System.out.println("========================");
    }

    @FXML
    private void handleCancel() {
        Stage stage = (Stage) emailField.getScene().getWindow();
        stage.close();
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
        successLabel.setVisible(false);
        successLabel.setManaged(false);
    }

    private void showSuccess(String message) {
        successLabel.setText(message);
        successLabel.setVisible(true);
        successLabel.setManaged(true);
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
    }

    private void clearMessages() {
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
        successLabel.setVisible(false);
        successLabel.setManaged(false);
    }

    private void clearInputs() {
        emailField.clear();
        userIdField.clear();
    }
}