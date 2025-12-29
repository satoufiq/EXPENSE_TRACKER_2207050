package org.example.controller;

import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.example.model.Expense;
import org.example.service.ExpenseService;

@SuppressWarnings({"unused", "FieldCanBeLocal"})
public class PersonalDashboardController {

    @FXML private Label userNameLabel;
    @FXML private Label modeLabel;
    @FXML private Label totalExpensesLabel;
    @FXML private Label monthExpensesLabel;
    @FXML private Label weekExpensesLabel;
    @FXML private Label transactionCountLabel;
    @FXML private FlowPane expenseCardsPane;
    @FXML private VBox emptyStatePane;
    @FXML private Button addExpenseButton;
    @FXML private Button viewAnalyticsButton;
    @FXML private Button viewAllButton;
    @FXML private Button backButton;
    @FXML private Button sendToParentButton;
    @FXML private Button alertButton;
    @FXML private Label alertBadge;
    @FXML private HBox alertButtonContainer;

    private ObservableList<Expense> expensesList;
    private String currentUserId;

    @FXML
    public void initialize() {
        org.example.util.SessionManager session = org.example.util.SessionManager.getInstance();
        if (session.getCurrentUser() != null) {
            this.currentUserId = session.getCurrentUser().getUserId();
            loadExpenses();
            updateAlertCount();
        }
    }

    public void initializeWithUser(String oderId) {
        this.currentUserId = oderId;
        loadExpenses();
    }

    private void updateAlertCount() {
        if (alertBadge != null && alertButton != null) {
            int count = org.example.service.ParentChildAlertService.getUnreadAlertCount(currentUserId);
            count += org.example.service.InviteService.getPendingParentInvitesForChild(currentUserId).size();
            count += org.example.service.InviteService.getPendingGroupInvitesForUser(currentUserId).size();

            if (count > 0) {
                alertBadge.setText(String.valueOf(count));
                alertBadge.setVisible(true);
                alertBadge.setManaged(true);
                alertButton.getStyleClass().add("alert-button-active");
            } else {
                alertBadge.setVisible(false);
                alertBadge.setManaged(false);
                alertButton.getStyleClass().remove("alert-button-active");
            }
        }
    }

    @FXML
    private void handleViewAlerts() {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/fxml/alerts.fxml"));
            javafx.scene.Parent root = loader.load();
            javafx.scene.Scene scene = new javafx.scene.Scene(root);
            var styleResource = getClass().getResource("/css/styles.css");
            if (styleResource != null) {
                scene.getStylesheets().add(styleResource.toExternalForm());
            }
            javafx.stage.Stage stage = (javafx.stage.Stage) alertButton.getScene().getWindow();
            stage.setScene(scene);
        } catch (Exception e) {
            System.err.println("Failed to navigate to alerts: " + e.getMessage());
        }
    }

    private void loadExpenses() {
        if (currentUserId == null || currentUserId.isEmpty()) {
            return;
        }

        expensesList = ExpenseService.getPersonalExpensesObservable(currentUserId);
        refreshExpenseCards();
        updateStatistics();

        // Listen for changes
        expensesList.addListener((ListChangeListener<Expense>) change -> {
            refreshExpenseCards();
            updateStatistics();
        });
    }

    private void refreshExpenseCards() {
        if (expenseCardsPane == null) return;

        expenseCardsPane.getChildren().clear();

        if (expensesList == null || expensesList.isEmpty()) {
            if (emptyStatePane != null) {
                emptyStatePane.setVisible(true);
                emptyStatePane.setManaged(true);
            }
            return;
        }

        if (emptyStatePane != null) {
            emptyStatePane.setVisible(false);
            emptyStatePane.setManaged(false);
        }

        // Show only recent 12 expenses
        int limit = Math.min(12, expensesList.size());
        for (int i = 0; i < limit; i++) {
            Expense expense = expensesList.get(i);
            expenseCardsPane.getChildren().add(createExpenseCard(expense));
        }
    }

    private VBox createExpenseCard(Expense expense) {
        VBox card = new VBox(10);
        card.getStyleClass().add("expense-card");
        card.setPrefWidth(340);
        card.setMinWidth(340);
        card.setMaxWidth(340);

        // Top row: Category badge and date
        HBox topRow = new HBox(10);
        topRow.setAlignment(Pos.CENTER_LEFT);

        Label categoryBadge = new Label(getCategoryEmoji(expense.getCategory()) + " " + expense.getCategory());
        categoryBadge.getStyleClass().add("expense-category-badge");

        Label dateLabel = new Label("📅 " + expense.getDate());
        dateLabel.getStyleClass().add("expense-date");

        topRow.getChildren().addAll(categoryBadge, dateLabel);
        HBox.setMargin(dateLabel, new Insets(0, 0, 0, 10));

        // Amount
        Label amountLabel = new Label(String.format("৳%.2f", expense.getAmount()));
        amountLabel.getStyleClass().add("expense-amount");

        // Note
        String noteText = expense.getNote() != null && !expense.getNote().isEmpty()
            ? expense.getNote()
            : "No description";
        Label noteLabel = new Label(noteText);
        noteLabel.getStyleClass().add("expense-note");
        noteLabel.setWrapText(true);
        noteLabel.setMaxWidth(300);

        // Action buttons
        HBox actionsRow = new HBox(10);
        actionsRow.setAlignment(Pos.CENTER_RIGHT);

        Button editBtn = new Button("✏️ Edit");
        editBtn.getStyleClass().addAll("card-action-button", "card-edit-button");
        editBtn.setOnAction(e -> handleEditExpense(expense));

        Button deleteBtn = new Button("🗑️ Delete");
        deleteBtn.getStyleClass().addAll("card-action-button", "card-delete-button");
        deleteBtn.setOnAction(e -> handleDeleteExpense(expense));

        actionsRow.getChildren().addAll(editBtn, deleteBtn);

        card.getChildren().addAll(topRow, amountLabel, noteLabel, actionsRow);

        return card;
    }

    private String getCategoryEmoji(String category) {
        if (category == null) return "📦";
        switch (category.toLowerCase()) {
            case "food": return "🍔";
            case "transport": return "🚗";
            case "shopping": return "🛒";
            case "entertainment": return "🎬";
            case "bills": return "📄";
            case "health": return "💊";
            case "education": return "📚";
            case "groceries": return "🥬";
            case "utilities": return "💡";
            case "rent": return "🏠";
            default: return "📦";
        }
    }

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

    private double calculateMonthTotal() {
        if (expensesList == null) return 0.0;
        return expensesList.stream()
            .filter(e -> isCurrentMonth(e.getDate()))
            .mapToDouble(Expense::getAmount)
            .sum();
    }

    private double calculateWeekTotal() {
        if (expensesList == null) return 0.0;
        return expensesList.stream()
            .filter(e -> isCurrentWeek(e.getDate()))
            .mapToDouble(Expense::getAmount)
            .sum();
    }

    private boolean isCurrentMonth(String dateStr) {
        try {
            java.time.LocalDate date = java.time.LocalDate.parse(dateStr);
            java.time.LocalDate today = java.time.LocalDate.now();
            return date.getYear() == today.getYear() && date.getMonth() == today.getMonth();
        } catch (Exception e) {
            return false;
        }
    }

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

    @FXML
    private void handleAddExpense() {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/fxml/add_expense.fxml"));
            javafx.scene.Parent root = loader.load();

            javafx.stage.Stage stage = new javafx.stage.Stage();
            stage.setTitle("Add Expense");
            javafx.scene.Scene scene = new javafx.scene.Scene(root);
            var styleResource = getClass().getResource("/css/styles.css");
            if (styleResource != null) {
                scene.getStylesheets().add(styleResource.toExternalForm());
            }
            stage.setScene(scene);
            stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            stage.setResizable(false);
            stage.showAndWait();

            // Refresh after adding
            loadExpenses();

        } catch (Exception e) {
            System.err.println("Failed to open add expense dialog: " + e.getMessage());
            showError("Failed to open add expense dialog: " + e.getMessage());
        }
    }

    private void handleEditExpense(Expense expense) {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/fxml/edit_expense.fxml"));
            javafx.scene.Parent root = loader.load();

            EditExpenseController controller = loader.getController();
            controller.setExpense(expense);

            javafx.stage.Stage stage = new javafx.stage.Stage();
            stage.setTitle("Edit Expense");
            javafx.scene.Scene scene = new javafx.scene.Scene(root);
            var styleResource = getClass().getResource("/css/styles.css");
            if (styleResource != null) {
                scene.getStylesheets().add(styleResource.toExternalForm());
            }
            stage.setScene(scene);
            stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            stage.setResizable(false);
            stage.showAndWait();

            // Refresh after editing
            loadExpenses();

        } catch (Exception e) {
            System.err.println("Failed to open edit expense dialog: " + e.getMessage());
            showError("Failed to open edit expense dialog: " + e.getMessage());
        }
    }

    private void handleDeleteExpense(Expense expense) {
        if (expense != null) {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Delete Expense");
            confirm.setHeaderText("Are you sure?");
            confirm.setContentText("This will permanently delete this expense.");

            confirm.showAndWait().ifPresent(response -> {
                if (response == ButtonType.OK) {
                    ExpenseService.deleteExpense(expense.getExpenseId());
                    loadExpenses();
                }
            });
        }
    }

    @FXML
    private void handleViewAll() {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/fxml/expense_list.fxml"));
            javafx.scene.Parent root = loader.load();
            javafx.scene.Scene scene = new javafx.scene.Scene(root);
            var styleResource = getClass().getResource("/css/styles.css");
            if (styleResource != null) {
                scene.getStylesheets().add(styleResource.toExternalForm());
            }
            javafx.stage.Stage stage = (javafx.stage.Stage) viewAllButton.getScene().getWindow();
            stage.setScene(scene);
        } catch (Exception e) {
            System.err.println("Failed to navigate to expense list: " + e.getMessage());
        }
    }

    @FXML
    private void handleBack() {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/fxml/mode_selection.fxml"));
            javafx.scene.Parent root = loader.load();
            javafx.scene.Scene scene = new javafx.scene.Scene(root);
            var styleResource = getClass().getResource("/css/styles.css");
            if (styleResource != null) {
                scene.getStylesheets().add(styleResource.toExternalForm());
            }

            javafx.stage.Stage stage = (javafx.stage.Stage) backButton.getScene().getWindow();
            stage.setScene(scene);
        } catch (Exception e) {
            System.err.println("Failed to navigate back: " + e.getMessage());
        }
    }

    @FXML
    private void handleViewAnalytics() {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/fxml/personal_analytics.fxml"));
            javafx.scene.Parent root = loader.load();
            javafx.scene.Scene scene = new javafx.scene.Scene(root);
            var styleResource = getClass().getResource("/css/styles.css");
            if (styleResource != null) {
                scene.getStylesheets().add(styleResource.toExternalForm());
            }
            javafx.stage.Stage stage = (javafx.stage.Stage) viewAnalyticsButton.getScene().getWindow();
            stage.setScene(scene);
        } catch (Exception e) {
            System.err.println("Failed to navigate to analytics: " + e.getMessage());
        }
    }

    @FXML
    private void handleSendAlert() {
        handleSendAlertToParent();
    }

    @FXML
    private void handleSendAlertToParent() {
        var parent = org.example.service.ParentService.getParentForChild(currentUserId);

        if (parent == null) {
            showError("You don't have a parent connected yet.\n\nAsk your parent to send you an invite from their Parent Dashboard.");
            return;
        }

        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Send Message to Parent");
        dialog.setHeaderText("Send a message to " + parent.getName());
        dialog.setContentText("Enter your message:");
        dialog.getDialogPane().setPrefWidth(400);

        dialog.showAndWait().ifPresent(message -> {
            if (message.trim().isEmpty()) return;

            boolean success = org.example.service.ParentChildAlertService.sendAlertToParent(
                currentUserId,
                parent.getUserId(),
                message.trim()
            );

            if (success) {
                org.example.util.AlertUtil.showInfo("Success", "Message sent to your parent!");
            } else {
                showError("Failed to send message");
            }
        });
    }


    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}

