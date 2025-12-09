package org.example.controller;

import javafx.fxml.FXML;
import javafx.scene.control.*;

/**
 * Personal Dashboard Controller
 */
public class PersonalDashboardController {

    @FXML
    private Label userNameLabel;

    @FXML
    private Label modeLabel;

    @FXML
    private Label totalExpensesLabel;

    @FXML
    private Label monthExpensesLabel;

    @FXML
    private Label weekExpensesLabel;

    @FXML
    private Label transactionCountLabel;

    @FXML
    private TableView<?> expensesTable;

    @FXML
    private TableColumn<?, ?> dateColumn;

    @FXML
    private TableColumn<?, ?> categoryColumn;

    @FXML
    private TableColumn<?, ?> amountColumn;

    @FXML
    private TableColumn<?, ?> noteColumn;

    @FXML
    private TableColumn<?, ?> actionsColumn;

    @FXML
    private Button addExpenseButton;

    @FXML
    private Button viewAnalyticsButton;

    @FXML
    private Button viewAllButton;

    @FXML
    private Button backButton;

    @FXML
    public void initialize() {
        // Initialize controller
    }
}

