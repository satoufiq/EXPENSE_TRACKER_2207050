package org.example.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import org.example.MainApp;

/**
 * Home Controller
 * Landing page with options to Login or Register
 */
public class HomeController {

    @FXML
    private Button loginButton;

    @FXML
    private Button registerButton;

    @FXML
    private Button exitButton;

    @FXML
    public void initialize() {
        // Additional initialization if needed
    }

    @FXML
    private void handleLogin() {
        MainApp.loadLogin();
    }

    @FXML
    private void handleRegister() {
        MainApp.loadRegister();
    }

    @FXML
    private void handleExit() {
        System.exit(0);
    }
}

