package org.example.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import org.example.MainApp;
import org.example.service.InviteService;
import org.example.service.UserService;
import org.example.util.SessionManager;

import java.util.List;

public class AlertsController {

    @FXML
    private Button backButton;

    @FXML
    private ListView<String> parentInvitesList;

    @FXML
    private ListView<String> groupInvitesList;

    @FXML
    private ListView<String> financialAlertsList;

    @FXML
    private Button acceptParentInviteButton;

    @FXML
    private Button declineParentInviteButton;

    @FXML
    private Button acceptGroupInviteButton;

    @FXML
    private Button declineGroupInviteButton;

    @FXML
    private Button dismissAlertButton;

    private final ObservableList<String> parentInvites = FXCollections.observableArrayList();
    private final ObservableList<String> groupInvites = FXCollections.observableArrayList();
    private final ObservableList<String> financialAlerts = FXCollections.observableArrayList();

    private final java.util.Map<String, String> parentInviteMap = new java.util.HashMap<>();
    private final java.util.Map<String, String> groupInviteMap = new java.util.HashMap<>();

    @FXML
    public void initialize() {
        // Back should go to Mode Selection
        backButton.setOnAction(e -> loadModeSelection());
        loadInvitesAndAlerts();
        setupSelectionHandlers();
    }

    private void loadModeSelection() {
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

        final String userId = currentUser.getUserId();

        // Load all invites and alerts concurrently
        org.example.util.ThreadPoolManager.getInstance().executeDatabaseWithCallback(
            () -> {
                // Fetch parent invites
                List<String[]> pInv = InviteService.getPendingParentInvitesForChild(userId);
                java.util.List<String> parentInvitesList = new java.util.ArrayList<>();
                java.util.Map<String, String> parentMap = new java.util.HashMap<>();

                for (String[] row : pInv) {
                    String inviteId = row[0];
                    String parentId = row[1];
                    var parentUser = UserService.getUserById(parentId);
                    String display = "Parent: " + (parentUser != null ? parentUser.getName() : parentId) + " wants to add you";
                    parentInvitesList.add(display);
                    parentMap.put(display, inviteId);
                }

                // Fetch group invites
                List<String[]> gInv = InviteService.getPendingGroupInvitesForUser(userId);
                java.util.List<String> groupInvitesList = new java.util.ArrayList<>();
                java.util.Map<String, String> groupMap = new java.util.HashMap<>();

                for (String[] row : gInv) {
                    String inviteId = row[0];
                    String groupId = row[1];
                    var group = org.example.service.GroupService.getGroupById(groupId);
                    String groupName = group != null ? group.getGroupName() : groupId;
                    String display = "Group invite: " + groupName;
                    groupInvitesList.add(display);
                    groupMap.put(display, inviteId);
                }

                return new Object[]{parentInvitesList, parentMap, groupInvitesList, groupMap};
            },
            result -> {
                Object[] data = (Object[]) result;
                @SuppressWarnings("unchecked")
                java.util.List<String> parentList = (java.util.List<String>) data[0];
                @SuppressWarnings("unchecked")
                java.util.Map<String, String> parentMap = (java.util.Map<String, String>) data[1];
                @SuppressWarnings("unchecked")
                java.util.List<String> groupList = (java.util.List<String>) data[2];
                @SuppressWarnings("unchecked")
                java.util.Map<String, String> groupMap = (java.util.Map<String, String>) data[3];

                // Update UI on JavaFX thread
                parentInvites.clear();
                parentInvites.addAll(parentList);
                parentInviteMap.clear();
                parentInviteMap.putAll(parentMap);
                parentInvitesList.setItems(parentInvites);

                groupInvites.clear();
                groupInvites.addAll(groupList);
                groupInviteMap.clear();
                groupInviteMap.putAll(groupMap);
                groupInvitesList.setItems(groupInvites);

                // Financial alerts - placeholder
                financialAlerts.clear();
                financialAlertsList.setItems(financialAlerts);

                acceptParentInviteButton.setDisable(true);
                declineParentInviteButton.setDisable(true);
                acceptGroupInviteButton.setDisable(true);
                declineGroupInviteButton.setDisable(true);
                dismissAlertButton.setDisable(true);
            },
            error -> {
                System.err.println("Error loading invites: " + error.getMessage());
                error.printStackTrace();
            }
        );
    }

    private void setupSelectionHandlers() {
        parentInvitesList.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> {
            boolean selected = n != null;
            acceptParentInviteButton.setDisable(!selected);
            declineParentInviteButton.setDisable(!selected);
        });
        groupInvitesList.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> {
            boolean selected = n != null;
            acceptGroupInviteButton.setDisable(!selected);
            declineGroupInviteButton.setDisable(!selected);
        });
        financialAlertsList.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> {
            boolean selected = n != null;
            dismissAlertButton.setDisable(!selected);
        });

        acceptParentInviteButton.setOnAction(e -> {
            String key = parentInvitesList.getSelectionModel().getSelectedItem();
            if (key == null) return;
            String inviteId = parentInviteMap.get(key);

            acceptParentInviteButton.setDisable(true);
            declineParentInviteButton.setDisable(true);

            org.example.util.ThreadPoolManager.getInstance().executeDatabaseWithCallback(
                () -> InviteService.acceptParentInvite(inviteId),
                success -> {
                    if (success) {
                        loadInvitesAndAlerts();
                    } else {
                        acceptParentInviteButton.setDisable(false);
                        declineParentInviteButton.setDisable(false);
                    }
                },
                error -> {
                    error.printStackTrace();
                    acceptParentInviteButton.setDisable(false);
                    declineParentInviteButton.setDisable(false);
                }
            );
        });
        declineParentInviteButton.setOnAction(e -> {
            String key = parentInvitesList.getSelectionModel().getSelectedItem();
            if (key == null) return;
            String inviteId = parentInviteMap.get(key);

            acceptParentInviteButton.setDisable(true);
            declineParentInviteButton.setDisable(true);

            org.example.util.ThreadPoolManager.getInstance().executeDatabaseWithCallback(
                () -> InviteService.declineParentInvite(inviteId),
                success -> {
                    if (success) {
                        loadInvitesAndAlerts();
                    } else {
                        acceptParentInviteButton.setDisable(false);
                        declineParentInviteButton.setDisable(false);
                    }
                },
                error -> {
                    error.printStackTrace();
                    acceptParentInviteButton.setDisable(false);
                    declineParentInviteButton.setDisable(false);
                }
            );
        });
        acceptGroupInviteButton.setOnAction(e -> {
            String key = groupInvitesList.getSelectionModel().getSelectedItem();
            if (key == null) return;
            String inviteId = groupInviteMap.get(key);

            acceptGroupInviteButton.setDisable(true);
            declineGroupInviteButton.setDisable(true);

            org.example.util.ThreadPoolManager.getInstance().executeDatabaseWithCallback(
                () -> InviteService.acceptGroupInvite(inviteId),
                success -> {
                    if (success) {
                        loadInvitesAndAlerts();
                    } else {
                        acceptGroupInviteButton.setDisable(false);
                        declineGroupInviteButton.setDisable(false);
                    }
                },
                error -> {
                    error.printStackTrace();
                    acceptGroupInviteButton.setDisable(false);
                    declineGroupInviteButton.setDisable(false);
                }
            );
        });
        declineGroupInviteButton.setOnAction(e -> {
            String key = groupInvitesList.getSelectionModel().getSelectedItem();
            if (key == null) return;
            String inviteId = groupInviteMap.get(key);

            acceptGroupInviteButton.setDisable(true);
            declineGroupInviteButton.setDisable(true);

            org.example.util.ThreadPoolManager.getInstance().executeDatabaseWithCallback(
                () -> InviteService.declineGroupInvite(inviteId),
                success -> {
                    if (success) {
                        loadInvitesAndAlerts();
                    } else {
                        acceptGroupInviteButton.setDisable(false);
                        declineGroupInviteButton.setDisable(false);
                    }
                },
                error -> {
                    error.printStackTrace();
                    acceptGroupInviteButton.setDisable(false);
                    declineGroupInviteButton.setDisable(false);
                }
            );
        });
        dismissAlertButton.setOnAction(e -> {
            // For now just remove the selected alert locally
            String key = financialAlertsList.getSelectionModel().getSelectedItem();
            if (key == null) return;
            financialAlerts.remove(key);
            financialAlertsList.setItems(financialAlerts);
            dismissAlertButton.setDisable(true);
        });
    }
}
