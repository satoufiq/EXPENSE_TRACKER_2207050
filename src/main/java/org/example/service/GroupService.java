package org.example.service;

import org.example.model.Group;
import org.example.model.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Group Service
 * Handles group management and member operations
 */
public class GroupService {

    /**
     * Create a new group
     */
    public static boolean createGroup(String groupName, String creatorUserId) {
        try (Connection conn = DatabaseHelper.getConnection()) {
            String groupId = UUID.randomUUID().toString();

            // Insert group
            String groupQuery = "INSERT INTO GROUPS (group_id, group_name) VALUES (?, ?)";
            PreparedStatement groupStmt = conn.prepareStatement(groupQuery);
            groupStmt.setString(1, groupId);
            groupStmt.setString(2, groupName);
            groupStmt.executeUpdate();

            // Add creator as first member
            String memberQuery = "INSERT INTO GROUP_MEMBERS (group_id, user_id) VALUES (?, ?)";
            PreparedStatement memberStmt = conn.prepareStatement(memberQuery);
            memberStmt.setString(1, groupId);
            memberStmt.setString(2, creatorUserId);
            memberStmt.executeUpdate();

            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Get all groups for a user
     */
    public static List<Group> getUserGroups(String userId) {
        List<Group> groups = new ArrayList<>();
        try (Connection conn = DatabaseHelper.getConnection()) {
            String query = "SELECT g.group_id, g.group_name FROM GROUPS g " +
                          "INNER JOIN GROUP_MEMBERS gm ON g.group_id = gm.group_id " +
                          "WHERE gm.user_id = ? " +
                          "ORDER BY g.group_name";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setString(1, userId);

            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                Group group = new Group();
                group.setGroupId(rs.getString("group_id"));
                group.setGroupName(rs.getString("group_name"));
                groups.add(group);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return groups;
    }

    /**
     * Get group by ID
     */
    public static Group getGroupById(String groupId) {
        try (Connection conn = DatabaseHelper.getConnection()) {
            String query = "SELECT * FROM GROUPS WHERE group_id = ?";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setString(1, groupId);

            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                Group group = new Group();
                group.setGroupId(rs.getString("group_id"));
                group.setGroupName(rs.getString("group_name"));
                return group;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Add a member to a group
     */
    public static boolean addMemberToGroup(String groupId, String userId) {
        try (Connection conn = DatabaseHelper.getConnection()) {
            // Check if already a member
            String checkQuery = "SELECT COUNT(*) FROM GROUP_MEMBERS WHERE group_id = ? AND user_id = ?";
            PreparedStatement checkStmt = conn.prepareStatement(checkQuery);
            checkStmt.setString(1, groupId);
            checkStmt.setString(2, userId);
            ResultSet rs = checkStmt.executeQuery();

            if (rs.next() && rs.getInt(1) > 0) {
                return false; // Already a member
            }

            // Add member
            String insertQuery = "INSERT INTO GROUP_MEMBERS (group_id, user_id) VALUES (?, ?)";
            PreparedStatement insertStmt = conn.prepareStatement(insertQuery);
            insertStmt.setString(1, groupId);
            insertStmt.setString(2, userId);
            insertStmt.executeUpdate();

            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Remove a member from a group (leave group)
     */
    public static boolean leaveGroup(String groupId, String userId) {
        try (Connection conn = DatabaseHelper.getConnection()) {
            String query = "DELETE FROM GROUP_MEMBERS WHERE group_id = ? AND user_id = ?";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setString(1, groupId);
            stmt.setString(2, userId);

            int rowsAffected = stmt.executeUpdate();

            // Check if group has no members left, if so delete the group
            if (rowsAffected > 0) {
                String countQuery = "SELECT COUNT(*) FROM GROUP_MEMBERS WHERE group_id = ?";
                PreparedStatement countStmt = conn.prepareStatement(countQuery);
                countStmt.setString(1, groupId);
                ResultSet rs = countStmt.executeQuery();

                if (rs.next() && rs.getInt(1) == 0) {
                    // No members left, delete the group
                    deleteGroup(groupId);
                }
            }

            return rowsAffected > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Delete a group
     */
    public static boolean deleteGroup(String groupId) {
        try (Connection conn = DatabaseHelper.getConnection()) {
            // Delete group members first
            String deleteMembersQuery = "DELETE FROM GROUP_MEMBERS WHERE group_id = ?";
            PreparedStatement deleteMembersStmt = conn.prepareStatement(deleteMembersQuery);
            deleteMembersStmt.setString(1, groupId);
            deleteMembersStmt.executeUpdate();

            // Delete group
            String deleteGroupQuery = "DELETE FROM GROUPS WHERE group_id = ?";
            PreparedStatement deleteGroupStmt = conn.prepareStatement(deleteGroupQuery);
            deleteGroupStmt.setString(1, groupId);
            int rowsAffected = deleteGroupStmt.executeUpdate();

            return rowsAffected > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Get all members of a group
     */
    public static List<String> getGroupMembers(String groupId) {
        List<String> memberIds = new ArrayList<>();
        try (Connection conn = DatabaseHelper.getConnection()) {
            String query = "SELECT user_id FROM GROUP_MEMBERS WHERE group_id = ?";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setString(1, groupId);

            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                memberIds.add(rs.getString("user_id"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return memberIds;
    }

    /**
     * Check if user is a member of a group
     */
    public static boolean isMember(String groupId, String userId) {
        try (Connection conn = DatabaseHelper.getConnection()) {
            String query = "SELECT COUNT(*) FROM GROUP_MEMBERS WHERE group_id = ? AND user_id = ?";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setString(1, groupId);
            stmt.setString(2, userId);

            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Get member count for a group
     */
    public static int getMemberCount(String groupId) {
        try (Connection conn = DatabaseHelper.getConnection()) {
            String query = "SELECT COUNT(*) FROM GROUP_MEMBERS WHERE group_id = ?";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setString(1, groupId);

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
     * Get all member details for a group (with user names)
     */
    public static java.util.List<User> getGroupMemberDetails(String groupId) {
        java.util.List<User> members = new java.util.ArrayList<>();
        try (Connection conn = DatabaseHelper.getConnection()) {
            String query = "SELECT u.* FROM USERS u " +
                          "INNER JOIN GROUP_MEMBERS gm ON u.user_id = gm.user_id " +
                          "WHERE gm.group_id = ? " +
                          "ORDER BY u.name";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setString(1, groupId);

            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                User user = new User();
                user.setUserId(rs.getString("user_id"));
                user.setName(rs.getString("name"));
                user.setEmail(rs.getString("email"));
                user.setPassword(rs.getString("password"));
                user.setRole(rs.getString("role"));
                members.add(user);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return members;
    }
}
