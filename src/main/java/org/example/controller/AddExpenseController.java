package org.example.controller;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import org.example.service.ExpenseService;
import org.example.util.SessionManager;

import java.time.LocalDate;

/**
 * Controller for Add Expense dialog
 * Works with ObservableList for automatic UI updates
 */
public class AddExpenseController {

    @FXML
    private ComboBox<String> categoryComboBox;

    @FXML
    private TextField amountField;

    @FXML
    private DatePicker datePicker;

    @FXML
    private TextArea noteArea;

    @FXML
    private Button saveButton;

    @FXML
    private Button cancelButton;

    private String userId;
    private String groupId;
    private boolean isPersonalMode = true;

    @FXML
    public void initialize() {
        // Initialize categories with emojis for better UX
        categoryComboBox.getItems().addAll(
            "🍔 Food",
            "🚗 Transport",
            "🛍️ Shopping",
            "🎬 Entertainment",
            "💡 Bills",
            "🏥 Healthcare",
            "📚 Education",
            "🏠 Housing",
            "💰 Savings",
            "📱 Technology",
            "✈️ Travel",
            "🎁 Gifts",
            "👔 Clothing",
            "🏋️ Fitness",
            "📝 Other"
        );

        // Set default date to today
        datePicker.setValue(LocalDate.now());

        // Get user from session
        SessionManager session = SessionManager.getInstance();
        if (session.getCurrentUser() != null) {
            userId = session.getCurrentUser().getUserId();
        }

        // Determine if personal or group mode
        groupId = session.getCurrentGroupId();
        isPersonalMode = (groupId == null || groupId.isEmpty());

        // Add input validation listeners
        setupValidation();
    }

    /**
     * Setup real-time validation
     */
    private void setupValidation() {
        // Only allow numbers and decimal point in amount field
        amountField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.matches("\\d*\\.?\\d{0,2}")) {
                amountField.setText(oldValue);
            }
        });
    }

    /**
     * Handle save button click
     */
    @FXML
    private void handleSave() {
        if (validateInput()) {
            // Extract category without emoji
            String categoryWithEmoji = categoryComboBox.getValue();
            String category = categoryWithEmoji.substring(categoryWithEmoji.indexOf(" ") + 1);

            double amount = Double.parseDouble(amountField.getText());
            String date = datePicker.getValue().toString();
            String note = noteArea.getText().trim();

            // If note is empty, set a default message
            if (note.isEmpty()) {
                note = "No note";
            }

            // Add expense - ObservableList will auto-update the UI
            boolean success = ExpenseService.addExpense(
                userId,
                isPersonalMode ? null : groupId,
                category,
                amount,
                date,
                note
            );

            if (success) {
                showAlert(Alert.AlertType.INFORMATION, "Success",
                    "✅ Expense added successfully!\n\nAmount: ৳" + String.format("%.2f", amount) +
                    "\nCategory: " + category +
                    "\nDate: " + date);
                closeDialog();
            } else {
                showAlert(Alert.AlertType.ERROR, "Error",
                    "❌ Failed to add expense!\n\nPlease try again.");
            }
        }
    }

    /**
     * Handle cancel button click
     */
    @FXML
    private void handleCancel() {
        // Confirm if user has entered data
        if (!amountField.getText().isEmpty() || categoryComboBox.getValue() != null ||
            !noteArea.getText().isEmpty()) {
            Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
            confirmAlert.setTitle("Confirm Cancel");
            confirmAlert.setHeaderText("Discard Changes?");
            confirmAlert.setContentText("You have unsaved changes. Are you sure you want to cancel?");

            var result = confirmAlert.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                closeDialog();
            }
        } else {
            closeDialog();
        }
    }

    /**
     * Validate all input fields
     */
    private boolean validateInput() {
        // Validate category
        if (categoryComboBox.getValue() == null || categoryComboBox.getValue().isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Validation Error",
                "⚠️ Please select a category!");
            categoryComboBox.requestFocus();
            return false;
        }

        // Validate amount
        if (amountField.getText().isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Validation Error",
                "⚠️ Please enter an amount!");
            amountField.requestFocus();
            return false;
        }

        try {
            double amount = Double.parseDouble(amountField.getText());
            if (amount <= 0) {
                showAlert(Alert.AlertType.WARNING, "Validation Error",
                    "⚠️ Amount must be greater than 0!");
                amountField.requestFocus();
                return false;
            }
            if (amount > 1000000) {
                showAlert(Alert.AlertType.WARNING, "Validation Error",
                    "⚠️ Amount seems too large! Please check.");
                amountField.requestFocus();
                return false;
            }
        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.WARNING, "Validation Error",
                "⚠️ Please enter a valid amount!");
            amountField.requestFocus();
            return false;
        }

        // Validate date
        if (datePicker.getValue() == null) {
            showAlert(Alert.AlertType.WARNING, "Validation Error",
                "⚠️ Please select a date!");
            datePicker.requestFocus();
            return false;
        }

        // Check if date is not in the future
        if (datePicker.getValue().isAfter(LocalDate.now())) {
            showAlert(Alert.AlertType.WARNING, "Validation Error",
                "⚠️ Date cannot be in the future!");
            datePicker.requestFocus();
            return false;
        }

        return true;
    }

    /**
     * Show alert dialog
     */
    private void showAlert(Alert.AlertType alertType, String title, String message) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Close the dialog
     */
    private void closeDialog() {
        Stage stage = (Stage) saveButton.getScene().getWindow();
        stage.close();
    }
}
