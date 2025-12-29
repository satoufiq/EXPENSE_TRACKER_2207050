package org.example.controller;

import javafx.fxml.FXML;
import javafx.scene.chart.PieChart;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import org.example.model.Expense;
import org.example.service.ExpenseService;
import org.example.service.GroupService;
import org.example.service.UserService;

import java.time.LocalDate;
import java.util.*;

public class GroupAnalyticsController {

    @FXML private Label titleLabel;
    @FXML private Label totalLabel;
    @FXML private Label monthLabel;
    @FXML private Label membersLabel;
    @FXML private PieChart categoryPieChart;
    @FXML private ListView<String> memberSpendList;

    private String groupId;
    private String userId;
    private String groupName;

    @FXML
    public void initialize() {

    }

    public void initWithGroup(String groupId, String userId, String groupName) {
        this.groupId = groupId;
        this.userId = userId;
        this.groupName = groupName;
        if (titleLabel != null) titleLabel.setText("📊 Group Analytics - " + groupName);
        loadData();
    }

    private void loadData() {
        if (groupId == null) return;
        List<Expense> expenses = ExpenseService.getGroupExpensesObservable(groupId);

        double total = 0.0, monthTotal = 0.0;
        LocalDate now = LocalDate.now();
        Map<String, Double> categoryTotals = new HashMap<>();
        Map<String, Double> memberMonthTotals = new HashMap<>(); // userId -> amount

        for (Expense e : expenses) {
            total += e.getAmount();
            try {
                LocalDate d = LocalDate.parse(e.getDate());
                if (d.getYear() == now.getYear() && d.getMonth() == now.getMonth()) {
                    monthTotal += e.getAmount();
                    categoryTotals.merge(Optional.ofNullable(e.getCategory()).orElse("Other"), e.getAmount(), Double::sum);
                    if (e.getUserId() != null) {
                        memberMonthTotals.merge(e.getUserId(), e.getAmount(), Double::sum);
                    }
                }
            } catch (Exception ignored) {}
        }

        if (totalLabel != null) totalLabel.setText(String.format("৳%.2f", total));
        if (monthLabel != null) monthLabel.setText(String.format("৳%.2f", monthTotal));
        if (membersLabel != null) membersLabel.setText(String.valueOf(GroupService.getMemberCount(groupId)));

        if (categoryPieChart != null) {
            categoryPieChart.getData().clear();
            categoryTotals.entrySet().stream()
                .sorted((a,b)->Double.compare(b.getValue(), a.getValue()))
                .forEach(en -> categoryPieChart.getData().add(new PieChart.Data(en.getKey(), en.getValue())));
        }

        if (memberSpendList != null) {
            List<String> items = new ArrayList<>();
            memberMonthTotals.entrySet().stream()
                .sorted((a,b)->Double.compare(b.getValue(), a.getValue()))
                .forEach(en -> {
                    var user = UserService.getUserById(en.getKey());
                    String name = user != null ? user.getName() : en.getKey();
                    items.add(String.format("%s: ৳%.2f", name, en.getValue()));
                });
            memberSpendList.getItems().setAll(items);
        }
    }


    @FXML
    private void handleVisualAnalytics() {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/fxml/group_visual_analytics.fxml"));
            javafx.scene.Parent root = loader.load();
            GroupVisualAnalyticsController controller = loader.getController();
            if (controller != null) {
                controller.initWithGroup(groupId, userId, groupName);
            }
            javafx.stage.Stage stage = (javafx.stage.Stage) titleLabel.getScene().getWindow();
            javafx.scene.Scene scene = new javafx.scene.Scene(root);
            scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
            stage.setScene(scene);
        } catch (Exception e) { e.printStackTrace(); }
    }

    @FXML
    private void handleCompareMembers() {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/fxml/group_member_analytics.fxml"));
            javafx.scene.Parent root = loader.load();
            Object controller = loader.getController();
            if (controller != null) {
                try { var m = controller.getClass().getMethod("initWithGroup", String.class, String.class, String.class); m.invoke(controller, groupId, userId, groupName);} catch (Exception ignored) {}
            }
            javafx.stage.Stage stage = (javafx.stage.Stage) titleLabel.getScene().getWindow();
            javafx.scene.Scene scene = new javafx.scene.Scene(root);
            scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
            stage.setScene(scene);
        } catch (Exception e) { e.printStackTrace(); }
    }

    @FXML
    private void handleBack() {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/fxml/group_dashboard.fxml"));
            javafx.scene.Parent root = loader.load();
            Object controller = loader.getController();
            if (controller != null) {
                try { var m = controller.getClass().getMethod("initWithGroup", String.class, String.class, String.class); m.invoke(controller, groupId, userId, groupName);} catch (Exception ignored) {}
            }
            javafx.stage.Stage stage = (javafx.stage.Stage) titleLabel.getScene().getWindow();
            stage.setScene(new javafx.scene.Scene(root));
        } catch (Exception e) { e.printStackTrace(); }
    }
}