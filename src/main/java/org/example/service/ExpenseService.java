package org.example.service;

import org.example.model.Expense;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Expense Service
 * Handles expense CRUD operations
 */
public class ExpenseService {

    /**
     * Get all personal expenses for a user
     */
    public static List<Expense> getPersonalExpenses(String userId) {
        List<Expense> expenses = new ArrayList<>();
        try (Connection conn = DatabaseHelper.getConnection()) {
            String query = "SELECT * FROM EXPENSES WHERE user_id = ? AND (group_id IS NULL OR group_id = '') ORDER BY date DESC";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setString(1, userId);

            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                Expense expense = new Expense();
                expense.setExpenseId(rs.getString("expense_id"));
                expense.setUserId(rs.getString("user_id"));
                expense.setGroupId(rs.getString("group_id"));
                expense.setCategory(rs.getString("category"));
                expense.setAmount(rs.getDouble("amount"));
                expense.setDate(rs.getString("date"));
                expense.setNote(rs.getString("note"));
                expenses.add(expense);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return expenses;
    }

    /**
     * Get all expenses for a group
     */
    public static List<Expense> getGroupExpenses(String groupId) {
        List<Expense> expenses = new ArrayList<>();
        try (Connection conn = DatabaseHelper.getConnection()) {
            String query = "SELECT * FROM EXPENSES WHERE group_id = ? ORDER BY date DESC";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setString(1, groupId);

            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                Expense expense = new Expense();
                expense.setExpenseId(rs.getString("expense_id"));
                expense.setUserId(rs.getString("user_id"));
                expense.setGroupId(rs.getString("group_id"));
                expense.setCategory(rs.getString("category"));
                expense.setAmount(rs.getDouble("amount"));
                expense.setDate(rs.getString("date"));
                expense.setNote(rs.getString("note"));
                expenses.add(expense);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return expenses;
    }

    /**
     * Add a new expense
     */
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

            stmt.executeUpdate();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Update an existing expense
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
            return rowsAffected > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Delete an expense
     */
    public static boolean deleteExpense(String expenseId) {
        try (Connection conn = DatabaseHelper.getConnection()) {
            String query = "DELETE FROM EXPENSES WHERE expense_id = ?";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setString(1, expenseId);

            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;
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
}

