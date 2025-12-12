package org.example;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.example.service.DatabaseHelper;
import org.example.util.SessionManager;

/**
 * Main JavaFX Application Class
 * Initializes the database and launches the home screen
 */
public class MainApp extends Application {

    private static Stage primaryStage;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) throws Exception {
        primaryStage = stage;

        // Initialize thread pools
        org.example.util.ThreadPoolManager.getInstance();

        // Initialize database asynchronously
        org.example.util.ThreadPoolManager.getInstance().executeDatabase(() -> {
            DatabaseHelper.initialize();
            return null;
        }).thenRun(() -> {
            System.out.println("Database initialization complete");
        });

        // Load home screen
        loadHome();

        primaryStage.setTitle("Expense Tracker");
        primaryStage.setWidth(1200);
        primaryStage.setHeight(700);
        primaryStage.setMinWidth(1200);
        primaryStage.setMinHeight(700);
        primaryStage.setResizable(true);
        primaryStage.show();
    }

    /**
     * Load the home screen
     */
    public static void loadHome() {
        try {
            FXMLLoader loader = new FXMLLoader(MainApp.class.getResource("/fxml/home.fxml"));
            Parent root = loader.load();
            Scene scene = new Scene(root);
            scene.getStylesheets().add(MainApp.class.getResource("/css/styles.css").toExternalForm());
            primaryStage.setScene(scene);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Load login screen
     */
    public static void loadLogin() {
        try {
            FXMLLoader loader = new FXMLLoader(MainApp.class.getResource("/fxml/login.fxml"));
            Parent root = loader.load();
            Scene scene = new Scene(root);
            scene.getStylesheets().add(MainApp.class.getResource("/css/styles.css").toExternalForm());
            primaryStage.setScene(scene);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Load register screen
     */
    public static void loadRegister() {
        try {
            FXMLLoader loader = new FXMLLoader(MainApp.class.getResource("/fxml/register.fxml"));
            Parent root = loader.load();
            Scene scene = new Scene(root);
            scene.getStylesheets().add(MainApp.class.getResource("/css/styles.css").toExternalForm());
            primaryStage.setScene(scene);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Load mode selection screen (Personal/Group/Parent)
     */
    public static void loadModeSelection() {
        try {
            FXMLLoader loader = new FXMLLoader(MainApp.class.getResource("/fxml/mode_selection.fxml"));
            Parent root = loader.load();
            Scene scene = new Scene(root);
            scene.getStylesheets().add(MainApp.class.getResource("/css/styles.css").toExternalForm());
            primaryStage.setScene(scene);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Get the primary stage
     */
    public static Stage getPrimaryStage() {
        return primaryStage;
    }

    @Override
    public void stop() {
        // Cleanup when application closes
        SessionManager.clearSession();
        org.example.util.ThreadPoolManager.getInstance().shutdown();
        System.out.println("Application closed");
    }
}

