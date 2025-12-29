package org.example.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.geometry.Insets;
import org.example.MainApp;
import org.example.model.Group;
import org.example.model.User;
import org.example.service.GroupService;
import org.example.util.SessionManager;

import java.util.List;
import java.util.Optional;

public class GroupSelectionController {

    @FXML
    private ListView<Group> groupsListView;

    @FXML
    private Button createGroupButton;

    @FXML
    private Button selectGroupButton;

    @FXML
    private Button viewMembersButton;

    @FXML
    private Button leaveGroupButton;

    @FXML
    private Button refreshButton;

    @FXML
    private Button backButton;

    private ObservableList<Group> groupsList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        setupListView();
        loadGroups();
    }

    private void setupListView() {
        groupsListView.setItems(groupsList);

        groupsListView.setCellFactory(param -> new ListCell<Group>() {
            @Override
            protected void updateItem(Group group, boolean empty) {
                super.updateItem(group, empty);
                if (empty || group == null) {
                    setText(null);
                    setGraphic(null);
                    setStyle("");
                } else {
                    setText("👥  " + group.getGroupName());
                    setStyle("-fx-font-size: 14px; -fx-padding: 15; " +
                            "-fx-background-color: rgba(255,255,255,0.05); " +
                            "-fx-text-fill: #E7E8FF; " +
                            "-fx-background-radius: 8;");
                }
            }
        });

        groupsListView.getSelectionModel().selectedItemProperty().addListener(
            (observable, oldValue, newValue) -> {
                boolean hasSelection = newValue != null;
                selectGroupButton.setDisable(!hasSelection);
                viewMembersButton.setDisable(!hasSelection);
                leaveGroupButton.setDisable(!hasSelection);
            }
        );

        groupsListView.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                Group selected = groupsListView.getSelectionModel().getSelectedItem();
                if (selected != null) {
                    handleSelectGroup();
                }
            }
        });
    }

    private void loadGroups() {
        User currentUser = SessionManager.getInstance().getCurrentUser();
        if (currentUser != null) {
            List<Group> groups = GroupService.getUserGroups(currentUser.getUserId());
            groupsList.clear();
            if (groups != null) {
                groupsList.addAll(groups);
            }
        }
    }

    @FXML
    private void handleCreateGroup() {

        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Create New Group");
        dialog.setHeaderText("Enter group name");

        ButtonType createButtonType = new ButtonType("Create", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(createButtonType, ButtonType.CANCEL);

        VBox content = new VBox(10);
        content.setPadding(new Insets(20));
        TextField groupNameField = new TextField();
        groupNameField.setPromptText("Group name");
        groupNameField.setPrefWidth(300);
        groupNameField.setStyle("-fx-font-size: 14px; -fx-padding: 10;");

        Label instructionLabel = new Label("Choose a unique name for your expense group");
        instructionLabel.setStyle("-fx-text-fill: #666; -fx-font-size: 12px;");

        content.getChildren().addAll(instructionLabel, groupNameField);
        dialog.getDialogPane().setContent(content);

        javafx.application.Platform.runLater(() -> groupNameField.requestFocus());

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == createButtonType) {
                return groupNameField.getText();
            }
            return null;
        });

        Optional<String> result = dialog.showAndWait();

        result.ifPresent(groupName -> {
            if (groupName.trim().isEmpty()) {
                showErrorAlert("Group name cannot be empty");
                return;
            }

            User currentUser = SessionManager.getInstance().getCurrentUser();
            if (currentUser != null) {
                boolean success = GroupService.createGroup(groupName.trim(), currentUser.getUserId());
                if (success) {
                    showSuccessAlert("Group created successfully!");
                    loadGroups(); // Refresh the list
                } else {
                    showErrorAlert("Failed to create group. Please try again.");
                }
            }
        });
    }

    @FXML
    private void handleSelectGroup() {
        Group selectedGroup = groupsListView.getSelectionModel().getSelectedItem();
        if (selectedGroup != null) {
            SessionManager.getInstance().setCurrentGroupId(selectedGroup.getGroupId());
            loadGroupDashboard();
        }
    }

    @FXML
    private void handleViewMembers() {
        Group selectedGroup = groupsListView.getSelectionModel().getSelectedItem();
        if (selectedGroup != null) {
            SessionManager.getInstance().setCurrentGroupId(selectedGroup.getGroupId());
            loadGroupMemberList();
        }
    }

    @FXML
    private void handleLeaveGroup() {
        Group selectedGroup = groupsListView.getSelectionModel().getSelectedItem();
        if (selectedGroup == null) return;

        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Leave Group");
        confirmAlert.setHeaderText("Are you sure you want to leave this group?");
        confirmAlert.setContentText("Group: " + selectedGroup.getGroupName());

        confirmAlert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                User currentUser = SessionManager.getInstance().getCurrentUser();
                if (currentUser != null) {
                    boolean success = GroupService.leaveGroup(
                        selectedGroup.getGroupId(),
                        currentUser.getUserId()
                    );
                    if (success) {
                        showSuccessAlert("You have left the group successfully");
                        loadGroups(); // Refresh the list
                    } else {
                        showErrorAlert("Failed to leave group. Please try again.");
                    }
                }
            }
        });
    }

    @FXML
    private void handleRefresh() {
        loadGroups();
    }

    @FXML
    private void handleBack() {
        MainApp.loadModeSelection();
    }

    private void loadGroupDashboard() {
        try {
            System.out.println("Loading Group Dashboard...");

            Group selectedGroup = groupsListView.getSelectionModel().getSelectedItem();
            if (selectedGroup == null) {
                showErrorAlert("Please select a group first!");
                return;
            }

            String groupId = selectedGroup.getGroupId();
            String groupName = selectedGroup.getGroupName();
            String userId = SessionManager.getInstance().getCurrentUser().getUserId();

            System.out.println("Opening group: " + groupName + " (ID: " + groupId + ")");

            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader();
            loader.setLocation(getClass().getResource("/fxml/group_dashboard.fxml"));

            if (loader.getLocation() == null) {
                showErrorAlert("Error: group_dashboard.fxml not found!");
                return;
            }

            javafx.scene.Parent root = loader.load();

            Object controllerObj = loader.getController();
            if (controllerObj != null) {
                try {
                    var method = controllerObj.getClass().getMethod("initWithGroup", String.class, String.class, String.class);
                    method.invoke(controllerObj, groupId, userId, groupName);
                    System.out.println("Initialized GroupDashboardController with groupId: " + groupId);
                } catch (Exception ex) {
                    System.err.println("Error initializing GroupDashboardController: " + ex.getMessage());
                    ex.printStackTrace();
                }
            }

            javafx.scene.Scene scene = new javafx.scene.Scene(root);

            java.net.URL cssResource = getClass().getResource("/css/styles.css");
            if (cssResource != null) {
                scene.getStylesheets().add(cssResource.toExternalForm());
            }

            MainApp.getPrimaryStage().setScene(scene);

            System.out.println("Group Dashboard loaded successfully!");
        } catch (Exception e) {
            System.err.println("Error loading Group Dashboard:");
            e.printStackTrace();
            showErrorAlert("Failed to load Group Dashboard: " + e.getMessage());
        }
    }

    private void loadGroupMemberList() {
        try {
            Group selectedGroup = groupsListView.getSelectionModel().getSelectedItem();
            if (selectedGroup == null) {
                showErrorAlert("Please select a group first!");
                return;
            }

            String groupId = selectedGroup.getGroupId();
            String groupName = selectedGroup.getGroupName();
            String userId = SessionManager.getInstance().getCurrentUser().getUserId();

            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader();
            loader.setLocation(getClass().getResource("/fxml/view_members.fxml"));

            javafx.scene.Parent root = loader.load();

            Object controllerObj = loader.getController();
            if (controllerObj != null) {
                try {
                    var method = controllerObj.getClass().getMethod("initWithGroup", String.class, String.class, String.class);
                    method.invoke(controllerObj, groupId, userId, groupName);
                    System.out.println("Initializing ViewMembersController with groupId: " + groupId + ", userId: " + userId);
                } catch (Exception ex) {
                    System.err.println("Error initializing ViewMembersController: " + ex.getMessage());
                }
            }

            javafx.scene.Scene scene = new javafx.scene.Scene(root);

            java.net.URL cssResource = getClass().getResource("/css/styles.css");
            if (cssResource != null) {
                scene.getStylesheets().add(cssResource.toExternalForm());
            }

            MainApp.getPrimaryStage().setScene(scene);
        } catch (Exception e) {
            e.printStackTrace();
            showErrorAlert("Failed to load Members view: " + e.getMessage());
        }
    }

    private void showSuccessAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Success");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showErrorAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}