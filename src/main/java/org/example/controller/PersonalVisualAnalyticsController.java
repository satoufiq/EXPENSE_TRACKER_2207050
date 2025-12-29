package org.example.controller;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import org.example.model.Expense;
import org.example.service.ExpenseService;
import org.example.util.SessionManager;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.WeekFields;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Controller for Personal Visual Analytics Dashboard
 * Provides comprehensive charts and visualizations for expense data
 */
public class PersonalVisualAnalyticsController {

    @FXML private ComboBox<String> timeRangeCombo;
    @FXML private Button backButton;

    // Summary Labels
    @FXML private Label totalSpentLabel;
    @FXML private Label avgDailyLabel;
    @FXML private Label categoryCountLabel;
    @FXML private Label peakDayLabel;
    @FXML private Label transactionCountLabel;

    // Charts
    @FXML private PieChart categoryPieChart;
    @FXML private BarChart<String, Number> categoryBarChart;
    @FXML private LineChart<String, Number> trendLineChart;
    @FXML private AreaChart<String, Number> cumulativeAreaChart;
    @FXML private BarChart<String, Number> weeklyBarChart;
    @FXML private BarChart<String, Number> dayOfWeekChart;

    private String userId;
    private List<Expense> allExpenses;
    private int selectedDays = 30;

    @FXML
    public void initialize() {
        SessionManager sm = SessionManager.getInstance();
        if (sm.getCurrentUser() == null) return;
        userId = sm.getCurrentUser().getUserId();

        // Setup time range combo
        timeRangeCombo.setItems(FXCollections.observableArrayList(
            "Last 7 Days", "Last 30 Days", "Last 90 Days", "This Year", "All Time"
        ));
        timeRangeCombo.setValue("Last 30 Days");
        timeRangeCombo.setOnAction(e -> onTimeRangeChanged());

        loadData();
    }

    private void onTimeRangeChanged() {
        String selected = timeRangeCombo.getValue();
        switch (selected) {
            case "Last 7 Days": selectedDays = 7; break;
            case "Last 30 Days": selectedDays = 30; break;
            case "Last 90 Days": selectedDays = 90; break;
            case "This Year": selectedDays = 365; break;
            case "All Time": selectedDays = Integer.MAX_VALUE; break;
            default: selectedDays = 30;
        }
        loadData();
    }

    private void loadData() {
        allExpenses = ExpenseService.getPersonalExpensesObservable(userId);

        // Filter by date range
        LocalDate today = LocalDate.now();
        LocalDate startDate = selectedDays == Integer.MAX_VALUE ?
            LocalDate.MIN : today.minusDays(selectedDays - 1);

        List<Expense> filteredExpenses = allExpenses.stream()
            .filter(e -> {
                try {
                    LocalDate date = LocalDate.parse(e.getDate());
                    return !date.isBefore(startDate) && !date.isAfter(today);
                } catch (Exception ex) {
                    return false;
                }
            })
            .collect(Collectors.toList());

        updateSummaryStats(filteredExpenses);
        updateCategoryPieChart(filteredExpenses);
        updateCategoryBarChart(filteredExpenses);
        updateTrendLineChart(filteredExpenses, startDate, today);
        updateCumulativeAreaChart(filteredExpenses, startDate, today);
        updateWeeklyBarChart(filteredExpenses);
        updateDayOfWeekChart(filteredExpenses);
    }

    private void updateSummaryStats(List<Expense> expenses) {
        double total = expenses.stream().mapToDouble(Expense::getAmount).sum();
        int transactionCount = expenses.size();

        Set<String> categories = expenses.stream()
            .map(Expense::getCategory)
            .collect(Collectors.toSet());

        // Find peak spending day
        Map<LocalDate, Double> dailyTotals = new HashMap<>();
        for (Expense e : expenses) {
            try {
                LocalDate date = LocalDate.parse(e.getDate());
                dailyTotals.merge(date, e.getAmount(), Double::sum);
            } catch (Exception ignored) {}
        }

        LocalDate peakDay = null;
        double peakAmount = 0;
        for (Map.Entry<LocalDate, Double> entry : dailyTotals.entrySet()) {
            if (entry.getValue() > peakAmount) {
                peakAmount = entry.getValue();
                peakDay = entry.getKey();
            }
        }

        int daysWithExpenses = dailyTotals.size();
        double avgDaily = daysWithExpenses > 0 ? total / daysWithExpenses : 0;

        totalSpentLabel.setText(String.format("৳%.2f", total));
        avgDailyLabel.setText(String.format("৳%.2f", avgDaily));
        categoryCountLabel.setText(String.valueOf(categories.size()));
        transactionCountLabel.setText(String.valueOf(transactionCount));

        if (peakDay != null) {
            peakDayLabel.setText(peakDay.format(DateTimeFormatter.ofPattern("MMM dd")));
        } else {
            peakDayLabel.setText("-");
        }
    }

    private void updateCategoryPieChart(List<Expense> expenses) {
        categoryPieChart.getData().clear();

        Map<String, Double> categoryTotals = expenses.stream()
            .collect(Collectors.groupingBy(
                e -> Optional.ofNullable(e.getCategory()).orElse("Other"),
                Collectors.summingDouble(Expense::getAmount)
            ));

        categoryTotals.entrySet().stream()
            .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
            .limit(8)
            .forEach(entry -> {
                PieChart.Data slice = new PieChart.Data(
                    entry.getKey() + " (৳" + String.format("%.0f", entry.getValue()) + ")",
                    entry.getValue()
                );
                categoryPieChart.getData().add(slice);
            });
    }

    private void updateCategoryBarChart(List<Expense> expenses) {
        categoryBarChart.getData().clear();

        Map<String, Double> categoryTotals = expenses.stream()
            .collect(Collectors.groupingBy(
                e -> Optional.ofNullable(e.getCategory()).orElse("Other"),
                Collectors.summingDouble(Expense::getAmount)
            ));

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Amount");

        categoryTotals.entrySet().stream()
            .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
            .limit(8)
            .forEach(entry -> {
                series.getData().add(new XYChart.Data<>(entry.getKey(), entry.getValue()));
            });

        categoryBarChart.getData().add(series);
    }

    private void updateTrendLineChart(List<Expense> expenses, LocalDate startDate, LocalDate endDate) {
        trendLineChart.getData().clear();

        Map<LocalDate, Double> dailyTotals = new TreeMap<>();

        // Initialize all dates with 0
        LocalDate current = startDate.isAfter(LocalDate.now().minusDays(365)) ? startDate : LocalDate.now().minusDays(365);
        while (!current.isAfter(endDate)) {
            dailyTotals.put(current, 0.0);
            current = current.plusDays(1);
        }

        // Fill in actual values
        for (Expense e : expenses) {
            try {
                LocalDate date = LocalDate.parse(e.getDate());
                dailyTotals.merge(date, e.getAmount(), Double::sum);
            } catch (Exception ignored) {}
        }

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Daily Spending");

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd");
        int skipFactor = Math.max(1, dailyTotals.size() / 15);
        int i = 0;

        for (Map.Entry<LocalDate, Double> entry : dailyTotals.entrySet()) {
            if (i % skipFactor == 0 || i == dailyTotals.size() - 1) {
                series.getData().add(new XYChart.Data<>(
                    entry.getKey().format(formatter),
                    entry.getValue()
                ));
            }
            i++;
        }

        trendLineChart.getData().add(series);
    }

    private void updateCumulativeAreaChart(List<Expense> expenses, LocalDate startDate, LocalDate endDate) {
        cumulativeAreaChart.getData().clear();

        Map<LocalDate, Double> dailyTotals = new TreeMap<>();

        LocalDate current = startDate.isAfter(LocalDate.now().minusDays(365)) ? startDate : LocalDate.now().minusDays(365);
        while (!current.isAfter(endDate)) {
            dailyTotals.put(current, 0.0);
            current = current.plusDays(1);
        }

        for (Expense e : expenses) {
            try {
                LocalDate date = LocalDate.parse(e.getDate());
                dailyTotals.merge(date, e.getAmount(), Double::sum);
            } catch (Exception ignored) {}
        }

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Cumulative");

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd");
        double cumulative = 0;
        int skipFactor = Math.max(1, dailyTotals.size() / 15);
        int i = 0;

        for (Map.Entry<LocalDate, Double> entry : dailyTotals.entrySet()) {
            cumulative += entry.getValue();
            if (i % skipFactor == 0 || i == dailyTotals.size() - 1) {
                series.getData().add(new XYChart.Data<>(
                    entry.getKey().format(formatter),
                    cumulative
                ));
            }
            i++;
        }

        cumulativeAreaChart.getData().add(series);
    }

    private void updateWeeklyBarChart(List<Expense> expenses) {
        weeklyBarChart.getData().clear();

        Map<Integer, Double> weeklyTotals = new TreeMap<>();
        WeekFields weekFields = WeekFields.of(Locale.getDefault());

        for (Expense e : expenses) {
            try {
                LocalDate date = LocalDate.parse(e.getDate());
                int weekNum = date.get(weekFields.weekOfWeekBasedYear());
                weeklyTotals.merge(weekNum, e.getAmount(), Double::sum);
            } catch (Exception ignored) {}
        }

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Weekly Total");

        weeklyTotals.entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .limit(12)
            .forEach(entry -> {
                series.getData().add(new XYChart.Data<>("W" + entry.getKey(), entry.getValue()));
            });

        weeklyBarChart.getData().add(series);
    }

    private void updateDayOfWeekChart(List<Expense> expenses) {
        dayOfWeekChart.getData().clear();

        Map<DayOfWeek, Double> dayTotals = new EnumMap<>(DayOfWeek.class);
        for (DayOfWeek day : DayOfWeek.values()) {
            dayTotals.put(day, 0.0);
        }

        for (Expense e : expenses) {
            try {
                LocalDate date = LocalDate.parse(e.getDate());
                dayTotals.merge(date.getDayOfWeek(), e.getAmount(), Double::sum);
            } catch (Exception ignored) {}
        }

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("By Day");

        String[] dayNames = {"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"};
        DayOfWeek[] days = {DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
                           DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY, DayOfWeek.SUNDAY};

        for (int i = 0; i < days.length; i++) {
            series.getData().add(new XYChart.Data<>(dayNames[i], dayTotals.get(days[i])));
        }

        dayOfWeekChart.getData().add(series);
    }

    @FXML
    private void handleBack() {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                getClass().getResource("/fxml/personal_analytics.fxml"));
            javafx.scene.Parent root = loader.load();
            javafx.stage.Stage stage = (javafx.stage.Stage) backButton.getScene().getWindow();
            javafx.scene.Scene scene = new javafx.scene.Scene(root);
            scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
            stage.setScene(scene);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

