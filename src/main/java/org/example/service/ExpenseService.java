package org.example.service;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.example.model.Expense;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Expense Service with ObservableList support for real-time updates
 * No refresh button needed - UI updates automatically
 */
public class ExpenseService {

    // Observable lists for real-time updates
    private static final ObservableList<Expense> personalExpensesList = FXCollections.observableArrayList();
    private static final ObservableList<Expense> groupExpensesList = FXCollections.observableArrayList();
    private static final ObservableList<Expense> allExpensesList = FXCollections.observableArrayList();

    /**
     * Get personal expenses as ObservableList for real-time UI updates
     */
    public static ObservableList<Expense> getPersonalExpensesObservable(String userId) {
        personalExpensesList.clear();
        List<Expense> expenses = getPersonalExpenses(userId);
        personalExpensesList.addAll(expenses);
        return personalExpensesList;
    }

    /**
     * Get group expenses as ObservableList for real-time UI updates
     */
    public static ObservableList<Expense> getGroupExpensesObservable(String groupId) {
        groupExpensesList.clear();
        List<Expense> expenses = getGroupExpenses(groupId);
        groupExpensesList.addAll(expenses);
        return groupExpensesList;
    }


    public static ObservableList<Expense> getAllExpensesObservable() {
        allExpensesList.clear();
        List<Expense> expenses = getAllExpenses();
        allExpensesList.addAll(expenses);
        return allExpensesList;
    }
    public static boolean addExpense(String userId, String groupId, String category,
                                    double amount, String date, String note) {
        try (Connection conn = DatabaseHelper.getConnection()) {
            String expenseId = UUID.randomUUID().toString();
            String query = "INSERT INTO EXPENSES (expense_id, user_id, group_id, category, amount, date, note) " +
                          "VALUES (?, ?, ?, ?, ?, ?, ?)";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setString(1, expenseId);
            stmt.setString(2, userId);
            stmt.setString(3, groupId);
            stmt.setString(4, category);
            stmt.setDouble(5, amount);
            stmt.setString(6, date);
            stmt.setString(7, note);

            int result = stmt.executeUpdate();

            if (result > 0) {
                Expense newExpense = new Expense(expenseId, userId, groupId, category, amount, date, note);
                // Add to beginning for newest first
                if (groupId == null || groupId.isEmpty()) {
                    personalExpensesList.add(0, newExpense);
                } else {
                    groupExpensesList.add(0, newExpense);
                }
                allExpensesList.add(0, newExpense);
                return true;
            }
            return false;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Update an existing expense and auto-update ObservableLists
     */
    public static boolean updateExpense(String expenseId, String category,
                                       double amount, String date, String note) {
        try (Connection conn = DatabaseHelper.getConnection()) {
            String query = "UPDATE EXPENSES SET category = ?, amount = ?, date = ?, note = ? " +
                          "WHERE expense_id = ?";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setString(1, category);
            stmt.setDouble(2, amount);
            stmt.setString(3, date);
            stmt.setString(4, note);
            stmt.setString(5, expenseId);

            int rowsAffected = stmt.executeUpdate();

            if (rowsAffected > 0) {
                // Update in all ObservableLists
                updateExpenseInList(personalExpensesList, expenseId, category, amount, date, note);
                updateExpenseInList(groupExpensesList, expenseId, category, amount, date, note);
                updateExpenseInList(allExpensesList, expenseId, category, amount, date, note);
                return true;
            }
            return false;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Delete an expense and auto-update ObservableLists
     */
    public static boolean deleteExpense(String expenseId) {
        try (Connection conn = DatabaseHelper.getConnection()) {
            String query = "DELETE FROM EXPENSES WHERE expense_id = ?";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setString(1, expenseId);

            int rowsAffected = stmt.executeUpdate();

            if (rowsAffected > 0) {
                // Remove from all ObservableLists
                personalExpensesList.removeIf(e -> e.getExpenseId().equals(expenseId));
                groupExpensesList.removeIf(e -> e.getExpenseId().equals(expenseId));
                allExpensesList.removeIf(e -> e.getExpenseId().equals(expenseId));
                return true;
            }
            return false;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Get expense by ID
     */
    public static Expense getExpenseById(String expenseId) {
        try (Connection conn = DatabaseHelper.getConnection()) {
            String query = "SELECT * FROM EXPENSES WHERE expense_id = ?";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setString(1, expenseId);

            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return createExpenseFromResultSet(rs);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Get total expenses for a user
     */
    public static double getTotalExpenses(String userId) {
        double total = 0.0;
        try (Connection conn = DatabaseHelper.getConnection()) {
            String query = "SELECT SUM(amount) as total FROM EXPENSES WHERE user_id = ? AND (group_id IS NULL OR group_id = '')";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setString(1, userId);

            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                total = rs.getDouble("total");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return total;
    }

    /**
     * Get total expenses for a group
     */
    public static double getGroupTotalExpenses(String groupId) {
        double total = 0.0;
        try (Connection conn = DatabaseHelper.getConnection()) {
            String query = "SELECT SUM(amount) as total FROM EXPENSES WHERE group_id = ?";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setString(1, groupId);

            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                total = rs.getDouble("total");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return total;
    }

    /**
     * Get all personal expenses for a user
     */
    private static List<Expense> getPersonalExpenses(String userId) {
        List<Expense> expenses = new ArrayList<>();
        try (Connection conn = DatabaseHelper.getConnection()) {
            String query = "SELECT * FROM EXPENSES WHERE user_id = ? AND (group_id IS NULL OR group_id = '') ORDER BY date DESC";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setString(1, userId);

            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                expenses.add(createExpenseFromResultSet(rs));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return expenses;
    }

    /**
     * Get all expenses for a group
     */
    private static List<Expense> getGroupExpenses(String groupId) {
        List<Expense> expenses = new ArrayList<>();
        try (Connection conn = DatabaseHelper.getConnection()) {
            String query = "SELECT * FROM EXPENSES WHERE group_id = ? ORDER BY date DESC";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setString(1, groupId);

            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                expenses.add(createExpenseFromResultSet(rs));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return expenses;
    }

    /**
     * Get all expenses
     */
    private static List<Expense> getAllExpenses() {
        List<Expense> expenses = new ArrayList<>();
        try (Connection conn = DatabaseHelper.getConnection()) {
            String query = "SELECT * FROM EXPENSES ORDER BY date DESC";
            PreparedStatement stmt = conn.prepareStatement(query);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                expenses.add(createExpenseFromResultSet(rs));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return expenses;
    }

    /**
     * Helper method to create Expense from ResultSet
     */
    private static Expense createExpenseFromResultSet(ResultSet rs) throws java.sql.SQLException {
        Expense expense = new Expense();
        expense.setExpenseId(rs.getString("expense_id"));
        expense.setUserId(rs.getString("user_id"));
        expense.setGroupId(rs.getString("group_id"));
        expense.setCategory(rs.getString("category"));
        expense.setAmount(rs.getDouble("amount"));
        expense.setDate(rs.getString("date"));
        expense.setNote(rs.getString("note"));
        return expense;
    }

    /**
     * Helper method to update expense in a list
     */
    private static void updateExpenseInList(ObservableList<Expense> list, String expenseId,
                                           String category, double amount, String date, String note) {
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).getExpenseId().equals(expenseId)) {
                Expense updated = list.get(i);
                updated.setCategory(category);
                updated.setAmount(amount);
                updated.setDate(date);
                updated.setNote(note);
                list.set(i, updated);
                break;
            }
        }
    }

    /**
     * Refresh personal expenses for a user
     */
    public static void refreshPersonalExpenses(String userId) {
        getPersonalExpensesObservable(userId);
    }

    /**
     * Refresh group expenses
     */
    public static void refreshGroupExpenses(String groupId) {
        getGroupExpensesObservable(groupId);
    }

    /**
     * Get all expenses for a specific user within a group.
     */
    public static List<Expense> getGroupExpensesByUser(String groupId, String userId) {
        List<Expense> expenses = new ArrayList<>();
        try (Connection conn = DatabaseHelper.getConnection()) {
            String query = "SELECT * FROM EXPENSES WHERE group_id = ? AND user_id = ? ORDER BY date DESC";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setString(1, groupId);
            stmt.setString(2, userId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                expenses.add(createExpenseFromResultSet(rs));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return expenses;
    }

    // ==================== JSON EXPORT/IMPORT OPERATIONS ====================

    /**
     * Export personal expenses to JSON file
     */
    public static boolean exportPersonalExpensesToJson(String userId, String filePath) {
        ObservableList<Expense> expenses = getPersonalExpensesObservable(userId);
        return JsonService.exportExpensesToJson(expenses, filePath);
    }

    /**
     * Export group expenses to JSON file
     */
    public static boolean exportGroupExpensesToJson(String groupId, String filePath) {
        ObservableList<Expense> expenses = getGroupExpensesObservable(groupId);
        return JsonService.exportExpensesToJson(expenses, filePath);
    }

    /**
     * Export all expenses to JSON file
     */
    public static boolean exportAllExpensesToJson(String filePath) {
        ObservableList<Expense> expenses = getAllExpensesObservable();
        return JsonService.exportExpensesToJson(expenses, filePath);
    }

    /**
     * Import expenses from JSON and add to database
     */
    public static int importExpensesFromJson(String filePath, String defaultUserId) {
        List<Expense> expenses = JsonService.importExpensesFromJson(filePath);
        if (expenses == null) return 0;

        int count = 0;
        for (Expense expense : expenses) {
            String userId = expense.getUserId() != null ? expense.getUserId() : defaultUserId;
            boolean added = addExpense(
                userId,
                expense.getGroupId(),
                expense.getCategory(),
                expense.getAmount(),
                expense.getDate(),
                expense.getNote()
            );
            if (added) count++;
        }
        return count;
    }

    /**
     * Get expense as JSON string
     */
    public static String getExpenseAsJson(String expenseId) {
        Expense expense = getExpenseById(expenseId);
        if (expense == null) return null;
        return JsonService.expenseToJson(expense);
    }
}
