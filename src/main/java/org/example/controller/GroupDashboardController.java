package org.example.controller;

import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.example.model.Expense;
import org.example.model.GroupMember;
import org.example.service.ExpenseService;
import org.example.service.GroupService;

import java.io.IOException;

/**
 * Group Dashboard Controller
 * Displays group information, members, and expenses
 */
public class GroupDashboardController {

    @FXML
    private Label groupNameLabel;

    @FXML
    private Label memberCountLabel;

    @FXML
    private Label memberCountStatLabel;

    @FXML
    private Label totalExpensesLabel;

    @FXML
    private Label monthExpensesLabel;

    @FXML
    private TableView<Expense> expensesTable;

    @FXML
    private TableColumn<Expense, String> dateColumn;

    @FXML
    private TableColumn<Expense, String> memberColumn;

    @FXML
    private TableColumn<Expense, String> categoryColumn;

    @FXML
    private TableColumn<Expense, Double> amountColumn;

    @FXML
    private TableColumn<Expense, String> noteColumn;

    private String currentGroupId;
    private String currentUserId;
    private String currentGroupName;

    @FXML
    public void initialize() {
        setupTableColumns();
    }

    /**
     * Initialize with group data
     */
    public void initWithGroup(String groupId, String userId, String groupName) {
        System.out.println("=== GroupDashboardController Initialization ===");
        this.currentGroupId = groupId;
        this.currentUserId = userId;
        this.currentGroupName = groupName;

        System.out.println("Group ID: " + groupId);
        System.out.println("User ID: " + userId);
        System.out.println("Group Name: " + groupName);

        // Set UI labels
        groupNameLabel.setText(groupName);

        // Load and display data
        loadMembersCount();
        loadExpenses();

        System.out.println("=== Initialization Complete ===");
    }

    /**
     * Backward compatibility method - calls initWithGroup
     */
    @Deprecated
    public void initializeWithGroup(String groupId, String userId) {
        initWithGroup(groupId, userId, "Group");
    }

    /**
     * Setup table columns with cell value factories
     */
    private void setupTableColumns() {
        dateColumn.setCellValueFactory(cellData ->
            new javafx.beans.property.SimpleStringProperty(cellData.getValue().getDate()));

        memberColumn.setCellValueFactory(cellData ->
            new javafx.beans.property.SimpleStringProperty(cellData.getValue().getUserId()));

        categoryColumn.setCellValueFactory(cellData ->
            new javafx.beans.property.SimpleStringProperty(cellData.getValue().getCategory()));

        amountColumn.setCellValueFactory(cellData ->
            new javafx.beans.property.SimpleDoubleProperty(cellData.getValue().getAmount()).asObject());

        noteColumn.setCellValueFactory(cellData ->
            new javafx.beans.property.SimpleStringProperty(cellData.getValue().getNote()));
    }

    /**
     * Load and display member count
     */
    private void loadMembersCount() {
        if (currentGroupId == null || currentGroupId.isEmpty()) {
            memberCountLabel.setText("Members: 0");
            if (memberCountStatLabel != null) {
                memberCountStatLabel.setText("0");
            }
            return;
        }

        ObservableList<GroupMember> members = GroupService.getGroupMembersObservable(currentGroupId);
        int count = members.size();
        memberCountLabel.setText("Members: " + count);
        if (memberCountStatLabel != null) {
            memberCountStatLabel.setText(String.valueOf(count));
        }

        System.out.println("Loaded " + count + " members for group: " + currentGroupName);
    }

    /**
     * Load and display expenses
     */
    private void loadExpenses() {
        if (currentGroupId == null || currentGroupId.isEmpty()) {
            expensesTable.setItems(null);
            updateExpensesSummary(0, 0);
            return;
        }

        ObservableList<Expense> expenses = ExpenseService.getGroupExpensesObservable(currentGroupId);
        expensesTable.setItems(expenses);

        // Calculate totals
        double total = 0;
        double monthTotal = 0;
        for (Expense exp : expenses) {
            total += exp.getAmount();
            if (isCurrentMonth(exp.getDate())) {
                monthTotal += exp.getAmount();
            }
        }

        updateExpensesSummary(total, monthTotal);

        System.out.println("Loaded " + expenses.size() + " expenses for group: " + currentGroupName);
    }

    /**
     * Check if date is in current month
     */
    private boolean isCurrentMonth(String dateStr) {
        try {
            java.time.LocalDate date = java.time.LocalDate.parse(dateStr);
            java.time.LocalDate today = java.time.LocalDate.now();
            return date.getYear() == today.getYear() && date.getMonth() == today.getMonth();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Update expenses summary labels
     */
    private void updateExpensesSummary(double total, double monthTotal) {
        totalExpensesLabel.setText(String.format("$%.2f", total));
        monthExpensesLabel.setText(String.format("$%.2f", monthTotal));
    }

    /**
     * Handle Add Member button click
     */
    @FXML
    private void handleAddMember() {
        if (currentGroupId == null) {
            showError("No group selected!");
            return;
        }

        try {
            System.out.println("Opening Add Member dialog for group: " + currentGroupName);

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/add_member.fxml"));
            Stage dialog = new Stage();
            dialog.setTitle("Add Member to " + currentGroupName);
            Scene scene = new Scene(loader.load());
            dialog.setScene(scene);
            dialog.initModality(Modality.WINDOW_MODAL);
            dialog.setResizable(false);
            dialog.setWidth(700);
            dialog.setHeight(500);

            // Initialize controller
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

            // Refresh after dialog closes
            System.out.println("Refreshing dashboard after add member dialog");
            loadMembersCount();
            loadExpenses();

        } catch (IOException e) {
            e.printStackTrace();
            showError("Failed to open add member dialog: " + e.getMessage());
        }
    }

    /**
     * Handle View Members button click
     */
    @FXML
    private void handleViewMembers() {
        if (currentGroupId == null) {
            showError("No group selected!");
            return;
        }

        try {
            System.out.println("Opening View Members for group: " + currentGroupName);

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/view_members.fxml"));
            Scene scene = new Scene(loader.load());

            // Initialize controller using reflection
            Object controllerObj = loader.getController();
            if (controllerObj != null) {
                try {
                    var method = controllerObj.getClass().getMethod("initWithGroup", String.class, String.class, String.class);
                    method.invoke(controllerObj, currentGroupId, currentUserId, currentGroupName);
                    System.out.println("View Members controller initialized");
                } catch (Exception ex) {
                    System.err.println("Error initializing View Members: " + ex.getMessage());
                }
            }

            Stage stage = (Stage) groupNameLabel.getScene().getWindow();
            stage.setScene(scene);

        } catch (IOException e) {
            e.printStackTrace();
            showError("Failed to open members view: " + e.getMessage());
        }
    }

    /**
     * Handle Add Expense button click
     */
    @FXML
    private void handleAddExpense() {
        showError("Add expense feature coming soon");
    }

    /**
     * Handle Back button click
     */
    @FXML
    private void handleBack() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/group_selection.fxml"));
            Scene scene = new Scene(loader.load());

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
}

