package org.example.controller;

import javafx.fxml.FXML;
import org.example.MainApp;

public class HomeController {

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