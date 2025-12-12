package org.example.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import org.example.MainApp;
import org.example.model.User;
import org.example.service.UserService;
import org.example.util.SessionManager;

/**
 * Login Controller
 * Handles user login functionality
 */
public class LoginController {

    @FXML
    private TextField emailField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private Button loginButton;

    @FXML
    private Button backButton;

    @FXML
    private Label errorLabel;

    @FXML
    public void initialize() {
        // Add Enter key listener to password field for quick login
        if (passwordField != null) {
            passwordField.setOnKeyPressed(this::handleKeyPress);
        }
        if (emailField != null) {
            emailField.setOnKeyPressed(this::handleKeyPress);
        }
    }

    /**
     * Handle Enter key press for login
     */
    private void handleKeyPress(KeyEvent event) {
        if (event.getCode() == KeyCode.ENTER) {
            handleLogin();
        }
    }

    /**
     * Handle login button action
     */
    @FXML
    private void handleLogin() {
        String email = emailField.getText().trim();
        String password = passwordField.getText();

        // Validate inputs
        if (email.isEmpty() || password.isEmpty()) {
            showError("Please enter both email and password");
            return;
        }

        // Validate email format
        if (!isValidEmail(email)) {
            showError("Please enter a valid email address");
            return;
        }

        // Disable button and show loading state
        loginButton.setDisable(true);
        loginButton.setText("Logging in...");
        hideError();

        // Attempt login asynchronously
        org.example.util.ThreadPoolManager.getInstance().executeDatabaseWithCallback(
            () -> UserService.login(email, password),
            user -> {
                // Re-enable button
                loginButton.setDisable(false);
                loginButton.setText("Login");

                if (user != null) {
                    // Login successful
                    SessionManager.getInstance().setCurrentUser(user);
                    // Navigate to mode selection screen
                    MainApp.loadModeSelection();
                } else {
                    // Login failed
                    showError("Invalid email or password. Please try again.");
                    passwordField.clear();
                }
            },
            error -> {
                // Re-enable button on error
                loginButton.setDisable(false);
                loginButton.setText("Login");
                showError("Login error: " + error.getMessage());
                error.printStackTrace();
            }
        );
    }

    /**
     * Navigate to register screen
     */
    @FXML
    private void handleGoToRegister(MouseEvent event) {
        MainApp.loadRegister();
    }

    /**
     * Navigate back to home screen
     */
    @FXML
    private void handleBack() {
        MainApp.loadHome();
    }

    /**
     * Display error message
     */
    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }

    /**
     * Hide error message
     */
    private void hideError() {
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
    }

    /**
     * Validate email format
     */
    private boolean isValidEmail(String email) {
        String emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";
        return email.matches(emailRegex);
    }
}
