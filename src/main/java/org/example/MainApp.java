package org.example;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import org.example.service.DatabaseHelper;
import org.example.util.SessionManager;

import java.io.InputStream;

public class MainApp extends Application {

    private static Stage primaryStage;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) throws Exception {
        primaryStage = stage;

        DatabaseHelper.initialize();
        System.out.println("Database initialization complete");

        primaryStage.setTitle("Expense Tracker");
        primaryStage.setWidth(1200);
        primaryStage.setHeight(700);
        primaryStage.setMinWidth(1200);
        primaryStage.setMinHeight(700);
        primaryStage.setResizable(true);

        loadAppIcon();

        loadHome();

        primaryStage.show();
    }

    private static void loadAppIcon() {
        try {
            InputStream iconStream = MainApp.class.getResourceAsStream("/icons/app-icon.png");
            if (iconStream != null) {
                Image icon = new Image(iconStream);
                primaryStage.getIcons().add(icon);
                System.out.println("✓ Application icon loaded successfully");
            } else {
                System.out.println("✗ Icon file not found: /icons/app-icon.png");
            }

        } catch (Exception e) {
            System.err.println("✗ Error loading application icon: " + e.getMessage());
            e.printStackTrace();
        }
    }

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

    public static Stage getPrimaryStage() {
        return primaryStage;
    }

    @Override
    public void stop() {
        SessionManager.clearSession();
        System.out.println("Application closed");
    }
}

