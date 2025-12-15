package org.example.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import org.example.model.Expense;
import org.example.service.ExpenseService;
import org.example.service.UserService;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class GroupVisualAnalyticsController {

    @FXML private Label titleLabel;
    @FXML private Label subtitleLabel;
    @FXML private Button backButton;

    @FXML private BarChart<String, Number> categoryChart;
    @FXML private LineChart<String, Number> trendChart;
    @FXML private BarChart<Number, String> topMembersChart;
    @FXML private BarChart<String, Number> compareChart;

    @FXML private ComboBox<String> member1Combo;
    @FXML private ComboBox<String> member2Combo;

    private String groupId;
    private String userId;
    private String groupName;

    @FXML
    public void initialize() {
        // Initialize combo boxes and charts
        if (member1Combo != null) {
            member1Combo.setItems(FXCollections.observableArrayList());
        }
        if (member2Combo != null) {
            member2Combo.setItems(FXCollections.observableArrayList());
        }
    }

    public void initWithGroup(String groupId, String userId, String groupName) {
        this.groupId = groupId;
        this.userId = userId;
        this.groupName = groupName;

        if (titleLabel != null && groupName != null) {
            titleLabel.setText("📊 Visual Analytics — " + groupName);
        }

        loadData();
    }

    private void loadData() {
        if (groupId == null || groupId.isEmpty()) {
            return;
        }

        try {
            List<Expense> expenses = ExpenseService.getGroupExpensesObservable(groupId);

            // Get member names for combo boxes
            Set<String> memberIds = new HashSet<>();
            for (Expense e : expenses) {
                if (e.getUserId() != null && !e.getUserId().isEmpty()) {
                    memberIds.add(e.getUserId());
                }
            }

            List<String> memberNames = new ArrayList<>();
            for (String memberId : memberIds) {
                var user = UserService.getUserById(memberId);
                if (user != null) {
                    memberNames.add(user.getName() + " (" + memberId + ")");
                }
            }

            if (member1Combo != null) {
                member1Combo.setItems(FXCollections.observableArrayList(memberNames));
            }
            if (member2Combo != null) {
                member2Combo.setItems(FXCollections.observableArrayList(memberNames));
            }

            // Load charts
            loadCategoryChart(expenses);
            loadTrendChart(expenses);
            loadTopMembersChart(expenses);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadCategoryChart(List<Expense> expenses) {
        if (categoryChart == null) return;

        try {
            categoryChart.getData().clear();

            LocalDate now = LocalDate.now();
            LocalDate monthStart = now.withDayOfMonth(1);

            Map<String, Map<String, Double>> memberCategoryMap = new HashMap<>();

            for (Expense expense : expenses) {
                try {
                    LocalDate expenseDate = LocalDate.parse(expense.getDate());
                    if (!expenseDate.isBefore(monthStart) && !expenseDate.isAfter(now)) {
                        String memberId = expense.getUserId() != null ? expense.getUserId() : "Unknown";
                        String category = expense.getCategory() != null ? expense.getCategory() : "Other";

                        memberCategoryMap.putIfAbsent(memberId, new HashMap<>());
                        memberCategoryMap.get(memberId).merge(category, expense.getAmount(), Double::sum);
                    }
                } catch (Exception ignored) {}
            }

            for (Map.Entry<String, Map<String, Double>> entry : memberCategoryMap.entrySet()) {
                String memberId = entry.getKey();
                var user = UserService.getUserById(memberId);
                String memberName = user != null ? user.getName() : memberId;

                XYChart.Series<String, Number> series = new XYChart.Series<>();
                series.setName(memberName);

                for (Map.Entry<String, Double> catEntry : entry.getValue().entrySet()) {
                    series.getData().add(new XYChart.Data<>(catEntry.getKey(), catEntry.getValue()));
                }

                categoryChart.getData().add(series);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadTrendChart(List<Expense> expenses) {
        if (trendChart == null) return;

        try {
            trendChart.getData().clear();

            LocalDate today = LocalDate.now();
            LocalDate startDate = today.minusDays(29);

            Map<LocalDate, Double> dailyTotals = new TreeMap<>();
            for (int i = 0; i < 30; i++) {
                dailyTotals.put(startDate.plusDays(i), 0.0);
            }

            for (Expense expense : expenses) {
                try {
                    LocalDate expenseDate = LocalDate.parse(expense.getDate());
                    if (!expenseDate.isBefore(startDate) && !expenseDate.isAfter(today)) {
                        dailyTotals.merge(expenseDate, expense.getAmount(), Double::sum);
                    }
                } catch (Exception ignored) {}
            }

            XYChart.Series<String, Number> series = new XYChart.Series<>();
            series.setName("Daily Total");

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd");
            int count = 0;
            for (Map.Entry<LocalDate, Double> entry : dailyTotals.entrySet()) {
                if (count % 5 == 0 || count == dailyTotals.size() - 1) {
                    series.getData().add(new XYChart.Data<>(entry.getKey().format(formatter), entry.getValue()));
                }
                count++;
            }

            trendChart.getData().add(series);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadTopMembersChart(List<Expense> expenses) {
        if (topMembersChart == null) return;

        try {
            topMembersChart.getData().clear();

            LocalDate now = LocalDate.now();
            LocalDate monthStart = now.withDayOfMonth(1);

            Map<String, Double> memberTotals = new HashMap<>();

            for (Expense expense : expenses) {
                try {
                    LocalDate expenseDate = LocalDate.parse(expense.getDate());
                    if (!expenseDate.isBefore(monthStart) && !expenseDate.isAfter(now)) {
                        String memberId = expense.getUserId() != null ? expense.getUserId() : "Unknown";
                        memberTotals.merge(memberId, expense.getAmount(), Double::sum);
                    }
                } catch (Exception ignored) {}
            }

            List<Map.Entry<String, Double>> sortedMembers = new ArrayList<>(memberTotals.entrySet());
            sortedMembers.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));

            XYChart.Series<Number, String> series = new XYChart.Series<>();
            series.setName("Spending");

            int limit = Math.min(5, sortedMembers.size());
            for (int i = 0; i < limit; i++) {
                Map.Entry<String, Double> entry = sortedMembers.get(i);
                var user = UserService.getUserById(entry.getKey());
                String memberName = user != null ? user.getName() : entry.getKey();
                series.getData().add(new XYChart.Data<>(entry.getValue(), memberName));
            }

            topMembersChart.getData().add(series);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleCompare() {
        if (compareChart == null || member1Combo == null || member2Combo == null) return;

        String member1 = member1Combo.getValue();
        String member2 = member2Combo.getValue();

        if (member1 == null || member2 == null) {
            return;
        }

        try {
            String memberId1 = extractMemberId(member1);
            String memberId2 = extractMemberId(member2);

            List<Expense> expenses = ExpenseService.getGroupExpensesObservable(groupId);

            LocalDate now = LocalDate.now();
            LocalDate monthStart = now.withDayOfMonth(1);

            Map<String, Double> member1Categories = new HashMap<>();
            Map<String, Double> member2Categories = new HashMap<>();

            for (Expense expense : expenses) {
                try {
                    LocalDate expenseDate = LocalDate.parse(expense.getDate());
                    if (!expenseDate.isBefore(monthStart) && !expenseDate.isAfter(now)) {
                        String category = expense.getCategory() != null ? expense.getCategory() : "Other";

                        if (memberId1.equals(expense.getUserId())) {
                            member1Categories.merge(category, expense.getAmount(), Double::sum);
                        }
                        if (memberId2.equals(expense.getUserId())) {
                            member2Categories.merge(category, expense.getAmount(), Double::sum);
                        }
                    }
                } catch (Exception ignored) {}
            }

            compareChart.getData().clear();

            XYChart.Series<String, Number> series1 = new XYChart.Series<>();
            var user1 = UserService.getUserById(memberId1);
            series1.setName(user1 != null ? user1.getName() : memberId1);
            for (Map.Entry<String, Double> entry : member1Categories.entrySet()) {
                series1.getData().add(new XYChart.Data<>(entry.getKey(), entry.getValue()));
            }

            XYChart.Series<String, Number> series2 = new XYChart.Series<>();
            var user2 = UserService.getUserById(memberId2);
            series2.setName(user2 != null ? user2.getName() : memberId2);
            for (Map.Entry<String, Double> entry : member2Categories.entrySet()) {
                series2.getData().add(new XYChart.Data<>(entry.getKey(), entry.getValue()));
            }

            compareChart.getData().add(series1);
            compareChart.getData().add(series2);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String extractMemberId(String comboValue) {
        if (comboValue == null) return "";
        int start = comboValue.lastIndexOf('(');
        int end = comboValue.lastIndexOf(')');
        if (start >= 0 && end > start) {
            return comboValue.substring(start + 1, end);
        }
        return comboValue;
    }

    @FXML
    private void handleBack() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/group_dashboard.fxml"));
            javafx.scene.Parent root = loader.load();

            Object controller = loader.getController();
            if (controller != null) {
                try {
                    var method = controller.getClass().getMethod("initWithGroup", String.class, String.class, String.class);
                    method.invoke(controller, groupId, userId, groupName);
                } catch (Exception ignored) {}
            }

            Stage stage = (Stage) backButton.getScene().getWindow();
            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
            stage.setScene(scene);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

