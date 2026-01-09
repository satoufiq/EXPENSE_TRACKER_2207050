package org.example.controller;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import org.example.model.Expense;
import org.example.model.GroupMember;
import org.example.service.ExpenseService;
import org.example.service.GroupService;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Controller for Group Member Comparison Analytics
 * Allows comparing expenses between two group members
 */
public class GroupCompareAnalyticsController {

    @FXML private Label titleLabel;
    @FXML private ComboBox<MemberItem> member1Combo;
    @FXML private ComboBox<MemberItem> member2Combo;
    @FXML private ComboBox<String> timeRangeCombo;
    @FXML private Button backButton;
    
    // Summary Labels
    @FXML private Label member1NameLabel;
    @FXML private Label member1TotalLabel;
    @FXML private Label member1CountLabel;
    @FXML private Label member1AvgLabel;
    
    @FXML private Label member2NameLabel;
    @FXML private Label member2TotalLabel;
    @FXML private Label member2CountLabel;
    @FXML private Label member2AvgLabel;
    
    @FXML private Label differenceLabel;
    @FXML private Label differenceDescLabel;
    
    @FXML private Label pie1Label;
    @FXML private Label pie2Label;
    
    // Charts
    @FXML private PieChart member1PieChart;
    @FXML private PieChart member2PieChart;
    @FXML private BarChart<String, Number> categoryCompareChart;
    @FXML private LineChart<String, Number> trendCompareChart;

    @FXML private ListView<String> insightsList;
    
    private String groupId;
    private String userId;
    private String groupName;
    private List<Expense> allExpenses;
    private int selectedDays = 30;
    
    // Helper class for combo box items
    public static class MemberItem {
        private final String oderId;
        private final String name;
        
        public MemberItem(String oderId, String name) {
            this.oderId = oderId;
            this.name = name;
        }
        
        public String getUserId() { return oderId; }
        public String getName() { return name; }
        
        @Override
        public String toString() { return name; }
    }

    @FXML
    public void initialize() {
        // Setup time range combo
        timeRangeCombo.setItems(FXCollections.observableArrayList(
            "Last 7 Days", "Last 30 Days", "Last 90 Days", "This Year", "All Time"
        ));
        timeRangeCombo.setValue("Last 30 Days");
    }
    
    public void initWithGroup(String groupId, String oderId, String groupName) {
        this.groupId = groupId;
        this.userId = oderId;
        this.groupName = groupName;
        
        if (titleLabel != null) {
            titleLabel.setText("⚖️ Member Comparison - " + groupName);
        }
        
        loadMembers();
        loadExpenses();
    }
    
    private void loadMembers() {
        List<GroupMember> members = GroupService.getGroupMembersWithDetails(groupId);
        List<MemberItem> items = members.stream()
            .map(m -> new MemberItem(m.getUserId(), m.getName()))
            .collect(Collectors.toList());
        
        member1Combo.setItems(FXCollections.observableArrayList(items));
        member2Combo.setItems(FXCollections.observableArrayList(items));
        
        // Pre-select if there are at least 2 members
        if (items.size() >= 2) {
            member1Combo.setValue(items.get(0));
            member2Combo.setValue(items.get(1));
        }
    }
    
    private void loadExpenses() {
        allExpenses = ExpenseService.getGroupExpensesObservable(groupId);
    }
    
    private List<Expense> filterExpenses(List<Expense> expenses, String memberId) {
        LocalDate today = LocalDate.now();
        LocalDate startDate = getStartDate();
        
        return expenses.stream()
            .filter(e -> {
                if (memberId != null && !memberId.equals(e.getUserId())) return false;
                try {
                    LocalDate date = LocalDate.parse(e.getDate());
                    return !date.isBefore(startDate) && !date.isAfter(today);
                } catch (Exception ex) {
                    return false;
                }
            })
            .collect(Collectors.toList());
    }
    
    private LocalDate getStartDate() {
        String selected = timeRangeCombo.getValue();
        if (selected == null) selected = "Last 30 Days";
        
        switch (selected) {
            case "Last 7 Days": selectedDays = 7; break;
            case "Last 30 Days": selectedDays = 30; break;
            case "Last 90 Days": selectedDays = 90; break;
            case "This Year": selectedDays = 365; break;
            case "All Time": selectedDays = Integer.MAX_VALUE; break;
            default: selectedDays = 30;
        }
        
        return selectedDays == Integer.MAX_VALUE ? 
            LocalDate.MIN : LocalDate.now().minusDays(selectedDays - 1);
    }
    
    @FXML
    private void handleCompare() {
        MemberItem member1 = member1Combo.getValue();
        MemberItem member2 = member2Combo.getValue();
        
        if (member1 == null || member2 == null) {
            showAlert("Please select both members to compare.");
            return;
        }
        
        if (member1.getUserId().equals(member2.getUserId())) {
            showAlert("Please select two different members to compare.");
            return;
        }
        
        List<Expense> member1Expenses = filterExpenses(allExpenses, member1.getUserId());
        List<Expense> member2Expenses = filterExpenses(allExpenses, member2.getUserId());
        
        updateSummaryStats(member1, member1Expenses, member2, member2Expenses);
        updatePieCharts(member1, member1Expenses, member2, member2Expenses);
        updateCategoryCompareChart(member1, member1Expenses, member2, member2Expenses);
        updateTrendCompareChart(member1, member1Expenses, member2, member2Expenses);
        generateInsights(member1, member1Expenses, member2, member2Expenses);
    }
    
    private void updateSummaryStats(MemberItem m1, List<Expense> exp1, MemberItem m2, List<Expense> exp2) {
        double total1 = exp1.stream().mapToDouble(Expense::getAmount).sum();
        double total2 = exp2.stream().mapToDouble(Expense::getAmount).sum();
        int count1 = exp1.size();
        int count2 = exp2.size();
        double avg1 = count1 > 0 ? total1 / count1 : 0;
        double avg2 = count2 > 0 ? total2 / count2 : 0;
        
        member1NameLabel.setText(m1.getName());
        member1TotalLabel.setText(String.format("৳%.2f", total1));
        member1CountLabel.setText(String.valueOf(count1));
        member1AvgLabel.setText(String.format("৳%.2f", avg1));
        
        member2NameLabel.setText(m2.getName());
        member2TotalLabel.setText(String.format("৳%.2f", total2));
        member2CountLabel.setText(String.valueOf(count2));
        member2AvgLabel.setText(String.format("৳%.2f", avg2));
        
        double diff = Math.abs(total1 - total2);
        differenceLabel.setText(String.format("৳%.2f", diff));
        
        if (total1 > total2) {
            differenceDescLabel.setText(m1.getName() + " spends more");
            differenceLabel.setStyle("-fx-font-size: 24px; -fx-text-fill: #4CAF50;");
        } else if (total2 > total1) {
            differenceDescLabel.setText(m2.getName() + " spends more");
            differenceLabel.setStyle("-fx-font-size: 24px; -fx-text-fill: #2196F3;");
        } else {
            differenceDescLabel.setText("Equal spending");
            differenceLabel.setStyle("-fx-font-size: 24px; -fx-text-fill: #FFFFFF;");
        }
        
        pie1Label.setText("🥧 " + m1.getName() + " Categories");
        pie2Label.setText("🥧 " + m2.getName() + " Categories");
    }
    
    private void updatePieCharts(MemberItem m1, List<Expense> exp1, MemberItem m2, List<Expense> exp2) {
        // Member 1 Pie Chart
        member1PieChart.getData().clear();
        Map<String, Double> cat1 = exp1.stream()
            .collect(Collectors.groupingBy(
                e -> Optional.ofNullable(e.getCategory()).orElse("Other"),
                Collectors.summingDouble(Expense::getAmount)
            ));
        
        cat1.entrySet().stream()
            .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
            .limit(6)
            .forEach(entry -> {
                member1PieChart.getData().add(new PieChart.Data(
                    entry.getKey(),
                    entry.getValue()
                ));
            });
        
        // Member 2 Pie Chart
        member2PieChart.getData().clear();
        Map<String, Double> cat2 = exp2.stream()
            .collect(Collectors.groupingBy(
                e -> Optional.ofNullable(e.getCategory()).orElse("Other"),
                Collectors.summingDouble(Expense::getAmount)
            ));
        
        cat2.entrySet().stream()
            .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
            .limit(6)
            .forEach(entry -> {
                member2PieChart.getData().add(new PieChart.Data(
                    entry.getKey(),
                    entry.getValue()
                ));
            });
    }
    
    private void updateCategoryCompareChart(MemberItem m1, List<Expense> exp1, MemberItem m2, List<Expense> exp2) {
        categoryCompareChart.getData().clear();
        
        Map<String, Double> cat1 = exp1.stream()
            .collect(Collectors.groupingBy(
                e -> Optional.ofNullable(e.getCategory()).orElse("Other"),
                Collectors.summingDouble(Expense::getAmount)
            ));
        
        Map<String, Double> cat2 = exp2.stream()
            .collect(Collectors.groupingBy(
                e -> Optional.ofNullable(e.getCategory()).orElse("Other"),
                Collectors.summingDouble(Expense::getAmount)
            ));
        
        // Get all categories
        Set<String> allCategories = new HashSet<>();
        allCategories.addAll(cat1.keySet());
        allCategories.addAll(cat2.keySet());
        
        // Sort by total spending
        List<String> sortedCategories = allCategories.stream()
            .sorted((a, b) -> Double.compare(
                cat1.getOrDefault(b, 0.0) + cat2.getOrDefault(b, 0.0),
                cat1.getOrDefault(a, 0.0) + cat2.getOrDefault(a, 0.0)
            ))
            .limit(8)
            .collect(Collectors.toList());
        
        XYChart.Series<String, Number> series1 = new XYChart.Series<>();
        series1.setName(m1.getName());
        
        XYChart.Series<String, Number> series2 = new XYChart.Series<>();
        series2.setName(m2.getName());
        
        for (String category : sortedCategories) {
            series1.getData().add(new XYChart.Data<>(category, cat1.getOrDefault(category, 0.0)));
            series2.getData().add(new XYChart.Data<>(category, cat2.getOrDefault(category, 0.0)));
        }
        
        categoryCompareChart.getData().addAll(series1, series2);
    }
    
    private void updateTrendCompareChart(MemberItem m1, List<Expense> exp1, MemberItem m2, List<Expense> exp2) {
        trendCompareChart.getData().clear();
        
        LocalDate today = LocalDate.now();
        LocalDate startDate = getStartDate();
        if (startDate.isBefore(today.minusDays(365))) {
            startDate = today.minusDays(365);
        }
        
        Map<LocalDate, Double> daily1 = new TreeMap<>();
        Map<LocalDate, Double> daily2 = new TreeMap<>();
        
        // Initialize all dates
        LocalDate current = startDate;
        while (!current.isAfter(today)) {
            daily1.put(current, 0.0);
            daily2.put(current, 0.0);
            current = current.plusDays(1);
        }
        
        // Fill values
        for (Expense e : exp1) {
            try {
                LocalDate date = LocalDate.parse(e.getDate());
                daily1.merge(date, e.getAmount(), Double::sum);
            } catch (Exception ignored) {}
        }
        
        for (Expense e : exp2) {
            try {
                LocalDate date = LocalDate.parse(e.getDate());
                daily2.merge(date, e.getAmount(), Double::sum);
            } catch (Exception ignored) {}
        }
        
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd");
        int skipFactor = Math.max(1, daily1.size() / 12);
        
        XYChart.Series<String, Number> series1 = new XYChart.Series<>();
        series1.setName(m1.getName());
        
        XYChart.Series<String, Number> series2 = new XYChart.Series<>();
        series2.setName(m2.getName());
        
        int i = 0;
        for (LocalDate date : daily1.keySet()) {
            if (i % skipFactor == 0 || i == daily1.size() - 1) {
                String label = date.format(formatter);
                series1.getData().add(new XYChart.Data<>(label, daily1.get(date)));
                series2.getData().add(new XYChart.Data<>(label, daily2.get(date)));
            }
            i++;
        }
        
        trendCompareChart.getData().addAll(series1, series2);
    }
    
    private void generateInsights(MemberItem m1, List<Expense> exp1, MemberItem m2, List<Expense> exp2) {
        List<String> insights = new ArrayList<>();
        
        double total1 = exp1.stream().mapToDouble(Expense::getAmount).sum();
        double total2 = exp2.stream().mapToDouble(Expense::getAmount).sum();
        
        // Total spending comparison
        if (total1 > total2 && total2 > 0) {
            double ratio = total1 / total2;
            insights.add(String.format("💰 %s spends %.1fx more than %s overall", 
                m1.getName(), ratio, m2.getName()));
        } else if (total2 > total1 && total1 > 0) {
            double ratio = total2 / total1;
            insights.add(String.format("💰 %s spends %.1fx more than %s overall", 
                m2.getName(), ratio, m1.getName()));
        } else if (total1 == total2 && total1 > 0) {
            insights.add("⚖️ Both members have equal total spending!");
        }
        
        // Transaction frequency
        if (exp1.size() > exp2.size() * 1.5) {
            insights.add(String.format("📈 %s makes more frequent transactions (%d vs %d)", 
                m1.getName(), exp1.size(), exp2.size()));
        } else if (exp2.size() > exp1.size() * 1.5) {
            insights.add(String.format("📈 %s makes more frequent transactions (%d vs %d)", 
                m2.getName(), exp2.size(), exp1.size()));
        }
        
        // Average transaction comparison
        double avg1 = exp1.isEmpty() ? 0 : total1 / exp1.size();
        double avg2 = exp2.isEmpty() ? 0 : total2 / exp2.size();
        
        if (avg1 > avg2 * 1.5 && avg2 > 0) {
            insights.add(String.format("💵 %s has higher average transaction amount (৳%.2f vs ৳%.2f)", 
                m1.getName(), avg1, avg2));
        } else if (avg2 > avg1 * 1.5 && avg1 > 0) {
            insights.add(String.format("💵 %s has higher average transaction amount (৳%.2f vs ৳%.2f)", 
                m2.getName(), avg2, avg1));
        }
        
        // Category comparison
        Map<String, Double> cat1 = exp1.stream()
            .collect(Collectors.groupingBy(
                e -> Optional.ofNullable(e.getCategory()).orElse("Other"),
                Collectors.summingDouble(Expense::getAmount)
            ));
        
        Map<String, Double> cat2 = exp2.stream()
            .collect(Collectors.groupingBy(
                e -> Optional.ofNullable(e.getCategory()).orElse("Other"),
                Collectors.summingDouble(Expense::getAmount)
            ));
        
        // Find top categories for each
        Optional<String> top1 = cat1.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey);
        
        Optional<String> top2 = cat2.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey);
        
        if (top1.isPresent()) {
            insights.add(String.format("🏷️ %s's top category: %s (৳%.2f)", 
                m1.getName(), top1.get(), cat1.get(top1.get())));
        }
        
        if (top2.isPresent()) {
            insights.add(String.format("🏷️ %s's top category: %s (৳%.2f)", 
                m2.getName(), top2.get(), cat2.get(top2.get())));
        }
        
        // Find categories where one spends significantly more
        Set<String> allCategories = new HashSet<>();
        allCategories.addAll(cat1.keySet());
        allCategories.addAll(cat2.keySet());
        
        for (String cat : allCategories) {
            double amount1 = cat1.getOrDefault(cat, 0.0);
            double amount2 = cat2.getOrDefault(cat, 0.0);
            
            if (amount1 > amount2 * 3 && amount2 > 0) {
                insights.add(String.format("📊 %s spends significantly more on %s", m1.getName(), cat));
            } else if (amount2 > amount1 * 3 && amount1 > 0) {
                insights.add(String.format("📊 %s spends significantly more on %s", m2.getName(), cat));
            }
        }
        

        if (insights.isEmpty()) {
            insights.add("📊 Add more expenses to see detailed comparison insights");
        }
        
        insightsList.setItems(FXCollections.observableArrayList(insights));
    }
    
    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Warning");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    @FXML
    private void handleBack() {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                getClass().getResource("/fxml/group_visual_analytics.fxml"));
            javafx.scene.Parent root = loader.load();
            GroupVisualAnalyticsController controller = loader.getController();
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
}

