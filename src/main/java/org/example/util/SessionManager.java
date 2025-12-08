package org.example.util;

import org.example.model.User;

/**
 * Session Manager to track logged-in user
 * Singleton pattern to maintain user session across the application
 */
public class SessionManager {
    private static SessionManager instance;
    private User currentUser;
    private String currentMode; // "personal", "group", or "parent"
    private String currentGroupId; // For group mode

    private SessionManager() {
    }

    public static SessionManager getInstance() {
        if (instance == null) {
            instance = new SessionManager();
        }
        return instance;
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
    }

    public User getCurrentUser() {
        return currentUser;
    }

    public void setCurrentMode(String mode) {
        this.currentMode = mode;
    }

    public String getCurrentMode() {
        return currentMode;
    }

    public void setCurrentGroupId(String groupId) {
        this.currentGroupId = groupId;
    }

    public String getCurrentGroupId() {
        return currentGroupId;
    }

    public static void clearSession() {
        if (instance != null) {
            instance.currentUser = null;
            instance.currentMode = null;
            instance.currentGroupId = null;
        }
    }

    public boolean isLoggedIn() {
        return currentUser != null;
    }
}

