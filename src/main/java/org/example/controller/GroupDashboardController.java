package org.example.controller;

import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.example.model.Expense;
import org.example.service.ExpenseService;
import org.example.service.GroupBudgetService;
import org.example.service.GroupService;
import org.example.service.UserService;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class GroupDashboardController {

    @FXML private Label groupNameLabel;
    @FXML private Label memberCountLabel;
    @FXML private Label memberCountStatLabel;
    @FXML private Label totalExpensesLabel;
    @FXML private Label monthExpensesLabel;
    @FXML private Label groupBudgetLabel;
    @FXML private FlowPane expenseCardsPane;
    @FXML private VBox emptyStatePane;
    @FXML private Button alertButton;
    @FXML private Label alertBadge;
    @FXML private HBox alertButtonContainer;

    private final Map<String, String> userNameCache = new HashMap<>();
    private String currentGroupId;
    private String currentUserId;
    private String currentGroupName;
    private ObservableList<Expense> expensesList;

    @FXML
    public void initialize() {
        // Initialize will be called before initWithGroup
    }

    public void initWithGroup(String groupId, String oderId, String groupName) {
        this.currentGroupId = groupId;
        this.currentUserId = oderId;
        this.currentGroupName = groupName;

        groupNameLabel.setText(groupName);

        loadMembersCount();
        loadExpenses();
        updateAlertCount();

        double budget = GroupBudgetService.getMonthlyBudget(groupId);
        if (groupBudgetLabel != null) {
            groupBudgetLabel.setText(String.format("৳%.2f", budget));
        }
    }

    private void updateAlertCount() {
        if (alertBadge != null && alertButton != null && currentUserId != null) {
            int count = org.example.service.ParentChildAlertService.getUnreadAlertCount(currentUserId);
            count += org.example.service.InviteService.getPendingParentInvitesForChild(currentUserId).size();
            count += org.example.service.InviteService.getPendingGroupInvitesForUser(currentUserId).size();

            if (count > 0) {
                alertBadge.setText(String.valueOf(count));
                alertBadge.setVisible(true);
                alertBadge.setManaged(true);
                alertButton.getStyleClass().add("alert-button-active");
            } else {
                alertBadge.setVisible(false);
                alertBadge.setManaged(false);
                alertButton.getStyleClass().remove("alert-button-active");
            }
        }
    }

    @FXML
    private void handleViewAlerts() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/alerts.fxml"));
            javafx.scene.Parent root = loader.load();
            javafx.scene.Scene scene = new javafx.scene.Scene(root);
            scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
            Stage stage = (Stage) groupNameLabel.getScene().getWindow();
            stage.setScene(scene);
        } catch (Exception e) {
            e.printStackTrace();
            showError("Failed to open alerts: " + e.getMessage());
        }
    }

    @Deprecated
    public void initializeWithGroup(String groupId, String oderId) {
        initWithGroup(groupId, oderId, "Group");
    }

    private void loadMembersCount() {
        if (currentGroupId == null || currentGroupId.isEmpty()) {
            memberCountLabel.setText("Members: 0");
            if (memberCountStatLabel != null) memberCountStatLabel.setText("0");
            return;
        }

        var members = GroupService.getGroupMembersObservable(currentGroupId);
        int count = members.size();
        memberCountLabel.setText("Members: " + count);
        if (memberCountStatLabel != null) memberCountStatLabel.setText(String.valueOf(count));
    }

    private void loadExpenses() {
        if (currentGroupId == null || currentGroupId.isEmpty()) {
            updateExpensesSummary(0, 0);
            return;
        }

        expensesList = ExpenseService.getGroupExpensesObservable(currentGroupId);
        refreshExpenseCards();

        double total = 0;
        double monthTotal = 0;
        for (Expense exp : expensesList) {
            total += exp.getAmount();
            if (isCurrentMonth(exp.getDate())) {
                monthTotal += exp.getAmount();
            }
        }

        updateExpensesSummary(total, monthTotal);

        expensesList.addListener((ListChangeListener<Expense>) change -> {
            refreshExpenseCards();
            double t = 0, m = 0;
            for (Expense e : expensesList) {
                t += e.getAmount();
                if (isCurrentMonth(e.getDate())) m += e.getAmount();
            }
            updateExpensesSummary(t, m);
        });
    }

    private void refreshExpenseCards() {
        if (expenseCardsPane == null) return;

        expenseCardsPane.getChildren().clear();

        if (expensesList == null || expensesList.isEmpty()) {
            if (emptyStatePane != null) {
                emptyStatePane.setVisible(true);
                emptyStatePane.setManaged(true);
            }
            return;
        }

        if (emptyStatePane != null) {
            emptyStatePane.setVisible(false);
            emptyStatePane.setManaged(false);
        }

        // Show recent 12 expenses
        int limit = Math.min(12, expensesList.size());
        for (int i = 0; i < limit; i++) {
            Expense expense = expensesList.get(i);
            expenseCardsPane.getChildren().add(createExpenseCard(expense));
        }
    }

    private VBox createExpenseCard(Expense expense) {
        VBox card = new VBox(10);
        card.getStyleClass().add("expense-card");
        card.setPrefWidth(340);
        card.setMinWidth(340);
        card.setMaxWidth(340);

        // Top row: Category badge and member name
        HBox topRow = new HBox(10);
        topRow.setAlignment(Pos.CENTER_LEFT);

        Label categoryBadge = new Label(getCategoryEmoji(expense.getCategory()) + " " + expense.getCategory());
        categoryBadge.getStyleClass().add("expense-category-badge");

        // Member name
        String memberName = getMemberName(expense.getUserId());
        Label memberLabel = new Label("👤 " + memberName);
        memberLabel.setStyle("-fx-text-fill: rgba(255,255,255,0.7); -fx-font-size: 11px;");

        topRow.getChildren().addAll(categoryBadge, memberLabel);

        // Amount and date row
        HBox amountRow = new HBox(15);
        amountRow.setAlignment(Pos.CENTER_LEFT);

        Label amountLabel = new Label(String.format("৳%.2f", expense.getAmount()));
        amountLabel.getStyleClass().add("expense-amount");

        Label dateLabel = new Label("📅 " + expense.getDate());
        dateLabel.getStyleClass().add("expense-date");

        amountRow.getChildren().addAll(amountLabel, dateLabel);

        // Note
        String noteText = expense.getNote() != null && !expense.getNote().isEmpty()
            ? expense.getNote()
            : "No note";
        Label noteLabel = new Label(noteText);
        noteLabel.getStyleClass().add("expense-note");
        noteLabel.setWrapText(true);
        noteLabel.setMaxWidth(300);

        // Action buttons
        HBox actionsRow = new HBox(10);
        actionsRow.setAlignment(Pos.CENTER_LEFT);

        Button editBtn = new Button("✏️ Edit");
        editBtn.getStyleClass().addAll("card-action-button", "card-edit-button");
        editBtn.setOnAction(e -> handleEditExpense(expense));

        Button deleteBtn = new Button("🗑️ Delete");
        deleteBtn.getStyleClass().addAll("card-action-button", "card-delete-button");
        deleteBtn.setOnAction(e -> handleDeleteExpense(expense));

        actionsRow.getChildren().addAll(editBtn, deleteBtn);

        card.getChildren().addAll(topRow, amountRow, noteLabel, actionsRow);

        return card;
    }

    private String getMemberName(String oderId) {
        if (oderId == null) return "Unknown";
        return userNameCache.computeIfAbsent(oderId, id -> {
            var user = UserService.getUserById(id);
            return user != null ? user.getName() : id;
        });
    }

    private String getCategoryEmoji(String category) {
        if (category == null) return "📦";
        switch (category.toLowerCase()) {
            case "food": return "🍔";
            case "transport": return "🚗";
            case "shopping": return "🛒";
            case "entertainment": return "🎬";
            case "bills": return "📄";
            case "health": case "healthcare": return "💊";
            case "education": return "📚";
            case "groceries": return "🥬";
            case "utilities": return "💡";
            case "rent": return "🏠";
            default: return "📦";
        }
    }

    private boolean isCurrentMonth(String dateStr) {
        try {
            java.time.LocalDate date = java.time.LocalDate.parse(dateStr);
            java.time.LocalDate today = java.time.LocalDate.now();
            return date.getYear() == today.getYear() && date.getMonth() == today.getMonth();
        } catch (Exception e) {
            return false;
        }
    }

    private void updateExpensesSummary(double total, double monthTotal) {
        totalExpensesLabel.setText(String.format("৳%.2f", total));
        monthExpensesLabel.setText(String.format("৳%.2f", monthTotal));
    }

    @FXML
    private void handleAddMember() {
        if (currentGroupId == null) { showError("No group selected!"); return; }

        // Check if user is admin
        if (!GroupService.isAdmin(currentGroupId, currentUserId)) {
            showError("Only group admins can add members.\nAsk an admin to add new members.");
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/add_member.fxml"));
            Stage dialog = new Stage();
            dialog.setTitle("Add Member to " + currentGroupName);
            Scene scene = new Scene(loader.load());
            dialog.setScene(scene);
            dialog.initModality(Modality.WINDOW_MODAL);
            dialog.setResizable(false);
            dialog.setWidth(700);
            dialog.setHeight(500);

            Object controllerObj = loader.getController();
            if (controllerObj != null) {
                try {
                    var method = controllerObj.getClass().getMethod("setGroupContext", String.class, String.class, String.class);
                    method.invoke(controllerObj, currentGroupId, currentGroupName, currentUserId);
                } catch (Exception e) {
                    try {
                        var method2 = controllerObj.getClass().getMethod("setGroupContext", String.class, String.class);
                        method2.invoke(controllerObj, currentGroupId, currentGroupName);
                    } catch (Exception ignored) {}
                }
            }

            dialog.showAndWait();
            loadMembersCount();
            loadExpenses();

        } catch (IOException e) {
            e.printStackTrace();
            showError("Failed to open add member dialog: " + e.getMessage());
        }
    }

    @FXML
    private void handleViewMembers() {
        if (currentGroupId == null) { showError("No group selected!"); return; }
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/view_members.fxml"));
            Scene scene = new Scene(loader.load());

            Object controllerObj = loader.getController();
            if (controllerObj != null) {
                try {
                    var method = controllerObj.getClass().getMethod("initWithGroup", String.class, String.class, String.class);
                    method.invoke(controllerObj, currentGroupId, currentUserId, currentGroupName);
                } catch (Exception ignored) {}
            }

            Stage stage = (Stage) groupNameLabel.getScene().getWindow();
            stage.setScene(scene);

        } catch (IOException e) {
            e.printStackTrace();
            showError("Failed to open members view: " + e.getMessage());
        }
    }

    @FXML
    private void handleAddExpense() {
        if (currentGroupId == null) { showError("No group selected!"); return; }
        try {
            org.example.util.SessionManager.getInstance().setCurrentGroupId(currentGroupId);

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/add_expense.fxml"));
            javafx.scene.Parent root = loader.load();

            Stage stage = new Stage();
            stage.setTitle("Add Expense - " + currentGroupName);
            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
            stage.setScene(scene);
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setResizable(false);
            stage.showAndWait();

            loadExpenses();

        } catch (IOException e) {
            e.printStackTrace();
            showError("Failed to open add expense dialog: " + e.getMessage());
        }
    }

    @FXML
    private void handleBack() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/group_selection.fxml"));
            Scene scene = new Scene(loader.load());
            Stage stage = (Stage) groupNameLabel.getScene().getWindow();
            stage.setScene(scene);
        } catch (IOException e) {
            e.printStackTrace();
            showError("Failed to go back: " + e.getMessage());
        }
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setContentText(message);
        alert.showAndWait();
    }

    @FXML
    private void handleOpenGroupAnalytics() {
        if (currentGroupId == null) { showError("No group selected!"); return; }
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/group_analytics.fxml"));
            javafx.scene.Parent root = loader.load();

            Object controller = loader.getController();
            if (controller != null) {
                try {
                    var m = controller.getClass().getMethod("initWithGroup", String.class, String.class, String.class);
                    m.invoke(controller, currentGroupId, currentUserId, currentGroupName);
                } catch (NoSuchMethodException ignored) {}
            }
            Stage stage = (Stage) groupNameLabel.getScene().getWindow();
            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
            stage.setScene(scene);
        } catch (Exception e) {
            e.printStackTrace();
            showError("Failed to open Group Analytics: " + e.getMessage());
        }
    }

    @FXML
    private void handleSetGroupBudget() {
        if (currentGroupId == null || currentGroupId.isEmpty()) { showError("No group selected!"); return; }
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Set Group Monthly Budget");
        dialog.setHeaderText("Set Monthly Budget for " + currentGroupName);
        dialog.setContentText("Enter budget amount in ৳ (BDT):");
        double current = GroupBudgetService.getMonthlyBudget(currentGroupId);
        if (current > 0) dialog.getEditor().setText(String.format("%.2f", current));
        java.util.Optional<String> result = dialog.showAndWait();
        result.ifPresent(val -> {
            try {
                double amt = Double.parseDouble(val.trim());
                if (amt < 0) { showError("Budget amount cannot be negative."); return; }
                GroupBudgetService.setMonthlyBudget(currentGroupId, amt);
                if (groupBudgetLabel != null) groupBudgetLabel.setText(String.format("৳%.2f", amt));
                org.example.util.AlertUtil.showInfo("Budget Set", String.format("Group budget set to ৳%.2f", amt));
            } catch (NumberFormatException ex) {
                showError("Please enter a valid number.");
            }
        });
    }

    private void handleEditExpense(Expense expense) {
        try {
            Dialog<javafx.util.Pair<String, javafx.util.Pair<String, javafx.util.Pair<Double, String>>>> dialog = new Dialog<>();
            dialog.setTitle("Edit Expense");
            dialog.setHeaderText("Edit expense details");

            ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
            dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

            javafx.scene.layout.GridPane grid = new javafx.scene.layout.GridPane();
            grid.setHgap(10);
            grid.setVgap(10);
            grid.setPadding(new Insets(20, 150, 10, 10));

            ComboBox<String> categoryCombo = new ComboBox<>();
            categoryCombo.getItems().addAll("Food", "Transport", "Entertainment", "Shopping", "Bills", "Healthcare", "Education", "Other");
            categoryCombo.setValue(expense.getCategory());

            TextField amountField = new TextField(String.valueOf(expense.getAmount()));
            DatePicker datePicker = new DatePicker(java.time.LocalDate.parse(expense.getDate()));
            TextField noteField = new TextField(expense.getNote() != null ? expense.getNote() : "");

            grid.add(new Label("Category:"), 0, 0);
            grid.add(categoryCombo, 1, 0);
            grid.add(new Label("Amount (৳):"), 0, 1);
            grid.add(amountField, 1, 1);
            grid.add(new Label("Date:"), 0, 2);
            grid.add(datePicker, 1, 2);
            grid.add(new Label("Note:"), 0, 3);
            grid.add(noteField, 1, 3);

            dialog.getDialogPane().setContent(grid);

            dialog.setResultConverter(dialogButton -> {
                if (dialogButton == saveButtonType) {
                    return new javafx.util.Pair<>(categoryCombo.getValue(),
                            new javafx.util.Pair<>(datePicker.getValue().toString(),
                                    new javafx.util.Pair<>(Double.parseDouble(amountField.getText()), noteField.getText())));
                }
                return null;
            });

            java.util.Optional<javafx.util.Pair<String, javafx.util.Pair<String, javafx.util.Pair<Double, String>>>> result = dialog.showAndWait();

            result.ifPresent(data -> {
                String category = data.getKey();
                String date = data.getValue().getKey();
                Double amount = data.getValue().getValue().getKey();
                String note = data.getValue().getValue().getValue();

                boolean success = ExpenseService.updateExpense(expense.getExpenseId(), category, amount, date, note);

                if (success) {
                    org.example.util.AlertUtil.showInfo("Success", "Expense updated successfully!");
                    loadExpenses();
                } else {
                    showError("Failed to update expense");
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
            showError("Error editing expense: " + e.getMessage());
        }
    }

    private void handleDeleteExpense(Expense expense) {
        try {
            Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
            confirmAlert.setTitle("Confirm Delete");
            confirmAlert.setHeaderText("Delete Expense");
            confirmAlert.setContentText("Are you sure you want to delete this expense?\n\n" +
                    "Category: " + expense.getCategory() + "\n" +
                    "Amount: ৳" + String.format("%.2f", expense.getAmount()) + "\n" +
                    "Date: " + expense.getDate());

            java.util.Optional<ButtonType> result = confirmAlert.showAndWait();

            if (result.isPresent() && result.get() == ButtonType.OK) {
                boolean success = ExpenseService.deleteExpense(expense.getExpenseId());

                if (success) {
                    org.example.util.AlertUtil.showInfo("Success", "Expense deleted successfully!");
                    loadExpenses();
                } else {
                    showError("Failed to delete expense");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            showError("Error deleting expense: " + e.getMessage());
        }
    }
}

