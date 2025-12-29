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
import org.example.service.UserService;
import org.example.util.SessionManager;

public class LoginController {

    @FXML
    private TextField emailField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private Button loginButton;

    @FXML
    private Label errorLabel;

    @FXML
    public void initialize() {
        if (passwordField != null) {
            passwordField.setOnKeyPressed(this::handleKeyPress);
        }
        if (emailField != null) {
            emailField.setOnKeyPressed(this::handleKeyPress);
        }
    }

    private void handleKeyPress(KeyEvent event) {
        if (event.getCode() == KeyCode.ENTER) {
            handleLogin();
        }
    }

    @FXML
    private void handleLogin() {
        String email = emailField.getText().trim();
        String password = passwordField.getText();

        if (email.isEmpty() || password.isEmpty()) {
            showError("Please enter both email and password");
            return;
        }

        if (!isValidEmail(email)) {
            showError("Please enter a valid email address");
            return;
        }

        loginButton.setDisable(true);
        loginButton.setText("Logging in...");
        hideError();

        var user = UserService.login(email, password);

        loginButton.setDisable(false);
        loginButton.setText("Login");

        if (user != null) {
            SessionManager.getInstance().setCurrentUser(user);
            MainApp.loadModeSelection();
        } else {
            showError("Invalid email or password. Please try again.");
            passwordField.clear();
        }
    }

    @FXML
    private void handleGoToRegister(MouseEvent event) {
        MainApp.loadRegister();
    }

    @FXML
    private void handleBack() {
        MainApp.loadHome();
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }

    private void hideError() {
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
    }

    private boolean isValidEmail(String email) {
        String emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";
        return email.matches(emailRegex);
    }
}