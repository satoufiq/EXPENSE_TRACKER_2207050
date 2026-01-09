package org.example.controller;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import org.example.model.Expense;
import org.example.service.ExpenseService;
import org.example.service.GroupService;
import org.example.service.UserService;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Controller for Group Visual Analytics Dashboard
 * Provides comprehensive charts and visualizations for group expense data
 */
public class GroupVisualAnalyticsController {

    @FXML private Label titleLabel;
    @FXML private Label subtitleLabel;
    @FXML private ComboBox<String> timeRangeCombo;
    @FXML private Button backButton;

    // Summary Labels
    @FXML private Label totalSpentLabel;
    @FXML private Label monthTotalLabel;
    @FXML private Label memberCountLabel;
    @FXML private Label avgPerMemberLabel;
    @FXML private Label transactionCountLabel;

    // Charts
    @FXML private PieChart categoryPieChart;
    @FXML private LineChart<String, Number> trendLineChart;

    private String groupId;
    private String userId;
    private String groupName;
    private List<Expense> allExpenses;
    private int selectedDays = 30;
    private Map<String, String> userIdToName = new HashMap<>();

    @FXML
    public void initialize() {
        // Setup time range combo
        timeRangeCombo.setItems(FXCollections.observableArrayList(
            "Last 7 Days", "Last 30 Days", "Last 90 Days", "This Year", "All Time"
        ));
        timeRangeCombo.setValue("Last 30 Days");
        timeRangeCombo.setOnAction(e -> onTimeRangeChanged());
    }

    public void initWithGroup(String groupId, String userId, String groupName) {
        this.groupId = groupId;
        this.userId = userId;
        this.groupName = groupName;

        if (titleLabel != null) {
            titleLabel.setText("📊 Group Visual Analytics - " + groupName);
        }

        // Load member names
        loadMemberNames();
        loadData();
    }

    private void loadMemberNames() {
        var members = GroupService.getGroupMembersWithDetails(groupId);
        for (var member : members) {
            userIdToName.put(member.getUserId(), member.getName());
        }
    }

    private String getMemberName(String memberId) {
        if (userIdToName.containsKey(memberId)) {
            return userIdToName.get(memberId);
        }
        var user = UserService.getUserById(memberId);
        if (user != null) {
            userIdToName.put(memberId, user.getName());
            return user.getName();
        }
        return memberId;
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
        if (groupId == null) return;

        allExpenses = ExpenseService.getGroupExpensesObservable(groupId);

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

        Platform.runLater(() -> {
            updateSummaryStats(filteredExpenses);
            updateCategoryPieChart(filteredExpenses);
            updateTrendLineChart(filteredExpenses, startDate, today);
        });
    }

    private void updateSummaryStats(List<Expense> expenses) {
        double total = expenses.stream().mapToDouble(Expense::getAmount).sum();
        int transactionCount = expenses.size();

        // This month total
        LocalDate now = LocalDate.now();
        double monthTotal = expenses.stream()
            .filter(e -> {
                try {
                    LocalDate date = LocalDate.parse(e.getDate());
                    return date.getYear() == now.getYear() && date.getMonth() == now.getMonth();
                } catch (Exception ex) {
                    return false;
                }
            })
            .mapToDouble(Expense::getAmount)
            .sum();

        int memberCount = GroupService.getMemberCount(groupId);
        double avgPerMember = memberCount > 0 ? total / memberCount : 0;

        totalSpentLabel.setText(String.format("৳%.2f", total));
        monthTotalLabel.setText(String.format("৳%.2f", monthTotal));
        memberCountLabel.setText(String.valueOf(memberCount));
        avgPerMemberLabel.setText(String.format("৳%.2f", avgPerMember));
        transactionCountLabel.setText(String.valueOf(transactionCount));
    }

    private void updateCategoryPieChart(List<Expense> expenses) {
        if (categoryPieChart == null) return;
        categoryPieChart.getData().clear();

        if (expenses == null || expenses.isEmpty()) return;

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


    private void updateTrendLineChart(List<Expense> expenses, LocalDate startDate, LocalDate endDate) {
        if (trendLineChart == null) return;
        trendLineChart.getData().clear();

        if (expenses == null || expenses.isEmpty()) return;

        // Group expenses by member and date
        Map<String, Map<LocalDate, Double>> memberDailyTotals = new HashMap<>();

        for (Expense e : expenses) {
            try {
                LocalDate date = LocalDate.parse(e.getDate());
                String memberId = e.getUserId();
                if (memberId == null) continue;

                memberDailyTotals
                    .computeIfAbsent(memberId, k -> new TreeMap<>())
                    .merge(date, e.getAmount(), Double::sum);
            } catch (Exception ignored) {}
        }

        // Also create group total line
        Map<LocalDate, Double> groupDailyTotals = new TreeMap<>();
        LocalDate current = startDate.isAfter(LocalDate.now().minusDays(365)) ? startDate : LocalDate.now().minusDays(365);
        while (!current.isAfter(endDate)) {
            groupDailyTotals.put(current, 0.0);
            current = current.plusDays(1);
        }

        for (Expense e : expenses) {
            try {
                LocalDate date = LocalDate.parse(e.getDate());
                groupDailyTotals.merge(date, e.getAmount(), Double::sum);
            } catch (Exception ignored) {}
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd");
        int skipFactor = Math.max(1, groupDailyTotals.size() / 15);

        // Group total line
        XYChart.Series<String, Number> groupSeries = new XYChart.Series<>();
        groupSeries.setName("Group Total");

        int i = 0;
        for (Map.Entry<LocalDate, Double> entry : groupDailyTotals.entrySet()) {
            if (i % skipFactor == 0 || i == groupDailyTotals.size() - 1) {
                groupSeries.getData().add(new XYChart.Data<>(
                    entry.getKey().format(formatter),
                    entry.getValue()
                ));
            }
            i++;
        }

        trendLineChart.getData().add(groupSeries);
    }


    @FXML
    private void handleCompareMembers() {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                getClass().getResource("/fxml/group_compare_analytics.fxml"));
            javafx.scene.Parent root = loader.load();
            GroupCompareAnalyticsController controller = loader.getController();
            if (controller != null) {
                controller.initWithGroup(groupId, userId, groupName);
            }
            javafx.stage.Stage stage = (javafx.stage.Stage) titleLabel.getScene().getWindow();
            javafx.scene.Scene scene = new javafx.scene.Scene(root);
            scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
            stage.setScene(scene);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleBack() {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                getClass().getResource("/fxml/group_analytics.fxml"));
            javafx.scene.Parent root = loader.load();
            Object controller = loader.getController();
            if (controller != null) {
                try {
                    var m = controller.getClass().getMethod("initWithGroup", String.class, String.class, String.class);
                    m.invoke(controller, groupId, userId, groupName);
                } catch (Exception ignored) {}
            }
            javafx.stage.Stage stage = (javafx.stage.Stage) titleLabel.getScene().getWindow();
            javafx.scene.Scene scene = new javafx.scene.Scene(root);
            scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
            stage.setScene(scene);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

