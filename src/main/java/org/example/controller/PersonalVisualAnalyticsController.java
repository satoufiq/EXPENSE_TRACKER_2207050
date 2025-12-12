package org.example.controller;

import javafx.fxml.FXML;
import javafx.scene.chart.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import org.example.model.Expense;
import org.example.service.AnalyticsService;
import org.example.service.BudgetService;
import org.example.service.ExpenseService;
import org.example.util.SessionManager;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoField;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Personal Visual Analytics Controller
 * Stunning visualization dashboard with multiple interactive charts
 */
public class PersonalVisualAnalyticsController {

    // Stats Labels
    @FXML private Label totalSpendingLabel;
    @FXML private Label avgPerDayLabel;
    @FXML private Label budgetUsedLabel;
    @FXML private Label topCategoryLabel;
    @FXML private Label transactionsLabel;

    // Charts
    @FXML private PieChart megaPieChart;
    @FXML private BarChart<String, Number> weeklyBarChart;
    @FXML private AreaChart<String, Number> trendAreaChart;
    @FXML private BarChart<Number, String> topCategoriesBarChart;
    @FXML private LineChart<String, Number> budgetProgressChart;

    // Buttons
    @FXML private Button toggleViewButton;
    @FXML private Button backButton;

    private String userId;

    @FXML
    public void initialize() {
        SessionManager sm = SessionManager.getInstance();
        if (sm.getCurrentUser() == null) return;
        userId = sm.getCurrentUser().getUserId();

        loadAllData();
    }

    private void loadAllData() {
        updateStatistics();
        updateMegaPieChart();
        updateWeeklyBarChart();
        updateTrendAreaChart();
        updateTopCategoriesChart();
        updateBudgetProgressChart();
    }

    private void updateStatistics() {
        List<Expense> expenses = ExpenseService.getPersonalExpensesObservable(userId);
        LocalDate now = LocalDate.now();

        // Total spending this month
        double monthlyTotal = expenses.stream()
            .filter(e -> {
                try {
                    LocalDate d = LocalDate.parse(e.getDate());
                    return d.getYear() == now.getYear() && d.getMonth() == now.getMonth();
                } catch (Exception ex) {
                    return false;
                }
            })
            .mapToDouble(Expense::getAmount)
            .sum();

        if (totalSpendingLabel != null) {
            totalSpendingLabel.setText(String.format("৳%.2f", monthlyTotal));
        }

        // Average per day
        int dayOfMonth = now.getDayOfMonth();
        double avgPerDay = dayOfMonth > 0 ? monthlyTotal / dayOfMonth : 0;
        if (avgPerDayLabel != null) {
            avgPerDayLabel.setText(String.format("৳%.2f", avgPerDay));
        }

        // Budget used percentage
        double budget = BudgetService.getMonthlyBudget(userId);
        if (budgetUsedLabel != null) {
            if (budget > 0) {
                double percentage = (monthlyTotal / budget) * 100;
                budgetUsedLabel.setText(String.format("%.1f%%", percentage));

                // Color code based on percentage
                if (percentage > 100) {
                    budgetUsedLabel.setStyle("-fx-text-fill: #F44336;");
                } else if (percentage > 90) {
                    budgetUsedLabel.setStyle("-fx-text-fill: #FF5722;");
                } else if (percentage > 75) {
                    budgetUsedLabel.setStyle("-fx-text-fill: #FF9800;");
                } else {
                    budgetUsedLabel.setStyle("-fx-text-fill: #4CAF50;");
                }
            } else {
                budgetUsedLabel.setText("N/A");
            }
        }

        // Top category
        Map<String, Double> categoryTotals = new HashMap<>();
        for (Expense e : expenses) {
            try {
                LocalDate d = LocalDate.parse(e.getDate());
                if (d.getYear() == now.getYear() && d.getMonth() == now.getMonth()) {
                    categoryTotals.merge(
                        Optional.ofNullable(e.getCategory()).orElse("Other"),
                        e.getAmount(),
                        Double::sum
                    );
                }
            } catch (Exception ex) {
                // Skip
            }
        }

        if (topCategoryLabel != null && !categoryTotals.isEmpty()) {
            String topCat = categoryTotals.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("-");
            topCategoryLabel.setText(topCat);
        }

        // Transaction count
        if (transactionsLabel != null) {
            long count = expenses.stream()
                .filter(e -> {
                    try {
                        LocalDate d = LocalDate.parse(e.getDate());
                        return d.getYear() == now.getYear() && d.getMonth() == now.getMonth();
                    } catch (Exception ex) {
                        return false;
                    }
                })
                .count();
            transactionsLabel.setText(String.valueOf(count));
        }
    }

    private void updateMegaPieChart() {
        if (megaPieChart == null) return;

        megaPieChart.getData().clear();

        List<Expense> expenses = ExpenseService.getPersonalExpensesObservable(userId);
        LocalDate now = LocalDate.now();

        Map<String, Double> categoryTotals = new HashMap<>();

        for (Expense e : expenses) {
            try {
                LocalDate d = LocalDate.parse(e.getDate());
                if (d.getYear() == now.getYear() && d.getMonth() == now.getMonth()) {
                    categoryTotals.merge(
                        Optional.ofNullable(e.getCategory()).orElse("Other"),
                        e.getAmount(),
                        Double::sum
                    );
                }
            } catch (Exception ex) {
                // Skip
            }
        }

        categoryTotals.entrySet().stream()
            .sorted((e1, e2) -> Double.compare(e2.getValue(), e1.getValue()))
            .forEach(entry -> {
                PieChart.Data slice = new PieChart.Data(
                    String.format("%s (৳%.0f)", entry.getKey(), entry.getValue()),
                    entry.getValue()
                );
                megaPieChart.getData().add(slice);
            });
    }

    private void updateWeeklyBarChart() {
        if (weeklyBarChart == null) return;

        weeklyBarChart.getData().clear();

        List<Expense> expenses = ExpenseService.getPersonalExpensesObservable(userId);
        LocalDate now = LocalDate.now();

        // Get last 4 weeks
        Map<String, Double> weeklyTotals = new TreeMap<>();

        for (int i = 3; i >= 0; i--) {
            LocalDate weekStart = now.minusWeeks(i);
            String weekLabel = "Week " + (4 - i);
            weeklyTotals.put(weekLabel, 0.0);

            for (Expense e : expenses) {
                try {
                    LocalDate d = LocalDate.parse(e.getDate());
                    if (d.isAfter(weekStart.minusDays(1)) && d.isBefore(weekStart.plusDays(7))) {
                        weeklyTotals.merge(weekLabel, e.getAmount(), Double::sum);
                    }
                } catch (Exception ex) {
                    // Skip
                }
            }
        }

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Spending");

        weeklyTotals.forEach((week, amount) -> {
            series.getData().add(new XYChart.Data<>(week, amount));
        });

        weeklyBarChart.getData().add(series);
    }

    private void updateTrendAreaChart() {
        if (trendAreaChart == null) return;

        trendAreaChart.getData().clear();

        List<Expense> expenses = ExpenseService.getPersonalExpensesObservable(userId);
        LocalDate now = LocalDate.now();
        LocalDate thirtyDaysAgo = now.minusDays(29);

        Map<LocalDate, Double> dailyTotals = new TreeMap<>();

        // Initialize all days
        for (int i = 0; i < 30; i++) {
            dailyTotals.put(thirtyDaysAgo.plusDays(i), 0.0);
        }

        // Sum expenses by date
        for (Expense e : expenses) {
            try {
                LocalDate d = LocalDate.parse(e.getDate());
                if (!d.isBefore(thirtyDaysAgo) && !d.isAfter(now)) {
                    dailyTotals.merge(d, e.getAmount(), Double::sum);
                }
            } catch (Exception ex) {
                // Skip
            }
        }

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Daily Spending");

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd");
        int i = 0;
        for (Map.Entry<LocalDate, Double> entry : dailyTotals.entrySet()) {
            if (i % 3 == 0 || i == dailyTotals.size() - 1) {
                series.getData().add(new XYChart.Data<>(
                    entry.getKey().format(formatter),
                    entry.getValue()
                ));
            }
            i++;
        }

        trendAreaChart.getData().add(series);
    }

    private void updateTopCategoriesChart() {
        if (topCategoriesBarChart == null) return;

        topCategoriesBarChart.getData().clear();

        List<Expense> expenses = ExpenseService.getPersonalExpensesObservable(userId);
        LocalDate now = LocalDate.now();

        Map<String, Double> categoryTotals = new HashMap<>();

        for (Expense e : expenses) {
            try {
                LocalDate d = LocalDate.parse(e.getDate());
                if (d.getYear() == now.getYear() && d.getMonth() == now.getMonth()) {
                    categoryTotals.merge(
                        Optional.ofNullable(e.getCategory()).orElse("Other"),
                        e.getAmount(),
                        Double::sum
                    );
                }
            } catch (Exception ex) {
                // Skip
            }
        }

        List<Map.Entry<String, Double>> topCategories = categoryTotals.entrySet().stream()
            .sorted((e1, e2) -> Double.compare(e2.getValue(), e1.getValue()))
            .limit(5)
            .collect(Collectors.toList());

        XYChart.Series<Number, String> series = new XYChart.Series<>();
        series.setName("Amount");

        for (Map.Entry<String, Double> entry : topCategories) {
            series.getData().add(new XYChart.Data<>(entry.getValue(), entry.getKey()));
        }

        topCategoriesBarChart.getData().add(series);
    }

    private void updateBudgetProgressChart() {
        if (budgetProgressChart == null) return;

        budgetProgressChart.getData().clear();

        List<Expense> expenses = ExpenseService.getPersonalExpensesObservable(userId);
        LocalDate now = LocalDate.now();
        double budget = BudgetService.getMonthlyBudget(userId);

        // Cumulative spending by day of month
        Map<Integer, Double> cumulativeByDay = new TreeMap<>();

        for (Expense e : expenses) {
            try {
                LocalDate d = LocalDate.parse(e.getDate());
                if (d.getYear() == now.getYear() && d.getMonth() == now.getMonth()) {
                    int day = d.getDayOfMonth();
                    cumulativeByDay.merge(day, e.getAmount(), Double::sum);
                }
            } catch (Exception ex) {
                // Skip
            }
        }

        // Convert to cumulative
        double cumulative = 0.0;
        XYChart.Series<String, Number> actualSeries = new XYChart.Series<>();
        actualSeries.setName("Actual Spending");

        for (int day = 1; day <= now.getDayOfMonth(); day++) {
            cumulative += cumulativeByDay.getOrDefault(day, 0.0);
            actualSeries.getData().add(new XYChart.Data<>(String.valueOf(day), cumulative));
        }

        budgetProgressChart.getData().add(actualSeries);

        // Add budget line if set
        if (budget > 0) {
            XYChart.Series<String, Number> budgetSeries = new XYChart.Series<>();
            budgetSeries.setName("Budget Limit");

            int daysInMonth = now.lengthOfMonth();
            for (int day = 1; day <= now.getDayOfMonth(); day++) {
                double expectedBudget = (budget / daysInMonth) * day;
                budgetSeries.getData().add(new XYChart.Data<>(String.valueOf(day), expectedBudget));
            }

            budgetProgressChart.getData().add(budgetSeries);
        }
    }

    @FXML
    private void handleToggleView() {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                getClass().getResource("/fxml/personal_analytics.fxml"));
            javafx.scene.Parent root = loader.load();
            javafx.scene.Scene scene = new javafx.scene.Scene(root);
            scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
            javafx.stage.Stage stage = (javafx.stage.Stage) toggleViewButton.getScene().getWindow();
            stage.setScene(scene);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleBack() {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                getClass().getResource("/fxml/personal_dashboard.fxml"));
            javafx.scene.Parent root = loader.load();
            javafx.scene.Scene scene = new javafx.scene.Scene(root);
            scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
            javafx.stage.Stage stage = (javafx.stage.Stage) backButton.getScene().getWindow();
            stage.setScene(scene);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

