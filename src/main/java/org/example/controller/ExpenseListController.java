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
import org.example.util.SessionManager;

@SuppressWarnings({"unused", "FieldCanBeLocal"})
public class ExpenseListController {

    @FXML private Label expenseCountLabel;
    @FXML private Label totalExpensesLabel;
    @FXML private Label monthExpensesLabel;
    @FXML private Label transactionCountLabel;
    @FXML private FlowPane expenseCardsPane;
    @FXML private VBox emptyStatePane;
    @FXML private Button addExpenseButton;
    @FXML private Button backButton;

    private ObservableList<Expense> expensesList;
    private String currentUserId;

    @FXML
    public void initialize() {
        SessionManager session = SessionManager.getInstance();
        if (session.getCurrentUser() != null) {
            this.currentUserId = session.getCurrentUser().getUserId();
            loadExpenses();
        }
    }

    private void loadExpenses() {
        if (currentUserId == null || currentUserId.isEmpty()) return;

        expensesList = ExpenseService.getPersonalExpensesObservable(currentUserId);
        refreshExpenseCards();
        updateStatistics();

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

        for (Expense expense : expensesList) {
            expenseCardsPane.getChildren().add(createExpenseCard(expense));
        }

        if (expenseCountLabel != null) {
            expenseCountLabel.setText(expensesList.size() + " expenses");
        }
    }

    private VBox createExpenseCard(Expense expense) {
        VBox card = new VBox(10);
        card.getStyleClass().add("expense-card");
        card.setPrefWidth(340);
        card.setMinWidth(340);
        card.setMaxWidth(340);

        // Top row: Category badge
        HBox topRow = new HBox(10);
        topRow.setAlignment(Pos.CENTER_LEFT);

        Label categoryBadge = new Label(getCategoryEmoji(expense.getCategory()) + " " + expense.getCategory());
        categoryBadge.getStyleClass().add("expense-category-badge");

        topRow.getChildren().add(categoryBadge);

        // Amount and date row
        HBox amountRow = new HBox(15);
        amountRow.setAlignment(Pos.CENTER_LEFT);

        Label amountLabel = new Label(String.format("৳%.2f", expense.getAmount()));
        amountLabel.getStyleClass().add("expense-amount");

        Label dateLabel = new Label("📅 " + expense.getDate());
        dateLabel.getStyleClass().add("expense-date");

        amountRow.getChildren().addAll(amountLabel, dateLabel);

        // Note
        String noteText = expense.getNote() != null && !expense.getNote().isEmpty()
            ? expense.getNote()
            : "No note";
        Label noteLabel = new Label(noteText);
        noteLabel.getStyleClass().add("expense-note");
        noteLabel.setWrapText(true);
        noteLabel.setMaxWidth(300);

        // Action buttons
        HBox actionsRow = new HBox(10);
        actionsRow.setAlignment(Pos.CENTER_LEFT);

        Button editBtn = new Button("✏️ Edit");
        editBtn.getStyleClass().addAll("card-action-button", "card-edit-button");
        editBtn.setOnAction(e -> handleEditExpense(expense));

        Button deleteBtn = new Button("🗑️ Delete");
        deleteBtn.getStyleClass().addAll("card-action-button", "card-delete-button");
        deleteBtn.setOnAction(e -> handleDeleteExpense(expense));

        actionsRow.getChildren().addAll(editBtn, deleteBtn);

        card.getChildren().addAll(topRow, amountRow, noteLabel, actionsRow);

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

        double total = expensesList.stream().mapToDouble(Expense::getAmount).sum();
        if (totalExpensesLabel != null) {
            totalExpensesLabel.setText(String.format("৳%.2f", total));
        }

        double monthTotal = expensesList.stream()
            .filter(e -> isCurrentMonth(e.getDate()))
            .mapToDouble(Expense::getAmount)
            .sum();
        if (monthExpensesLabel != null) {
            monthExpensesLabel.setText(String.format("৳%.2f", monthTotal));
        }

        if (transactionCountLabel != null) {
            transactionCountLabel.setText(String.valueOf(expensesList.size()));
        }
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

            loadExpenses();

        } catch (Exception e) {
            System.err.println("Failed to open add expense dialog: " + e.getMessage());
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

            loadExpenses();

        } catch (Exception e) {
            System.err.println("Failed to open edit expense dialog: " + e.getMessage());
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
    private void handleBack() {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/fxml/personal_dashboard.fxml"));
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
}