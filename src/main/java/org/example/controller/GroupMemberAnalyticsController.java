package org.example.controller;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import org.example.model.Expense;
import org.example.service.ExpenseService;
import org.example.service.GroupBudgetService;
import org.example.service.UserService;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Group Member Analytics Controller - Shows member spending ranking with tips
 */
public class GroupMemberAnalyticsController {

    @FXML private Label titleLabel;
    @FXML private TableView<MemberRow> rankingTable;
    @FXML private TableColumn<MemberRow, String> nameColumn;
    @FXML private TableColumn<MemberRow, Double> amountColumn;
    @FXML private ListView<String> tipsListView;
    @FXML private ListView<String> suggestionsListView;

    private String groupId;
    private String userId;
    private String groupName;

    public static class MemberRow {
        private final String name;
        private final Double amount;
        public MemberRow(String name, Double amount) { this.name = name; this.amount = amount; }
        public String getName() { return name; }
        public Double getAmount() { return amount; }
    }

    @FXML
    public void initialize() {
        if (nameColumn != null) nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        if (amountColumn != null) {
            amountColumn.setCellValueFactory(new PropertyValueFactory<>("amount"));
            // Format amount column to show Taka currency
            amountColumn.setCellFactory(column -> new javafx.scene.control.TableCell<MemberRow, Double>() {
                @Override
                protected void updateItem(Double amount, boolean empty) {
                    super.updateItem(amount, empty);
                    if (empty || amount == null) {
                        setText(null);
                    } else {
                        setText(String.format("৳%.2f", amount));
                    }
                }
            });
        }
    }

    public void initWithGroup(String groupId, String userId, String groupName) {
        this.groupId = groupId;
        this.userId = userId;
        this.groupName = groupName;
        if (titleLabel != null) titleLabel.setText("👥 Member Analytics — " + groupName);
        loadData();
        loadTipsAndSuggestions();
    }

    private void loadData() {
        List<Expense> expenses = ExpenseService.getGroupExpensesObservable(groupId);
        LocalDate now = LocalDate.now();
        Map<String, Double> totals = new HashMap<>();
        for (Expense e : expenses) {
            try {
                LocalDate d = LocalDate.parse(e.getDate());
                if (d.getYear() == now.getYear() && d.getMonth() == now.getMonth()) {
                    totals.merge(e.getUserId(), e.getAmount(), Double::sum);
                }
            } catch (Exception ignored) {}
        }
        var rows = totals.entrySet().stream()
            .sorted((a,b)->Double.compare(b.getValue(), a.getValue()))
            .map(en -> {
                var user = UserService.getUserById(en.getKey());
                String name = user != null ? user.getName() : en.getKey();
                return new MemberRow(name, en.getValue());
            }).collect(Collectors.toList());
        rankingTable.setItems(FXCollections.observableArrayList(rows));
    }

    private void loadTipsAndSuggestions() {
        // Tips for group leaders
        List<String> tips = new ArrayList<>();
        tips.add("📊 Review spending patterns regularly to identify trends");
        tips.add("💬 Communicate budget limits clearly to all members");
        tips.add("🎯 Set category-wise spending limits for better control");
        tips.add("📱 Encourage members to log expenses promptly");
        tips.add("🔔 Enable alerts for high-spending activities");
        tips.add("📈 Use visual analytics to share insights with the group");
        tips.add("⚖️ Ensure fair distribution of expenses among members");
        tips.add("💡 Hold monthly reviews to discuss spending patterns");

        if (tipsListView != null) {
            tipsListView.setItems(FXCollections.observableArrayList(tips));
        }

        // Generate smart suggestions based on group data
        List<String> suggestions = generateSmartSuggestions();
        if (suggestionsListView != null) {
            suggestionsListView.setItems(FXCollections.observableArrayList(suggestions));
        }
    }

    private List<String> generateSmartSuggestions() {
        List<String> suggestions = new ArrayList<>();

        try {
            List<Expense> expenses = ExpenseService.getGroupExpensesObservable(groupId);
            LocalDate now = LocalDate.now();
            LocalDate monthStart = now.withDayOfMonth(1);

            // Calculate monthly total
            double monthTotal = 0;
            Map<String, Double> categoryTotals = new HashMap<>();
            Map<String, Double> memberTotals = new HashMap<>();

            for (Expense e : expenses) {
                try {
                    LocalDate d = LocalDate.parse(e.getDate());
                    if (!d.isBefore(monthStart) && !d.isAfter(now)) {
                        monthTotal += e.getAmount();
                        categoryTotals.merge(e.getCategory(), e.getAmount(), Double::sum);
                        memberTotals.merge(e.getUserId(), e.getAmount(), Double::sum);
                    }
                } catch (Exception ignored) {}
            }

            // Check budget status
            double groupBudget = GroupBudgetService.getMonthlyBudget(groupId);
            if (groupBudget > 0) {
                double budgetPercent = (monthTotal / groupBudget) * 100;
                if (budgetPercent > 90) {
                    suggestions.add("⚠️ Group spending is at " + String.format("%.1f%%", budgetPercent) + " of budget!");
                } else if (budgetPercent > 75) {
                    suggestions.add("📢 Approaching budget limit (" + String.format("%.1f%%", budgetPercent) + " used)");
                } else {
                    suggestions.add("✅ On track with budget (" + String.format("%.1f%%", budgetPercent) + " used)");
                }
            } else {
                suggestions.add("💵 Set a group budget to track spending limits");
            }

            // Highest spending category
            if (!categoryTotals.isEmpty()) {
                var topCategory = categoryTotals.entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .orElse(null);
                if (topCategory != null) {
                    double percent = (topCategory.getValue() / monthTotal) * 100;
                    suggestions.add("📊 " + topCategory.getKey() + " is " + String.format("%.1f%%", percent) + " of total spending");
                }
            }

            // Member count analysis
            int memberCount = memberTotals.size();
            if (memberCount > 0) {
                double avgPerMember = monthTotal / memberCount;
                suggestions.add("👥 Average spending per member: ৳" + String.format("%.2f", avgPerMember));

                // Check for spending imbalance
                var sortedMembers = memberTotals.values().stream()
                    .sorted(Comparator.reverseOrder())
                    .collect(Collectors.toList());
                if (sortedMembers.size() > 1) {
                    double highest = sortedMembers.get(0);
                    double lowest = sortedMembers.get(sortedMembers.size() - 1);
                    if (highest > lowest * 3) {
                        suggestions.add("⚖️ Significant spending imbalance among members");
                    }
                }
            }

            // Time-based suggestions
            int daysLeft = now.lengthOfMonth() - now.getDayOfMonth();
            if (daysLeft < 7 && monthTotal > 0) {
                double dailyAvg = monthTotal / now.getDayOfMonth();
                double projected = dailyAvg * now.lengthOfMonth();
                suggestions.add("📅 " + daysLeft + " days left, projected: ৳" + String.format("%.2f", projected));
            }

            // Category diversity
            if (categoryTotals.size() <= 2) {
                suggestions.add("📝 Consider tracking more expense categories for better insights");
            }

        } catch (Exception e) {
            suggestions.add("💡 Add more expenses to see personalized insights");
        }

        if (suggestions.isEmpty()) {
            suggestions.add("📈 Start tracking group expenses to get smart suggestions");
            suggestions.add("💡 Regular expense logging helps with better financial planning");
        }

        return suggestions;
    }

    @FXML
    private void handleBack() {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/fxml/group_analytics.fxml"));
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
        } catch (Exception e) { e.printStackTrace(); }
    }
}
