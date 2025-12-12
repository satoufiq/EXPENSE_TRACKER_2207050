package org.example.controller;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.example.model.User;
import org.example.service.GroupService;
import org.example.service.UserService;

/**
 * Add Member Dialog Controller
 * Handles adding members to a group by email or user ID
 */
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
        // Setup radio button listeners for email/ID switching
        byEmailRadio.selectedProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal) {
                // Email is selected
                emailSection.setVisible(true);
                emailSection.setManaged(true);
                userIdSection.setVisible(false);
                userIdSection.setManaged(false);
            } else {
                // User ID is selected
                emailSection.setVisible(false);
                emailSection.setManaged(false);
                userIdSection.setVisible(true);
                userIdSection.setManaged(true);
            }
        });

        byUserIdRadio.selectedProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal) {
                // User ID is selected
                emailSection.setVisible(false);
                emailSection.setManaged(false);
                userIdSection.setVisible(true);
                userIdSection.setManaged(true);
            } else {
                // Email is selected
                emailSection.setVisible(true);
                emailSection.setManaged(true);
                userIdSection.setVisible(false);
                userIdSection.setManaged(false);
            }
        });

        // Initial setup - email is selected by default
        emailSection.setVisible(true);
        emailSection.setManaged(true);
        userIdSection.setVisible(false);
        userIdSection.setManaged(false);
    }

    /**
     * Set group context for this dialog
     */
    public void setGroupContext(String groupId, String groupName) {
        this.currentGroupId = groupId;
        this.currentGroupName = groupName;
        System.out.println("AddMemberDialog initialized for group: " + groupName + " (ID: " + groupId + ")");
    }

    /**
     * Handle Add Member button
     */
    @FXML
    private void handleAddMember() {
        System.out.println("=== ADD MEMBER REQUEST ===");
        clearMessages();

        // Validate group ID
        if (currentGroupId == null || currentGroupId.isEmpty()) {
            showError("Error: No group selected!");
            System.err.println("CRITICAL: groupId is null or empty!");
            return;
        }

        System.out.println("Group ID: " + currentGroupId);

        User userToAdd = null;

        if (byEmailRadio.isSelected()) {
            // Add by email
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
            // Add by user ID
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

        // Check if already a member
        if (GroupService.isMemberOfGroup(currentGroupId, userToAdd.getUserId())) {
            showError(userToAdd.getName() + " is already a member of this group");
            System.err.println("User already member of group");
            return;
        }

        // Send invite instead of direct add
        System.out.println("Sending group invite...");
        String inviterId = org.example.util.SessionManager.getInstance().getCurrentUser() != null
                ? org.example.util.SessionManager.getInstance().getCurrentUser().getUserId() : null;

        if (inviterId == null) {
            showError("You must be logged in to send invites.");
            System.err.println("No logged-in user found");
            return;
        }

        String inviteId = org.example.service.InviteService.sendGroupInvite(currentGroupId, inviterId, userToAdd.getUserId());

        if (inviteId != null) {
            showSuccess("Invite sent to " + userToAdd.getName() + "!\nThey must accept it from Alerts to join the group.");
            System.out.println("✓ Group invite sent successfully! Invite ID: " + inviteId);
            clearInputs();

            // Close dialog after short delay
            javafx.application.Platform.runLater(() -> {
                try {
                    Thread.sleep(2000);
                    Stage stage = (Stage) emailField.getScene().getWindow();
                    stage.close();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            });
        } else {
            showError("Failed to send invite. Please try again.");
            System.err.println("Failed to create invite in database");
        }

        System.out.println("========================");
    }

    /**
     * Handle Cancel button
     */
    @FXML
    private void handleCancel() {
        Stage stage = (Stage) emailField.getScene().getWindow();
        stage.close();
    }

    /**
     * Show error message
     */
    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
        successLabel.setVisible(false);
        successLabel.setManaged(false);
    }

    /**
     * Show success message
     */
    private void showSuccess(String message) {
        successLabel.setText(message);
        successLabel.setVisible(true);
        successLabel.setManaged(true);
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
    }

    /**
     * Clear all messages
     */
    private void clearMessages() {
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
        successLabel.setVisible(false);
        successLabel.setManaged(false);
    }

    /**
     * Clear input fields
     */
    private void clearInputs() {
        emailField.clear();
        userIdField.clear();
    }
}

