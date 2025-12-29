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

            // Create GROUP_MEMBERS table with member role
            String createGroupMembersTable = """
                CREATE TABLE IF NOT EXISTS GROUP_MEMBERS (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    group_id TEXT NOT NULL,
                    user_id TEXT NOT NULL,
                    member_role TEXT NOT NULL DEFAULT 'member',
                    joined_at TEXT,
                    FOREIGN KEY (group_id) REFERENCES GROUPS(group_id),
                    FOREIGN KEY (user_id) REFERENCES USERS(user_id)
                )
            """;
            stmt.execute(createGroupMembersTable);

            // Add member_role column if not exists (for migration)
            try {
                stmt.execute("ALTER TABLE GROUP_MEMBERS ADD COLUMN member_role TEXT NOT NULL DEFAULT 'member'");
            } catch (Exception ignored) {} // Column may already exist

            try {
                stmt.execute("ALTER TABLE GROUP_MEMBERS ADD COLUMN joined_at TEXT");
            } catch (Exception ignored) {} // Column may already exist

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

            // Create PARENT_INVITES table
            String createParentInvitesTable = """
                CREATE TABLE IF NOT EXISTS PARENT_INVITES (
                    invite_id TEXT PRIMARY KEY,
                    parent_id TEXT NOT NULL,
                    child_id TEXT NOT NULL,
                    status TEXT NOT NULL DEFAULT 'pending', -- 'pending','accepted','declined'
                    created_at TEXT NOT NULL,
                    FOREIGN KEY (parent_id) REFERENCES USERS(user_id),
                    FOREIGN KEY (child_id) REFERENCES USERS(user_id)
                )
            """;
            stmt.execute(createParentInvitesTable);

            // Create GROUP_INVITES table
            String createGroupInvitesTable = """
                CREATE TABLE IF NOT EXISTS GROUP_INVITES (
                    invite_id TEXT PRIMARY KEY,
                    group_id TEXT NOT NULL,
                    inviter_id TEXT NOT NULL,
                    invitee_id TEXT NOT NULL,
                    status TEXT NOT NULL DEFAULT 'pending', -- 'pending','accepted','declined'
                    created_at TEXT NOT NULL,
                    FOREIGN KEY (group_id) REFERENCES GROUPS(group_id),
                    FOREIGN KEY (inviter_id) REFERENCES USERS(user_id),
                    FOREIGN KEY (invitee_id) REFERENCES USERS(user_id)
                )
            """;
            stmt.execute(createGroupInvitesTable);

            // Create BUDGETS table (per-user monthly budget)
            String createBudgetsTable = """
                CREATE TABLE IF NOT EXISTS BUDGETS (
                    user_id TEXT PRIMARY KEY,
                    monthly_budget REAL NOT NULL DEFAULT 0,
                    currency TEXT NOT NULL DEFAULT 'BDT',
                    updated_at TEXT NOT NULL,
                    FOREIGN KEY (user_id) REFERENCES USERS(user_id)
                )
            """;
            stmt.execute(createBudgetsTable);

            // Create GROUP_BUDGETS table (per-group monthly budget)
            String createGroupBudgetsTable = """
                CREATE TABLE IF NOT EXISTS GROUP_BUDGETS (
                    group_id TEXT PRIMARY KEY,
                    monthly_budget REAL NOT NULL DEFAULT 0,
                    currency TEXT NOT NULL DEFAULT 'BDT',
                    updated_at TEXT NOT NULL,
                    FOREIGN KEY (group_id) REFERENCES GROUPS(group_id)
                )
            """;
            stmt.execute(createGroupBudgetsTable);

            // Create PARENT_CHILD_ALERTS table (for alerts from child to parent and suggestions from parent to child)
            String createAlertsTable = """
                CREATE TABLE IF NOT EXISTS PARENT_CHILD_ALERTS (
                    alert_id TEXT PRIMARY KEY,
                    from_user_id TEXT NOT NULL,
                    to_user_id TEXT NOT NULL,
                    type TEXT NOT NULL,
                    message TEXT NOT NULL,
                    created_at TEXT NOT NULL,
                    read_status TEXT NOT NULL DEFAULT 'unread',
                    FOREIGN KEY (from_user_id) REFERENCES USERS(user_id),
                    FOREIGN KEY (to_user_id) REFERENCES USERS(user_id)
                )
            """;
            stmt.execute(createAlertsTable);

            System.out.println("Database initialized successfully");

        } catch (Exception e) {
            System.err.println("Error initializing database: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
