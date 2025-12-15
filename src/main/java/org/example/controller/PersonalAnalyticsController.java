package org.example.controller;

import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.layout.VBox;
import org.example.model.Expense;
import org.example.service.AnalyticsService;
import org.example.service.BudgetService;
import org.example.service.ExpenseService;
import org.example.util.SessionManager;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Personal Analytics Controller with Charts and Enhanced Features
 */
public class PersonalAnalyticsController {

    @FXML private Label monthlyTotalLabel;
    @FXML private Label weeklyTotalLabel;
    @FXML private Label highestDayLabel;
    @FXML private Label budgetLabel;
    @FXML private Label budgetStatusLabel;
    @FXML private ListView<String> categoryList;
    @FXML private ListView<String> suggestionsList;
    @FXML private Button setBudgetButton;
    @FXML private Button backButton;
    @FXML private PieChart categoryPieChart;
    @FXML private LineChart<String, Number> trendLineChart;
    @FXML private VBox root;

    private String userId;
    private String overrideUserId; // optional override when viewing other member
    private String currentGroupId; // when invoked from group mode

    /**
     * Allow external controllers to set which user's analytics to display.
     */
    public void setUserIdForAnalytics(String userId) {
        this.overrideUserId = userId;
    }

    @FXML
    public void initialize() {
        SessionManager sm = SessionManager.getInstance();
        currentGroupId = sm.getCurrentGroupId();
        if (overrideUserId != null && !overrideUserId.isEmpty()) {
            userId = overrideUserId;
        } else {
            if (sm.getCurrentUser() == null) return;
            userId = sm.getCurrentUser().getUserId();
        }
        refresh();
    }

    private void refresh() {
        AnalyticsService.PersonalAnalyticsSummary s = AnalyticsService.buildPersonalSummary(userId);

        // Update labels
        if (monthlyTotalLabel != null) {
            monthlyTotalLabel.setText(String.format("৳%.2f", s.monthlyTotal));
        }

        if (weeklyTotalLabel != null) {
            weeklyTotalLabel.setText(String.format("৳%.2f", s.weeklyTotal));
        }

        if (highestDayLabel != null) {
            String txt = s.highestSpendingDay == null ? "-" :
                s.highestSpendingDay.format(DateTimeFormatter.ofPattern("MMM dd")) +
                "\n৳" + String.format("%.2f", s.highestSpendingAmount);
            highestDayLabel.setText(txt);
        }

        // Update budget info
        double budget = BudgetService.getMonthlyBudget(userId);
        if (budgetLabel != null) {
            budgetLabel.setText(String.format("৳%.2f", budget));
        }

        // Update budget status
        if (budgetStatusLabel != null) {
            updateBudgetStatus(s.monthlyTotal, budget);
        }

        // Update category list
        if (categoryList != null) {
            categoryList.getItems().clear();
            s.categoryTotals.forEach((k, v) ->
                categoryList.getItems().add(String.format("%s: ৳%.2f (%.1f%%)",
                    k, v, (v / s.monthlyTotal * 100))));
        }

        // Update suggestions list
        if (suggestionsList != null) {
            suggestionsList.getItems().setAll(s.suggestions);
        }

        // Update charts
        updatePieChart(s.categoryTotals);
        updateTrendChart();
    }

    private void updateBudgetStatus(double spent, double budget) {
        if (budget <= 0) {
            budgetStatusLabel.setText("Not Set");
            budgetStatusLabel.setStyle("-fx-text-fill: #888888; -fx-font-size: 16px;");
            return;
        }

        double percentage = (spent / budget) * 100;

        if (percentage < 75) {
            budgetStatusLabel.setText("✓ On Track");
            budgetStatusLabel.setStyle("-fx-text-fill: #4CAF50; -fx-font-size: 16px;");
        } else if (percentage < 90) {
            budgetStatusLabel.setText("⚠ Caution");
            budgetStatusLabel.setStyle("-fx-text-fill: #FF9800; -fx-font-size: 16px;");
        } else if (percentage < 100) {
            budgetStatusLabel.setText("⚠ Near Limit");
            budgetStatusLabel.setStyle("-fx-text-fill: #FF5722; -fx-font-size: 16px;");
        } else {
            budgetStatusLabel.setText("✗ Exceeded");
            budgetStatusLabel.setStyle("-fx-text-fill: #F44336; -fx-font-size: 16px;");
        }
    }

    private void updatePieChart(Map<String, Double> categoryTotals) {
        if (categoryPieChart == null) return;

        categoryPieChart.getData().clear();

        // Add data to pie chart
        categoryTotals.entrySet().stream()
            .sorted((e1, e2) -> Double.compare(e2.getValue(), e1.getValue()))
            .limit(8) // Top 8 categories
            .forEach(entry -> {
                PieChart.Data slice = new PieChart.Data(
                    entry.getKey(),
                    entry.getValue()
                );
                categoryPieChart.getData().add(slice);
            });

        // Style the pie chart
        categoryPieChart.setLabelsVisible(true);
        categoryPieChart.setLegendVisible(true);
    }

    private void updateTrendChart() {
        if (trendLineChart == null) return;

        trendLineChart.getData().clear();

        // Get expenses for last 30 days, group-scoped when in group mode
        List<org.example.model.Expense> expenses;
        if (currentGroupId != null && !currentGroupId.isEmpty()) {
            expenses = ExpenseService.getGroupExpensesByUser(currentGroupId, userId);
        } else {
            expenses = ExpenseService.getPersonalExpensesObservable(userId);
        }
        LocalDate today = LocalDate.now();
        LocalDate thirtyDaysAgo = today.minusDays(29);

        Map<LocalDate, Double> dailyTotals = new TreeMap<>();

        // Initialize all days with 0
        for (int i = 0; i < 30; i++) {
            dailyTotals.put(thirtyDaysAgo.plusDays(i), 0.0);
        }

        // Sum expenses by date
        for (Expense e : expenses) {
            try {
                LocalDate date = LocalDate.parse(e.getDate());
                if (!date.isBefore(thirtyDaysAgo) && !date.isAfter(today)) {
                    dailyTotals.merge(date, e.getAmount(), Double::sum);
                }
            } catch (Exception ex) {
                // Skip invalid dates
            }
        }

        // Create series
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Daily Spending");

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd");

        // Add data points (show every 5th day for readability)
        int i = 0;
        for (Map.Entry<LocalDate, Double> entry : dailyTotals.entrySet()) {
            if (i % 5 == 0 || i == dailyTotals.size() - 1) {
                series.getData().add(new XYChart.Data<>(
                    entry.getKey().format(formatter),
                    entry.getValue()
                ));
            }
            i++;
        }

        trendLineChart.getData().add(series);
    }

    @FXML
    private void handleSetBudget() {
        javafx.scene.control.TextInputDialog dialog = new javafx.scene.control.TextInputDialog();
        dialog.setTitle("Set Monthly Budget");
        dialog.setHeaderText("Set Your Monthly Budget");
        dialog.setContentText("Enter budget amount in ৳ (BDT):");

        // Get current budget
        double currentBudget = BudgetService.getMonthlyBudget(userId);
        if (currentBudget > 0) {
            dialog.getEditor().setText(String.format("%.2f", currentBudget));
        }

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(val -> {
            try {
                double amt = Double.parseDouble(val.trim());
                if (amt < 0) {
                    showAlert(javafx.scene.control.Alert.AlertType.ERROR,
                        "Invalid Amount", "Budget amount cannot be negative.");
                    return;
                }
                BudgetService.setMonthlyBudget(userId, amt);
                refresh();

                showAlert(javafx.scene.control.Alert.AlertType.INFORMATION,
                    "Budget Set",
                    String.format("Your monthly budget has been set to ৳%.2f", amt));

            } catch (NumberFormatException ex) {
                showAlert(javafx.scene.control.Alert.AlertType.ERROR,
                    "Invalid Input", "Please enter a valid number.");
            }
        });
    }

    @FXML
    private void handleBack() {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                getClass().getResource("/fxml/personal_dashboard.fxml"));
            javafx.scene.Parent rootNode = loader.load();
            javafx.stage.Stage stage = (javafx.stage.Stage) backButton.getScene().getWindow();
            javafx.scene.Scene scene = new javafx.scene.Scene(rootNode);
            scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
            stage.setScene(scene);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleVisualAnalytics() {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                getClass().getResource("/fxml/personal_visual_analytics.fxml"));
            javafx.scene.Parent rootNode = loader.load();
            javafx.stage.Stage stage = (javafx.stage.Stage) backButton.getScene().getWindow();
            javafx.scene.Scene scene = new javafx.scene.Scene(rootNode);
            scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
            stage.setScene(scene);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showAlert(javafx.scene.control.Alert.AlertType type, String title, String message) {
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
