package org.example.controller;

import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.chart.PieChart;
import javafx.scene.control.*;
import org.example.MainApp;
import org.example.model.Expense;
import org.example.service.ExpenseService;
import org.example.service.ParentChildAlertService;
import org.example.service.UserService;
import org.example.util.SessionManager;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

@SuppressWarnings({"unused", "FieldCanBeLocal"})
public class ChildAnalyticsController {

    @FXML private Label childNameLabel;
    @FXML private Label totalExpensesLabel;
    @FXML private Label monthExpensesLabel;
    @FXML private Label categoryCountLabel;
    @FXML private PieChart categoryPieChart;
    @FXML private ListView<String> recentExpensesList;
    @FXML private TextArea suggestionTextArea;
    @FXML private Button sendSuggestionButton;
    @FXML private Button backButton;

    private String childUserId;
    private String parentUserId;

    @FXML
    public void initialize() {
        childUserId = SessionManager.getInstance().getSelectedChildId();
        parentUserId = SessionManager.getInstance().getCurrentUser().getUserId();

        if (childUserId == null) {
            showError("No child selected!");
            goBack();
            return;
        }

        loadChildAnalytics();

        sendSuggestionButton.setOnAction(e -> sendSuggestion());
        backButton.setOnAction(e -> goBack());
    }

    private void loadChildAnalytics() {
        var child = UserService.getUserById(childUserId);
        if (child != null) {
            childNameLabel.setText("Child Analytics: " + child.getName());
        }

        ObservableList<Expense> expenses = ExpenseService.getPersonalExpensesObservable(childUserId);

        double total = 0;
        double monthTotal = 0;
        Map<String, Double> categoryMap = new HashMap<>();

        LocalDate now = LocalDate.now();
        LocalDate monthStart = now.withDayOfMonth(1);

        for (Expense exp : expenses) {
            total += exp.getAmount();

            try {
                LocalDate expDate = LocalDate.parse(exp.getDate());
                if (!expDate.isBefore(monthStart) && !expDate.isAfter(now)) {
                    monthTotal += exp.getAmount();
                    categoryMap.merge(exp.getCategory(), exp.getAmount(), Double::sum);
                }
            } catch (Exception ignored) {}
        }

        totalExpensesLabel.setText(String.format("৳%.2f", total));
        monthExpensesLabel.setText(String.format("৳%.2f", monthTotal));
        categoryCountLabel.setText(String.valueOf(categoryMap.size()));

        categoryPieChart.getData().clear();
        for (Map.Entry<String, Double> entry : categoryMap.entrySet()) {
            PieChart.Data slice = new PieChart.Data(
                entry.getKey() + " (৳" + String.format("%.0f", entry.getValue()) + ")",
                entry.getValue()
            );
            categoryPieChart.getData().add(slice);
        }

        javafx.collections.ObservableList<String> expenseStrings = javafx.collections.FXCollections.observableArrayList();
        int limit = Math.min(10, expenses.size());
        for (int i = 0; i < limit; i++) {
            Expense exp = expenses.get(i);
            String expStr = String.format("%s - %s: ৳%.2f (%s)",
                exp.getDate(),
                exp.getCategory(),
                exp.getAmount(),
                exp.getNote() != null ? exp.getNote() : "No note"
            );
            expenseStrings.add(expStr);
        }
        recentExpensesList.setItems(expenseStrings);
    }


    private void sendSuggestion() {
        String suggestion = suggestionTextArea.getText();
        if (suggestion == null || suggestion.trim().isEmpty()) {
            showError("Please enter a suggestion message.");
            return;
        }

        sendSuggestionButton.setDisable(true);
        sendSuggestionButton.setText("Sending...");

        boolean success = ParentChildAlertService.sendSuggestionToChild(
            parentUserId,
            childUserId,
            suggestion.trim()
        );

        sendSuggestionButton.setDisable(false);
        sendSuggestionButton.setText("Send Suggestion");

        if (success) {
            suggestionTextArea.clear();
            org.example.util.AlertUtil.showInfo("Success", "Suggestion sent successfully!");
        } else {
            showError("Failed to send suggestion");
        }
    }

    private void goBack() {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                getClass().getResource("/fxml/parent_dashboard.fxml"));
            javafx.scene.Parent root = loader.load();
            javafx.scene.Scene scene = new javafx.scene.Scene(root);
            scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
            MainApp.getPrimaryStage().setScene(scene);
        } catch (Exception ex) {
            ex.printStackTrace();
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