package org.example.service;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.example.model.Group;
import org.example.model.GroupMember;
import org.example.model.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Group Service with ObservableList support for real-time updates
 * Handles group management, member operations, and admin functionality
 */
public class GroupService {

    private static final ObservableList<GroupMember> groupMembersList = FXCollections.observableArrayList();
    private static final ObservableList<Group> userGroupsList = FXCollections.observableArrayList();

    /**
     * Get group members as ObservableList for real-time updates
     */
    public static ObservableList<GroupMember> getGroupMembersObservable(String groupId) {
        groupMembersList.clear();
        List<GroupMember> members = getGroupMembersWithDetails(groupId);
        groupMembersList.addAll(members);
        return groupMembersList;
    }

    /**
     * Get user groups as ObservableList for real-time updates
     */
    public static ObservableList<Group> getUserGroupsObservable(String oderId) {
        userGroupsList.clear();
        List<Group> groups = getUserGroups(oderId);
        userGroupsList.addAll(groups);
        return userGroupsList;
    }

    /**
     * Create a new group - Creator becomes admin automatically
     */
    public static boolean createGroup(String groupName, String creatorUserId) {
        Connection conn = null;
        try {
            conn = DatabaseHelper.getConnection();
            conn.setAutoCommit(true);

            String groupId = UUID.randomUUID().toString();
            String now = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

            // Insert group
            String groupQuery = "INSERT INTO GROUPS (group_id, group_name) VALUES (?, ?)";
            PreparedStatement groupStmt = conn.prepareStatement(groupQuery);
            groupStmt.setString(1, groupId);
            groupStmt.setString(2, groupName);
            groupStmt.executeUpdate();
            groupStmt.close();

            // Add creator as admin member
            String memberQuery = "INSERT INTO GROUP_MEMBERS (group_id, user_id, member_role, joined_at) VALUES (?, ?, 'admin', ?)";
            PreparedStatement memberStmt = conn.prepareStatement(memberQuery);
            memberStmt.setString(1, groupId);
            memberStmt.setString(2, creatorUserId);
            memberStmt.setString(3, now);
            memberStmt.executeUpdate();
            memberStmt.close();

            // Update observable list
            Group newGroup = new Group(groupId, groupName);
            userGroupsList.add(0, newGroup);

            System.out.println("✓ Group created successfully. Creator is admin.");
            return true;
        } catch (Exception e) {
            System.err.println("ERROR creating group: " + e.getMessage());
            e.printStackTrace();
            return false;
        } finally {
            if (conn != null) {
                try { conn.close(); } catch (Exception e) { e.printStackTrace(); }
            }
        }
    }

    /**
     * Get all groups for a user
     */
    public static List<Group> getUserGroups(String oderId) {
        List<Group> groups = new ArrayList<>();
        try (Connection conn = DatabaseHelper.getConnection()) {
            String query = "SELECT g.group_id, g.group_name FROM GROUPS g " +
                          "INNER JOIN GROUP_MEMBERS gm ON g.group_id = gm.group_id " +
                          "WHERE gm.user_id = ? ORDER BY g.group_name";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setString(1, oderId);

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
     * Get group members with full user details including member role
     */
    public static List<GroupMember> getGroupMembersWithDetails(String groupId) {
        List<GroupMember> members = new ArrayList<>();
        try (Connection conn = DatabaseHelper.getConnection()) {
            String query = "SELECT u.user_id, u.name, u.email, u.role, gm.member_role, gm.joined_at " +
                          "FROM USERS u " +
                          "INNER JOIN GROUP_MEMBERS gm ON u.user_id = gm.user_id " +
                          "WHERE gm.group_id = ? " +
                          "ORDER BY gm.member_role DESC, u.name"; // Admins first
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setString(1, groupId);

            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                GroupMember member = new GroupMember();
                member.setUserId(rs.getString("user_id"));
                member.setName(rs.getString("name"));
                member.setEmail(rs.getString("email"));
                member.setRole(rs.getString("role"));
                member.setGroupId(groupId);
                String memberRole = rs.getString("member_role");
                member.setMemberRole(memberRole != null ? memberRole : "member");
                member.setJoinedAt(rs.getString("joined_at"));
                members.add(member);
            }
        } catch (Exception e) {
            e.printStackTrace();
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
     * Check if user is admin of a group
     */
    public static boolean isAdmin(String groupId, String oderId) {
        try (Connection conn = DatabaseHelper.getConnection()) {
            String query = "SELECT member_role FROM GROUP_MEMBERS WHERE group_id = ? AND user_id = ?";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setString(1, groupId);
            stmt.setString(2, oderId);

            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                String role = rs.getString("member_role");
                return "admin".equalsIgnoreCase(role);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Get admin count for a group
     */
    public static int getAdminCount(String groupId) {
        try (Connection conn = DatabaseHelper.getConnection()) {
            String query = "SELECT COUNT(*) FROM GROUP_MEMBERS WHERE group_id = ? AND member_role = 'admin'";
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
     * Promote a member to admin (only admins can do this)
     */
    public static boolean promoteToAdmin(String groupId, String targetUserId, String requesterId) {
        if (!isAdmin(groupId, requesterId)) {
            System.err.println("Only admins can promote members to admin");
            return false;
        }

        try (Connection conn = DatabaseHelper.getConnection()) {
            String query = "UPDATE GROUP_MEMBERS SET member_role = 'admin' WHERE group_id = ? AND user_id = ?";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setString(1, groupId);
            stmt.setString(2, targetUserId);

            int result = stmt.executeUpdate();
            if (result > 0) {
                // Update observable list
                for (GroupMember m : groupMembersList) {
                    if (m.getUserId().equals(targetUserId) && m.getGroupId().equals(groupId)) {
                        m.setMemberRole("admin");
                        break;
                    }
                }
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Demote an admin to member (only admins can do this)
     * Cannot demote if it would leave the group without admins
     */
    public static boolean demoteToMember(String groupId, String targetUserId, String requesterId) {
        if (!isAdmin(groupId, requesterId)) {
            System.err.println("Only admins can demote members");
            return false;
        }

        // Check if this would leave the group without admins
        int adminCount = getAdminCount(groupId);
        if (adminCount <= 1 && isAdmin(groupId, targetUserId)) {
            System.err.println("Cannot demote the last admin");
            return false;
        }

        try (Connection conn = DatabaseHelper.getConnection()) {
            String query = "UPDATE GROUP_MEMBERS SET member_role = 'member' WHERE group_id = ? AND user_id = ?";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setString(1, groupId);
            stmt.setString(2, targetUserId);

            int result = stmt.executeUpdate();
            if (result > 0) {
                // Update observable list
                for (GroupMember m : groupMembersList) {
                    if (m.getUserId().equals(targetUserId) && m.getGroupId().equals(groupId)) {
                        m.setMemberRole("member");
                        break;
                    }
                }
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Add a member to a group by email (Admin only)
     * This directly adds the member - for invite flow, use InviteService
     */
    public static boolean addMemberToGroupByEmail(String groupId, String email, String requesterId) {
        // Check if requester is admin
        if (!isAdmin(groupId, requesterId)) {
            System.err.println("Only admins can add members directly");
            return false;
        }

        Connection conn = null;
        try {
            conn = DatabaseHelper.getConnection();
            email = email.trim().toLowerCase();
            String now = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

            // Get user by email
            String userQuery = "SELECT user_id, name, email, role FROM USERS WHERE LOWER(email) = LOWER(?)";
            PreparedStatement userStmt = conn.prepareStatement(userQuery);
            userStmt.setString(1, email);
            ResultSet userRs = userStmt.executeQuery();

            if (!userRs.next()) {
                System.err.println("User not found with email: " + email);
                return false;
            }

            String oderId = userRs.getString("user_id");
            String userName = userRs.getString("name");
            String userEmail = userRs.getString("email");
            String userRole = userRs.getString("role");
            userStmt.close();

            // Check if already a member
            String checkQuery = "SELECT COUNT(*) FROM GROUP_MEMBERS WHERE group_id = ? AND user_id = ?";
            PreparedStatement checkStmt = conn.prepareStatement(checkQuery);
            checkStmt.setString(1, groupId);
            checkStmt.setString(2, oderId);
            ResultSet rs = checkStmt.executeQuery();

            if (rs.next() && rs.getInt(1) > 0) {
                System.err.println("User is already a member of the group");
                return false;
            }
            checkStmt.close();

            // Add member (as regular member, not admin)
            String insertQuery = "INSERT INTO GROUP_MEMBERS (group_id, user_id, member_role, joined_at) VALUES (?, ?, 'member', ?)";
            PreparedStatement insertStmt = conn.prepareStatement(insertQuery);
            insertStmt.setString(1, groupId);
            insertStmt.setString(2, oderId);
            insertStmt.setString(3, now);
            int result = insertStmt.executeUpdate();
            insertStmt.close();

            if (result > 0) {
                GroupMember newMember = new GroupMember(oderId, userName, userEmail, userRole, groupId, "member");
                groupMembersList.add(newMember);
                return true;
            }
            return false;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            if (conn != null) {
                try { conn.close(); } catch (Exception e) { e.printStackTrace(); }
            }
        }
    }

    /**
     * Add a member to a group (internal use - after invite accepted)
     */
    public static boolean addMemberToGroup(String groupId, String oderId) {
        try (Connection conn = DatabaseHelper.getConnection()) {
            String now = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

            // Check if already a member
            String checkQuery = "SELECT COUNT(*) FROM GROUP_MEMBERS WHERE group_id = ? AND user_id = ?";
            PreparedStatement checkStmt = conn.prepareStatement(checkQuery);
            checkStmt.setString(1, groupId);
            checkStmt.setString(2, oderId);
            ResultSet rs = checkStmt.executeQuery();

            if (rs.next() && rs.getInt(1) > 0) {
                return false; // Already a member
            }

            // Get user details
            String userQuery = "SELECT name, email, role FROM USERS WHERE user_id = ?";
            PreparedStatement userStmt = conn.prepareStatement(userQuery);
            userStmt.setString(1, oderId);
            ResultSet userRs = userStmt.executeQuery();

            if (!userRs.next()) {
                return false; // User not found
            }

            String userName = userRs.getString("name");
            String userEmail = userRs.getString("email");
            String userRole = userRs.getString("role");

            // Add member
            String insertQuery = "INSERT INTO GROUP_MEMBERS (group_id, user_id, member_role, joined_at) VALUES (?, ?, 'member', ?)";
            PreparedStatement insertStmt = conn.prepareStatement(insertQuery);
            insertStmt.setString(1, groupId);
            insertStmt.setString(2, oderId);
            insertStmt.setString(3, now);
            int result = insertStmt.executeUpdate();

            if (result > 0) {
                GroupMember newMember = new GroupMember(oderId, userName, userEmail, userRole, groupId, "member");
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
     * Remove a member from a group (Admin only, cannot remove self if last admin)
     */
    public static boolean removeMemberFromGroup(String groupId, String targetUserId, String requesterId) {
        // Check if requester is admin
        if (!isAdmin(groupId, requesterId)) {
            System.err.println("Only admins can remove members");
            return false;
        }

        // Check if trying to remove the last admin
        if (isAdmin(groupId, targetUserId) && getAdminCount(groupId) <= 1) {
            System.err.println("Cannot remove the last admin. Promote another member first.");
            return false;
        }

        try (Connection conn = DatabaseHelper.getConnection()) {
            String query = "DELETE FROM GROUP_MEMBERS WHERE group_id = ? AND user_id = ?";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setString(1, groupId);
            stmt.setString(2, targetUserId);

            int rowsAffected = stmt.executeUpdate();

            if (rowsAffected > 0) {
                groupMembersList.removeIf(m -> m.getUserId().equals(targetUserId) && m.getGroupId().equals(groupId));

                // Check if group has no members left
                int memberCount = getMemberCount(groupId);
                if (memberCount == 0) {
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
     * Leave group (anyone can leave, but last admin cannot leave without promoting someone)
     */
    public static boolean leaveGroup(String groupId, String oderId) {
        // If user is the only admin, they cannot leave
        if (isAdmin(groupId, oderId) && getAdminCount(groupId) <= 1 && getMemberCount(groupId) > 1) {
            System.err.println("You are the only admin. Promote another member to admin before leaving.");
            return false;
        }

        try (Connection conn = DatabaseHelper.getConnection()) {
            String query = "DELETE FROM GROUP_MEMBERS WHERE group_id = ? AND user_id = ?";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setString(1, groupId);
            stmt.setString(2, oderId);

            int rowsAffected = stmt.executeUpdate();

            if (rowsAffected > 0) {
                groupMembersList.removeIf(m -> m.getUserId().equals(oderId) && m.getGroupId().equals(groupId));
                userGroupsList.removeIf(g -> g.getGroupId().equals(groupId));

                // Check if group has no members left
                int memberCount = getMemberCount(groupId);
                if (memberCount == 0) {
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
     * Delete a group
     */
    public static boolean deleteGroup(String groupId) {
        try (Connection conn = DatabaseHelper.getConnection()) {
            // Delete group members first
            String deleteMembersQuery = "DELETE FROM GROUP_MEMBERS WHERE group_id = ?";
            PreparedStatement deleteMembersStmt = conn.prepareStatement(deleteMembersQuery);
            deleteMembersStmt.setString(1, groupId);
            deleteMembersStmt.executeUpdate();

            // Delete group expenses
            String deleteExpensesQuery = "DELETE FROM EXPENSES WHERE group_id = ?";
            PreparedStatement deleteExpensesStmt = conn.prepareStatement(deleteExpensesQuery);
            deleteExpensesStmt.setString(1, groupId);
            deleteExpensesStmt.executeUpdate();

            // Delete group
            String deleteGroupQuery = "DELETE FROM GROUPS WHERE group_id = ?";
            PreparedStatement deleteGroupStmt = conn.prepareStatement(deleteGroupQuery);
            deleteGroupStmt.setString(1, groupId);
            int rowsAffected = deleteGroupStmt.executeUpdate();

            if (rowsAffected > 0) {
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
     * Get all admin IDs of a group
     */
    public static List<String> getGroupAdmins(String groupId) {
        List<String> adminIds = new ArrayList<>();
        try (Connection conn = DatabaseHelper.getConnection()) {
            String query = "SELECT user_id FROM GROUP_MEMBERS WHERE group_id = ? AND member_role = 'admin'";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setString(1, groupId);

            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                adminIds.add(rs.getString("user_id"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return adminIds;
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
    public static boolean isMemberOfGroup(String groupId, String oderId) {
        try (Connection conn = DatabaseHelper.getConnection()) {
            String query = "SELECT COUNT(*) FROM GROUP_MEMBERS WHERE group_id = ? AND user_id = ?";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setString(1, groupId);
            stmt.setString(2, oderId);

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
    public static boolean exportUserGroupsToJson(String oderId, String filePath) {
        List<Group> groups = getUserGroups(oderId);
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

