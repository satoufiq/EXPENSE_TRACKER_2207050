package org.example.controller;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import org.example.service.GroupService;
import org.example.service.InviteService;
import org.example.service.UserService;
import org.example.util.SessionManager;

/**
 * Add Member Controller with ObservableList integration
 * Sends group invites instead of direct add
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
    private String groupName;
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
     * For backward compatibility with GroupDashboardController reflection
     */
    public void setGroupContext(String groupId, String groupName) {
        this.groupId = groupId;
        this.groupName = groupName;
    }

    /**
     * Set group ID only (older code path)
     */
    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    /**
     * Handle add member action: send invite
     */
    @FXML
    private void handleAddMember() {
        hideMessages();

        if (groupId == null || groupId.isEmpty()) {
            showError("ERROR: Group ID is not set. Please ensure you selected a group before adding members.");
            System.err.println("CRITICAL: groupId is null or empty!");
            return;
        }

        String email = emailField.getText().trim().toLowerCase();
        if (email.isEmpty()) {
            showError("Please enter an email address");
            return;
        }
        if (!isValidEmail(email)) {
            showError("Please enter a valid email address");
            return;
        }

        System.out.println("=== Group Invite Request ===");
        System.out.println("Group ID: " + groupId);
        System.out.println("Email: " + email);

        // Check if user exists first
        org.example.model.User user = UserService.getUserByEmail(email);
        if (user == null) {
            showError("No user found with email: " + email + "\nPlease make sure the user has registered first.");
            return;
        }

        System.out.println("Found user: " + user.getName() + " (ID: " + user.getUserId() + ")");

        // Check if already a member
        if (GroupService.isMemberOfGroup(groupId, user.getUserId())) {
            showError("This user is already a member of the group.");
            return;
        }

        // Send invite instead of direct add
        String inviterId = SessionManager.getInstance().getCurrentUser() != null
                ? SessionManager.getInstance().getCurrentUser().getUserId() : null;
        if (inviterId == null) {
            showError("You must be logged in to send invites.");
            return;
        }

        String inviteId = InviteService.sendGroupInvite(groupId, inviterId, user.getUserId());
        if (inviteId != null) {
            showSuccess("Invite sent to " + user.getName() + ". They must accept it from Alerts.");
            emailField.clear();
            // Optionally close after short delay
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
            showError("Failed to send invite. Please try again.");
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
