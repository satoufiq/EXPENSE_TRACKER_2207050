package org.example.controller;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import org.example.MainApp;
import org.example.service.UserService;

public class RegisterController {

    @FXML private TextField nameField;
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private RadioButton normalUserRadio;
    @FXML private RadioButton parentUserRadio;
    @FXML private ToggleGroup roleGroup;
    @FXML private Button registerButton;
    @FXML private Button backButton;
    @FXML private Label errorLabel;

    // Password strength indicator components
    @FXML private HBox strengthBar1;
    @FXML private HBox strengthBar2;
    @FXML private HBox strengthBar3;
    @FXML private HBox strengthBar4;
    @FXML private Label strengthLabel;

    // Observable property for password strength
    private final StringProperty passwordStrength = new SimpleStringProperty("");

    @FXML
    public void initialize() {
        if (confirmPasswordField != null) {
            confirmPasswordField.setOnKeyPressed(this::handleKeyPress);
        }

        // Bind password strength label to observable property
        if (strengthLabel != null) {
            strengthLabel.textProperty().bind(passwordStrength);
        }

        // Add listener to password field to calculate strength in real-time
        if (passwordField != null) {
            passwordField.textProperty().addListener((observable, oldValue, newValue) -> {
                updatePasswordStrength(newValue);
            });
        }
    }

    private void updatePasswordStrength(String password) {
        int strength = calculatePasswordStrength(password);

        // Reset all bars to default
        String defaultStyle = "-fx-background-color: rgba(255,255,255,0.2); -fx-background-radius: 2;";
        String weakStyle = "-fx-background-color: #f44336; -fx-background-radius: 2;";
        String fairStyle = "-fx-background-color: #ff9800; -fx-background-radius: 2;";
        String goodStyle = "-fx-background-color: #ffeb3b; -fx-background-radius: 2;";
        String strongStyle = "-fx-background-color: #4caf50; -fx-background-radius: 2;";

        if (strengthBar1 != null) strengthBar1.setStyle(defaultStyle);
        if (strengthBar2 != null) strengthBar2.setStyle(defaultStyle);
        if (strengthBar3 != null) strengthBar3.setStyle(defaultStyle);
        if (strengthBar4 != null) strengthBar4.setStyle(defaultStyle);

        if (password == null || password.isEmpty()) {
            passwordStrength.set("");
            return;
        }

        switch (strength) {
            case 1:
                if (strengthBar1 != null) strengthBar1.setStyle(weakStyle);
                passwordStrength.set("Weak");
                if (strengthLabel != null) strengthLabel.setStyle("-fx-text-fill: #f44336; -fx-font-size: 11px; -fx-font-weight: bold;");
                break;
            case 2:
                if (strengthBar1 != null) strengthBar1.setStyle(fairStyle);
                if (strengthBar2 != null) strengthBar2.setStyle(fairStyle);
                passwordStrength.set("Fair");
                if (strengthLabel != null) strengthLabel.setStyle("-fx-text-fill: #ff9800; -fx-font-size: 11px; -fx-font-weight: bold;");
                break;
            case 3:
                if (strengthBar1 != null) strengthBar1.setStyle(goodStyle);
                if (strengthBar2 != null) strengthBar2.setStyle(goodStyle);
                if (strengthBar3 != null) strengthBar3.setStyle(goodStyle);
                passwordStrength.set("Good");
                if (strengthLabel != null) strengthLabel.setStyle("-fx-text-fill: #ffeb3b; -fx-font-size: 11px; -fx-font-weight: bold;");
                break;
            case 4:
                if (strengthBar1 != null) strengthBar1.setStyle(strongStyle);
                if (strengthBar2 != null) strengthBar2.setStyle(strongStyle);
                if (strengthBar3 != null) strengthBar3.setStyle(strongStyle);
                if (strengthBar4 != null) strengthBar4.setStyle(strongStyle);
                passwordStrength.set("Strong 💪");
                if (strengthLabel != null) strengthLabel.setStyle("-fx-text-fill: #4caf50; -fx-font-size: 11px; -fx-font-weight: bold;");
                break;
            default:
                passwordStrength.set("");
                break;
        }
    }

    private int calculatePasswordStrength(String password) {
        if (password == null || password.isEmpty()) return 0;

        int score = 0;

        // Length checks
        if (password.length() >= 6) score++;
        if (password.length() >= 8) score++;
        if (password.length() >= 12) score++;

        // Character type checks
        boolean hasLower = password.matches(".*[a-z].*");
        boolean hasUpper = password.matches(".*[A-Z].*");
        boolean hasDigit = password.matches(".*\\d.*");
        boolean hasSpecial = password.matches(".*[!@#$%^&*(),.?\":{}|<>].*");

        int typeCount = 0;
        if (hasLower) typeCount++;
        if (hasUpper) typeCount++;
        if (hasDigit) typeCount++;
        if (hasSpecial) typeCount++;

        if (typeCount >= 2) score++;
        if (typeCount >= 3) score++;
        if (typeCount >= 4) score++;

        // Cap at 4
        if (score > 4) score = 4;
        if (score < 1 && password.length() > 0) score = 1;

        return score;
    }

    private void handleKeyPress(KeyEvent event) {
        if (event.getCode() == KeyCode.ENTER) {
            handleRegister();
        }
    }

    @FXML
    private void handleRegister() {
        String name = nameField.getText().trim();
        String email = emailField.getText().trim();
        String password = passwordField.getText();
        String confirmPassword = confirmPasswordField.getText();

        if (name.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            showError("Please fill in all fields");
            return;
        }

        if (name.length() < 2) {
            showError("Name must be at least 2 characters long");
            return;
        }

        if (!isValidEmail(email)) {
            showError("Please enter a valid email address");
            return;
        }

        if (password.length() < 6) {
            showError("Password must be at least 6 characters long");
            return;
        }

        if (!password.equals(confirmPassword)) {
            showError("Passwords do not match");
            confirmPasswordField.clear();
            return;
        }

        String role = normalUserRadio.isSelected() ? "normal" : "parent";

        registerButton.setDisable(true);
        registerButton.setText("Creating Account...");
        hideError();

        boolean emailExists = UserService.emailExists(email);

        if (emailExists) {
            registerButton.setDisable(false);
            registerButton.setText("Create Account");
            showError("This email is already registered. Please login or use a different email.");
            return;
        }

        boolean success = UserService.register(name, email, password, role);
        registerButton.setDisable(false);
        registerButton.setText("Create Account");

        if (success) {
            hideError();
            showSuccessAndRedirect();
        } else {
            showError("Registration failed. Please try again.");
        }
    }

    @FXML
    private void handleGoToLogin(MouseEvent event) {
        MainApp.loadLogin();
    }

    @FXML
    private void handleBack() {
        MainApp.loadHome();
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
        errorLabel.setStyle("-fx-text-fill: #ff4444;");
    }

    private void hideError() {
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
    }

    private void showSuccessAndRedirect() {
        errorLabel.setText("✓ Account created successfully! Redirecting to login...");
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
        errorLabel.setStyle("-fx-text-fill: #4CAF50;");

        registerButton.setDisable(true);

        javafx.animation.PauseTransition pause = new javafx.animation.PauseTransition(javafx.util.Duration.seconds(1.5));
        pause.setOnFinished(event -> MainApp.loadLogin());
        pause.play();
    }

    private boolean isValidEmail(String email) {
        String emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";
        return email.matches(emailRegex);
    }
}

