package org.example.model;

/**
 * Parent-Child Alert Model
 * Represents alerts sent from child to parent or suggestions from parent to child
 */
public class ParentChildAlert {
    private String alertId;
    private String fromUserId;
    private String toUserId;
    private String type; // "alert" or "suggestion"
    private String message;
    private String createdAt;
    private String readStatus; // "read" or "unread"

    // For display purposes
    private String fromUserName;
    private String toUserName;

    public ParentChildAlert() {}

    public ParentChildAlert(String alertId, String fromUserId, String toUserId, String type,
                           String message, String createdAt, String readStatus) {
        this.alertId = alertId;
        this.fromUserId = fromUserId;
        this.toUserId = toUserId;
        this.type = type;
        this.message = message;
        this.createdAt = createdAt;
        this.readStatus = readStatus;
    }

    // Getters and Setters
    public String getAlertId() { return alertId; }
    public void setAlertId(String alertId) { this.alertId = alertId; }

    public String getFromUserId() { return fromUserId; }
    public void setFromUserId(String fromUserId) { this.fromUserId = fromUserId; }

    public String getToUserId() { return toUserId; }
    public void setToUserId(String toUserId) { this.toUserId = toUserId; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public String getReadStatus() { return readStatus; }
    public void setReadStatus(String readStatus) { this.readStatus = readStatus; }

    public String getFromUserName() { return fromUserName; }
    public void setFromUserName(String fromUserName) { this.fromUserName = fromUserName; }

    public String getToUserName() { return toUserName; }
    public void setToUserName(String toUserName) { this.toUserName = toUserName; }

    @Override
    public String toString() {
        return "ParentChildAlert{" +
                "alertId='" + alertId + '\'' +
                ", type='" + type + '\'' +
                ", message='" + message + '\'' +
                ", createdAt='" + createdAt + '\'' +
                ", readStatus='" + readStatus + '\'' +
                '}';
    }
}

