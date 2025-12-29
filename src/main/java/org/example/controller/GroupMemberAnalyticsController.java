package org.example.controller;

import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.example.model.Expense;
import org.example.service.ExpenseService;
import org.example.service.GroupBudgetService;
import org.example.service.UserService;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

public class GroupMemberAnalyticsController {

    @FXML private Label titleLabel;
    @FXML private VBox memberRankingPane;
    @FXML private VBox emptyRankingPane;
    @FXML private VBox suggestionsPane;
    @FXML private VBox tipsPane;

    private String groupId;
    private String oderId;
    private String groupName;

    @FXML
    public void initialize() {
        // Will be initialized in initWithGroup
    }

    public void initWithGroup(String groupId, String oderId, String groupName) {
        this.groupId = groupId;
        this.oderId = oderId;
        this.groupName = groupName;
        if (titleLabel != null) {
            titleLabel.setText("👥 Member Analytics — " + groupName);
        }
        loadMemberRanking();
        loadSuggestions();
        loadTips();
    }

    private void loadMemberRanking() {
        if (memberRankingPane == null) return;
        memberRankingPane.getChildren().clear();

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

        if (totals.isEmpty()) {
            if (emptyRankingPane != null) {
                emptyRankingPane.setVisible(true);
                emptyRankingPane.setManaged(true);
            }
            return;
        }

        if (emptyRankingPane != null) {
            emptyRankingPane.setVisible(false);
            emptyRankingPane.setManaged(false);
        }

        // Sort by amount descending
        List<Map.Entry<String, Double>> sortedEntries = totals.entrySet().stream()
            .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
            .collect(Collectors.toList());

        int rank = 1;
        for (Map.Entry<String, Double> entry : sortedEntries) {
            var user = UserService.getUserById(entry.getKey());
            String name = user != null ? user.getName() : entry.getKey();
            memberRankingPane.getChildren().add(createMemberRankCard(rank, name, entry.getValue()));
            rank++;
        }
    }

    private HBox createMemberRankCard(int rank, String name, double amount) {
        HBox card = new HBox(15);
        card.getStyleClass().add("child-card");
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(15, 20, 15, 20));

        // Rank badge
        String rankEmoji = rank == 1 ? "🥇" : rank == 2 ? "🥈" : rank == 3 ? "🥉" : "#" + rank;
        Label rankLabel = new Label(rankEmoji);
        rankLabel.setStyle("-fx-font-size: 24px; -fx-min-width: 50;");

        // Member info
        VBox infoBox = new VBox(3);
        infoBox.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(infoBox, Priority.ALWAYS);

        Label nameLabel = new Label(name);
        nameLabel.getStyleClass().add("member-name");

        Label rankTextLabel = new Label("Rank #" + rank + " this month");
        rankTextLabel.setStyle("-fx-text-fill: rgba(255,255,255,0.6); -fx-font-size: 11px;");

        infoBox.getChildren().addAll(nameLabel, rankTextLabel);

        // Amount
        Label amountLabel = new Label(String.format("৳%.2f", amount));
        amountLabel.getStyleClass().add("expense-amount");
        amountLabel.setStyle("-fx-font-size: 18px; -fx-text-fill: #7b8ae4; -fx-font-weight: bold;");

        card.getChildren().addAll(rankLabel, infoBox, amountLabel);
        return card;
    }

    private void loadSuggestions() {
        if (suggestionsPane == null) return;
        suggestionsPane.getChildren().clear();

        List<String> suggestions = generateSmartSuggestions();
        for (String suggestion : suggestions) {
            suggestionsPane.getChildren().add(createSuggestionCard(suggestion));
        }
    }

    private VBox createSuggestionCard(String text) {
        VBox card = new VBox(5);
        card.setStyle("-fx-background-color: rgba(255,255,255,0.08); -fx-background-radius: 10; -fx-padding: 12;");

        Label label = new Label(text);
        label.setWrapText(true);
        label.setStyle("-fx-text-fill: rgba(255,255,255,0.9); -fx-font-size: 12px;");

        card.getChildren().add(label);
        return card;
    }

    private void loadTips() {
        if (tipsPane == null) return;
        tipsPane.getChildren().clear();

        List<String> tips = new ArrayList<>();
        tips.add("📊 Review spending patterns regularly to identify trends");
        tips.add("💬 Communicate budget limits clearly to all members");
        tips.add("🎯 Set category-wise spending limits for better control");
        tips.add("📱 Encourage members to log expenses promptly");

        for (String tip : tips) {
            tipsPane.getChildren().add(createTipCard(tip));
        }
    }

    private VBox createTipCard(String text) {
        VBox card = new VBox(5);
        card.setStyle("-fx-background-color: rgba(92, 107, 192, 0.15); -fx-background-radius: 10; -fx-padding: 12;");

        Label label = new Label(text);
        label.setWrapText(true);
        label.setStyle("-fx-text-fill: rgba(255,255,255,0.85); -fx-font-size: 12px;");

        card.getChildren().add(label);
        return card;
    }

    private List<String> generateSmartSuggestions() {
        List<String> suggestions = new ArrayList<>();

        try {
            List<Expense> expenses = ExpenseService.getGroupExpensesObservable(groupId);
            LocalDate now = LocalDate.now();
            LocalDate monthStart = now.withDayOfMonth(1);

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
            }

            if (!categoryTotals.isEmpty()) {
                var topCategory = categoryTotals.entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .orElse(null);
                if (topCategory != null && monthTotal > 0) {
                    double percent = (topCategory.getValue() / monthTotal) * 100;
                    suggestions.add("📊 " + topCategory.getKey() + " is " + String.format("%.1f%%", percent) + " of total spending");
                }
            }

            int memberCount = memberTotals.size();
            if (memberCount > 0 && monthTotal > 0) {
                double avgPerMember = monthTotal / memberCount;
                suggestions.add("👥 Average spending per member: ৳" + String.format("%.2f", avgPerMember));

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

        } catch (Exception e) {
            suggestions.add("💡 Add more expenses to see personalized insights");
        }

        if (suggestions.isEmpty()) {
            suggestions.add("📈 Start tracking group expenses to get smart suggestions");
        }

        return suggestions;
    }

    @FXML
    private void handleBack() {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                getClass().getResource("/fxml/group_dashboard.fxml"));
            javafx.scene.Parent root = loader.load();

            Object controller = loader.getController();
            if (controller != null) {
                try {
                    var method = controller.getClass().getMethod("initWithGroup", String.class, String.class, String.class);
                    method.invoke(controller, groupId, oderId, groupName);
                } catch (Exception ignored) {}
            }

            javafx.scene.Scene scene = new javafx.scene.Scene(root);
            var css = getClass().getResource("/css/styles.css");
            if (css != null) scene.getStylesheets().add(css.toExternalForm());
            org.example.MainApp.getPrimaryStage().setScene(scene);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

