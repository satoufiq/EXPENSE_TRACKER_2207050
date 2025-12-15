package org.example.service;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDateTime;

/**
 * Group Budget Service: store and fetch per-group monthly budget in BDT.
 */
public class GroupBudgetService {

    public static double getMonthlyBudget(String groupId) {
        try (Connection conn = DatabaseHelper.getConnection()) {
            PreparedStatement ps = conn.prepareStatement("SELECT monthly_budget FROM GROUP_BUDGETS WHERE group_id = ?");
            ps.setString(1, groupId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getDouble(1);
        } catch (Exception e) { e.printStackTrace(); }
        return 0.0;
    }

    public static void setMonthlyBudget(String groupId, double amountBDT) {
        try (Connection conn = DatabaseHelper.getConnection()) {
            PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO GROUP_BUDGETS (group_id, monthly_budget, currency, updated_at) VALUES (?, ?, 'BDT', ?) " +
                    "ON CONFLICT(group_id) DO UPDATE SET monthly_budget = excluded.monthly_budget, updated_at = excluded.updated_at");
            ps.setString(1, groupId);
            ps.setDouble(2, amountBDT);
            ps.setString(3, LocalDateTime.now().toString());
            ps.executeUpdate();
        } catch (Exception e) { e.printStackTrace(); }
    }
}

