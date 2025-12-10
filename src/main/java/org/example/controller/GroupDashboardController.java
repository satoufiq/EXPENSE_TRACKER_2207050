package org.example.controller;

import javafx.fxml.FXML;
import javafx.scene.control.*;

/**
 * Group Dashboard Controller
 */
public class GroupDashboardController {

    @FXML
    private Label groupNameLabel;

    @FXML
    private Label memberCountLabel;

    @FXML
    private Label totalExpensesLabel;

    @FXML
    private Label monthExpensesLabel;

    @FXML
    private Label activeMembersLabel;

    @FXML
    private Label transactionCountLabel;

    @FXML
    private TableView<?> expensesTable;

    @FXML
    private TableColumn<?, ?> dateColumn;

    @FXML
    private TableColumn<?, ?> memberColumn;

    @FXML
    private TableColumn<?, ?> categoryColumn;

    @FXML
    private TableColumn<?, ?> amountColumn;

    @FXML
    private TableColumn<?, ?> noteColumn;

    @FXML
    private TableColumn<?, ?> actionsColumn;

    @FXML
    private Button addMemberButton;

    @FXML
    private Button addExpenseButton;

    @FXML
    private Button viewMembersButton;

    @FXML
    private Button backButton;

    @FXML
    public void initialize() {
        // Initialize controller
    }
}

