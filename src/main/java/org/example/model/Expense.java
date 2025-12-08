package org.example.model;

/**
 * Expense Model Class
 * Represents an expense record
 */
public class Expense {
    private String expenseId;
    private String userId;
    private String groupId;
    private String category;
    private double amount;
    private String date;
    private String note;

    public Expense() {
    }

    public Expense(String expenseId, String userId, String groupId, String category,
                   double amount, String date, String note) {
        this.expenseId = expenseId;
        this.userId = userId;
        this.groupId = groupId;
        this.category = category;
        this.amount = amount;
        this.date = date;
        this.note = note;
    }

    // Getters and Setters
    public String getExpenseId() {
        return expenseId;
    }

    public void setExpenseId(String expenseId) {
        this.expenseId = expenseId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    @Override
    public String toString() {
        return "Expense{" +
                "expenseId='" + expenseId + '\'' +
                ", userId='" + userId + '\'' +
                ", groupId='" + groupId + '\'' +
                ", category='" + category + '\'' +
                ", amount=" + amount +
                ", date='" + date + '\'' +
                ", note='" + note + '\'' +
                '}';
    }
}

