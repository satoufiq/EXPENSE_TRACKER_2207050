package org.example.controller;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import org.example.service.GroupService;

/**
 * Add Member Controller with ObservableList integration
 * Adds members to groups with real-time updates
 */
public class AddMemberController {

    @FXML
    private TextField emailField;

    @FXML
    private Label errorLabel;

    @FXML
    private Label successLabel;

    @FXML
    private Button addButton;

    @FXML
    private Button cancelButton;

    private String groupId;
    private Stage dialogStage;

    @FXML
    public void initialize() {
        // Setup listeners
    }

    /**
     * Set the dialog stage
     */
    public void setDialogStage(Stage stage) {
        this.dialogStage = stage;
    }

    /**
     * Set group context
     */
    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    /**
     * Handle add member action
     */
    @FXML
    private void handleAddMember() {
        hideMessages();

        // Validate groupId first
        if (groupId == null || groupId.isEmpty()) {
            showError("ERROR: Group ID is not set. Please ensure you selected a group before adding members.");
            System.err.println("CRITICAL: groupId is null or empty!");
            return;
        }

        String email = emailField.getText().trim().toLowerCase(); // Trim and lowercase for consistency

        if (email.isEmpty()) {
            showError("Please enter an email address");
            return;
        }

        if (!isValidEmail(email)) {
            showError("Please enter a valid email address");
            return;
        }

        System.out.println("=== Add Member Request ===");
        System.out.println("Group ID: " + groupId);
        System.out.println("Email: " + email);

        // Check if user exists first
        org.example.model.User user = org.example.service.UserService.getUserByEmail(email);
        if (user == null) {
            showError("No user found with email: " + email + "\nPlease make sure the user has registered first.");
            return;
        }

        System.out.println("Found user: " + user.getName() + " (ID: " + user.getUserId() + ")");

        // Check if already a member
        if (org.example.service.GroupService.isMemberOfGroup(groupId, user.getUserId())) {
            showError("This user is already a member of the group.");
            return;
        }

        // Add member - this will automatically update ObservableList
        boolean success = org.example.service.GroupService.addMemberToGroupByEmail(groupId, email);

        if (success) {
            showSuccess("Member added successfully!");
            // Clear field for adding another member
            emailField.clear();

            // Close dialog after a short delay to show success message
            javafx.application.Platform.runLater(() -> {
                try {
                    Thread.sleep(1000);
                    if (dialogStage != null) {
                        dialogStage.close();
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            });
        } else {
            showError("Failed to add member. Please try again.");
        }
    }

    /**
     * Handle cancel action
     */
    @FXML
    private void handleCancel() {
        if (dialogStage != null) {
            dialogStage.close();
        }
    }

    /**
     * Validate email format
     */
    private boolean isValidEmail(String email) {
        String emailRegex = "^[A-Za-z0-9+_.-]+@(.+)$";
        return email.matches(emailRegex);
    }

    /**
     * Show error message
     */
    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
        successLabel.setVisible(false);
    }

    /**
     * Show success message
     */
    private void showSuccess(String message) {
        successLabel.setText(message);
        successLabel.setVisible(true);
        errorLabel.setVisible(false);
    }

    /**
     * Hide all messages
     */
    private void hideMessages() {
        errorLabel.setVisible(false);
        successLabel.setVisible(false);
    }
}

