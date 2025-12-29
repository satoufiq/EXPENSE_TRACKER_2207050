package org.example.service;

import org.example.model.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

/**
 * Parent Service
 * Handles parent-child relationships and queries
 */
public class ParentService {

    /**
     * Get children for the given parent user ID
     */
    public static List<User> getChildrenForParent(String parentUserId) {
        List<User> children = new ArrayList<>();
        String query = "SELECT u.user_id, u.name, u.email, u.password, u.role " +
                "FROM PARENT_RELATION pr " +
                "JOIN USERS u ON pr.child_id = u.user_id " +
                "WHERE pr.parent_id = ?";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, parentUserId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                User user = new User();
                user.setUserId(rs.getString("user_id"));
                user.setName(rs.getString("name"));
                user.setEmail(rs.getString("email"));
                user.setPassword(rs.getString("password"));
                user.setRole(rs.getString("role"));
                children.add(user);
            }
        } catch (Exception e) {
            System.err.println("Error fetching children for parent: " + e.getMessage());
            e.printStackTrace();
        }
        return children;
    }

    /**
     * Get parent for the given child user ID
     */
    public static User getParentForChild(String childUserId) {
        String query = "SELECT u.user_id, u.name, u.email, u.password, u.role " +
                "FROM PARENT_RELATION pr " +
                "JOIN USERS u ON pr.parent_id = u.user_id " +
                "WHERE pr.child_id = ?";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, childUserId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                User user = new User();
                user.setUserId(rs.getString("user_id"));
                user.setName(rs.getString("name"));
                user.setEmail(rs.getString("email"));
                user.setPassword(rs.getString("password"));
                user.setRole(rs.getString("role"));
                return user;
            }
        } catch (Exception e) {
            System.err.println("Error fetching parent for child: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }
}

