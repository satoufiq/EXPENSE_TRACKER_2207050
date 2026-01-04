package org.example.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.example.MainApp;
import org.example.model.Expense;
import org.example.service.ExpenseService;
import org.example.service.UserService;
import org.example.util.SessionManager;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@SuppressWarnings({"unused", "FieldCanBeLocal"})
public class ChildExpenseListController {

    @FXML private Label childNameLabel;
    @FXML private FlowPane expenseCardsPane;
    @FXML private VBox emptyStatePane;
    @FXML private Label totalExpensesLabel;
    @FXML private Label transactionCountLabel;
    @FXML private Button backButton;
    @FXML private TextArea suggestionTextArea;
    @FXML private Button sendSuggestionButton;

    // Filter controls
    @FXML private TextField searchField;
    @FXML private ComboBox<String> categoryFilter;
    @FXML private DatePicker fromDatePicker;
    @FXML private DatePicker toDatePicker;
    @FXML private Button clearFiltersButton;

    private String childUserId;
    private String parentUserId;
    private ObservableList<Expense> expensesList;
    private ObservableList<Expense> filteredExpenses;

    @FXML
    public void initialize() {
        childUserId = SessionManager.getInstance().getSelectedChildId();
        parentUserId = SessionManager.getInstance().getCurrentUser().getUserId();

        if (childUserId == null) {
            showError("No child selected!");
            handleBack();
            return;
        }

        loadChildExpenses();
        setupFilters();
    }

    private void setupFilters() {
        // Initialize category filter
        if (categoryFilter != null) {
            List<String> categories = Arrays.asList(
                "All Categories", "Food", "Transport", "Shopping", "Entertainment",
                "Bills", "Health", "Education", "Groceries", "Utilities", "Rent", "Other"
            );
            categoryFilter.setItems(FXCollections.observableArrayList(categories));
            categoryFilter.setValue("All Categories");
            categoryFilter.setOnAction(e -> applyFilters());
        }

        // Setup search field listener
        if (searchField != null) {
            searchField.textProperty().addListener((obs, oldVal, newVal) -> applyFilters());
        }

        // Setup date pickers listeners
        if (fromDatePicker != null) {
            fromDatePicker.setOnAction(e -> applyFilters());
        }
        if (toDatePicker != null) {
            toDatePicker.setOnAction(e -> applyFilters());
        }
    }

    @FXML
    private void handleClearFilters() {
        if (searchField != null) searchField.clear();
        if (categoryFilter != null) categoryFilter.setValue("All Categories");
        if (fromDatePicker != null) fromDatePicker.setValue(null);
        if (toDatePicker != null) toDatePicker.setValue(null);
        applyFilters();
    }

    private void applyFilters() {
        if (expensesList == null) return;

        List<Expense> filtered = expensesList.stream()
            .filter(this::matchesSearchFilter)
            .filter(this::matchesCategoryFilter)
            .filter(this::matchesDateFilter)
            .collect(Collectors.toList());

        filteredExpenses = FXCollections.observableArrayList(filtered);
        refreshExpenseCards();
        updateStatistics();
    }

    private boolean matchesSearchFilter(Expense expense) {
        if (searchField == null || searchField.getText() == null || searchField.getText().trim().isEmpty()) {
            return true;
        }

        String search = searchField.getText().toLowerCase();
        String note = expense.getNote() != null ? expense.getNote().toLowerCase() : "";
        String amount = String.valueOf(expense.getAmount());
        String category = expense.getCategory() != null ? expense.getCategory().toLowerCase() : "";

        return note.contains(search) || amount.contains(search) || category.contains(search);
    }

    private boolean matchesCategoryFilter(Expense expense) {
        if (categoryFilter == null || categoryFilter.getValue() == null ||
            categoryFilter.getValue().equals("All Categories")) {
            return true;
        }

        return expense.getCategory() != null &&
               expense.getCategory().equalsIgnoreCase(categoryFilter.getValue());
    }

    private boolean matchesDateFilter(Expense expense) {
        try {
            LocalDate expenseDate = LocalDate.parse(expense.getDate());
            LocalDate fromDate = fromDatePicker != null ? fromDatePicker.getValue() : null;
            LocalDate toDate = toDatePicker != null ? toDatePicker.getValue() : null;

            if (fromDate != null && expenseDate.isBefore(fromDate)) {
                return false;
            }

            if (toDate != null && expenseDate.isAfter(toDate)) {
                return false;
            }

            return true;
        } catch (Exception e) {
            return true;
        }
    }

    private void loadChildExpenses() {
        var child = UserService.getUserById(childUserId);
        if (child != null && childNameLabel != null) {
            childNameLabel.setText("💰 Expenses - " + child.getName());
        }

        expensesList = ExpenseService.getPersonalExpensesObservable(childUserId);
        filteredExpenses = FXCollections.observableArrayList(expensesList);
        refreshExpenseCards();
        updateStatistics();
    }

    private void refreshExpenseCards() {
        if (expenseCardsPane == null) return;

        expenseCardsPane.getChildren().clear();

        ObservableList<Expense> displayList = filteredExpenses != null ? filteredExpenses : expensesList;

        if (displayList == null || displayList.isEmpty()) {
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

        for (Expense expense : displayList) {
            expenseCardsPane.getChildren().add(createExpenseCard(expense));
        }
    }

    private VBox createExpenseCard(Expense expense) {
        VBox card = new VBox(10);
        card.getStyleClass().add("expense-card");
        card.setPrefWidth(320);
        card.setMinWidth(320);
        card.setMaxWidth(320);

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
        noteLabel.setMaxWidth(290);

        // Read-only indicator
        Label readOnlyLabel = new Label("👁️ Read-only");
        readOnlyLabel.setStyle("-fx-text-fill: rgba(255,255,255,0.5); -fx-font-size: 10px;");

        card.getChildren().addAll(topRow, amountRow, noteLabel, readOnlyLabel);

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
            case "health": case "healthcare": return "💊";
            case "education": return "📚";
            case "groceries": return "🥬";
            case "utilities": return "💡";
            case "rent": return "🏠";
            default: return "📦";
        }
    }

    private void updateStatistics() {
        ObservableList<Expense> displayList = filteredExpenses != null ? filteredExpenses : expensesList;

        double total = displayList != null ? displayList.stream()
            .mapToDouble(Expense::getAmount)
            .sum() : 0.0;

        if (totalExpensesLabel != null) {
            totalExpensesLabel.setText(String.format("৳%.2f", total));
        }
        if (transactionCountLabel != null) {
            transactionCountLabel.setText(String.valueOf(displayList != null ? displayList.size() : 0));
        }
    }

    @FXML
    private void handleSendSuggestion() {
        if (suggestionTextArea == null) return;

        String suggestion = suggestionTextArea.getText();
        if (suggestion == null || suggestion.trim().isEmpty()) {
            showError("Please enter a suggestion message.");
            return;
        }

        sendSuggestionButton.setDisable(true);
        sendSuggestionButton.setText("Sending...");

        boolean success = org.example.service.ParentChildAlertService.sendSuggestionToChild(
            parentUserId,
            childUserId,
            suggestion.trim()
        );

        sendSuggestionButton.setDisable(false);
        sendSuggestionButton.setText("📤 Send Suggestion");

        if (success) {
            suggestionTextArea.clear();
            org.example.util.AlertUtil.showInfo("Success", "Suggestion sent successfully!");
        } else {
            showError("Failed to send suggestion");
        }
    }

    @FXML
    private void handleBack() {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                getClass().getResource("/fxml/parent_dashboard.fxml"));
            javafx.scene.Parent root = loader.load();
            javafx.scene.Scene scene = new javafx.scene.Scene(root);
            var styleResource = getClass().getResource("/css/styles.css");
            if (styleResource != null) {
                scene.getStylesheets().add(styleResource.toExternalForm());
            }
            MainApp.getPrimaryStage().setScene(scene);
        } catch (Exception ex) {
            System.err.println("Failed to navigate back: " + ex.getMessage());
        }
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}

