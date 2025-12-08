package org.example.service;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

/**
 * Database Helper Service
 * Manages SQLite database connection and initialization
 */
public class DatabaseHelper {

    private static final String DB_URL = "jdbc:sqlite:expense_tracker.db";

    /**
     * Get database connection
     */
    public static Connection getConnection() throws Exception {
        Class.forName("org.sqlite.JDBC");
        return DriverManager.getConnection(DB_URL);
    }

    /**
     * Initialize database tables
     */
    public static void initialize() {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {

            // Create USERS table
            String createUsersTable = """
                CREATE TABLE IF NOT EXISTS USERS (
                    user_id TEXT PRIMARY KEY,
                    name TEXT NOT NULL,
                    email TEXT UNIQUE NOT NULL,
                    password TEXT NOT NULL,
                    role TEXT NOT NULL DEFAULT 'normal'
                )
            """;
            stmt.execute(createUsersTable);

            // Create GROUPS table
            String createGroupsTable = """
                CREATE TABLE IF NOT EXISTS GROUPS (
                    group_id TEXT PRIMARY KEY,
                    group_name TEXT NOT NULL
                )
            """;
            stmt.execute(createGroupsTable);

            // Create GROUP_MEMBERS table
            String createGroupMembersTable = """
                CREATE TABLE IF NOT EXISTS GROUP_MEMBERS (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    group_id TEXT NOT NULL,
                    user_id TEXT NOT NULL,
                    FOREIGN KEY (group_id) REFERENCES GROUPS(group_id),
                    FOREIGN KEY (user_id) REFERENCES USERS(user_id)
                )
            """;
            stmt.execute(createGroupMembersTable);

            // Create EXPENSES table
            String createExpensesTable = """
                CREATE TABLE IF NOT EXISTS EXPENSES (
                    expense_id TEXT PRIMARY KEY,
                    user_id TEXT NOT NULL,
                    group_id TEXT,
                    category TEXT NOT NULL,
                    amount REAL NOT NULL,
                    date TEXT NOT NULL,
                    note TEXT,
                    FOREIGN KEY (user_id) REFERENCES USERS(user_id),
                    FOREIGN KEY (group_id) REFERENCES GROUPS(group_id)
                )
            """;
            stmt.execute(createExpensesTable);

            // Create PARENT_RELATION table
            String createParentRelationTable = """
                CREATE TABLE IF NOT EXISTS PARENT_RELATION (
                    parent_id TEXT NOT NULL,
                    child_id TEXT NOT NULL,
                    PRIMARY KEY (parent_id, child_id),
                    FOREIGN KEY (parent_id) REFERENCES USERS(user_id),
                    FOREIGN KEY (child_id) REFERENCES USERS(user_id)
                )
            """;
            stmt.execute(createParentRelationTable);

            System.out.println("Database initialized successfully");

        } catch (Exception e) {
            System.err.println("Error initializing database: " + e.getMessage());
            e.printStackTrace();
        }
    }
}

