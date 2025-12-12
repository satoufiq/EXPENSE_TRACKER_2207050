package org.example.controller;

import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import org.example.model.Expense;
import org.example.service.ExpenseService;

/**
 * Personal Dashboard Controller with ObservableList for real-time updates
 * No refresh button needed - UI updates automatically when data changes
 */
public class PersonalDashboardController {

    @FXML
    private Label userNameLabel;

    @FXML
    private Label modeLabel;

    @FXML
    private Label totalExpensesLabel;

    @FXML
    private Label monthExpensesLabel;

    @FXML
    private Label weekExpensesLabel;

    @FXML
    private Label transactionCountLabel;

    @FXML
    private TableView<Expense> expensesTable;

    @FXML
    private TableColumn<Expense, String> dateColumn;

    @FXML
    private TableColumn<Expense, String> categoryColumn;

    @FXML
    private TableColumn<Expense, Double> amountColumn;

    @FXML
    private TableColumn<Expense, String> noteColumn;

    @FXML
    private TableColumn<Expense, String> actionsColumn;

    @FXML
    private Button addExpenseButton;

    @FXML
    private Button viewAnalyticsButton;

    @FXML
    private Button viewAllButton;

    @FXML
    private Button backButton;

    private ObservableList<Expense> expensesList;
    private String currentUserId;

    @FXML
    public void initialize() {
        // Get current user from session
        org.example.util.SessionManager session = org.example.util.SessionManager.getInstance();
        if (session.getCurrentUser() != null) {
            this.currentUserId = session.getCurrentUser().getUserId();

            // Setup table and load data
            setupTableColumns();
            loadExpenses();
        }
    }

    /**
     * Initialize with user data and ObservableList
     */
    public void initializeWithUser(String userId) {
        this.currentUserId = userId;
        setupTableColumns();
        loadExpenses();
    }

    /**
     * Setup table columns with proper cell value factories
     */
    private void setupTableColumns() {
        if (dateColumn == null || categoryColumn == null || amountColumn == null ||
            noteColumn == null || actionsColumn == null) {
            return;
        }

        dateColumn.setCellValueFactory(cellData ->
            new javafx.beans.property.SimpleStringProperty(cellData.getValue().getDate()));

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

        // Add action buttons in the actions column
        actionsColumn.setCellFactory(column -> new ActionButtonCell());
    }

    /**
     * Load expenses using ObservableList - automatically updates UI on changes
     */
    private void loadExpenses() {
        if (currentUserId == null || currentUserId.isEmpty()) {
            return;
        }

        // Get ObservableList from service - UI will automatically update when list changes
        expensesList = ExpenseService.getPersonalExpensesObservable(currentUserId);
        expensesTable.setItems(expensesList);

        // Update statistics
        updateStatistics();

        // Listen for changes in the observable list
        expensesList.addListener((javafx.collections.ListChangeListener<Expense>) change -> {
            updateStatistics();
        });
    }

    /**
     * Update dashboard statistics
     */
    private void updateStatistics() {
        if (currentUserId == null || expensesList == null) return;

        double total = ExpenseService.getTotalExpenses(currentUserId);
        if (totalExpensesLabel != null) {
            totalExpensesLabel.setText(String.format("৳%.2f", total));
        }

        double monthTotal = calculateMonthTotal();
        if (monthExpensesLabel != null) {
            monthExpensesLabel.setText(String.format("৳%.2f", monthTotal));
        }

        double weekTotal = calculateWeekTotal();
        if (weekExpensesLabel != null) {
            weekExpensesLabel.setText(String.format("৳%.2f", weekTotal));
        }

        if (transactionCountLabel != null) {
            transactionCountLabel.setText(String.valueOf(expensesList.size()));
        }
    }

    /**
     * Calculate total expenses for current month
     */
    private double calculateMonthTotal() {
        if (expensesList == null) return 0.0;
        return expensesList.stream()
            .filter(e -> isCurrentMonth(e.getDate()))
            .mapToDouble(Expense::getAmount)
            .sum();
    }

    /**
     * Calculate total expenses for current week
     */
    private double calculateWeekTotal() {
        if (expensesList == null) return 0.0;
        return expensesList.stream()
            .filter(e -> isCurrentWeek(e.getDate()))
            .mapToDouble(Expense::getAmount)
            .sum();
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
     * Check if date is in current week
     */
    private boolean isCurrentWeek(String dateStr) {
        try {
            java.time.LocalDate date = java.time.LocalDate.parse(dateStr);
            java.time.LocalDate today = java.time.LocalDate.now();
            java.time.temporal.WeekFields weekFields = java.time.temporal.WeekFields.of(java.util.Locale.US);
            return date.get(weekFields.weekOfYear()) == today.get(weekFields.weekOfYear()) &&
                   date.getYear() == today.getYear();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Handle add expense button click - open add expense dialog
     */
    @FXML
    private void handleAddExpense() {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/fxml/add_expense.fxml"));
            javafx.scene.Parent root = loader.load();

            javafx.stage.Stage stage = new javafx.stage.Stage();
            stage.setTitle("Add Expense");
            javafx.scene.Scene scene = new javafx.scene.Scene(root);
            scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
            stage.setScene(scene);
            stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            stage.setResizable(false);
            stage.showAndWait();

            // ObservableList will automatically update the UI when dialog closes
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(javafx.scene.control.Alert.AlertType.ERROR, "Error",
                "Failed to open add expense dialog: " + e.getMessage());
        }
    }

    /**
     * Show alert dialog
     */
    private void showAlert(javafx.scene.control.Alert.AlertType alertType, String title, String message) {
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Inner class for action buttons in table
     */
    private class ActionButtonCell extends TableCell<Expense, String> {
        private final Button editButton = new Button("Edit");
        private final Button deleteButton = new Button("Delete");

        public ActionButtonCell() {
            editButton.setStyle("-fx-padding: 5; -fx-font-size: 11;");
            deleteButton.setStyle("-fx-padding: 5; -fx-font-size: 11;");

            editButton.setOnAction(event -> {
                Expense expense = getTableView().getItems().get(getIndex());
                handleEditExpense(expense);
            });

            deleteButton.setOnAction(event -> {
                Expense expense = getTableView().getItems().get(getIndex());
                handleDeleteExpense(expense);
            });
        }

        @Override
        protected void updateItem(String item, boolean empty) {
            super.updateItem(item, empty);
            if (empty) {
                setGraphic(null);
            } else {
                javafx.scene.layout.HBox hbox = new javafx.scene.layout.HBox(5);
                hbox.getChildren().addAll(editButton, deleteButton);
                setGraphic(hbox);
            }
        }
    }

    /**
     * Handle edit expense action
     */
    private void handleEditExpense(Expense expense) {
        // Open edit dialog
        // ObservableList will auto-update when edit is saved
    }

    /**
     * Handle delete expense action
     */
    private void handleDeleteExpense(Expense expense) {
        if (expense != null) {
            // Delete from database - observable list will auto-update
            ExpenseService.deleteExpense(expense.getExpenseId());
            // UI automatically updates because we're using ObservableList!
        }
    }

    /**
     * Handle back button click - navigate back to mode selection
     */
    @FXML
    private void handleBack() {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/fxml/mode_selection.fxml"));
            javafx.scene.Parent root = loader.load();
            javafx.scene.Scene scene = new javafx.scene.Scene(root);
            scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());

            javafx.stage.Stage stage = (javafx.stage.Stage) backButton.getScene().getWindow();
            stage.setScene(scene);
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Failed to navigate back: " + e.getMessage());
        }
    }

    /**
     * Handle view analytics button click - navigate to analytics view
     */
    @FXML
    private void handleViewAnalytics() {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/fxml/personal_analytics.fxml"));
            javafx.scene.Parent root = loader.load();
            javafx.scene.Scene scene = new javafx.scene.Scene(root);
            scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
            javafx.stage.Stage stage = (javafx.stage.Stage) viewAnalyticsButton.getScene().getWindow();
            stage.setScene(scene);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
