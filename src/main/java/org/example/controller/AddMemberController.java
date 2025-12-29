package org.example.controller;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import org.example.service.GroupService;
import org.example.service.InviteService;
import org.example.service.UserService;
import org.example.util.SessionManager;

public class AddMemberController {

    @FXML private TextField emailField;
    @FXML private Label errorLabel;
    @FXML private Label successLabel;
    @FXML private Button addButton;
    @FXML private Button cancelButton;

    private String groupId;
    private String groupName;
    private String requesterId;
    private Stage dialogStage;

    @FXML
    public void initialize() {
    }

    public void setDialogStage(Stage stage) {
        this.dialogStage = stage;
    }

    public void setGroupContext(String groupId, String groupName) {
        this.groupId = groupId;
        this.groupName = groupName;
        if (SessionManager.getInstance().getCurrentUser() != null) {
            this.requesterId = SessionManager.getInstance().getCurrentUser().getUserId();
        }
    }

    public void setGroupContext(String groupId, String groupName, String requesterId) {
        this.groupId = groupId;
        this.groupName = groupName;
        this.requesterId = requesterId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    @FXML
    private void handleAddMember() {
        hideMessages();

        if (groupId == null || groupId.isEmpty()) {
            showError("ERROR: Group ID is not set.");
            return;
        }

        // Get requester ID
        if (requesterId == null && SessionManager.getInstance().getCurrentUser() != null) {
            requesterId = SessionManager.getInstance().getCurrentUser().getUserId();
        }

        // Check if requester is admin
        if (requesterId == null || !GroupService.isAdmin(groupId, requesterId)) {
            showError("Only group admins can add members.");
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

        org.example.model.User user = UserService.getUserByEmail(email);
        if (user == null) {
            showError("No user found with email: " + email + "\nPlease make sure the user has registered first.");
            return;
        }

        if (GroupService.isMemberOfGroup(groupId, user.getUserId())) {
            showError("This user is already a member of the group.");
            return;
        }

        // Check if invite already pending
        if (InviteService.hasPendingGroupInvite(groupId, user.getUserId())) {
            showError("An invite is already pending for this user.");
            return;
        }

        String inviteId = InviteService.sendGroupInvite(groupId, requesterId, user.getUserId());
        if (inviteId != null) {
            showSuccess("Invite sent to " + user.getName() + ".\nThey must accept it from their Alerts.");
            emailField.clear();

            javafx.application.Platform.runLater(() -> {
                try {
                    Thread.sleep(1500);
                    if (dialogStage != null) {
                        dialogStage.close();
                    } else {
                        Stage stage = (Stage) addButton.getScene().getWindow();
                        stage.close();
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            });
        } else {
            showError("Failed to send invite. Please try again.");
        }
    }

    @FXML
    private void handleCancel() {
        if (dialogStage != null) {
            dialogStage.close();
        } else if (cancelButton != null) {
            Stage stage = (Stage) cancelButton.getScene().getWindow();
            stage.close();
        }
    }

    private boolean isValidEmail(String email) {
        String emailRegex = "^[A-Za-z0-9+_.-]+@(.+)$";
        return email.matches(emailRegex);
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
        successLabel.setVisible(false);
    }

    private void showSuccess(String message) {
        successLabel.setText(message);
        successLabel.setVisible(true);
        errorLabel.setVisible(false);
    }

    private void hideMessages() {
        errorLabel.setVisible(false);
        successLabel.setVisible(false);
    }
}