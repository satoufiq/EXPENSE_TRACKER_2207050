package org.example.controller;

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

    private String childUserId;
    private String parentUserId;
    private ObservableList<Expense> expensesList;

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
    }

    private void loadChildExpenses() {
        var child = UserService.getUserById(childUserId);
        if (child != null && childNameLabel != null) {
            childNameLabel.setText("💰 Expenses - " + child.getName());
        }

        expensesList = ExpenseService.getPersonalExpensesObservable(childUserId);
        refreshExpenseCards();
        updateStatistics();
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
        double total = expensesList.stream()
            .mapToDouble(Expense::getAmount)
            .sum();

        if (totalExpensesLabel != null) {
            totalExpensesLabel.setText(String.format("৳%.2f", total));
        }
        if (transactionCountLabel != null) {
            transactionCountLabel.setText(String.valueOf(expensesList.size()));
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

