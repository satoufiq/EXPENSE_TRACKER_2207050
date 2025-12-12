package org.example.controller;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import org.example.MainApp;
import org.example.service.UserService;

/**
 * Register Controller
 * Handles user registration functionality
 */
public class RegisterController {

    @FXML
    private TextField nameField;

    @FXML
    private TextField emailField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private PasswordField confirmPasswordField;

    @FXML
    private RadioButton normalUserRadio;

    @FXML
    private RadioButton parentUserRadio;

    @FXML
    private ToggleGroup roleGroup;

    @FXML
    private Button registerButton;

    @FXML
    private Button backButton;

    @FXML
    private Label errorLabel;

    @FXML
    public void initialize() {
        // Add Enter key listener for quick registration
        if (confirmPasswordField != null) {
            confirmPasswordField.setOnKeyPressed(this::handleKeyPress);
        }
    }

    /**
     * Handle Enter key press for registration
     */
    private void handleKeyPress(KeyEvent event) {
        if (event.getCode() == KeyCode.ENTER) {
            handleRegister();
        }
    }

    /**
     * Handle register button action
     */
    @FXML
    private void handleRegister() {
        String name = nameField.getText().trim();
        String email = emailField.getText().trim();
        String password = passwordField.getText();
        String confirmPassword = confirmPasswordField.getText();

        // Validate inputs
        if (name.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            showError("Please fill in all fields");
            return;
        }

        // Validate name (at least 2 characters)
        if (name.length() < 2) {
            showError("Name must be at least 2 characters long");
            return;
        }

        // Validate email format
        if (!isValidEmail(email)) {
            showError("Please enter a valid email address");
            return;
        }

        // Check if email already exists
        if (UserService.emailExists(email)) {
            showError("This email is already registered. Please login or use a different email.");
            return;
        }

        // Validate password strength
        if (password.length() < 6) {
            showError("Password must be at least 6 characters long");
            return;
        }

        // Confirm password match
        if (!password.equals(confirmPassword)) {
            showError("Passwords do not match");
            confirmPasswordField.clear();
            return;
        }

        // Get selected role
        String role = normalUserRadio.isSelected() ? "normal" : "parent";

        // Disable button and show loading state
        registerButton.setDisable(true);
        registerButton.setText("Creating Account...");
        hideError();

        // Check email exists asynchronously first
        org.example.util.ThreadPoolManager.getInstance().executeDatabaseWithCallback(
            () -> UserService.emailExists(email),
            emailExists -> {
                if (emailExists) {
                    registerButton.setDisable(false);
                    registerButton.setText("Register");
                    showError("This email is already registered. Please login or use a different email.");
                    return;
                }

                // Email is available, proceed with registration
                org.example.util.ThreadPoolManager.getInstance().executeDatabaseWithCallback(
                    () -> UserService.register(name, email, password, role),
                    success -> {
                        registerButton.setDisable(false);
                        registerButton.setText("Register");

                        if (success) {
                            // Registration successful
                            hideError();
                            showSuccessAndRedirect();
                        } else {
                            // Registration failed
                            showError("Registration failed. Please try again.");
                        }
                    },
                    error -> {
                        registerButton.setDisable(false);
                        registerButton.setText("Register");
                        showError("Registration error: " + error.getMessage());
                        error.printStackTrace();
                    }
                );
            },
            error -> {
                registerButton.setDisable(false);
                registerButton.setText("Register");
                showError("Error checking email: " + error.getMessage());
                error.printStackTrace();
            }
        );
    }

    /**
     * Navigate to login screen
     */
    @FXML
    private void handleGoToLogin(MouseEvent event) {
        MainApp.loadLogin();
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
        errorLabel.setStyle("-fx-text-fill: #ff4444;");
    }

    /**
     * Hide error message
     */
    private void hideError() {
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
    }

    /**
     * Show success message and redirect to login
     */
    private void showSuccessAndRedirect() {
        errorLabel.setText("✓ Account created successfully! Redirecting to login...");
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
        errorLabel.setStyle("-fx-text-fill: #4CAF50;");

        // Disable the register button to prevent double submission
        registerButton.setDisable(true);

        // Redirect to login after a short delay using thread pool
        org.example.util.ThreadPoolManager.getInstance().executeBackground(() -> {
            Thread.sleep(1500); // 1.5 seconds delay
            return null;
        }).thenRun(() -> {
            org.example.util.ThreadPoolManager.runOnUIThread(() -> MainApp.loadLogin());
        });
    }

    /**
     * Validate email format
     */
    private boolean isValidEmail(String email) {
        String emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";
        return email.matches(emailRegex);
    }
}

