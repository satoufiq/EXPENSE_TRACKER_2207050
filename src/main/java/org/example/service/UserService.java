package org.example.service;

import org.example.model.User;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.UUID;

/**
 * User Service
 * Handles user authentication and management
 */
public class UserService {

    public static User login(String email, String password) {
        try (Connection conn = DatabaseHelper.getConnection()) {
            String query = "SELECT * FROM USERS WHERE email = ? COLLATE NOCASE AND password = ?";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setString(1, email.trim());
            stmt.setString(2, password);

            System.out.println("Login attempt with email: " + email.trim());

            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                User user = new User();
                user.setUserId(rs.getString("user_id"));
                user.setName(rs.getString("name"));
                user.setEmail(rs.getString("email"));
                user.setPassword(rs.getString("password"));
                user.setRole(rs.getString("role"));
                System.out.println("Login successful for user: " + user.getName());
                return user;
            }
            System.out.println("Login failed - no matching user found");
        } catch (Exception e) {
            System.err.println("Error during login: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    public static boolean register(String name, String email, String password, String role) {
        try (Connection conn = DatabaseHelper.getConnection()) {
            String userId = UUID.randomUUID().toString();

            // Normalize email to lowercase for consistency
            email = email.trim().toLowerCase();

            System.out.println("Registering user: " + name + " with email: " + email);

            String query = "INSERT INTO USERS (user_id, name, email, password, role) VALUES (?, ?, ?, ?, ?)";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setString(1, userId);
            stmt.setString(2, name);
            stmt.setString(3, email);
            stmt.setString(4, password);
            stmt.setString(5, role);

            int result = stmt.executeUpdate();
            System.out.println("User registered successfully. User ID: " + userId + ", Rows affected: " + result);

            return true;
        } catch (Exception e) {
            System.err.println("Error registering user: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public static boolean emailExists(String email) {
        try (Connection conn = DatabaseHelper.getConnection()) {
            String query = "SELECT COUNT(*) FROM USERS WHERE email = ? COLLATE NOCASE";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setString(1, email.trim());

            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                boolean exists = rs.getInt(1) > 0;
                System.out.println("Email " + email + " exists: " + exists);
                return exists;
            }
        } catch (Exception e) {
            System.err.println("Error checking email existence: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Get user by email (case-insensitive)
     */
    public static User getUserByEmail(String email) {
        try (Connection conn = DatabaseHelper.getConnection()) {
            String query = "SELECT * FROM USERS WHERE email = ? COLLATE NOCASE";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setString(1, email.trim());

            System.out.println("Searching for user with email: " + email.trim());

            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                User user = new User();
                user.setUserId(rs.getString("user_id"));
                user.setName(rs.getString("name"));
                user.setEmail(rs.getString("email"));
                user.setPassword(rs.getString("password"));
                user.setRole(rs.getString("role"));
                System.out.println("Found user: " + user.getName() + " (" + user.getEmail() + ")");
                return user;
            }
            System.out.println("No user found with email: " + email.trim());
        } catch (Exception e) {
            System.err.println("Error getting user by email: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Get user by ID
     */
    public static User getUserById(String userId) {
        try (Connection conn = DatabaseHelper.getConnection()) {
            String query = "SELECT * FROM USERS WHERE user_id = ?";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setString(1, userId);

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
            e.printStackTrace();
        }
        return null;
    }
}
