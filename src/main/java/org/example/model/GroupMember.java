package org.example.model;

/**
 * GroupMember Model Class
 * Represents a member in a group with their user details
 */
public class GroupMember {
    private String userId;
    private String name;
    private String email;
    private String role;
    private String groupId;

    public GroupMember() {
    }

    public GroupMember(String userId, String name, String email, String role, String groupId) {
        this.userId = userId;
        this.name = name;
        this.email = email;
        this.role = role;
        this.groupId = groupId;
    }

    // Getters and Setters
    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
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

    @Override
    public String toString() {
        return name + " (" + email + ")";
    }
}

