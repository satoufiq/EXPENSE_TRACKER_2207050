package org.example.service;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.example.model.ParentChildAlert;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Service for managing alerts between parents and children
 */
public class ParentChildAlertService {

    private static final ObservableList<ParentChildAlert> alertsList = FXCollections.observableArrayList();

    /**
     * Send an alert from child to parent about budget concerns
     */
    public static boolean sendAlertToParent(String childId, String parentId, String message) {
        try (Connection conn = DatabaseHelper.getConnection()) {
            String alertId = UUID.randomUUID().toString();
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

            String query = "INSERT INTO PARENT_CHILD_ALERTS (alert_id, from_user_id, to_user_id, type, message, created_at, read_status) " +
                          "VALUES (?, ?, ?, ?, ?, ?, ?)";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setString(1, alertId);
            stmt.setString(2, childId);
            stmt.setString(3, parentId);
            stmt.setString(4, "alert");
            stmt.setString(5, message);
            stmt.setString(6, timestamp);
            stmt.setString(7, "unread");

            return stmt.executeUpdate() > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Send a suggestion from parent to child
     */
    public static boolean sendSuggestionToChild(String parentId, String childId, String message) {
        try (Connection conn = DatabaseHelper.getConnection()) {
            String alertId = UUID.randomUUID().toString();
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

            String query = "INSERT INTO PARENT_CHILD_ALERTS (alert_id, from_user_id, to_user_id, type, message, created_at, read_status) " +
                          "VALUES (?, ?, ?, ?, ?, ?, ?)";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setString(1, alertId);
            stmt.setString(2, parentId);
            stmt.setString(3, childId);
            stmt.setString(4, "suggestion");
            stmt.setString(5, message);
            stmt.setString(6, timestamp);
            stmt.setString(7, "unread");

            return stmt.executeUpdate() > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Get all alerts for a specific user (parent or child)
     */
    public static ObservableList<ParentChildAlert> getAlertsForUser(String userId) {
        alertsList.clear();
        List<ParentChildAlert> alerts = new ArrayList<>();

        try (Connection conn = DatabaseHelper.getConnection()) {
            String query = "SELECT * FROM PARENT_CHILD_ALERTS WHERE to_user_id = ? ORDER BY created_at DESC";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setString(1, userId);

            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                ParentChildAlert alert = new ParentChildAlert();
                alert.setAlertId(rs.getString("alert_id"));
                alert.setFromUserId(rs.getString("from_user_id"));
                alert.setToUserId(rs.getString("to_user_id"));
                alert.setType(rs.getString("type"));
                alert.setMessage(rs.getString("message"));
                alert.setCreatedAt(rs.getString("created_at"));
                alert.setReadStatus(rs.getString("read_status"));

                // Get user names
                var fromUser = UserService.getUserById(alert.getFromUserId());
                if (fromUser != null) {
                    alert.setFromUserName(fromUser.getName());
                }

                alerts.add(alert);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        alertsList.addAll(alerts);
        return alertsList;
    }

    /**
     * Mark alert as read
     */
    public static boolean markAsRead(String alertId) {
        try (Connection conn = DatabaseHelper.getConnection()) {
            String query = "UPDATE PARENT_CHILD_ALERTS SET read_status = 'read' WHERE alert_id = ?";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setString(1, alertId);
            return stmt.executeUpdate() > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Get unread alert count for a user
     */
    public static int getUnreadAlertCount(String userId) {
        try (Connection conn = DatabaseHelper.getConnection()) {
            String query = "SELECT COUNT(*) FROM PARENT_CHILD_ALERTS WHERE to_user_id = ? AND read_status = 'unread'";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setString(1, userId);

            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    /**
     * Delete an alert
     */
    public static boolean deleteAlert(String alertId) {
        try (Connection conn = DatabaseHelper.getConnection()) {
            String query = "DELETE FROM PARENT_CHILD_ALERTS WHERE alert_id = ?";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setString(1, alertId);
            return stmt.executeUpdate() > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Get alerts sent from a specific user
     */
    public static List<ParentChildAlert> getSentAlerts(String userId) {
        List<ParentChildAlert> alerts = new ArrayList<>();

        try (Connection conn = DatabaseHelper.getConnection()) {
            String query = "SELECT * FROM PARENT_CHILD_ALERTS WHERE from_user_id = ? ORDER BY created_at DESC";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setString(1, userId);

            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                ParentChildAlert alert = new ParentChildAlert();
                alert.setAlertId(rs.getString("alert_id"));
                alert.setFromUserId(rs.getString("from_user_id"));
                alert.setToUserId(rs.getString("to_user_id"));
                alert.setType(rs.getString("type"));
                alert.setMessage(rs.getString("message"));
                alert.setCreatedAt(rs.getString("created_at"));
                alert.setReadStatus(rs.getString("read_status"));

                // Get user names
                var toUser = UserService.getUserById(alert.getToUserId());
                if (toUser != null) {
                    alert.setToUserName(toUser.getName());
                }

                alerts.add(alert);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return alerts;
    }
}

