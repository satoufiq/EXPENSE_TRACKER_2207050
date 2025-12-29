package org.example.model;

/**
 * GroupMember Model Class
 * Represents a member in a group with their user details and group-specific role
 */
public class GroupMember {
    private String oderId;
    private String name;
    private String email;
    private String role;         // User's global role (normal/parent)
    private String groupId;
    private String memberRole;   // Group-specific role (admin/member)
    private String joinedAt;

    public GroupMember() {
    }

    public GroupMember(String oderId, String name, String email, String role, String groupId) {
        this.oderId = oderId;
        this.name = name;
        this.email = email;
        this.role = role;
        this.groupId = groupId;
        this.memberRole = "member"; // Default to member
    }

    public GroupMember(String oderId, String name, String email, String role, String groupId, String memberRole) {
        this.oderId = oderId;
        this.name = name;
        this.email = email;
        this.role = role;
        this.groupId = groupId;
        this.memberRole = memberRole;
    }

    // Getters and Setters
    public String getUserId() {
        return oderId;
    }

    public void setUserId(String oderId) {
        this.oderId = oderId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public String getMemberRole() {
        return memberRole;
    }

    public void setMemberRole(String memberRole) {
        this.memberRole = memberRole;
    }

    public String getJoinedAt() {
        return joinedAt;
    }

    public void setJoinedAt(String joinedAt) {
        this.joinedAt = joinedAt;
    }

    public boolean isAdmin() {
        return "admin".equalsIgnoreCase(memberRole);
    }

    @Override
    public String toString() {
        return name + " (" + email + ")" + (isAdmin() ? " 👑" : "");
    }
}

