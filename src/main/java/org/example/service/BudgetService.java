package org.example.service;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDateTime;

/**
 * Budget Service: store and fetch per-user monthly budget in BDT.
 */
public class BudgetService {

    public static double getMonthlyBudget(String userId) {
        try (Connection conn = DatabaseHelper.getConnection()) {
            PreparedStatement ps = conn.prepareStatement("SELECT monthly_budget FROM BUDGETS WHERE user_id = ?");
            ps.setString(1, userId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getDouble(1);
        } catch (Exception e) { e.printStackTrace(); }
        return 0.0;
    }

    public static void setMonthlyBudget(String userId, double amountBDT) {
        try (Connection conn = DatabaseHelper.getConnection()) {
            // upsert
            PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO BUDGETS (user_id, monthly_budget, currency, updated_at) VALUES (?, ?, 'BDT', ?) " +
                    "ON CONFLICT(user_id) DO UPDATE SET monthly_budget = excluded.monthly_budget, updated_at = excluded.updated_at");
            ps.setString(1, userId);
            ps.setDouble(2, amountBDT);
            ps.setString(3, LocalDateTime.now().toString());
            ps.executeUpdate();
        } catch (Exception e) { e.printStackTrace(); }
    }
}

