package org.example.controller;

import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.example.model.Expense;
import org.example.service.ExpenseService;
import org.example.service.GroupBudgetService;
import org.example.service.GroupService;
import org.example.service.UserService;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

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

    @FXML
    private TableColumn<Expense, Void> actionsColumn;

    @FXML
    private Label groupBudgetLabel;

    // Cache userId -> name to avoid repeated DB lookups
    private final Map<String, String> userNameCache = new HashMap<>();

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

        // Show current group budget
        double budget = GroupBudgetService.getMonthlyBudget(groupId);
        if (groupBudgetLabel != null) {
            groupBudgetLabel.setText(String.format("৳%.2f", budget));
        }

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

        // Resolve member name from userId with a small cache
        memberColumn.setCellValueFactory(cellData -> {
            String userId = cellData.getValue().getUserId();
            if (userId == null) {
                return new javafx.beans.property.SimpleStringProperty("");
            }
            String name = userNameCache.computeIfAbsent(userId, id -> {
                var user = UserService.getUserById(id);
                return user != null ? user.getName() : id;
            });
            return new javafx.beans.property.SimpleStringProperty(name);
        });

        categoryColumn.setCellValueFactory(cellData ->
            new javafx.beans.property.SimpleStringProperty(cellData.getValue().getCategory()));

        amountColumn.setCellValueFactory(cellData ->
            new javafx.beans.property.SimpleDoubleProperty(cellData.getValue().getAmount()).asObject());

        // Format amount column to show Taka currency
        amountColumn.setCellFactory(column -> new javafx.scene.control.TableCell<Expense, Double>() {
            @Override
            protected void updateItem(Double amount, boolean empty) {
                super.updateItem(amount, empty);
                if (empty || amount == null) {
                    setText(null);
                } else {
                    setText(String.format("৳%.2f", amount));
                }
            }
        });

        noteColumn.setCellValueFactory(cellData ->
            new javafx.beans.property.SimpleStringProperty(cellData.getValue().getNote()));

        // Setup actions column with Edit and Delete buttons
        actionsColumn.setCellFactory(column -> new TableCell<Expense, Void>() {
            private final Button editButton = new Button("✏️ Edit");
            private final Button deleteButton = new Button("🗑️ Delete");
            private final javafx.scene.layout.HBox buttonBox = new javafx.scene.layout.HBox(8);

            {
                editButton.getStyleClass().add("action-button-edit");
                deleteButton.getStyleClass().add("action-button-delete");
                editButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-cursor: hand; -fx-font-size: 11px; -fx-padding: 5 10;");
                deleteButton.setStyle("-fx-background-color: #f44336; -fx-text-fill: white; -fx-cursor: hand; -fx-font-size: 11px; -fx-padding: 5 10;");

                editButton.setOnAction(event -> {
                    Expense expense = getTableView().getItems().get(getIndex());
                    handleEditExpense(expense);
                });

                deleteButton.setOnAction(event -> {
                    Expense expense = getTableView().getItems().get(getIndex());
                    handleDeleteExpense(expense);
                });

                buttonBox.getChildren().addAll(editButton, deleteButton);
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(buttonBox);
                }
            }
        });
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

        // Load members asynchronously
        org.example.util.ThreadPoolManager.getInstance().executeDatabaseWithCallback(
            () -> GroupService.getGroupMembersObservable(currentGroupId),
            members -> {
                int count = members.size();
                memberCountLabel.setText("Members: " + count);
                if (memberCountStatLabel != null) {
                    memberCountStatLabel.setText(String.valueOf(count));
                }
                System.out.println("Loaded " + count + " members for group: " + currentGroupName);
            },
            error -> {
                System.err.println("Error loading members: " + error.getMessage());
                error.printStackTrace();
            }
        );
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

        // Load expenses asynchronously
        org.example.util.ThreadPoolManager.getInstance().executeDatabaseWithCallback(
            () -> {
                ObservableList<Expense> expenses = ExpenseService.getGroupExpensesObservable(currentGroupId);

                // Calculate totals in background thread
                double total = 0;
                double monthTotal = 0;
                for (Expense exp : expenses) {
                    total += exp.getAmount();
                    if (isCurrentMonth(exp.getDate())) {
                        monthTotal += exp.getAmount();
                    }
                }

                return new Object[]{expenses, total, monthTotal};
            },
            result -> {
                Object[] data = (Object[]) result;
                @SuppressWarnings("unchecked")
                ObservableList<Expense> expenses = (ObservableList<Expense>) data[0];
                double total = (double) data[1];
                double monthTotal = (double) data[2];

                expensesTable.setItems(expenses);
                updateExpensesSummary(total, monthTotal);
                System.out.println("Loaded " + expenses.size() + " expenses for group: " + currentGroupName);
            },
            error -> {
                System.err.println("Error loading expenses: " + error.getMessage());
                error.printStackTrace();
                expensesTable.setItems(null);
                updateExpensesSummary(0, 0);
            }
        );
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
        totalExpensesLabel.setText(String.format("৳%.2f", total));
        monthExpensesLabel.setText(String.format("৳%.2f", monthTotal));
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
        if (currentGroupId == null) {
            showError("No group selected!");
            return;
        }

        try {
            System.out.println("Opening Add Expense dialog for group: " + currentGroupName);

            // Set the current group in session so AddExpenseController knows it's group mode
            org.example.util.SessionManager.getInstance().setCurrentGroupId(currentGroupId);

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/add_expense.fxml"));
            javafx.scene.Parent root = loader.load();

            Stage stage = new Stage();
            stage.setTitle("Add Expense - " + currentGroupName);
            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
            stage.setScene(scene);
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setResizable(false);
            stage.showAndWait();

            // Refresh after dialog closes
            System.out.println("Refreshing dashboard after add expense dialog");
            loadExpenses();

        } catch (IOException e) {
            e.printStackTrace();
            showError("Failed to open add expense dialog: " + e.getMessage());
        }
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

    @FXML
    private void handleOpenGroupAnalytics() {
        if (currentGroupId == null) { showError("No group selected!"); return; }
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/group_analytics.fxml"));
            javafx.scene.Parent root = loader.load();
            // Initialize controller with group context if method exists
            Object controller = loader.getController();
            if (controller != null) {
                try {
                    var m = controller.getClass().getMethod("initWithGroup", String.class, String.class, String.class);
                    m.invoke(controller, currentGroupId, currentUserId, currentGroupName);
                } catch (NoSuchMethodException ignored) {}
            }
            Stage stage = (Stage) groupNameLabel.getScene().getWindow();
            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
            stage.setScene(scene);
        } catch (Exception e) {
            e.printStackTrace();
            showError("Failed to open Group Analytics: " + e.getMessage());
        }
    }

    @FXML
    private void handleOpenGroupVisualAnalytics() {
        if (currentGroupId == null) { showError("No group selected!"); return; }
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/group_visual_analytics.fxml"));
            javafx.scene.Parent root = loader.load();
            Object controller = loader.getController();
            if (controller != null) {
                try {
                    var m = controller.getClass().getMethod("initWithGroup", String.class, String.class, String.class);
                    m.invoke(controller, currentGroupId, currentUserId, currentGroupName);
                } catch (NoSuchMethodException ignored) {}
            }
            Stage stage = (Stage) groupNameLabel.getScene().getWindow();
            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
            stage.setScene(scene);
        } catch (Exception e) {
            e.printStackTrace();
            showError("Failed to open Group Visual Analytics: " + e.getMessage());
        }
    }

    @FXML
    private void handleOpenMemberAnalytics() {
        if (currentGroupId == null) { showError("No group selected!"); return; }
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/group_member_analytics.fxml"));
            javafx.scene.Parent root = loader.load();
            Object controller = loader.getController();
            if (controller != null) {
                try {
                    var m = controller.getClass().getMethod("initWithGroup", String.class, String.class, String.class);
                    m.invoke(controller, currentGroupId, currentUserId, currentGroupName);
                } catch (NoSuchMethodException ignored) {}
            }
            Stage stage = (Stage) groupNameLabel.getScene().getWindow();
            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
            stage.setScene(scene);
        } catch (Exception e) {
            e.printStackTrace();
            showError("Failed to open Member Analytics: " + e.getMessage());
        }
    }

    @FXML
    private void handleSetGroupBudget() {
        if (currentGroupId == null || currentGroupId.isEmpty()) { showError("No group selected!"); return; }
        javafx.scene.control.TextInputDialog dialog = new javafx.scene.control.TextInputDialog();
        dialog.setTitle("Set Group Monthly Budget");
        dialog.setHeaderText("Set Monthly Budget for " + currentGroupName);
        dialog.setContentText("Enter budget amount in ৳ (BDT):");
        double current = GroupBudgetService.getMonthlyBudget(currentGroupId);
        if (current > 0) dialog.getEditor().setText(String.format("%.2f", current));
        java.util.Optional<String> result = dialog.showAndWait();
        result.ifPresent(val -> {
            try {
                double amt = Double.parseDouble(val.trim());
                if (amt < 0) { showError("Budget amount cannot be negative."); return; }
                GroupBudgetService.setMonthlyBudget(currentGroupId, amt);
                if (groupBudgetLabel != null) groupBudgetLabel.setText(String.format("৳%.2f", amt));
                org.example.util.AlertUtil.showInfo("Budget Set", String.format("Group budget set to ৳%.2f", amt));
            } catch (NumberFormatException ex) {
                showError("Please enter a valid number.");
            }
        });
    }

    /**
     * Handle edit expense
     */
    private void handleEditExpense(Expense expense) {
        try {
            // Create edit dialog
            javafx.scene.control.Dialog<javafx.util.Pair<String, javafx.util.Pair<String, javafx.util.Pair<Double, String>>>> dialog = new javafx.scene.control.Dialog<>();
            dialog.setTitle("Edit Expense");
            dialog.setHeaderText("Edit expense details");

            // Set button types
            javafx.scene.control.ButtonType saveButtonType = new javafx.scene.control.ButtonType("Save", javafx.scene.control.ButtonBar.ButtonData.OK_DONE);
            dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, javafx.scene.control.ButtonType.CANCEL);

            // Create form fields
            javafx.scene.layout.GridPane grid = new javafx.scene.layout.GridPane();
            grid.setHgap(10);
            grid.setVgap(10);
            grid.setPadding(new javafx.geometry.Insets(20, 150, 10, 10));

            javafx.scene.control.ComboBox<String> categoryCombo = new javafx.scene.control.ComboBox<>();
            categoryCombo.getItems().addAll("Food", "Transport", "Entertainment", "Shopping", "Bills", "Healthcare", "Education", "Other");
            categoryCombo.setValue(expense.getCategory());

            javafx.scene.control.TextField amountField = new javafx.scene.control.TextField(String.valueOf(expense.getAmount()));
            javafx.scene.control.DatePicker datePicker = new javafx.scene.control.DatePicker(java.time.LocalDate.parse(expense.getDate()));
            javafx.scene.control.TextField noteField = new javafx.scene.control.TextField(expense.getNote() != null ? expense.getNote() : "");

            grid.add(new javafx.scene.control.Label("Category:"), 0, 0);
            grid.add(categoryCombo, 1, 0);
            grid.add(new javafx.scene.control.Label("Amount (৳):"), 0, 1);
            grid.add(amountField, 1, 1);
            grid.add(new javafx.scene.control.Label("Date:"), 0, 2);
            grid.add(datePicker, 1, 2);
            grid.add(new javafx.scene.control.Label("Note:"), 0, 3);
            grid.add(noteField, 1, 3);

            dialog.getDialogPane().setContent(grid);

            // Convert result
            dialog.setResultConverter(dialogButton -> {
                if (dialogButton == saveButtonType) {
                    return new javafx.util.Pair<>(categoryCombo.getValue(),
                            new javafx.util.Pair<>(datePicker.getValue().toString(),
                                    new javafx.util.Pair<>(Double.parseDouble(amountField.getText()), noteField.getText())));
                }
                return null;
            });

            java.util.Optional<javafx.util.Pair<String, javafx.util.Pair<String, javafx.util.Pair<Double, String>>>> result = dialog.showAndWait();

            result.ifPresent(data -> {
                String category = data.getKey();
                String date = data.getValue().getKey();
                Double amount = data.getValue().getValue().getKey();
                String note = data.getValue().getValue().getValue();

                // Update expense
                boolean success = ExpenseService.updateExpense(expense.getExpenseId(), category, amount, date, note);

                if (success) {
                    org.example.util.AlertUtil.showInfo("Success", "Expense updated successfully!");
                    loadExpenses(); // Refresh table
                } else {
                    showError("Failed to update expense");
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
            showError("Error editing expense: " + e.getMessage());
        }
    }

    /**
     * Handle delete expense
     */
    private void handleDeleteExpense(Expense expense) {
        try {
            // Confirm deletion
            javafx.scene.control.Alert confirmAlert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.CONFIRMATION);
            confirmAlert.setTitle("Confirm Delete");
            confirmAlert.setHeaderText("Delete Expense");
            confirmAlert.setContentText("Are you sure you want to delete this expense?\n\n" +
                    "Category: " + expense.getCategory() + "\n" +
                    "Amount: ৳" + String.format("%.2f", expense.getAmount()) + "\n" +
                    "Date: " + expense.getDate());

            java.util.Optional<javafx.scene.control.ButtonType> result = confirmAlert.showAndWait();

            if (result.isPresent() && result.get() == javafx.scene.control.ButtonType.OK) {
                boolean success = ExpenseService.deleteExpense(expense.getExpenseId());

                if (success) {
                    org.example.util.AlertUtil.showInfo("Success", "Expense deleted successfully!");
                    loadExpenses(); // Refresh table
                } else {
                    showError("Failed to delete expense");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            showError("Error deleting expense: " + e.getMessage());
        }
    }
}
