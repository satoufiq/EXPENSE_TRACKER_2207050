package org.example.service;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.example.model.Group;
import org.example.model.GroupMember;
import org.example.model.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Group Service with ObservableList support for real-time updates
 * Handles group management and member operations
 */
public class GroupService {

    // Observable lists for real-time updates
    private static final ObservableList<GroupMember> groupMembersList = FXCollections.observableArrayList();
    private static final ObservableList<Group> userGroupsList = FXCollections.observableArrayList();

    /**
     * Get group members as ObservableList for real-time updates
     */
    public static ObservableList<GroupMember> getGroupMembersObservable(String groupId) {
        System.out.println("Getting members for group: " + groupId);
        groupMembersList.clear();
        List<GroupMember> members = getGroupMembersWithDetails(groupId);
        System.out.println("Found " + members.size() + " members");
        groupMembersList.addAll(members);
        return groupMembersList;
    }

    /**
     * Get user groups as ObservableList for real-time updates
     */
    public static ObservableList<Group> getUserGroupsObservable(String userId) {
        userGroupsList.clear();
        List<Group> groups = getUserGroups(userId);
        userGroupsList.addAll(groups);
        return userGroupsList;
    }

    /**
     * Create a new group
     */
    public static boolean createGroup(String groupName, String creatorUserId) {
        Connection conn = null;
        try {
            conn = DatabaseHelper.getConnection();
            conn.setAutoCommit(true); // Ensure autocommit is enabled

            String groupId = UUID.randomUUID().toString();

            System.out.println("=== CREATING GROUP ===");
            System.out.println("Group name: " + groupName);
            System.out.println("Creator user ID: " + creatorUserId);
            System.out.println("Group ID: " + groupId);

            // Insert group
            String groupQuery = "INSERT INTO GROUPS (group_id, group_name) VALUES (?, ?)";
            PreparedStatement groupStmt = conn.prepareStatement(groupQuery);
            groupStmt.setString(1, groupId);
            groupStmt.setString(2, groupName);
            int groupResult = groupStmt.executeUpdate();
            groupStmt.close();

            System.out.println("✓ Group inserted. Rows affected: " + groupResult);

            // Add creator as first member
            String memberQuery = "INSERT INTO GROUP_MEMBERS (group_id, user_id) VALUES (?, ?)";
            PreparedStatement memberStmt = conn.prepareStatement(memberQuery);
            memberStmt.setString(1, groupId);
            memberStmt.setString(2, creatorUserId);
            int memberResult = memberStmt.executeUpdate();
            memberStmt.close();

            System.out.println("✓ Creator added as member. Rows affected: " + memberResult);

            // Verify the insert
            String verifyQuery = "SELECT COUNT(*) FROM GROUP_MEMBERS WHERE group_id = ? AND user_id = ?";
            PreparedStatement verifyStmt = conn.prepareStatement(verifyQuery);
            verifyStmt.setString(1, groupId);
            verifyStmt.setString(2, creatorUserId);
            java.sql.ResultSet rs = verifyStmt.executeQuery();
            if (rs.next()) {
                int count = rs.getInt(1);
                System.out.println("✓ Verification: " + count + " member(s) in group");
            }
            verifyStmt.close();

            // Update observable list
            Group newGroup = new Group(groupId, groupName);
            userGroupsList.add(0, newGroup);

            System.out.println("✓ Group creation successful!");
            System.out.println("======================");
            return true;
        } catch (Exception e) {
            System.err.println("✗ ERROR creating group: " + e.getMessage());
            e.printStackTrace();
            return false;
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
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
     * Get group members with full user details
     */
    public static List<GroupMember> getGroupMembersWithDetails(String groupId) {
        List<GroupMember> members = new ArrayList<>();
        Connection conn = null;
        try {
            conn = DatabaseHelper.getConnection();

            System.out.println("=== QUERYING GROUP MEMBERS ===");
            System.out.println("Group ID: " + groupId);

            String query = "SELECT u.user_id, u.name, u.email, u.role " +
                          "FROM USERS u " +
                          "INNER JOIN GROUP_MEMBERS gm ON u.user_id = gm.user_id " +
                          "WHERE gm.group_id = ? " +
                          "ORDER BY u.name";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setString(1, groupId);

            System.out.println("Executing query: " + query);
            System.out.println("With groupId parameter: " + groupId);

            ResultSet rs = stmt.executeQuery();

            int count = 0;
            while (rs.next()) {
                count++;
                GroupMember member = new GroupMember();
                member.setUserId(rs.getString("user_id"));
                member.setName(rs.getString("name"));
                member.setEmail(rs.getString("email"));
                member.setRole(rs.getString("role"));
                member.setGroupId(groupId);
                members.add(member);
                System.out.println("  ✓ Member " + count + ": " + member.getName() + " (" + member.getEmail() + ") - ID: " + member.getUserId());
            }
            stmt.close();

            System.out.println("✓ Total members found: " + members.size());
            System.out.println("==============================");
        } catch (Exception e) {
            System.err.println("✗ ERROR getting group members: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return members;
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
     * Add a member to a group by email and update ObservableList
     */
    public static boolean addMemberToGroupByEmail(String groupId, String email) {
        Connection conn = null;
        try {
            conn = DatabaseHelper.getConnection();

            if (conn == null) {
                System.err.println("Database connection is null!");
                return false;
            }

            email = email.trim().toLowerCase(); // Normalize email

            System.out.println("=== Adding Member ===");
            System.out.println("Group ID: " + groupId);
            System.out.println("Email: " + email);

            // Get user by email (case-insensitive with COLLATE NOCASE)
            String userQuery = "SELECT user_id, name, email, role FROM USERS WHERE LOWER(email) = LOWER(?)";
            PreparedStatement userStmt = conn.prepareStatement(userQuery);
            userStmt.setString(1, email);

            System.out.println("Executing query: " + userQuery + " with email: " + email);

            ResultSet userRs = userStmt.executeQuery();

            if (!userRs.next()) {
                System.err.println("User not found with email: " + email);
                userStmt.close();
                return false; // User not found
            }

            String userId = userRs.getString("user_id");
            String userName = userRs.getString("name");
            String userEmail = userRs.getString("email");
            String userRole = userRs.getString("role");

            System.out.println("Found user: " + userName + " (" + userEmail + ") with ID: " + userId);
            userStmt.close();

            // Check if already a member
            String checkQuery = "SELECT COUNT(*) as cnt FROM GROUP_MEMBERS WHERE group_id = ? AND user_id = ?";
            PreparedStatement checkStmt = conn.prepareStatement(checkQuery);
            checkStmt.setString(1, groupId);
            checkStmt.setString(2, userId);
            ResultSet rs = checkStmt.executeQuery();

            int count = 0;
            if (rs.next()) {
                count = rs.getInt("cnt");
            }

            System.out.println("Member check result: " + count);
            checkStmt.close();

            if (count > 0) {
                System.err.println("User is already a member of the group");
                return false; // Already a member
            }

            // Add member
            String insertQuery = "INSERT INTO GROUP_MEMBERS (group_id, user_id) VALUES (?, ?)";
            PreparedStatement insertStmt = conn.prepareStatement(insertQuery);
            insertStmt.setString(1, groupId);
            insertStmt.setString(2, userId);

            System.out.println("Inserting: groupId=" + groupId + ", userId=" + userId);

            int result = insertStmt.executeUpdate();
            insertStmt.close();

            System.out.println("Insert result: " + result + " rows affected");

            if (result > 0) {
                System.out.println("Successfully added member to group");
                // Update observable list
                GroupMember newMember = new GroupMember(userId, userName, userEmail, userRole, groupId);
                groupMembersList.add(newMember);
                System.out.println("Observable list updated. New size: " + groupMembersList.size());
                return true;
            }

            System.err.println("Insert returned 0 rows");
            return false;
        } catch (Exception e) {
            System.err.println("Exception in addMemberToGroupByEmail: " + e.getClass().getName() + " - " + e.getMessage());
            e.printStackTrace();
            return false;
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Add a member to a group and update ObservableList
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

            // Get user details
            String userQuery = "SELECT name, email, role FROM USERS WHERE user_id = ?";
            PreparedStatement userStmt = conn.prepareStatement(userQuery);
            userStmt.setString(1, userId);
            ResultSet userRs = userStmt.executeQuery();

            if (!userRs.next()) {
                return false; // User not found
            }

            String userName = userRs.getString("name");
            String userEmail = userRs.getString("email");
            String userRole = userRs.getString("role");

            // Add member
            String insertQuery = "INSERT INTO GROUP_MEMBERS (group_id, user_id) VALUES (?, ?)";
            PreparedStatement insertStmt = conn.prepareStatement(insertQuery);
            insertStmt.setString(1, groupId);
            insertStmt.setString(2, userId);
            int result = insertStmt.executeUpdate();

            if (result > 0) {
                // Update observable list
                GroupMember newMember = new GroupMember(userId, userName, userEmail, userRole, groupId);
                groupMembersList.add(newMember);
                return true;
            }

            return false;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Remove a member from a group and update ObservableList
     */
    public static boolean removeMemberFromGroup(String groupId, String userId) {
        try (Connection conn = DatabaseHelper.getConnection()) {
            String query = "DELETE FROM GROUP_MEMBERS WHERE group_id = ? AND user_id = ?";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setString(1, groupId);
            stmt.setString(2, userId);

            int rowsAffected = stmt.executeUpdate();

            if (rowsAffected > 0) {
                // Update observable list
                groupMembersList.removeIf(m -> m.getUserId().equals(userId) && m.getGroupId().equals(groupId));

                // Check if group has no members left
                String countQuery = "SELECT COUNT(*) FROM GROUP_MEMBERS WHERE group_id = ?";
                PreparedStatement countStmt = conn.prepareStatement(countQuery);
                countStmt.setString(1, groupId);
                ResultSet rs = countStmt.executeQuery();

                if (rs.next() && rs.getInt(1) == 0) {
                    // No members left, delete the group
                    deleteGroup(groupId);
                }
                return true;
            }

            return false;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Remove a member from a group (leave group)
     */
    public static boolean leaveGroup(String groupId, String userId) {
        return removeMemberFromGroup(groupId, userId);
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

            if (rowsAffected > 0) {
                // Update observable lists
                groupMembersList.removeIf(m -> m.getGroupId().equals(groupId));
                userGroupsList.removeIf(g -> g.getGroupId().equals(groupId));
                return true;
            }

            return false;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Get all member IDs of a group
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
     * Check if user is member of group
     */
    public static boolean isMemberOfGroup(String groupId, String userId) {
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
     * Refresh group members list
     */
    public static void refreshGroupMembers(String groupId) {
        getGroupMembersObservable(groupId);
    }

    // ==================== JSON EXPORT/IMPORT OPERATIONS ====================

    /**
     * Export group members to JSON file
     */
    public static boolean exportGroupMembersToJson(String groupId, String filePath) {
        ObservableList<GroupMember> members = getGroupMembersObservable(groupId);
        return JsonService.exportGroupMembersToJson(members, filePath);
    }

    /**
     * Export user groups to JSON file
     */
    public static boolean exportUserGroupsToJson(String userId, String filePath) {
        List<Group> groups = getUserGroups(userId);
        return JsonService.exportGroupsToJson(groups, filePath);
    }

    /**
     * Import groups from JSON file
     */
    public static int importGroupsFromJson(String filePath, String defaultUserId) {
        List<Group> groups = JsonService.importGroupsFromJson(filePath);
        if (groups == null) return 0;

        int count = 0;
        for (Group group : groups) {
            boolean created = createGroup(group.getGroupName(), defaultUserId);
            if (created) count++;
        }
        return count;
    }
}
