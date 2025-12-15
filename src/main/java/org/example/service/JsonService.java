package org.example.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import javafx.collections.ObservableList;
import org.example.model.Expense;
import org.example.model.Group;
import org.example.model.GroupMember;
import org.example.model.User;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

/**
 * JSON Service for import/export functionality with ObservableList support
 */
public class JsonService {

    private static final Gson gson = new GsonBuilder()
            .setPrettyPrinting()
            .setDateFormat("yyyy-MM-dd")
            .create();

    // ==================== EXPENSE JSON OPERATIONS ====================

    /**
     * Export expenses to JSON file
     */
    public static boolean exportExpensesToJson(ObservableList<Expense> expenses, String filePath) {
        try (Writer writer = new OutputStreamWriter(new FileOutputStream(filePath), StandardCharsets.UTF_8)) {
            gson.toJson(expenses, writer);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Import expenses from JSON file
     */
    public static List<Expense> importExpensesFromJson(String filePath) {
        try (Reader reader = new InputStreamReader(new FileInputStream(filePath), StandardCharsets.UTF_8)) {
            Type expenseListType = new TypeToken<List<Expense>>(){}.getType();
            return gson.fromJson(reader, expenseListType);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Export single expense to JSON string
     */
    public static String expenseToJson(Expense expense) {
        return gson.toJson(expense);
    }

    /**
     * Import single expense from JSON string
     */
    public static Expense expenseFromJson(String json) {
        return gson.fromJson(json, Expense.class);
    }

    // ==================== GROUP JSON OPERATIONS ====================

    /**
     * Export groups to JSON file
     */
    public static boolean exportGroupsToJson(List<Group> groups, String filePath) {
        try (Writer writer = new OutputStreamWriter(new FileOutputStream(filePath), StandardCharsets.UTF_8)) {
            gson.toJson(groups, writer);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Import groups from JSON file
     */
    public static List<Group> importGroupsFromJson(String filePath) {
        try (Reader reader = new InputStreamReader(new FileInputStream(filePath), StandardCharsets.UTF_8)) {
            Type groupListType = new TypeToken<List<Group>>(){}.getType();
            return gson.fromJson(reader, groupListType);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    // ==================== USER JSON OPERATIONS ====================

    /**
     * Export user to JSON string
     */
    public static String userToJson(User user) {
        return gson.toJson(user);
    }

    /**
     * Import user from JSON string
     */
    public static User userFromJson(String json) {
        return gson.fromJson(json, User.class);
    }

    // ==================== GROUP MEMBER JSON OPERATIONS ====================

    /**
     * Export group members to JSON file
     */
    public static boolean exportGroupMembersToJson(ObservableList<GroupMember> members, String filePath) {
        try (Writer writer = new OutputStreamWriter(new FileOutputStream(filePath), StandardCharsets.UTF_8)) {
            gson.toJson(members, writer);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Import group members from JSON file
     */
    public static List<GroupMember> importGroupMembersFromJson(String filePath) {
        try (Reader reader = new InputStreamReader(new FileInputStream(filePath), StandardCharsets.UTF_8)) {
            Type memberListType = new TypeToken<List<GroupMember>>(){}.getType();
            return gson.fromJson(reader, memberListType);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    // ==================== BACKUP/RESTORE OPERATIONS ====================

    /**
     * Create full backup of all data to JSON
     */
    public static boolean createFullBackup(String backupDir) {
        try {
            Files.createDirectories(Paths.get(backupDir));

            String timestamp = String.valueOf(System.currentTimeMillis());

            // Backup all expenses
            List<Expense> allExpenses = ExpenseService.getAllExpensesObservable();
            exportExpensesToJson((ObservableList<Expense>) allExpenses,
                backupDir + "/expenses_" + timestamp + ".json");

            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Read JSON file as string
     */
    public static String readJsonFile(String filePath) {
        try {
            return new String(Files.readAllBytes(Paths.get(filePath)), StandardCharsets.UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Write JSON string to file
     */
    public static boolean writeJsonFile(String filePath, String jsonContent) {
        try (Writer writer = new OutputStreamWriter(new FileOutputStream(filePath), StandardCharsets.UTF_8)) {
            writer.write(jsonContent);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Pretty print JSON string
     */
    public static String prettyPrintJson(String json) {
        Object jsonObject = gson.fromJson(json, Object.class);
        return gson.toJson(jsonObject);
    }

    /**
     * Validate JSON string
     */
    public static boolean isValidJson(String json) {
        try {
            gson.fromJson(json, Object.class);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}

