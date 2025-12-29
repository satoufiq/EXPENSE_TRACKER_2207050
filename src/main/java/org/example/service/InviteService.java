package org.example.service;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class InviteService {

    public static String sendParentInvite(String parentId, String childId) {
        String inviteId = UUID.randomUUID().toString();
        String sql = "INSERT INTO PARENT_INVITES (invite_id, parent_id, child_id, status, created_at) VALUES (?, ?, ?, 'pending', datetime('now'))";
        try (Connection conn = DatabaseHelper.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, inviteId);
            ps.setString(2, parentId);
            ps.setString(3, childId);
            ps.executeUpdate();
            return inviteId;
        } catch (Exception e) {
            System.err.println("Error sending parent invite: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    public static boolean acceptParentInvite(String inviteId) {
        String select = "SELECT parent_id, child_id FROM PARENT_INVITES WHERE invite_id = ? AND status = 'pending'";
        String update = "UPDATE PARENT_INVITES SET status = 'accepted' WHERE invite_id = ?";
        try (Connection conn = DatabaseHelper.getConnection(); PreparedStatement sel = conn.prepareStatement(select)) {
            sel.setString(1, inviteId);
            ResultSet rs = sel.executeQuery();
            if (rs.next()) {
                String parentId = rs.getString(1);
                String childId = rs.getString(2);
                // create relation
                String relate = "INSERT OR IGNORE INTO PARENT_RELATION (parent_id, child_id) VALUES (?, ?)";
                try (PreparedStatement rel = conn.prepareStatement(relate)) {
                    rel.setString(1, parentId);
                    rel.setString(2, childId);
                    rel.executeUpdate();
                }
                try (PreparedStatement upd = conn.prepareStatement(update)) {
                    upd.setString(1, inviteId);
                    upd.executeUpdate();
                }
                return true;
            }
        } catch (Exception e) {
            System.err.println("Error accepting parent invite: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    public static boolean declineParentInvite(String inviteId) {
        String update = "UPDATE PARENT_INVITES SET status = 'declined' WHERE invite_id = ?";
        try (Connection conn = DatabaseHelper.getConnection(); PreparedStatement ps = conn.prepareStatement(update)) {
            ps.setString(1, inviteId);
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            System.err.println("Error declining parent invite: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public static List<String[]> getPendingParentInvitesForChild(String childId) {
        List<String[]> invites = new ArrayList<>();
        String sql = "SELECT invite_id, parent_id FROM PARENT_INVITES WHERE child_id = ? AND status = 'pending' ORDER BY created_at DESC";
        try (Connection conn = DatabaseHelper.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, childId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                invites.add(new String[]{rs.getString(1), rs.getString(2)});
            }
        } catch (Exception e) {
            System.err.println("Error fetching parent invites: " + e.getMessage());
            e.printStackTrace();
        }
        return invites;
    }

    public static String sendGroupInvite(String groupId, String inviterId, String inviteeId) {
        String inviteId = UUID.randomUUID().toString();
        String sql = "INSERT INTO GROUP_INVITES (invite_id, group_id, inviter_id, invitee_id, status, created_at) VALUES (?, ?, ?, ?, 'pending', datetime('now'))";
        try (Connection conn = DatabaseHelper.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, inviteId);
            ps.setString(2, groupId);
            ps.setString(3, inviterId);
            ps.setString(4, inviteeId);
            ps.executeUpdate();
            return inviteId;
        } catch (Exception e) {
            System.err.println("Error sending group invite: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    public static boolean acceptGroupInvite(String inviteId) {
        String select = "SELECT group_id, invitee_id FROM GROUP_INVITES WHERE invite_id = ? AND status = 'pending'";
        String update = "UPDATE GROUP_INVITES SET status = 'accepted' WHERE invite_id = ?";
        try (Connection conn = DatabaseHelper.getConnection(); PreparedStatement sel = conn.prepareStatement(select)) {
            sel.setString(1, inviteId);
            ResultSet rs = sel.executeQuery();
            if (rs.next()) {
                String groupId = rs.getString(1);
                String inviteeId = rs.getString(2);
                // Add as regular member (not admin)
                String add = "INSERT INTO GROUP_MEMBERS (group_id, user_id, member_role, joined_at) VALUES (?, ?, 'member', datetime('now'))";
                try (PreparedStatement ad = conn.prepareStatement(add)) {
                    ad.setString(1, groupId);
                    ad.setString(2, inviteeId);
                    ad.executeUpdate();
                }
                try (PreparedStatement upd = conn.prepareStatement(update)) {
                    upd.setString(1, inviteId);
                    upd.executeUpdate();
                }
                return true;
            }
        } catch (Exception e) {
            System.err.println("Error accepting group invite: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    public static boolean declineGroupInvite(String inviteId) {
        String update = "UPDATE GROUP_INVITES SET status = 'declined' WHERE invite_id = ?";
        try (Connection conn = DatabaseHelper.getConnection(); PreparedStatement ps = conn.prepareStatement(update)) {
            ps.setString(1, inviteId);
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            System.err.println("Error declining group invite: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Check if there's a pending invite for a user to a group
     */
    public static boolean hasPendingGroupInvite(String groupId, String userId) {
        String sql = "SELECT COUNT(*) FROM GROUP_INVITES WHERE group_id = ? AND invitee_id = ? AND status = 'pending'";
        try (Connection conn = DatabaseHelper.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, groupId);
            ps.setString(2, userId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch (Exception e) {
            System.err.println("Error checking pending invite: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    public static List<String[]> getPendingGroupInvitesForUser(String userId) {
        List<String[]> invites = new ArrayList<>();
        String sql = "SELECT invite_id, group_id FROM GROUP_INVITES WHERE invitee_id = ? AND status = 'pending' ORDER BY created_at DESC";
        try (Connection conn = DatabaseHelper.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, userId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                invites.add(new String[]{rs.getString(1), rs.getString(2)});
            }
        } catch (Exception e) {
            System.err.println("Error fetching group invites: " + e.getMessage());
            e.printStackTrace();
        }
        return invites;
    }
}

